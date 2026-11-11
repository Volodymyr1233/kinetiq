import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { Toaster } from 'sonner'
import { describe, expect, it } from 'vitest'
import '@/lib/i18n'
import { WeightUnitContext } from '@/lib/weight'
import { json, problem, stubApi } from '@/test/page'
import { TodayRoute } from './today-route'

const TODAY = {
  date: '2026-11-09',
  state: 'SESSION',
  session: {
    name: 'Lower body',
    day: 'MONDAY',
    entries: [
      {
        exerciseId: '00000000-0000-4000-9100-000000000011',
        exerciseName: 'Back squat',
        measure: 'REPS',
        sets: 3,
        amount: 5,
        loadKg: 100,
        rpe: 8,
        restSeconds: 120,
      },
    ],
  },
}

const checkIn = (inUse: boolean, open = true) => ({
  date: '2026-11-09',
  open,
  checkIn: {
    sleep: 2,
    soreness: 4,
    stress: 3,
    score: 33,
    band: 'RECOVERY_DAY',
    savedAt: '2026-11-09T07:00:00Z',
    adjustment: {
      inUse,
      causes: ['SLEEP', 'SORENESS'],
      entries: [{ position: 0, loadKg: 90, amount: 5, rpe: 7 }],
    },
  },
})

/** Today on a recovery day; `switchReply` answers the switch, and by default stores the choice sent. */
function openToday(
  options: { open?: boolean; switchReply?: (choice: string) => Response } = {},
) {
  let inUse = true
  const requests = stubApi(async (request) => {
    const path = new URL(request.url).pathname
    if (path === '/api/v1/check-ins/today/targets') {
      const { choice } = (await request.json()) as { choice: string }
      if (options.switchReply) return options.switchReply(choice)
      inUse = choice === 'LIGHTER'
      return json(checkIn(inUse, options.open))
    }
    if (path === '/api/v1/check-ins/today') {
      return json(checkIn(inUse, options.open))
    }
    if (path === '/api/v1/session-states') {
      return json({ today: '2026-11-09', sessions: [] })
    }
    if (path === '/api/v1/plan/changes/current') {
      return problem(404, { status: 404, code: 'plan-change.not-found' })
    }
    return json(TODAY)
  })
  render(
    <QueryClientProvider
      client={
        new QueryClient({
          defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
          },
        })
      }
    >
      <WeightUnitContext value="KG">
        <MemoryRouter>
          <TodayRoute />
          <Toaster />
        </MemoryRouter>
      </WeightUnitContext>
    </QueryClientProvider>,
  )
  return requests
}

const targetsSent = (requests: Request[]) =>
  requests.filter(
    (request) =>
      new URL(request.url).pathname === '/api/v1/check-ins/today/targets',
  )

describe('TodayRoute on a recovery day', () => {
  it('shows the lighter targets with their reason, and the original ones in one tap with Undo', async () => {
    const user = userEvent.setup()
    const requests = openToday()

    expect(await screen.findByText('100 kg, today 90 kg')).toBeInTheDocument()
    expect(screen.getByText('Adjusted for today')).toBeInTheDocument()
    expect(
      screen.getByText(
        'Poor sleep and high soreness, so today’s targets are lighter.',
      ),
    ).toBeInTheDocument()

    await user.click(
      screen.getByRole('button', { name: 'Use original targets' }),
    )

    // no dialog: the planned targets show at once
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(screen.queryByText('100 kg, today 90 kg')).toBeNull()
    expect(screen.getByText('100 kg')).toBeInTheDocument()
    expect(screen.queryByText('Adjusted for today')).toBeNull()
    expect(screen.getByText('Original targets')).toBeInTheDocument()
    const switchBack = screen.getByRole('button', {
      name: 'Use lighter targets',
    })
    expect(switchBack).toHaveFocus()
    expect(
      await screen.findByText('Original targets restored'),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Undo' }))

    expect(await screen.findByText('100 kg, today 90 kg')).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Use original targets' }),
    ).toHaveFocus()
    await waitFor(() => expect(targetsSent(requests)).toHaveLength(2))
    expect(await targetsSent(requests)[0].json()).toEqual({
      choice: 'ORIGINAL',
    })
    expect(await targetsSent(requests)[1].json()).toEqual({
      choice: 'LIGHTER',
    })
  })

  it('switches back to the lighter targets in one tap, closing the Undo message', async () => {
    const user = userEvent.setup()
    openToday()

    await user.click(
      await screen.findByRole('button', { name: 'Use original targets' }),
    )
    await screen.findByText('Original targets restored')
    await user.click(
      screen.getByRole('button', { name: 'Use lighter targets' }),
    )

    expect(await screen.findByText('100 kg, today 90 kg')).toBeInTheDocument()
    expect(screen.getByText('Lighter targets in use.')).toBeInTheDocument()
    // well before the message would close by itself
    await waitFor(
      () => expect(screen.queryByText('Original targets restored')).toBeNull(),
      { timeout: 2000 },
    )
  })

  it('keeps one Undo message when the targets are switched again and again', async () => {
    const user = userEvent.setup()
    openToday()

    for (let round = 0; round < 3; round++) {
      await user.click(
        await screen.findByRole('button', { name: 'Use original targets' }),
      )
      await user.click(
        await screen.findByRole('button', { name: 'Use lighter targets' }),
      )
    }
    await user.click(
      await screen.findByRole('button', { name: 'Use original targets' }),
    )

    // well before the earlier messages would close by themselves
    await waitFor(
      () =>
        expect(screen.getAllByText('Original targets restored')).toHaveLength(
          1,
        ),
      { timeout: 2000 },
    )
  })

  it('returns to the stored targets with a message when the switch fails', async () => {
    const user = userEvent.setup()
    openToday({
      switchReply: () =>
        problem(409, { status: 409, code: 'check-in.no-adjustment' }),
    })

    await user.click(
      await screen.findByRole('button', { name: 'Use original targets' }),
    )

    expect(await screen.findByText('100 kg, today 90 kg')).toBeInTheDocument()
    expect(
      await screen.findByText(
        'Today’s targets are as planned, so there is nothing to switch.',
      ),
    ).toBeInTheDocument()
  })

  it('offers no switch once training has started', async () => {
    openToday({ open: false })

    expect(await screen.findByText('100 kg, today 90 kg')).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Use original targets' }),
    ).toBeNull()
    expect(
      screen.getByText(
        'Answers and targets are fixed once training has started.',
      ),
    ).toBeInTheDocument()
  })
})
