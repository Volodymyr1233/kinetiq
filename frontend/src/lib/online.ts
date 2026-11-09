import { onlineManager } from '@tanstack/react-query'
import { useSyncExternalStore } from 'react'

/** Whether the browser reports a connection: the same signal that makes a log wait, so a banner never disagrees. */
export function useOnline() {
  return useSyncExternalStore(onlineManager.subscribe, () =>
    onlineManager.isOnline(),
  )
}
