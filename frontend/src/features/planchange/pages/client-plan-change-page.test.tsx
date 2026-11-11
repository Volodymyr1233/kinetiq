import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, describe, expect, it } from 'vitest'
import i18n from '@/lib/i18n'
import { json, problem, stubApi, type Reply } from '@/test/page'
import type { ClientPlanChange } from '../api/client-plan-change'
import { ClientPlanChangePage } from './client-plan-change-page'

const ID = '00000000-0000-4000-9600-000000000001'
const SQUAT = '00000000-0000-4000-9100-000000000011'

const PENDING: ClientPlanChange = {
  id: ID,
  counterpartName: 'Marek',
  rule: 'HIGH_RPE',
  state: 'PENDING',
  title: 'Back squat: cut volume by 13%',
  reason: 'Average RPE was 8.4 and 8.6 in the last 2 weeks, above 8.',
  effectiveWeek: '2026-11-16',
  windowStart: '2026-10-26',
  windowEnd: '2026-11-08',
  targets: [
    {
      sessionDay: 'TUESDAY',
      sessionName: 'Full body A',
      position: 0,
      exerciseId: SQUAT,
      exerciseName: 'Back squat',
      measure: 'REPS',
      target: 'AMOUNT',
      oldValue: 8,
      newValue: 7,
    },
  ],
  moves: [],
  concernedExercises: [{ id: SQUAT, name: 'Back squat' }],
  figures: [
    { kind: 'MEAN_RPE', weekStart: '2026-10-26', value: 8.4 },
    { kind: 'MEAN_RPE', weekStart: '2026-11-02', value: 8.6 },
  ],
  acceptedByTrainer: false,
  acceptedByClient: false,
}

const ENDED = '2026-11-12T08:00:00Z'

function renderPage(reply: Reply, id = ID) {
  const requests = stubApi(reply)
  const router = createMemoryRouter(
    [
      { path: '/app/plan/change/:id', Component: ClientPlanChangePage },
      { path: '/app/plan', element: <p>The plan</p> },
    ],
    { initialEntries: [`/app/plan/change/${id}`] },
  )
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
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
  return { requests, router }
}

/** Answers the read with `current`, and each decision with what `decisions` gives for its call. */
function server(
  current: () => ClientPlanChange,
  decisions: Record<string, () => Response> = {},
): Reply {
  return (request) => {
    const url = new URL(request.url)
    const call = `${request.method} ${url.pathname.replace(ID, ':id')}`
    if (call in decisions) return decisions[call]()
    if (call === 'GET /api/v1/plan/changes/:id') return json(current())
    return problem(404, { status: 404, code: 'plan-change.not-found' })
  }
}

const bodyOf = async (request: Request) =>
  (await request.clone().json()) as Record<string, unknown>

