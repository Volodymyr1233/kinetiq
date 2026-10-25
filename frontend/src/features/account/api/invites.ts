import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api/client'
import type { components } from '@/lib/api/schema'
import { receive } from './account'

export type Invite = components['schemas']['InviteResponse']

const INVITES = ['invites']

export function useInvites() {
  return useQuery({
    queryKey: INVITES,
    queryFn: () => receive(api.GET('/api/v1/invites')),
  })
}

// every change refreshes the list, also a failed one: a 404 means the list is out of date
function useInviteMutation<V, D>(mutationFn: (variables: V) => Promise<D>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSettled: () => queryClient.invalidateQueries({ queryKey: INVITES }),
  })
}

export function useCreateInvite() {
  return useInviteMutation((email: string) =>
    receive(api.POST('/api/v1/invites', { body: { email } })),
  )
}

export function useReplaceInviteLink() {
  return useInviteMutation(async (id: string) => {
    const { link } = await receive(
      api.POST('/api/v1/invites/{id}/links', { params: { path: { id } } }),
    )
    return link
  })
}

export function useRevokeInvite() {
  return useInviteMutation(async (id: string) => {
    const { error } = await api.DELETE('/api/v1/invites/{id}', {
      params: { path: { id } },
    })
    if (error !== undefined) throw error
  })
}
