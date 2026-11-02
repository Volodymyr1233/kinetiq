import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import i18n from '@/lib/i18n'
import { dayLabel, listTime, timeOfDay } from './message-time'

// 23:30 in Tokyo, where the calendar day differs from UTC's
const NOW = new Date('2026-11-04T14:30:00Z')
const t = i18n.getFixedT('en')
const pl = i18n.getFixedT('pl')

describe('message times', () => {
  beforeEach(() => {
    vi.stubEnv('TZ', 'Asia/Tokyo')
  })

  afterEach(() => {
    vi.unstubAllEnvs()
  })

  it('shows the time of day in the time zone of the viewer', () => {
    expect(timeOfDay(NOW, 'en')).toBe('23:30')
    expect(timeOfDay(new Date('2026-11-04T00:05:00Z'), 'pl')).toBe('09:05')
  })

  it('separates days by the calendar of the viewer', () => {
    // 00:10 on the 5th in Tokyo, though still the 4th in UTC
    const nextDay = new Date('2026-11-04T15:10:00Z')

    expect(dayLabel(NOW, NOW, 'en', t)).toBe('Today')
    expect(dayLabel(NOW, nextDay, 'en', t)).toBe('Yesterday')
    expect(dayLabel(new Date('2026-11-03T15:10:00Z'), nextDay, 'pl', pl)).toBe(
      'Wczoraj',
    )
  })

  it('names an older day with its date, in the language of the screen', () => {
    const older = new Date('2026-10-05T10:00:00Z')

    expect(dayLabel(older, NOW, 'en', t)).toBe('Mon, Oct 5')
    expect(dayLabel(older, NOW, 'pl', pl)).toBe('pon., 5 paź')
  })

  it('adds the year to a day of another year', () => {
    expect(dayLabel(new Date('2025-10-05T10:00:00Z'), NOW, 'en', t)).toContain(
      '2025',
    )
  })

  it('shortens a list time by how long ago it was', () => {
    expect(listTime(NOW, NOW, 'en', t)).toBe('23:30')
    expect(listTime(new Date('2026-11-03T10:00:00Z'), NOW, 'en', t)).toBe(
      'Yesterday',
    )
    expect(listTime(new Date('2026-11-02T10:00:00Z'), NOW, 'en', t)).toBe('Mon')
    expect(listTime(new Date('2026-11-02T10:00:00Z'), NOW, 'pl', pl)).toBe(
      'pon.',
    )
    expect(listTime(new Date('2026-10-01T10:00:00Z'), NOW, 'en', t)).toBe(
      'Oct 1',
    )
  })
})
