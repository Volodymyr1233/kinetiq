import { describe, expect, it } from 'vitest'
import { errorMessageKey } from './problem'

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
