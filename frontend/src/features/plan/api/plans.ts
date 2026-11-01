import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
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

// the client's status and goal and the library's plan counts follow from what was saved
function useRefreshAfterSaving(client: string) {
  const queryClient = useQueryClient()
  return (plan: Plan) => {
    queryClient.setQueryData(planKey(client), plan)
    void queryClient.invalidateQueries({ queryKey: ['clients'] })
    void queryClient.invalidateQueries({ queryKey: ['exercises'] })
  }
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
