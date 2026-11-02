import {
  queryOptions,
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
  type InfiniteData,
} from '@tanstack/react-query'
import { api } from '@/lib/api/client'
import type { components } from '@/lib/api/schema'

export type Conversation = components['schemas']['ConversationResponse']
export type Message = components['schemas']['MessageResponse']
type MessagePage = components['schemas']['MessagePageResponse']

/** The most a message can hold, in UTF-16 units; the backend's `MessageText` holds the same limit. */
export const MESSAGE_MAX_LENGTH = 2000

// within the 15 seconds of SC-003; the query library pauses these in a hidden tab, so nothing is fetched or read there
const OPEN_SCREEN_REFRESH_MS = 5_000
const NAVIGATION_REFRESH_MS = 10_000

// the list and the count share a root, because everything that changes one changes the other
const CONVERSATIONS = ['conversations']
const messagesKey = (clientId: string) => ['messages', clientId]

export const conversationsQuery = queryOptions({
  queryKey: [...CONVERSATIONS, 'list'],
  queryFn: async () => {
    const { data, error } = await api.GET('/api/v1/conversations')
    // openapi-fetch returns errors instead of throwing; throw the problem detail so the query fails with its code
    if (error !== undefined) throw error
    return data
  },
})

export function useConversations() {
  return useQuery({
    ...conversationsQuery,
    refetchInterval: OPEN_SCREEN_REFRESH_MS,
  })
}

/** Returns the messages of the loaded pages, oldest to newest: the newest page comes first in the cache. */
export function messagesOf(data: { pages: MessagePage[] } | undefined) {
  return [...(data?.pages ?? [])].reverse().flatMap((page) => page.messages)
}

export function useMessages(clientId: string) {
  return useInfiniteQuery({
    queryKey: messagesKey(clientId),
    queryFn: async ({ pageParam }) => {
      const { data, error } = await api.GET(
        '/api/v1/conversations/{clientId}/messages',
        { params: { path: { clientId }, query: { before: pageParam } } },
      )
      if (error !== undefined) throw error
      return data
    },
    initialPageParam: undefined as string | undefined,
    // the first page is the newest, so the "next" page is the one further back in time
    getNextPageParam: (page) =>
      page.hasMore ? page.messages[0]?.id : undefined,
    // ponytail: every refresh asks for all loaded pages (research R7); ask for the newest page only if thousands get loaded
    refetchInterval: OPEN_SCREEN_REFRESH_MS,
  })
}

function withMessage(
  data: InfiniteData<MessagePage, string | undefined>,
  message: Message,
) {
  const known = data.pages.some((page) =>
    page.messages.some((other) => other.id === message.id),
  )
  if (known) return data
  const [newest, ...earlier] = data.pages
  return {
    ...data,
    pages: [
      { ...newest!, messages: [...newest!.messages, message] },
      ...earlier,
    ],
  }
}

export function useSendMessage() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({
      clientId,
      id,
      text,
    }: {
      clientId: string
      id: string
      text: string
    }) => {
      const { data, error } = await api.POST(
        '/api/v1/conversations/{clientId}/messages',
        { params: { path: { clientId } }, body: { id, text } },
      )
      if (error !== undefined) throw error
      return data
    },
    onSuccess: async (message, { clientId }) => {
      const key = messagesKey(clientId)
      // a refresh that left before this message was stored must not overwrite it when it lands
      await queryClient.cancelQueries({ queryKey: key })
      queryClient.setQueryData<InfiniteData<MessagePage, string | undefined>>(
        key,
        (data) => data && withMessage(data, message),
      )
      // nothing loaded to put it in: the load failed, and asking again brings the message
      if (queryClient.getQueryData(key) === undefined) {
        void queryClient.invalidateQueries({ queryKey: key })
      }
      void queryClient.invalidateQueries({ queryKey: CONVERSATIONS })
    },
  })
}

export function useMarkRead() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (clientId: string) => {
      const { error } = await api.PUT('/api/v1/conversations/{clientId}/read', {
        params: { path: { clientId } },
      })
      if (error !== undefined) throw error
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CONVERSATIONS }),
  })
}

export function useUnreadCount() {
  return useQuery({
    queryKey: [...CONVERSATIONS, 'unread-count'],
    queryFn: async () => {
      const { data, error } = await api.GET(
        '/api/v1/conversations/unread-count',
      )
      if (error !== undefined) throw error
      return data
    },
    refetchInterval: NAVIGATION_REFRESH_MS,
    // the count is a nicety: a failure hides it and the next refresh tries again
    retry: false,
  })
}
