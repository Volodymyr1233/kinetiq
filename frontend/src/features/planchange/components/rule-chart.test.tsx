import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import i18n from '@/lib/i18n'
import { WeightUnitContext } from '@/lib/weight'
import { RuleChart } from './rule-chart'

type ChartedPlanChange = Parameters<typeof RuleChart>[0]['planChange']

const SQUAT = '00000000-0000-4000-9100-000000000011'
const WINDOW = { windowStart: '2026-10-26', windowEnd: '2026-11-15' }

const RPE: ChartedPlanChange = {
  rule: 'HIGH_RPE',
  concernedExercises: [],
  ...WINDOW,
  figures: [
    { kind: 'MEAN_RPE', weekStart: '2026-10-26', value: 8.4 },
    { kind: 'MEAN_RPE', weekStart: '2026-11-09', value: 8.6 },
  ],
}

beforeEach(() => {
  // jsdom measures every box as 0 × 0, and Recharts renders nothing in an empty container
  vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockReturnValue(
    new DOMRect(0, 0, 320, 200),
  )
})

afterEach(async () => {
  vi.restoreAllMocks()
  await i18n.changeLanguage('en')
})

describe('RuleChart', () => {
  it('names every week and says a week without logs has none, instead of drawing a zero', () => {
    render(<RuleChart planChange={RPE} />)

    expect(
      screen.getByRole('img', {
        name: 'Average RPE: Oct 26: 8.4, Nov 2: no logs, Nov 9: 8.6. Rule threshold 8.',
      }),
    ).toBeInTheDocument()
  })

  it('lists the same values as a table on request, with a gap for the week without logs', async () => {
    const user = userEvent.setup()
    render(<RuleChart planChange={RPE} />)
    expect(screen.queryByRole('table')).not.toBeInTheDocument()

    const toggle = screen.getByRole('button', { name: 'Show as table' })
    expect(toggle).toHaveAttribute('aria-pressed', 'false')
    await user.click(toggle)

    const rows = within(screen.getByRole('table')).getAllByRole('row')
    expect(
      rows.map((row) =>
        [...row.querySelectorAll('th, td')].map((cell) => cell.textContent),
      ),
    ).toEqual([
      ['Week', 'Average RPE'],
      ['Oct 26', '8.4'],
      ['Nov 2', 'No logs'],
      ['Nov 9', '8.6'],
    ])
    expect(screen.getByRole('button', { name: 'Hide table' })).toHaveAttribute(
      'aria-pressed',
      'true',
    )
  })

  it('takes the series colour from the design system token for its meaning, and never a raw colour', () => {
    const { container } = render(<RuleChart planChange={RPE} />)

    const style = container.querySelector('style')?.textContent ?? ''
    expect(style).toContain('--color-value: var(--chart-2)')
    // the series, its dots and the threshold; the library's default axis ink is overridden by the chart's classes
    const paints = [
      ...container.querySelectorAll(
        '.recharts-line [stroke], .recharts-line [fill], .recharts-reference-line [stroke]',
      ),
    ].flatMap((element) => [
      element.getAttribute('stroke'),
      element.getAttribute('fill'),
    ])
    expect(paints.length).toBeGreaterThan(0)
    expect(paints.filter((paint) => /^(#|rgb)/i.test(paint ?? ''))).toEqual([])
  })

  it('draws the threshold as a dashed muted line and names it in the legend, so colour is not all that says it', () => {
    const { container } = render(<RuleChart planChange={RPE} />)

    const line = container.querySelector('.recharts-reference-line line')
    expect(line).toHaveAttribute('stroke', 'var(--muted-foreground)')
    expect(line).toHaveAttribute('stroke-dasharray', '4 4')
    expect(screen.getByText('Rule threshold, 8')).toBeInTheDocument()
  })

  it('draws a chart for each stalled exercise, in the viewers weight unit', () => {
    render(
      <WeightUnitContext value="LB">
        <RuleChart
          planChange={{
            rule: 'STALLED_WEIGHT',
            ...WINDOW,
            concernedExercises: [{ id: SQUAT, name: 'Back squat' }],
            figures: [
              {
                kind: 'BEST_LOAD_KG',
                exerciseId: SQUAT,
                weekStart: '2026-10-26',
                value: 100,
              },
              {
                kind: 'BEST_LOAD_KG',
                exerciseId: SQUAT,
                weekStart: '2026-11-09',
                value: 101,
              },
            ],
          }}
        />
      </WeightUnitContext>,
    )

    expect(
      screen.getByRole('img', {
        name: 'Best load, Back squat: Oct 26: 220.5 lb, Nov 2: no logs, Nov 9: 222.7 lb. Rule threshold 231.5 lb.',
      }),
    ).toBeInTheDocument()
  })

  it('shows the completion share, with a gap when no session was planned', () => {
    render(
      <RuleChart
        planChange={{
          rule: 'LOW_COMPLETION',
          ...WINDOW,
          concernedExercises: [],
          figures: [
            { kind: 'SESSIONS_PLANNED', weekStart: '2026-10-26', value: 4 },
            { kind: 'SESSIONS_FINISHED', weekStart: '2026-10-26', value: 2 },
            { kind: 'SESSIONS_PLANNED', weekStart: '2026-11-02', value: 0 },
            { kind: 'SESSIONS_FINISHED', weekStart: '2026-11-02', value: 0 },
            { kind: 'SESSIONS_PLANNED', weekStart: '2026-11-09', value: 6 },
            { kind: 'SESSIONS_FINISHED', weekStart: '2026-11-09', value: 3 },
          ],
        }}
      />,
    )

    expect(
      screen.getByRole('img', {
        name: 'Sessions finished: Oct 26: 50%, Nov 2: no logs, Nov 9: 50%. Rule threshold 70%.',
      }),
    ).toBeInTheDocument()
  })

  it('writes the axis and the numbers in Polish for a Polish viewer', async () => {
    await i18n.changeLanguage('pl')
    render(<RuleChart planChange={RPE} />)

    expect(
      screen.getByRole('img', {
        name: 'Średnie RPE: 26 paź: 8,4, 2 lis: brak zapisów, 9 lis: 8,6. Próg reguły 8.',
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Pokaż jako tabelę' }),
    ).toBeInTheDocument()
  })

  describe('compact', () => {
    it('keeps the label that says every value, and drops the card, the legend swatch and the table', () => {
      render(<RuleChart compact planChange={RPE} />)

      expect(
        screen.getByRole('img', {
          name: 'Average RPE: Oct 26: 8.4, Nov 2: no logs, Nov 9: 8.6. Rule threshold 8.',
        }),
      ).toBeInTheDocument()
      expect(
        screen.queryByRole('heading', { name: 'Why this change' }),
      ).not.toBeInTheDocument()
      expect(
        screen.queryByRole('button', { name: 'Show as table' }),
      ).not.toBeInTheDocument()
      expect(screen.getByText('Rule threshold, 8')).toBeInTheDocument()
    })
  })
})
