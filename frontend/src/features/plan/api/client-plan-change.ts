import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api/client'
import { problemCode } from '@/lib/api/problem'
import type { components } from '@/lib/api/schema'

export type OpenPlanChange =
  components['schemas']['ClientPlanChangeDetailResponse']

/** The client's plan change that has not ended, or `null` when there is none; the Plan and Today cards show it. */
export function useOpenPlanChange() {
  return useQuery({
    queryKey: ['plan', 'change', 'current'],
    queryFn: async (): Promise<OpenPlanChange | null> => {
      const { data, error } = await api.GET('/api/v1/plan/changes/current')
      if (problemCode(error) === 'plan-change.not-found') return null
      if (error !== undefined) throw error
      return data
    },
    // a missing card is better than an error above the plan: the next visit tries again
    retry: false,
  })
}
