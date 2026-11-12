import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api/client'
import { problemCode } from '@/lib/api/problem'
import type { components } from '@/lib/api/schema'

export type Client = components['schemas']['ClientResponse']

export function useClient(id: string | undefined) {
  return useQuery({
    queryKey: ['clients', id],
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/clients/{id}', {
        params: { path: { id: id! } },
      })
      if (error !== undefined) throw error
      return data
    },
    enabled: id !== undefined,
  })
}

export type OpenPlanChange = components['schemas']['ClientPlanChangeResponse']

/** The client's open plan change, or `null` when there is none. */
export function useOpenPlanChange(id: string | undefined) {
  return useQuery({
    queryKey: ['clients', id, 'plan-change'],
    queryFn: async (): Promise<OpenPlanChange | null> => {
      const { data, error } = await api.GET(
        '/api/v1/clients/{id}/plan-change',
        { params: { path: { id: id! } } },
      )
      if (problemCode(error) === 'plan-change.not-found') return null
      if (error !== undefined) throw error
      return data
    },
    enabled: id !== undefined,
  })
}

// another trainer's client and an unknown one are both 404; text that is no id at all is a 400
export function isNotFound(error: unknown) {
  const status =
    typeof error === 'object' && error !== null && 'status' in error
      ? error.status
      : undefined
  return status === 404 || status === 400
}

export type EndedPlanChange =
  components['schemas']['PlanChangeApprovalResponse']

/** The last 5 plan changes of the client that ended, the latest first. */
export function useEndedPlanChanges(id: string | undefined) {
  return useQuery({
    queryKey: ['clients', id, 'plan-changes', 'ended'],
    queryFn: async () => {
      const { data, error } = await api.GET(
        '/api/v1/clients/{id}/plan-changes/ended',
        { params: { path: { id: id! } } },
      )
      if (error !== undefined) throw error
      return data.items
    },
    enabled: id !== undefined,
  })
}
