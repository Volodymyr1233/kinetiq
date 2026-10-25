import { describe, expect, it } from 'vitest'
import { daysLeft, sentDay } from './invite-format'

const EXPIRES = '2026-11-01T10:00:00Z'

describe('daysLeft', () => {
  it.each([
    ['2026-10-25T10:00:00Z', 7],
    ['2026-10-26T09:00:00Z', 7],
    ['2026-10-31T11:00:00Z', 1],
    ['2026-11-01T09:59:59Z', 1],
  ])('at %s counts %i days, rounded up', (now, days) => {
    expect(daysLeft(EXPIRES, new Date(now))).toBe(days)
  })
})

// local times, so the day boundary is the one of whatever zone the tests run in
describe('sentDay', () => {
  const sent = new Date(2026, 9, 25, 0, 30).toISOString()

  it('leaves today to the caller', () => {
    expect(sentDay(sent, new Date(2026, 9, 25, 23, 30), 'en')).toBeUndefined()
  })

  it('names an earlier day in the given language', () => {
    const label = sentDay(sent, new Date(2026, 9, 26, 0, 10), 'en')

    expect(label).toContain('25')
    expect(label).toContain('Oct')
  })
})
