import type { TFunction } from 'i18next'
import type { Exercise } from './api/exercises'

export function defaultLabel(t: TFunction, exercise: Exercise) {
  return t(
    exercise.measure === 'SECONDS'
      ? 'exercises.default.seconds'
      : 'exercises.default.reps',
    { sets: exercise.sets, amount: exercise.amount },
  )
}

export function plansLabel(t: TFunction, plansUsing: number, noneKey: string) {
  return plansUsing === 0
    ? t(noneKey)
    : t('exercises.plans.count', { count: plansUsing })
}
