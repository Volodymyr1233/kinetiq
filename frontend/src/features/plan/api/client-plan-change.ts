import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '@/lib/api/client'
import { problemCode } from '@/lib/api/problem'
import type { components } from '@/lib/api/schema'

export type OpenPlanChange =
  components['schemas']['ClientPlanChangeDetailResponse']

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
