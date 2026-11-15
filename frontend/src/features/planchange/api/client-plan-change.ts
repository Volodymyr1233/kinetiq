import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '@/lib/api/client'
import { problemStatus } from '@/lib/api/problem'
import type { components } from '@/lib/api/schema'
import { CONVERSATIONS, MESSAGES } from '@/lib/query-keys'

export type ClientPlanChange =
  components['schemas']['ClientPlanChangeDetailResponse']
export type DeclineReason =
  components['schemas']['KeepCurrentPlanRequest']['reason']

/** The reasons in the order the form offers them. */
export const DECLINE_REASONS = [
  'TOO_EASY',
  'TOO_HARD',
  'SCHEDULE',
  'INJURY',
  'OTHER',
] as const satisfies readonly DeclineReason[]

// plan changes come in the account's language, which the UI follows, so the language is part of the key
const planChangeKey = (id: string, language: string) => [
  'plan',
  'changes',
  id,
  language,
]
// the open, the upcoming and the applied plan change all follow a decision
const CURRENT_KEY = ['plan', 'change']

export function useClientPlanChange(id: string | undefined) {
  const { i18n } = useTranslation()
  return useQuery({
    queryKey: planChangeKey(id ?? '', i18n.language),
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/plan/changes/{id}', {
        params: { path: { id: id! } },
      })
      // openapi-fetch returns errors instead of throwing; throw the problem detail so the query fails with its code
      if (error !== undefined) throw error
      return data
    },
    enabled: id !== undefined,
  })
}

// the answer is the plan change as it stands now, so it replaces what the screen holds
function useDecision<Variables extends { id: string }>(
  send: (variables: Variables) => Promise<ClientPlanChange>,
  { posts }: { posts?: boolean } = {},
) {
  const queryClient = useQueryClient()
  const { i18n } = useTranslation()
  return useMutation({
    mutationFn: send,
    // a refusal says the state moved on (the other person decided meanwhile), so the screen asks again
    onError: (error, { id }) => {
      if (problemStatus(error) === 409) {
        void queryClient.invalidateQueries({
          queryKey: planChangeKey(id, i18n.language),
        })
      }
    },
    onSuccess: (planChange, { id }) => {
      queryClient.setQueryData(planChangeKey(id, i18n.language), planChange)
      void queryClient.invalidateQueries({ queryKey: CURRENT_KEY })
      if (posts) {
        // the decline left a message in the conversation
        void queryClient.invalidateQueries({ queryKey: CONVERSATIONS })
        void queryClient.invalidateQueries({ queryKey: MESSAGES })
      }
    },
  })
}

export function useAcceptPlanChange() {
  return useDecision(async ({ id }: { id: string }) => {
    const { data, error } = await api.POST(
      '/api/v1/plan/changes/{id}/acceptance',
      { params: { path: { id } } },
    )
    if (error !== undefined) throw error
    return data
  })
}

export function useUndoPlanChange() {
  return useDecision(async ({ id }: { id: string }) => {
    const { data, error } = await api.DELETE(
      '/api/v1/plan/changes/{id}/acceptance',
      { params: { path: { id } } },
    )
    if (error !== undefined) throw error
    return data
  })
}

export function useKeepCurrentPlan() {
  return useDecision(
    async ({
      id,
      reason,
      note,
    }: {
      id: string
      reason: DeclineReason
      note?: string
    }) => {
      const { data, error } = await api.POST(
        '/api/v1/plan/changes/{id}/declination',
        { params: { path: { id } }, body: { reason, note } },
      )
      if (error !== undefined) throw error
      return data
    },
    { posts: true },
  )
}
