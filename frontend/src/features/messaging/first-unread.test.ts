import { describe, expect, it } from 'vitest'
import type { Message } from './api/messaging'
import { firstUnread } from './first-unread'

function message(id: string, mine: boolean): Message {
  return { id, text: id, mine, sentAt: '2026-11-02T09:00:00Z' }
}

const MESSAGES = [
  message('theirs 1', false),
  message('mine 1', true),
  message('theirs 2', false),
  message('theirs 3', false),
  message('mine 2', true),
]

describe('firstUnread', () => {
  it('finds nothing when nothing is unread', () => {
    expect(firstUnread(MESSAGES, 0)).toEqual({ id: undefined, complete: true })
  })

  it('counts only the messages of the other person', () => {
    expect(firstUnread(MESSAGES, 1).id).toBe('theirs 3')
    expect(firstUnread(MESSAGES, 2).id).toBe('theirs 2')
    expect(firstUnread(MESSAGES, 3)).toEqual({
      id: 'theirs 1',
      complete: true,
    })
  })

  it('asks for earlier pages when the loaded ones hold too few', () => {
    expect(firstUnread(MESSAGES, 4)).toEqual({
      id: 'theirs 1',
      complete: false,
    })
  })
})
