import type { TFunction } from 'i18next'
import { describe, expect, it } from 'vitest'
import { chartConfig, SERIES, type SeriesMeaning } from './chart-series'

const t = ((key: string) => `t:${key}`) as unknown as TFunction

describe('SERIES', () => {
  it('gives each of the five meanings its own token', () => {
    const tokens = Object.values(SERIES).map((series) => series.token)

    expect(new Set(tokens).size).toBe(5)
    expect(SERIES.completion.token).toBe('--chart-1')
    expect(SERIES.rpe.token).toBe('--chart-2')
    expect(SERIES.load.token).toBe('--chart-3')
    expect(SERIES.wearables.token).toBe('--chart-4')
    expect(SERIES.previous.token).toBe('--chart-5')
  })

  it('dashes the previous period and nothing else', () => {
    const dashed = (Object.keys(SERIES) as SeriesMeaning[]).filter(
      (meaning) => SERIES[meaning].dashed,
    )

    expect(dashed).toEqual(['previous'])
  })
})

describe('chartConfig', () => {
  it('keys the config by meaning, with the translated name and the colour of the token', () => {
    const config = chartConfig(
      [
        { meaning: 'completion', dataKey: 'done' },
        { meaning: 'previous', dataKey: 'before' },
      ],
      t,
    )

    expect(config).toEqual({
      completion: {
        label: 't:charts.series.completion',
        color: 'var(--chart-1)',
      },
      previous: { label: 't:charts.series.previous', color: 'var(--chart-5)' },
    })
  })

  it('refuses a fourth series', () => {
    expect(() =>
      chartConfig(
        [
          { meaning: 'completion', dataKey: 'a' },
          { meaning: 'rpe', dataKey: 'b' },
          { meaning: 'load', dataKey: 'c' },
          { meaning: 'wearables', dataKey: 'd' },
        ],
        t,
      ),
    ).toThrow()
  })

  it('refuses a chart with no series', () => {
    expect(() => chartConfig([], t)).toThrow()
  })

  it('refuses the same meaning twice', () => {
    expect(() =>
      chartConfig(
        [
          { meaning: 'rpe', dataKey: 'a' },
          { meaning: 'rpe', dataKey: 'b' },
        ],
        t,
      ),
    ).toThrow()
  })
})
