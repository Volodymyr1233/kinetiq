import { useTranslation } from 'react-i18next'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { cn } from '@/lib/utils'
import { fromKg, useWeightUnit } from '@/lib/weight'
import type { ClientSessionLog } from '../api/client-training'

type Entry = ClientSessionLog['entries'][number]

// the design system's RPE colours, by the set's target
function rpeTone(logged: number, target: number) {
  if (logged < target) return 'bg-rpe-easy'
  return logged === target ? 'bg-rpe-target' : 'bg-rpe-hard'
}

/**
 * Every planned set of a session against its target, one row per set, as the trainer reads a client's log. Loads are
 * in the trainer's unit; an exercise measured in seconds shows its amounts in seconds.
 */
export function SessionLogTable({ entries }: { entries: Entry[] }) {
  const { t, i18n } = useTranslation()
  const unit = useWeightUnit()
  const number = new Intl.NumberFormat(i18n.language, {
    maximumFractionDigits: 1,
  })
  const amount = (value: number, measure: Entry['measure']) =>
    measure === 'SECONDS'
      ? t('sessionlog.logs.seconds', { value })
      : number.format(value)
  const load = (kg: number) => number.format(fromKg(kg, unit))
  const hasSeconds = entries.some((entry) => entry.measure === 'SECONDS')

  return (
    <Table data-density="compact">
      <TableHeader>
        <TableRow>
          <TableHead>{t('sessionlog.logs.columns.exercise')}</TableHead>
          <TableHead className="text-right">
            {t('sessionlog.logs.columns.set')}
          </TableHead>
          <TableHead className="text-right">
            {t('sessionlog.logs.columns.target')}
          </TableHead>
          <TableHead className="text-right">
            {t(`plan.units.${unit}`)}
          </TableHead>
          <TableHead className="text-right">
            {t(
              hasSeconds
                ? 'sessionlog.logs.columns.repsOrSeconds'
                : 'sessionlog.logs.columns.reps',
            )}
          </TableHead>
          <TableHead className="text-right">
            {t('sessionlog.logs.columns.rpe')}
          </TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {entries.flatMap((entry) =>
          entry.sets.map((set) => (
            <TableRow key={`${entry.position}-${set.number}`}>
              <TableCell className="font-medium">
                {/* the name shows once per exercise, and is still read out on every row */}
                <span className={cn(set.number > 1 && 'sr-only')}>
                  {entry.exerciseName}
                </span>
              </TableCell>
              <TableCell className="num text-right">{set.number}</TableCell>
              <TableCell className="num text-right">
                {set.target.loadKg === undefined
                  ? amount(set.target.amount, entry.measure)
                  : t('sessionlog.logs.target', {
                      amount: amount(set.target.amount, entry.measure),
                      load: load(set.target.loadKg),
                    })}
              </TableCell>
              {set.log === undefined ? (
                <TableCell
                  colSpan={3}
                  className="text-muted-foreground text-right"
                >
                  {t('sessionlog.logs.notLogged')}
                </TableCell>
              ) : (
                <>
                  <TableCell className="num text-right">
                    {set.log.loadKg === undefined ? (
                      <>
                        <span aria-hidden>–</span>
                        <span className="sr-only">
                          {t('sessionlog.logs.noLoad')}
                        </span>
                      </>
                    ) : (
                      load(set.log.loadKg)
                    )}
                  </TableCell>
                  <TableCell className="num text-right">
                    {amount(set.log.amount, entry.measure)}
                  </TableCell>
                  <TableCell className="text-right">
                    <span className="inline-flex items-center gap-1.5">
                      <span
                        aria-hidden
                        className={cn(
                          'size-2 rounded-full',
                          rpeTone(set.log.rpe, set.target.rpe),
                        )}
                      />
                      <span className="num">{set.log.rpe}</span>
                    </span>
                  </TableCell>
                </>
              )}
            </TableRow>
          )),
        )}
      </TableBody>
    </Table>
  )
}
