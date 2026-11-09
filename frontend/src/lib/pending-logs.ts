import { createSyncStoragePersister } from '@tanstack/query-sync-storage-persister'
import { problemStatus } from '@/lib/api/problem'
import type { DehydrateOptions, QueryClient } from '@tanstack/react-query'
import type {
  PersistedClient,
  Persister,
} from '@tanstack/react-query-persist-client'

/** First part of the key of every set-log mutation: what is kept on the phone and replayed. */
export const LOG_SET = 'log-set'

const PREFIX = 'kinetiq.pending-logs.'
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

export const pendingLogsKey = (accountId: string) => `${PREFIX}${accountId}`

// reading `localStorage` itself can throw (blocked site data), so every access sits in a guard: with no storage the
// queue still works for the open tab, it just is not kept
const storage = {
  getItem(key: string) {
    try {
      return localStorage.getItem(key)
    } catch {
      return null
    }
  },
  setItem(key: string, value: string) {
    try {
      localStorage.setItem(key, value)
    } catch {
      // private mode or a full quota: the log stays in memory
    }
  },
  removeItem(key: string) {
    try {
      localStorage.removeItem(key)
    } catch {
      // nothing is stored then
    }
  },
}

/** Tells whether a write to the phone's storage succeeds; the logger says so when it does not. */
export function storageWorks() {
  const probe = `${PREFIX}probe`
  try {
    localStorage.setItem(probe, '1')
    localStorage.removeItem(probe)
    return true
  } catch {
    return false
  }
}

function ownKeys() {
  const keys: string[] = []
  try {
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key?.startsWith(PREFIX)) keys.push(key)
    }
  } catch {
    // no storage, nothing kept
  }
  return keys
}

/** Removes what another account left on this phone, so it can never be sent under this one. */
export function discardOtherAccounts(accountId: string) {
  for (const key of ownKeys()) {
    if (key !== pendingLogsKey(accountId)) storage.removeItem(key)
  }
}

/** Removes what one account kept on the phone, or what every account kept when the account is not known. */
export function clearPendingLogs(accountId?: string) {
  if (accountId !== undefined) storage.removeItem(pendingLogsKey(accountId))
  else ownKeys().forEach((key) => storage.removeItem(key))
}

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null

const isUuid = (value: unknown) => typeof value === 'string' && UUID.test(value)

function isPendingLog(mutation: unknown) {
  if (!isRecord(mutation) || !isRecord(mutation.state)) return false
  const { mutationKey } = mutation
  const { variables } = mutation.state
  return (
    Array.isArray(mutationKey) &&
    mutationKey[0] === LOG_SET &&
    isRecord(variables) &&
    isUuid(variables.setLogId) &&
    isUuid(variables.sessionId) &&
    isRecord(variables.body)
  )
}

// all or nothing: a value written by another version, or by hand, is not worth guessing at
function isKept(client: unknown): client is PersistedClient {
  return (
    isRecord(client) &&
    typeof client.timestamp === 'number' &&
    isRecord(client.clientState) &&
    Array.isArray(client.clientState.queries) &&
    client.clientState.queries.length === 0 &&
    Array.isArray(client.clientState.mutations) &&
    client.clientState.mutations.every(isPendingLog)
  )
}

/** Keeps the pending logs of one account under its own key, and holds nothing else. */
export function createPendingLogsPersister(accountId: string): Persister {
  const key = pendingLogsKey(accountId)
  const base = createSyncStoragePersister({
    storage: {
      getItem: storage.getItem,
      // an empty queue is no key at all, so sending the last log or signing out leaves nothing behind
      setItem: (k, value) =>
        value === '' ? storage.removeItem(k) : storage.setItem(k, value),
      removeItem: storage.removeItem,
    },
    key,
    // the tab may close right after a log, so a write is not held back
    throttleTime: 0,
    serialize: (client) =>
      client.clientState.mutations.length === 0 ? '' : JSON.stringify(client),
  })
  return {
    ...base,
    async restoreClient() {
      try {
        const client: unknown = await base.restoreClient()
        if (isKept(client)) return client
      } catch {
        // unreadable: dropped below
      }
      storage.removeItem(key)
      return undefined
    },
  }
}

/** Keeps the set-log mutations that the server has not stored, and nothing else. */
export const pendingLogsDehydrateOptions: DehydrateOptions = {
  shouldDehydrateQuery: () => false,
  shouldDehydrateMutation: (mutation) =>
    mutation.options.mutationKey?.[0] === LOG_SET &&
    mutation.state.status !== 'success',
}

/**
 * Sends the kept logs after they were restored: those that were waiting for a connection, and those a sign-in cut off,
 * which only waited for the same account to sign in again. A refused one stays as it is, for Retry.
 */
export function replayPendingLogs(queryClient: QueryClient) {
  const kept = queryClient
    .getMutationCache()
    .getAll()
    .filter((mutation) => mutation.options.mutationKey?.[0] === LOG_SET)
  for (const mutation of kept) {
    if (mutation.state.status === 'pending' && !mutation.state.isPaused) {
      // it was on its way when the tab closed; a repeat stores the set once
      void mutation.continue().catch(() => {})
    } else if (
      mutation.state.status === 'error' &&
      problemStatus(mutation.state.error) === 401
    ) {
      void mutation.execute(mutation.state.variables).catch(() => {})
    }
  }
  void queryClient.resumePausedMutations()
}
