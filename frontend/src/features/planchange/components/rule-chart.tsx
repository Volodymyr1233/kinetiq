import { useTranslation } from 'react-i18next'
import { Line, LineChart, ReferenceLine, XAxis, YAxis } from 'recharts'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { ChartContainer, type ChartConfig } from '@/components/ui/chart'
import { chartSeries } from '@/lib/chart-series'
import { formatShortDate } from '@/lib/dates'
import { fromKg, useWeightUnit } from '@/lib/weight'
import type { PlanChange } from '../api/plan-change'

type Point = { week: string; value: number }
type Series = { exercise?: string; points: Point[]; threshold: number }

// the thresholds the rules compare against (spec 125)
const RPE_LIMIT = 8
const REQUIRED_RISE = 1.05
const COMPLETION_TARGET = 0.7

const SERIES = {
  HIGH_RPE: chartSeries.rpe,
  STALLED_WEIGHT: chartSeries.load,
  LOW_COMPLETION: chartSeries.completion,
} as const

/**
 * The numbers that made the rule fire, one point a week, against the rule's threshold as a dashed line. The chart's
 * label says every value in words. Stalled weight shows a chart for each exercise the rule found.
 */
export function RuleChart({ planChange }: { planChange: PlanChange }) {
  const { t, i18n } = useTranslation()
  const unit = useWeightUnit()
  const locale = i18n.language
  const rule = planChange.rule
  const decimal = new Intl.NumberFormat(locale, { maximumFractionDigits: 1 })
  const share = new Intl.NumberFormat(locale, {
    style: 'percent',
    maximumFractionDigits: 0,
  })
  const of = (kind: PlanChange['figures'][number]['kind']) =>
    planChange.figures
      .filter((figure) => figure.kind === kind)
      .sort((a, b) => a.weekStart.localeCompare(b.weekStart))

  // stalled weight draws one chart per exercise the rule found; the other rules one chart for the week figures
  const series: Series[] = []
  if (rule === 'HIGH_RPE') {
    series.push({
      points: of('MEAN_RPE').map((figure) => ({
        week: figure.weekStart,
        value: figure.value,
      })),
      threshold: RPE_LIMIT,
    })
  } else if (rule === 'STALLED_WEIGHT') {
    for (const exercise of planChange.concernedExercises) {
      const points = of('BEST_LOAD_KG')
        .filter((figure) => figure.exerciseId === exercise.id)
        .map((figure) => ({
          week: figure.weekStart,
          value: fromKg(figure.value, unit),
        }))
      series.push({
        exercise: exercise.name,
        points,
        threshold: (points[0]?.value ?? 0) * REQUIRED_RISE,
      })
    }
  } else {
    const planned = of('SESSIONS_PLANNED')
    series.push({
      points: of('SESSIONS_FINISHED').map((figure) => {
        const week = planned.find((plan) => plan.weekStart === figure.weekStart)
        return {
          week: figure.weekStart,
          value: week && week.value > 0 ? figure.value / week.value : 0,
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
      <CardContent className="flex flex-col gap-3">
        {series.map((one) => (
          <SeriesChart
            key={one.exercise ?? rule}
            rule={rule}
            series={one}
            format={format}
          />
        ))}
      </CardContent>
    </Card>
  )
}

/** One series against its threshold, with its legend. */
function SeriesChart({
  rule,
  series,
  format,
}: {
  rule: PlanChange['rule']
  series: Series
  format: (value: number) => string
}) {
  const { t, i18n } = useTranslation()
  const data = series.points.map((point) => ({
    ...point,
    label: formatShortDate(point.week, i18n.language),
  }))
  const values = data
    .map((point) =>
      t('planChange.chart.point', {
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
  const all = [...data.map((point) => point.value), series.threshold]
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
        className="num aspect-auto h-44 w-full"
      >
        <LineChart
          data={data}
          margin={{ top: 16, right: 28, bottom: 0, left: 0 }}
          // the label holds the numbers for screen readers
          accessibilityLayer={false}
        >
          <XAxis
            dataKey="label"
            interval={0}
            tickLine={false}
            axisLine={false}
            tickMargin={8}
          />
          <YAxis
            domain={domain}
            tickFormatter={format}
            width={rule === 'STALLED_WEIGHT' ? 64 : 44}
            tickLine={false}
            axisLine={false}
          />
          <ReferenceLine
            y={series.threshold}
            stroke="var(--foreground)"
            strokeDasharray="3 3"
          />
          <Line
            dataKey="value"
            type="linear"
            stroke="var(--color-value)"
            strokeWidth={SERIES[rule].strokeWidth}
            dot={{ r: 4, fill: 'var(--color-value)' }}
            isAnimationActive={false}
          />
        </LineChart>
      </ChartContainer>
      <ul className="text-muted-foreground flex flex-wrap gap-x-4 gap-y-1 text-sm">
        <li className="flex items-center gap-2">
          <span
            aria-hidden
            className="inline-block h-0.5 w-4"
            style={{ background: SERIES[rule].color }}
          />
          {config.value.label}
        </li>
        <li className="flex items-center gap-2">
          <span
            aria-hidden
            className="border-foreground inline-block w-4 border-t-2 border-dashed"
          />
          {t('planChange.chart.threshold', { value: format(series.threshold) })}
        </li>
      </ul>
    </div>
  )
}
