/** Every cached session log, today's and earlier ones; readiness refreshes them when today's targets change. */
export const SESSION_LOGS_KEY = ['session-log']

/** The conversation list and the unread count share a root, because everything that changes one changes the other. */
export const CONVERSATIONS = ['conversations']

/** Every loaded conversation; a message posted by another feature, such as a plan change decline, refreshes them. */
export const MESSAGES = ['messages']

/** Everything the Approvals list and its count hold: drafted plans and plan changes. */
export const APPROVALS = ['approvals']

/** The signed-in person's notifications and their unread count; the event stream refreshes them. */
export const NOTIFICATIONS = ['notifications']
