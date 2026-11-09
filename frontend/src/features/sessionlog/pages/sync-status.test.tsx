import {
  focusManager,
  onlineManager,
  QueryClientProvider,
} from '@tanstack/react-query'
import { act, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import i18n from '@/lib/i18n'
import { WeightUnitContext } from '@/lib/weight'
import { json, loggerClient, problem, stubApi, type Reply } from '@/test/page'
import { LoggerPage } from './logger-page'
import { eventually } from '@/test/eventually'

const SESSION = '00000000-0000-4000-a000-000000000001'

const exercise = (position: number, name: string, logged?: object) => ({
  position,
  exerciseId: `00000000-0000-4000-9100-00000000001${position}`,
  exerciseName: name,
  measure: 'REPS',
  restSeconds: 0,
  sets: [
    {
      number: 1,
      target: { loadKg: 100, amount: 5, rpe: 8 },
      ...(logged && { log: logged }),
    },
    { number: 2, target: { loadKg: 100, amount: 5, rpe: 8 } },
  ],
})

const stored = (amount = 5) => ({
  id: '00000000-0000-4000-b000-000000000001',
  loadKg: 100,
  amount,
  rpe: 8,
  loggedAt: '2026-11-07T10:05:00Z',
})

type Put = { id: string; amount: number }

// the answer to a set log is decided by the test; the session start is always answered
function server(
  answer: (put: Put, request: Request) => Response | Promise<Response>,
  entries = [exercise(0, 'Back squat')],
) {
  const puts: Put[] = []
  const reply: Reply = async (request) => {
    if (request.method === 'POST') {
      return json(
        {
          id: SESSION,
          date: '2026-11-07',
          name: 'Lower body',
          startedAt: new Date().toISOString(),
          entries,
        },
        201,
      )
    }
    const body = (await request.clone().json()) as { amount: number }
    const put = {
      id: new URL(request.url).pathname.split('/').pop()!,
      amount: body.amount,
    }
    puts.push(put)
    return answer(put, request)
  }
  return { reply, puts }
}

const accepted = (put: Put) =>
  json({
    id: put.id,
    loggedAt: '2026-11-07T10:05:00Z',
    entryPosition: 0,
    setNumber: 1,
    loadKg: 100,
    amount: put.amount,
    rpe: 8,
  })

const rejected = () =>
  problem(400, { status: 400, code: 'request.invalid', detail: 'secret' })

let requests: Request[]

function openLogger(reply: Reply) {
  requests = stubApi(reply)
  const router = createMemoryRouter(
    [{ path: '/app/session', Component: LoggerPage }],
    { initialEntries: ['/app/session'] },
  )
  render(
    <QueryClientProvider client={loggerClient()}>
      <WeightUnitContext value="KG">
        <RouterProvider router={router} />
      </WeightUnitContext>
    </QueryClientProvider>,
  )
}

const rowOf = (exerciseName: string, index = 0) =>
  within(screen.getByRole('region', { name: exerciseName })).getAllByRole(
    'listitem',
  )[index]

const tick = (user: ReturnType<typeof userEvent.setup>, name = 'Back squat') =>
  user.click(
    screen.getByRole('button', { name: `Log set 1 of ${name} as planned` }),
  )

const posts = () => requests.filter((request) => request.method === 'POST')

const goOffline = () => act(() => onlineManager.setOnline(false))
const goOnline = () => act(() => onlineManager.setOnline(true))

describe('where a logged set stands', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    localStorage.clear()
  })
  afterEach(async () => {
    onlineManager.setOnline(true)
    vi.useRealTimers()
    await i18n.changeLanguage('en')
  })

  const setup = () => userEvent.setup({ advanceTimers: vi.advanceTimersByTime })

  describe('the offline banner', () => {
    it('shows while the connection is down, keeps rows editable, and goes away online', async () => {
      const user = setup()
      openLogger(server(accepted).reply)
      await screen.findByRole('region', { name: 'Back squat' })
      expect(screen.queryByText("You're offline")).not.toBeInTheDocument()

      await goOffline()

      expect(screen.getByText("You're offline")).toBeVisible()
      expect(
        screen.getByText('Logs save on this phone and sync later.'),
      ).toBeVisible()
      // said once, without moving focus
      expect(
        screen.getByText(
          "You're offline. Logs save on this phone and sync later.",
        ),
      ).toHaveAttribute('role', 'status')
      await user.click(
        screen.getByRole('button', { name: /^Choose set 1 of Back squat/ }),
      )
      expect(screen.getByRole('button', { name: 'Log set' })).toBeEnabled()

      await goOnline()

      expect(screen.queryByText("You're offline")).not.toBeInTheDocument()
    })

    it('says when the browser cannot keep logs on the phone', async () => {
      vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
        throw new DOMException('full', 'QuotaExceededError')
      })
      openLogger(server(accepted).reply)
      await screen.findByRole('region', { name: 'Back squat' })

      await goOffline()

      expect(
        screen.getByText(/can't keep logs on the phone, so keep this tab open/),
      ).toBeVisible()
    })

    it('says it in Polish', async () => {
      await i18n.changeLanguage('pl')
      openLogger(server(accepted).reply)
      await screen.findByRole('region', { name: 'Back squat' })

      await goOffline()

      expect(screen.getByText('Brak połączenia')).toBeVisible()
    })
  })

  describe('the state of a row', () => {
    it('says Saved on this phone while offline and Synced once the server has it, in a status', async () => {
      const user = setup()
      const { reply, puts } = server(accepted)
      openLogger(reply)
      await screen.findByRole('region', { name: 'Back squat' })
      await goOffline()

      await tick(user)

      const status = within(rowOf('Back squat')).getByRole('status')
      expect(status).toHaveTextContent('Saved on this phone')
      expect(puts).toHaveLength(0)
      // a set kept on the phone is a set logged
      expect(screen.getByText('1 of 2 sets logged')).toBeVisible()

      await goOnline()
      await eventually(() => expect(status).toHaveTextContent('Synced'))
      expect(puts).toHaveLength(1)
    })

    it('shows the worst state of its sets on the exercise', async () => {
      const user = setup()
      const { reply } = server(
        () => rejected(),
        [exercise(0, 'Back squat', stored()), exercise(1, 'Bench press')],
      )
      openLogger(reply)
      await screen.findByRole('region', { name: 'Back squat' })
      const header = (name: string) =>
        within(
          screen.getByRole('region', { name: name })
            .firstElementChild as HTMLElement,
        )
      expect(header('Back squat').getByText('Synced')).toBeVisible()
      expect(header('Bench press').queryByText(/sync|phone/i)).toBeNull()

      await user.click(
        screen.getByRole('button', {
          name: 'Log set 2 of Back squat as planned',
        }),
      )

      expect(await header('Back squat').findByText('Not synced')).toBeVisible()
    })

    it('says Not synced with the reason, Retry and Discard when the server refuses, and sends nothing by itself again', async () => {
      const user = setup()
      const { reply, puts } = server(() => rejected())
      openLogger(reply)
      await screen.findByRole('region', { name: 'Back squat' })

      await tick(user)

      const row = rowOf('Back squat')
      expect(await within(row).findByText('Not synced')).toBeVisible()
      expect(
        within(row).getByText(
          'The values were not accepted. Change them and try again.',
        ),
      ).toBeVisible()
      expect(
        within(row).getByRole('button', { name: 'Retry set 1 of Back squat' }),
      ).toBeVisible()
      expect(
        within(row).getByRole('button', {
          name: 'Discard set 1 of Back squat',
        }),
      ).toBeVisible()
      // what the client entered stays, and the server's own words are never shown
      expect(within(row).getByText('5 reps')).toBeVisible()
      expect(screen.queryByText('secret')).not.toBeInTheDocument()
      await act(() => vi.advanceTimersByTimeAsync(60_000))
      expect(puts).toHaveLength(1)
    })

    it('tries a lost connection 3 times, then says it cannot reach the server; Retry starts a new round with the same id', async () => {
      const user = setup()
      let down = true
      const { reply, puts } = server((put) => {
        if (down) throw new TypeError('Failed to fetch')
        return accepted(put)
      })
      openLogger(reply)
      await screen.findByRole('region', { name: 'Back squat' })

      await tick(user)
      await eventually(() => expect(puts).toHaveLength(1))
      // still on its way, kept on the phone
      expect(within(rowOf('Back squat')).getByRole('status')).toHaveTextContent(
        'Saved on this phone',
      )
      await act(() => vi.advanceTimersByTimeAsync(30_000))

      expect(puts).toHaveLength(3)
      const row = rowOf('Back squat')
      expect(within(row).getByText("Can't reach the server.")).toBeVisible()
      expect(within(row).getByText('Not synced')).toBeVisible()

      await user.click(
        within(row).getByRole('button', { name: 'Retry set 1 of Back squat' }),
      )
      await act(() => vi.advanceTimersByTimeAsync(30_000))
      expect(puts).toHaveLength(6)
      expect(within(row).getByText('Not synced')).toBeVisible()

      down = false
      await user.click(
        within(row).getByRole('button', { name: 'Retry set 1 of Back squat' }),
      )

      await eventually(() =>
        expect(within(row).getByRole('status')).toHaveTextContent('Synced'),
      )
      expect(puts).toHaveLength(7)
      expect(new Set(puts.map((put) => put.id)).size).toBe(1)
    })

    it('shows a set the server will not take for this session as refused, and Retry reaches Synced when it takes it', async () => {
      const user = setup()
      let refuse = true
      const { reply } = server((put) =>
        refuse
          ? problem(404, { status: 404, code: 'session-log.not-found' })
          : accepted(put),
      )
      openLogger(reply)
      await screen.findByRole('region', { name: 'Back squat' })

      await tick(user)

      const row = rowOf('Back squat')
      expect(
        await within(row).findByText(
          "This set can't be saved to this session.",
        ),
      ).toBeVisible()

      refuse = false
      // by keyboard: the button is a real one, focusable, named by exercise and set
      const retry = within(row).getByRole('button', {
        name: 'Retry set 1 of Back squat',
      })
      retry.focus()
      await user.keyboard('{Enter}')

      await eventually(() =>
        expect(within(row).getByRole('status')).toHaveTextContent('Synced'),
      )
      // the row's own button takes the focus back when Retry disappears
      expect(
        within(row).getByRole('button', {
          name: /^Change set 1 of Back squat/,
        }),
      ).toHaveFocus()
    })

    it('puts a discarded set back to its targets, sends nothing more, and returns to the stored values for a stored set', async () => {
      const user = setup()
      const { reply, puts } = server(() => rejected())
      openLogger(reply)
      await screen.findByRole('region', { name: 'Back squat' })
      await tick(user)
      const row = rowOf('Back squat')
      await within(row).findByText('Not synced')

      await user.click(
        within(row).getByRole('button', {
          name: 'Discard set 1 of Back squat',
        }),
      )

      expect(within(row).queryByText('Not synced')).not.toBeInTheDocument()
      expect(
        within(row).getByRole('button', {
          name: 'Log set 1 of Back squat as planned',
        }),
      ).toBeVisible()
      await goOffline()
      await goOnline()
      expect(puts).toHaveLength(1)
    })

    it('returns a discarded change to what the server holds', async () => {
      const user = setup()
      const { reply } = server(
        () => rejected(),
        [exercise(0, 'Back squat', stored(5))],
      )
      openLogger(reply)
      await screen.findByRole('region', { name: 'Back squat' })
      await user.click(
        screen.getByRole('button', { name: /^Change set 1 of Back squat/ }),
      )
      await user.click(
        screen.getByRole('button', {
          name: 'More: Repetitions, set 1 of Back squat',
        }),
      )
      await user.click(screen.getByRole('button', { name: 'Update set' }))
      const row = rowOf('Back squat')
      expect(await within(row).findByText('6 reps')).toBeVisible()
      await within(row).findByText('Not synced')

      await user.click(
        within(row).getByRole('button', {
          name: 'Discard set 1 of Back squat',
        }),
      )

      expect(within(row).getByText('5 reps')).toBeVisible()
      expect(within(row).getByRole('status')).toHaveTextContent('Synced')
    })

    it('says the states in Polish', async () => {
      await i18n.changeLanguage('pl')
      const user = setup()
      openLogger(server(() => rejected()).reply)
      await screen.findByRole('region', { name: 'Back squat' })

      await user.click(
        screen.getByRole('button', {
          name: 'Zapisz serię 1: Back squat zgodnie z planem',
        }),
      )

      const row = rowOf('Back squat')
      expect(await within(row).findByText('Nie zsynchronizowano')).toBeVisible()
      expect(
        within(row).getByRole('button', { name: 'Ponów serię 1: Back squat' }),
      ).toBeVisible()
      expect(
        within(row).getByRole('button', { name: 'Odrzuć serię 1: Back squat' }),
      ).toBeVisible()
    })
  })

  describe('Finish session', () => {
    const finish = () => screen.getByRole('button', { name: 'Finish session' })

    it('is unavailable offline, with the reason, and available when back online', async () => {
      openLogger(server(accepted, [exercise(0, 'Back squat', stored())]).reply)
      await screen.findByRole('region', { name: 'Back squat' })
      expect(finish()).toBeEnabled()

      await goOffline()

      expect(finish()).toBeDisabled()
      expect(finish()).toHaveAccessibleDescription(
        "You're offline. Finish the session when you're back online.",
      )

      await goOnline()

      expect(finish()).toBeEnabled()
    })

    it('waits while a set is not synced, and is available when every set is', async () => {
      const user = setup()
      let release!: () => void
      const gate = new Promise<void>((resolve) => (release = resolve))
      openLogger(
        server(
          async (put) => {
            await gate
            return accepted(put)
          },
          [exercise(0, 'Back squat', stored())],
        ).reply,
      )
      await screen.findByRole('region', { name: 'Back squat' })

      await user.click(
        screen.getByRole('button', {
          name: 'Log set 2 of Back squat as planned',
        }),
      )

      expect(finish()).toBeDisabled()
      expect(finish()).toHaveAccessibleDescription(
        'Finish the session when every set is synced.',
      )

      release()

      await eventually(() => expect(finish()).toBeEnabled())
    })
  })

  describe('a second tab', () => {
    const focusAgain = () =>
      act(() => {
        focusManager.setFocused(false)
        focusManager.setFocused(true)
      })

    it('shows what the first stored when it regains focus, while nothing here waits to be sent', async () => {
      openLogger(server(accepted).reply)
      await screen.findByRole('region', { name: 'Back squat' })
      expect(posts()).toHaveLength(1)

      await focusAgain()

      await eventually(() => expect(posts()).toHaveLength(2))
    })

    it('does not ask again while a set here waits to be sent', async () => {
      const user = setup()
      openLogger(server(() => new Promise<Response>(() => {})).reply)
      await screen.findByRole('region', { name: 'Back squat' })
      await tick(user)
      expect(within(rowOf('Back squat')).getByRole('status')).toHaveTextContent(
        'Saved on this phone',
      )

      await focusAgain()
      await act(() => vi.advanceTimersByTimeAsync(1000))

      expect(posts()).toHaveLength(1)
    })
  })
})
