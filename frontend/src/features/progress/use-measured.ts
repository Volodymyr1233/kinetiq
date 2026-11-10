import { useTranslation } from 'react-i18next'
import { fromKg, useWeightUnit } from '@/lib/weight'
import type { ExerciseProgress } from './api/progress'

/** A size of an exercise's set: a load in the reader's weight unit, or repetitions or seconds. */
export function useMeasured(measure: ExerciseProgress['measure']) {
  const { t, i18n } = useTranslation()
  const weightUnit = useWeightUnit()

  return (size: number, isLoad: boolean) => {
    const value = (n: number) =>
      new Intl.NumberFormat(i18n.language, {
        maximumFractionDigits: 1,
      }).format(n)
    if (isLoad) {
      return {
        value: value(fromKg(size, weightUnit)),
        unit: t(`plan.units.${weightUnit}`),
      }
    }
    return {
      value: value(size),
      unit:
        measure === 'SECONDS'
          ? t('progress.units.seconds')
          : t('progress.units.reps', { count: size }),
    }
  }
}
