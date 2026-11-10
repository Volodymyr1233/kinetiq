import { CircleAlertIcon } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { errorMessageKey, problemCode } from '@/lib/api/problem'
import { useProgress } from '../api/progress'
import { ExerciseProgress } from '../components/exercise-progress'
import { PeriodSwitch, type Period } from '../components/period-switch'
import { ProgressTiles } from '../components/progress-tiles'
import { SessionsChart } from '../components/sessions-chart'

/** The client's Progress tab: the plan weeks of the chosen period in tiles and charts, and each exercise's progress. */
export function ProgressPage() {
  const { t } = useTranslation()
  const [weeks, setWeeks] = useState<Period>(8)
  const query = useProgress(weeks)
  const progress = query.data

  if (progress === undefined) {
    if (query.isPending) {
      return (
        <div className="flex flex-col gap-4 px-4">
          <p role="status" className="sr-only">
            {t('progress.loading')}
          </p>
          <div aria-hidden className="flex flex-col gap-4">
            <Skeleton className="h-4 w-40" />
            <Skeleton className="h-14 w-60" />
            <div className="grid grid-cols-2 gap-3">
              {Array.from({ length: 4 }, (_, i) => (
                <Skeleton key={i} className="h-32" />
              ))}
            </div>
            <Skeleton className="h-64 w-full" />
          </div>
        </div>
      )
    }
    if (problemCode(query.error) === 'plan.not-found') {
      return (
        <p className="text-muted-foreground px-4 text-sm">
          {t('progress.noPlan')}
        </p>
      )
    }
    return (
      <div className="px-4">
        <Alert variant="destructive">
          <CircleAlertIcon aria-hidden />
          <AlertDescription>{t(errorMessageKey(query.error))}</AlertDescription>
          <AlertAction>
            <Button variant="outline" onClick={() => void query.refetch()}>
              {t('progress.retry')}
            </Button>
          </AlertAction>
        </Alert>
      </div>
    )
  }

  const goal = t(`quiz.goal.options.${progress.goal}.title`)
  const nothingLogged =
    progress.sessionsDone === 0 && progress.exercises.length === 0

  return (
    <div className="flex flex-col gap-4 px-4">
      <p className="num text-muted-foreground text-xs tracking-wider uppercase">
        {progress.finished
          ? t('progress.header.finished', { goal })
          : t('progress.header.week', { goal, week: progress.week })}
      </p>
      <PeriodSwitch value={weeks} onChange={setWeeks} />
      <ProgressTiles progress={progress} />
      {nothingLogged ? (
        <p className="text-muted-foreground text-sm">
          {t('progress.nothingLogged')}
        </p>
      ) : (
        <>
          <SessionsChart weeks={progress.weeks} />
          {progress.exercises.length > 0 && (
            <ExerciseProgress
              exercises={progress.exercises}
              weeks={progress.weeks.map((week) => week.number)}
            />
          )}
        </>
      )}
    </div>
  )
}
