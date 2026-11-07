import { CheckIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { fromKg, useWeightUnit } from '@/lib/weight'
import { cn } from '@/lib/utils'
import type { LoggedEntry, PlannedSet } from '../api/session-log'

/** One planned set: its targets in grey until it is logged, and what the client did afterwards. */
export function SetRow({
  entry,
  set,
}: {
  entry: LoggedEntry
  set: PlannedSet
}) {
  const { t } = useTranslation()
  const unit = useWeightUnit()
  const logged = set.log !== undefined
  // what the row shows: the logged values, else the targets
  const shown = set.log ?? set.target
  const measure = t(`plan.entries.measures.${entry.measure}`)

  return (
    <li
      className={cn(
        'flex min-h-14 items-center gap-3 px-4 py-2',
        !logged && 'text-muted-foreground',
      )}
    >
      <span className="num w-6 shrink-0 text-sm font-medium">{set.number}</span>
      <span className="flex min-w-0 flex-1 flex-wrap gap-x-3 gap-y-0.5">
        <span className="num">
          {t('sessionlog.row.amount', { amount: shown.amount, unit: measure })}
        </span>
        <span className="num">
          {shown.loadKg === undefined
            ? t('sessionlog.row.noLoad')
            : t('sessionlog.row.load', {
                load: fromKg(shown.loadKg, unit),
                unit: t(`plan.units.${unit}`),
              })}
        </span>
        <span className="num">
          {t('sessionlog.row.rpe', { rpe: shown.rpe })}
        </span>
      </span>
      {/* logged is told by an icon and by text, not by colour alone */}
      {logged ? (
        <span className="text-foreground flex shrink-0 items-center gap-1 text-sm font-medium">
          <CheckIcon aria-hidden className="size-5" />
          {t('sessionlog.row.logged')}
        </span>
      ) : (
        <span className="sr-only">{t('sessionlog.row.planned')}</span>
      )}
    </li>
  )
}
