import type { OpenPlanChange } from '../api/client-plan-change'

/**
 * A plan change that touches the week on screen: an upcoming one marks what is going to change from its Monday, an
 * applied one notes what it changed. The plan itself holds the old values until the Monday and the new ones after.
 */
export type PlanChangeMark = {
  kind: 'upcoming' | 'applied'
  change: OpenPlanChange
}

export type ChangedTarget = OpenPlanChange['targets'][number]

/**
 * The mark of the week that starts on `weekStart`: an upcoming plan change from its effective week on, else the applied
 * one from its own. ISO dates compare as text.
 */
export function markOfWeek(
  weekStart: string,
  upcoming: OpenPlanChange | null | undefined,
  applied: OpenPlanChange | null | undefined,
): PlanChangeMark | undefined {
  if (upcoming && weekStart >= upcoming.effectiveWeek) {
    return { kind: 'upcoming', change: upcoming }
  }
  if (applied && weekStart >= applied.effectiveWeek) {
    return { kind: 'applied', change: applied }
  }
  return undefined
}

/** The changed targets of the entry at `position` of the session on `day`. */
export function changesOfEntry(
  mark: PlanChangeMark | undefined,
  day: string,
  position: number,
): ChangedTarget[] {
  return (
    mark?.change.targets.filter(
      (target) => target.sessionDay === day && target.position === position,
    ) ?? []
  )
}
