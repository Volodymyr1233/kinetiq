import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { json, problem, renderAppAt, SIGNED_IN, type Reply } from '@/test/page'

const ANNA = '00000000-0000-4000-8000-000000000003'
const PIOTR = '00000000-0000-4000-8000-000000000005'

function server({
  failSend = () => false,
  conversations,
}: {
  failSend?: () => boolean
  conversations?: object[]
} = {}): Reply {
  const unread = new Map([
    [ANNA, 2],
    [PIOTR, 0],
  ])
  const rows = () =>
    conversations ?? [
      {
        clientId: ANNA,
        name: 'Anna Kowalska',
        goal: 'BUILD_MUSCLE',
        currentWeek: 3,
        planWeeks: 8,
        unreadCount: unread.get(ANNA),
        lastMessage: {
          preview: 'Can I swap leg curls?',
          sentByMe: false,
          sentAt: new Date().toISOString(),
        },
      },
      {
        clientId: PIOTR,
        name: 'Piotr Zieliński',
        unreadCount: unread.get(PIOTR),
      },
    ]
  return async (request) => {
    const { pathname } = new URL(request.url)
    const [, clientId, action] =
      /\/conversations\/([^/]+)\/(\w+)$/.exec(pathname) ?? []
    if (pathname === '/api/v1/conversations') {
      return json({ conversations: rows() })
    }
    if (pathname === '/api/v1/conversations/unread-count') {
      return json({
        count: [...unread.values()].reduce((sum, count) => sum + count),
      })
    }
    if (action === 'read') {
      unread.set(clientId!, 0)
      return new Response(null, { status: 204 })
    }
    if (action === 'messages' && request.method === 'POST') {
      if (failSend()) {
        return problem(500, { status: 500, code: 'internal.error' })
      }
      const { id, text } = await request.json()
      return json(
        { id, text, mine: true, sentAt: new Date().toISOString() },
        201,
      )
    }
    if (action === 'messages') {
      return json({
        messages:
          clientId === ANNA
            ? [
                {
                  id: '00000000-0000-4000-8000-000000000701',
                  text: 'Can I swap leg curls?',
                  mine: false,
                  sentAt: new Date().toISOString(),
                },
              ]
            : [],
        hasMore: false,
      })
    }
    return new Response(null, { status: 204 })
  }
}

const reads = (requests: Request[]) =>
  requests.filter((request) => request.method === 'PUT')
const list = () => screen.findByRole('navigation', { name: 'Conversations' })
const composer = (name: string) =>
  screen.findByRole('textbox', { name: `Message ${name}` })

