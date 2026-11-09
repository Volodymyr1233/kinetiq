import { useTranslation } from 'react-i18next'
import { TargetChange } from '@/components/target-change'
import { ValueList } from '@/components/value-list'
import type { components } from '@/lib/api/schema'
import { fromKg, useWeightUnit } from '@/lib/weight'
import type { OwnPlan } from '../api/client-plan'

type Entry = OwnPlan['sessions'][number]['entries'][number]
type Target = components['schemas']['AdjustedTargetResponse']

/**
 * One exercise of a session with its targets, as a list item; the week view and Today draw it the same way. `today`
 * holds today's lighter targets on a recovery day: each one that differs is shown as planned → today.
 */
export function EntryRow({ entry, today }: { entry: Entry; today?: Target }) {
  const { t } = useTranslation()
  const unit = useWeightUnit()

  const amount = (value: number) =>
    t('plan.client.entry.target', {
      sets: entry.sets,
      amount: value,
      unit: t(`plan.entries.measures.${entry.measure}`),
    })
  const load = (loadKg: number | undefined) =>
    loadKg === undefined
      ? t('plan.client.entry.noLoad')
      : t('plan.client.entry.load', {
          load: fromKg(loadKg, unit),
          unit: t(`plan.units.${unit}`),
        })
  const rpe = (value: number) => t('plan.client.entry.rpe', { rpe: value })

  return (
    <li className="flex flex-col gap-1 px-4 py-3">
      <span className="font-medium break-words">{entry.exerciseName}</span>
      <ValueList className="text-muted-foreground text-sm">
        <TargetChange
          planned={amount(entry.amount)}
          today={amount(today?.amount ?? entry.amount)}
        />
        <TargetChange
          planned={load(entry.loadKg)}
          today={load(today ? today.loadKg : entry.loadKg)}
        />
        <TargetChange
          planned={rpe(entry.rpe)}
          today={rpe(today?.rpe ?? entry.rpe)}
        />
        <span className="num">
          {t('plan.client.entry.rest', { seconds: entry.restSeconds })}
        </span>
      </ValueList>
    </li>
  )
}
