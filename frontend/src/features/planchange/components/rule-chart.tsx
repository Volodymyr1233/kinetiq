import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  CartesianGrid,
  LabelList,
  Line,
  LineChart,
  ReferenceLine,
  XAxis,
  YAxis,
} from 'recharts'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from '@/components/ui/chart'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { chartSeries } from '@/lib/chart-series'
import { formatShortDate, localDate } from '@/lib/dates'
import { fromKg, useWeightUnit } from '@/lib/weight'
import type { PlanChange } from '../api/plan-change'

// a week without logs has no value, so the line breaks there instead of dropping to zero
type Point = { week: string; value: number | null }
type Series = { exercise?: string; points: Point[]; threshold: number }
type ChartedPlanChange = Pick<
  PlanChange,
  'rule' | 'figures' | 'concernedExercises' | 'windowStart' | 'windowEnd'
>

// the thresholds the rules compare against (spec 125)
const RPE_LIMIT = 8
const REQUIRED_RISE = 1.05
const COMPLETION_TARGET = 0.7

const SERIES = {
  HIGH_RPE: chartSeries.rpe,
  STALLED_WEIGHT: chartSeries.load,
  LOW_COMPLETION: chartSeries.completion,
} as const

const isoDate = (date: Date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

// every Monday of the window the rule looked at, and any week a figure is for outside it
function weeksOf(planChange: ChartedPlanChange) {
  const weeks = new Set(planChange.figures.map((figure) => figure.weekStart))
  const last = localDate(planChange.windowEnd)
  for (
    const week = localDate(planChange.windowStart);
    week <= last;
    week.setDate(week.getDate() + 7)
  ) {
    weeks.add(isoDate(week))
  }
  return [...weeks].sort()
}

/**
 * The numbers that made the rule fire, one point a week, against the rule's threshold as a dashed line; a week without
 * logs is a gap. The chart's label says every value in words, and the values can be shown as a table. Stalled weight
 * shows a chart for each exercise the rule found. `compact` is the phone's version: the same chart without its card,
 * axis scale and legend.
 */
export function RuleChart({
  planChange,
  compact = false,
}: {
  planChange: ChartedPlanChange
  compact?: boolean
}) {
  const { t, i18n } = useTranslation()
  const unit = useWeightUnit()
  const locale = i18n.language
  const rule = planChange.rule
  const decimal = new Intl.NumberFormat(locale, { maximumFractionDigits: 1 })
  const share = new Intl.NumberFormat(locale, {
    style: 'percent',
    maximumFractionDigits: 0,
  })
  const weeks = weeksOf(planChange)
  const of = (kind: PlanChange['figures'][number]['kind']) =>
    planChange.figures.filter((figure) => figure.kind === kind)
  // the value a figure gives for a week, or nothing
  const on = (
    figures: PlanChange['figures'],
    week: string,
  ): number | undefined =>
    figures.find((figure) => figure.weekStart === week)?.value

  // stalled weight draws one chart per exercise the rule found; the other rules one chart for the week figures
  const series: Series[] = []
  if (rule === 'HIGH_RPE') {
    const rpe = of('MEAN_RPE')
    series.push({
      points: weeks.map((week) => ({ week, value: on(rpe, week) ?? null })),
      threshold: RPE_LIMIT,
    })
  } else if (rule === 'STALLED_WEIGHT') {
    for (const exercise of planChange.concernedExercises) {
      const best = of('BEST_LOAD_KG').filter(
        (figure) => figure.exerciseId === exercise.id,
      )
      const points = weeks.map((week) => {
        const load = on(best, week)
        return { week, value: load === undefined ? null : fromKg(load, unit) }
      })
      const first = points.find((point) => point.value !== null)
      series.push({
        exercise: exercise.name,
        points,
        threshold: (first?.value ?? 0) * REQUIRED_RISE,
      })
    }
  } else {
    const planned = of('SESSIONS_PLANNED')
    const finished = of('SESSIONS_FINISHED')
    series.push({
      points: weeks.map((week) => {
        const plan = on(planned, week)
        const done = on(finished, week)
        return {
          week,
          value:
            plan !== undefined && plan > 0 && done !== undefined
              ? done / plan
              : null,
        }
      }),
      threshold: COMPLETION_TARGET,
    })
  }
  const format = (value: number) =>
    rule === 'HIGH_RPE'
      ? decimal.format(value)
      : rule === 'STALLED_WEIGHT'
        ? t('planChange.values.load', {
            load: decimal.format(value),
            unit: t(`plan.units.${unit}`),
          })
        : share.format(value)

  const charts = series.map((one) => (
    <SeriesChart
      key={one.exercise ?? rule}
      rule={rule}
      series={one}
      format={format}
      compact={compact}
    />
  ))

  if (compact) return <div className="flex flex-col gap-3">{charts}</div>

  return (
    <Card>
      <CardHeader>
        <CardTitle>
          <h2>{t('planChange.chart.title')}</h2>
        </CardTitle>
        <CardDescription>
          {t(`planChange.chart.subtitle.${rule}`)}
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">{charts}</CardContent>
    </Card>
  )
}

/** One series against its threshold, with its legend and, off the phone, its values as a table. */
function SeriesChart({
  rule,
  series,
  format,
  compact,
}: {
  rule: PlanChange['rule']
  series: Series
  format: (value: number) => string
  compact: boolean
}) {
  const { t, i18n } = useTranslation()
  const [table, setTable] = useState(false)
  const data = series.points.map((point) => ({
    ...point,
    label: formatShortDate(point.week, i18n.language),
  }))
  const values = data
    .map((point) =>
      point.value === null
        ? t('planChange.chart.noLogs', { week: point.label })
        : t('planChange.chart.point', {
            week: point.label,
            value: format(point.value),
          }),
    )
    .join(', ')
  const config = {
    value: {
      label: t(`planChange.chart.series.${rule}`, {
        exercise: series.exercise,
      }),
      color: SERIES[rule].color,
    },
  } satisfies ChartConfig
  const present = data.flatMap((point) =>
    point.value === null ? [] : [point.value],
  )
  const all = [...present, series.threshold]
  const domain =
    rule === 'LOW_COMPLETION'
      ? [0, 1]
      : [
          Math.floor(Math.min(...all) * 0.95),
          Math.ceil(Math.max(...all) * 1.02),
        ]

  return (
    <div className="flex flex-col gap-2">
      <ChartContainer
        config={config}
        role="img"
        aria-label={t('planChange.chart.description', {
          series: config.value.label,
          values,
          threshold: format(series.threshold),
        })}
        className={
          compact
            ? 'num aspect-auto h-24 w-full'
            : 'num aspect-auto h-44 w-full'
        }
      >
        <LineChart
          data={data}
          margin={{
            top: 20,
            right: compact ? 16 : 28,
            bottom: 0,
            left: compact ? 16 : 0,
          }}
          // the label holds the numbers for screen readers
          accessibilityLayer={false}
        >
          {!compact && (
            <CartesianGrid vertical={false} stroke="var(--border)" />
          )}
          <XAxis
            dataKey="label"
            interval={0}
            padding={{ left: 16, right: 16 }}
            tickLine={false}
            axisLine={false}
            tickMargin={8}
          />
          <YAxis
            domain={domain}
            hide={compact}
            tickFormatter={format}
            width={rule === 'STALLED_WEIGHT' ? 64 : 44}
            tickLine={false}
            axisLine={false}
          />
          <ChartTooltip
            cursor={false}
            content={
              <ChartTooltipContent
                hideIndicator
                formatter={(value) => (
                  <span className="flex items-center gap-2">
                    <span className="text-muted-foreground">
                      {config.value.label}
                    </span>
                    <span className="font-medium">{format(Number(value))}</span>
                  </span>
                )}
              />
            }
          />
          <ReferenceLine
            y={series.threshold}
            stroke="var(--muted-foreground)"
            strokeDasharray="4 4"
          />
          <Line
            dataKey="value"
            type="linear"
            stroke="var(--color-value)"
            strokeWidth={SERIES[rule].strokeWidth}
            dot={{ r: 4, fill: 'var(--color-value)' }}
            connectNulls={false}
            isAnimationActive={false}
          >
            {/* each point says its value, so nothing needs the legend or the hover to be read */}
            <LabelList
              dataKey="value"
              position="top"
              offset={8}
              className="fill-foreground text-xs"
              formatter={(value: unknown) =>
                typeof value === 'number' ? format(value) : ''
              }
            />
          </Line>
        </LineChart>
      </ChartContainer>
      <ul className="text-muted-foreground flex flex-wrap gap-x-4 gap-y-1 text-sm">
        {!compact && (
          <li className="flex items-center gap-2">
            <span
              aria-hidden
              className="inline-block h-0.5 w-4"
              style={{ background: SERIES[rule].color }}
            />
            {config.value.label}
          </li>
        )}
        <li className="flex items-center gap-2">
          <span
            aria-hidden
            className="border-muted-foreground inline-block w-4 border-t-2 border-dashed"
          />
          {t('planChange.chart.threshold', { value: format(series.threshold) })}
        </li>
      </ul>
      {!compact && (
        <>
          <Button
            variant="ghost"
            size="sm"
            className="self-start"
            aria-pressed={table}
            onClick={() => setTable(!table)}
          >
            {table
              ? t('planChange.chart.hideTable')
              : t('planChange.chart.showTable')}
          </Button>
          {table && (
            <Table data-density="compact">
              <TableHeader>
                <TableRow>
                  <TableHead>{t('planChange.chart.week')}</TableHead>
                  <TableHead className="text-end">
                    {config.value.label}
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.map((point) => (
                  <TableRow key={point.week}>
                    <TableCell>{point.label}</TableCell>
                    <TableCell className="num text-end">
                      {point.value === null
                        ? t('planChange.chart.noLogsShort')
                        : format(point.value)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </>
      )}
    </div>
  )
}
