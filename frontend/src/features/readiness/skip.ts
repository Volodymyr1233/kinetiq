// the client's day from the server, so a skip ends when their day does; nothing of it is stored on the server
const KEY = 'kinetiq.checkIn.skippedOn'

/** Tells whether the client skipped the check-in of `date` on this device. */
export function isSkipped(date: string): boolean {
  try {
    return localStorage.getItem(KEY) === date
  } catch {
    return false
  }
}

/** Remembers on this device that the check-in of `date` was skipped. */
export function skip(date: string) {
  try {
    localStorage.setItem(KEY, date)
  } catch {
    // without storage the skip lasts only while the page is open
  }
}

/** Forgets a skip, so the check-in is offered again. */
export function unskip() {
  try {
    localStorage.removeItem(KEY)
  } catch {
    // nothing was stored
  }
}
