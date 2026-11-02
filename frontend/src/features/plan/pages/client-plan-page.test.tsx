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

const servePlan: Reply = (request) => json(planAt(weekAsked(request)))

function openPlan(
  reply: Reply,
  address = '/app/plan',
  unit: WeightUnit = 'KG',
) {
  const requests = stubApi(reply)
  const router = createMemoryRouter(
    [{ path: '/app/plan', Component: ClientPlanPage }],
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
})
