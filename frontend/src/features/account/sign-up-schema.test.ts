import { describe, expect, it } from 'vitest'
import { isValidPassword, joinSchema, signUpSchema } from './sign-up-schema'

const valid = {
  name: 'Marek Nowak',
  email: 'marek@example.com',
  password: 'Kinetiq-2026',
  adult: true,
}

function firstError(values: Partial<typeof valid>) {
  const result = signUpSchema.safeParse({ ...valid, ...values })
  return result.success ? undefined : result.error.issues[0].message
}

describe('isValidPassword', () => {
  const ofLength = (length: number) => 'Aa1!' + 'a'.repeat(length - 4)

  it.each([8, 128])('accepts %i characters', (length) => {
    expect(isValidPassword(ofLength(length))).toBe(true)
  })

  it.each([7, 129])('rejects %i characters', (length) => {
    expect(isValidPassword(ofLength(length))).toBe(false)
  })

  it.each(['kinetiq-2026', 'KINETIQ-2026', 'Kinetiq-dwa', 'Kinetiq2026'])(
    'rejects %s, which misses a character class',
    (password) => {
      expect(isValidPassword(password)).toBe(false)
    },
  )

  it.each(['Kinetiq 2026', 'Kinetiqą2026'])(
    'counts spaces and non-Latin letters as other characters in %s',
    (password) => {
      expect(isValidPassword(password)).toBe(true)
    },
  )
})

describe('signUpSchema', () => {
  it('trims name and email', () => {
    const result = signUpSchema.parse({
      ...valid,
      name: '  Marek Nowak ',
      email: ' Marek@Example.com ',
    })

    expect(result.name).toBe('Marek Nowak')
    expect(result.email).toBe('Marek@Example.com')
  })

  it.each([
    ['a blank name', { name: '   ' }, 'account.errors.name'],
    [
      'a name with a line break',
      { name: 'Marek\nNowak' },
      'account.errors.name',
    ],
    [
      'a 101-character name',
      { name: 'a'.repeat(101) },
      'account.errors.nameLength',
    ],
    ['a malformed email', { email: 'marek@example' }, 'account.errors.email'],
    [
      'a 255-character email',
      { email: 'a'.repeat(243) + '@example.com' },
      'account.errors.email',
    ],
    ['a weak password', { password: 'kinetiq-2026' }, 'account.passwordRules'],
    ['no adult confirmation', { adult: false }, 'account.errors.adult'],
  ])('rejects %s', (_, values, message) => {
    expect(firstError(values)).toBe(message)
  })

  it('accepts a 100-character name and a 254-character email', () => {
    expect(
      firstError({
        name: 'a'.repeat(100),
        email: 'a'.repeat(242) + '@example.com',
      }),
    ).toBeUndefined()
  })
})

describe('joinSchema', () => {
  const join = {
    name: 'Anna Kowalska',
    password: 'Kinetiq-2026',
    atLeastSixteen: true,
    healthDataConsent: true,
  }

  function messages(values: Partial<typeof join>) {
    const result = joinSchema.safeParse({ ...join, ...values })
    return result.success
      ? []
      : result.error.issues.map((issue) => issue.message)
  }

  it('accepts a trimmed name, a valid password and both boxes ticked', () => {
    expect(messages({ name: '  Anna Kowalska ' })).toEqual([])
  })

  it('asks for each box with its own message', () => {
    expect(
      messages({ atLeastSixteen: false, healthDataConsent: false }),
    ).toEqual([
      'account.errors.atLeastSixteen',
      'account.errors.healthDataConsent',
    ])
  })
})