describe('TrainerMessagesPage', () => {
  it('opens with the list and a prompt, and marks nothing read', async () => {
    const { requests } = renderAppAt(
      '/trainer/messages',
      SIGNED_IN.TRAINER,
      server(),
    )

    expect(
      await within(await list()).findByRole('link', {
        name: /^Anna Kowalska, 2 unread/,
      }),
    ).toBeInTheDocument()
    expect(screen.getByText('Choose a conversation')).toBeInTheDocument()
    expect(reads(requests)).toHaveLength(0)
  })

  it('opens a chosen row by its address, marks it read and its count goes', async () => {
    const { requests, router } = renderAppAt(
      '/trainer/messages',
      SIGNED_IN.TRAINER,
      server(),
    )
    const rows = within(await list())

    await userEvent.click(
      await rows.findByRole('link', { name: /^Anna Kowalska, 2 unread/ }),
    )

    expect(router.state.location.pathname).toBe(`/trainer/messages/${ANNA}`)
    expect(
      await screen.findByRole('heading', { name: 'Anna Kowalska' }),
    ).toBeInTheDocument()
    expect(
      await screen.findByRole('log', { name: 'Messages with Anna' }),
    ).toHaveTextContent('Can I swap leg curls?')
    await waitFor(() => expect(reads(requests)).toHaveLength(1))
    expect(new URL(reads(requests)[0]!.url).pathname).toContain(ANNA)
    expect(
      await rows.findByRole('link', { name: /^Anna Kowalska(?!, 2 unread)/ }),
    ).toHaveAttribute('aria-current', 'true')
  })

  it('drops the total in the sidebar after opening a conversation', async () => {
    renderAppAt('/trainer/messages', SIGNED_IN.TRAINER, server())
    const sidebar = within(
      await screen.findByRole('navigation', { name: 'Trainer area' }),
    )
    expect(
      await sidebar.findByRole('link', { name: 'Messages, 2 unread' }),
    ).toBeInTheDocument()

    await userEvent.click(
      await within(await list()).findByRole('link', { name: /^Anna/ }),
    )

    expect(
      await sidebar.findByRole('link', { name: 'Messages' }),
    ).toBeInTheDocument()
  })

  it('opens the same conversation from its address', async () => {
    renderAppAt(`/trainer/messages/${ANNA}`, SIGNED_IN.TRAINER, server())

    expect(
      await screen.findByRole('heading', { name: 'Anna Kowalska' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Build muscle · Week 3 of 8')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Open profile' })).toHaveAttribute(
      'href',
      `/trainer/clients/${ANNA}`,
    )
    expect(
      screen.getByText('Enter sends. Shift+Enter adds a line.'),
    ).toBeInTheDocument()
  })

  it('leaves out the week of a finished plan', async () => {
    renderAppAt(
      `/trainer/messages/${ANNA}`,
      SIGNED_IN.TRAINER,
      server({
        conversations: [
          {
            clientId: ANNA,
            name: 'Anna Kowalska',
            goal: 'BUILD_MUSCLE',
            currentWeek: 9,
            planWeeks: 8,
            unreadCount: 0,
          },
        ],
      }),
    )

    expect(await screen.findByText('Build muscle')).toBeInTheDocument()
  })

  it.each(['00000000-0000-4000-8000-000000000099', 'not-an-id'])(
    'shows the not-found state for %s and keeps the list',
    async (id) => {
      const { requests } = renderAppAt(
        `/trainer/messages/${id}`,
        SIGNED_IN.TRAINER,
        server(),
      )

      expect(
        await screen.findByText('Conversation not found'),
      ).toBeInTheDocument()
      expect(
        within(await list()).getByRole('link', { name: /^Anna/ }),
      ).toBeInTheDocument()
      expect(
        requests.some((request) =>
          new URL(request.url).pathname.includes(`/${id}/`),
        ),
      ).toBe(false)
    },
  )

  it('keeps the open conversation writable when the search hides its row', async () => {
    renderAppAt(`/trainer/messages/${ANNA}`, SIGNED_IN.TRAINER, server())
    const rows = within(await list())

    await userEvent.type(
      await rows.findByRole('searchbox', { name: 'Search clients' }),
      'piotr',
    )

    expect(rows.queryByRole('link', { name: /^Anna/ })).toBeNull()
    await userEvent.type(await composer('Anna'), 'Still here{Enter}')
    expect(await screen.findByText('Still here')).toBeInTheDocument()
  })

  it('sends on Enter and adds a line on Shift+Enter', async () => {
    const { requests } = renderAppAt(
      `/trainer/messages/${ANNA}`,
      SIGNED_IN.TRAINER,
      server(),
    )
    const posted = () => requests.filter((request) => request.method === 'POST')

    await userEvent.type(await composer('Anna'), 'one{Shift>}{Enter}{/Shift}')
    expect(posted()).toHaveLength(0)

    await userEvent.keyboard('two{Enter}')

    await waitFor(() => expect(posted()).toHaveLength(1))
    expect(await posted()[0]!.clone().json()).toMatchObject({
      text: 'one\ntwo',
    })
  })

  it('offers Retry for a failed send, and keeps it while another conversation is open', async () => {
    let fail = true
    renderAppAt(
      `/trainer/messages/${ANNA}`,
      SIGNED_IN.TRAINER,
      server({ failSend: () => fail }),
    )
    const user = userEvent.setup()
    const rows = within(await list())

    await user.type(await composer('Anna'), 'Rest 3 min{Enter}')
    expect(await screen.findByText('Not sent')).toBeInTheDocument()

    await user.click(await rows.findByRole('link', { name: /^Piotr/ }))
    expect(await composer('Piotr')).toHaveValue('')
    expect(screen.queryByText('Rest 3 min')).toBeNull()

    await user.click(rows.getByRole('link', { name: /^Anna/ }))
    expect(await screen.findByText('Rest 3 min')).toBeInTheDocument()

    fail = false
    await user.click(await screen.findByRole('button', { name: 'Retry' }))

    expect(await screen.findByText(/^Sent · \d\d:\d\d$/)).toBeInTheDocument()
    expect(screen.queryByText('Not sent')).toBeNull()
  })

  it('offers a way back to the list from a conversation', async () => {
    const { router } = renderAppAt(
      `/trainer/messages/${ANNA}`,
      SIGNED_IN.TRAINER,
      server(),
    )

    await userEvent.click(
      await screen.findByRole('link', { name: 'All conversations' }),
    )

    expect(router.state.location.pathname).toBe('/trainer/messages')
    expect(await screen.findByText('Choose a conversation')).toBeInTheDocument()
  })

  it('offers an invite when the trainer has no clients', async () => {
    renderAppAt(
      '/trainer/messages',
      SIGNED_IN.TRAINER,
      server({ conversations: [] }),
    )

    expect(
      await screen.findByRole('link', { name: 'Invite a client' }),
    ).toHaveAttribute('href', '/trainer/clients/invites')
    expect(screen.getByText('No conversations yet')).toBeInTheDocument()
    expect(screen.queryByText('Choose a conversation')).toBeNull()
  })

  it('offers Try again when the list fails to load', async () => {
    renderAppAt('/trainer/messages', SIGNED_IN.TRAINER, (request) =>
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
