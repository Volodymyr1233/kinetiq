import {
  useMutation,
  useMutationState,
  useQuery,
  useQueryClient,
  type QueryClient,
} from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '@/lib/api/client'
import { isWorthRetrying, problemStatus } from '@/lib/api/problem'
import { LOG_SET } from '@/lib/pending-logs'
import type { components } from '@/lib/api/schema'
import { SESSION_LOGS_KEY as SESSION_LOGS } from '@/lib/query-keys'

export type SessionLog = components['schemas']['SessionLogResponse']
export type LoggedEntry = SessionLog['entries'][number]
export type PlannedSet = LoggedEntry['sets'][number]

/**
 * Writes a change into every cached copy of the session: today's logger and an earlier session opened by its id can
 * hold the same one.
 */
export function updateSessionLog(
  queryClient: QueryClient,
  sessionId: string,
  update: (session: SessionLog) => SessionLog,
) {
  queryClient.setQueriesData<SessionLog>(
    { queryKey: SESSION_LOGS },
    (session) => (session?.id === sessionId ? update(session) : session),
  )
}

/**
 * Today's session, started if it was not yet, or the client's session of `id`, of any day, without starting anything.
 * Opening the logger again returns the same one, so this is a query. A rest day or no plan fails with its code, and so
 * does an id that is not the client's, which the page turns into a way back.
 */
export function useSession(id?: string) {
  const { i18n } = useTranslation()
  return useQuery({
    // the backend names exercises in the UI language, so the language is part of the key
    queryKey: [...SESSION_LOGS, i18n.language, id ?? 'today'],
    queryFn: async () => {
      const { data, error } =
        id === undefined
          ? await api.POST('/api/v1/session-logs')
          : await api.GET('/api/v1/session-logs/{id}', {
              params: { path: { id } },
            })
      // openapi-fetch returns errors instead of throwing; throw the problem detail so the query fails with its code
      if (error !== undefined) throw error
      return data
    },
    // what is logged here is written into the cache as it happens; a refetch would only repeat the start
    staleTime: Infinity,
    refetchOnWindowFocus: false,
  })
}

export type LogSetBody = components['schemas']['LogSetRequest']
type LoggedSet = components['schemas']['SetLogResponse']

// the answer says which place was logged, so the row shows as logged without asking the server again
function withLoggedSet(session: SessionLog, set: LoggedSet): SessionLog {
  return {
    ...session,
    entries: session.entries.map((entry) =>
      entry.position !== set.entryPosition
        ? entry
        : {
            ...entry,
            sets: entry.sets.map((planned) =>
              planned.number !== set.setNumber
                ? planned
                : {
                    ...planned,
                    log: {
                      id: set.id,
                      loadKg: set.loadKg,
                      amount: set.amount,
                      rpe: set.rpe,
                      loggedAt: set.loggedAt,
                    },
                  },
            ),
          },
    ),
  }
}

export type LogSetVariables = {
  sessionId: string
  setLogId: string
  body: LogSetBody
}

/** The key of every send of one place; the first part is what the phone keeps and replays. */
export function logSetKey(
  sessionId: string,
  entryPosition: number,
  setNumber: number,
) {
  return [LOG_SET, sessionId, entryPosition, setNumber] as const
}

/**
 * What a send of a set does, kept with the query client so a send restored after a reload still knows how to run and
 * what to update. The sends of one set run one after another in the order made, so the last values win.
 */
export function registerLogSetDefaults(queryClient: QueryClient) {
  queryClient.setMutationDefaults([LOG_SET], {
    mutationFn: async ({ sessionId, setLogId, body }: LogSetVariables) => {
      const { data, error } = await api.PUT(
        '/api/v1/session-logs/{id}/set-logs/{setLogId}',
        { params: { path: { id: sessionId, setLogId } }, body },
      )
      if (error !== undefined) throw error
      return data
    },
    // a refusal is final; a lost connection or a failing server is tried again, then left to Retry
    retry: (failures, error) => failures < 3 && isWorthRetrying(error),
    retryDelay: (attempt) => Math.min(1000 * 2 ** attempt, 30_000),
    // a failed send must outlive its row until the client retries or discards it
    gcTime: Infinity,
    onSuccess: (set, { sessionId }) => {
      updateSessionLog(queryClient, sessionId, (session) =>
        withLoggedSet(session, set),
      )
      // a finished session's summary follows its sets
      void queryClient.invalidateQueries({ queryKey: ['session-summary'] })
    },
  })
}

/**
 * Logs a set, or changes it when its id was logged already; sending the same id again stores the set once. With no
 * connection the send waits, and the phone keeps it (`registerLogSetDefaults` says how it runs).
 */
export function useLogSet(
  sessionId: string,
  entryPosition: number,
  setNumber: number,
  setLogId: string,
) {
  return useMutation<LoggedSet, unknown, LogSetVariables>({
    mutationKey: logSetKey(sessionId, entryPosition, setNumber),
    scope: { id: `${LOG_SET}:${setLogId}` },
  })
}

/**
 * The newest send of a place that the server has not stored, or `undefined`. The row shows its values in place of the
 * server's and reuses its id, so logging the place again after a reload sends the id already kept. `failed` is a
 * refusal or the end of the tries; a send cut off by a sign-in is still waiting.
 */
export function usePendingLog(
  sessionId: string,
  entryPosition: number,
  setNumber: number,
) {
  const queryClient = useQueryClient()
  const [newest] = useMutationState({
    filters: {
      mutationKey: logSetKey(sessionId, entryPosition, setNumber),
      predicate: (mutation) => mutation.state.status !== 'success',
    },
    select: (mutation) => ({
      id: mutation.mutationId,
      failed:
        mutation.state.status === 'error' &&
        problemStatus(mutation.state.error) !== 401,
      submittedAt: mutation.state.submittedAt,
      variables: mutation.state.variables as LogSetVariables,
    }),
  }).sort((a, b) => b.submittedAt - a.submittedAt)
  if (newest === undefined) return undefined
  return {
    setLogId: newest.variables.setLogId,
    body: newest.variables.body,
    failed: newest.failed,
    discard: () => {
      const cache = queryClient.getMutationCache()
      const mutation = cache.getAll().find((m) => m.mutationId === newest.id)
      if (mutation) cache.remove(mutation)
    },
  }
}
