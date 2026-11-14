import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '@/lib/api/client'
import type { components } from '@/lib/api/schema'

export type Progress = components['schemas']['ProgressResponse']
export type ProgressWeek = components['schemas']['ProgressWeekResponse']
export type ExerciseProgress = components['schemas']['ExerciseProgressResponse']
export type ExerciseWeek = components['schemas']['ExerciseWeekResponse']
export type TopSet = components['schemas']['TopSetResponse']
export type TopSetChange = components['schemas']['TopSetChangeResponse']

/** The signed-in client's progress over the last 4, 8 or 12 plan weeks. */
export function useProgress(weeks: 4 | 8 | 12) {
  const { i18n } = useTranslation()
  return useQuery({
    // the backend names exercises in the UI language, so the language is part of the key
    queryKey: ['progress', weeks, i18n.language],
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/progress', {
        params: { query: { weeks } },
      })
      // openapi-fetch returns errors instead of throwing; throw the problem detail so the query fails with its code
      if (error !== undefined) throw error
      return data
    },
    // another period keeps the screen as it is until its figures arrive
    placeholderData: keepPreviousData,
  })
}

/** One of the signed-in trainer's clients' progress over the last 4, 8 or 12 plan weeks, the figures the client reads. */
export function useClientProgress(clientId: string, weeks: 4 | 8 | 12) {
  const { i18n } = useTranslation()
  return useQuery({
    // the backend names exercises in the UI language, so the language is part of the key
    queryKey: ['progress', 'client', clientId, weeks, i18n.language],
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/clients/{id}/progress', {
        params: { path: { id: clientId }, query: { weeks } },
      })
      if (error !== undefined) throw error
      return data
    },
    // another period keeps the section as it is until its figures arrive
    placeholderData: keepPreviousData,
  })
}
