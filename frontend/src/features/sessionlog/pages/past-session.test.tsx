import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it } from 'vitest'
import { json, problem, stubApi, type Reply, loggerClient } from '@/test/page'
import { LoggerPage } from './logger-page'

const HOUR = 60 * 60 * 1000
const ID = '00000000-0000-4000-a000-000000000007'

const session = (startedAt: Date) => ({
  id: ID,
  date: '2026-11-05',
  name: 'Lower body',
  startedAt: startedAt.toISOString(),
  entries: [
    {
      position: 0,
      exerciseId: '00000000-0000-4000-9100-000000000011',
      exerciseName: 'Back squat',
      measure: 'REPS',
      restSeconds: 0,
      sets: [{ number: 1, target: { loadKg: 100, amount: 5, rpe: 8 } }],
    },
  ],
})

// a GET reads the session, a PUT logs a set and is answered with what was sent
const serving =
  (startedAt: Date): Reply =>
  async (request) =>
    request.method === 'GET'
      ? json(session(startedAt))
      : json({
          id: new URL(request.url).pathname.split('/').pop(),
          loggedAt: new Date().toISOString(),
          ...(await request.clone().json()),
        })

// opened from the Plan card of week 5, which passes its address along
function openPastSession(
  reply: Reply,
  from: string | null = '/app/plan?week=5',
) {
  const requests = stubApi(reply)
  const router = createMemoryRouter(
    [
      { path: '/app/session/:id', Component: LoggerPage },
      { path: '/app/plan', Component: () => <h1>Plan</h1> },
      { path: '/app/today', Component: () => <h1>Today</h1> },
    ],
    {
      initialEntries: [
        {
          pathname: `/app/session/${ID}`,
          state: from === null ? null : { from },
        },
      ],
    },
  )
  render(
    <QueryClientProvider client={loggerClient()}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
  return requests
}

describe('the logger on an earlier session', () => {
  it('reads the session of the id in the address and starts nothing', async () => {
    const requests = openPastSession(serving(new Date(Date.now() - 12 * HOUR)))

    expect(
      await screen.findByRole('heading', { name: 'Lower body' }),
    ).toBeInTheDocument()
    expect(
      requests.map((r) => `${r.method} ${new URL(r.url).pathname}`),
    ).toEqual([`GET /api/v1/session-logs/${ID}`])
  })

  it('logs a set into it while its 24 hours last', async () => {
    const user = userEvent.setup()
    openPastSession(serving(new Date(Date.now() - 12 * HOUR)))

    await user.click(
      await screen.findByRole('button', {
        name: 'Log set 1 of Back squat as planned',
      }),
    )

    expect(
      await within(
        screen.getByRole('region', { name: 'Back squat' }),
      ).findByText('Logged'),
    ).toBeInTheDocument()
  })

  it('shows it locked once its 24 hours have passed', async () => {
    openPastSession(serving(new Date(Date.now() - 25 * HOUR)))

    expect(
      await screen.findByText(
        'Sets can be logged and changed for 24 hours after a session starts.',
      ),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: /Log set 1/ }),
    ).not.toBeInTheDocument()
  })

  it('goes back to the Plan week it came from', async () => {
    openPastSession(serving(new Date(Date.now() - 12 * HOUR)))

    expect(await screen.findByRole('link', { name: 'Plan' })).toHaveAttribute(
      'href',
      '/app/plan?week=5',
    )
  })

  it('goes back to Today when it was not opened from the Plan tab', async () => {
    openPastSession(serving(new Date(Date.now() - 12 * HOUR)), null)

    expect(await screen.findByRole('link', { name: 'Today' })).toHaveAttribute(
      'href',
      '/app/today',
    )
  })

  it("goes to the Plan tab when the session is not the client's or no longer there", async () => {
    openPastSession(() =>
      problem(404, { status: 404, code: 'session-log.not-found' }),
    )

    expect(
      await screen.findByRole('heading', { name: 'Plan' }),
    ).toBeInTheDocument()
  })
})
