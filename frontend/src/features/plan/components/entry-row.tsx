import { CalendarClockIcon, HistoryIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { Badge } from '@/components/ui/badge'
import { TargetChange } from '@/components/target-change'
import { ValueList } from '@/components/value-list'
import type { components } from '@/lib/api/schema'
import { fromKg, useWeightUnit } from '@/lib/weight'
import { formatDay } from '@/lib/dates'
import type { OwnPlan } from '../api/client-plan'
import type { ChangedTarget, PlanChangeMark } from './plan-change-mark'

type Entry = OwnPlan['sessions'][number]['entries'][number]
type Target = components['schemas']['AdjustedTargetResponse']

const KEYS = {
  SETS: 'sets',
  AMOUNT: 'amount',
  LOAD_KG: 'loadKg',
  RPE: 'rpe',
  REST_SECONDS: 'restSeconds',
} as const

/**
 * The targets of an entry before and after a plan change that has not taken effect: the plan still holds the old ones,
 * so the new ones are the changed values on top of them. An applied plan change is in the plan already.
 */
function targetsOf(
  entry: Entry,
  mark: PlanChangeMark | undefined,
  changes: ChangedTarget[],
) {
  const next = { ...entry }
  if (mark?.kind === 'upcoming') {
    for (const change of changes) next[KEYS[change.target]] = change.newValue
  }
  return next
}

/**
 * One exercise of a session with its targets, as a list item; the week view and Today draw it the same way. `today`
 * holds today's lighter targets on a recovery day: each one that differs is shown as planned → today. `mark` and
 * `changes` tell what a plan change does to this entry: an upcoming one shows the new values next to the old with the
 * day they take effect, an applied one links to itself.
 */
export function EntryRow({
  entry,
  today,
  mark,
  changes = [],
}: {
  entry: Entry
  today?: Target
  mark?: PlanChangeMark
  changes?: ChangedTarget[]
}) {
  const { t, i18n } = useTranslation()
  const unit = useWeightUnit()
  const next = targetsOf(entry, mark, changes)
  const upcoming = mark?.kind === 'upcoming' && changes.length > 0

  const amount = (sets: number, value: number) =>
    t('plan.client.entry.target', {
      sets,
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
  const rest = (seconds: number) => t('plan.client.entry.rest', { seconds })

  return (
    <li className="flex flex-col gap-1 px-4 py-3">
      <span className="font-medium break-words">{entry.exerciseName}</span>
      <ValueList className="text-muted-foreground text-sm">
        <TargetChange
          planned={amount(entry.sets, entry.amount)}
          today={amount(next.sets, today?.amount ?? next.amount)}
          upcoming={upcoming}
        />
        <TargetChange
          planned={load(entry.loadKg)}
          today={load(today ? today.loadKg : next.loadKg)}
          upcoming={upcoming}
        />
        <TargetChange
          planned={rpe(entry.rpe)}
          today={rpe(today?.rpe ?? next.rpe)}
          upcoming={upcoming}
        />
        <TargetChange
          planned={rest(entry.restSeconds)}
          today={rest(next.restSeconds)}
          upcoming={upcoming}
        />
      </ValueList>
      {mark && changes.length > 0 && mark.kind === 'upcoming' && (
        <Badge variant="info" className="self-start">
          <CalendarClockIcon aria-hidden />
          {t('plan.client.change.takesEffect', {
            date: formatDay(mark.change.effectiveWeek, i18n.language),
          })}
        </Badge>
      )}
      {mark && changes.length > 0 && mark.kind === 'applied' && (
        <Link
          to={`/app/plan/change/${mark.change.id}`}
          className="text-muted-foreground focus-visible:ring-ring/50 inline-flex items-center gap-1.5 self-start rounded text-sm underline underline-offset-2 outline-none focus-visible:ring-3"
        >
          <HistoryIcon aria-hidden className="size-3.5" />
          {t('plan.client.change.changedOn', {
            date: formatDay(mark.change.effectiveWeek, i18n.language),
          })}
        </Link>
      )}
    </li>
  )
}
