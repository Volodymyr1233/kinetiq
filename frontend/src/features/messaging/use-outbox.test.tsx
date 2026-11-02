import { act, renderHook, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { useOutbox } from './use-outbox'

describe('useOutbox', () => {
  it('shows a message as sending and removes it once confirmed', async () => {
    let confirm = () => {}
    const send = vi.fn(
      () =>
        new Promise<void>((resolve) => {
          confirm = resolve
        }),
    )
    const { result } = renderHook(() => useOutbox(send))

    act(() => result.current.submit('anna', 'hello'))

    expect(result.current.entries).toMatchObject([
      { conversation: 'anna', text: 'hello', status: 'sending' },
    ])
    await act(async () => confirm())
    expect(result.current.entries).toEqual([])
  })

  it('keeps the text as failed when sending fails', async () => {
    const send = vi.fn().mockRejectedValue(new Error('offline'))
    const { result } = renderHook(() => useOutbox(send))

    act(() => result.current.submit('anna', 'hello'))

    await waitFor(() =>
      expect(result.current.entries).toMatchObject([
        { text: 'hello', status: 'failed' },
      ]),
    )
  })

  it('sends the same id and text again on retry', async () => {
    const send = vi
      .fn()
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce(undefined)
    const { result } = renderHook(() => useOutbox(send))
    act(() => result.current.submit('anna', 'hello'))
    await waitFor(() =>
      expect(result.current.entries[0]?.status).toBe('failed'),
    )
    const id = result.current.entries[0]!.id

    act(() => result.current.retry(id))

    await waitFor(() => expect(result.current.entries).toEqual([]))
    expect(send).toHaveBeenCalledTimes(2)
    expect(send.mock.calls[1]![0]).toMatchObject({ id, text: 'hello' })
  })

  it('does not send again while an attempt is open', async () => {
    const send = vi.fn(() => new Promise(() => {}))
    const { result } = renderHook(() => useOutbox(send))
    act(() => result.current.submit('anna', 'hello'))

    act(() => result.current.retry(result.current.entries[0]!.id))

    expect(send).toHaveBeenCalledTimes(1)
  })

  it('keeps two quick messages as two entries with their own ids', () => {
    const send = vi.fn(() => new Promise(() => {}))
    const { result } = renderHook(() => useOutbox(send))

    act(() => {
      result.current.submit('anna', 'one')
      result.current.submit('anna', 'two')
    })

    const [first, second] = result.current.entries
    expect(first?.text).toBe('one')
    expect(second?.text).toBe('two')
    expect(first?.id).not.toBe(second?.id)
  })
})
