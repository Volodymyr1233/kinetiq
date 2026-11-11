import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api/client'
import type { components } from '@/lib/api/schema'

export type PlanChangeApproval =
  components['schemas']['PlanChangeApprovalResponse']

/**
 * The trainer's plan changes that wait or ended in the last 30 days, newest first; a decision refreshes it through the
 * `approvals` key.
 */
export function usePlanChangeApprovals() {
  return useQuery({
    queryKey: ['approvals', 'plan-changes'],
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/approvals/plan-changes')
      if (error !== undefined) throw error
      return data
    },
  })
}
