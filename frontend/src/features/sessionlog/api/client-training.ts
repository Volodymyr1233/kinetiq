import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api/client'
import type { components } from '@/lib/api/schema'

export type ClientOverview = components['schemas']['ClientOverviewResponse']
export type ClientSession = components['schemas']['ClientSessionResponse']

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
