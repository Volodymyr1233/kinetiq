import type { TFunction } from 'i18next'

const DAY_MS = 24 * 60 * 60 * 1000
const WEEK_DAYS = 7

function daysBetween(earlier: Date, later: Date) {
  // calendar days in the viewer's time zone; rounded because a day can have 23 or 25 hours
  const start = (date: Date) =>
    new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
  return Math.round((start(later) - start(earlier)) / DAY_MS)
}

/** Tells whether two instants fall on one calendar day for the viewer. */
export function sameDay(one: Date, other: Date) {
  return daysBetween(one, other) === 0
}

/** Returns HH:mm in the viewer's time zone. */
export function timeOfDay(instant: Date, locale: string) {
  return new Intl.DateTimeFormat(locale, {
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).format(instant)
}

/** Returns the label of a day separator: "Today", "Yesterday", then the date, with the year when it is not this one. */
export function dayLabel(
  instant: Date,
  now: Date,
  locale: string,
  t: TFunction,
) {
  const days = daysBetween(instant, now)
  if (days === 0) return t('messaging.today')
  if (days === 1) return t('messaging.yesterday')
  return new Intl.DateTimeFormat(locale, {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: instant.getFullYear() === now.getFullYear() ? undefined : 'numeric',
  }).format(instant)
}

/** Returns the time of a list row: the time today, "Yesterday", the weekday within a week, else a short date. */
export function listTime(
  instant: Date,
  now: Date,
  locale: string,
  t: TFunction,
) {
  const days = daysBetween(instant, now)
  if (days === 0) return timeOfDay(instant, locale)
  if (days === 1) return t('messaging.yesterday')
  if (days < WEEK_DAYS) {
    return new Intl.DateTimeFormat(locale, { weekday: 'short' }).format(instant)
  }
  return new Intl.DateTimeFormat(locale, {
    day: 'numeric',
    month: 'short',
    year: instant.getFullYear() === now.getFullYear() ? undefined : 'numeric',
  }).format(instant)
}
