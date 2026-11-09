import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it } from 'vitest'
import { json, stubApi, type Reply, loggerClient } from '@/test/page'
import { LoggerPage } from './logger-page'

const entry = (position: number, restSeconds: number) => ({
  position,
  exerciseId: '00000000-0000-4000-9100-000000000011',
  exerciseName: position === 0 ? 'Back squat' : 'Bench press',
  measure: 'REPS',
  restSeconds,
  sets: [
    { number: 1, target: { loadKg: 100, amount: 5, rpe: 8 } },
    { number: 2, target: { loadKg: 100, amount: 5, rpe: 8 } },
  ],
})

function openLogger(entries: object[]) {
  // a session of its own per test: the logger keeps a running rest by session
  const id = crypto.randomUUID()
  const reply: Reply = async (request) => {
    if (request.method === 'POST') {
      return json(
        {
          id,
          date: '2026-11-07',
          name: 'Lower body',
          // started just now, so the session is open: it locks 24 hours after its start
          startedAt: new Date().toISOString(),
          entries,
        },
        201,
      )
    }
    const body = await request.clone().json()
    return json({
      id: new URL(request.url).pathname.split('/').pop(),
      loggedAt: '2026-11-07T10:05:00Z',
      ...body,
    })
  }
  stubApi(reply)
  const router = createMemoryRouter(
    [
      { path: '/app/session', Component: LoggerPage },
      { path: '/app/plan', Component: () => <h1>Plan</h1> },
    ],
    { initialEntries: ['/app/session'] },
  )
  render(
    <QueryClientProvider client={loggerClient()}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
  return router
}

const tick = (exercise: string, number: number) =>
  screen.findByRole('button', {
    name: `Log set ${number} of ${exercise} as planned`,
  })

describe('the rest timer in the logger', () => {
  it("starts with the exercise's rest after a set is logged", async () => {
    const user = userEvent.setup()
    openLogger([entry(0, 120)])

    expect(screen.queryByRole('timer')).not.toBeInTheDocument()
    await user.click(await tick('Back squat', 1))

    expect(await screen.findByRole('timer')).toHaveTextContent(/^(2:00|1:59)$/)
  })

  it('does not block input while it runs, and a set logged meanwhile restarts it with its own rest', async () => {
    const user = userEvent.setup()
    openLogger([entry(0, 120), entry(1, 45)])
    await user.click(await tick('Back squat', 1))
    expect(await screen.findByRole('timer')).toHaveTextContent(/^(2:00|1:59)$/)

    // the panel of another set opens and the next tick logs, while the countdown runs
    await user.click(
      screen.getByRole('button', { name: /^Choose set 2 of Back squat/ }),
    )
    expect(screen.getByRole('button', { name: 'Log set' })).toBeEnabled()
    await user.click(screen.getByRole('button', { name: 'Log set' }))
    await user.click(await tick('Bench press', 1))

    const rows = within(
      screen.getByRole('region', { name: 'Bench press' }),
    ).getAllByRole('listitem')
    expect(await within(rows[0]).findByText('Logged')).toBeInTheDocument()
    expect(screen.getByRole('timer')).toHaveTextContent(/^(0:45|0:44)$/)
  })

  it('keeps running when the client leaves the logger for another tab and comes back', async () => {
    const user = userEvent.setup()
    const router = openLogger([entry(0, 120)])
    await user.click(await tick('Back squat', 1))
    await screen.findByRole('timer')

    await router.navigate('/app/plan')
    await screen.findByRole('heading', { name: 'Plan' })
    await router.navigate('/app/session')

    expect(await screen.findByRole('timer')).toHaveTextContent(
      /^(2:00|1:59|1:58)$/,
    )
  })

  it('goes away when it is dismissed', async () => {
    const user = userEvent.setup()
    openLogger([entry(0, 120)])
    await user.click(await tick('Back squat', 1))
    await screen.findByRole('timer')

    await user.click(
      screen.getByRole('button', { name: 'Dismiss the rest timer' }),
    )

    expect(screen.queryByRole('timer')).not.toBeInTheDocument()
  })

  it('does not start for an exercise with no rest', async () => {
    const user = userEvent.setup()
    openLogger([entry(0, 0)])

    await user.click(await tick('Back squat', 1))

    await within(screen.getByRole('region', { name: 'Back squat' })).findByText(
      'Logged',
    )
    expect(screen.queryByRole('timer')).not.toBeInTheDocument()
  })

  it('does not start when a logged set is only changed', async () => {
    const user = userEvent.setup()
    openLogger([entry(0, 120)])
    await user.click(await tick('Back squat', 1))
    await screen.findByRole('timer')
    await user.click(
      screen.getByRole('button', { name: 'Dismiss the rest timer' }),
    )

    await user.click(
      screen.getByRole('button', { name: /^Change set 1 of Back squat/ }),
    )
    await user.click(screen.getByRole('button', { name: 'Update set' }))

    await within(
      screen.getByRole('region', { name: 'Back squat' }),
    ).findAllByText('Logged')
    expect(screen.queryByRole('timer')).not.toBeInTheDocument()
  })
})
