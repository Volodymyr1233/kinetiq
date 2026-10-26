import { z } from 'zod'
import { CATEGORIES, type Exercise } from './api/exercises'

export const MEASURES = [
  'REPS',
  'SECONDS',
] as const satisfies readonly Exercise['measure'][]

// a number field holds text; only a whole number in the range passes, as the backend's request records allow
function wholeNumber(min: number, max: number, message: string) {
  return z
    .string()
    .trim()
    .refine(
      (value) =>
        /^\d+$/.test(value) && Number(value) >= min && Number(value) <= max,
      message,
    )
    .transform(Number)
}

// same rules as the backend's CreateExerciseRequest; messages are translation keys, rendered with t()
export const exerciseSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, 'exercises.errors.name')
    .max(100, 'exercises.errors.nameLength'),
  category: z.enum(CATEGORIES),
  measure: z.enum(MEASURES),
  equipment: z.string().trim().max(50, 'exercises.errors.equipmentLength'),
  sets: wholeNumber(1, 20, 'exercises.errors.sets'),
  amount: wholeNumber(1, 3600, 'exercises.errors.amount'),
  restSeconds: wholeNumber(0, 600, 'exercises.errors.rest'),
  cues: z.string().trim().max(500, 'exercises.errors.cuesLength'),
})

export type ExerciseValues = z.input<typeof exerciseSchema>

/** The board's defaults for a new exercise. */
export const exerciseDefaults: ExerciseValues = {
  name: '',
  category: 'LEGS',
  measure: 'REPS',
  equipment: '',
  sets: '3',
  amount: '10',
  restSeconds: '90',
  cues: '',
}

export function toValues(exercise: Exercise): ExerciseValues {
  return {
    name: exercise.name,
    category: exercise.category,
    measure: exercise.measure,
    equipment: exercise.equipment,
    sets: String(exercise.sets),
    amount: String(exercise.amount),
    restSeconds: String(exercise.restSeconds),
    cues: exercise.cues,
  }
}
