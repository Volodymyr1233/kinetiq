import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  json,
  pathsOf,
  problem,
  renderAppAt,
  SIGNED_IN,
  type Reply,
} from '@/test/page'

const CLIENT = SIGNED_IN.CLIENT.id
const MESSAGES = `/api/v1/conversations/${CLIENT}/messages`
const READ = `/api/v1/conversations/${CLIENT}/read`
const FROM_TRAINER = {
  id: '00000000-0000-4000-8000-000000000701',
  text: 'Nice depth on set 3.',
  mine: false,
  sentAt: new Date().toISOString(),
}

function server({
  messages = [FROM_TRAINER] as object[],
  unreadCount = 0,
  onSend,
  onMessages,
}: {
  messages?: object[]
  unreadCount?: number
  onSend?: Reply
  onMessages?: Reply
} = {}): Reply {
  let unread = unreadCount
  return async (request) => {
    const { pathname } = new URL(request.url)
    if (pathname === READ) {
      unread = 0
      return new Response(null, { status: 204 })
    }
    if (pathname === '/api/v1/conversations') {
      return json({
        conversations: [
          { clientId: CLIENT, name: 'Marek Nowak', unreadCount: unread },
        ],
      })
    }
    if (pathname === '/api/v1/conversations/unread-count') {
      return json({ count: unread })
    }
    if (pathname === MESSAGES && request.method === 'GET') {
      return onMessages?.(request) ?? json({ messages, hasMore: false })
    }
    if (pathname === MESSAGES && request.method === 'POST') {
      if (onSend) return onSend(request)
      const { id, text } = await request.json()
      return json(
        { id, text, mine: true, sentAt: new Date().toISOString() },
        201,
      )
    }
    return new Response(null, { status: 204 })
  }
}

const reads = (requests: Request[]) =>
  requests.filter(
    (request) =>
      request.method === 'PUT' && new URL(request.url).pathname === READ,
  )
const sends = (requests: Request[]) =>
  requests.filter(
    (request) =>
      request.method === 'POST' && new URL(request.url).pathname === MESSAGES,
  )

function hideTab(state: 'hidden' | 'visible') {
  vi.spyOn(document, 'visibilityState', 'get').mockReturnValue(state)
  document.dispatchEvent(new Event('visibilitychange'))
}

const field = () => screen.findByRole('textbox', { name: 'Message Marek' })

