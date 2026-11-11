import {
  queryOptions,
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
  type InfiniteData,
  type QueryClient,
} from '@tanstack/react-query'
import { api } from '@/lib/api/client'
import { CONVERSATIONS, MESSAGES } from '@/lib/query-keys'
import type { components } from '@/lib/api/schema'
import { streamIsCurrent } from '../stream-health'

export type Conversation = components['schemas']['ConversationResponse']
export type Message = components['schemas']['MessageResponse']
/** The logged set a message is about, as its chip shows it. */
export type About = components['schemas']['AboutResponse']
type MessagePage = components['schemas']['MessagePageResponse']
export /** The most a message can hold, in UTF-16 units; the backend's `MessageText` holds the same limit. */
const MESSAGE_MAX_LENGTH = 2000

export const messagesKey = (clientId: string) => [...MESSAGES, clientId]

// fresh until an event or the stream's catch-up marks them otherwise, in a hidden tab too: asking again on coming back
// to the tab or the network, or when another screen shows them, would only repeat what the stream said
const KEPT_LIVE = { staleTime: Infinity }

export const conversationsQuery = queryOptions({
  queryKey: [...CONVERSATIONS, 'list'],
  queryFn: async () => {
    const { data, error } = await api.GET('/api/v1/conversations')
    // openapi-fetch returns errors instead of throwing; throw the problem detail so the query fails with its code
    if (error !== undefined) throw error
    return data
  },
  ...KEPT_LIVE,
})

export function useConversations() {
  return useQuery(conversationsQuery)
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
    ...KEPT_LIVE,
  })
}

export type LoadedMessages = InfiniteData<MessagePage, string | undefined>

/** Adds the messages not loaded yet after the newest one, in their order; one already loaded shows once. */
function withMessages(data: LoadedMessages, messages: Message[]) {
  const known = new Set(messagesOf(data).map((message) => message.id))
  const added = messages.filter((message) => !known.has(message.id))
  if (added.length === 0) return data
  const [newest, ...earlier] = data.pages
  return {
    ...data,
    pages: [
      { ...newest!, messages: [...newest!.messages, ...added] },
      ...earlier,
    ],
  }
}

/**
 * Brings a conversation this tab has loaded up to date after a new message in it. Earlier pages never change, so
 * only the newest page is fetched; a gap wider than one page, after a long time offline, loads everything again.
 */
export async function refreshConversation(
  queryClient: QueryClient,
  clientId: string,
) {
  const key = messagesKey(clientId)
  if (queryClient.getQueryData(key) === undefined) return
  // a refresh that left before this message was stored must not overwrite it when it lands
  await queryClient.cancelQueries({ queryKey: key })
  const { data: page, error } = await api.GET(
    '/api/v1/conversations/{clientId}/messages',
    { params: { path: { clientId } } },
  )
  // the next event, a reconnect or the tab coming back on screen brings it
  if (error !== undefined) return
  const data = queryClient.getQueryData<LoadedMessages>(key)
  if (data === undefined) return
  const known = new Set(messagesOf(data).map((message) => message.id))
  if (page.hasMore && !page.messages.some(({ id }) => known.has(id))) {
    void queryClient.invalidateQueries({ queryKey: key })
    return
  }
  queryClient.setQueryData(key, withMessages(data, page.messages))
}

export function useSendMessage() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({
      clientId,
      id,
      text,
      about,
    }: {
      clientId: string
      id: string
      text: string
      about?: About
    }) => {
      const { data, error } = await api.POST(
        '/api/v1/conversations/{clientId}/messages',
        {
          params: { path: { clientId } },
          body: { id, text, aboutSetLogId: about?.setLogId },
        },
      )
      if (error !== undefined) throw error
      return data
    },
    onSuccess: async (message, { clientId }) => {
      const key = messagesKey(clientId)
      // a refresh that left before this message was stored must not overwrite it when it lands
      await queryClient.cancelQueries({ queryKey: key })
      queryClient.setQueryData<LoadedMessages>(
        key,
        (data) => data && withMessages(data, [message]),
      )
      // nothing loaded to put it in: the load failed, and asking again brings the message
      if (queryClient.getQueryData(key) === undefined) {
        void queryClient.invalidateQueries({ queryKey: key })
      }
      // a live stream tells this tab too, and its event refreshes the list
      if (!streamIsCurrent()) {
        void queryClient.invalidateQueries({ queryKey: CONVERSATIONS })
      }
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
    // a live stream tells this tab too, and its event refreshes the list and the count
    onSuccess: () => {
      if (!streamIsCurrent()) {
        void queryClient.invalidateQueries({ queryKey: CONVERSATIONS })
      }
    },
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
    // the count is a nicety: a failure hides it and the next event or reconnect tries again
    retry: false,
    ...KEPT_LIVE,
  })
}
