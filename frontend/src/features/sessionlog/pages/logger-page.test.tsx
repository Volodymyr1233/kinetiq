import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, describe, expect, it } from 'vitest'
import i18n from '@/lib/i18n'
import { WeightUnitContext, type WeightUnit } from '@/lib/weight'
import { json, problem, stubApi, type Reply, loggerClient } from '@/test/page'
import { LoggerPage } from './logger-page'

const SQUAT = {
  position: 0,
  exerciseId: '00000000-0000-4000-9100-000000000011',
  exerciseName: 'Back squat',
  measure: 'REPS',
  restSeconds: 120,
  sets: [
    { number: 1, target: { loadKg: 100, amount: 5, rpe: 8 } },
    { number: 2, target: { loadKg: 100, amount: 5, rpe: 8 } },
  ],
}
const PLANK = {
  position: 1,
  exerciseId: '00000000-0000-4000-9100-000000000026',
  exerciseName: 'Plank',
  measure: 'SECONDS',
  restSeconds: 60,
  sets: [{ number: 1, target: { amount: 45, rpe: 7 } }],
}

const session = (entries: object[] = [SQUAT, PLANK]) => ({
  id: '00000000-0000-4000-a000-000000000001',
  date: '2026-11-07',
  name: 'Lower body',
  // started just now, so the session is open: it locks 24 hours after its start
  startedAt: new Date().toISOString(),
  entries,
})

const started = () => json(session(), 201)

function openLogger(reply: Reply, unit: WeightUnit = 'KG') {
  const requests = stubApi(reply)
  // the same address as in app/router.ts; a feature does not import the app (frontend/CLAUDE.md)
  const router = createMemoryRouter(
    [
      { path: '/app/session', Component: LoggerPage },
      { path: '/app/today', Component: () => <h1>Today</h1> },
    ],
    { initialEntries: ['/app/session'] },
  )
  render(
    <QueryClientProvider client={loggerClient()}>
      <WeightUnitContext value={unit}>
        <RouterProvider router={router} />
      </WeightUnitContext>
    </QueryClientProvider>,
  )
  return { requests, router }
}

