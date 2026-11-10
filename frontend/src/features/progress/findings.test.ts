import { describe, expect, it } from 'vitest'
import type { ExerciseWeek, ProgressWeek } from './api/progress'
import { rpePlace, topSetTrend, weeksAtTarget } from './findings'

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

describe('topSetTrend', () => {
  const week = (number: number, amount: number, loadKg?: number) =>
    ({
      number,
      top: { loadKg, amount, rpe: 8 },
      averageRpe: 8,
    }) satisfies ExerciseWeek

  it('says by how much the load rose from the first week to the last', () => {
    expect(
      topSetTrend([
        week(11, 5, 100),
        week(12, 5, 100),
        week(13, 5, 102.5),
        week(14, 3, 105),
      ]),
    ).toEqual({
      inLoad: true,
      direction: 'rose',
      change: 5,
      weeks: 4,
      last: 105,
    })
  })

  it('says by how much it fell', () => {
    expect(topSetTrend([week(3, 8, 60), week(5, 8, 57.5)])).toMatchObject({
      direction: 'fell',
      change: 2.5,
      weeks: 3,
    })
  })

  it('says it held when the first and the last week are the same', () => {
    expect(
      topSetTrend([week(1, 5, 80), week(2, 5, 85), week(3, 8, 80)]),
    ).toMatchObject({ direction: 'held', change: 0, last: 80 })
  })

  it('holds at the only week', () => {
    expect(topSetTrend([week(4, 5, 80)])).toMatchObject({
      direction: 'held',
      weeks: 1,
      last: 80,
    })
  })

  it('leaves out weeks without a load when any week has one', () => {
    expect(
      topSetTrend([week(1, 12), week(2, 5, 10), week(3, 6, 12.5), week(4, 15)]),
    ).toMatchObject({ inLoad: true, direction: 'rose', change: 2.5, weeks: 2 })
  })

  it('compares repetitions or seconds when no week has a load', () => {
    expect(topSetTrend([week(1, 30), week(2, 45)])).toEqual({
      inLoad: false,
      direction: 'rose',
      change: 15,
      weeks: 2,
      last: 45,
    })
  })
})
