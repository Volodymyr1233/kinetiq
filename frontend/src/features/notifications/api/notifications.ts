import {
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import { api } from '@/lib/api/client'
import type { components } from '@/lib/api/schema'
import { NOTIFICATIONS } from '@/lib/query-keys'

export type Notification = components['schemas']['NotificationResponse']

// fresh until the event stream says a notification arrived, so no screen asks again on its own
export const notificationsQuery = queryOptions({
  queryKey: NOTIFICATIONS,
  queryFn: async () => {
    const { data, error } = await api.GET('/api/v1/notifications')
    // openapi-fetch returns errors instead of throwing; throw the problem detail so the query fails with its code
    if (error !== undefined) throw error
    return data
  },
  staleTime: Infinity,
})

/** The signed-in person's latest notifications and their unread count. */
export function useNotifications() {
  return useQuery(notificationsQuery)
}

/** Marks one notification read and reads the list again. */
export function useMarkNotificationRead() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (id: string) => {
      const { error } = await api.POST('/api/v1/notifications/{id}/read', {
        params: { path: { id } },
      })
      if (error !== undefined) throw error
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: NOTIFICATIONS }),
  })
}
