import { useTranslation } from 'react-i18next'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { weekdayName } from '@/lib/dates'
import { fromKg, useWeightUnit } from '@/lib/weight'
import type { PlanChange, PlanChangeTarget } from '../api/plan-change'

type Row = {
  key: string
  name: string
  session: string
  now: string
  next: string
  change: string
}

/**
 * Every change of a plan change as a row: what it touches, where in the plan, the value now (struck through), the new
 * value and the change. A removed session and the entries that move out of it are rows too.
 */
export function PlanChangeTable({
  planChange,
  list = false,
}: {
  planChange: Pick<PlanChange, 'rule' | 'targets' | 'removedSession' | 'moves'>
  /** One item per change instead of table columns, for a phone. */
  list?: boolean
}) {
  const { t, i18n } = useTranslation()
  const unit = useWeightUnit()
  const locale = i18n.language
  const number = new Intl.NumberFormat(locale, { maximumFractionDigits: 1 })
  const percent = new Intl.NumberFormat(locale, {
    style: 'percent',
    maximumFractionDigits: 0,
    signDisplay: 'exceptZero',
  })

  const value = (target: PlanChangeTarget, raw: number) => {
    switch (target.target) {
      case 'SETS':
        return t('planChange.values.sets', { count: raw })
      case 'AMOUNT':
        return target.measure === 'SECONDS'
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

  const session = (
    day: PlanChange['targets'][number]['sessionDay'],
    name: string,
  ) => t('planChange.table.sessionOn', { name, day: weekdayName(day, locale) })

  const rows: Row[] = [
    ...planChange.targets.map((target) => ({
      key: `${target.sessionDay}-${target.position}-${target.target}`,
      name: target.exerciseName,
      session: session(target.sessionDay, target.sessionName),
      now: value(target, target.oldValue),
      next: value(target, target.newValue),
      change: percent.format(
        (target.newValue - target.oldValue) / target.oldValue,
      ),
    })),
    ...(planChange.removedSession
      ? [
          {
            key: 'removed',
            name: planChange.removedSession.name,
            session: weekdayName(planChange.removedSession.day, locale),
            now: t('planChange.table.planned'),
            next: t('planChange.table.removed'),
            change: t('planChange.table.oneSessionLess'),
          },
        ]
      : []),
    ...planChange.moves.map((move) => ({
      key: `move-${move.exerciseId}-${move.toDay}`,
      name: move.exerciseName,
      session: weekdayName(move.fromDay, locale),
      now: weekdayName(move.fromDay, locale),
      next: weekdayName(move.toDay, locale),
      change: t('planChange.table.moved'),
    })),
  ]

  if (list) {
    return (
      <ul className="flex flex-col gap-3">
        {rows.map((row) => (
          <li
            key={row.key}
            className="grid grid-cols-[minmax(0,1fr)_auto] items-baseline gap-x-3"
          >
            <span className="font-medium">{row.name}</span>
            <span className="num text-end">{row.change}</span>
            <span className="text-muted-foreground text-sm">{row.session}</span>
            <span className="num text-end">
              {/* struck through, and a screen reader hears it as the value now */}
              <s className="text-muted-foreground">{row.now}</s>
              {' → '}
              <b>{row.next}</b>
            </span>
          </li>
        ))}
      </ul>
    )
  }

  return (
    <Table data-density="compact">
      <TableHeader>
        <TableRow>
          <TableHead>
            {planChange.rule === 'LOW_COMPLETION'
              ? t('planChange.table.plan')
              : t('planChange.table.exercise')}
          </TableHead>
          <TableHead>{t('planChange.table.session')}</TableHead>
          <TableHead className="text-end">
            {t('planChange.table.now')}
          </TableHead>
          <TableHead className="text-end">
            {t('planChange.table.new')}
          </TableHead>
          <TableHead className="text-end">
            {t('planChange.table.change')}
          </TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {rows.map((row) => (
          <TableRow key={row.key}>
            <TableCell className="font-medium whitespace-normal">
              {row.name}
            </TableCell>
            <TableCell className="text-muted-foreground whitespace-normal">
              {row.session}
            </TableCell>
            <TableCell className="num text-muted-foreground text-end">
              {/* struck through, and a screen reader hears it as the value now */}
              <s>{row.now}</s>
            </TableCell>
            <TableCell className="num text-end font-semibold">
              {row.next}
            </TableCell>
            <TableCell className="num text-end">{row.change}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
