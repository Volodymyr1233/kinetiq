import { useEffect } from 'react'
import { useConversations, useMarkRead } from './api/messaging'
import { usePageVisible } from './use-page-visible'

/**
 * Marks an open conversation read whenever a refresh of the list shows it has unread messages and its tab is on screen.
 * A hidden tab waits, and so does a conversation whose messages are not `shown` yet.
 */
export function useMarkReadWhileVisible(clientId: string, shown: boolean) {
  const visible = usePageVisible()
  const { mutate: markRead } = useMarkRead()
  const { data, dataUpdatedAt } = useConversations()
  const unread =
    data?.conversations.find((row) => row.clientId === clientId)?.unreadCount ??
    0

  // every refresh counts, not only a changed number: a new message can leave the same count, and a failed mark needs a retry
  useEffect(() => {
    if (shown && visible && unread > 0) markRead(clientId)
  }, [shown, visible, unread, clientId, markRead, dataUpdatedAt])
}
