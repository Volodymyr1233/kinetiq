import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it } from 'vitest'
import { json, problem, stubApi, type Reply } from '@/test/page'
import { LoggerPage } from './logger-page'

const SESSION_ID = '00000000-0000-4000-a000-000000000001'

const logged = (number: number) => ({
  number,
  target: { loadKg: 100, amount: 5, rpe: 8 },
  log: {
    id: `00000000-0000-4000-b000-00000000000${number}`,
    loadKg: 100,
    amount: 5,
    rpe: 8,
    loggedAt: '2026-11-07T10:05:00Z',
  },
})
const open = (number: number) => ({
  number,
  target: { loadKg: 100, amount: 5, rpe: 8 },
})

function session(sets: object[], extra: object = {}) {
  return {
    id: SESSION_ID,
    date: '2026-11-07',
    name: 'Lower body',
    startedAt: new Date().toISOString(),
    entries: [
      {
        position: 0,
        exerciseId: '00000000-0000-4000-9100-000000000011',
        exerciseName: 'Back squat',
        measure: 'REPS',
        restSeconds: 120,
        sets,
      },
    ],
    ...extra,
  }
}

const SUMMARY = {
  sessionLogId: SESSION_ID,
  name: 'Lower body',
  date: '2026-11-07',
  startedAt: new Date().toISOString(),
  setsLogged: 2,
  setsPlanned: 2,
  averageRpe: 8,
  minutes: 5,
}

function openLogger(body: object, finish?: Reply) {
  const requests = stubApi((request) => {
    const path = new URL(request.url).pathname
    if (path.endsWith('/finish')) {
      return finish ? finish(request) : json(SUMMARY)
    }
    return json(body)
  })
  const router = createMemoryRouter(
    [
      { path: '/app/session', Component: LoggerPage },
      { path: '/app/summary/:id', Component: () => <p>Done screen</p> },
    ],
    { initialEntries: ['/app/session'] },
  )
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
  return requests
}

const finishes = (requests: Request[]) =>
  requests.filter((request) => request.url.endsWith('/finish'))

describe('finishing a session in the logger', () => {
  it('cannot finish, and says why, while no set is logged', async () => {
    openLogger(session([open(1), open(2)]))

    const button = await screen.findByRole('button', {
      name: 'Finish session',
    })

    expect(button).toBeDisabled()
    expect(button).toHaveAccessibleDescription(
      'Log at least one set to finish.',
    )
    expect(screen.getByText('0 of 2 sets logged')).toBeInTheDocument()
  })

  it('finishes at once and shows the done screen when every set is logged', async () => {
    const requests = openLogger(session([logged(1), logged(2)]))
    const user = userEvent.setup()

    await user.click(
      await screen.findByRole('button', { name: 'Finish session' }),
    )

    expect(await screen.findByText('Done screen')).toBeInTheDocument()
    expect(finishes(requests)).toHaveLength(1)
  })

  it('asks first, naming the sets not logged, and Keep logging changes nothing', async () => {
    const requests = openLogger(session([logged(1), open(2)]))
    const user = userEvent.setup()
    const button = await screen.findByRole('button', {
      name: 'Finish session',
    })

    await user.click(button)
    const dialog = await screen.findByRole('alertdialog')
    expect(dialog).toHaveTextContent('1 set is not logged.')
    await user.click(screen.getByRole('button', { name: 'Keep logging' }))

    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    expect(finishes(requests)).toHaveLength(0)
    expect(button).toHaveFocus()
  })

  it('finishes after the question is accepted', async () => {
    const requests = openLogger(session([logged(1), open(2)]))
    const user = userEvent.setup()

    await user.click(
      await screen.findByRole('button', { name: 'Finish session' }),
    )
    const dialog = await screen.findByRole('alertdialog')
    await user.click(
      screen.getByRole('button', { name: 'Finish session', description: '' }),
    )

    expect(dialog).not.toBeInTheDocument()
    expect(await screen.findByText('Done screen')).toBeInTheDocument()
    expect(finishes(requests)).toHaveLength(1)
  })

  it('shows a translated message and keeps the logger when the finish fails', async () => {
    openLogger(session([logged(1), logged(2)]), () =>
      problem(500, { status: 500, code: 'internal.error' }),
    )
    const user = userEvent.setup()

    await user.click(
      await screen.findByRole('button', { name: 'Finish session' }),
    )

    expect(
      await screen.findByText(
        'Something went wrong on our side. Try again in a moment.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByText('Back squat')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Finish session' })).toBeEnabled()
  })

  it('turns the logger read-only when the server says the session is locked', async () => {
    openLogger(session([logged(1), logged(2)]), () =>
      problem(409, { status: 409, code: 'session-log.locked' }),
    )
    const user = userEvent.setup()

    await user.click(
      await screen.findByRole('button', { name: 'Finish session' }),
    )

    expect(await screen.findAllByText('Session locked')).not.toHaveLength(0)
    expect(
      screen.queryByRole('button', { name: 'Finish session' }),
    ).not.toBeInTheDocument()
  })

  it('offers the summary instead when the session is finished and open', async () => {
    openLogger(
      session([logged(1), logged(2)], { finishedAt: '2026-11-07T10:30:00Z' }),
    )

    const link = await screen.findByRole('link', { name: 'View summary' })

    expect(link).toHaveAttribute('href', `/app/summary/${SESSION_ID}`)
    expect(
      screen.queryByRole('button', { name: 'Finish session' }),
    ).not.toBeInTheDocument()
  })

  it('offers nothing when the session is locked', async () => {
    openLogger(
      session([logged(1), logged(2)], {
        startedAt: new Date(Date.now() - 25 * 3600 * 1000).toISOString(),
      }),
    )

    await screen.findByText('Session locked')

    expect(
      screen.queryByRole('button', { name: 'Finish session' }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('link', { name: 'View summary' }),
    ).not.toBeInTheDocument()
  })
})
