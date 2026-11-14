import { act, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  json,
  pathsOf,
  problem,
  quizStatus,
  renderAppAt,
  SIGNED_IN,
  type Reply,
} from '@/test/page'
import { FakeEventSource } from '@/test/event-source'

const DRAFT = {
  answers: {
    goal: 'BUILD_MUSCLE',
    experience: 'YEARS_1_TO_2',
    level: 'INTERMEDIATE',
    age: '29',
    height: '168',
    weight: '62,5',
    likes: [],
    days: 3,
    notes: '',
  },
  weightUnit: 'KG',
  reached: 4,
}

const COMPLETE_DRAFT = {
  ...DRAFT,
  answers: { ...DRAFT.answers, likes: ['WEIGHTS'] },
  reached: 7,
}

// the server knows the client's quiz status, ends a sign-in, and keeps the settings it is sent
function server(status: string) {
  let account: object = SIGNED_IN.CLIENT
  return async (request: Request) => {
    const call = `${request.method} ${new URL(request.url).pathname}`
    if (call === 'GET /api/v1/quiz') return json({ status })
    if (call === 'PATCH /api/v1/accounts/me') {
      account = { ...account, ...(await request.json()) }
      return json(account)
    }
    return new Response(null, { status: 204 })
  }
}

async function openSettings() {
  const user = userEvent.setup()
  await user.click(await screen.findByRole('button', { name: 'Settings' }))
  const sheet = within(await screen.findByRole('dialog', { name: 'Settings' }))
  return { sheet, user }
}

describe('quiz area', () => {
  it.each([
    ['a quiz step', '/app/quiz/4', 'NOT_SUBMITTED'],
    ['the waiting state', '/app/quiz/waiting', 'WAITING_FOR_PLAN'],
  ])(
    'opens the settings sheet on %s and signs out, clearing the answers in the tab',
    async (_, path, status) => {
      sessionStorage.setItem('kinetiq.quiz', JSON.stringify(DRAFT))
      const { requests, router } = renderAppAt(
        path,
        SIGNED_IN.CLIENT,
        server(status),
      )

      const { sheet, user } = await openSettings()
      expect(sheet.getByText('Anna Kowalska')).toBeInTheDocument()
      await user.click(sheet.getByRole('button', { name: 'Sign out' }))

      expect(
        await screen.findByRole('heading', { name: 'Sign in' }),
      ).toBeInTheDocument()
      expect(router.state.location.pathname).toBe('/sign-in')
      expect(sessionStorage.length).toBe(0)
      expect(
        requests.some(
          (request) =>
            request.method === 'DELETE' &&
            new URL(request.url).pathname === '/api/v1/sign-ins/current',
        ),
      ).toBe(true)
    },
  )

  it('shows a typed body weight in the unit chosen in the sheet', async () => {
    sessionStorage.setItem('kinetiq.quiz', JSON.stringify(DRAFT))
    renderAppAt('/app/quiz/4', SIGNED_IN.CLIENT, server('NOT_SUBMITTED'))
    expect(
      await screen.findByRole('textbox', { name: 'Body weight' }),
    ).toHaveValue('62,5')

    const { sheet, user } = await openSettings()
    await user.click(
      within(sheet.getByRole('radiogroup', { name: 'Units' })).getByRole(
        'radio',
        { name: 'lb' },
      ),
    )
    await user.keyboard('{Escape}')

    expect(
      await screen.findByRole('textbox', { name: 'Body weight' }),
    ).toHaveValue('137,8')
    expect(JSON.parse(sessionStorage.getItem('kinetiq.quiz')!)).toMatchObject({
      answers: { weight: '137,8' },
      weightUnit: 'LB',
    })
  })

  // the page behind the open sheet is hidden from the accessibility tree, so its weight is found with `hidden` or as text
  it.each<[string, string, () => void]>([
    [
      'a quiz step',
      '/app/quiz/4',
      () =>
        expect(
          screen.getByRole('textbox', { name: 'Body weight', hidden: true }),
        ).toHaveValue('137,8'),
    ],
    [
      'the review',
      '/app/quiz/review',
      () =>
        expect(
          screen.getByText('29 years · 168 cm · 137.8 lb'),
        ).toBeInTheDocument(),
    ],
  ])(
    'keeps the settings sheet open on %s after a unit change, and closing it focuses Settings',
    async (_, path, showsTheWeightInLb) => {
      sessionStorage.setItem('kinetiq.quiz', JSON.stringify(COMPLETE_DRAFT))
      renderAppAt(path, SIGNED_IN.CLIENT, server('NOT_SUBMITTED'))
      const { sheet, user } = await openSettings()
      const lb = within(
        sheet.getByRole('radiogroup', { name: 'Units' }),
      ).getByRole('radio', { name: 'lb' })

      await user.click(lb)
      await waitFor(showsTheWeightInLb)

      expect(
        screen.getByRole('dialog', { name: 'Settings' }),
      ).toBeInTheDocument()
      expect(lb).toHaveFocus()

      await user.keyboard('{Escape}')

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Settings' })).toHaveFocus()
    },
  )
})

