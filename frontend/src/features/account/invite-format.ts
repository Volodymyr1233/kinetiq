const DAY_MS = 24 * 60 * 60 * 1000

/** Whole days left, rounded up, so an invite reads "In 1 day" throughout its last day. */
export function daysLeft(expiresAt: string, now: Date) {
  return Math.max(
    1,
    Math.ceil((Date.parse(expiresAt) - now.getTime()) / DAY_MS),
  )
}

/** The day an invite was sent in the browser's time zone, or `undefined` for today. */
export function sentDay(sentAt: string, now: Date, locale: string) {
  const sent = new Date(sentAt)
  if (sent.toDateString() === now.toDateString()) return undefined
  return new Intl.DateTimeFormat(locale, {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
  }).format(sent)
}
