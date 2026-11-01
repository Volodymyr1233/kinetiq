type Guard = () => Promise<boolean>

let guard: Guard | undefined

/** Registers the question to ask before an action that leaves the page; returns the way to take it back. */
export function setLeaveGuard(next: Guard) {
  guard = next
  return () => {
    if (guard === next) guard = undefined
  }
}

/**
 * Asks whoever has unsaved work whether to leave, and answers yes when nobody has. For actions a navigation blocker
 * would catch too late: signing out ends the sign-in first and navigates afterwards.
 */
export function confirmLeaving() {
  return guard ? guard() : Promise.resolve(true)
}
