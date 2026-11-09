import { onlineManager, QueryClientProvider } from '@tanstack/react-query'
import { PersistQueryClientProvider } from '@tanstack/react-query-persist-client'
import { act, cleanup, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { useSignOut } from '@/features/account/api/account'
import {
  createPendingLogsPersister,
  discardOtherAccounts,
  pendingLogsDehydrateOptions,
  pendingLogsKey,
  replayPendingLogs,
} from '@/lib/pending-logs'
import { WeightUnitContext } from '@/lib/weight'
import { json, loggerClient, SIGNED_IN, stubApi } from '@/test/page'
import { LoggerPage } from './logger-page'
import { eventually } from '@/test/eventually'

const ACCOUNT = SIGNED_IN.CLIENT.id
const SESSION = '00000000-0000-4000-a000-000000000001'

const SQUAT = {
  position: 0,
  exerciseId: '00000000-0000-4000-9100-000000000011',
  exerciseName: 'Back squat',
  measure: 'REPS',
  restSeconds: 120,
  sets: [
    { number: 1, target: { loadKg: 100, amount: 5, rpe: 8 } },
    { number: 2, target: { loadKg: 100, amount: 5, rpe: 8 } },
  ],
}

type Put = { id: string; body: Record<string, number> }

// the server keeps what it is sent and shows it when the session is opened again
function server() {
  const puts: Put[] = []
  // a send waits here while a test holds it, as a slow connection does
  const hold = { gate: Promise.resolve() }
  const stored = new Map<string, Record<string, unknown>>()
  const reply = async (request: Request) => {
    if (request.method === 'POST') {
      return json(
        {
          id: SESSION,
          date: '2026-11-07',
          name: 'Lower body',
          startedAt: new Date().toISOString(),
          entries: [
            {
              ...SQUAT,
              sets: SQUAT.sets.map((set) => ({
                ...set,
                ...(stored.has(String(set.number)) && {
                  log: stored.get(String(set.number)),
                }),
              })),
            },
          ],
        },
        201,
      )
    }
    await hold.gate
    const body = (await request.clone().json()) as Put['body']
    const id = new URL(request.url).pathname.split('/').pop()!
    puts.push({ id, body })
    const log = { id, loggedAt: '2026-11-07T10:05:00Z', ...body }
    stored.set(String(body.setNumber), log)
    return json(log)
  }
  return { reply, puts, hold }
}

// as the client area mounts it: the account's kept logs come back before the logger shows, and are sent
function openLogger(reply: (request: Request) => Promise<Response>) {
  stubApi(reply)
  const client = loggerClient()
  const router = createMemoryRouter(
    [{ path: '/app/session', Component: LoggerPage }],
    { initialEntries: ['/app/session'] },
  )
  render(
    <PersistQueryClientProvider
      client={client}
      persistOptions={{
        persister: createPendingLogsPersister(ACCOUNT),
        dehydrateOptions: pendingLogsDehydrateOptions,
      }}
      onSuccess={() => {
        discardOtherAccounts(ACCOUNT)
        replayPendingLogs(client)
      }}
    >
      <WeightUnitContext value="KG">
        <RouterProvider router={router} />
      </WeightUnitContext>
    </PersistQueryClientProvider>,
  )
  return client
}

// the logger is open when the connection drops: opening it without one is not supported
async function openLoggerThenGoOffline(
  reply: (request: Request) => Promise<Response>,
) {
  openLogger(reply)
  await findRow()
  act(() => onlineManager.setOnline(false))
}

const tick = async () =>
  userEvent.click(
    await screen.findByRole('button', {
      name: 'Log set 1 of Back squat as planned',
    }),
  )

const firstRow = () =>
  within(screen.getByRole('region', { name: 'Back squat' })).getAllByRole(
    'listitem',
  )[0]

const KEY = pendingLogsKey(ACCOUNT)

const goOnline = () => act(() => onlineManager.setOnline(true))

async function keptOnPhone() {
  await eventually(() => expect(localStorage.getItem(KEY)).not.toBeNull())
  return JSON.parse(localStorage.getItem(KEY)!).clientState.mutations as Array<{
    state: { variables: { setLogId: string; body: Put['body'] } }
  }>
}

describe('logging without a connection', () => {
  beforeEach(() => localStorage.clear())
  afterEach(() => {
    onlineManager.setOnline(true)
    localStorage.clear()
  })

  it('shows a set as logged at once, sends nothing, and sends it once when the connection returns', async () => {
    const { reply, puts } = server()
    await openLoggerThenGoOffline(reply)

    await tick()

    expect(
      await within(firstRow()).findByText('Saved on this phone'),
    ).toBeInTheDocument()
    expect(within(firstRow()).getByText('5 reps')).toBeInTheDocument()
    expect(puts).toHaveLength(0)
    const [kept] = await keptOnPhone()

    await goOnline()

    await eventually(() => expect(puts).toHaveLength(1))
    expect(puts[0].id).toBe(kept.state.variables.setLogId)
    expect(puts[0].body).toEqual({
      entryPosition: 0,
      setNumber: 1,
      loadKg: 100,
      amount: 5,
      rpe: 8,
    })
    expect(within(firstRow()).getByText('Synced')).toBeInTheDocument()
    // the server has it, so the phone keeps nothing
    await eventually(() => expect(localStorage.getItem(KEY)).toBeNull())
    expect(puts).toHaveLength(1)
  })

  it('sends a set kept before the tab was closed, once, with its id, when the logger opens online', async () => {
    const { reply, puts } = server()
    await openLoggerThenGoOffline(reply)
    await tick()
    const [kept] = await keptOnPhone()
    // the tab closes: nothing of it runs any more
    cleanup()
    expect(puts).toHaveLength(0)

    onlineManager.setOnline(true)
    openLogger(reply)

    await eventually(() => expect(puts).toHaveLength(1))
    expect(puts[0].id).toBe(kept.state.variables.setLogId)
    expect(await within(await findRow()).findByText('Synced')).toBeVisible()
    await eventually(() => expect(localStorage.getItem(KEY)).toBeNull())
    expect(puts).toHaveLength(1)
  })

  it('shows a kept set as logged after a reload, and changes it with the id it has', async () => {
    const { reply, puts, hold } = server()
    await openLoggerThenGoOffline(reply)
    await tick()
    const [kept] = await keptOnPhone()
    cleanup()

    // reopened with a slow connection: the kept send is on its way, and the row already shows it
    let release!: () => void
    hold.gate = new Promise((resolve) => (release = resolve))
    onlineManager.setOnline(true)
    openLogger(reply)
    expect(
      await within(await findRow()).findByText('Saved on this phone'),
    ).toBeVisible()
    expect(
      screen.queryByRole('button', {
        name: 'Log set 1 of Back squat as planned',
      }),
    ).not.toBeInTheDocument()
    await userEvent.click(
      screen.getByRole('button', { name: /^Change set 1 of Back squat/ }),
    )
    await userEvent.click(
      screen.getByRole('button', {
        name: 'More: Repetitions, set 1 of Back squat',
      }),
    )
    await userEvent.click(screen.getByRole('button', { name: 'Update set' }))
    expect(await within(firstRow()).findByText('6 reps')).toBeInTheDocument()

    release()

    // the kept send and the change go in order, with one id, and the server ends with the last values
    await eventually(() => expect(puts).toHaveLength(2))
    expect(puts.map((put) => put.id)).toEqual([
      kept.state.variables.setLogId,
      kept.state.variables.setLogId,
    ])
    expect(puts.map((put) => put.body.amount)).toEqual([5, 6])
  })

  it('sends the last values when a set is changed twice without a connection', async () => {
    const { reply, puts } = server()
    await openLoggerThenGoOffline(reply)
    await tick()
    await within(firstRow()).findByText('Saved on this phone')

    for (let step = 0; step < 2; step++) {
      await userEvent.click(
        screen.getByRole('button', { name: /^Change set 1 of Back squat/ }),
      )
      await userEvent.click(
        screen.getByRole('button', {
          name: 'More: Repetitions, set 1 of Back squat',
        }),
      )
      await userEvent.click(screen.getByRole('button', { name: 'Update set' }))
      await within(firstRow()).findByText(`${6 + step} reps`)
    }
    expect(puts).toHaveLength(0)

    await goOnline()

    await eventually(() => expect(puts).toHaveLength(3))
    expect(puts.map((put) => put.body.amount)).toEqual([5, 6, 7])
    expect(new Set(puts.map((put) => put.id)).size).toBe(1)
    expect(await within(firstRow()).findByText('7 reps')).toBeInTheDocument()
  })

  it('removes what the phone kept when the client signs out', async () => {
    const { reply } = server()
    await openLoggerThenGoOffline(reply)
    await tick()
    await keptOnPhone()
    cleanup()

    // back online and signing out, as the settings sheet does it; no logger is open to send what was kept
    onlineManager.setOnline(true)
    const client = loggerClient()
    client.setQueryData(['current-account'], SIGNED_IN.CLIENT)
    stubApi(() => new Response(null, { status: 204 }))
    const router = createMemoryRouter(
      [
        { path: '/', Component: SignOutButton },
        { path: '/sign-in', Component: () => <h1>Sign in</h1> },
      ],
      { initialEntries: ['/'] },
    )
    render(
      <QueryClientProvider client={client}>
        <RouterProvider router={router} />
      </QueryClientProvider>,
    )
    expect(localStorage.getItem(KEY)).not.toBeNull()

    await userEvent.click(screen.getByRole('button', { name: 'Sign out' }))

    expect(
      await screen.findByRole('heading', { name: 'Sign in' }),
    ).toBeVisible()
    expect(localStorage.getItem(KEY)).toBeNull()
  })
})

function SignOutButton() {
  const signOut = useSignOut()
  return <button onClick={() => signOut.mutate()}>Sign out</button>
}

const findRow = async () => {
  await screen.findByRole('region', { name: 'Back squat' })
  return firstRow()
}
