import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import { api } from '@/lib/api/client'
import { problemCode } from '@/lib/api/problem'
import type { components } from '@/lib/api/schema'

export type Plan = components['schemas']['PlanResponse']
export type SavePlanBody = components['schemas']['SavePlanRequest']

const planKey = (client: string) => ['plans', client]

/** The client's plan; `null` when this own client has none. Not asked for while `enabled` is false. */
export function usePlan(client: string, enabled: boolean) {
  return useQuery({
    queryKey: planKey(client),
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/clients/{id}/plan', {
        params: { path: { id: client } },
      })
      if (error !== undefined) {
        if (problemCode(error) === 'plan.not-found') return null
        throw error
      }
      return data
    },
    enabled,
    // the editor copies the plan once, so opening it must never start from an old copy and then be stale
    gcTime: 0,
  })
}

/** While mounted, asks every 5 seconds whether drafting has ended, skipping a hidden tab; the client's state moves the tab on. */
export function useFollowDrafting(client: string) {
  const queryClient = useQueryClient()
  useEffect(() => {
    const timer = setInterval(() => {
      if (!document.hidden) {
        void queryClient.invalidateQueries({ queryKey: ['clients', client] })
      }
    }, 5000)
    return () => clearInterval(timer)
  }, [queryClient, client])
}

// the client's status and goal, the library's plan counts and the drafted plans follow from what was saved
function useRefreshAfterSaving(client: string) {
  const queryClient = useQueryClient()
  return (plan: Plan) => {
    queryClient.setQueryData(planKey(client), plan)
    void queryClient.invalidateQueries({ queryKey: ['clients'] })
    void queryClient.invalidateQueries({ queryKey: ['exercises'] })
    void queryClient.invalidateQueries({ queryKey: ['approvals'] })
  }
}

export function usePublishPlan(client: string) {
  const refresh = useRefreshAfterSaving(client)
  return useMutation({
    mutationFn: async (version: number) => {
      const { data, error } = await api.POST(
        '/api/v1/clients/{id}/plan/publication',
        { params: { path: { id: client } }, body: { version } },
      )
      if (error !== undefined) throw error
      return data
    },
    onSuccess: refresh,
  })
}

export function useSavePlan(client: string) {
  const refresh = useRefreshAfterSaving(client)
  return useMutation({
    mutationFn: async (body: SavePlanBody) => {
      const { data, error } = await api.PUT('/api/v1/clients/{id}/plan', {
        params: { path: { id: client } },
        body,
      })
      if (error !== undefined) throw error
      return data
    },
    onSuccess: refresh,
  })
}

/** Asks for a new automatic draft; the client's state then says drafting runs, so the tab follows it. */
export function useBuildPlanAgain(client: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async () => {
      const { error } = await api.POST('/api/v1/clients/{id}/plan/drafting', {
        params: { path: { id: client } },
      })
      if (error !== undefined) throw error
    },
    // awaited, so the caller's onSuccess sees the client's new state; a refusal for a drafting already running moves
    // the tab to it, too
    onSettled: (_, error) =>
      !error || problemCode(error) === 'plan.drafting'
        ? queryClient.invalidateQueries({ queryKey: ['clients'] })
        : undefined,
  })
}
