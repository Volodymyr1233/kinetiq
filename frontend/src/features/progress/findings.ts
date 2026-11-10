import type { ExerciseWeek, ProgressWeek } from './api/progress'

export type RpePlace = 'below' | 'within' | 'above'

/** Where a one-decimal mean RPE stands against the usual 7–8 target; 7.0 and 8.0 are within. */
export function rpePlace(value: number): RpePlace {
  if (value < 7) return 'below'
  return value > 8 ? 'above' : 'within'
}

/**
 * The completed weeks with planned sessions, and how many of them reached 70% of their sessions; a running week has not
 * missed 70% yet, so it counts in neither.
 */
export function weeksAtTarget(weeks: readonly ProgressWeek[]) {
  const counted = weeks.filter((week) => !week.running && week.planned > 0)
  return {
    // whole numbers, so 7 of 10 is exactly 70%
    atTarget: counted.filter((week) => week.done * 10 >= week.planned * 7)
      .length,
    of: counted.length,
  }
}

/**
 * How an exercise's top set moved from the first to the last period week that has one: in load when any week has a
 * load, leaving out the weeks without, otherwise in repetitions or seconds. `weeks` counts both ends; `last` is the
 * size in the last of them. Nothing when no week has a top set in that measure.
 */
export function topSetTrend(weeks: readonly ExerciseWeek[]) {
  const inLoad = weeks.some((week) => week.top.loadKg !== undefined)
  const sizes = weeks.flatMap((week) => {
    const size = inLoad ? week.top.loadKg : week.top.amount
    return size === undefined ? [] : [{ week: week.number, size }]
  })
  const first = sizes.at(0)
  const last = sizes.at(-1)
  if (first === undefined || last === undefined) return undefined
  const change = last.size - first.size
  return {
    inLoad,
    direction: change > 0 ? 'rose' : change < 0 ? 'fell' : 'held',
    change: Math.abs(change),
    weeks: last.week - first.week + 1,
    last: last.size,
  } as const
}
