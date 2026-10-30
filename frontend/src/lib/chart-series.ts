import type { TFunction } from 'i18next'
import type { ChartConfig } from '@/components/ui/chart'

/** What a line or a bar in a chart means. Each meaning has one colour in every chart, in light and in dark. */
export type SeriesMeaning =
  'completion' | 'rpe' | 'load' | 'wearables' | 'previous'

export const SERIES: Record<
  SeriesMeaning,
  { token: `--chart-${1 | 2 | 3 | 4 | 5}`; dashed: boolean }
> = {
  completion: { token: '--chart-1', dashed: false },
  rpe: { token: '--chart-2', dashed: false },
  load: { token: '--chart-3', dashed: false },
  wearables: { token: '--chart-4', dashed: false },
  previous: { token: '--chart-5', dashed: true },
}

export const MAX_SERIES = 3

export type SeriesEntry = { meaning: SeriesMeaning; dataKey: string }

/** The chart config of one to three series, keyed by meaning, so a chart paints `var(--color-<meaning>)` and never a colour of its own. */
export function chartConfig(
  series: readonly SeriesEntry[],
  t: TFunction,
): ChartConfig {
  if (series.length < 1 || series.length > MAX_SERIES) {
    throw new Error(`a chart has 1 to ${MAX_SERIES} series`)
  }
  if (new Set(series.map((entry) => entry.meaning)).size !== series.length) {
    throw new Error('a meaning appears once in a chart')
  }
  return Object.fromEntries(
    series.map(({ meaning }) => [
      meaning,
      {
        label: t(`charts.series.${meaning}`),
        color: `var(${SERIES[meaning].token})`,
      },
    ]),
  )
}
