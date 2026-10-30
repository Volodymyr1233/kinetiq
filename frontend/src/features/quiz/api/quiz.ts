import {
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { api } from '@/lib/api/client'
import { problemCode } from '@/lib/api/problem'
import type { components } from '@/lib/api/schema'
import { parseWeight, toKg, useWeightUnit } from '@/lib/weight'
import { clearDraft } from '../quiz-draft'
import {
  answersSchemaFor,
  parseWholeNumber,
  type QuizDraft,
} from '../quiz-schema'

export type QuizStatus = components['schemas']['QuizStatusResponse']['status']

export const quizStatusQuery = queryOptions({
  queryKey: ['quiz-status'],
  queryFn: async () => {
    const { data, error } = await api.GET('/api/v1/quiz')
    // openapi-fetch returns errors instead of throwing; throw the problem detail so the query fails with its code
    if (error !== undefined) throw error
    return data.status
  },
  // the quiz pages wait on it, so a failure shows "Try again" at once instead of after several retries
  retry: false,
})

export function useQuizStatus() {
  return useQuery(quizStatusQuery)
}

/** Sends the draft's answers once; afterwards the status is the waiting one and the draft is gone. */
export function useSubmitQuiz() {
  const queryClient = useQueryClient()
  const unit = useWeightUnit()
  return useMutation({
    mutationFn: async (draft: QuizDraft) => {
      // the review submits only a complete draft; parsing gives its answers their checked types
      const answers = answersSchemaFor(unit).parse(draft.answers)
      const { data, error } = await api.POST('/api/v1/quiz', {
        body: {
          goal: answers.goal,
          experience: answers.experience,
          level: answers.level,
          age: parseWholeNumber(answers.age),
          heightCm: parseWholeNumber(answers.height),
          // the draft holds the weight in the person's unit (loadDraft); the server stores kilograms
          weightKg: toKg(parseWeight(answers.weight)!, unit),
          likes: answers.likes,
          daysPerWeek: answers.days,
          notes: answers.notes,
        },
      })
      if (error !== undefined) throw error
      return data.status
    },
    onSuccess: (status) => {
      clearDraft()
      queryClient.setQueryData(quizStatusQuery.queryKey, status)
    },
    onError: (error) => {
      // the answers are stored already, from another tab or an earlier request
      if (problemCode(error) === 'quiz.already-submitted') {
        clearDraft()
        void queryClient.invalidateQueries({
          queryKey: quizStatusQuery.queryKey,
        })
      }
    },
  })
}
