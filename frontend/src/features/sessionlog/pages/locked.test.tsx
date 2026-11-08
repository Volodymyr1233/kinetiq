import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import i18n from '@/lib/i18n'
import { json, problem, stubApi, type Reply } from '@/test/page'
import { LoggerPage } from './logger-page'

const HOUR = 60 * 60 * 1000
const REASON =
  'Sets can be logged and changed for 24 hours after a session starts.'

const SQUAT = {
  position: 0,
  exerciseId: '00000000-0000-4000-9100-000000000011',
  exerciseName: 'Back squat',
  measure: 'REPS',
  restSeconds: 120,
  sets: [
    {
      number: 1,
      target: { loadKg: 100, amount: 5, rpe: 8 },
      log: {
        id: '00000000-0000-4000-b000-000000000001',
        loadKg: 102.5,
        amount: 5,
        rpe: 9,
        loggedAt: '2026-11-07T10:05:00Z',
      },
    },
    { number: 2, target: { loadKg: 100, amount: 5, rpe: 8 } },
  ],
}

// a log is answered with what was sent, as the server stores it
const stored: Reply = async (request) =>
  json({
    id: new URL(request.url).pathname.split('/').pop(),
    loggedAt: new Date().toISOString(),
    ...(await request.clone().json()),
  })

function openLogger(startedAt: Date, put: Reply = stored) {
  stubApi((request) =>
    request.method === 'POST'
      ? json({
          id: '00000000-0000-4000-a000-000000000001',
          date: '2026-11-07',
          name: 'Lower body',
          startedAt: startedAt.toISOString(),
          entries: [SQUAT],
        })
      : put(request),
  )
  const router = createMemoryRouter(
    [{ path: '/app/session', Component: LoggerPage }],
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
}

const rows = () =>
  within(screen.getByRole('region', { name: 'Back squat' })).getAllByRole(
    'listitem',
  )

describe('a locked session in the logger', () => {
  afterEach(async () => {
    vi.useRealTimers()
    await i18n.changeLanguage('en')
  })

  it('shows a session started 24 hours ago read-only and says why, with nothing to focus but the way back', async () => {
    const user = userEvent.setup()
    openLogger(new Date(Date.now() - 24 * HOUR))

    expect(await screen.findByText('Session locked')).toBeInTheDocument()
    expect(screen.getByText(REASON)).toBeInTheDocument()
    expect(rows()[0]).toHaveTextContent(/102\.5/)
    expect(rows()[0]).toHaveTextContent('Logged')
    expect(rows()[1]).toHaveTextContent('Not logged')
    expect(
      within(screen.getByRole('region', { name: 'Back squat' })).queryByRole(
        'button',
      ),
    ).not.toBeInTheDocument()
    // locked on arrival, so nothing changed while the client watched
    expect(screen.getByRole('status')).toBeEmptyDOMElement()

    await user.tab()
    expect(screen.getByRole('link', { name: 'Today' })).toHaveFocus()
    await user.tab()
    expect(document.body).toHaveFocus()
  })

  it('turns read-only when its 24 hours end while it is open, closing the panel and the rest timer, and says so', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    openLogger(new Date(Date.now() - 24 * HOUR + 60_000))
    await user.click(
      await screen.findByRole('button', {
        name: 'Log set 2 of Back squat as planned',
      }),
    )
    expect(await screen.findByRole('timer')).toBeInTheDocument()
    await user.click(
      screen.getByRole('button', { name: /^Change set 1 of Back squat/ }),
    )
    expect(
      screen.getByRole('button', { name: 'Update set' }),
    ).toBeInTheDocument()

    await act(() => vi.advanceTimersByTimeAsync(60_000))

    expect(screen.getByText(REASON)).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Update set' }),
    ).not.toBeInTheDocument()
    expect(screen.queryByRole('timer')).not.toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Session locked')
  })

  it('turns read-only when the server refuses a log as locked, and the set stays as it was', async () => {
    const user = userEvent.setup()
    openLogger(new Date(), async () =>
      problem(409, { status: 409, code: 'session-log.locked' }),
    )

    await user.click(
      await screen.findByRole('button', {
        name: 'Log set 2 of Back squat as planned',
      }),
    )

    expect(await screen.findByText(REASON)).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Session locked')
    expect(rows()[1]).toHaveTextContent('Not logged')
    expect(
      screen.queryByText('Something went wrong. Try again.'),
    ).not.toBeInTheDocument()
  })

  it('says it in Polish', async () => {
    await i18n.changeLanguage('pl')
    openLogger(new Date(Date.now() - 25 * HOUR))

    expect(await screen.findByText('Sesja zablokowana')).toBeInTheDocument()
    expect(
      screen.getByText(
        'Serie można zapisywać i zmieniać przez 24 godziny od rozpoczęcia sesji.',
      ),
    ).toBeInTheDocument()
    expect(rows()[1]).toHaveTextContent('Nie zapisano')
  })
})