const MESSAGES = `/api/v1/conversations/${SIGNED_IN.CLIENT.id}/messages`

const fromTrainer = (id: string, text: string) => ({
  id,
  text,
  mine: false,
  sentAt: new Date().toISOString(),
})

// the waiting client's conversation with their trainer holds `messages`, and a sent message joins them
function waitingWith(messages: object[]) {
  return quizStatus('WAITING_FOR_PLAN', async (request) => {
    const call = `${request.method} ${new URL(request.url).pathname}`
    if (call === 'GET /api/v1/conversations') {
      return json({
        conversations: [
          {
            clientId: SIGNED_IN.CLIENT.id,
            name: 'Marek Nowak',
            unreadCount: 0,
          },
        ],
      })
    }
    if (call === `GET ${MESSAGES}`) return json({ messages, hasMore: false })
    if (call === `POST ${MESSAGES}`) {
      const { id, text } = await request.json()
      const sent = { id, text, mine: true, sentAt: new Date().toISOString() }
      messages.push(sent)
      return json(sent, 201)
    }
    return new Response(null, { status: 204 })
  })
}

const NAMED_TEXT =
  "Marek checks it before you see it. You get a notice here when it's ready."
const UNNAMED_TEXT =
  "Your trainer checks it before you see it. You get a notice here when it's ready."

function waitingWhereTheList(list: Reply) {
  const others = waitingWith([])
  return (request: Request) =>
    new URL(request.url).pathname === '/api/v1/conversations'
      ? list(request)
      : others(request)
}

describe('the trainer on the waiting state', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('takes the name from the conversation list and leaves the message button as it was', async () => {
    renderAppAt('/app/quiz/waiting', SIGNED_IN.CLIENT, waitingWith([]))

    expect(await screen.findByText(NAMED_TEXT)).toBeInTheDocument()
    expect(screen.getByText('MN')).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Message your trainer' }),
    ).toBeInTheDocument()
  })

  it.each<[string, Reply]>([
    ['fails', () => problem(403, { status: 403, code: 'request.not-allowed' })],
    ['holds no conversation', () => json({ conversations: [] })],
  ])('says "your trainer" when the list %s', async (_, list) => {
    const { requests } = renderAppAt(
      '/app/quiz/waiting',
      SIGNED_IN.CLIENT,
      waitingWhereTheList(list),
    )
    await screen.findByText(UNNAMED_TEXT)

    await waitFor(() =>
      expect(pathsOf(requests)).toContain('/api/v1/conversations'),
    )

    expect(await screen.findByText(UNNAMED_TEXT)).toBeInTheDocument()
    expect(screen.queryByText('MN')).not.toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Message your trainer' }),
    ).toBeInTheDocument()
  })

  it('shows the name a later read brings, with focus where it was', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    let failures = 1
    renderAppAt(
      '/app/quiz/waiting',
      SIGNED_IN.CLIENT,
      waitingWhereTheList(() =>
        failures-- > 0
          ? problem(500, { status: 500, code: 'internal.error' })
          : json({
              conversations: [
                {
                  clientId: SIGNED_IN.CLIENT.id,
                  name: 'Marek Nowak',
                  unreadCount: 0,
                },
              ],
            }),
      ),
    )
    const heading = await screen.findByRole('heading', {
      name: 'Your trainer is reviewing your plan',
    })
    expect(screen.getByText(UNNAMED_TEXT)).toBeInTheDocument()

    await act(() => vi.advanceTimersByTimeAsync(2_000))

    expect(await screen.findByText(NAMED_TEXT)).toBeInTheDocument()
    expect(heading).toHaveFocus()
  })
})

