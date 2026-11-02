import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { Message } from '../api/messaging'
import type { OutboxEntry } from '../use-outbox'
import { MessageList } from './message-list'

const NOW = new Date('2026-11-02T12:00:00')

function message(id: string, text: string, mine: boolean, at: string): Message {
  return { id, text, mine, sentAt: new Date(at).toISOString() }
}

function renderList(
  messages: Message[],
  {
    outbox = [],
    hasMore = false,
    onRetry = () => {},
    onLoadEarlier = () => {},
  }: {
    outbox?: OutboxEntry[]
    hasMore?: boolean
    onRetry?: (id: string) => void
    onLoadEarlier?: () => void
  } = {},
) {
  render(
    <MessageList
      messages={messages}
      outbox={outbox}
      hasMore={hasMore}
      onLoadEarlier={onLoadEarlier}
      onRetry={onRetry}
      peerName="Marek"
      now={NOW}
    />,
  )
  return screen.getByRole('log', { name: 'Messages with Marek' })
}

describe('MessageList', () => {
  it('shows messages in order with the newest last and marks them for a screen reader', () => {
    const list = renderList([
      message('1', 'first', false, '2026-11-02T09:00:00'),
      message('2', 'second', true, '2026-11-02T09:05:00'),
    ])

    expect(list.textContent).toMatch(/first.*second/)
    expect(list).toHaveAttribute('aria-live', 'polite')
    expect(screen.getByText('first').textContent).toBe('Marek: first')
    expect(screen.getByText('second').textContent).toBe('You: second')
  })

  it('shows the time only under the last message of a run', () => {
    renderList([
      message('1', 'one', true, '2026-11-02T09:00:00'),
      message('2', 'two', true, '2026-11-02T09:01:00'),
      message('3', 'three', false, '2026-11-02T09:02:00'),
      message('4', 'four', false, '2026-11-02T09:03:00'),
    ])

    expect(screen.queryByText('Sent · 09:00')).not.toBeInTheDocument()
    expect(screen.getByText('Sent · 09:01')).toBeInTheDocument()
    expect(screen.queryByText('Marek · 09:02')).not.toBeInTheDocument()
    expect(screen.getByText('Marek · 09:03')).toBeInTheDocument()
  })

  it('separates the days and ends a run at the end of a day', () => {
    renderList([
      message('1', 'old', true, '2026-10-05T09:00:00'),
      message('2', 'then', true, '2026-11-01T09:00:00'),
      message('3', 'now', true, '2026-11-02T09:00:00'),
    ])

    expect(screen.getByText('Mon, Oct 5')).toBeInTheDocument()
    expect(screen.getByText('Yesterday')).toBeInTheDocument()
    expect(screen.getByText('Today')).toBeInTheDocument()
    expect(screen.getAllByText(/^Sent · 09:00$/)).toHaveLength(3)
  })

  it('shows a sending message with its state', () => {
    renderList([], {
      outbox: [
        { id: 'a', conversation: 'c', text: 'on its way', status: 'sending' },
      ],
    })

    expect(screen.getByText('on its way')).toBeInTheDocument()
    expect(screen.getByText('Sending…')).toBeInTheDocument()
    expect(screen.getByText('Today')).toBeInTheDocument()
  })

  it('keeps the text of a failed message and retries it by id', async () => {
    const onRetry = vi.fn()
    renderList([], {
      outbox: [{ id: 'a', conversation: 'c', text: 'lost', status: 'failed' }],
      onRetry,
    })

    expect(screen.getByText('lost')).toBeInTheDocument()
    expect(screen.getByText('Not sent')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }))

    expect(onRetry).toHaveBeenCalledWith('a')
  })

  it('shows once a failed message whose first attempt did arrive', () => {
    renderList([message('a', 'arrived', true, '2026-11-02T09:00:00')], {
      outbox: [
        { id: 'a', conversation: 'c', text: 'arrived', status: 'failed' },
      ],
    })

    expect(screen.getAllByText('arrived')).toHaveLength(1)
    expect(screen.queryByText('Not sent')).not.toBeInTheDocument()
  })

  it('never suggests whether the other person has read a message', () => {
    const list = renderList([message('1', 'one', true, '2026-11-02T09:00:00')])

    expect(list).not.toHaveTextContent(/read|seen|delivered/i)
    expect(list.querySelector('svg')).toBeNull()
  })

  it('offers earlier messages only when more exist', async () => {
    const onLoadEarlier = vi.fn()
    renderList([message('1', 'one', true, '2026-11-02T09:00:00')], {
      hasMore: true,
      onLoadEarlier,
    })

    await userEvent.click(
      screen.getByRole('button', { name: 'Load earlier messages' }),
    )
    expect(onLoadEarlier).toHaveBeenCalledOnce()
  })

  it('offers nothing earlier when the first message is reached', () => {
    renderList([message('1', 'one', true, '2026-11-02T09:00:00')])

    expect(
      screen.queryByRole('button', { name: 'Load earlier messages' }),
    ).not.toBeInTheDocument()
  })

  it('shows markup and addresses as typed', () => {
    const list = renderList([
      message(
        '1',
        '<b>x</b> https://example.com',
        false,
        '2026-11-02T09:00:00',
      ),
    ])

    expect(screen.getByText('<b>x</b> https://example.com')).toBeInTheDocument()
    expect(list.querySelector('b')).toBeNull()
    expect(screen.queryByRole('link')).not.toBeInTheDocument()
  })
})
