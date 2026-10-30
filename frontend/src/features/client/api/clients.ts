import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api/client'
import type { components } from '@/lib/api/schema'

export type Client = components['schemas']['ClientResponse']

export function useClients() {
  return useQuery({
    queryKey: ['clients'],
    queryFn: async () => {
      const { data, error } = await api.GET('/api/v1/clients')
      // openapi-fetch returns errors instead of throwing; throw the problem detail so the query fails with its code
      if (error !== undefined) throw error
      return data
    },
  })
}

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

// another trainer's client and an unknown one are both 404; text that is no id at all is a 400
export function isNotFound(error: unknown) {
  const status =
    typeof error === 'object' && error !== null && 'status' in error
      ? error.status
      : undefined
  return status === 404 || status === 400
}
