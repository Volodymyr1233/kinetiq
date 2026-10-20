import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { cn } from '@/lib/utils'
import type { Exercise } from '../api/exercises'
import { defaultLabel, plansLabel } from '../format'

function Row({
  label,
  value,
  measured = true,
}: {
  label: string
  value: string
  measured?: boolean
}) {
  return (
    <div className="flex justify-between gap-4">
      <dt className="text-muted-foreground whitespace-nowrap">{label}</dt>
      <dd className={cn('text-right', measured && 'num')}>{value}</dd>
    </div>
  )
}

export function ExerciseDetail({ exercise }: { exercise: Exercise }) {
  const { t } = useTranslation()

  return (
    <Card>
      <CardHeader className="gap-3">
        <Badge variant="secondary">
          {t(`exercises.categories.${exercise.category}`)}
        </Badge>
        <h2 className="font-heading text-lg font-semibold">{exercise.name}</h2>
      </CardHeader>
      <CardContent>
        <dl className="flex flex-col gap-2">
          <Row
            label={t('exercises.detail.equipment')}
            value={exercise.equipment}
            measured={false}
          />
          <Row
            label={t('exercises.detail.default')}
            value={defaultLabel(t, exercise)}
          />
          <Row
            label={t('exercises.detail.rest')}
            value={t('exercises.detail.restSeconds', {
              seconds: exercise.restSeconds,
            })}
          />
          <Row
            label={t('exercises.detail.plansUsing')}
            value={plansLabel(
              t,
              exercise.plansUsing,
              'exercises.detail.notUsedYet',
            )}
            measured={exercise.plansUsing > 0}
          />
        </dl>
        {exercise.cues && (
          <div className="flex flex-col gap-1">
            <h3 className="text-muted-foreground text-xs font-medium uppercase">
              {t('exercises.detail.cues')}
            </h3>
            <p>{exercise.cues}</p>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
