import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api/client'
import { problemStatus } from '@/lib/api/problem'
import type { components } from '@/lib/api/schema'
import { APPROVALS, CONVERSATIONS, MESSAGES } from '@/lib/query-keys'
import type { DeclineReason } from './client-plan-change'

export type PlanChange = components['schemas']['PlanChangeResponse']
export type PlanChangeTarget = components['schemas']['PlanChangeTargetResponse']

const planChangeKey = (id: string) => ['plan-changes', id]

export function usePlanChange(id: string | undefined) {
  return useQuery({
    queryKey: planChangeKey(id ?? ''),
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/plan-changes/{id}', {
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
  send: (variables: Variables) => Promise<PlanChange>,
  { posts }: { posts?: boolean } = {},
) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: send,
    // a refusal says the state moved on (the other person decided meanwhile), so the screen asks again
    onError: (error, { id }) => {
      if (problemStatus(error) === 409) {
        void queryClient.invalidateQueries({ queryKey: planChangeKey(id) })
      }
    },
    onSuccess: (planChange, { id }) => {
      queryClient.setQueryData(planChangeKey(id), planChange)
      // the lists, the nav count, the client's page and its open plan change all follow the state
      void queryClient.invalidateQueries({ queryKey: APPROVALS })
      void queryClient.invalidateQueries({
        queryKey: ['clients', planChange.clientId],
      })
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
      '/api/v1/plan-changes/{id}/acceptance',
      { params: { path: { id } } },
    )
    if (error !== undefined) throw error
    return data
  })
}

export function useUndoPlanChange() {
  return useDecision(async ({ id }: { id: string }) => {
    const { data, error } = await api.DELETE(
      '/api/v1/plan-changes/{id}/acceptance',
      { params: { path: { id } } },
    )
    if (error !== undefined) throw error
    return data
  })
}

export function useKeepCurrentPlanChange() {
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
        '/api/v1/plan-changes/{id}/declination',
        { params: { path: { id } }, body: { reason, note } },
      )
      if (error !== undefined) throw error
      return data
    },
    { posts: true },
  )
}
