import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { UnreadCount } from './unread-count'

function renderInLink(count: number) {
  render(
    <a href="/messages">
      Messages
      <UnreadCount count={count} />
    </a>,
  )
  return screen.getByRole('link')
}

describe('UnreadCount', () => {
  it('shows nothing at 0', () => {
    expect(renderInLink(0)).toHaveAccessibleName('Messages')
    expect(screen.queryByText('0')).not.toBeInTheDocument()
  })

  it('shows the count and makes it part of the announced name', () => {
    expect(renderInLink(5)).toHaveAccessibleName('Messages, 5 unread')
    expect(screen.getByText('5')).toBeInTheDocument()
  })

  it('shows 99+ above 99 and still announces the whole number', () => {
    expect(renderInLink(120)).toHaveAccessibleName('Messages, 120 unread')
    expect(screen.getByText('99+')).toBeInTheDocument()
  })
})
