import {
  focusManager,
  onlineManager,
  QueryClient,
  QueryClientProvider,
} from '@tanstack/react-query'
import { act, renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { FakeEventSource } from '@/test/event-source'
import { eventually } from '@/test/eventually'
import { json, pathsOf, problem, stubApi } from '@/test/page'
import {
  messagesOf,
  useConversations,
  useMessages,
  useUnreadCount,
  type Message,
} from './api/messaging'
import { useConversationEvents } from './use-conversation-events'

const ANNA = '00000000-0000-4000-8000-000000000003'
const PIOTR = '00000000-0000-4000-8000-000000000004'
const MESSAGES_OF_ANNA = `/api/v1/conversations/${ANNA}/messages`

function message(id: string, text: string): Message {
  return { id, text, mine: false, sentAt: '2026-11-11T09:12:00Z' }
}

/** A trainer's screen: the list, the count and Anna's conversation, as the server holds them now. */
function serve(messages: Message[], unread = { count: 0 }) {
  return stubApi((request) => {
    const path = new URL(request.url).pathname
    if (path === MESSAGES_OF_ANNA)
      return json({ messages: [...messages], hasMore: false })
    if (path === '/api/v1/conversations/unread-count') return json(unread)
    if (path === '/api/v1/conversations') return json({ conversations: [] })
    return new Response(null, { status: 404 })
  })
}

function renderEvents(accountId: string | undefined) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  )
  return renderHook(
    ({ id }) => {
      useConversationEvents(id)
      useConversations()
      useUnreadCount()
      return useMessages(ANNA)
    },
    { initialProps: { id: accountId }, wrapper },
  )
}

const count = (requests: Request[], path: string) =>
  pathsOf(requests).filter((other) => other === path).length

const stream = () => {
  const [only, ...others] = FakeEventSource.open()
  expect(others).toHaveLength(0)
  return only!
}

afterEach(() => {
  vi.useRealTimers()
  focusManager.setFocused(undefined)
  onlineManager.setOnline(true)
})