describe('LoggerPage', () => {
  afterEach(() => i18n.changeLanguage('en'))

  it("starts today's session and lists each exercise with one numbered row per planned set", async () => {
    const { requests } = openLogger(started)

    expect(
      await screen.findByRole('heading', { name: 'Lower body' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Saturday, November 7')).toBeInTheDocument()
    const squat = within(screen.getByRole('region', { name: 'Back squat' }))
    expect(squat.getAllByRole('listitem')).toHaveLength(2)
    expect(squat.getAllByText('5 reps')).toHaveLength(2)
    expect(squat.getAllByText('100 kg')).toHaveLength(2)
    expect(squat.getAllByText('RPE 8')).toHaveLength(2)
    expect(
      within(screen.getByRole('region', { name: 'Plank' })).getAllByRole(
        'listitem',
      ),
    ).toHaveLength(1)
    expect(
      requests.map((r) => `${r.method} ${new URL(r.url).pathname}`),
    ).toEqual(['POST /api/v1/session-logs'])
  })

  it('shows a timed exercise in seconds and a set without a load as "No load"', async () => {
    openLogger(started)

    const plank = within(await screen.findByRole('region', { name: 'Plank' }))
    expect(plank.getByText('45 s')).toBeInTheDocument()
    expect(plank.getByText('No load')).toBeInTheDocument()
    expect(plank.queryByText(/reps/)).not.toBeInTheDocument()
  })

  it('shows a load in pounds to a client whose unit is pounds', async () => {
    openLogger(started, 'LB')

    expect(await screen.findAllByText('220.5 lb')).toHaveLength(2)
  })

  it('shows the same exercise twice as two groups', async () => {
    openLogger(() => json(session([SQUAT, { ...SQUAT, position: 1 }])))

    expect(
      await screen.findAllByRole('region', { name: 'Back squat' }),
    ).toHaveLength(2)
  })

  it('shows a set that was logged with what the client did, marked by text', async () => {
    const logged = {
      ...SQUAT,
      sets: [
        {
          ...SQUAT.sets[0],
          log: {
            id: '00000000-0000-4000-b000-000000000001',
            loadKg: 102.5,
            amount: 6,
            rpe: 9,
            loggedAt: '2026-11-07T10:05:00Z',
          },
        },
        SQUAT.sets[1],
      ],
    }
    openLogger(() => json(session([logged])))

    const rows = within(
      await screen.findByRole('region', { name: 'Back squat' }),
    ).getAllByRole('listitem')
    expect(within(rows[0]).getByText('Logged')).toBeInTheDocument()
    expect(within(rows[0]).getByText('6 reps')).toBeInTheDocument()
    expect(within(rows[0]).getByText('102.5 kg')).toBeInTheDocument()
    expect(within(rows[0]).getByText('RPE 9')).toBeInTheDocument()
    expect(within(rows[1]).queryByText('Logged')).not.toBeInTheDocument()
  })

  it('goes back to Today when there is no session to log', async () => {
    openLogger(() =>
      problem(409, { status: 409, code: 'session-log.no-session-today' }),
    )

    expect(
      await screen.findByRole('heading', { name: 'Today' }),
    ).toBeInTheDocument()
  })

  it('goes back to Today when the client has no published plan', async () => {
    openLogger(() => problem(404, { status: 404, code: 'plan.not-found' }))

    expect(
      await screen.findByRole('heading', { name: 'Today' }),
    ).toBeInTheDocument()
  })

  it('offers to try again when the session cannot be loaded', async () => {
    const user = userEvent.setup()
    let failing = true
    openLogger(() =>
      failing
        ? problem(429, { status: 429, code: 'request.too-many' })
        : started(),
    )

    const retry = await screen.findByRole('button', { name: 'Try again' })
    failing = false
    await user.click(retry)

    expect(
      await screen.findByRole('heading', { name: 'Lower body' }),
    ).toBeInTheDocument()
  })

  it('speaks Polish when the UI does', async () => {
    await i18n.changeLanguage('pl')
    openLogger(started)

    expect(
      await screen.findByRole('heading', { name: 'Lower body' }),
    ).toBeInTheDocument()
    expect(screen.getByText('sobota, 7 listopada')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Dzisiaj' })).toBeInTheDocument()
    expect(screen.getAllByText('5 powt.')).toHaveLength(2)
  })

  it('shows the lighter targets as planned → today on a recovery day, and logs a set at them', async () => {
    const user = userEvent.setup()
    const lighter = { loadKg: 90, amount: 5, rpe: 7 }
    const adjusted = {
      ...SQUAT,
      planned: { loadKg: 100, amount: 5, rpe: 8 },
      sets: SQUAT.sets.map((set) => ({ ...set, target: lighter })),
    }
    const { requests } = openLogger((request) =>
      request.method === 'PUT'
        ? json({
            id: '00000000-0000-4000-b000-000000000001',
            entryPosition: 0,
            setNumber: 1,
            ...lighter,
            loggedAt: new Date().toISOString(),
          })
        : json(session([adjusted]), 201),
    )

    const squat = within(
      await screen.findByRole('region', { name: 'Back squat' }),
    )
    // marked once for the session, not on each exercise
    expect(screen.getAllByText('Adjusted')).toHaveLength(1)
    expect(squat.queryByText('Adjusted')).toBeNull()
    expect(squat.getByText('100 kg, today 90 kg')).toBeInTheDocument()
    expect(squat.getByText('RPE 8, today RPE 7')).toBeInTheDocument()
    // the amount did not change, so it is not repeated in the line
    expect(squat.queryByText(/5 reps, today/)).toBeNull()

    await user.click(squat.getAllByRole('button', { name: /^Log set 1/ })[0])

    const put = requests.find((request) => request.method === 'PUT')
    expect(await put?.json()).toMatchObject({ loadKg: 90, amount: 5, rpe: 7 })
  })
})
