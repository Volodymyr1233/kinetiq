import { describe, expect, it } from 'vitest'
import { timeAgo } from './dates'

describe('timeAgo', () => {
  const now = new Date(2026, 10, 12, 10, 0)
  const at = (...parts: [number, number, number, number]) =>
    new Date(2026, ...parts).toISOString()

  it('counts minutes within an hour, hours within a day, then calendar days', () => {
    expect(timeAgo(at(10, 12, 9, 15), now, 'en')).toBe('45 minutes ago')
    expect(timeAgo(at(10, 12, 0, 30), now, 'en')).toBe('9 hours ago')
    expect(timeAgo(at(10, 11, 8, 0), now, 'en')).toBe('Yesterday')
    expect(timeAgo(at(10, 6, 23, 0), now, 'en')).toBe('6 days ago')
  })

  it('writes the reader’s language in sentence case', () => {
    expect(timeAgo(at(10, 11, 8, 0), now, 'pl')).toBe('Wczoraj')
    expect(timeAgo(at(10, 12, 10, 0), now, 'en')).toBe('Now')
  })
})
