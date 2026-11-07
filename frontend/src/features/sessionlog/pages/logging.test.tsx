import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, describe, expect, it } from 'vitest'
import i18n from '@/lib/i18n'
import { WeightUnitContext, type WeightUnit } from '@/lib/weight'
import { json, problem, stubApi, type Reply } from '@/test/page'
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

const started = () =>
  json(
    {
      id: '00000000-0000-4000-a000-000000000001',
      date: '2026-11-07',
      name: 'Lower body',
      startedAt: '2026-11-07T10:00:00Z',
      entries: [SQUAT],
    },
    201,
  )

function openLogger(reply: Reply, unit: WeightUnit = 'KG') {
  stubApi(reply)
  const router = createMemoryRouter(
    [
      { path: '/app/session', Component: LoggerPage },
      { path: '/app/today', Component: () => <h1>Today</h1> },
    ],
    { initialEntries: ['/app/session'] },
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
}

type Put = { id: string; body: Record<string, number> }

// the server answers a PUT with the set as it was sent, and records what it was sent
function server(failing: () => boolean = () => false) {
  const puts: Put[] = []
  const reply: Reply = async (request) => {
    if (request.method === 'POST') return started()
    if (failing()) return problem(500, { status: 500, code: 'internal.error' })
    const body = (await request.clone().json()) as Put['body']
    const id = new URL(request.url).pathname.split('/').pop()!
    puts.push({ id, body })
    return json({ id, loggedAt: '2026-11-07T10:05:00Z', ...body })
  }
  return { reply, puts }
}

const firstRow = () =>
  within(screen.getByRole('region', { name: 'Back squat' })).getAllByRole(
    'listitem',
  )[0]

const choose = (user: ReturnType<typeof userEvent.setup>, number = 1) =>
  user.click(
    screen.getByRole('button', {
      name: new RegExp(`^Choose set ${number} of Back squat`),
    }),
  )

describe('logging a set', () => {
  afterEach(() => i18n.changeLanguage('en'))

  it('logs a set as planned with one tick, and shows it as logged', async () => {
    const user = userEvent.setup()
    const { reply, puts } = server()
    openLogger(reply)

    await user.click(
      await screen.findByRole('button', {
        name: 'Log set 1 of Back squat as planned',
      }),
    )

    expect(await within(firstRow()).findByText('Logged')).toBeInTheDocument()
    expect(puts).toHaveLength(1)
    expect(puts[0].body).toEqual({
      entryPosition: 0,
      setNumber: 1,
      loadKg: 100,
      amount: 5,
      rpe: 8,
    })
    expect(puts[0].id).toMatch(/^[0-9a-f-]{36}$/)
    // the second set is untouched
    expect(
      screen.getByRole('button', {
        name: 'Log set 2 of Back squat as planned',
      }),
    ).toBeInTheDocument()
  })

  it('logs a set with changed values: a load step, fewer repetitions and another RPE', async () => {
    const user = userEvent.setup()
    const { reply, puts } = server()
    openLogger(reply)

    await screen.findByRole('region', { name: 'Back squat' })
    await choose(user)
    await user.click(
      screen.getByRole('button', { name: 'More load for set 1 of Back squat' }),
    )
    await user.click(
      screen.getByRole('button', {
        name: 'Fewer: Repetitions, set 1 of Back squat',
      }),
    )
    await user.click(screen.getByRole('radio', { name: /^RPE 9 for set 1/ }))
    await user.click(screen.getByRole('button', { name: 'Log set' }))

    expect(await within(firstRow()).findByText('Logged')).toBeInTheDocument()
    expect(within(firstRow()).getByText('4 reps')).toBeInTheDocument()
    expect(within(firstRow()).getByText('102.5 kg')).toBeInTheDocument()
    expect(within(firstRow()).getByText('RPE 9')).toBeInTheDocument()
    expect(puts[0].body).toMatchObject({ loadKg: 102.5, amount: 4, rpe: 9 })
    // the panel closes once the set is logged
    expect(
      screen.queryByRole('button', { name: 'Log set' }),
    ).not.toBeInTheDocument()
  })

  it('offers RPE from 5 to 10 only', async () => {
    const user = userEvent.setup()
    openLogger(server().reply)

    await screen.findByRole('region', { name: 'Back squat' })
    await choose(user)

    expect(
      screen.getAllByRole('radio').map((chip) => chip.textContent),
    ).toEqual(['5', '6', '7', '8', '9', '10'])
  })

  it('sends a load typed in pounds in kilograms', async () => {
    const user = userEvent.setup()
    const { reply, puts } = server()
    openLogger(reply, 'LB')

    await screen.findByRole('region', { name: 'Back squat' })
    await choose(user)
    const load = screen.getByRole('textbox', {
      name: 'Load in lb for set 1 of Back squat',
    })
    await user.clear(load)
    await user.type(load, '225')
    await user.click(screen.getByRole('button', { name: 'Log set' }))

    await within(firstRow()).findByText('Logged')
    expect(puts[0].body.loadKg).toBe(102.1)
  })

  it('does not log 0 repetitions and says how to fix the field', async () => {
    const user = userEvent.setup()
    const { reply, puts } = server()
    openLogger(reply)

    await screen.findByRole('region', { name: 'Back squat' })
    await choose(user)
    const amount = screen.getByRole('textbox', {
      name: 'Repetitions for set 1 of Back squat',
    })
    await user.clear(amount)
    await user.type(amount, '0')
    await user.click(screen.getByRole('button', { name: 'Log set' }))

    expect(amount).toHaveAttribute('aria-invalid', 'true')
    expect(
      screen.getByText('Enter a whole number from 1 to 3600.'),
    ).toBeInTheDocument()
    expect(puts).toHaveLength(0)
  })

  it('does not log a load of 0', async () => {
    const user = userEvent.setup()
    const { reply, puts } = server()
    openLogger(reply)

    await screen.findByRole('region', { name: 'Back squat' })
    await choose(user)
    const load = screen.getByRole('textbox', {
      name: 'Load in kg for set 1 of Back squat',
    })
    await user.clear(load)
    await user.type(load, '0')
    await user.click(screen.getByRole('button', { name: 'Log set' }))

    expect(load).toHaveAttribute('aria-invalid', 'true')
    expect(puts).toHaveLength(0)
  })

  it('changes a logged set with the id it was logged with, and updates the row', async () => {
    const user = userEvent.setup()
    const { reply, puts } = server()
    openLogger(reply)
    await user.click(
      await screen.findByRole('button', {
        name: 'Log set 1 of Back squat as planned',
      }),
    )
    await within(firstRow()).findByText('Logged')

    await user.click(
      screen.getByRole('button', { name: /^Change set 1 of Back squat/ }),
    )
    await user.click(
      screen.getByRole('button', {
        name: 'More: Repetitions, set 1 of Back squat',
      }),
    )
    await user.click(screen.getByRole('button', { name: 'Update set' }))

    expect(await within(firstRow()).findByText('6 reps')).toBeInTheDocument()
    expect(puts).toHaveLength(2)
    expect(puts[1].id).toBe(puts[0].id)
    expect(puts[1].body.amount).toBe(6)
  })

  it('keeps what was entered when logging fails, and logs it on a retry', async () => {
    const user = userEvent.setup()
    let failing = true
    const { reply, puts } = server(() => failing)
    openLogger(reply)

    await screen.findByRole('region', { name: 'Back squat' })
    await choose(user)
    await user.click(
      screen.getByRole('button', { name: 'More load for set 1 of Back squat' }),
    )
    await user.click(screen.getByRole('button', { name: 'Log set' }))

    expect(await screen.findByRole('alert')).toBeInTheDocument()
    expect(
      screen.getByRole('textbox', {
        name: 'Load in kg for set 1 of Back squat',
      }),
    ).toHaveValue('102.5')

    failing = false
    await user.click(screen.getByRole('button', { name: 'Log set' }))
    await within(firstRow()).findByText('Logged')
    expect(puts).toHaveLength(1)
    expect(puts[0].body.loadKg).toBe(102.5)
  })

  it('sends the same id when a tick is retried after a failure', async () => {
    const user = userEvent.setup()
    let failing = true
    const ids: string[] = []
    openLogger(async (request) => {
      if (request.method === 'POST') return started()
      ids.push(new URL(request.url).pathname.split('/').pop()!)
      if (failing) return problem(500, { status: 500, code: 'internal.error' })
      return json({
        id: ids[0],
        loggedAt: '2026-11-07T10:05:00Z',
        ...(await request.clone().json()),
      })
    })
    const tick = await screen.findByRole('button', {
      name: 'Log set 1 of Back squat as planned',
    })

    await user.click(tick)
    await screen.findByRole('alert')
    failing = false
    await user.click(tick)

    await within(firstRow()).findByText('Logged')
    expect(ids).toHaveLength(2)
    expect(ids[1]).toBe(ids[0])
  })

  it('gives the controls of the entry panel a touch target of 48 px', async () => {
    const user = userEvent.setup()
    openLogger(server().reply)

    await screen.findByRole('region', { name: 'Back squat' })
    await choose(user)

    const controls = [
      screen.getByRole('button', { name: 'Less load for set 1 of Back squat' }),
      screen.getByRole('button', { name: 'More load for set 1 of Back squat' }),
      screen.getByRole('button', { name: 'Log set' }),
      ...screen.getAllByRole('radio'),
    ]
    // jsdom has no layout, so what can be checked is the class that gives 48 px
    for (const control of controls) {
      expect(control.className).toMatch(/size-12|w-12|h-\(--control-h-touch\)/)
    }
  })

  it('speaks Polish in the entry panel', async () => {
    await i18n.changeLanguage('pl')
    const user = userEvent.setup()
    openLogger(server().reply)

    await user.click(
      await screen.findByRole('button', {
        name: /^Wybierz serię 1: Back squat/,
      }),
    )

    expect(screen.getByText('Powtórzenia')).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Zapisz serię' }),
    ).toBeInTheDocument()
  })
})
