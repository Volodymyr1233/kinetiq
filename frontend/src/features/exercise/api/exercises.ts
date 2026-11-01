import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api/client'
import type { components } from '@/lib/api/schema'
import type { Exercise } from '@/lib/exercise-library'

type CreateBody = components['schemas']['CreateExerciseRequest']

// every save refreshes the list, also a failed one: a 404 means the list is out of date
function useExerciseMutation<V>(save: (variables: V) => Promise<Exercise>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: save,
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['exercises'] }),
  })
}

export function useCreateExercise() {
  return useExerciseMutation(async (body: CreateBody) => {
    const { data, error } = await api.POST('/api/v1/exercises', { body })
    if (error !== undefined) throw error
    return data
  })
}

export function useUpdateExercise() {
  return useExerciseMutation(
    async ({
      id,
      body,
    }: {
      id: string
      body: components['schemas']['UpdateExerciseRequest']
    }) => {
      const { data, error } = await api.PUT('/api/v1/exercises/{id}', {
        params: { path: { id } },
        body,
      })
      if (error !== undefined) throw error
      return data
    },
  )
}
