/** A `yyyy-mm-dd` date from the backend as a local date, so no time zone moves it to another day. */
export function localDate(iso: string): Date {
  const [year, month, day] = iso.split('-').map(Number)
  return new Date(year, month - 1, day)
}

/** A day such as "Mon, 16 Nov" in the given locale. */
export function formatDay(iso: string, locale: string): string {
  return new Intl.DateTimeFormat(locale, {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
  }).format(localDate(iso))
}

/** The day an instant such as `2026-10-14T23:30:00Z` falls on in the viewer's time zone, as {@link formatDay} writes it. */
export function formatInstantDay(instant: string, locale: string): string {
  return new Intl.DateTimeFormat(locale, {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
  }).format(new Date(instant))
}

/** A day of a week as a short date such as "26 Oct", for chart axes. */
export function formatShortDate(iso: string, locale: string): string {
  return new Intl.DateTimeFormat(locale, {
    day: 'numeric',
    month: 'short',
  }).format(localDate(iso))
}

const DAYS = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
] as const

export type Weekday = (typeof DAYS)[number]

/** The name of a weekday, such as "Tuesday", in the given locale. */
export function weekdayName(day: Weekday, locale: string): string {
  // 2024-01-01 was a Monday
  return new Intl.DateTimeFormat(locale, { weekday: 'long' }).format(
    new Date(2024, 0, 1 + DAYS.indexOf(day)),
  )
}

const MINUTE_MS = 60 * 1000
const MINUTES_PER_HOUR = 60
const MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR

/**
 * How long ago an instant was, such as "5 minutes ago", "2 hours ago" or "Yesterday", in the given locale and sentence
 * case: minutes within an hour, hours within a day, then calendar days in the viewer's time zone.
 */
export function timeAgo(instant: string, now: Date, locale: string): string {
  const text = relative(instant, now, locale)
  return text.charAt(0).toLocaleUpperCase(locale) + text.slice(1)
}

function relative(instant: string, now: Date, locale: string): string {
  const then = new Date(instant)
  const format = new Intl.RelativeTimeFormat(locale, { numeric: 'auto' })
  const minutes = Math.max(
    0,
    Math.floor((now.getTime() - then.getTime()) / MINUTE_MS),
  )
  // "now" rather than "this minute"
  if (minutes === 0) return format.format(0, 'second')
  if (minutes < MINUTES_PER_HOUR) return format.format(-minutes, 'minute')
  if (minutes < MINUTES_PER_DAY) {
    return format.format(-Math.floor(minutes / MINUTES_PER_HOUR), 'hour')
  }
  const day = (date: Date) =>
    new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
  // rounded because a day can have 23 or 25 hours
  return format.format(
    -Math.round((day(now) - day(then)) / (MINUTES_PER_DAY * MINUTE_MS)),
    'day',
  )
}
