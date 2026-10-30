import { render, screen, within } from '@testing-library/react'
import type { ComponentProps } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import i18n from '@/lib/i18n'
import { FindingChart } from './finding-chart'

// a pass-through over Recharts' Line that records how the chart asked it to animate
type DrawnLine = {
  name?: string | number
  stroke?: string
  strokeDasharray?: string | number
  isAnimationActive?: boolean
}
type DrawnBar = DrawnLine & { fill?: string; fillOpacity?: number }
const drawn = vi.hoisted(() => ({
  lines: [] as DrawnLine[],
  bars: [] as DrawnBar[],
}))
vi.mock('recharts', async (importOriginal) => {
  const actual = await importOriginal<typeof import('recharts')>()
  const Line = (props: ComponentProps<typeof actual.Line>) => {
    drawn.lines.push(props as DrawnLine)
    return <actual.Line {...props} />
  }
  const Bar = (props: ComponentProps<typeof actual.Bar>) => {
    drawn.bars.push(props as DrawnBar)
    return <actual.Bar {...props} />
  }
  return { ...actual, Line, Bar }
})

const DATA = [
  { week: 'W1', done: 3, before: 2 },
  { week: 'W2', done: 4, before: 3 },
]

const SERIES = [
  { meaning: 'completion', dataKey: 'done' },
  { meaning: 'previous', dataKey: 'before' },
] as const

function chart(props: Partial<Parameters<typeof FindingChart>[0]> = {}) {
  return (
    <FindingChart
      title="You trained 4 of 5 planned sessions"
      kind="line"
      data={DATA}
      xKey="week"
      xLabel="Week"
      series={SERIES}
      emptyText="No sessions logged yet"
      {...props}
    />
  )
}

// jsdom has no layout: a resize observer that reports a size lets Recharts draw its children
class SizedObserver {
  callback: ResizeObserverCallback
  constructor(callback: ResizeObserverCallback) {
    this.callback = callback
  }
  observe(target: Element) {
    const rect = { width: 400, height: 300 } as DOMRectReadOnly
    this.callback(
      [{ target, contentRect: rect } as ResizeObserverEntry],
      this as unknown as ResizeObserver,
    )
  }
  unobserve() {}
  disconnect() {}
}

beforeEach(() => {
  drawn.lines.length = 0
  drawn.bars.length = 0
  vi.stubGlobal('ResizeObserver', SizedObserver)
})

afterEach(async () => {
  await i18n.changeLanguage('en')
})

describe('FindingChart', () => {
  it('shows the finding as a heading that names the figure', () => {
    render(chart())

    expect(
      screen.getByRole('heading', {
        name: 'You trained 4 of 5 planned sessions',
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('figure', {
        name: 'You trained 4 of 5 planned sessions',
      }),
    ).toBeInTheDocument()
  })

  it('gives the data as a table, with every series named in words', () => {
    render(chart())

    const table = screen.getByRole('table', {
      name: 'You trained 4 of 5 planned sessions',
    })
    expect(
      within(table)
        .getAllByRole('columnheader')
        .map((header) => header.textContent),
    ).toEqual(['Week', 'Sessions done', 'Previous period'])
    const rows = within(table).getAllByRole('row').slice(1)
    expect(rows.map((row) => row.textContent)).toEqual(['W132', 'W243'])
  })

  it('names the series in Polish when the language is Polish', async () => {
    await i18n.changeLanguage('pl')
    render(chart({ xLabel: 'Tydzień' }))

    expect(
      within(screen.getByRole('table'))
        .getAllByRole('columnheader')
        .map((header) => header.textContent),
    ).toEqual(['Tydzień', 'Wykonane sesje', 'Poprzedni okres'])
  })

  it('says so instead of drawing an empty chart', () => {
    render(chart({ data: [] }))

    expect(screen.getByText('No sessions logged yet')).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('draws a bar chart as well', () => {
    render(chart({ kind: 'bar' }))

    expect(screen.getByRole('table')).toBeInTheDocument()
  })

  it('does not take a fourth series', () => {
    const four = [
      { meaning: 'completion', dataKey: 'done' },
      { meaning: 'rpe', dataKey: 'done' },
      { meaning: 'load', dataKey: 'done' },
      { meaning: 'wearables', dataKey: 'done' },
    ] as const
    vi.spyOn(console, 'error').mockImplementation(() => {})

    expect(() =>
      render(
        chart({
          // @ts-expect-error a chart has one to three series
          series: four,
        }),
      ),
    ).toThrow()
  })

  it('paints each series in the colour of its meaning and dashes only the previous period', () => {
    render(chart())

    const byMeaning = Object.fromEntries(
      drawn.lines.map((line) => [line.name, line]),
    )
    expect(byMeaning.completion.stroke).toBe('var(--color-completion)')
    expect(byMeaning.completion.strokeDasharray).toBeUndefined()
    expect(byMeaning.previous.stroke).toBe('var(--color-previous)')
    expect(byMeaning.previous.strokeDasharray).toBeTruthy()
  })

  it('fills each bar with the colour of its meaning and lightens only the previous period', () => {
    render(chart({ kind: 'bar' }))

    const byMeaning = Object.fromEntries(
      drawn.bars.map((bar) => [bar.name, bar]),
    )
    expect(byMeaning.completion.fill).toBe('var(--color-completion)')
    expect(byMeaning.completion.fillOpacity).toBe(1)
    expect(byMeaning.completion.strokeDasharray).toBeUndefined()
    expect(byMeaning.previous.fill).toBe('var(--color-previous)')
    expect(byMeaning.previous.fillOpacity).toBeLessThan(1)
    expect(byMeaning.previous.strokeDasharray).toBeTruthy()
  })

  it('animates bars by default and not when the person asks for less motion', () => {
    render(chart({ kind: 'bar' }))
    expect(drawn.bars.length).toBeGreaterThan(0)
    expect(drawn.bars.every((bar) => bar.isAnimationActive === true)).toBe(true)

    drawn.bars.length = 0
    vi.stubGlobal('matchMedia', (query: string) => ({
      matches: query.includes('prefers-reduced-motion'),
      media: query,
      addEventListener: () => {},
      removeEventListener: () => {},
    }))
    render(chart({ kind: 'bar' }))

    expect(drawn.bars.length).toBeGreaterThan(0)
    expect(drawn.bars.every((bar) => bar.isAnimationActive === false)).toBe(
      true,
    )
  })

  it.each(['', '   '])('refuses a chart whose title is %j', (title) => {
    vi.spyOn(console, 'error').mockImplementation(() => {})

    expect(() => render(chart({ title }))).toThrow()
  })

  it('names every series in the legend, not only by colour', () => {
    const { container } = render(chart())

    const legend = container.querySelector('.recharts-legend-wrapper')
    expect(legend).toHaveTextContent('Sessions done')
    expect(legend).toHaveTextContent('Previous period')
  })

  it('animates the line by default', () => {
    render(chart())

    expect(drawn.lines.length).toBeGreaterThan(0)
    expect(drawn.lines.every((line) => line.isAnimationActive === true)).toBe(
      true,
    )
  })

  it('stops animating when the person asks for less motion', () => {
    vi.stubGlobal('matchMedia', (query: string) => ({
      matches: query.includes('prefers-reduced-motion'),
      media: query,
      addEventListener: () => {},
      removeEventListener: () => {},
    }))

    render(chart())

    expect(drawn.lines.length).toBeGreaterThan(0)
    expect(drawn.lines.every((line) => line.isAnimationActive === false)).toBe(
      true,
    )
  })
})
