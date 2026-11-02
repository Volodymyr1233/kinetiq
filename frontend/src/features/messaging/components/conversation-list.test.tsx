import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { renderPage } from '@/test/page'
import type { Conversation } from '../api/messaging'
import { ConversationList } from './conversation-list'

const NOW = new Date('2026-11-02T12:00:00')
const ANNA: Conversation = {
  clientId: 'anna',
  name: 'Anna Kowalska',
  unreadCount: 2,
  lastMessage: {
    preview: 'Can I swap leg curls?',
    sentByMe: false,
    sentAt: new Date('2026-11-02T09:20:00').toISOString(),
  },
}
const PIOTR: Conversation = {
  clientId: 'piotr',
  name: 'Piotr Zieliński',
  unreadCount: 0,
  lastMessage: {
    preview: 'Try 3 × 6 on the press.',
    sentByMe: true,
    sentAt: new Date('2026-11-01T18:00:00').toISOString(),
  },
}
const OLA: Conversation = {
  clientId: 'ola',
  name: 'Ola Dąbrowska',
  unreadCount: 0,
}

function renderList(conversations: Conversation[], openId?: string) {
  renderPage(
    <ConversationList
      conversations={conversations}
      openId={openId}
      now={NOW}
    />,
  )
}

const rowNames = () =>
  screen
    .getAllByRole('listitem')
    .map((row) => within(row).getByRole('link').textContent)

describe('ConversationList', () => {
  it('shows each row with its name, last message, time and count', () => {
    renderList([ANNA, PIOTR, OLA])

    const anna = screen.getByRole('link', { name: /^Anna Kowalska, 2 unread/ })
    expect(anna).toHaveAttribute('href', '/trainer/messages/anna')
    expect(within(anna).getByText('Can I swap leg curls?')).toBeInTheDocument()
    expect(within(anna).getByText('09:20')).toBeInTheDocument()
    expect(within(anna).getByText('2')).toBeInTheDocument()

    const piotr = screen.getByRole('link', { name: /^Piotr/ })
    expect(
      within(piotr).getByText('You: Try 3 × 6 on the press.'),
    ).toBeInTheDocument()
    expect(within(piotr).getByText('Yesterday')).toBeInTheDocument()
  })

  it('says when a client has no messages yet', () => {
    renderList([OLA])

    expect(
      within(screen.getByRole('link', { name: /^Ola/ })).getByText(
        'No messages yet',
      ),
    ).toBeInTheDocument()
  })

  it('narrows the rows as the trainer types, keeping order and counts', async () => {
    renderList([ANNA, PIOTR, OLA])

    await userEvent.type(
      screen.getByRole('searchbox', { name: 'Search clients' }),
      'A',
    )
    expect(rowNames()).toHaveLength(2)

    await userEvent.type(
      screen.getByRole('searchbox', { name: 'Search clients' }),
      'nna',
    )
    expect(
      screen.getByRole('link', { name: /^Anna Kowalska, 2 unread/ }),
    ).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /^Piotr/ })).toBeNull()
  })

  it('says when no client matches and brings every client back when cleared', async () => {
    renderList([ANNA, PIOTR])
    const search = screen.getByRole('searchbox', { name: 'Search clients' })

    await userEvent.type(search, 'zzz')
    expect(screen.getByRole('status')).toHaveTextContent(
      'No client matches that name.',
    )
    expect(screen.queryByRole('listitem')).toBeNull()

    await userEvent.clear(search)
    expect(rowNames()).toHaveLength(2)
    expect(screen.queryByRole('status')).toBeNull()
  })

  it('marks the open conversation', () => {
    renderList([ANNA, PIOTR], 'piotr')

    expect(screen.getByRole('link', { name: /^Piotr/ })).toHaveAttribute(
      'aria-current',
      'true',
    )
    expect(screen.getByRole('link', { name: /^Anna/ })).not.toHaveAttribute(
      'aria-current',
    )
  })
})
