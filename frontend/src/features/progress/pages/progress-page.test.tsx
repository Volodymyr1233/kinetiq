import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import i18n from '@/lib/i18n'
import { WeightUnitContext, type WeightUnit } from '@/lib/weight'
import { json, problem, stubApi, type Reply } from '@/test/page'
import type { Progress } from '../api/progress'
import { ProgressPage } from './progress-page'

// Wednesday of plan week 14: weeks 11 and 12 full, week 13 at 2 of 3, week 14 running with 1 of 3 done
const PROGRESS = {
  goal: 'BUILD_MUSCLE',
  week: 14,
  finished: false,
  sessionsDone: 9,
  sessionsPlanned: 12,
  sessionsDoneChange: -1,
  averageRpe: 8.4,
  averageRpeChange: 0.6,
  weeksInARow: 4,
  weeks: [
    { number: 11, startsOn: '2026-10-19', planned: 3, done: 3, running: false },
    { number: 12, startsOn: '2026-10-26', planned: 3, done: 3, running: false },
    { number: 13, startsOn: '2026-11-02', planned: 3, done: 2, running: false },
    { number: 14, startsOn: '2026-11-09', planned: 3, done: 1, running: true },
  ],
  exercises: [
    {
      exerciseId: '00000000-0000-4000-9100-000000000001',
      name: 'Back squat',
      measure: 'REPS',
      best: { loadKg: 102.5, amount: 5, rpe: 8 },
      topSetChange: { loadKg: 2.5 },
      prToday: true,
      weeks: [
        { number: 13, top: { loadKg: 100, amount: 5, rpe: 8 }, averageRpe: 8 },
        {
          number: 14,
          top: { loadKg: 102.5, amount: 5, rpe: 8 },
          averageRpe: 8,
        },
      ],
      recent: [
        { date: '2026-11-11', top: { loadKg: 102.5, amount: 5, rpe: 8 } },
        { date: '2026-11-04', top: { loadKg: 100, amount: 5, rpe: 8 } },
      ],
    },
  ],
} satisfies Progress

function openProgress(reply: Reply, unit: WeightUnit = 'KG') {
  const requests = stubApi(reply)
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <WeightUnitContext value={unit}>
        <ProgressPage />
      </WeightUnitContext>
    </QueryClientProvider>,
  )
  return requests
}

const tiles = async (name = 'Figures for the period') =>
  within(await screen.findByRole('region', { name }))

