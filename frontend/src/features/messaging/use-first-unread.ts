import { useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import {
  conversationsQuery,
  messagesOf,
  useMarkRead,
  type useMessages,
} from './api/messaging'
import { firstUnread } from './first-unread'
import { usePageVisible } from './use-page-visible'

/**
 * Settles where an open conversation starts reading, then marks it read, because reading it moves the count to 0.
 * This runs when the conversation opens and when its tab comes back on screen, with everything fetched anew and earlier
 * pages added until the first unread message is loaded. A hidden tab waits, so what arrived there stays unread.
 */
export function useFirstUnread(
  clientId: string,
  {
    data: messages,
    refetch: refetchMessages,
    fetchNextPage,
  }: ReturnType<typeof useMessages>,
) {
  const visible = usePageVisible()
  const { mutate: markRead } = useMarkRead()
  // the page's own observer refreshes the list; a second interval here would double its requests
  const {
    data: list,
    dataUpdatedAt,
    refetch: refetchList,
  } = useQuery(conversationsQuery)
  const [firstUnreadId, setFirstUnreadId] = useState<string>()
  const [settled, setSettled] = useState(false)
  const [opened, setOpened] = useState(false)
  const unread =
    list?.conversations.find((row) => row.clientId === clientId)?.unreadCount ??
    0

  useEffect(() => {
    if (!visible) return
    let left = false
    const settle = async () => {
      const [freshList, freshMessages] = await Promise.all([
        refetchList(),
        refetchMessages(),
      ])
      const count =
        freshList.data?.conversations.find((row) => row.clientId === clientId)
          ?.unreadCount ?? 0
      let { data, isError } = freshMessages
      while (
        !left &&
        !isError &&
        data?.pages[data.pages.length - 1]?.hasMore &&
        !firstUnread(messagesOf(data), count).complete
      ) {
        ;({ data, isError } = await fetchNextPage())
      }
      if (left) return
      // a count of 0 keeps an earlier separator: what it marked is still what the reader last came back to
      const { id } = firstUnread(messagesOf(data), count)
      if (id !== undefined) setFirstUnreadId(id)
      setSettled(true)
      setOpened(true)
    }
    void settle()
    return () => {
      left = true
      setSettled(false)
    }
  }, [visible, clientId, refetchList, refetchMessages, fetchNextPage])

  // every refresh counts, not only a changed number: a new message can leave the same count, and a failed mark needs a retry
  useEffect(() => {
    if (settled && messages !== undefined && visible && unread > 0) {
      markRead(clientId)
    }
  }, [settled, messages, visible, unread, clientId, markRead, dataUpdatedAt])

  return { firstUnreadId, positioning: visible && !opened }
}
