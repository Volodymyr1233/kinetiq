import { useId, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Line,
  LineChart,
  ReferenceArea,
  XAxis,
  YAxis,
  type LabelProps,
} from 'recharts'
import { Card, CardContent } from '@/components/ui/card'
import { ChartContainer, type ChartConfig } from '@/components/ui/chart'
import {
  Table,
  TableBody,
  TableCaption,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { chartSeries } from '@/lib/chart-series'
import { fromKg, useWeightUnit } from '@/lib/weight'
import type { ExerciseProgress as Exercise, TopSet } from '../api/progress'
import { topSetTrend } from '../findings'
import { useMeasured } from '../use-measured'
import { ChartCard } from './chart-card'

const RPE_AXIS = [5, 10]

/**
 * "By exercise": the period's exercises to choose from, the chosen one's top set and average RPE week by week, and its
 * last sessions. `weeks` are the period's week numbers, so a week without the exercise leaves a gap; `compact` gives
 * the tables the trainer's density.
 */
export function ExerciseProgress({
  exercises,
  weeks,
  compact = false,
}: {
  exercises: Exercise[]
  weeks: number[]
  compact?: boolean
}) {
  const { t } = useTranslation()
  const headingId = useId()
  const [chosenId, setChosenId] = useState<string>()
  // another period may not have the exercise chosen before; the first one stands in
  const chosen =
    exercises.find((exercise) => exercise.exerciseId === chosenId) ??
    exercises.at(0)
  if (chosen === undefined) return null

  return (
    <section aria-labelledby={headingId} className="flex flex-col gap-3">
      <h2 id={headingId} className="font-heading text-base font-medium">
        {t('progress.exercise.title')}
      </h2>
      <ToggleGroup
        type="single"
        variant="outline"
        spacing={2}
        aria-labelledby={headingId}
        value={chosen.exerciseId}
        // choosing the chosen chip again gives an empty value; one exercise always stays chosen
        onValueChange={(next) => next && setChosenId(next)}
        className="flex-wrap"
      >
        {exercises.map((exercise) => (
          <ToggleGroupItem
            key={exercise.exerciseId}
            value={exercise.exerciseId}
            className="data-[state=on]:bg-primary data-[state=on]:text-primary-foreground data-[state=on]:hover:bg-primary data-[state=on]:hover:text-primary-foreground h-12 rounded-full px-4"
          >
            {exercise.name}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
      <ExerciseChart exercise={chosen} weeks={weeks} compact={compact} />
      <RecentSessions exercise={chosen} compact={compact} />
    </section>
  )
}

function ExerciseChart({
  exercise,
  weeks,
  compact,
}: {
  exercise: Exercise
  weeks: number[]
  compact: boolean
}) {
  const { t, i18n } = useTranslation()
  const weightUnit = useWeightUnit()
  const measured = useMeasured(exercise.measure)
  const trend = topSetTrend(exercise.weeks)
  const lastWeek = exercise.weeks.at(-1)
  if (trend === undefined || lastWeek === undefined) return null

  const rpe = (value: number) =>
    new Intl.NumberFormat(i18n.language, {
      minimumFractionDigits: 1,
      maximumFractionDigits: 1,
    }).format(value)
  const size = (value: number, isLoad = trend.inLoad) =>
    t('progress.units.value', measured(value, isLoad))
  const shown = (top: TopSet) =>
    size(top.loadKg ?? top.amount, top.loadKg !== undefined)

  const config = {
    top: {
      label: t('progress.exercise.topSet'),
      color: chartSeries.load.color,
    },
    rpe: {
      label: t('progress.exercise.rpe'),
      color: chartSeries.rpe.color,
    },
  } satisfies ChartConfig
  const byNumber = new Map(exercise.weeks.map((week) => [week.number, week]))
  const data = weeks.map((number) => {
    const week = byNumber.get(number)
    const top = trend.inLoad ? week?.top.loadKg : week?.top.amount
    return {
      week: number,
      // a gap, not a zero, for a week without the exercise or, in load, without a load
      top:
        top === undefined ? null : trend.inLoad ? fromKg(top, weightUnit) : top,
      rpe: week?.averageRpe ?? null,
    }
  })
  const tops = data.flatMap((point) => (point.top === null ? [] : [point.top]))
  const low = Math.min(...tops)
  const high = Math.max(...tops)
  // room above and below the line, also when it is flat
  const pad = (high - low) / 4 || Math.max(high / 10, 1)
  const topDomain = [low - pad, high + pad]
  const lastTop = tops.at(-1) ?? low
  // the higher line end is labelled above its point and the lower below, so the labels stay apart where the lines meet
  const topAbove =
    (lastTop - topDomain[0]) / (topDomain[1] - topDomain[0]) >=
    (lastWeek.averageRpe - RPE_AXIS[0]) / (RPE_AXIS[1] - RPE_AXIS[0])

  const title =
    trend.direction === 'held'
      ? t('progress.exercise.held', { value: size(trend.last) })
      : t(`progress.exercise.${trend.direction}`, {
          change: size(trend.change),
          count: trend.weeks,
        })
  const subtitle = trend.inLoad
    ? t('progress.exercise.subtitle.load', {
        unit: t(`plan.units.${weightUnit}`),
      })
    : t(
        `progress.exercise.subtitle.${exercise.measure === 'SECONDS' ? 'seconds' : 'reps'}`,
      )

  return (
    <ChartCard
      level={3}
      title={title}
      subtitle={subtitle}
      chart={
        <ChartContainer
          config={config}
          role="img"
          aria-label={t('progress.exercise.description', {
            exercise: exercise.name,
            title,
            rpe: rpe(lastWeek.averageRpe),
            week: lastWeek.number,
          })}
          className="num aspect-auto h-48 w-full"
        >
          <LineChart
            data={data}
            margin={{ top: 16, right: 64, bottom: 0, left: 0 }}
            // the table holds the numbers for the keyboard and screen readers
            accessibilityLayer={false}
          >
            <XAxis
              dataKey="week"
              interval={0}
              tickLine={false}
              axisLine={false}
              tickMargin={8}
            />
            <YAxis yAxisId="top" hide domain={topDomain} />
            <YAxis
              yAxisId="rpe"
              domain={RPE_AXIS}
              ticks={RPE_AXIS}
              width={28}
              tickLine={false}
              axisLine={false}
            />
            <ReferenceArea
              yAxisId="rpe"
              y1={7}
              y2={8}
              fill="var(--color-rpe)"
              fillOpacity={0.1}
              // beside the RPE axis: at the right edge the label met the lines' last points, which mostly end in the band
              label={{
                value: t('progress.exercise.band'),
                position: 'left',
                fill: 'var(--muted-foreground)',
              }}
            />
            <Line
              yAxisId="top"
              dataKey="top"
              stroke="var(--color-top)"
              strokeWidth={chartSeries.load.strokeWidth}
              dot={{ r: 3, fill: 'var(--color-top)', strokeWidth: 0 }}
              activeDot={false}
              label={endLabel(
                lastIndex(data, 'top'),
                size(trend.last),
                topAbove,
              )}
            />
            <Line
              yAxisId="rpe"
              dataKey="rpe"
              stroke="var(--color-rpe)"
              strokeWidth={chartSeries.rpe.strokeWidth}
              dot={{ r: 3, fill: 'var(--color-rpe)', strokeWidth: 0 }}
              activeDot={false}
              label={endLabel(
                lastIndex(data, 'rpe'),
                t('progress.exercise.rpeValue', {
                  rpe: rpe(lastWeek.averageRpe),
                }),
                !topAbove,
              )}
            />
          </LineChart>
        </ChartContainer>
      }
      table={
        <Table data-density={compact ? 'compact' : undefined}>
          <TableHeader>
            <TableRow>
              <TableHead>{t('progress.exercise.week')}</TableHead>
              <TableHead className="text-end">
                {t('progress.exercise.topSet')}
              </TableHead>
              <TableHead className="text-end">
                {t('progress.exercise.rpe')}
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody className="num">
            {exercise.weeks.map((week) => (
              <TableRow key={week.number}>
                <TableCell>{week.number}</TableCell>
                <TableCell className="text-end">{shown(week.top)}</TableCell>
                <TableCell className="text-end">
                  {rpe(week.averageRpe)}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      }
    />
  )
}

function RecentSessions({
  exercise,
  compact,
}: {
  exercise: Exercise
  compact: boolean
}) {
  const { t, i18n } = useTranslation()
  const measured = useMeasured(exercise.measure)
  // plain dates, read in UTC so no time zone can move the day
  const date = new Intl.DateTimeFormat(i18n.language, {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    timeZone: 'UTC',
  })

  return (
    <Card>
      <CardContent>
        <Table data-density={compact ? 'compact' : undefined}>
          <TableCaption className="sr-only">
            {t('progress.exercise.recent', { exercise: exercise.name })}
          </TableCaption>
          <TableHeader>
            <TableRow>
              <TableHead>{t('progress.exercise.date')}</TableHead>
              <TableHead className="text-end">
                {t('progress.exercise.topSet')}
              </TableHead>
              <TableHead className="text-end">
                {t('progress.exercise.rpe')}
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {exercise.recent.map(({ date: day, top }, index) => (
              // two sessions can share a day
              <TableRow key={`${day}-${index}`}>
                <TableCell>
                  {date.format(new Date(`${day}T00:00:00Z`))}
                </TableCell>
                <TableCell className="num text-end">
                  {t(
                    'progress.units.value',
                    measured(
                      top.loadKg ?? top.amount,
                      top.loadKg !== undefined,
                    ),
                  )}
                </TableCell>
                <TableCell className="num text-end">{top.rpe}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  )
}

const lastIndex = (
  data: { top: number | null; rpe: number | null }[],
  key: 'top' | 'rpe',
) => data.findLastIndex((point) => point[key] !== null)

// a line's last value beside its last point, in place of a legend (design system, section 3.7)
const endLabel =
  (index: number, text: string, above: boolean) => (props: LabelProps) =>
    props.index === index ? (
      <text
        x={Number(props.x) + 6}
        y={Number(props.y)}
        dy={above ? -6 : 14}
        fill="var(--foreground)"
        className="text-xs"
      >
        {text}
      </text>
    ) : null
