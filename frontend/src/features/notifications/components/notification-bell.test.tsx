import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import i18n from '@/lib/i18n'
import { NOTIFICATIONS } from '@/lib/query-keys'
import { json, stubApi, type Reply } from '@/test/page'
import { NotificationBell } from './notification-bell'

const toast = vi.hoisted(() => vi.fn())
vi.mock('sonner', () => ({ toast }))

const CLIENT_ID = '00000000-0000-4000-8000-000000000003'

const item = (over: Record<string, unknown>) => ({
  id: crypto.randomUUID(),
  kind: 'PLAN_PUBLISHED',
  createdAt: '2026-11-14T10:00:00Z',
  read: false,
  ...over,
})

const list = (items: object[], unread = items.length) => json({ items, unread })

function Where() {
  return <p data-testid="where">{useLocation().pathname}</p>
}

function open(
  area: 'client' | 'trainer',
  reply: Reply,
  client = new QueryClient({ defaultOptions: { queries: { retry: false } } }),
) {
  const requests = stubApi(reply)
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <NotificationBell area={area} />
        <Routes>
          <Route path="*" element={<Where />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
  return { requests, client }
}

describe('NotificationBell', () => {
  beforeEach(() => toast.mockClear())
  afterEach(() => i18n.changeLanguage('en'))

  it('says how many notifications are unread, and 99+ above 99', async () => {
    open('client', () => list([item({})], 120))

    expect(
      await screen.findByRole('button', {
        name: 'Notifications, 120 unread',
      }),
    ).toBeInTheDocument()
    expect(screen.getByText('99+')).toBeInTheDocument()
  })

  it('has no count when everything is read', async () => {
    open('client', () => list([item({ read: true })], 0))

    expect(
      await screen.findByRole('button', { name: 'Notifications' }),
    ).toBeInTheDocument()
  })

  it('lists the notifications in the sheet, naming a client where there is one', async () => {
    const user = userEvent.setup()
    open('trainer', () =>
      list(
        [
          item({
            kind: 'RECOVERY_DAY',
            clientId: CLIENT_ID,
            clientName: 'Ola Kowalska',
          }),
          item({ kind: 'CLIENT_DELETED', read: true }),
        ],
        1,
      ),
    )

    await user.click(
      await screen.findByRole('button', { name: 'Notifications, 1 unread' }),
    )

    expect(screen.getByText('Ola Kowalska has a recovery day.')).toBeVisible()
    expect(screen.getByText('A client deleted their account.')).toBeVisible()
    expect(screen.getByText('Unread.')).toBeInTheDocument()
  })

  it('says there is nothing when there are no notifications', async () => {
    const user = userEvent.setup()
    open('client', () => list([]))

    await user.click(
      await screen.findByRole('button', { name: 'Notifications' }),
    )

    expect(screen.getByText('You have no notifications.')).toBeVisible()
  })

  it('marks a chosen notification read and goes to the screen it is about', async () => {
    const user = userEvent.setup()
    const note = item({ kind: 'NEW_MESSAGE' })
    const { requests } = open('client', (request) =>
      request.method === 'POST'
        ? new Response(null, { status: 204 })
        : list([note]),
    )

    await user.click(
      await screen.findByRole('button', { name: 'Notifications, 1 unread' }),
    )
    await user.click(screen.getByText('New message from your trainer.'))

    expect(screen.getByTestId('where')).toHaveTextContent('/app/messages')
    const post = requests.find((request) => request.method === 'POST')
    expect(new URL(post!.url).pathname).toBe(
      `/api/v1/notifications/${note.id}/read`,
    )
  })

  it("opens the client's page from a recovery day", async () => {
    const user = userEvent.setup()
    open('trainer', (request) =>
      request.method === 'POST'
        ? new Response(null, { status: 204 })
        : list([
            item({
              kind: 'RECOVERY_DAY',
              clientId: CLIENT_ID,
              clientName: 'Ola Kowalska',
            }),
          ]),
    )

    await user.click(
      await screen.findByRole('button', { name: 'Notifications, 1 unread' }),
    )
    await user.click(screen.getByText('Ola Kowalska has a recovery day.'))

    expect(screen.getByTestId('where')).toHaveTextContent(
      `/trainer/clients/${CLIENT_ID}`,
    )
  })

  it('marks a notification that points nowhere read and stays where it is', async () => {
    const user = userEvent.setup()
    const { requests } = open('trainer', (request) =>
      request.method === 'POST'
        ? new Response(null, { status: 204 })
        : list([item({ kind: 'CLIENT_DELETED' })]),
    )

    await user.click(
      await screen.findByRole('button', { name: 'Notifications, 1 unread' }),
    )
    await user.click(screen.getByText('A client deleted their account.'))

    expect(screen.getByTestId('where')).toHaveTextContent('/')
    expect(requests.some((request) => request.method === 'POST')).toBe(true)
  })

  it('shows a toast for a notification that arrives, and none for those already there', async () => {
    const first = item({ kind: 'PLAN_PUBLISHED' })
    const arrived = item({ kind: 'PLAN_CHANGED' })
    let answer = [first]
    const { client } = open('client', () => list(answer))
    await screen.findByRole('button', { name: 'Notifications, 1 unread' })
    expect(toast).not.toHaveBeenCalled()

    answer = [arrived, first]
    await act(() => client.invalidateQueries({ queryKey: NOTIFICATIONS }))

    await screen.findByRole('button', { name: 'Notifications, 2 unread' })
    expect(toast).toHaveBeenCalledTimes(1)
    expect(toast).toHaveBeenCalledWith('Your trainer changed your plan.')
  })

  it('speaks Polish', async () => {
    await i18n.changeLanguage('pl')
    const user = userEvent.setup()
    open('trainer', () =>
      list([
        item({
          kind: 'DRAFT_TO_REVIEW',
          clientId: CLIENT_ID,
          clientName: 'Ola Kowalska',
        }),
      ]),
    )

    await user.click(
      await screen.findByRole('button', {
        name: 'Powiadomienia, 1 nieprzeczytane',
      }),
    )

    expect(
      screen.getByText('Szkic planu dla Ola Kowalska czeka na przegląd.'),
    ).toBeVisible()
  })
})
