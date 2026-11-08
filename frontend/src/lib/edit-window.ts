// the backend's rule (session-log.locked): screens follow it, and the server still decides
const EDIT_WINDOW_MS = 24 * 60 * 60 * 1000

/** The moment, in milliseconds, from which the sets of a session that started at `startedAt` can no longer change. */
export function locksAt(startedAt: string): number {
  return Date.parse(startedAt) + EDIT_WINDOW_MS
}
