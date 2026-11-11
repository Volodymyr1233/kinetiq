import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it } from 'vitest'
import i18n from '@/lib/i18n'
import { WeightUnitContext, type WeightUnit } from '@/lib/weight'
import {
  json,
  problem,
  stubApi,
  withOpenPlanChange,
  type Reply,
} from '@/test/page'
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

const NEXT = {
  date: '2026-11-09',
  session: {
    ...SESSION,
    name: 'Lower body',
    day: 'MONDAY',
    entries: [
      { ...SESSION.entries[0], amount: 8, sets: 3 },
      SESSION.entries[1],
    ],
  },
}

// the states and the plan answer beside Today, as the rest-day card reads them
const restDay =
  (next?: typeof NEXT, state?: string): Reply =>
  (request) => {
    const path = new URL(request.url).pathname
    if (path === '/api/v1/session-states') {
      return json({
        today: '2026-11-06',
        sessions: [
          { day: 'MONDAY', date: '2026-11-02', state },
          { day: 'TUESDAY', date: '2026-11-03' },
          { day: 'THURSDAY', date: '2026-11-05' },
        ],
      })
    }
    if (path === '/api/v1/plan') {
      return json({ sessions: [{}, {}, {}] })
    }
    return json({ date: '2026-11-06', state: 'REST', next })
  }

