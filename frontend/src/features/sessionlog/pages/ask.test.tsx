import { onlineManager, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { WeightUnitContext } from '@/lib/weight'
import { json, loggerClient, problem, stubApi } from '@/test/page'
import { LoggerPage } from './logger-page'

const HOUR = 60 * 60 * 1000
const SESSION = '00000000-0000-4000-a000-000000000001'
const SET_LOG = '00000000-0000-4000-b000-000000000001'
const ASK_SET_1 = 'Ask your trainer about set 1 of Back squat'

const SQUAT = {
  position: 0,
  exerciseId: '00000000-0000-4000-9100-000000000011',
  exerciseName: 'Back squat',
  measure: 'REPS',
  restSeconds: 0,
  sets: [
    {
      number: 1,
      target: { loadKg: 100, amount: 5, rpe: 8 },
      log: {
        id: SET_LOG,
        loadKg: 102.5,
        amount: 5,
        rpe: 9,
        loggedAt: '2026-11-07T10:05:00Z',
      },
    },
    { number: 2, target: { loadKg: 100, amount: 5, rpe: 8 } },
  ],
}

// every set log is refused, so a set logged here ends as not synced
function openLogger(
  startedAt = new Date(),
  onAsk: ((about: object) => void) | undefined = vi.fn(),
) {
  stubApi((request) =>
    request.method === 'POST'
      ? json({
          id: SESSION,
          date: '2026-11-07',
          name: 'Lower body',
          startedAt: startedAt.toISOString(),
          entries: [SQUAT],
        })
      : problem(400, { status: 400, code: 'request.invalid' }),
  )
  const router = createMemoryRouter(
    [{ path: '/app/session', element: <LoggerPage onAsk={onAsk} /> }],
    { initialEntries: ['/app/session'] },
  )
  render(
    <QueryClientProvider client={loggerClient()}>
      <WeightUnitContext value="KG">
        <RouterProvider router={router} />
      </WeightUnitContext>
    </QueryClientProvider>,
  )
  return onAsk
}

const squat = () => within(screen.getByRole('region', { name: 'Back squat' }))
const askButtons = () =>
  squat().queryAllByRole('button', { name: /^Ask your trainer about/ })
const logSet2 = () =>
  userEvent.click(
    screen.getByRole('button', { name: 'Log set 2 of Back squat as planned' }),
  )

describe('asking the trainer about a set in the logger', () => {
  afterEach(() => {
    onlineManager.setOnline(true)
  })

  it('offers it on a set the server has stored, and passes that set with its session, exercise, place and RPE', async () => {
    const onAsk = openLogger()

    await userEvent.click(
      await screen.findByRole('button', { name: ASK_SET_1 }),
    )

    expect(onAsk).toHaveBeenCalledExactlyOnceWith({
      setLogId: SET_LOG,
      sessionLogId: SESSION,
      sessionName: 'Lower body',
      sessionDate: '2026-11-07',
      exerciseName: 'Back squat',
      entryPosition: 0,
      setNumber: 1,
      rpe: 9,
    })
    // asking is not choosing the row: no entry panel opens
    expect(
      screen.queryByRole('button', { name: 'Update set' }),
    ).not.toBeInTheDocument()
  })

  it('does not offer it on a set that is not logged', async () => {
    openLogger()

    await screen.findByRole('button', { name: ASK_SET_1 })

    expect(askButtons()).toHaveLength(1)
  })

  it('does not offer it on a set the server refused, which is not synced', async () => {
    openLogger()
    await screen.findByRole('button', { name: ASK_SET_1 })

    await logSet2()

    // the exercise's header and the row both say where the set stands
    expect(await squat().findAllByText('Not synced')).not.toHaveLength(0)
    expect(askButtons()).toHaveLength(1)
  })

  it('does not offer it on a set that is only saved on this phone', async () => {
    openLogger()
    await screen.findByRole('button', { name: ASK_SET_1 })
    onlineManager.setOnline(false)

    await logSet2()

    expect(await squat().findAllByText('Saved on this phone')).not.toHaveLength(
      0,
    )
    expect(askButtons()).toHaveLength(1)
  })

  it('offers it on the logged sets of a locked session, and nothing else there takes focus', async () => {
    const user = userEvent.setup()
    const onAsk = openLogger(new Date(Date.now() - 24 * HOUR))

    expect(await screen.findByText('Session locked')).toBeInTheDocument()
    expect(squat().getAllByRole('button')).toHaveLength(1)

    await user.tab()
    expect(screen.getByRole('link', { name: 'Today' })).toHaveFocus()
    await user.tab()
    expect(screen.getByRole('button', { name: ASK_SET_1 })).toHaveFocus()
    await user.keyboard('{Enter}')
    expect(onAsk).toHaveBeenCalledOnce()
    await user.tab()
    expect(document.body).toHaveFocus()
  })

  it('offers nothing when the page is given no way to ask', async () => {
    stubApi(() =>
      json({
        id: SESSION,
        date: '2026-11-07',
        name: 'Lower body',
        startedAt: new Date().toISOString(),
        entries: [SQUAT],
      }),
    )
    const router = createMemoryRouter(
      [{ path: '/app/session', Component: LoggerPage }],
      { initialEntries: ['/app/session'] },
    )
    render(
      <QueryClientProvider client={loggerClient()}>
        <RouterProvider router={router} />
      </QueryClientProvider>,
    )

    await screen.findByRole('button', { name: /^Change set 1 of Back squat/ })

    expect(askButtons()).toHaveLength(0)
  })
})
