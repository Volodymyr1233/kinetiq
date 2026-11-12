import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api/client'
import type { components } from '@/lib/api/schema'

export type Overview = components['schemas']['OverviewResponse']
export type OverviewClient = components['schemas']['OverviewClientResponse']
export type OverviewAlert = components['schemas']['OverviewAlertResponse']
export type ClientStatus = OverviewClient['status']

/** Client statuses most urgent first, the order the trainer's lists sort in. */
export const STATUSES: ClientStatus[] = [
  'AT_RISK',
  'WATCH',
  'PLAN_TO_REVIEW',
  'NO_PLAN',
  'ON_TRACK',
]

// under the clients' key, so whatever refreshes a client refreshes the overview too
export function useOverview() {
  return useQuery({
    queryKey: ['clients', 'overview'],
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/clients/overview')
      if (error !== undefined) throw error
      return data
    },
  })
}
