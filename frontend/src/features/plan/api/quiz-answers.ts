import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api/client'
import { problemCode } from '@/lib/api/problem'
import type { components } from '@/lib/api/schema'

export type QuizAnswers = components['schemas']['QuizAnswersResponse']

/** The client's quiz answers; `null` when this own client has none stored. */
export function useQuizAnswers(client: string) {
  return useQuery({
    queryKey: ['quiz-answers', client],
    queryFn: async () => {
      const { data, error } = await api.GET(
        '/api/v1/clients/{id}/quiz-answers',
        { params: { path: { id: client } } },
      )
      if (error !== undefined) {
        if (problemCode(error) === 'quiz.not-found') return null
        throw error
      }
      return data
    },
    // the answers never change once submitted
    staleTime: Infinity,
  })
}
