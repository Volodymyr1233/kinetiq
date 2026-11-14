import { useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import { api } from '@/lib/api/client'
import type { paths } from '@/lib/api/schema'
import {
  APPROVALS,
  CONVERSATIONS,
  MESSAGES,
  NOTIFICATIONS,
} from '@/lib/query-keys'
import { refreshConversation } from './api/messaging'
import { streamHeard, streamLost } from './stream-health'

const EVENTS: keyof paths = '/api/v1/conversations/events'

// waits before each new attempt once the browser gave up on the stream; the last one repeats
const BACKOFF_MS = [1_000, 2_000, 4_000, 8_000, 16_000, 30_000]

// the server pings every 25 seconds; a proxy or a sleeping network can leave a dead stream looking open
const SILENCE_MS = 60_000

/**
 * Holds this tab's one stream of conversation events for the signed-in account, and keeps the list, the unread count
 * and any loaded conversation up to date with it. Without an account it holds none.
 */
export function useConversationEvents(accountId: string | undefined) {
  const queryClient = useQueryClient()

  useEffect(() => {
    if (accountId === undefined) return
    let source: EventSource | undefined
    let timer: ReturnType<typeof setTimeout> | undefined
    let watchdog: ReturnType<typeof setTimeout> | undefined
    let failures = 0
    let left = false

    const refreshList = () =>
      void queryClient.invalidateQueries({ queryKey: CONVERSATIONS })

    const connect = () => {
      const opened = new EventSource(EVENTS)
      source = opened
      const giveUp = () => {
        clearTimeout(watchdog)
        streamLost()
        opened.close()
        retryLater()
      }
      const watch = () => {
        clearTimeout(watchdog)
        watchdog = setTimeout(giveUp, SILENCE_MS)
      }
      const alive = () => {
        streamHeard()
        watch()
      }
      watch()
      // the first connect too: a message stored between a page's first load and now would be missed
      opened.addEventListener('open', () => {
        alive()
        failures = 0
        refreshList()
        void queryClient.invalidateQueries({ queryKey: MESSAGES })
      })
      opened.addEventListener('message', (event) => {
        alive()
        refreshList()
        void refreshConversation(queryClient, event.data)
      })
      opened.addEventListener('read', () => {
        alive()
        refreshList()
      })
      // a notification arrived: the bell reads its list again, and what Approvals counts may have changed with it
      opened.addEventListener('notification', () => {
        alive()
        void queryClient.invalidateQueries({ queryKey: NOTIFICATIONS })
        void queryClient.invalidateQueries({ queryKey: APPROVALS })
      })
      opened.addEventListener('ping', alive)
      opened.addEventListener('error', () => {
        // while connecting again the browser retries by itself; closed means it gave up
        if (opened.readyState === EventSource.CLOSED) giveUp()
      })
    }

    const retryLater = () => {
      const delay = BACKOFF_MS[Math.min(failures, BACKOFF_MS.length - 1)]
      failures += 1
      timer = setTimeout(() => void probe(), delay)
    }

    // the browser does not say why it gave up: a signed-out answer here sends the person to sign-in, as any call does
    const probe = async () => {
      let status = 0
      try {
        ;({
          response: { status },
        } = await api.GET('/api/v1/conversations/unread-count'))
      } catch {
        // not reachable yet
      }
      if (left || status === 401) return
      if (status >= 200 && status < 300) connect()
      else retryLater()
    }

    connect()
    return () => {
      left = true
      clearTimeout(timer)
      clearTimeout(watchdog)
      streamLost()
      source?.close()
    }
  }, [accountId, queryClient])
}
