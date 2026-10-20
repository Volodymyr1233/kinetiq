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
import type { Exercise } from '../api/exercises'
import { defaultLabel, plansLabel } from '../format'

export function ExerciseTable({
  exercises,
  chosenId,
  onChoose,
}: {
  exercises: Exercise[]
  chosenId?: string
  onChoose: (id: string) => void
}) {
  const { t } = useTranslation()

  return (
    <Table data-density="compact">
      <TableHeader>
        <TableRow>
          <TableHead>{t('exercises.columns.exercise')}</TableHead>
          <TableHead>{t('exercises.columns.category')}</TableHead>
          <TableHead>{t('exercises.columns.equipment')}</TableHead>
          <TableHead className="text-right">
            {t('exercises.columns.default')}
          </TableHead>
          <TableHead className="text-right">
            {t('exercises.columns.plans')}
          </TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {exercises.map((exercise) => {
          const chosen = exercise.id === chosenId
          return (
            <TableRow
              key={exercise.id}
              data-state={chosen ? 'selected' : undefined}
            >
              <TableCell className="whitespace-normal">
                {/* underlined when chosen, so the choice is not shown by colour alone */}
                <button
                  type="button"
                  aria-current={chosen ? 'true' : undefined}
                  onClick={() => onChoose(exercise.id)}
                  className={cn(
                    'focus-visible:ring-ring/50 rounded-sm text-left font-medium underline-offset-4 outline-none hover:underline focus-visible:ring-[3px]',
                    chosen && 'underline',
                  )}
                >
                  {exercise.name}
                </button>
              </TableCell>
              <TableCell className="text-muted-foreground whitespace-normal">
                {t(`exercises.categories.${exercise.category}`)}
              </TableCell>
              <TableCell className="text-muted-foreground whitespace-normal">
                {exercise.equipment}
              </TableCell>
              <TableCell className="num text-right">
                {defaultLabel(t, exercise)}
              </TableCell>
              <TableCell
                className={cn('text-right', exercise.plansUsing > 0 && 'num')}
              >
                {plansLabel(t, exercise.plansUsing, 'exercises.plans.notUsed')}
              </TableCell>
            </TableRow>
          )
        })}
      </TableBody>
    </Table>
  )
}
