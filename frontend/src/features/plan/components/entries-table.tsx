import { useTranslation } from 'react-i18next'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import type { WeightUnit } from '@/lib/weight'
import {
  entryFromExercise,
  MAX_EXERCISES,
  type DraftEntry,
  type DraftSession,
} from '../plan-draft'
import { ExercisePicker } from './exercise-picker'

/** The session's exercises in order. Reads only for now. */
export function EntriesTable({
  session,
  sessionLabel,
  unit,
  onChange,
}: {
  session: DraftSession
  sessionLabel: string
  unit: WeightUnit
  onChange: (entries: DraftEntry[]) => void
}) {
  const { t } = useTranslation()
  const full = session.entries.length >= MAX_EXERCISES

  return (
    <div className="rounded-lg border" data-density="compact">
      {session.entries.length === 0 ? (
        <p className="text-muted-foreground px-3 py-4 text-sm">
          {t('plan.entries.empty')}
        </p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t('plan.entries.exercise')}</TableHead>
              <TableHead className="text-right">
                {t('plan.entries.sets')}
              </TableHead>
              <TableHead className="text-right">
                {t('plan.entries.amount')}
              </TableHead>
              <TableHead className="text-right">
                {t('plan.entries.load', { unit: t(`plan.units.${unit}`) })}
              </TableHead>
              <TableHead className="text-right">
                {t('plan.entries.rpe')}
              </TableHead>
              <TableHead className="text-right">
                {t('plan.entries.rest')}
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {session.entries.map((entry) => (
              <TableRow key={entry.key}>
                <TableCell className="font-medium whitespace-normal">
                  {entry.exerciseName}
                </TableCell>
                <TableCell className="num text-right">{entry.sets}</TableCell>
                <TableCell className="num text-right">
                  {entry.amount} {t(`plan.entries.measures.${entry.measure}`)}
                </TableCell>
                <TableCell className="num text-right">{entry.load}</TableCell>
                <TableCell className="num text-right">{entry.rpe}</TableCell>
                <TableCell className="num text-right">{entry.rest}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
      <div className="flex flex-wrap items-center gap-3 border-t px-3 py-2">
        <ExercisePicker
          disabled={full}
          sessionLabel={sessionLabel}
          onPick={(exercise) =>
            onChange([...session.entries, entryFromExercise(exercise)])
          }
        />
        <span className="text-muted-foreground text-sm">
          {t(full ? 'plan.entries.limit' : 'plan.entries.fromLibrary', {
            count: MAX_EXERCISES,
          })}
        </span>
      </div>
    </div>
  )
}
