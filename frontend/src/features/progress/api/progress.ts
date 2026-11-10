import { keepPreviousData, useQuery } from '@tanstack/react-query'
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
  return useQuery({
    queryKey: ['progress', weeks],
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
