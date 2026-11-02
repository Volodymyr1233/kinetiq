import { useEffect } from 'react'
import { useMarkRead } from './api/messaging'
import { usePageVisible } from './use-page-visible'

/** Marks an open conversation read whenever it has unread messages and its tab is on screen; a hidden tab waits. */
export function useMarkReadWhileVisible(clientId: string, unread: number) {
  const visible = usePageVisible()
  const { mutate: markRead } = useMarkRead()

  useEffect(() => {
    if (visible && unread > 0) markRead(clientId)
  }, [visible, unread, clientId, markRead])
}
