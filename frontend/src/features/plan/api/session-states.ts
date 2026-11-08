import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api/client'
import type { components } from '@/lib/api/schema'

export type SessionStates = components['schemas']['SessionStatesResponse']
export type SessionStateEntry = SessionStates['sessions'][number]
export type SessionState = NonNullable<SessionStateEntry['state']>

/**
 * Where each planned session of a plan week stands, the current week without `week`. A failure leaves the screens
 * without badges: callers treat missing data as no state.
 */
export function useSessionStates(week?: number) {
  return useQuery({
    queryKey: ['session-states', week ?? 'current'],
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/session-states', {
        params: { query: { week } },
      })
      // openapi-fetch returns errors instead of throwing; throw the problem detail so the query fails with its code
      if (error !== undefined) throw error
      return data
    },
    retry: false,
  })
}
