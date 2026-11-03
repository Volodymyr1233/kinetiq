import { act, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { json, quizStatus, renderAppAt, SIGNED_IN } from '@/test/page'

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
    vi.useFakeTimers({ shouldAdvanceTime: true })
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    const messages = [fromTrainer('a', 'Any injuries I should know about?')]
    renderAppAt('/app/quiz/waiting', SIGNED_IN.CLIENT, waitingWith(messages))
    const sheet = await openConversation(user)
    await sheet.findByText('Any injuries I should know about?')

    messages.push(fromTrainer('b', 'Your plan is almost ready.'))
    await act(() => vi.advanceTimersByTimeAsync(5_000))

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
