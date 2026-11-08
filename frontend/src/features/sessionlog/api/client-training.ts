import { skipToken, useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api/client'
import type { components } from '@/lib/api/schema'

export type ClientOverview = components['schemas']['ClientOverviewResponse']
export type ClientSession = components['schemas']['ClientSessionResponse']
export type ClientSessionLog = components['schemas']['ClientSessionLogResponse']

/** One of the trainer's clients as their trainer sees them: this week, the recent sessions and the quiz answers. */
export function useClientOverview(clientId: string) {
  return useQuery({
    queryKey: ['client-overview', clientId],
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/clients/{id}/overview', {
        params: { path: { id: clientId } },
      })
      // openapi-fetch returns errors instead of throwing; throw the problem detail so the query fails with its code
      if (error !== undefined) throw error
      return data
    },
  })
}

/** Every session of one of the trainer's clients, newest first. */
export function useClientSessions(clientId: string) {
  return useQuery({
    queryKey: ['client-sessions', clientId],
    queryFn: async () => {
      const { data, error } = await api.GET(
        '/api/v1/clients/{id}/session-logs',
        { params: { path: { id: clientId } } },
      )
      if (error !== undefined) throw error
      return data.sessions
    },
  })
}

/**
 * One session log of one of the trainer's clients, each planned set with its target and its log. A session never
 * started has no log, so nothing is asked for it.
 */
export function useClientSessionLog(
  clientId: string,
  sessionLogId: string | undefined,
) {
  return useQuery({
    queryKey: ['client-session-log', clientId, sessionLogId],
    queryFn:
      sessionLogId === undefined
        ? skipToken
        : async () => {
            const { data, error } = await api.GET(
              '/api/v1/clients/{id}/session-logs/{sessionLogId}',
              { params: { path: { id: clientId, sessionLogId } } },
            )
            if (error !== undefined) throw error
            return data
          },
  })
}
