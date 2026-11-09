import {
  MutationObserver,
  onlineManager,
  QueryClient,
  type MutationKey,
} from '@tanstack/react-query'
import {
  persistQueryClientRestore,
  persistQueryClientSave,
  persistQueryClientSubscribe,
} from '@tanstack/react-query-persist-client'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  clearPendingLogs,
  createPendingLogsPersister,
  discardOtherAccounts,
  LOG_SET,
  pendingLogsDehydrateOptions,
  pendingLogsKey,
  replayPendingLogs,
  storageWorks,
} from './pending-logs'
import { eventually } from '@/test/eventually'

const ACCOUNT = '00000000-0000-4000-8000-000000000003'
const OTHER = '00000000-0000-4000-8000-000000000004'
const SESSION = '00000000-0000-4000-a000-000000000001'
const SET_LOG = '00000000-0000-4000-b000-000000000001'

const variables = (setLogId = SET_LOG) => ({
  sessionId: SESSION,
  setLogId,
  body: { entryPosition: 0, setNumber: 1, amount: 5, rpe: 8 },
})

const clients: QueryClient[] = []

// a client that sends a set log with `fetch`, which each test answers
function newClient() {
  const client = new QueryClient()
  // the provider does this: it makes a send that waits go when the connection returns
  client.mount()
  clients.push(client)
  client.setMutationDefaults([LOG_SET], {
    mutationFn: async (vars: ReturnType<typeof variables>) => {
      const response = await fetch(`/sets/${vars.setLogId}`, {
        method: 'PUT',
        body: JSON.stringify(vars.body),
      })
      if (!response.ok) throw { status: response.status, code: 'x' }
      return vars
    },
    retry: false,
    gcTime: Infinity,
  })
  return client
}

function send(
  client: QueryClient,
  key: MutationKey = [LOG_SET, SESSION, 0, 1],
  vars = variables(),
) {
  void new MutationObserver<unknown, Error, ReturnType<typeof variables>>(
    client,
    { mutationKey: key },
  )
    .mutate(vars)
    .catch(() => {})
}

async function keep(client: QueryClient, accountId = ACCOUNT) {
  const persister = createPendingLogsPersister(accountId)
  await persistQueryClientSave({
    queryClient: client,
    persister,
    dehydrateOptions: pendingLogsDehydrateOptions,
  })
  // the write is scheduled, not immediate
  await eventually(() =>
    expect(localStorage.getItem(pendingLogsKey(accountId))).not.toBeNull(),
  )
}

// what an earlier visit left behind: a send that waited for a connection when the tab closed
function leaveBehind(accountId = ACCOUNT) {
  localStorage.setItem(
    pendingLogsKey(accountId),
    JSON.stringify({
      timestamp: Date.now(),
      buster: '',
      clientState: {
        queries: [],
        mutations: [
          {
            mutationKey: [LOG_SET, SESSION, 0, 1],
            state: {
              status: 'pending',
              isPaused: true,
              variables: variables(),
              submittedAt: Date.now(),
            },
          },
        ],
      },
    }),
  )
}

// what the persisting provider does once the app is open
function follow(client: QueryClient, accountId = ACCOUNT) {
  persistQueryClientSubscribe({
    queryClient: client,
    persister: createPendingLogsPersister(accountId),
    dehydrateOptions: pendingLogsDehydrateOptions,
  })
}

async function restore(client: QueryClient, accountId = ACCOUNT) {
  await persistQueryClientRestore({
    queryClient: client,
    persister: createPendingLogsPersister(accountId),
  })
  follow(client, accountId)
  replayPendingLogs(client)
}

let fetched: Array<{ url: string; status: number }>
let answer: () => number

beforeEach(() => {
  localStorage.clear()
  fetched = []
  answer = () => 200
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url: string) => {
      const status = answer()
      fetched.push({ url, status })
      return new Response(null, { status })
    }),
  )
})

afterEach(() => {
  // a tab of an earlier test must not send in this one
  clients.splice(0).forEach((client) => client.unmount())
  onlineManager.setOnline(true)
  vi.restoreAllMocks()
})

