import { useId, useSyncExternalStore } from 'react'
import { useTranslation } from 'react-i18next'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  XAxis,
  YAxis,
} from 'recharts'
import {
  ChartContainer,
  ChartLegend,
  ChartLegendContent,
  ChartTooltip,
  ChartTooltipContent,
} from '@/components/ui/chart'
import {
  chartConfig,
  SERIES,
  type SeriesEntry,
  type SeriesMeaning,
} from '@/lib/chart-series'

type Row = Record<string, string | number>

// one to three series: a fourth is a type error here and an error in chartConfig
type SeriesList =
  | readonly [SeriesEntry]
  | readonly [SeriesEntry, SeriesEntry]
  | readonly [SeriesEntry, SeriesEntry, SeriesEntry]

type FindingChartProps = {
  /** The finding as a sentence ("You trained 4 of 5 planned sessions"), not only the subject. */
  title: string
  kind: 'line' | 'bar'
  data: readonly Row[]
  xKey: string
  /** Heading of the x column in the text version of the data. */
  xLabel: string
  series: SeriesList
  /** Shown instead of an empty chart. */
  emptyText: string
}

const REDUCED_MOTION = '(prefers-reduced-motion: reduce)'

function usePrefersReducedMotion() {
  return useSyncExternalStore(
    (notify) => {
      const query = window.matchMedia(REDUCED_MOTION)
      query.addEventListener('change', notify)
      return () => query.removeEventListener('change', notify)
    },
    () => window.matchMedia(REDUCED_MOTION).matches,
    () => false,
  )
}

const paint = (meaning: SeriesMeaning) => `var(--color-${meaning})`

/** A titled line or bar chart of one to three series whose colours come from their meaning, with a text version of its data. */
export function FindingChart({
  title,
  kind,
  data,
  xKey,
  xLabel,
  series,
  emptyText,
}: FindingChartProps) {
  const { t } = useTranslation()
  const titleId = useId()
  const reducedMotion = usePrefersReducedMotion()
  if (title.trim() === '') {
    throw new Error('a chart needs a title that states the finding')
  }
  const config = chartConfig(series, t)

  return (
    <figure aria-labelledby={titleId} className="flex flex-col gap-3">
      <h3 id={titleId} className="text-base font-semibold tracking-tight">
        {title}
      </h3>
      {data.length === 0 ? (
        <p className="text-muted-foreground text-sm">{emptyText}</p>
      ) : (
        <>
          <ChartContainer config={config} className="aspect-[4/3] w-full">
            {kind === 'line' ? (
              <LineChart accessibilityLayer data={[...data]}>
                <CartesianGrid vertical={false} />
                <XAxis dataKey={xKey} tickLine={false} axisLine={false} />
                <YAxis tickLine={false} axisLine={false} width={32} />
                <ChartTooltip content={<ChartTooltipContent />} />
                <ChartLegend content={<ChartLegendContent nameKey="value" />} />
                {series.map(({ meaning, dataKey }) => (
                  <Line
                    key={meaning}
                    dataKey={dataKey}
                    name={meaning}
                    type="monotone"
                    stroke={paint(meaning)}
                    strokeWidth={2}
                    strokeDasharray={SERIES[meaning].dashed ? '6 4' : undefined}
                    isAnimationActive={!reducedMotion}
                  />
                ))}
              </LineChart>
            ) : (
              <BarChart accessibilityLayer data={[...data]}>
                <CartesianGrid vertical={false} />
                <XAxis dataKey={xKey} tickLine={false} axisLine={false} />
                <YAxis tickLine={false} axisLine={false} width={32} />
                <ChartTooltip content={<ChartTooltipContent />} />
                <ChartLegend content={<ChartLegendContent nameKey="value" />} />
                {series.map(({ meaning, dataKey }) => (
                  <Bar
                    key={meaning}
                    dataKey={dataKey}
                    name={meaning}
                    fill={paint(meaning)}
                    // the previous period is the dashed, lighter one, so it is told by more than colour
                    fillOpacity={SERIES[meaning].dashed ? 0.4 : 1}
                    stroke={SERIES[meaning].dashed ? paint(meaning) : undefined}
                    strokeDasharray={SERIES[meaning].dashed ? '4 3' : undefined}
                    radius={4}
                    isAnimationActive={!reducedMotion}
                  />
                ))}
              </BarChart>
            )}
          </ChartContainer>
          <table className="sr-only">
            <caption>{title}</caption>
            <thead>
              <tr>
                <th scope="col">{xLabel}</th>
                {series.map(({ meaning }) => (
                  <th key={meaning} scope="col">
                    {t(`charts.series.${meaning}`)}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {data.map((row, index) => (
                <tr key={index}>
                  <th scope="row">{row[xKey]}</th>
                  {series.map(({ meaning, dataKey }) => (
                    <td key={meaning}>{row[dataKey]}</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </figure>
  )
}
