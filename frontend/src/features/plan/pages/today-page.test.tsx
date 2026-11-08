import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it } from 'vitest'
import i18n from '@/lib/i18n'
import { WeightUnitContext, type WeightUnit } from '@/lib/weight'
import { json, problem, stubApi, type Reply } from '@/test/page'
import { TodayPage } from './today-page'

const SESSION = {
  name: 'Upper body',
  day: 'THURSDAY',
  entries: [
    {
      exerciseId: '00000000-0000-4000-9100-000000000001',
      exerciseName: 'Bench press',
      measure: 'REPS',
      sets: 4,
      amount: 8,
      loadKg: 50,
      rpe: 8,
      restSeconds: 120,
    },
    {
      exerciseId: '00000000-0000-4000-9100-000000000026',
      exerciseName: 'Plank',
      measure: 'SECONDS',
      sets: 3,
      amount: 45,
      rpe: 7,
      restSeconds: 60,
    },
  ],
}

// the states of the week are empty unless a test says otherwise
const sessionDay: Reply = (request) =>
  request && new URL(request.url).pathname === '/api/v1/session-states'
    ? json({ today: '2026-11-05', sessions: [] })
    : json({ date: '2026-11-05', state: 'SESSION', session: SESSION })

function openToday(
  reply: Reply,
  unit: WeightUnit = 'KG',
  beforeSession?: ReactNode,
) {
  const requests = stubApi(reply)
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <WeightUnitContext value={unit}>
        <MemoryRouter>
          <TodayPage beforeSession={beforeSession} />
        </MemoryRouter>
      </WeightUnitContext>
    </QueryClientProvider>,
  )
  return requests
}

describe('TodayPage', () => {
  afterEach(() => i18n.changeLanguage('en'))

  it('shows the date and the session with every exercise and its targets', async () => {
    openToday(sessionDay)

    expect(await screen.findByText('Thursday, November 5')).toBeInTheDocument()
    expect(
      screen.getByRole('heading', { name: 'Upper body' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Bench press')).toBeInTheDocument()
    expect(screen.getByText('4 × 8 reps')).toBeInTheDocument()
    expect(screen.getByText('50 kg')).toBeInTheDocument()
    expect(screen.getByText('RPE 8')).toBeInTheDocument()
    expect(screen.getByText('Rest 120 s')).toBeInTheDocument()
    expect(screen.getAllByRole('listitem')).toHaveLength(2)
  })

  it('reads an exercise measured in seconds in seconds, and says so when there is no load', async () => {
    openToday(sessionDay)

    expect(await screen.findByText('3 × 45 s')).toBeInTheDocument()
    expect(screen.getByText('No load')).toBeInTheDocument()
    expect(screen.queryByText('3 × 45 reps')).not.toBeInTheDocument()
  })

  it('shows a load in pounds to a client whose unit is pounds', async () => {
    openToday(sessionDay, 'LB')

    expect(await screen.findByText('110.2 lb')).toBeInTheDocument()
  })

  it('offers Start session on a session day, disabled, with the reason beside it', async () => {
    openToday(sessionDay)

    const start = await screen.findByRole('button', { name: 'Start session' })
    expect(start).toBeDisabled()
    expect(start).toHaveAccessibleDescription(
      'Starting a session arrives with logging.',
    )
  })

  it('says it is a rest day, with no session and no Start session', async () => {
    openToday(() => json({ date: '2026-11-06', state: 'REST' }))

    expect(
      await screen.findByRole('heading', { name: 'Rest day' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Friday, November 6')).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Start session' }),
    ).not.toBeInTheDocument()
    expect(screen.queryByRole('list')).not.toBeInTheDocument()
  })

  it('says the trainer is preparing the next plan when the plan has finished', async () => {
    openToday(() => json({ date: '2026-11-06', state: 'FINISHED' }))

    expect(
      await screen.findByRole('heading', {
        name: 'Your trainer is preparing your next plan.',
      }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Start session' }),
    ).not.toBeInTheDocument()
    expect(screen.queryByRole('list')).not.toBeInTheDocument()
  })

  it('says there is no plan yet, without an error, when the client has none', async () => {
    openToday(() => problem(404, { status: 404, code: 'plan.not-found' }))

    expect(
      await screen.findByText("You don't have a plan yet."),
    ).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('offers to try again when Today cannot be loaded', async () => {
    const user = userEvent.setup()
    let failing = true
    openToday((request) =>
      failing
        ? problem(429, { status: 429, code: 'request.too-many' })
        : sessionDay(request),
    )

    const retry = await screen.findByRole('button', { name: 'Try again' })
    failing = false
    await user.click(retry)

    expect(
      await screen.findByRole('heading', { name: 'Upper body' }),
    ).toBeInTheDocument()
  })

  it('speaks Polish when the UI does', async () => {
    await i18n.changeLanguage('pl')
    openToday(sessionDay)

    expect(await screen.findByText('czwartek, 5 listopada')).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Rozpocznij sesję' }),
    ).toBeDisabled()
    expect(screen.getByText('4 × 8 powt.')).toBeInTheDocument()
  })

  const states = (state?: string) =>
    json({
      today: '2026-11-05',
      sessions: [
        {
          day: 'THURSDAY',
          date: '2026-11-05',
          state,
          sessionLogId: state === 'DONE' ? 'log-1' : undefined,
        },
      ],
    })
  const withStates =
    (state?: string): Reply =>
    (request) =>
      new URL(request.url).pathname === '/api/v1/session-states'
        ? states(state)
        : sessionDay(request)

  it('says the session is done and links to its summary instead of the start action', async () => {
    openToday(withStates('DONE'))

    expect(await screen.findByText('Done')).toBeInTheDocument()
    expect(screen.getByText('Session done')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'View summary' })).toHaveAttribute(
      'href',
      '/app/summary/log-1',
    )
    expect(
      screen.queryByRole('button', { name: 'Start session' }),
    ).not.toBeInTheDocument()
  })

  it('keeps the disabled start action when the session is not done', async () => {
    openToday(withStates('IN_PROGRESS'))

    expect(await screen.findByText('In progress')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Start session' })).toBeDisabled()
    expect(
      screen.queryByRole('link', { name: 'View summary' }),
    ).not.toBeInTheDocument()
  })

  it('shows the session as before when the states cannot be read', async () => {
    openToday((request) =>
      new URL(request.url).pathname === '/api/v1/session-states'
        ? problem(500, { status: 500, code: 'internal.error' })
        : sessionDay(request),
    )

    expect(
      await screen.findByRole('heading', { name: 'Upper body' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Start session' })).toBeDisabled()
  })

  it('shows what comes before the session only on a day with a session', async () => {
    openToday(sessionDay, 'KG', <p>Check-in</p>)
    expect(await screen.findByText('Check-in')).toBeInTheDocument()
    cleanup()

    for (const state of ['REST', 'FINISHED']) {
      openToday(
        () => json({ date: '2026-11-06', state }),
        'KG',
        <p>Check-in</p>,
      )
      expect(await screen.findByRole('heading')).toBeInTheDocument()
      expect(screen.queryByText('Check-in')).toBeNull()
      cleanup()
    }
  })
})