function waitingWhereTheSummary(summary: Reply) {
  const others = waitingWith([])
  return (request: Request) =>
    new URL(request.url).pathname === '/api/v1/plan/draft-summary'
      ? summary(request)
      : others(request)
}

describe('the draft card on the waiting state', () => {
  it('stands between the text and the message button', async () => {
    renderAppAt(
      '/app/quiz/waiting',
      SIGNED_IN.CLIENT,
      waitingWhereTheSummary(() =>
        json({ trainingDays: ['TUESDAY'], weeks: 12, goal: 'LOSE_FAT' }),
      ),
    )

    const card = await screen.findByText('Tue')
    const text = screen.getByText(NAMED_TEXT)
    const button = screen.getByRole('button', { name: 'Message your trainer' })

    expect(text.compareDocumentPosition(card)).toBe(
      Node.DOCUMENT_POSITION_FOLLOWING,
    )
    expect(card.compareDocumentPosition(button)).toBe(
      Node.DOCUMENT_POSITION_FOLLOWING,
    )
  })

  it('leaves the waiting state as it is when there is no summary', async () => {
    const { requests } = renderAppAt(
      '/app/quiz/waiting',
      SIGNED_IN.CLIENT,
      waitingWhereTheSummary(() =>
        problem(404, { status: 404, code: 'plan.not-found' }),
      ),
    )

    expect(await screen.findByText(NAMED_TEXT)).toBeInTheDocument()
    await waitFor(() =>
      expect(pathsOf(requests)).toContain('/api/v1/plan/draft-summary'),
    )

    expect(screen.queryByText('Training days')).not.toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Message your trainer' }),
    ).toBeInTheDocument()
  })
})

async function openConversation(user: ReturnType<typeof userEvent.setup>) {
  await user.click(
    await screen.findByRole('button', { name: 'Message your trainer' }),
  )
  return within(await screen.findByRole('dialog', { name: 'Messages' }))
}

describe('message your trainer', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('opens the conversation with the trainer in a focused sheet, and closing it returns to the waiting state', async () => {
    const user = userEvent.setup()
    renderAppAt(
      '/app/quiz/waiting',
      SIGNED_IN.CLIENT,
      waitingWith([fromTrainer('a', 'Any injuries I should know about?')]),
    )

    const sheet = await openConversation(user)

    expect(screen.getByRole('dialog', { name: 'Messages' })).toHaveFocus()
    expect(
      await sheet.findByRole('heading', { name: 'Marek Nowak' }),
    ).toBeInTheDocument()
    expect(
      await sheet.findByText('Any injuries I should know about?'),
    ).toBeInTheDocument()

    await user.click(sheet.getByRole('button', { name: 'Close' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(
      screen.getByRole('heading', {
        name: 'Your trainer is reviewing your plan',
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Message your trainer' }),
    ).toHaveFocus()
  })

  it('shows a message the trainer sends while the sheet is open', async () => {
    const user = userEvent.setup()
    const messages = [fromTrainer('a', 'Any injuries I should know about?')]
    renderAppAt('/app/quiz/waiting', SIGNED_IN.CLIENT, waitingWith(messages))
    const sheet = await openConversation(user)
    await sheet.findByText('Any injuries I should know about?')

    messages.push(fromTrainer('b', 'Your plan is almost ready.'))
    act(() => FakeEventSource.open()[0]!.emit('message', SIGNED_IN.CLIENT.id))

    expect(
      await sheet.findByText('Your plan is almost ready.'),
    ).toBeInTheDocument()
  })

  it('invites the first message and sends it from the sheet', async () => {
    const user = userEvent.setup()
    const { requests } = renderAppAt(
      '/app/quiz/waiting',
      SIGNED_IN.CLIENT,
      waitingWith([]),
    )
    const sheet = await openConversation(user)

    expect(await sheet.findByText('Say hello to Marek.')).toBeInTheDocument()

    await user.type(
      await sheet.findByRole('textbox', { name: 'Message Marek' }),
      'Can I train on Sundays?{Enter}',
    )

    expect(await sheet.findByText(/^Sent · \d\d:\d\d$/)).toBeInTheDocument()
    expect(sheet.getByText('Can I train on Sundays?')).toBeInTheDocument()
    expect(
      requests.filter(
        (request) =>
          request.method === 'POST' &&
          new URL(request.url).pathname === MESSAGES,
      ),
    ).toHaveLength(1)
  })
})
