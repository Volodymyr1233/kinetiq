import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '@/lib/api/client'
import { problemCode } from '@/lib/api/problem'
import type { components } from '@/lib/api/schema'

export type UpcomingPlanChange = components['schemas']['PlanChangeResponse']
export type EndedPlanChange =
  components['schemas']['PlanChangeApprovalResponse']

/** The plan change the trainer and the client both accepted whose Monday has not begun, or `null` when there is none. */
export function useUpcomingPlanChange(clientId: string) {
  const { i18n } = useTranslation()
  return useQuery({
    // under the client's key, so a decision on the plan change refreshes it
    queryKey: ['clients', clientId, 'plan-change', 'upcoming', i18n.language],
    queryFn: async (): Promise<UpcomingPlanChange | null> => {
      const { data, error } = await api.GET(
        '/api/v1/clients/{id}/plan-changes/upcoming',
        { params: { path: { id: clientId } } },
      )
      if (problemCode(error) === 'plan-change.not-found') return null
      if (error !== undefined) throw error
      return data
    },
    // the banner is a nicety above the editor: a failed read leaves the editor as it is
    retry: false,
  })
}

/** The latest plan change of the client that the plan uses, from the last that ended; `null` when none is in effect. */
export function useLatestAppliedPlanChange(clientId: string) {
  const { i18n } = useTranslation()
  return useQuery({
    // the key and the answer are those of the client page's list, so both read one request
    queryKey: ['clients', clientId, 'plan-changes', 'ended', i18n.language],
    queryFn: async () => {
      const { data, error } = await api.GET(
        '/api/v1/clients/{id}/plan-changes/ended',
        { params: { path: { id: clientId } } },
      )
      if (error !== undefined) throw error
      return data.items
    },
    select: (items): EndedPlanChange | null =>
      items.find((change) => change.tookEffectAt !== undefined) ?? null,
    retry: false,
  })
}
