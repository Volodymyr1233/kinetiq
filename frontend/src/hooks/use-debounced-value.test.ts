import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useDebouncedValue } from './use-debounced-value'

describe('useDebouncedValue', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('passes a new value on only after the delay', () => {
    const { result, rerender } = renderHook(
      ({ value }) => useDebouncedValue(value, 300),
      { initialProps: { value: 'a' } },
    )

    rerender({ value: 'ab' })
    act(() => vi.advanceTimersByTime(299))
    expect(result.current).toBe('a')

    act(() => vi.advanceTimersByTime(1))
    expect(result.current).toBe('ab')
  })

  it('passes on only the last of several quick changes', () => {
    const seen: string[] = []
    const { rerender } = renderHook(
      ({ value }) => {
        const settled = useDebouncedValue(value, 300)
        if (seen.at(-1) !== settled) seen.push(settled)
        return settled
      },
      { initialProps: { value: 's' } },
    )

    for (const value of ['sq', 'squ', 'squa', 'squat']) {
      rerender({ value })
      act(() => vi.advanceTimersByTime(100))
    }
    act(() => vi.advanceTimersByTime(300))

    expect(seen).toEqual(['s', 'squat'])
  })
})
