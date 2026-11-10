import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api/client'
import type { components } from '@/lib/api/schema'

export type PlanChange = components['schemas']['PlanChangeResponse']
export type PlanChangeTarget = components['schemas']['PlanChangeTargetResponse']

export function usePlanChange(id: string | undefined) {
  return useQuery({
    queryKey: ['plan-changes', id],
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/plan-changes/{id}', {
        params: { path: { id: id! } },
      })
      // openapi-fetch returns errors instead of throwing; throw the problem detail so the query fails with its code
      if (error !== undefined) throw error
      return data
    },
    enabled: id !== undefined,
  })
}
