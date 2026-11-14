import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '@/lib/api/client'
import type { components } from '@/lib/api/schema'
import { updateSessionLog } from './session-log'

export type SessionSummary = components['schemas']['SessionSummaryResponse']

// the backend names the exercises of the PRs in the UI language, so the language is part of the key
export const summaryKey = (language: string, sessionId: string) => [
  'session-summary',
  language,
  sessionId,
]

/** The key prefix of the states on Plan and Today, which a finish makes stale. */
const SESSION_STATES = ['session-states']

/** Finishes the session; the answer is its summary, which the done screen shows without asking again. */
export function useFinishSession(sessionId: string) {
  const queryClient = useQueryClient()
  const { i18n } = useTranslation()
  return useMutation({
    mutationFn: async () => {
      const { data, error } = await api.POST(
        '/api/v1/session-logs/{id}/finish',
        { params: { path: { id: sessionId } } },
      )
      if (error !== undefined) throw error
      return data
    },
    onSuccess: (summary) => {
      queryClient.setQueryData(summaryKey(i18n.language, sessionId), summary)
      // the logger offers "View summary" from now on
      updateSessionLog(queryClient, sessionId, (session) => ({
        ...session,
        finishedAt: new Date().toISOString(),
      }))
      void queryClient.invalidateQueries({ queryKey: SESSION_STATES })
    },
  })
}

/** The summary of a finished session, as its sets are now; a session not finished fails with its code. */
export function useSessionSummary(sessionId: string) {
  const { i18n } = useTranslation()
  return useQuery({
    queryKey: summaryKey(i18n.language, sessionId),
    queryFn: async () => {
      const { data, error } = await api.GET(
        '/api/v1/session-logs/{id}/summary',
        { params: { path: { id: sessionId } } },
      )
      // openapi-fetch returns errors instead of throwing; throw the problem detail so the query fails with its code
      if (error !== undefined) throw error
      return data
    },
  })
}

/** Saves the note for the trainer; empty text removes it. */
export function useSaveNote(sessionId: string) {
  const queryClient = useQueryClient()
  const { i18n } = useTranslation()
  return useMutation({
    mutationFn: async (text: string) => {
      const { error } = await api.PUT('/api/v1/session-logs/{id}/note', {
        params: { path: { id: sessionId } },
        body: { text },
      })
      if (error !== undefined) throw error
    },
    onSuccess: (_, text) =>
      queryClient.setQueryData<SessionSummary>(
        summaryKey(i18n.language, sessionId),
        (summary) => summary && { ...summary, note: text.trim() || undefined },
      ),
  })
}
