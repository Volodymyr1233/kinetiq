import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '@/lib/api/client'
import type { components } from '@/lib/api/schema'

export type Today = components['schemas']['TodayResponse']

/** What the signed-in client trains today in their time zone: a session, a rest day or a finished plan. */
export function useToday() {
  const { i18n } = useTranslation()
  return useQuery({
    // the backend names exercises in the UI language, so the language is part of the key
    queryKey: ['today', i18n.language],
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/today')
      // openapi-fetch returns errors instead of throwing; throw the problem detail so the query fails with its code
      if (error !== undefined) throw error
      return data
    },
  })
}
