import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '@/lib/api/client'
import type { components } from '@/lib/api/schema'

export type Exercise = components['schemas']['ExerciseResponse']
export type Category = Exercise['category']

// a Record, so the type check fails when a category from the backend is missing here
const CATEGORY_ORDER = {
  CHEST: true,
  BACK: true,
  LEGS: true,
  SHOULDERS: true,
  ARMS: true,
  CORE: true,
  FULL_BODY: true,
  CARDIO: true,
  MOBILITY: true,
} satisfies Record<Category, true>

export const CATEGORIES = Object.keys(CATEGORY_ORDER) as Category[]

// the backend answers in the UI language, so the language is part of the key
export function useExercises({
  category,
  name,
}: {
  category?: Category
  name: string
}) {
  const { i18n } = useTranslation()
  const trimmed = name.trim()
  return useQuery({
    queryKey: ['exercises', i18n.language, category, trimmed],
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/exercises', {
        params: { query: { category, name: trimmed || undefined } },
      })
      // openapi-fetch returns errors instead of throwing; throw the problem detail so the query fails with its code
      if (error !== undefined) throw error
      return data
    },
    // the old list stays on screen while the next one loads
    placeholderData: keepPreviousData,
  })
}