describe('useConversationEvents', () => {
  it('holds one stream for a signed-in account and none without one', () => {
    const { rerender, unmount } = renderEvents(undefined)
    expect(FakeEventSource.open()).toHaveLength(0)

    rerender({ id: 'trainer' })
    expect(stream().url).toBe('/api/v1/conversations/events')

    rerender({ id: 'other trainer' })
    expect(FakeEventSource.instances).toHaveLength(2)
    expect(FakeEventSource.instances[0]!.readyState).toBe(
      FakeEventSource.CLOSED,
    )
    expect(FakeEventSource.open()).toHaveLength(1)

    unmount()
    expect(FakeEventSource.open()).toHaveLength(0)
  })

  it('shows a new message of a loaded conversation once, with the list and count', async () => {
    const messages = [message('a', 'Thanks. The knee felt fine today.')]
    const requests = serve(messages)
    const { result } = renderEvents('trainer')
    await eventually(() => expect(result.current.isSuccess).toBe(true))
    const before = count(requests, '/api/v1/conversations/unread-count')

    messages.push(message('b', 'Can I swap leg curls for Nordic curls?'))
    act(() => stream().emit('message', ANNA))
    act(() => stream().emit('message', ANNA))

    await eventually(() =>
      expect(messagesOf(result.current.data).map(({ id }) => id)).toEqual([
        'a',
        'b',
      ]),
    )
    await eventually(() =>
      expect(count(requests, '/api/v1/conversations/unread-count')).toBe(
        before + 2,
      ),
    )
    expect(count(requests, '/api/v1/conversations')).toBeGreaterThan(1)
  })

  it('fetches no messages for a conversation this tab has not loaded', async () => {
    const requests = serve([])
    const { result } = renderEvents('trainer')
    await eventually(() => expect(result.current.isSuccess).toBe(true))
    const loads = count(requests, MESSAGES_OF_ANNA)

    act(() => stream().emit('message', PIOTR))

    await eventually(() =>
      expect(count(requests, '/api/v1/conversations')).toBe(2),
    )
    expect(pathsOf(requests)).not.toContain(
      `/api/v1/conversations/${PIOTR}/messages`,
    )
    expect(count(requests, MESSAGES_OF_ANNA)).toBe(loads)
  })

  it('refreshes the count when the person read a conversation elsewhere', async () => {
    const requests = serve([], { count: 2 })
    renderEvents('trainer')
    await eventually(() =>
      expect(count(requests, '/api/v1/conversations/unread-count')).toBe(1),
    )

    act(() => stream().emit('read', ANNA))

    await eventually(() =>
      expect(count(requests, '/api/v1/conversations/unread-count')).toBe(2),
    )
    expect(count(requests, MESSAGES_OF_ANNA)).toBe(1)
  })

  it('asks nothing again when the tab comes back or the network returns', async () => {
    const requests = serve([])
    const { result } = renderEvents('trainer')
    await eventually(() => expect(result.current.isSuccess).toBe(true))
    const before = requests.length

    act(() => focusManager.setFocused(false))
    act(() => focusManager.setFocused(true))
    act(() => onlineManager.setOnline(false))
    act(() => onlineManager.setOnline(true))
    await new Promise((resolve) => setTimeout(resolve, 100))

    expect(requests).toHaveLength(before)
  })

  it('catches up on everything it shows when the stream opens', async () => {
    const requests = serve([])
    const { result } = renderEvents('trainer')
    await eventually(() => expect(result.current.isSuccess).toBe(true))

    act(() => stream().connect())

    await eventually(() => {
      expect(count(requests, '/api/v1/conversations')).toBe(2)
      expect(count(requests, '/api/v1/conversations/unread-count')).toBe(2)
      expect(count(requests, MESSAGES_OF_ANNA)).toBe(2)
    })
  })

  it('opens the stream again after the browser gave up, waiting longer each time', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    let reachable = false
    stubApi((request) =>
      new URL(request.url).pathname === '/api/v1/conversations/unread-count'
        ? reachable
          ? json({ count: 0 })
          : problem(502, { status: 502 })
        : json({ conversations: [], messages: [], hasMore: false }),
    )
    renderEvents('trainer')

    act(() => stream().giveUp())
    expect(FakeEventSource.open()).toHaveLength(0)
    // the first wait is one second, the next two
    await act(() => vi.advanceTimersByTimeAsync(1_000))
    expect(FakeEventSource.open()).toHaveLength(0)
    reachable = true
    await act(() => vi.advanceTimersByTimeAsync(1_000))
    expect(FakeEventSource.open()).toHaveLength(0)
    await act(() => vi.advanceTimersByTimeAsync(1_000))

    await eventually(() => expect(FakeEventSource.open()).toHaveLength(1))
  })

  it('replaces a stream that has gone silent, but not one that pings', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    stubApi((request) =>
      new URL(request.url).pathname === '/api/v1/conversations/unread-count'
        ? json({ count: 0 })
        : json({ conversations: [], messages: [], hasMore: false }),
    )
    renderEvents('trainer')
    const first = stream()
    act(() => first.connect())

    await act(() => vi.advanceTimersByTimeAsync(50_000))
    act(() => first.emit('ping', 'ping'))
    await act(() => vi.advanceTimersByTimeAsync(50_000))
    expect(first.readyState).toBe(FakeEventSource.OPEN)

    await act(() => vi.advanceTimersByTimeAsync(10_000))
    expect(first.readyState).toBe(FakeEventSource.CLOSED)
    await act(() => vi.advanceTimersByTimeAsync(1_000))
    await eventually(() => expect(FakeEventSource.open()).toHaveLength(1))
    expect(stream()).not.toBe(first)
  })

  it('stops trying once the sign-in has ended', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    stubApi(() =>
      problem(401, { status: 401, code: 'request.unauthenticated' }),
    )
    renderEvents('trainer')

    act(() => stream().giveUp())
    await act(() => vi.advanceTimersByTimeAsync(60_000))

    expect(FakeEventSource.instances).toHaveLength(1)
  })

  it('makes no attempt after the page has gone', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    const requests = stubApi(() => json({ count: 0 }))
    const { unmount } = renderEvents('trainer')
    // the screen's own first loads
    await eventually(() => expect(requests).toHaveLength(3))

    act(() => stream().giveUp())
    unmount()
    const before = requests.length
    await act(() => vi.advanceTimersByTimeAsync(5_000))

    expect(requests.length).toBe(before)
    expect(FakeEventSource.instances).toHaveLength(1)
  })
})
