import { describe, expect, it } from 'vitest'
import {
  exerciseDefaults,
  exerciseSchema,
  type ExerciseValues,
} from './exercise-schema'

const valid: ExerciseValues = { ...exerciseDefaults, name: 'Goblet squat' }

function firstError(values: Partial<ExerciseValues>) {
  const result = exerciseSchema.safeParse({ ...valid, ...values })
  return result.success ? undefined : result.error.issues[0].message
}

describe('exerciseSchema', () => {
  it('turns the defaults with a name into numbers and trimmed text', () => {
    expect(
      exerciseSchema.parse({ ...valid, name: '  Goblet squat ', sets: ' 4 ' }),
    ).toEqual({
      name: 'Goblet squat',
      category: 'LEGS',
      measure: 'REPS',
      equipment: '',
      sets: 4,
      amount: 10,
      restSeconds: 90,
      cues: '',
    })
  })

  it.each(['', '   '])('asks for a name when it is "%s"', (name) => {
    expect(firstError({ name })).toBe('exercises.errors.name')
  })

  it.each([
    ['name', 'a'.repeat(100), 'a'.repeat(101), 'exercises.errors.nameLength'],
    [
      'equipment',
      'a'.repeat(50),
      'a'.repeat(51),
      'exercises.errors.equipmentLength',
    ],
    ['cues', 'a'.repeat(500), 'a'.repeat(501), 'exercises.errors.cuesLength'],
  ] as const)('limits %s at its length', (field, longest, tooLong, message) => {
    expect(firstError({ [field]: longest })).toBeUndefined()
    expect(firstError({ [field]: tooLong })).toBe(message)
  })

  it.each([
    ['sets', '1', '20', ['0', '21'], 'exercises.errors.sets'],
    ['amount', '1', '3600', ['0', '3601'], 'exercises.errors.amount'],
    ['restSeconds', '0', '600', ['-1', '601'], 'exercises.errors.rest'],
  ] as const)(
    'accepts %s from %s to %s and nothing outside',
    (field, lowest, highest, outside, message) => {
      expect(firstError({ [field]: lowest })).toBeUndefined()
      expect(firstError({ [field]: highest })).toBeUndefined()
      for (const value of outside) {
        expect(firstError({ [field]: value })).toBe(message)
      }
    },
  )

  it.each(['', '3.5', 'three', '1e1'])('refuses "%s" as a number', (sets) => {
    expect(firstError({ sets })).toBe('exercises.errors.sets')
  })
})
