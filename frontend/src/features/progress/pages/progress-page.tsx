import { CircleAlertIcon } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { errorMessageKey, problemCode } from '@/lib/api/problem'
import { useProgress } from '../api/progress'
import { ExerciseProgress } from '../components/exercise-progress'
import { ProgressTiles } from '../components/progress-tiles'
import { SessionsChart } from '../components/sessions-chart'

const PERIODS = [4, 8, 12] as const
type Period = (typeof PERIODS)[number]

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
      <ToggleGroup
        type="single"
        spacing={1}
        value={String(weeks)}
        // a single toggle group sends an empty value when the chosen option is chosen again; a period always has one
        onValueChange={(next) => {
          if (next) setWeeks(Number(next) as Period)
        }}
        aria-label={t('progress.period.label')}
        className="bg-muted rounded-lg p-1"
      >
        {PERIODS.map((period) => (
          <ToggleGroupItem
            key={period}
            value={String(period)}
            // a border, not a ring, so the focus ring still shows on the chosen option
            className="text-muted-foreground data-[state=on]:bg-background data-[state=on]:text-foreground data-[state=on]:border-primary h-12 border border-transparent px-4 font-normal hover:bg-transparent data-[state=on]:font-medium data-[state=on]:shadow-sm"
          >
            {t('progress.period.weeks', { count: period })}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
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
