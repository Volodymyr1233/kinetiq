import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '@/lib/api/client'
import type { components } from '@/lib/api/schema'

export type OwnPlan = components['schemas']['ClientPlanResponse']

/** The signed-in client's published plan for one plan week, the current one without `week`. */
export function useOwnPlan(week?: number) {
  const { i18n } = useTranslation()
  return useQuery({
    // the backend names exercises in the UI language, so the language is part of the key
    queryKey: ['own-plan', i18n.language, week ?? 'current'],
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/plan', {
        params: { query: { week } },
      })
      // openapi-fetch returns errors instead of throwing; throw the problem detail so the query fails with its code
      if (error !== undefined) throw error
      return data
    },
    // moving to another week keeps the strip on screen until the next one arrives
    placeholderData: keepPreviousData,
  })
}

/** Marks the client's published plan as seen, so the plan-ready screen does not show again. */
export function useAcknowledgePlan() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async () => {
      const { error } = await api.POST('/api/v1/plan/acknowledgement')
      if (error !== undefined) throw error
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['own-plan'] }),
  })
}