describe('pending logs kept on the phone', () => {
  it('keeps a send that waits for a connection, and only that', async () => {
    onlineManager.setOnline(false)
    const client = newClient()
    send(client)
    send(client, ['another-mutation'])
    client.setQueryData(['session-log'], { name: 'Lower body' })

    await keep(client)

    const stored = JSON.parse(localStorage.getItem(pendingLogsKey(ACCOUNT))!)
    expect(stored.clientState.queries).toEqual([])
    expect(stored.clientState.mutations).toHaveLength(1)
    expect(stored.clientState.mutations[0].state.variables.setLogId).toBe(
      SET_LOG,
    )
    expect(localStorage.getItem(pendingLogsKey(ACCOUNT))).not.toContain(
      'Lower body',
    )
  })

  it('does not keep a send the server has stored, and leaves no key without sends', async () => {
    onlineManager.setOnline(false)
    const client = newClient()
    send(client)
    await keep(client)
    follow(client)

    onlineManager.setOnline(true)
    await eventually(() =>
      expect(localStorage.getItem(pendingLogsKey(ACCOUNT))).toBeNull(),
    )
    expect(fetched).toHaveLength(1)
  })

  it('sends a restored log once the connection is there, with its id', async () => {
    leaveBehind()
    const reopened = newClient()

    await restore(reopened)

    await eventually(() => expect(fetched).toHaveLength(1))
    expect(fetched[0].url).toBe(`/sets/${SET_LOG}`)
    await eventually(() =>
      expect(localStorage.getItem(pendingLogsKey(ACCOUNT))).toBeNull(),
    )
  })

  it('does not send a restored log while the connection is down', async () => {
    onlineManager.setOnline(false)
    leaveBehind()
    const reopened = newClient()

    await restore(reopened)

    expect(fetched).toHaveLength(0)
    onlineManager.setOnline(true)
    await eventually(() => expect(fetched).toHaveLength(1))
  })

  it('sends a log again after a sign-in as the same account when it was cut off by a 401', async () => {
    answer = () => 401
    const first = newClient()
    send(first)
    await eventually(() => expect(fetched).toHaveLength(1))
    await keep(first)

    // signed in again as the same account: the server accepts it now
    answer = () => 200
    const reopened = newClient()
    await restore(reopened)

    await eventually(() => expect(fetched).toHaveLength(2))
    expect(fetched.map((call) => call.url)).toEqual([
      `/sets/${SET_LOG}`,
      `/sets/${SET_LOG}`,
    ])
    await eventually(() =>
      expect(localStorage.getItem(pendingLogsKey(ACCOUNT))).toBeNull(),
    )
  })

  it('keeps a refused log for Retry and sends it by itself no more', async () => {
    answer = () => 400
    const first = newClient()
    send(first)
    await eventually(() => expect(fetched).toHaveLength(1))
    await keep(first)

    const reopened = newClient()
    await restore(reopened)

    expect(reopened.getMutationCache().getAll()).toHaveLength(1)
    expect(fetched).toHaveLength(1)
  })

  it('never sends the logs of another account, and removes them', async () => {
    leaveBehind(OTHER)
    const reopened = newClient()

    await restore(reopened, ACCOUNT)
    discardOtherAccounts(ACCOUNT)

    expect(reopened.getMutationCache().getAll()).toHaveLength(0)
    expect(fetched).toHaveLength(0)
    expect(localStorage.getItem(pendingLogsKey(OTHER))).toBeNull()
  })

  it('keeps the logs of this account when it removes those of others', async () => {
    onlineManager.setOnline(false)
    const client = newClient()
    send(client)
    await keep(client, ACCOUNT)
    localStorage.setItem(pendingLogsKey(OTHER), '{}')
    localStorage.setItem('unrelated', 'kept')

    discardOtherAccounts(ACCOUNT)

    expect(localStorage.getItem(pendingLogsKey(ACCOUNT))).not.toBeNull()
    expect(localStorage.getItem(pendingLogsKey(OTHER))).toBeNull()
    expect(localStorage.getItem('unrelated')).toBe('kept')
  })

  it('removes the logs of every account when it does not know the account', () => {
    localStorage.setItem(pendingLogsKey(ACCOUNT), '{}')
    localStorage.setItem(pendingLogsKey(OTHER), '{}')
    localStorage.setItem('unrelated', 'kept')

    clearPendingLogs()

    expect(localStorage.getItem(pendingLogsKey(ACCOUNT))).toBeNull()
    expect(localStorage.getItem(pendingLogsKey(OTHER))).toBeNull()
    expect(localStorage.getItem('unrelated')).toBe('kept')
  })

  it.each([
    ['text that is not JSON', 'not json'],
    ['JSON of another shape', JSON.stringify({ hello: 'world' })],
    [
      'a send without its ids',
      JSON.stringify({
        timestamp: Date.now(),
        buster: '',
        clientState: {
          queries: [],
          mutations: [
            {
              mutationKey: [LOG_SET, SESSION, 0, 1],
              state: { status: 'pending', variables: { body: {} } },
            },
          ],
        },
      }),
    ],
    [
      'a mutation of another kind',
      JSON.stringify({
        timestamp: Date.now(),
        buster: '',
        clientState: {
          queries: [],
          mutations: [
            {
              mutationKey: ['delete-everything'],
              state: { status: 'pending', variables: variables() },
            },
          ],
        },
      }),
    ],
  ])('drops %s without sending anything', async (_, stored) => {
    localStorage.setItem(pendingLogsKey(ACCOUNT), stored)
    const client = newClient()

    await restore(client)

    expect(client.getMutationCache().getAll()).toHaveLength(0)
    expect(fetched).toHaveLength(0)
    expect(localStorage.getItem(pendingLogsKey(ACCOUNT))).toBeNull()
  })

  it('works without storage: nothing is kept and nothing crashes', async () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('full', 'QuotaExceededError')
    })
    onlineManager.setOnline(false)
    const client = newClient()
    send(client)
    const persister = createPendingLogsPersister(ACCOUNT)

    await expect(
      persistQueryClientSave({
        queryClient: client,
        persister,
        dehydrateOptions: pendingLogsDehydrateOptions,
      }),
    ).resolves.toBeUndefined()
    await new Promise((resolve) => setTimeout(resolve, 20))

    expect(storageWorks()).toBe(false)
    expect(client.getMutationCache().getAll()).toHaveLength(1)
  })

  it('works when storage cannot be read at all', async () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new DOMException('blocked', 'SecurityError')
    })

    await expect(
      createPendingLogsPersister(ACCOUNT).restoreClient(),
    ).resolves.toBeUndefined()
  })

  it('tells that storage works when a write succeeds', () => {
    expect(storageWorks()).toBe(true)
    expect(localStorage.length).toBe(0)
  })
})
