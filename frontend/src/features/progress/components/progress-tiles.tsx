import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription } from '@/components/ui/card'
import { Progress as ProgressBar } from '@/components/ui/progress'
import { cn } from '@/lib/utils'
import type { ExerciseProgress, Progress } from '../api/progress'
import { rpePlace } from '../findings'
import { useMeasured } from '../use-measured'

const PLACE_COLOUR = {
  below: 'text-rpe-easy',
  within: 'text-rpe-target',
  above: 'text-rpe-hard',
} as const

/**
 * The tiles of the progress: sessions done, average RPE, the first exercise's best set and the week streak. Without
 * `rewards` there is no week streak and no "PR today", which the design system shows on client screens only.
 */
export function ProgressTiles({
  progress,
  rewards = true,
  className,
}: {
  progress: Progress
  rewards?: boolean
  className?: string
}) {
  const { t, i18n } = useTranslation()
  const number = (value: number, digits: number, minDigits = digits) =>
    new Intl.NumberFormat(i18n.language, {
      minimumFractionDigits: minDigits,
      maximumFractionDigits: digits,
    }).format(value)
  const { sessionsDone: done, sessionsPlanned: planned } = progress
  const rpe = progress.averageRpe
  const place = rpe === undefined ? undefined : rpePlace(rpe)
  const first = progress.exercises.at(0)

  return (
    <section
      aria-label={t('progress.tiles.label')}
      className={cn('grid grid-cols-2 gap-3', className)}
    >
      <Tile label={t('progress.sessions.label')}>
        <Figure
          value={number(done, 0)}
          unit={t('progress.sessions.of', { planned: number(planned, 0) })}
        />
        <ProgressBar
          value={planned > 0 ? (done / planned) * 100 : 0}
          aria-label={t('progress.sessions.label')}
          getValueLabel={() =>
            t('progress.sessions.value', { done, planned, count: planned })
          }
          className="*:data-[slot=progress-indicator]:bg-chart-1 h-2"
        />
        <Change
          value={progress.sessionsDoneChange}
          format={(n) => number(n, 0)}
        />
      </Tile>
      <Tile label={t('progress.rpe.label')}>
        {rpe === undefined || place === undefined ? (
          <Empty />
        ) : (
          <>
            <Figure value={number(rpe, 1, 1)} />
            <p className={cn('text-sm', PLACE_COLOUR[place])}>
              {t(`progress.rpe.${place}`)}
            </p>
            <Change
              value={progress.averageRpeChange}
              format={(n) => number(n, 1, 1)}
            />
          </>
        )}
      </Tile>
      {first === undefined ? (
        <Tile label={t('progress.best.none')}>
          <Empty />
        </Tile>
      ) : (
        <BestSetTile exercise={first} rewards={rewards} />
      )}
      {rewards && (
        <Tile label={t('progress.inARow.label')}>
          <Figure
            value={number(progress.weeksInARow, 0)}
            unit={t('progress.inARow.weeks', { count: progress.weeksInARow })}
          />
          <p className="text-muted-foreground text-sm">
            {t('progress.inARow.rule')}
          </p>
        </Tile>
      )}
    </section>
  )
}

function BestSetTile({
  exercise,
  rewards,
}: {
  exercise: ExerciseProgress
  rewards: boolean
}) {
  const { t } = useTranslation()
  const measured = useMeasured(exercise.measure)
  const { best, topSetChange: change } = exercise
  // a set with a load is shown by its load, one without by its repetitions or seconds
  const shown = measured(best.loadKg ?? best.amount, best.loadKg !== undefined)

  return (
    <Tile label={t('progress.best.label', { exercise: exercise.name })}>
      <Figure value={shown.value} unit={shown.unit} />
      {rewards && exercise.prToday && (
        <Badge variant="brand">{t('progress.best.prToday')}</Badge>
      )}
      {change !== undefined && (
        <Change
          value={change.loadKg ?? change.amount}
          format={(size) =>
            t(
              'progress.units.value',
              measured(size, change.loadKg !== undefined),
            )
          }
        />
      )}
    </Tile>
  )
}

function Tile({ label, children }: { label: string; children: ReactNode }) {
  return (
    <Card size="sm" variant="metric">
      <CardContent className="gap-1.5">
        <CardDescription>{label}</CardDescription>
        {children}
      </CardContent>
    </Card>
  )
}

function Figure({ value, unit }: { value: string; unit?: string }) {
  return (
    <p className="flex flex-wrap items-baseline gap-x-1.5">
      <span className="num text-metric font-medium">{value}</span>
      {unit !== undefined && (
        <span className="text-muted-foreground text-sm">{unit}</span>
      )}
    </p>
  )
}

function Empty() {
  const { t } = useTranslation()

  return (
    <>
      <p className="num text-metric font-medium">—</p>
      <p className="text-muted-foreground text-sm">
        {t('progress.tiles.noSets')}
      </p>
    </>
  )
}

/** The design system's KPI delta: an arrow, the size and what is compared; no line when there is nothing to compare. */
function Change({
  value,
  format,
}: {
  value: number | undefined
  format: (size: number) => string
}) {
  const { t } = useTranslation()
  if (value === undefined) return null
  if (value === 0) {
    return (
      <p className="text-muted-foreground text-sm">
        {t('progress.change.same')}
      </p>
    )
  }
  const up = value > 0

  return (
    <p className="text-muted-foreground text-sm">
      {/* the arrow is drawn; its direction is read out in words */}
      <span aria-hidden="true">{up ? '▲' : '▼'} </span>
      <span className="sr-only">
        {up ? t('progress.change.up') : t('progress.change.down')}{' '}
      </span>
      {t('progress.change.weekOnWeek', { change: format(Math.abs(value)) })}
    </p>
  )
}
