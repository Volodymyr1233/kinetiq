import { describe, expect, it } from 'vitest'
import { errorMessageKey, isWorthRetrying } from './problem'

describe('errorMessageKey', () => {
  it('uses the translation of a known code', () => {
    expect(errorMessageKey({ code: 'internal.error' })).toBe(
      'errors.internal.error',
    )
  })

  it.each([
    ['an unknown code', { code: 'nobody.knows' }],
    ['a body without a code', { title: 'Bad Request' }],
    ['no body', undefined],
    ['a network failure', new TypeError('Failed to fetch')],
  ])('falls back to the generic message for %s', (_, error) => {
    expect(errorMessageKey(error)).toBe('errors.generic')
  })
})

describe('isWorthRetrying', () => {
  it.each([
    ['a 401 problem', { status: 401, code: 'request.unauthenticated' }, false],
    ['a 400 problem', { status: 400, code: 'validation.failed' }, false],
    ['a 500 problem', { status: 500, code: 'internal.error' }, true],
    ['a network failure', new TypeError('Failed to fetch'), true],
  ])('answers for %s', (_, error, expected) => {
    expect(isWorthRetrying(error)).toBe(expected)
  })
})