describe('ProgressPage', () => {
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

  it('shows the plan week, the tiles and the sessions chart for 8 weeks at first', async () => {
    const requests = openProgress(() => json(PROGRESS))
    const tile = await tiles()

    expect(screen.getByText('Build muscle · Week 14')).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: '8 weeks' })).toBeChecked()
    expect(new URL(requests[0].url).searchParams.get('weeks')).toBe('8')
    expect(tile.getByText('of 12')).toBeInTheDocument()
    expect(
      tile.getByRole('progressbar', { name: 'Sessions done' }),
    ).toHaveAttribute('aria-valuetext', '9 of 12 sessions')
    expect(tile.getByText('8.4')).toBeInTheDocument()
    expect(tile.getByText('Above 7–8 target')).toBeInTheDocument()
    expect(tile.getByText('Back squat best')).toBeInTheDocument()
    expect(tile.getByText('102.5')).toBeInTheDocument()
    expect(tile.getByText('PR today')).toBeInTheDocument()
    expect(tile.getByText('At least 2 sessions each week')).toBeInTheDocument()
    expect(
      screen.getByRole('heading', { name: '2 of 3 weeks at 70% or more' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('img', {
        name: '2 of 3 weeks at 70% or more. Week 14 has 2 sessions left.',
      }),
    ).toBeInTheDocument()
    const byExercise = within(
      screen.getByRole('region', { name: 'By exercise' }),
    )
    expect(byExercise.getByRole('radio', { name: 'Back squat' })).toBeChecked()
    expect(
      byExercise.getByRole('heading', {
        name: 'Top set rose 2.5 kg in 2 weeks',
      }),
    ).toBeInTheDocument()
  })

  it('says the running week has sessions left', async () => {
    openProgress(() => json(PROGRESS))

    expect(
      await screen.findByText('Week 14 has 2 sessions left.'),
    ).toBeInTheDocument()
  })

  it('compares with the week before up, down and the same', async () => {
    openProgress(() => json({ ...PROGRESS, averageRpeChange: 0 }))
    const tile = await tiles()

    expect(tile.getByText('1 week on week')).toHaveTextContent(
      'Down 1 week on week',
    )
    expect(tile.getByText('2.5 kg week on week')).toHaveTextContent(
      'Up 2.5 kg week on week',
    )
    expect(tile.getByText('Same as the week before')).toBeInTheDocument()
  })

  it('shows no comparison when there is nothing to compare', async () => {
    // fields without a value are left out of the answer
    openProgress(() =>
      json({
        ...PROGRESS,
        sessionsDoneChange: undefined,
        averageRpeChange: undefined,
        exercises: [{ ...PROGRESS.exercises[0], topSetChange: undefined }],
      }),
    )
    const tile = await tiles()

    expect(tile.queryByText(/week on week|week before/)).not.toBeInTheDocument()
  })

  it('asks for 4 weeks when the client chooses them', async () => {
    const user = userEvent.setup()
    const requests = openProgress(() => json(PROGRESS))
    await tiles()

    await user.click(screen.getByRole('radio', { name: '4 weeks' }))

    await waitFor(() =>
      expect(
        requests.map((r) => new URL(r.url).searchParams.get('weeks')),
      ).toEqual(['8', '4']),
    )
    expect(screen.getByRole('radio', { name: '4 weeks' })).toBeChecked()
  })

  it('shows loads in pounds for a client in pounds', async () => {
    openProgress(() => json(PROGRESS), 'LB')
    const tile = await tiles()

    expect(tile.getByText('226')).toBeInTheDocument()
    expect(tile.getByText('lb')).toBeInTheDocument()
    expect(tile.getByText('5.5 lb week on week')).toBeInTheDocument()
  })

  it('shows a best set without a load in repetitions', async () => {
    const squat = PROGRESS.exercises[0]
    openProgress(() =>
      json({
        ...PROGRESS,
        exercises: [
          {
            ...squat,
            name: 'Pull-up',
            best: { amount: 12, rpe: 8 },
            topSetChange: { amount: 1 },
            prToday: false,
          },
        ],
      }),
    )
    const tile = await tiles()

    expect(tile.getByText('12')).toBeInTheDocument()
    expect(tile.getByText('reps')).toBeInTheDocument()
    expect(tile.getByText('1 rep week on week')).toBeInTheDocument()
    expect(tile.queryByText('PR today')).not.toBeInTheDocument()
  })

  it('shows the sessions chart as a table and back', async () => {
    const user = userEvent.setup()
    // without "By exercise", whose chart and table have their own tests
    openProgress(() => json({ ...PROGRESS, exercises: [] }))
    await tiles()

    await user.click(screen.getByRole('button', { name: 'Show as table' }))

    const rows = within(screen.getByRole('table')).getAllByRole('row')
    expect(rows.map((row) => row.textContent)).toEqual([
      'WeekDonePlanned',
      '1133',
      '1233',
      '1323',
      '1413',
    ])
    expect(screen.getByText('Week 14 has 2 sessions left.')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Show as chart' }))
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('asks the client to log a session when nothing is logged in the period', async () => {
    openProgress(() =>
      json({
        ...PROGRESS,
        sessionsDone: 0,
        averageRpe: undefined,
        weeks: PROGRESS.weeks.map((week) => ({ ...week, done: 0 })),
        exercises: [],
      }),
    )
    const tile = await tiles()

    expect(
      screen.getByText('Log a session to see your progress here.'),
    ).toBeInTheDocument()
    expect(screen.queryByRole('heading')).not.toBeInTheDocument()
    expect(tile.getByText('Best set')).toBeInTheDocument()
    expect(tile.getAllByText('No sets logged in this period.')).toHaveLength(2)
  })

  it('says the progress shows once the trainer publishes a plan', async () => {
    openProgress(() => problem(404, { status: 404, code: 'plan.not-found' }))

    expect(
      await screen.findByText(
        'Your progress shows here once your trainer publishes your plan.',
      ),
    ).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('offers to try again when the progress cannot be read', async () => {
    const user = userEvent.setup()
    let failing = true
    openProgress(() =>
      failing
        ? problem(429, { status: 429, code: 'request.too-many' })
        : json(PROGRESS),
    )

    const retry = await screen.findByRole('button', { name: 'Try again' })
    failing = false
    await user.click(retry)

    expect(await tiles()).toBeDefined()
  })

  it('shows a finished plan in its header', async () => {
    openProgress(() =>
      json({
        ...PROGRESS,
        finished: true,
        weeks: PROGRESS.weeks.map((week) => ({ ...week, running: false })),
      }),
    )

    expect(
      await screen.findByText('Build muscle · Plan finished'),
    ).toBeInTheDocument()
    expect(screen.queryByText(/sessions left/)).not.toBeInTheDocument()
  })

  it('speaks Polish', async () => {
    await i18n.changeLanguage('pl')
    openProgress(() => json(PROGRESS))
    const tile = await tiles('Wyniki z okresu')

    expect(screen.getByText('Budowa mięśni · Tydzień 14')).toBeInTheDocument()
    expect(
      screen.getByRole('radio', { name: '4 tygodnie' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: '8 tygodni' })).toBeChecked()
    expect(tile.getByText('8,4')).toBeInTheDocument()
    expect(tile.getByText('Powyżej celu 7–8')).toBeInTheDocument()
    expect(tile.getByText('tygodnie')).toBeInTheDocument()
    expect(
      screen.getByRole('heading', { name: '2 z 3 tygodni na 70% lub więcej' }),
    ).toBeInTheDocument()
    expect(
      screen.getByText('W tygodniu 14 zostały 2 sesje.'),
    ).toBeInTheDocument()
  })
})
