import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '@/lib/api/client'
import type { components } from '@/lib/api/schema'

export type SessionLog = components['schemas']['SessionLogResponse']
export type LoggedEntry = SessionLog['entries'][number]
export type PlannedSet = LoggedEntry['sets'][number]

export const sessionLogKey = (language: string) => ['session-log', language]

/**
 * Today's session, started if it was not yet; opening the logger again returns the same one, so this is a query. A
 * rest day or no plan fails with its code, which the page turns into a way back to Today.
 */
export function useSession() {
  const { i18n } = useTranslation()
  return useQuery({
    // the backend names exercises in the UI language, so the language is part of the key
    queryKey: sessionLogKey(i18n.language),
    queryFn: async () => {
      const { data, error } = await api.POST('/api/v1/session-logs')
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

/** Logs a set, or changes it when the id was logged already; sending the same id again stores the set once. */
export function useLogSet(sessionId: string) {
  const queryClient = useQueryClient()
  const { i18n } = useTranslation()
  return useMutation({
    mutationFn: async ({
      setLogId,
      body,
    }: {
      setLogId: string
      body: LogSetBody
    }) => {
      const { data, error } = await api.PUT(
        '/api/v1/session-logs/{id}/set-logs/{setLogId}',
        { params: { path: { id: sessionId, setLogId } }, body },
      )
      if (error !== undefined) throw error
      return data
    },
    onSuccess: (set) => {
      queryClient.setQueryData<SessionLog>(
        sessionLogKey(i18n.language),
        (session) => session && withLoggedSet(session, set),
      )
      // a finished session's summary follows its sets
      void queryClient.invalidateQueries({ queryKey: ['session-summary'] })
    },
  })
}
