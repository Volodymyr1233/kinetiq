import { describe, expect, it } from 'vitest'
import { landingFor, safeNext } from './landing'

describe('safeNext', () => {
  it.each(['/trainer', '/trainer/exercises?from=mail', '/trainer?tab=open'])(
    'keeps %s for a trainer',
    (next) => {
      expect(safeNext(next, 'TRAINER')).toBe(next)
    },
  )

  it.each([
    null,
    '',
    'trainer/clients',
    '//evil.example/trainer',
    '/\\evil.example',
    'https://evil.example/trainer',
    '/app/today',
    '/trainers',
    '/sign-in',
  ])('drops %s for a trainer', (next) => {
    expect(safeNext(next, 'TRAINER')).toBeUndefined()
  })

  it('keeps the quiz for a client but not a look-alike path', () => {
    expect(safeNext('/app/quiz/goal', 'CLIENT')).toBe('/app/quiz/goal')
    expect(safeNext('/application', 'CLIENT')).toBeUndefined()
  })
})

describe('landingFor', () => {
  it('lands each role in its own area', () => {
    expect(landingFor('TRAINER')).toBe('/trainer')
    expect(landingFor('CLIENT')).toBe('/app')
  })
})