describe('ClientPlanChangePage', () => {
  afterEach(async () => {
    await i18n.changeLanguage('en')
  })

  it('shows what changes, why and the two people, with both actions while it waits for both', async () => {
    renderPage(server(() => PENDING))

    expect(
      await screen.findByRole('heading', { name: PENDING.title, level: 2 }),
    ).toBeInTheDocument()
    expect(screen.getByText(PENDING.reason)).toBeInTheDocument()
    expect(screen.getByText('Waiting for both')).toBeInTheDocument()
    const change = screen.getByText('Back squat').closest('li')!
    expect(change).toHaveTextContent('Full body A, Tuesday')
    expect(change).toHaveTextContent('8 reps → 7 reps')
    expect(change).toHaveTextContent('-13%')
    const people = screen.getByRole('list', { name: 'Who decided' })
    expect(within(people).getByText('You · not decided')).toBeInTheDocument()
    expect(within(people).getByText('Marek · not decided')).toBeInTheDocument()
    expect(
      screen.getByText('Takes effect Mon, Nov 16 if you both accept.'),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Keep current plan' }),
    ).toBeEnabled()
  })

  it('does not style Accept as the forward action in lime', async () => {
    renderPage(server(() => PENDING))

    const accept = await screen.findByRole('button', { name: 'Accept change' })

    expect(accept).toHaveAttribute('data-variant', 'default')
  })

  it('moves to waiting with Undo after accepting, and back after undoing', async () => {
    const user = userEvent.setup()
    let state: ClientPlanChange = PENDING
    const accepted = {
      ...PENDING,
      state: 'ACCEPTED_BY_ONE',
      acceptedByClient: true,
    } as const
    const requests = renderPage(
      server(() => state, {
        'POST /api/v1/plan/changes/:id/acceptance': () => {
          state = accepted
          return json(state)
        },
        'DELETE /api/v1/plan/changes/:id/acceptance': () => {
          state = PENDING
          return json(state)
        },
      }),
    ).requests

    await user.click(
      await screen.findByRole('button', { name: 'Accept change' }),
    )

    expect(
      await screen.findByText('You accepted. Waiting for Marek.'),
    ).toBeInTheDocument()
    expect(screen.getByText('Waiting for Marek')).toBeInTheDocument()
    expect(screen.getByText('You · accepted')).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Accept change' }),
    ).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Undo' }))

    expect(
      await screen.findByRole('button', { name: 'Accept change' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Waiting for both')).toBeInTheDocument()
    expect(
      requests
        .map((request) => `${request.method} ${new URL(request.url).pathname}`)
        .filter((call) => !call.startsWith('GET')),
    ).toEqual([
      `POST /api/v1/plan/changes/${ID}/acceptance`,
      `DELETE /api/v1/plan/changes/${ID}/acceptance`,
    ])
  })

  it('says it is the clients turn when the trainer accepted first', async () => {
    renderPage(
      server(() => ({
        ...PENDING,
        state: 'ACCEPTED_BY_ONE',
        acceptedByTrainer: true,
      })),
    )

    expect(
      await screen.findByText('Marek accepted. Your decision is next.'),
    ).toBeInTheDocument()
    expect(screen.getByText('Waiting for you')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Accept change' })).toBeEnabled()
  })

  it('opens the keep sheet with no reason chosen, enables the button once one is, and sends reason and note', async () => {
    const user = userEvent.setup()
    let state: ClientPlanChange = PENDING
    const requests = renderPage(
      server(() => state, {
        'POST /api/v1/plan/changes/:id/declination': () => {
          state = {
            ...PENDING,
            state: 'DECLINED',
            declinedBy: 'CLIENT',
            declineReason: 'SCHEDULE',
            declineNote: 'Away that week',
            endedAt: ENDED,
          }
          return json(state)
        },
      }),
    ).requests

    await user.click(
      await screen.findByRole('button', { name: 'Keep current plan' }),
    )
    const sheet = await screen.findByRole('dialog', {
      name: 'Keep the current plan?',
    })
    const submit = within(sheet).getByRole('button', {
      name: 'Keep current plan',
    })
    expect(
      within(sheet)
        .getAllByRole('radio')
        .every((radio) => radio.getAttribute('aria-checked') === 'false'),
    ).toBe(true)
    expect(submit).toBeDisabled()

    await user.click(within(sheet).getByRole('radio', { name: 'Schedule' }))
    await user.type(
      within(sheet).getByRole('textbox', { name: 'Note (optional)' }),
      'Away that week',
    )
    expect(submit).toBeEnabled()
    await user.click(submit)

    expect(
      await screen.findByText(
        'You kept the current plan: Schedule. Marek sees your reason in your chat.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByText('Away that week')).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    const post = requests.find((request) => request.method === 'POST')!
    expect(new URL(post.url).pathname).toBe(
      `/api/v1/plan/changes/${ID}/declination`,
    )
    expect(await bodyOf(post)).toEqual({
      reason: 'SCHEDULE',
      note: 'Away that week',
    })
  })

  it('sends no note when the note is left empty', async () => {
    const user = userEvent.setup()
    const requests = renderPage(
      server(() => PENDING, {
        'POST /api/v1/plan/changes/:id/declination': () =>
          json({
            ...PENDING,
            state: 'DECLINED',
            declinedBy: 'CLIENT',
            declineReason: 'INJURY',
            endedAt: ENDED,
          }),
      }),
    ).requests

    await user.click(
      await screen.findByRole('button', { name: 'Keep current plan' }),
    )
    const sheet = await screen.findByRole('dialog')
    await user.click(within(sheet).getByRole('radio', { name: 'Injury' }))
    await user.click(
      within(sheet).getByRole('button', { name: 'Keep current plan' }),
    )

    await screen.findByText(/You kept the current plan: Injury/)
    const post = requests.find((request) => request.method === 'POST')!
    expect(await bodyOf(post)).toEqual({ reason: 'INJURY' })
  })

  it('closes the sheet without sending when Back is chosen', async () => {
    const user = userEvent.setup()
    const requests = renderPage(server(() => PENDING)).requests

    await user.click(
      await screen.findByRole('button', { name: 'Keep current plan' }),
    )
    await user.click(
      within(await screen.findByRole('dialog')).getByRole('button', {
        name: 'Back',
      }),
    )

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(requests.every((request) => request.method === 'GET')).toBe(true)
  })

  it.each([
    [
      'applied',
      {
        state: 'APPLIED',
        acceptedByTrainer: true,
        acceptedByClient: true,
        endedAt: ENDED,
      } as const,
      'You both accepted. Takes effect Mon, Nov 16.',
    ],
    [
      'declined by the trainer',
      {
        state: 'DECLINED',
        declinedBy: 'TRAINER',
        declineReason: 'TOO_HARD',
        endedAt: ENDED,
      } as const,
      'Marek kept the current plan: Too hard.',
    ],
    [
      'expired',
      { state: 'EXPIRED' } as const,
      "The week started before you both decided. The plan didn't change.",
    ],
  ])(
    'shows the outcome and no decision once it is %s',
    async (_name, change, text) => {
      renderPage(server(() => ({ ...PENDING, ...change })))

      expect(await screen.findByText(text)).toBeInTheDocument()
      expect(
        screen.queryByRole('button', { name: 'Accept change' }),
      ).not.toBeInTheDocument()
      expect(
        screen.queryByRole('button', { name: 'Keep current plan' }),
      ).not.toBeInTheDocument()
      expect(
        screen.queryByRole('button', { name: 'Undo' }),
      ).not.toBeInTheDocument()
    },
  )

  it('links the applied outcome to the plan', async () => {
    renderPage(
      server(() => ({
        ...PENDING,
        state: 'APPLIED',
        acceptedByTrainer: true,
        acceptedByClient: true,
      })),
    )

    expect(
      await screen.findByRole('link', { name: 'See plan' }),
    ).toHaveAttribute('href', '/app/plan')
  })

  it('shows a trainers note as plain text, markup and all', async () => {
    renderPage(
      server(() => ({
        ...PENDING,
        state: 'DECLINED',
        declinedBy: 'TRAINER',
        declineReason: 'OTHER',
        declineNote: '<b>x</b>',
      })),
    )

    expect(await screen.findByText('<b>x</b>')).toBeInTheDocument()
  })

  it('shows the translated refusal and keeps the last state when the week has begun', async () => {
    const user = userEvent.setup()
    renderPage(
      server(() => PENDING, {
        'POST /api/v1/plan/changes/:id/acceptance': () =>
          problem(409, { status: 409, code: 'plan-change.week-begun' }),
      }),
    )

    await user.click(
      await screen.findByRole('button', { name: 'Accept change' }),
    )

    expect(
      await screen.findByText(
        'The week of this plan change has already started, so it can no longer be decided.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Accept change' })).toBeEnabled()
  })

  it('shows the refusal inside the sheet when keeping the plan is refused', async () => {
    const user = userEvent.setup()
    renderPage(
      server(() => PENDING, {
        'POST /api/v1/plan/changes/:id/declination': () =>
          problem(409, { status: 409, code: 'plan-change.ended' }),
      }),
    )

    await user.click(
      await screen.findByRole('button', { name: 'Keep current plan' }),
    )
    const sheet = await screen.findByRole('dialog')
    await user.click(within(sheet).getByRole('radio', { name: 'Other' }))
    await user.click(
      within(sheet).getByRole('button', { name: 'Keep current plan' }),
    )

    expect(
      await within(sheet).findByText(
        'This plan change has ended, so nobody can decide on it any more.',
      ),
    ).toBeInTheDocument()
  })

  it('shows not found for another clients plan change, with a way back', async () => {
    renderPage(() =>
      problem(404, { status: 404, code: 'plan-change.not-found' }),
    )

    expect(await screen.findByText('Plan change not found')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to plan' })).toHaveAttribute(
      'href',
      '/app/plan',
    )
  })

  it('reads in Polish for a Polish client', async () => {
    await i18n.changeLanguage('pl')
    renderPage(server(() => PENDING))

    expect(
      await screen.findByRole('button', { name: 'Zaakceptuj zmianę' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Zostań przy obecnym planie' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Czeka na obie strony')).toBeInTheDocument()
  })
})
