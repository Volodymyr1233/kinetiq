/**
 * The five chart series of the design system's section 3.7, by meaning. A series keeps its colour and line style in
 * every chart, so `color` goes into a `ChartConfig` and the stroke values onto the series' `Line`.
 */
export const chartSeries = {
  completion: { color: 'var(--chart-1)', strokeWidth: 2.5 },
  rpe: { color: 'var(--chart-2)', strokeWidth: 2.5 },
  load: { color: 'var(--chart-3)', strokeWidth: 2 },
  wearables: {
    color: 'var(--chart-4)',
    strokeWidth: 2,
    strokeDasharray: '2 3',
  },
  previous: { color: 'var(--chart-5)', strokeWidth: 2, strokeDasharray: '4 4' },
} as const
