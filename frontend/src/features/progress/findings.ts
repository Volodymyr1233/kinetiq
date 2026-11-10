import type { ProgressWeek } from './api/progress'

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
