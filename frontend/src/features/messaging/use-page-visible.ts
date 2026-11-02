import { useSyncExternalStore } from 'react'

function subscribe(onChange: () => void) {
  document.addEventListener('visibilitychange', onChange)
  return () => document.removeEventListener('visibilitychange', onChange)
}

/** Tells whether the browser tab is on screen. */
export function usePageVisible() {
  return useSyncExternalStore(
    subscribe,
    () => document.visibilityState === 'visible',
  )
}
