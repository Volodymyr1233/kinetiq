import { z } from 'zod'
import { parseWeight, toKg, type WeightUnit } from '@/lib/weight'

// option codes are what is stored and sent; labels come from the locale files (data-model.md)
export const GOALS = [
  'BUILD_MUSCLE',
  'GET_STRONGER',
  'LOSE_FAT',
  'MOVE_BETTER',
  'GENERAL_FITNESS',
] as const
export const EXPERIENCES = [
  'LESS_THAN_3_MONTHS',
  'MONTHS_3_TO_6',
  'MONTHS_6_TO_12',
  'YEARS_1_TO_2',
  'YEARS_2_TO_5',
  'YEARS_5_PLUS',
] as const
export const LEVELS = [
  'BEGINNER',
  'INTERMEDIATE',
  'ADVANCED',
  'ATHLETE',
] as const
export const LIKES = [
  'WEIGHTS',
  'CARDIO',
  'HIIT',
  'YOGA_PILATES',
  'BODYWEIGHT',
] as const

export const STEP_COUNT = 7
export const DAYS_MIN = 2
export const DAYS_MAX = 6
export const NOTES_MAX_LENGTH = 500

export function parseWholeNumber(text: string): number | undefined {
  const trimmed = text.trim()
  return /^\d+$/.test(trimmed) ? Number(trimmed) : undefined
}

const within = (value: number | undefined, min: number, max: number) =>
  value !== undefined && value >= min && value <= max

// messages are translation keys, rendered with t(); task #55 checks the same rules
const rules = {
  goal: z.enum(GOALS),
  experience: z.enum(EXPERIENCES),
  level: z.enum(LEVELS),
  age: z
    .string()
    .refine(
      (text) => within(parseWholeNumber(text), 16, 99),
      'quiz.about.age.error',
    ),
  height: z
    .string()
    .refine(
      (text) => within(parseWholeNumber(text), 120, 230),
      'quiz.about.height.error',
    ),
  likes: z
    .array(z.enum(LIKES))
    .min(1)
    .refine((likes) => new Set(likes).size === likes.length),
  days: z.number().int().min(DAYS_MIN).max(DAYS_MAX),
  // lengths in UTF-16 units, as Java's String.length() on the server (research R8)
  notes: z.string().max(NOTES_MAX_LENGTH),
}

// the weight is typed in the person's unit; its bounds are in kilograms, as stored
const rulesFor = (unit: WeightUnit) => ({
  ...rules,
  weight: z.string().refine((text) => {
    const weight = parseWeight(text)
    return weight !== undefined && within(toKg(weight, unit), 30, 250)
  }, `quiz.about.weight.${unit}.error`),
})

export const answersSchemaFor = (unit: WeightUnit) => z.object(rulesFor(unit))

export type QuizAnswers = z.infer<ReturnType<typeof answersSchemaFor>>

/** What the browser tab keeps: answers as typed, and the furthest step reached with Continue. */
export type QuizDraft = {
  answers: Partial<Pick<QuizAnswers, 'goal' | 'experience' | 'level'>> &
    Omit<QuizAnswers, 'goal' | 'experience' | 'level'>
  reached: number
}

export const emptyDraft: QuizDraft = {
  answers: {
    age: '',
    height: '',
    weight: '',
    likes: [],
    days: 3,
    notes: '',
  },
  reached: 1,
}

// every field falls back on its own, so one bad value never costs the other answers
export const draftSchema = z
  .object({
    answers: z
      .object({
        goal: rules.goal.optional().catch(undefined),
        experience: rules.experience.optional().catch(undefined),
        level: rules.level.optional().catch(undefined),
        age: rules.age.catch(emptyDraft.answers.age),
        height: rules.height.catch(emptyDraft.answers.height),
        // only the form is checked: whether it is in range depends on the person's unit
        weight: z
          .string()
          .refine((text) => text === '' || parseWeight(text) !== undefined)
          .catch(emptyDraft.answers.weight),
        likes: rules.likes.catch(emptyDraft.answers.likes),
        days: rules.days.catch(emptyDraft.answers.days),
        notes: rules.notes.catch(emptyDraft.answers.notes),
      })
      .catch(emptyDraft.answers),
    reached: z.number().int().min(1).max(STEP_COUNT).catch(emptyDraft.reached),
  })
  .catch(emptyDraft)

const STEP_FIELDS: Record<number, (keyof QuizAnswers)[]> = {
  1: ['goal'],
  2: ['experience'],
  3: ['level'],
  4: ['age', 'height', 'weight'],
  5: ['likes'],
  6: ['days'],
  7: ['notes'],
}

export function isStepValid(
  step: number,
  answers: Partial<Record<keyof QuizAnswers, unknown>>,
  unit: WeightUnit = 'KG',
): boolean {
  const stepRules = rulesFor(unit)
  return (STEP_FIELDS[step] ?? []).every(
    (field) => stepRules[field].safeParse(answers[field]).success,
  )
}

/** The step the client may open: the requested one, no further than reached, nor past an unanswered step. */
export function allowedStep(
  requested: string | undefined,
  draft: {
    answers: Partial<Record<keyof QuizAnswers, unknown>>
    reached: number
  },
  unit: WeightUnit = 'KG',
): number {
  const asked = /^[1-7]$/.test(requested ?? '')
    ? Number(requested)
    : draft.reached
  const furthest = Math.min(asked, draft.reached)
  for (let step = 1; step < furthest; step++) {
    if (!isStepValid(step, draft.answers, unit)) return step
  }
  return furthest
}

/** Whether the review may open: step 7 reached and every earlier step answered. */
export function isComplete(draft: QuizDraft, unit: WeightUnit = 'KG'): boolean {
  return allowedStep(String(STEP_COUNT), draft, unit) === STEP_COUNT
}
