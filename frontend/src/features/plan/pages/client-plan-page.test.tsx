import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, describe, expect, it } from 'vitest'
import i18n from '@/lib/i18n'
import { WeightUnitContext, type WeightUnit } from '@/lib/weight'
import { json, problem, stubApi, type Reply } from '@/test/page'
import { ClientPlanPage } from './client-plan-page'

const SESSIONS = [
  {
    name: 'Upper body',
    day: 'MONDAY',
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
    ],
  },
  {
    name: 'Core',
    day: 'THURSDAY',
    entries: [
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
  },
]

// plan week 6 of 12 started on Monday 2 November; every plan week starts a week after the one before
function planAt(number: number) {
  const startsOn = new Date(Date.UTC(2026, 10, 2 + (number - 6) * 7))
  return {
    goal: 'LOSE_FAT',
    weeks: 12,
    currentWeek: 6,
    finished: false,
    seen: true,
    week: { number, startsOn: startsOn.toISOString().slice(0, 10) },
    sessions: SESSIONS,
  }
}

const weekAsked = (request: Request) => {
  const week = new URL(request.url).searchParams.get('week')
  return week === null ? 6 : Number(week)
}

const LOG = '00000000-0000-4000-a000-000000000001'

// the states of week 6: Monday is done on a recovery day's lighter targets and Thursday is missed
const STATES = {
  today: '2026-11-06',
  sessions: [
    {
      day: 'MONDAY',
      date: '2026-11-02',
      state: 'DONE',
      sessionLogId: LOG,
      adjusted: true,
    },
    { day: 'THURSDAY', date: '2026-11-05', state: 'MISSED', adjusted: false },
  ],
}

const isStates = (request: Request) =>
  new URL(request.url).pathname === '/api/v1/session-states'

const servePlan: Reply = (request) =>
  isStates(request)
    ? json({ today: '2026-11-06', sessions: [] })
    : json(planAt(weekAsked(request)))

const servePlanWithStates: Reply = (request) =>
  isStates(request) ? json(STATES) : json(planAt(weekAsked(request)))

function openPlan(
  reply: Reply,
  address = '/app/plan',
  unit: WeightUnit = 'KG',
) {
  const requests = stubApi(reply)
  const router = createMemoryRouter(
    [
      { path: '/app/plan', Component: ClientPlanPage },
      { path: '/app/session/:id', Component: () => <h1>Logger</h1> },
    ],
    { initialEntries: [address] },
  )
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <WeightUnitContext value={unit}>
        <RouterProvider router={router} />
      </WeightUnitContext>
    </QueryClientProvider>,
  )
  return { requests, router }
}

const acknowledgements = (requests: Request[]) =>
  requests.filter(
    (request) =>
      request.method === 'POST' &&
      new URL(request.url).pathname === '/api/v1/plan/acknowledgement',
  )

const asked = (requests: Request[]) =>
  requests.map((request) => new URL(request.url).searchParams.get('week'))

