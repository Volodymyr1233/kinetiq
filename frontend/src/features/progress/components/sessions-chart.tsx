import { useTranslation } from 'react-i18next'
import {
  Bar,
  BarChart,
  Rectangle,
  ReferenceLine,
  XAxis,
  YAxis,
  type BarShapeProps,
} from 'recharts'
import { ChartContainer, type ChartConfig } from '@/components/ui/chart'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { chartSeries } from '@/lib/chart-series'
import type { ProgressWeek } from '../api/progress'
import { weeksAtTarget } from '../findings'
import { ChartCard } from './chart-card'

// the completion below which the low completion rule calls for a plan change (spec 125)
const TARGET = 0.7

// a lighter bar for the week still running, outlined since the light fill alone is under 3:1 against the card; the
// line under the chart says it in words
const RunningAwareBar = (props: BarShapeProps) =>
  (props.payload as { running: boolean }).running ? (
    <Rectangle
      {...props}
      fillOpacity={0.45}
      stroke={props.fill}
      strokeWidth={1.5}
    />
  ) : (
    <Rectangle {...props} />
  )

/**
 * The sessions done each week of the period as a share of the sessions planned, against the 70% line; `compact` gives
 * its table the trainer's density.
 */
export function SessionsChart({
  weeks,
  compact = false,
}: {
  weeks: ProgressWeek[]
  compact?: boolean
}) {
  const { t, i18n } = useTranslation()
  const config = {
    share: {
      label: t('progress.sessionsChart.done'),
      color: chartSeries.completion.color,
    },
  } satisfies ChartConfig
  const data = weeks.map((week) => ({
    week: week.number,
    share: week.planned > 0 ? week.done / week.planned : 0,
    running: week.running,
  }))
  const { atTarget, of } = weeksAtTarget(weeks)
  const title =
    of === 0
      ? t('progress.sessionsChart.noneCompleted')
      : t('progress.sessionsChart.title', { atTarget, count: of })
  const running = weeks.find((week) => week.running)
  const left =
    running &&
    t('progress.sessionsChart.left', {
      week: running.number,
      count: running.planned - running.done,
    })

  return (
    <ChartCard
      title={title}
      subtitle={t('progress.sessionsChart.subtitle')}
      chart={
        <ChartContainer
          config={config}
          role="img"
          aria-label={
            left === undefined
              ? title
              : t('progress.sessionsChart.description', { title, left })
          }
          className="num aspect-auto h-40 w-full"
        >
          <BarChart
            data={data}
            margin={{ top: 16, right: 0, bottom: 0, left: 0 }}
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
            <YAxis hide domain={[0, 1]} />
            <Bar
              dataKey="share"
              fill="var(--color-share)"
              radius={[4, 4, 0, 0]}
              shape={RunningAwareBar}
            />
            <ReferenceLine
              y={TARGET}
              stroke="var(--foreground)"
              strokeDasharray="3 3"
              label={{
                value: new Intl.NumberFormat(i18n.language, {
                  style: 'percent',
                }).format(TARGET),
                position: 'insideBottomRight',
                fill: 'var(--muted-foreground)',
              }}
            />
          </BarChart>
        </ChartContainer>
      }
      table={
        <Table data-density={compact ? 'compact' : undefined}>
          <TableHeader>
            <TableRow>
              <TableHead>{t('progress.sessionsChart.week')}</TableHead>
              <TableHead className="text-end">
                {t('progress.sessionsChart.done')}
              </TableHead>
              <TableHead className="text-end">
                {t('progress.sessionsChart.planned')}
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody className="num">
            {weeks.map((week) => (
              <TableRow key={week.number}>
                <TableCell>{week.number}</TableCell>
                <TableCell className="text-end">{week.done}</TableCell>
                <TableCell className="text-end">{week.planned}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      }
      note={
        left !== undefined && (
          <p className="text-muted-foreground text-sm">{left}</p>
        )
      }
    />
  )
}
