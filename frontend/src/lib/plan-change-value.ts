import { useTranslation } from 'react-i18next'
import type { components } from '@/lib/api/schema'
import { fromKg, useWeightUnit } from '@/lib/weight'

type Changed = Pick<
  components['schemas']['PlanChangeTargetResponse'],
  'target' | 'measure'
>

/** Writes a value of a changed target in words and the viewer's unit: `3 sets`, `9 reps`, `82.5 kg`, `RPE 8`. */
export function usePlanChangeValue() {
  const { t, i18n } = useTranslation()
  const unit = useWeightUnit()
  const number = new Intl.NumberFormat(i18n.language, {
    maximumFractionDigits: 1,
  })

  return (changed: Changed, raw: number) => {
    switch (changed.target) {
      case 'SETS':
        return t('planChange.values.sets', { count: raw })
      case 'AMOUNT':
        return changed.measure === 'SECONDS'
          ? t('planChange.values.seconds', { count: raw })
          : t('planChange.values.reps', { count: raw })
      case 'LOAD_KG':
        return t('planChange.values.load', {
          load: number.format(fromKg(raw, unit)),
          unit: t(`plan.units.${unit}`),
        })
      case 'RPE':
        return t('planChange.values.rpe', { rpe: raw })
      case 'REST_SECONDS':
        return t('planChange.values.seconds', { count: raw })
    }
  }
}