describe('ClientPlanPage', () => {
  afterEach(() => i18n.changeLanguage('en'))

  it('shows the current week with its sessions on their days and rest days between', async () => {
    openPlan(servePlan)

    expect(await screen.findByText('Week 6 of 12')).toBeInTheDocument()
    const days = screen.getAllByRole('listitem')
    expect(days).toHaveLength(7)
    expect(within(days[0]).getByText('Mon')).toBeInTheDocument()
    expect(
      within(days[0]).getByRole('button', { name: /Upper body/ }),
    ).toBeInTheDocument()
    expect(
      within(days[3]).getByRole('button', { name: /Core/ }),
    ).toBeInTheDocument()
    expect(within(days[1]).getByText('Rest day')).toBeInTheDocument()
    expect(screen.getAllByText('Rest day')).toHaveLength(5)
    expect(screen.getByText(/Nov 2.*8/)).toBeInTheDocument()
  })

  it('moves to the next and the previous week, and back to the current one', async () => {
    const user = userEvent.setup()
    const { requests } = openPlan(servePlan)
    await screen.findByText('Week 6 of 12')
    expect(
      screen.queryByRole('button', { name: 'Back to current week' }),
    ).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Next week' }))
    expect(await screen.findByText('Week 7 of 12')).toBeInTheDocument()
    expect(screen.getByText(/Nov 9.*15/)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Previous week' }))
    await user.click(screen.getByRole('button', { name: 'Previous week' }))
    expect(await screen.findByText('Week 5 of 12')).toBeInTheDocument()

    await user.click(
      screen.getByRole('button', { name: 'Back to current week' }),
    )
    expect(await screen.findByText('Week 6 of 12')).toBeInTheDocument()
    expect(asked(requests)).toContain('7')
    expect(asked(requests)).toContain('5')
  })

  it('has no previous week in week 1 and no next week in the last week', async () => {
    openPlan(servePlan, '/app/plan?week=1')
    expect(await screen.findByText('Week 1 of 12')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Previous week' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Next week' })).toBeEnabled()
  })

  it('has no next week in the last week', async () => {
    openPlan(servePlan, '/app/plan?week=12')
    expect(await screen.findByText('Week 12 of 12')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Next week' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Previous week' })).toBeEnabled()
  })

  it('opens a session to its exercises and targets, and closes it again', async () => {
    const user = userEvent.setup()
    openPlan(servePlan)
    const trigger = await screen.findByRole('button', { name: /Upper body/ })
    expect(trigger).toHaveAttribute('aria-expanded', 'false')
    expect(screen.queryByText('Bench press')).not.toBeInTheDocument()

    await user.click(trigger)

    expect(trigger).toHaveAttribute('aria-expanded', 'true')
    expect(screen.getByText('Bench press')).toBeInTheDocument()
    expect(screen.getByText('4 × 8 reps')).toBeInTheDocument()
    expect(screen.getByText('50 kg')).toBeInTheDocument()
    expect(screen.getByText('RPE 8')).toBeInTheDocument()
    expect(screen.getByText('Rest 120 s')).toBeInTheDocument()

    await user.click(trigger)
    expect(screen.queryByText('Bench press')).not.toBeInTheDocument()
  })

  it('reads an exercise measured in seconds in seconds, and says so when there is no load', async () => {
    const user = userEvent.setup()
    openPlan(servePlan)

    await user.click(await screen.findByRole('button', { name: /Core/ }))

    expect(screen.getByText('3 × 45 s')).toBeInTheDocument()
    expect(screen.getByText('No load')).toBeInTheDocument()
    expect(screen.queryByText(/reps/)).not.toBeInTheDocument()
  })

  it('shows a load in pounds to a client whose unit is pounds', async () => {
    const user = userEvent.setup()
    openPlan(servePlan, '/app/plan', 'LB')

    await user.click(await screen.findByRole('button', { name: /Upper body/ }))

    expect(screen.getByText('110.2 lb')).toBeInTheDocument()
  })

  it('counts the exercises of a session with the plural of the language', async () => {
    await i18n.changeLanguage('pl')
    openPlan(servePlan)

    expect(await screen.findByText('Tydzień 6 z 12')).toBeInTheDocument()
    expect(screen.getAllByText('1 ćwiczenie')).toHaveLength(2)
    expect(
      screen.getByRole('button', { name: 'Poprzedni tydzień' }),
    ).toBeEnabled()
    expect(screen.getAllByText('Dzień odpoczynku')).toHaveLength(5)
  })

  it('offers to try again when the plan cannot be loaded', async () => {
    const user = userEvent.setup()
    let failing = true
    openPlan((request) =>
      failing
        ? problem(429, { status: 429, code: 'request.too-many' })
        : servePlan(request),
    )

    const retry = await screen.findByRole('button', { name: 'Try again' })
    failing = false
    await user.click(retry)

    expect(await screen.findByText('Week 6 of 12')).toBeInTheDocument()
  })

  it('says there is no plan yet, without an error, when the client has none', async () => {
    openPlan(() => problem(404, { status: 404, code: 'plan.not-found' }))

    expect(
      await screen.findByText("You don't have a plan yet."),
    ).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('goes to the current week when the address names a week the plan does not have', async () => {
    const { router } = openPlan((request) =>
      weekAsked(request) === 99
        ? problem(400, { status: 400, code: 'plan.week-out-of-range' })
        : servePlan(request),
    )
    await screen.findByText('Week 6 of 12')
    await router.navigate('/app/plan?week=99')

    // the old week stays on screen until the refusal arrives, so the address is what shows the move
    await waitFor(() => expect(router.state.location.search).toBe(''))
    expect(await screen.findByText('Week 6 of 12')).toBeInTheDocument()
  })
  it('says the plan has finished, without a week strip, once it is past its last week', async () => {
    openPlan(() =>
      json({
        goal: 'LOSE_FAT',
        weeks: 12,
        currentWeek: 14,
        finished: true,
        seen: true,
        sessions: SESSIONS,
      }),
    )

    expect(
      await screen.findByRole('heading', {
        name: 'Your trainer is preparing your next plan.',
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByText('Your 12-week plan has finished.'),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Next week' }),
    ).not.toBeInTheDocument()
    expect(screen.queryByText('Rest day')).not.toBeInTheDocument()
  })

  it('marks an unseen plan as seen once, when the tab opens', async () => {
    let seen = false
    const { requests } = openPlan((request) => {
      if (request.method === 'POST') {
        seen = true
        return new Response(null, { status: 204 })
      }
      return json({ ...planAt(6), seen })
    })

    expect(await screen.findByText('Week 6 of 12')).toBeInTheDocument()
    await waitFor(() => expect(acknowledgements(requests)).toHaveLength(1))
    await screen.findByText('Week 6 of 12')
    expect(acknowledgements(requests)).toHaveLength(1)
  })

  it('does not mark a plan that was already seen', async () => {
    const { requests } = openPlan(servePlan)

    expect(await screen.findByText('Week 6 of 12')).toBeInTheDocument()
    expect(acknowledgements(requests)).toHaveLength(0)
  })

  it('shows the state of each session as a word, on the right day', async () => {
    openPlan(servePlanWithStates)

    const days = await screen.findAllByRole('listitem')
    // the mark names the state for assistive technology, and the card's badge shows it
    expect(await within(days[0]).findAllByText('Done')).toHaveLength(2)
    expect(within(days[3]).getAllByText('Missed')).toHaveLength(2)
    expect(within(days[1]).queryByText(/Done|Missed|Today/)).toBeNull()
  })

  it('marks the sessions whose targets were adjusted', async () => {
    openPlan(servePlanWithStates)

    const days = await screen.findAllByRole('listitem')
    expect(await within(days[0]).findByText('Adjusted')).toBeInTheDocument()
    expect(within(days[3]).queryByText('Adjusted')).toBeNull()
  })

  it('counts the sessions done in the week shown', async () => {
    openPlan(servePlanWithStates)

    expect(await screen.findByText('1 of 2 sessions done')).toBeInTheDocument()
  })

  it('offers the summary of a done session in its opened card', async () => {
    openPlan(servePlanWithStates)
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: /Upper body/ }))

    expect(screen.getByRole('link', { name: 'View summary' })).toHaveAttribute(
      'href',
      `/app/summary/${LOG}`,
    )
  })

  describe('the way to a past session', () => {
    const HOUR = 60 * 60 * 1000

    // week 6 with Monday's session started at `startedAt` and finished, and Thursday's missed without a log
    const withMondaysLog =
      (startedAt: Date, today = '2026-11-06'): Reply =>
      (request) =>
        isStates(request)
          ? json({
              today,
              sessions: [
                {
                  day: 'MONDAY',
                  date: '2026-11-02',
                  state: 'DONE',
                  sessionLogId: LOG,
                  startedAt: startedAt.toISOString(),
                },
                { day: 'THURSDAY', date: '2026-11-05', state: 'MISSED' },
              ],
            })
          : json(planAt(weekAsked(request)))

    it('offers "Edit log" within its 24 hours, beside the summary, and takes the week along for the way back', async () => {
      const { router } = openPlan(
        withMondaysLog(new Date(Date.now() - 2 * HOUR)),
        '/app/plan?week=6',
      )
      const user = userEvent.setup()

      await user.click(
        await screen.findByRole('button', { name: /Upper body/ }),
      )

      expect(screen.getByRole('link', { name: 'View summary' })).toBeVisible()
      await user.click(screen.getByRole('link', { name: 'Edit log' }))
      expect(router.state.location.pathname).toBe(`/app/session/${LOG}`)
      expect(router.state.location.state).toEqual({ from: '/app/plan?week=6' })
    })

    it('offers "View log" once its 24 hours have passed', async () => {
      openPlan(withMondaysLog(new Date(Date.now() - 25 * HOUR)))
      const user = userEvent.setup()

      await user.click(
        await screen.findByRole('button', { name: /Upper body/ }),
      )

      expect(screen.getByRole('link', { name: 'View log' })).toHaveAttribute(
        'href',
        `/app/session/${LOG}`,
      )
      expect(screen.queryByRole('link', { name: 'Edit log' })).toBeNull()
    })

    it('offers no log for a past day without one', async () => {
      openPlan(withMondaysLog(new Date()))
      const user = userEvent.setup()

      await user.click(await screen.findByRole('button', { name: /Core/ }))

      expect(
        screen.queryByRole('link', { name: /Edit log|View log/ }),
      ).toBeNull()
    })

    it("offers no log for today's session, which Today opens", async () => {
      openPlan(withMondaysLog(new Date(), '2026-11-02'))
      const user = userEvent.setup()

      await user.click(
        await screen.findByRole('button', { name: /Upper body/ }),
      )

      expect(screen.getByRole('link', { name: 'View summary' })).toBeVisible()
      expect(
        screen.queryByRole('link', { name: /Edit log|View log/ }),
      ).toBeNull()
    })
  })

  it('asks for the states of the week it shows', async () => {
    const { requests } = openPlan(servePlanWithStates, '/app/plan?week=5')

    await screen.findByText('Week 5 of 12')

    expect(
      requests
        .filter(isStates)
        .map((request) => new URL(request.url).searchParams.get('week')),
    ).toEqual(['5'])
  })

  it('still shows the plan, without states, when they cannot be read', async () => {
    openPlan((request) =>
      isStates(request)
        ? problem(500, { status: 500, code: 'internal.error' })
        : json(planAt(weekAsked(request))),
    )

    expect(await screen.findByText('Week 6 of 12')).toBeInTheDocument()
    expect(screen.queryByText(/sessions done/)).not.toBeInTheDocument()
    expect(screen.getByText('Upper body')).toBeInTheDocument()
  })
})
