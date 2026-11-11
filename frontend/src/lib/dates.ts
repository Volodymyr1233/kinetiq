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
