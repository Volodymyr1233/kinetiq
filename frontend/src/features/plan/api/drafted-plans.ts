import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api/client'
import type { components } from '@/lib/api/schema'

export type DraftedPlan = components['schemas']['DraftedPlanResponse']

/** The trainer's drafted plans, oldest first; a save or publish refreshes it through the `approvals` key. */
export function useDraftedPlans() {
  return useQuery({
    queryKey: ['approvals', 'drafted-plans'],
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/approvals/drafted-plans')
      if (error !== undefined) throw error
      return data
    },
  })
}
