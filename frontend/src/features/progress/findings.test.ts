import { describe, expect, it } from 'vitest'
import type { ProgressWeek } from './api/progress'
import { rpePlace, weeksAtTarget } from './findings'

describe('rpePlace', () => {
  it('counts 7.0 to 8.0 as within the target', () => {
    expect(rpePlace(7)).toBe('within')
    expect(rpePlace(7.5)).toBe('within')
    expect(rpePlace(8)).toBe('within')
  })

  it('puts the values around the target below or above it', () => {
    expect(rpePlace(6.9)).toBe('below')
    expect(rpePlace(8.1)).toBe('above')
  })
})

describe('weeksAtTarget', () => {
  const week = (done: number, planned: number, running = false) =>
    ({
      number: 1,
      startsOn: '2026-11-02',
      done,
      planned,
      running,
    }) satisfies ProgressWeek

  it('counts weeks with at least 70% of their sessions done', () => {
    expect(weeksAtTarget([week(3, 3), week(7, 10), week(2, 3)])).toEqual({
      atTarget: 2,
      of: 3,
    })
  })

  it('leaves out the running week and weeks without planned sessions', () => {
    expect(weeksAtTarget([week(0, 0), week(3, 3), week(0, 3, true)])).toEqual({
      atTarget: 1,
      of: 1,
    })
  })
})
