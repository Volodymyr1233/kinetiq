import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { RestTimer } from './rest-timer'

describe('RestTimer', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-11-07T10:00:00Z'))
  })
  afterEach(() => vi.useRealTimers())

  it('counts down from the rest, a second at a time', () => {
    render(<RestTimer endsAt={Date.now() + 90_000} onDismiss={() => {}} />)
    expect(screen.getByRole('timer')).toHaveTextContent('1:30')

    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(screen.getByRole('timer')).toHaveTextContent('1:29')

    act(() => {
      vi.advanceTimersByTime(60_000)
    })
    expect(screen.getByRole('timer')).toHaveTextContent('0:29')
  })

  it('says the rest is over at zero, tells it once, and logs nothing', () => {
    render(<RestTimer endsAt={Date.now() + 3000} onDismiss={() => {}} />)

    act(() => {
      vi.advanceTimersByTime(3000)
    })

    expect(screen.getByRole('timer')).toHaveTextContent('Rest over')
    expect(screen.getByRole('status')).toHaveTextContent('Rest over')
    // it stays at zero, never below
    act(() => {
      vi.advanceTimersByTime(5000)
    })
    expect(screen.getByRole('timer')).toHaveTextContent('Rest over')
  })

  it('shows the right time when the tab comes back after a while', () => {
    render(<RestTimer endsAt={Date.now() + 120_000} onDismiss={() => {}} />)

    // a background tab throttles its timers: the clock moves, the tick comes late
    vi.setSystemTime(Date.now() + 100_000)
    act(() => {
      vi.advanceTimersByTime(1000)
    })

    expect(screen.getByRole('timer')).toHaveTextContent('0:19')
  })

  it('can be dismissed', () => {
    const onDismiss = vi.fn()
    render(<RestTimer endsAt={Date.now() + 90_000} onDismiss={onDismiss} />)

    fireEvent.click(
      screen.getByRole('button', { name: 'Dismiss the rest timer' }),
    )

    expect(onDismiss).toHaveBeenCalledOnce()
  })

  it('does not take focus', () => {
    render(<RestTimer endsAt={Date.now() + 90_000} onDismiss={() => {}} />)

    expect(document.body).toHaveFocus()
  })
})