function openToday(
  reply: Reply,
  unit: WeightUnit = 'KG',
  beforeSession?: ReactNode,
  openChange: object | null = null,
) {
  const requests = stubApi(withOpenPlanChange(openChange, reply))
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

const OPEN_CHANGE = {
  id: '00000000-0000-4000-9600-000000000001',
  counterpartName: 'Marek',
  rule: 'HIGH_RPE',
  state: 'PENDING',
  title: 'Back squat: cut volume by 13%',
  reason: 'Average RPE was 8.4 and 8.6 in the last 2 weeks, above 8.',
  effectiveWeek: '2026-11-16',
  acceptedByTrainer: false,
  acceptedByClient: false,
}

describe('TodayPage', () => {
  afterEach(() => i18n.changeLanguage('en'))

  it('shows the open plan change as a card that waits for both, and links to it', async () => {
    openToday(sessionDay, 'KG', undefined, OPEN_CHANGE)

    const card = (
      await screen.findByRole('heading', { name: OPEN_CHANGE.title })
    ).closest('[data-slot="card"]') as HTMLElement
    expect(within(card).getByText('Waiting for both')).toBeInTheDocument()
    expect(
      within(card).getByRole('link', { name: 'Open plan change' }),
    ).toHaveAttribute('href', `/app/plan/change/${OPEN_CHANGE.id}`)
  })

  it('reads Waiting for you once the trainer accepted', async () => {
    openToday(sessionDay, 'KG', undefined, {
      ...OPEN_CHANGE,
      state: 'ACCEPTED_BY_ONE',
      acceptedByTrainer: true,
    })

    expect(await screen.findByText('Waiting for you')).toBeInTheDocument()
  })

  it('shows no plan change card when there is none open', async () => {
    openToday(sessionDay)

    await screen.findByText('Thursday, November 5')
    expect(
      screen.queryByRole('link', { name: 'Open plan change' }),
    ).not.toBeInTheDocument()
  })

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

  it('offers Start session on a session day, linking to the logger', async () => {
    openToday(sessionDay)

    const start = await screen.findByRole('link', { name: 'Start session' })
    expect(start).toHaveAttribute('href', '/app/session')
  })

  it('says it is a rest day, with no session and no Start session', async () => {
    openToday(restDay())

    expect(
      await screen.findByRole('heading', { name: 'Rest day' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Rest')).toBeInTheDocument()
    expect(screen.getByText('Friday, November 6')).toBeInTheDocument()
    expect(
      screen.getByText('No session today. No check-in either.'),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('link', { name: 'Start session' }),
    ).not.toBeInTheDocument()
    expect(screen.queryByRole('list')).not.toBeInTheDocument()
  })

  it('shows the next session by name, date and estimated time, and the way to the week', async () => {
    openToday(restDay(NEXT))

    // 3 × (120 + 8 × 4) + 3 × (60 + 45) = 771 seconds, the plan editor's way
    expect(
      await screen.findByText('Next: Lower body · Mon, Nov 9 · 13 min'),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'See this week' })).toHaveAttribute(
      'href',
      '/app/plan',
    )
  })

  it('leaves out the next session when the plan has none left', async () => {
    openToday(restDay())

    await screen.findByRole('heading', { name: 'Rest day' })
    expect(screen.queryByText(/^Next:/)).not.toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: 'See this week' }),
    ).toBeInTheDocument()
  })

  it('counts the done sessions of the week under the card', async () => {
    openToday(restDay(NEXT, 'DONE'))

    expect(
      await screen.findByText('1 of 3 sessions done this week'),
    ).toBeInTheDocument()
  })

  it('leaves out the count when the states cannot be read', async () => {
    openToday((request) =>
      new URL(request.url).pathname === '/api/v1/session-states'
        ? problem(500, { status: 500, code: 'internal.error' })
        : restDay(NEXT)(request),
    )

    await screen.findByRole('heading', { name: 'Rest day' })
    expect(
      screen.queryByText(/sessions done this week/),
    ).not.toBeInTheDocument()
    expect(screen.getByText(/^Next:/)).toBeInTheDocument()
  })

  it('speaks Polish on a rest day', async () => {
    await i18n.changeLanguage('pl')
    openToday(restDay(NEXT, 'DONE'))

    expect(
      await screen.findByText('Następna: Lower body · pon., 9 lis · 13 min'),
    ).toBeInTheDocument()
    expect(screen.getByText('Odpoczynek')).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: 'Zobacz ten tydzień' }),
    ).toBeInTheDocument()
    expect(
      await screen.findByText('Zrobione sesje w tym tygodniu: 1 z 3'),
    ).toBeInTheDocument()
  })

  it('shows none of the rest-day elements on a session day', async () => {
    openToday(sessionDay)

    await screen.findByRole('heading', { name: 'Upper body' })
    expect(
      screen.queryByRole('link', { name: 'See this week' }),
    ).not.toBeInTheDocument()
    expect(screen.queryByText('Rest')).not.toBeInTheDocument()
  })

  it('says the trainer is preparing the next plan when the plan has finished', async () => {
    openToday(() => json({ date: '2026-11-06', state: 'FINISHED' }))

    expect(
      await screen.findByRole('heading', {
        name: 'Your trainer is preparing your next plan.',
      }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('link', { name: 'Start session' }),
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
      screen.getByRole('link', { name: 'Rozpocznij sesję' }),
    ).toHaveAttribute('href', '/app/session')
    expect(screen.getByText('4 × 8 powt.')).toBeInTheDocument()
  })

  const states = (state?: string, startedAt?: string) =>
    json({
      today: '2026-11-05',
      sessions: [
        {
          day: 'THURSDAY',
          date: '2026-11-05',
          state,
          sessionLogId: state === 'DONE' ? 'log-1' : undefined,
          startedAt,
        },
      ],
    })
  const withStates =
    (state?: string, startedAt?: string): Reply =>
    (request) =>
      new URL(request.url).pathname === '/api/v1/session-states'
        ? states(state, startedAt)
        : sessionDay(request)
  const hoursAgo = (hours: number) =>
    new Date(Date.now() - hours * 60 * 60 * 1000).toISOString()

  it('says the session is done and links to its summary instead of the start action', async () => {
    openToday(withStates('DONE'))

    expect(await screen.findByText('Done')).toBeInTheDocument()
    expect(screen.getByText('Session done')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'View summary' })).toHaveAttribute(
      'href',
      '/app/summary/log-1',
    )
    expect(
      screen.queryByRole('link', { name: 'Start session' }),
    ).not.toBeInTheDocument()
  })

  it('offers Edit log on a done session while its 24 hours last, linking to that session', async () => {
    openToday(withStates('DONE', hoursAgo(1)))

    expect(
      await screen.findByRole('link', { name: 'Edit log' }),
    ).toHaveAttribute('href', '/app/session/log-1')
    expect(
      screen.getByRole('link', { name: 'View summary' }),
    ).toBeInTheDocument()
  })

  it('offers no Edit log on a done session after its 24 hours', async () => {
    openToday(withStates('DONE', hoursAgo(25)))

    expect(
      await screen.findByRole('link', { name: 'View summary' }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('link', { name: 'Edit log' }),
    ).not.toBeInTheDocument()
  })

  it('offers to continue a session in progress, linking to the logger', async () => {
    openToday(withStates('IN_PROGRESS'))

    expect(await screen.findByText('In progress')).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: 'Continue session' }),
    ).toHaveAttribute('href', '/app/session')
    expect(
      screen.queryByRole('link', { name: 'Start session' }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('link', { name: 'View summary' }),
    ).not.toBeInTheDocument()
  })

  it('shows no Today chip on the Today tab, which already says it', async () => {
    openToday(withStates('TODAY'))

    expect(
      await screen.findByRole('link', { name: 'Start session' }),
    ).toBeEnabled()
    expect(screen.queryByText('Today')).not.toBeInTheDocument()
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
    expect(screen.getByRole('link', { name: 'Start session' })).toBeEnabled()
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

  it("shows each changed target as planned and today's, with the adjusted session's mark, reason and note", async () => {
    stubApi(withOpenPlanChange(null, sessionDay))
    render(
      <QueryClientProvider
        client={
          new QueryClient({ defaultOptions: { queries: { retry: false } } })
        }
      >
        <WeightUnitContext value="KG">
          <MemoryRouter>
            <TodayPage
              adjusted={{
                targets: [
                  { position: 0, loadKg: 45, amount: 8, rpe: 7 },
                  { position: 1, amount: 40, rpe: 6 },
                ],
                mark: <span>Mark</span>,
                reason: <p>Reason</p>,
                note: <p>Note</p>,
              }}
            />
          </MemoryRouter>
        </WeightUnitContext>
      </QueryClientProvider>,
    )

    expect(await screen.findByText('50 kg, today 45 kg')).toBeInTheDocument()
    expect(screen.getByText('RPE 8, today RPE 7')).toBeInTheDocument()
    expect(screen.getByText('3 × 45 s, today 3 × 40 s')).toBeInTheDocument()
    // unchanged targets read once
    expect(screen.getByText('4 × 8 reps')).toBeInTheDocument()
    expect(screen.getByText('Mark')).toBeInTheDocument()
    expect(screen.getByText('Reason')).toBeInTheDocument()
    expect(screen.getByText('Note')).toBeInTheDocument()
  })

  it('shows the planned targets when the adjusted session has none in use', async () => {
    openToday(sessionDay)

    expect(await screen.findByText('50 kg')).toBeInTheDocument()
    expect(screen.queryByText(/today 45 kg/)).toBeNull()
  })
})
