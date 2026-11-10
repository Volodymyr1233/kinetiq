import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import i18n from '@/lib/i18n'
import { WeightUnitContext } from '@/lib/weight'
import type { ExerciseProgress as Exercise } from '../api/progress'
import { ExerciseProgress } from './exercise-progress'

const set = (loadKg: number | undefined, amount: number, rpe = 8) => ({
  loadKg,
  amount,
  rpe,
})

// logged once a week, the last one today: 100, 100, 102.5 and 105 kg at RPE 8 (spec, User Story 2)
const SQUAT = {
  exerciseId: '00000000-0000-4000-9100-000000000001',
  name: 'Back squat',
  measure: 'REPS',
  best: set(105, 5),
  topSetChange: { loadKg: 2.5 },
  prToday: true,
  weeks: [
    { number: 11, top: set(100, 5), averageRpe: 8 },
    { number: 12, top: set(100, 5), averageRpe: 8 },
    { number: 13, top: set(102.5, 5), averageRpe: 8 },
    { number: 14, top: set(105, 5), averageRpe: 8 },
  ],
  recent: [
    { date: '2026-11-10', top: set(105, 5) },
    { date: '2026-11-03', top: set(102.5, 5, 9) },
    { date: '2026-10-27', top: set(100, 5) },
  ],
} satisfies Exercise

const PLANK = {
  exerciseId: '00000000-0000-4000-9100-000000000002',
  name: 'Plank',
  measure: 'SECONDS',
  best: set(undefined, 60, 7),
  prToday: false,
  weeks: [
    { number: 12, top: set(undefined, 60, 7), averageRpe: 7 },
    { number: 14, top: set(undefined, 45, 7), averageRpe: 7.5 },
  ],
  recent: [{ date: '2026-11-09', top: set(undefined, 45, 7) }],
} satisfies Exercise

const BENCH = {
  exerciseId: '00000000-0000-4000-9100-000000000003',
  name: 'Bench press',
  measure: 'REPS',
  best: set(80, 5),
  prToday: false,
  weeks: [
    { number: 13, top: set(80, 5), averageRpe: 7 },
    { number: 14, top: set(80, 6), averageRpe: 7.5 },
  ],
  recent: [{ date: '2026-11-10', top: set(80, 6) }],
} satisfies Exercise

function show() {
  render(
    <WeightUnitContext value="KG">
      <ExerciseProgress
        exercises={[SQUAT, PLANK, BENCH]}
        weeks={[11, 12, 13, 14]}
      />
    </WeightUnitContext>,
  )
}

const rows = (table: HTMLElement) =>
  within(table)
    .getAllByRole('row')
    .map((row) => row.textContent)

describe('ExerciseProgress', () => {
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

  it('opens on the first exercise and says how its top set rose', () => {
    show()

    expect(screen.getByRole('radio', { name: 'Back squat' })).toBeChecked()
    expect(
      screen.getByRole('heading', { name: 'Top set rose 5 kg in 4 weeks' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Top set, kg · Average RPE')).toBeInTheDocument()
    expect(
      screen.getByRole('img', {
        name: 'Back squat: Top set rose 5 kg in 4 weeks. Average RPE 8.0 in week 14.',
      }),
    ).toBeInTheDocument()
  })

  it('lists the last sessions with the exercise, newest first', () => {
    show()

    expect(
      rows(
        screen.getByRole('table', { name: 'Last sessions with Back squat' }),
      ),
    ).toEqual([
      'DateTop setRPE',
      'Tue, Nov 10105 kg8',
      'Tue, Nov 3102.5 kg9',
      'Tue, Oct 27100 kg8',
    ])
  })

  it('shows another exercise in its own measure when it is chosen', async () => {
    const user = userEvent.setup()
    show()

    await user.click(screen.getByRole('radio', { name: 'Plank' }))

    expect(screen.getByRole('radio', { name: 'Plank' })).toBeChecked()
    expect(
      screen.getByRole('heading', { name: 'Top set fell 15 s in 3 weeks' }),
    ).toBeInTheDocument()
    expect(
      screen.getByText('Top set, seconds · Average RPE'),
    ).toBeInTheDocument()
    expect(
      rows(screen.getByRole('table', { name: 'Last sessions with Plank' })),
    ).toEqual(['DateTop setRPE', 'Mon, Nov 945 s7'])
  })

  it('says the top set held when the first and the last week are the same', async () => {
    const user = userEvent.setup()
    show()

    await user.click(screen.getByRole('radio', { name: 'Bench press' }))

    expect(
      screen.getByRole('heading', { name: 'Top set held at 80 kg' }),
    ).toBeInTheDocument()
  })

  it('shows the chart as a table of the weeks with the exercise', async () => {
    const user = userEvent.setup()
    show()

    await user.click(screen.getByRole('button', { name: 'Show as table' }))

    // the weeks' table comes before the last sessions
    expect(rows(screen.getAllByRole('table')[0])).toEqual([
      'WeekTop setRPE',
      '11100 kg8.0',
      '12100 kg8.0',
      '13102.5 kg8.0',
      '14105 kg8.0',
    ])
    await user.click(screen.getByRole('button', { name: 'Show as chart' }))
    expect(screen.getAllByRole('table')).toHaveLength(1)
  })

  it('speaks Polish', async () => {
    await i18n.changeLanguage('pl')
    show()

    expect(
      screen.getByRole('region', { name: 'Według ćwiczeń' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('heading', {
        name: 'Najlepsza seria wzrosła o 5 kg w 4 tygodnie',
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByText('Najlepsza seria, kg · Średnie RPE'),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('img', {
        name: 'Back squat: Najlepsza seria wzrosła o 5 kg w 4 tygodnie. Średnie RPE 8,0 w tygodniu 14.',
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('table', {
        name: 'Ostatnie sesje z ćwiczeniem Back squat',
      }),
    ).toBeInTheDocument()
  })
})
