import { describe, expect, it } from 'vitest'
import {
  allowedStep,
  draftSchema,
  emptyDraft,
  isComplete,
  isStepValid,
  type QuizDraft,
} from './quiz-schema'

const VALID: QuizDraft['answers'] = {
  goal: 'BUILD_MUSCLE',
  experience: 'YEARS_1_TO_2',
  level: 'INTERMEDIATE',
  age: '29',
  height: '168',
  weight: '62.5',
  likes: ['WEIGHTS'],
  days: 3,
  notes: '',
}

const about = (fields: Partial<QuizDraft['answers']>) =>
  isStepValid(4, { ...VALID, ...fields })

describe('quiz rules', () => {
  it.each([
    ['15', false],
    ['16', true],
    ['99', true],
    ['100', false],
    ['29.5', false],
    [' 29 ', true],
    ['', false],
  ])('age %j is valid: %s', (age, valid) => {
    expect(about({ age })).toBe(valid)
  })

  it.each([
    ['119', false],
    ['120', true],
    ['230', true],
    ['231', false],
  ])('height %j is valid: %s', (height, valid) => {
    expect(about({ height })).toBe(valid)
  })

  it.each([
    ['29.9', false],
    ['30', true],
    ['62,5', true],
    ['62.5', true],
    ['62.55', false],
    ['250', true],
    ['250.1', false],
    [' 62 ', true],
    ['62kg', false],
  ])('weight %j is valid: %s', (weight, valid) => {
    expect(about({ weight })).toBe(valid)
  })

  it.each([
    ['66.0', false],
    ['66.1', true],
    ['137,8', true],
    ['300', true],
    ['551.2', true],
    ['551.3', false],
  ])('weight %j in pounds is valid: %s', (weight, valid) => {
    expect(isStepValid(4, { ...VALID, weight }, 'LB')).toBe(valid)
  })

  it('checks a weight against kilograms when the unit is kg', () => {
    expect(isStepValid(4, { ...VALID, weight: '300' }, 'KG')).toBe(false)
  })

  it.each([
    [[], false],
    [['CARDIO'], true],
    [['CARDIO', 'CARDIO'], false],
    [['DANCING'], false],
  ])('likes %j are valid: %s', (likes, valid) => {
    expect(isStepValid(5, { ...VALID, likes })).toBe(valid)
  })

  it.each([
    [1, false],
    [2, true],
    [6, true],
    [7, false],
  ])('%i days a week are valid: %s', (days, valid) => {
    expect(isStepValid(6, { ...VALID, days })).toBe(valid)
  })

  it('takes notes up to 500 units, spaces only included', () => {
    expect(isStepValid(7, { ...VALID, notes: 'x'.repeat(500) })).toBe(true)
    expect(isStepValid(7, { ...VALID, notes: 'x'.repeat(501) })).toBe(false)
    expect(isStepValid(7, { ...VALID, notes: '   ' })).toBe(true)
  })

  it('needs a choice on the single-choice steps', () => {
    expect(isStepValid(1, { ...VALID, goal: undefined })).toBe(false)
    expect(isStepValid(1, VALID)).toBe(true)
  })
})

describe('loading a stored draft', () => {
  it('keeps every valid answer', () => {
    expect(draftSchema.parse({ answers: VALID, reached: 7 })).toEqual({
      answers: VALID,
      reached: 7,
    })
  })

  it.each([
    ['an unknown goal code', { goal: 'DANCE' }, { goal: undefined }],
    ['likes with an unknown code', { likes: ['WEIGHTS', 'X'] }, { likes: [] }],
    ['an age out of range', { age: '12' }, { age: '' }],
    ['days that are not a number', { days: 'three' }, { days: 3 }],
  ])('drops %s and nothing else', (_, stored, expected) => {
    const draft = draftSchema.parse({
      answers: { ...VALID, ...stored },
      reached: 5,
    })

    expect(draft).toEqual({ answers: { ...VALID, ...expected }, reached: 5 })
  })

  it.each([null, 'text', 42, { reached: 99 }])(
    'turns %j into an empty draft',
    (stored) => {
      expect(draftSchema.parse(stored)).toEqual(emptyDraft)
    },
  )
})

describe('which step may open', () => {
  const draft = (reached: number, answers = VALID) => ({ answers, reached })

  it('opens the requested step up to the furthest one reached', () => {
    expect(allowedStep('3', draft(5))).toBe(3)
    expect(allowedStep('7', draft(5))).toBe(5)
  })

  it('stops at the first earlier step without a valid answer', () => {
    expect(allowedStep('5', draft(5, { ...VALID, age: '' }))).toBe(4)
  })

  it.each(['review', '0', '8', undefined])(
    'treats %j as the furthest step reached',
    (requested) => {
      expect(allowedStep(requested, draft(6))).toBe(6)
    },
  )
})

describe('whether the review may open', () => {
  it('opens once step 7 is reached with every answer valid', () => {
    expect(isComplete({ answers: VALID, reached: 7 })).toBe(true)
  })

  it('stays closed before step 7 is reached', () => {
    expect(isComplete({ answers: VALID, reached: 6 })).toBe(false)
  })

  it('stays closed while an earlier step has no valid answer', () => {
    expect(isComplete({ answers: { ...VALID, age: '' }, reached: 7 })).toBe(
      false,
    )
  })
})
