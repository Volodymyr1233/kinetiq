import { useTranslation } from 'react-i18next'
import { fromKg, useWeightUnit } from '@/lib/weight'
import type { OwnPlan } from '../api/client-plan'

type Entry = OwnPlan['sessions'][number]['entries'][number]

/** One exercise of a session with its targets, as a list item; the week view and Today draw it the same way. */
export function EntryRow({ entry }: { entry: Entry }) {
  const { t } = useTranslation()
  const unit = useWeightUnit()

  return (
    <li className="flex flex-col gap-1 px-4 py-3">
      <span className="font-medium break-words">{entry.exerciseName}</span>
      <span className="text-muted-foreground flex flex-wrap gap-x-3 gap-y-0.5 text-sm">
        <span className="num">
          {t('plan.client.entry.target', {
            sets: entry.sets,
            amount: entry.amount,
            unit: t(`plan.entries.measures.${entry.measure}`),
          })}
        </span>
        <span className="num">
          {entry.loadKg === undefined
            ? t('plan.client.entry.noLoad')
            : t('plan.client.entry.load', {
                load: fromKg(entry.loadKg, unit),
                unit: t(`plan.units.${unit}`),
              })}
        </span>
        <span className="num">
          {t('plan.client.entry.rpe', { rpe: entry.rpe })}
        </span>
        <span className="num">
          {t('plan.client.entry.rest', { seconds: entry.restSeconds })}
        </span>
      </span>
    </li>
  )
}
