import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it } from 'vitest'
import i18n from '@/lib/i18n'
import { json, problem, renderAppAt, SIGNED_IN } from '@/test/page'
import type { PlanChange } from '../api/plan-change'

const ID = '00000000-0000-4000-9600-000000000001'
const CLIENT_ID = '00000000-0000-4000-8000-000000000003'
const SQUAT = '00000000-0000-4000-9100-000000000011'

const PENDING: PlanChange = {
  id: ID,
  clientId: CLIENT_ID,
  clientName: 'Ola Kowalska',
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

const ENDED_AT = '2026-11-12T08:00:00Z'

const call = (request: Request) =>
  `${request.method} ${new URL(request.url).pathname.replace(ID, ':id')}`

/**
 * The trainer's screen with a server that holds the plan change; `decisions` answers a decision by its call and may
 * change what the server holds.
 */
function open(
  initial: PlanChange,
  decisions: Record<
    string,
    (hold: (change: PlanChange) => Response) => Response
  > = {},
  language = 'en',
) {
  let current = initial
  const hold = (change: PlanChange) => {
    current = change
    return json(change)
  }
  const requests: Request[] = []
  renderAppAt(
    `/trainer/plan-changes/${ID}`,
    { ...SIGNED_IN.TRAINER, language: language.toUpperCase() },
    (request) => {
      requests.push(request)
      const name = call(request)
      if (name in decisions) return decisions[name](hold)
      if (name === 'GET /api/v1/plan-changes/:id') return json(current)
      if (name.startsWith('GET /api/v1/approvals/')) return json({ items: [] })
      return problem(404, { status: 404, code: 'plan-change.not-found' })
    },
  )
  return requests
}

const decisionsSent = (requests: Request[]) =>
  requests.map(call).filter((name) => !name.startsWith('GET'))

describe('PlanChangePage decisions', () => {
  afterEach(async () => {
    await i18n.changeLanguage('en')
  })

  it('accepts, shows the wait with Undo, and undoes', async () => {
    const user = userEvent.setup()
    const requests = open(PENDING, {
      'POST /api/v1/plan-changes/:id/acceptance': (hold) =>
        hold({ ...PENDING, state: 'ACCEPTED_BY_ONE', acceptedByTrainer: true }),
      'DELETE /api/v1/plan-changes/:id/acceptance': (hold) => hold(PENDING),
    })

    await user.click(
      await screen.findByRole('button', { name: 'Accept change' }),
    )

    expect(
      await screen.findByText('You accepted. Waiting for Ola Kowalska.'),
    ).toBeInTheDocument()
    expect(screen.getByText('Waiting for Ola')).toBeInTheDocument()
    expect(screen.getByText('You · accepted')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Undo' }))

    expect(
      await screen.findByRole('button', { name: 'Accept change' }),
    ).toBeInTheDocument()
    expect(decisionsSent(requests)).toEqual([
      'POST /api/v1/plan-changes/:id/acceptance',
      'DELETE /api/v1/plan-changes/:id/acceptance',
    ])
  })

  it('says the client accepted first, and shows Applied once the trainer accepts too', async () => {
    const user = userEvent.setup()
    open(
      { ...PENDING, state: 'ACCEPTED_BY_ONE', acceptedByClient: true },
      {
        'POST /api/v1/plan-changes/:id/acceptance': (hold) =>
          hold({
            ...PENDING,
            state: 'APPLIED',
            acceptedByTrainer: true,
            acceptedByClient: true,
            endedAt: ENDED_AT,
          }),
      },
    )

    expect(
      await screen.findByText('Ola Kowalska accepted. Your decision is next.'),
    ).toBeInTheDocument()
    expect(screen.getByText('Waiting for you')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Accept change' }))

    expect(
      await screen.findByText('You both accepted. Takes effect Mon, Nov 16.'),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Undo' }),
    ).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'See plan' })).toHaveAttribute(
      'href',
      `/trainer/clients/${CLIENT_ID}/plan`,
    )
  })

  it('keeps the current plan through a dialog that needs a reason, and sends reason and note', async () => {
    const user = userEvent.setup()
    const requests = open(PENDING, {
      'POST /api/v1/plan-changes/:id/declination': (hold) =>
        hold({
          ...PENDING,
          state: 'DECLINED',
          declinedBy: 'TRAINER',
          declineReason: 'TOO_EASY',
          declineNote: 'Raise it instead',
          endedAt: ENDED_AT,
        }),
    })

    await user.click(
      await screen.findByRole('button', { name: 'Keep current plan' }),
    )
    const dialog = await screen.findByRole('dialog', {
      name: 'Keep the current plan?',
    })
    expect(
      within(dialog).getByText(
        "The change won't happen. Ola Kowalska gets your reason.",
      ),
    ).toBeInTheDocument()
    const submit = within(dialog).getByRole('button', {
      name: 'Keep current plan',
    })
    expect(submit).toBeDisabled()
    await user.click(within(dialog).getByRole('radio', { name: 'Too easy' }))
    await user.type(
      within(dialog).getByRole('textbox', { name: 'Note (optional)' }),
      'Raise it instead',
    )
    await user.click(submit)

    expect(
      await screen.findByText(
        'You kept the current plan: Too easy. Ola Kowalska sees your reason in the chat.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByText('Raise it instead')).toBeInTheDocument()
    const post = requests.find((request) => request.method === 'POST')!
    expect(await post.clone().json()).toEqual({
      reason: 'TOO_EASY',
      note: 'Raise it instead',
    })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it.each([
    [
      'declined by the client',
      {
        state: 'DECLINED',
        declinedBy: 'CLIENT',
        declineReason: 'SCHEDULE',
      } as const,
      'Ola Kowalska kept the current plan: Schedule.',
    ],
    [
      'applied',
      {
        state: 'APPLIED',
        acceptedByTrainer: true,
        acceptedByClient: true,
      } as const,
      'You both accepted. Takes effect Mon, Nov 16.',
    ],
    [
      'expired',
      { state: 'EXPIRED' } as const,
      "The week started before you both decided. The plan didn't change.",
    ],
  ])(
    'shows the outcome and no actions once it is %s',
    async (_name, patch, text) => {
      open({ ...PENDING, ...patch })

      expect(await screen.findByText(text)).toBeInTheDocument()
      for (const name of ['Accept change', 'Keep current plan', 'Undo']) {
        expect(screen.queryByRole('button', { name })).not.toBeInTheDocument()
      }
    },
  )

  it('shows the clients note as plain text, markup and all', async () => {
    open({
      ...PENDING,
      state: 'DECLINED',
      declinedBy: 'CLIENT',
      declineReason: 'OTHER',
      declineNote: '<b>x</b>',
    })

    expect(await screen.findByText('<b>x</b>')).toBeInTheDocument()
  })

  it('shows the translated refusal, then the state the other person left it in', async () => {
    const user = userEvent.setup()
    let reads = 0
    renderAppAt(`/trainer/plan-changes/${ID}`, SIGNED_IN.TRAINER, (request) => {
      const name = call(request)
      if (name === 'POST /api/v1/plan-changes/:id/acceptance') {
        return problem(409, { status: 409, code: 'plan-change.ended' })
      }
      if (name === 'GET /api/v1/plan-changes/:id') {
        return json(
          reads++ === 0
            ? PENDING
            : {
                ...PENDING,
                state: 'DECLINED',
                declinedBy: 'CLIENT',
                declineReason: 'TOO_HARD',
              },
        )
      }
      return name.startsWith('GET /api/v1/approvals/')
        ? json({ items: [] })
        : problem(404, { status: 404, code: 'plan-change.not-found' })
    })

    await user.click(
      await screen.findByRole('button', { name: 'Accept change' }),
    )

    expect(
      await screen.findByText(
        'This plan change has ended, so nobody can decide on it any more.',
      ),
    ).toBeInTheDocument()
    expect(
      await screen.findByText('Ola Kowalska kept the current plan: Too hard.'),
    ).toBeInTheDocument()
  })

  it('links back to Approvals and to the client in a breadcrumb', async () => {
    open(PENDING)

    const crumbs = await screen.findByRole('navigation', {
      name: 'Breadcrumb',
    })
    expect(
      within(crumbs).getByRole('link', { name: 'Approvals' }),
    ).toHaveAttribute('href', '/trainer/approvals')
    expect(
      within(crumbs).getByRole('link', { name: 'Ola Kowalska' }),
    ).toHaveAttribute('href', `/trainer/clients/${CLIENT_ID}`)
  })

  it('reads in Polish', async () => {
    await i18n.changeLanguage('pl')
    open(PENDING, {}, 'pl')

    expect(await screen.findByText('Czeka na obie strony')).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Zaakceptuj zmianę' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Zostań przy obecnym planie' }),
    ).toBeInTheDocument()
  })
})
