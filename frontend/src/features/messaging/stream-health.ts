// the server pings every 25 seconds, so a stream heard from within this time missed nothing
const CURRENT_MS = 35_000

let heardAt = 0

/** Notes that this tab's stream of conversation events just delivered something. */
export function streamHeard() {
  heardAt = Date.now()
}

/** Notes that this tab has no stream, or one that went quiet. */
export function streamLost() {
  heardAt = 0
}

/**
 * Whether this tab's stream is live and so keeps conversations current: then its events refresh what is shown, and
 * nothing needs to be asked again. A tab a phone froze, or a dropped connection, has not been heard from lately.
 */
export function streamIsCurrent() {
  return Date.now() - heardAt < CURRENT_MS
}
