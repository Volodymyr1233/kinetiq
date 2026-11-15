import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '@/lib/api/client'
import { problemCode } from '@/lib/api/problem'
import type { components } from '@/lib/api/schema'

export type OpenPlanChange =
  components['schemas']['ClientPlanChangeDetailResponse']

// the plan change a client both accepted: upcoming until its Monday, then applied and in effect
function usePlanChangeRead(
  kind: 'upcoming' | 'applied',
  read: () => Promise<{ data?: OpenPlanChange; error?: unknown }>,
) {
  const { i18n } = useTranslation()
  return useQuery({
    // plan changes come in the account's language, which the UI follows, so the language is part of the key
    queryKey: ['plan', 'change', kind, i18n.language],
    queryFn: async (): Promise<OpenPlanChange | null> => {
      const { data, error } = await read()
      if (problemCode(error) === 'plan-change.not-found') return null
      if (error !== undefined) throw error
      return data ?? null
    },
    // the marks are a nicety on the plan: a failed read leaves the plan as it is
    retry: false,
  })
}

/** The client's plan change that both accepted and whose Monday has not begun, or `null`. */
export function useUpcomingPlanChange() {
  return usePlanChangeRead('upcoming', () =>
    api.GET('/api/v1/plan/changes/upcoming'),
  )
}

/** The client's latest plan change that the plan uses, or `null`. */
export function useAppliedPlanChange() {
  return usePlanChangeRead('applied', () =>
    api.GET('/api/v1/plan/changes/applied'),
  )
}

/** The client's plan change that has not ended, or `null` when there is none; the Plan and Today cards show it. */
export function useOpenPlanChange() {
  const { i18n } = useTranslation()
  return useQuery({
    // plan changes come in the account's language, which the UI follows, so the language is part of the key
    queryKey: ['plan', 'change', 'current', i18n.language],
    queryFn: async (): Promise<OpenPlanChange | null> => {
      const { data, error } = await api.GET('/api/v1/plan/changes/current')
      if (problemCode(error) === 'plan-change.not-found') return null
      if (error !== undefined) throw error
      return data
    },
    // a missing card is better than an error above the plan: the next visit tries again
    retry: false,
    // another language keeps the card, and the week below it, in place until its words arrive
    placeholderData: keepPreviousData,
  })
}
