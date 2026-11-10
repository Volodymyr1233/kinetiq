import { CircleAlertIcon } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { errorMessageKey } from '@/lib/api/problem'
import { useClientProgress } from '../api/progress'
import { ExerciseProgress } from './exercise-progress'
import { PeriodSwitch, type Period } from './period-switch'
import { ProgressTiles } from './progress-tiles'
import { SessionsChart } from './sessions-chart'

/**
 * A client's progress for their trainer: the period, tiles and charts of the client's Progress tab with the same
 * figures, without the rewards the design system keeps for client screens.
 */
export function ClientProgressSection({ clientId }: { clientId: string }) {
  const { t } = useTranslation()
  const [weeks, setWeeks] = useState<Period>(8)
  const query = useClientProgress(clientId, weeks)
  const progress = query.data

  if (progress === undefined) {
    if (query.isPending) {
      return (
        <>
          <span className="sr-only" role="status">
            {t('progress.trainer.loading')}
          </span>
          <div aria-hidden className="flex flex-col gap-4">
            <Skeleton className="h-14 w-60" />
            <div className="grid grid-cols-3 gap-3">
              {Array.from({ length: 3 }, (_, i) => (
                <Skeleton key={i} className="h-32" />
              ))}
            </div>
            <div className="grid items-start gap-6 lg:grid-cols-2">
              <Skeleton className="h-80" />
              <Skeleton className="h-80" />
            </div>
          </div>
        </>
      )
    }
    return (
      <Alert variant="destructive">
        <CircleAlertIcon aria-hidden />
        <AlertDescription>{t(errorMessageKey(query.error))}</AlertDescription>
        <AlertAction>
          <Button
            variant="outline"
            size="sm"
            onClick={() => void query.refetch()}
          >
            {t('progress.retry')}
          </Button>
        </AlertAction>
      </Alert>
    )
  }

  const nothingLogged =
    progress.sessionsDone === 0 && progress.exercises.length === 0

  return (
    <section
      aria-label={t('progress.trainer.label')}
      className="flex flex-col gap-4"
    >
      <PeriodSwitch value={weeks} onChange={setWeeks} />
      <ProgressTiles
        progress={progress}
        rewards={false}
        className="grid-cols-3"
      />
      {nothingLogged ? (
        <p className="text-muted-foreground text-sm">
          {t('progress.trainer.nothingLogged')}
        </p>
      ) : (
        <div className="grid items-start gap-6 lg:grid-cols-2">
          <SessionsChart weeks={progress.weeks} compact />
          {progress.exercises.length > 0 && (
            <ExerciseProgress
              exercises={progress.exercises}
              weeks={progress.weeks.map((week) => week.number)}
              compact
            />
          )}
        </div>
      )}
    </section>
  )
}
