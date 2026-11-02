import { describe, expect, it } from 'vitest'
import type { Conversation } from './api/messaging'
import { filterConversations } from './conversation-filter'

function row(name: string, preview = 'hello'): Conversation {
  return {
    clientId: name,
    name,
    unreadCount: 0,
    lastMessage: { preview, sentByMe: false, sentAt: '2026-11-02T09:00:00Z' },
  }
}

const ROWS = [
  row('Piotr Zieliński'),
  row('Łukasz Kowal'),
  row('Anna Nowak', 'Piotr said zielinski'),
]
const names = (query: string) =>
  filterConversations(ROWS, query).map(({ name }) => name)

describe('filterConversations', () => {
  it('ignores Polish diacritics', () => {
    expect(names('zielinski')).toEqual(['Piotr Zieliński'])
    expect(names('lukasz')).toEqual(['Łukasz Kowal'])
    expect(names('ŁUKASZ')).toEqual(['Łukasz Kowal'])
  })

  it('matches the first name and the surname, in any case', () => {
    expect(names('ANNA')).toEqual(['Anna Nowak'])
    expect(names('nowak')).toEqual(['Anna Nowak'])
  })

  it('never looks at the text of a message', () => {
    expect(names('said')).toEqual([])
  })

  it('keeps every row and their order for a blank query', () => {
    expect(names('   ')).toEqual(ROWS.map(({ name }) => name))
  })
})
