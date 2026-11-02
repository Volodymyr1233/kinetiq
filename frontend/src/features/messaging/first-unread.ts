import type { Message } from './api/messaging'

/**
 * Finds the first of the last `unread` messages from the other person. `complete` is false while earlier pages are
 * still needed to reach it; `id` is then the earliest one loaded.
 */
export function firstUnread(messages: Message[], unread: number) {
  const theirs = messages.filter((message) => !message.mine)
  return {
    id:
      unread > 0 ? theirs[Math.max(theirs.length - unread, 0)]?.id : undefined,
    complete: theirs.length >= unread,
  }
}