describe('ClientMessagesPage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('opens in the conversation with the name of the trainer and the newest message', async () => {
    renderAppAt('/app/messages', SIGNED_IN.CLIENT, server())

    expect(
      await screen.findByRole('heading', { name: 'Marek Nowak' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Your trainer')).toBeInTheDocument()
    expect(await screen.findByText(FROM_TRAINER.text)).toBeInTheDocument()
  })

  it('invites the first message in an empty conversation', async () => {
    renderAppAt('/app/messages', SIGNED_IN.CLIENT, server({ messages: [] }))

    expect(await screen.findByText('Say hello to Marek.')).toBeInTheDocument()
    expect(await field()).toBeInTheDocument()
  })

  it('shows a message as sending and then as sent', async () => {
    let answer: (response: Response) => void = () => {}
    const answered = new Promise<Response>((resolve) => {
      answer = resolve
    })
    const { requests } = renderAppAt(
      '/app/messages',
      SIGNED_IN.CLIENT,
      server({ onSend: () => answered }),
    )

    await userEvent.type(await field(), 'The knee felt fine.{Enter}')

    expect(screen.getByText('The knee felt fine.')).toBeInTheDocument()
    expect(screen.getByText('Sending…')).toBeInTheDocument()
    expect(await field()).toHaveValue('')

    await waitFor(() => expect(sends(requests)).toHaveLength(1))
    const { id, text } = await sends(requests)[0]!.clone().json()
    answer(
      json({ id, text, mine: true, sentAt: new Date().toISOString() }, 201),
    )

    expect(await screen.findByText(/^Sent · \d\d:\d\d$/)).toBeInTheDocument()
    expect(screen.queryByText('Sending…')).not.toBeInTheDocument()
    expect(screen.getAllByText('The knee felt fine.')).toHaveLength(1)
  })

  it('keeps the text of a failed message, and Retry sends the same id once', async () => {
    let fail = true
    const { requests } = renderAppAt(
      '/app/messages',
      SIGNED_IN.CLIENT,
      server({
        onSend: async (request) => {
          if (fail) return problem(500, { status: 500, code: 'internal.error' })
          const { id, text } = await request.json()
          return json(
            { id, text, mine: true, sentAt: new Date().toISOString() },
            201,
          )
        },
      }),
    )
    const user = userEvent.setup()

    await user.type(await field(), 'Can I swap leg curls?{Enter}')

    expect(await screen.findByText('Not sent')).toBeInTheDocument()
    expect(screen.getByText('Can I swap leg curls?')).toBeInTheDocument()
    expect(screen.queryByText(/internal/)).not.toBeInTheDocument()

    fail = false
    await user.click(screen.getByRole('button', { name: 'Retry' }))

    expect(await screen.findByText(/^Sent · \d\d:\d\d$/)).toBeInTheDocument()
    expect(screen.queryByText('Not sent')).not.toBeInTheDocument()
    expect(screen.getAllByText('Can I swap leg curls?')).toHaveLength(1)
    const [first, second] = await Promise.all(
      sends(requests).map((request) => request.clone().json()),
    )
    expect(sends(requests)).toHaveLength(2)
    expect(second).toEqual(first)
  })

  it('marks a visible conversation with unread messages read, once', async () => {
    const { requests } = renderAppAt(
      '/app/messages',
      SIGNED_IN.CLIENT,
      server({ unreadCount: 2 }),
    )

    await waitFor(() => expect(reads(requests)).toHaveLength(1))
  })

  it('marks nothing read while the messages have not loaded', async () => {
    const { requests } = renderAppAt(
      '/app/messages',
      SIGNED_IN.CLIENT,
      server({
        unreadCount: 2,
        onMessages: () => problem(500, { status: 500, code: 'internal.error' }),
      }),
    )

    expect(
      await screen.findByRole('heading', { name: 'Marek Nowak' }),
    ).toBeInTheDocument()
    await waitFor(() =>
      expect(pathsOf(requests)).toContain('/api/v1/conversations'),
    )
    expect(reads(requests)).toHaveLength(0)
  })

  it('marks nothing read when nothing is unread', async () => {
    const { requests } = renderAppAt(
      '/app/messages',
      SIGNED_IN.CLIENT,
      server(),
    )

    await screen.findByText(FROM_TRAINER.text)
    expect(reads(requests)).toHaveLength(0)
  })

  it('leaves messages unread in a hidden tab until it is shown', async () => {
    hideTab('hidden')
    const { requests } = renderAppAt(
      '/app/messages',
      SIGNED_IN.CLIENT,
      server({ unreadCount: 2 }),
    )

    await screen.findByText(FROM_TRAINER.text)
    expect(reads(requests)).toHaveLength(0)

    hideTab('visible')

    await waitFor(() => expect(reads(requests)).toHaveLength(1))
  })

  it('offers Try again when the conversation fails to load', async () => {
    let fail = true
    renderAppAt(
      '/app/messages',
      SIGNED_IN.CLIENT,
      server({
        onMessages: () =>
          fail
            ? problem(404, { status: 404, code: 'conversation.not-found' })
            : json({ messages: [FROM_TRAINER], hasMore: false }),
      }),
    )

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent(
      'This conversation does not exist, or is not yours.',
    )

    fail = false
    await userEvent.click(
      within(alert).getByRole('button', { name: 'Try again' }),
    )

    expect(await screen.findByText(FROM_TRAINER.text)).toBeInTheDocument()
  })

  it('offers Try again when the trainer cannot be loaded', async () => {
    renderAppAt('/app/messages', SIGNED_IN.CLIENT, (request) =>
      new URL(request.url).pathname === '/api/v1/conversations'
        ? problem(404, { status: 404, code: 'request.not-found' })
        : new Response(null, { status: 204 }),
    )

    expect(
      within(await screen.findByRole('alert')).getByRole('button', {
        name: 'Try again',
      }),
    ).toBeInTheDocument()
  })
})
