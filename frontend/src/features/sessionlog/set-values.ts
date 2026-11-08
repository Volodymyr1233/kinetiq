import { fromKg, parseWeight, toKg, type WeightUnit } from '@/lib/weight'
import type { PlannedSet } from './api/session-log'

export const MIN_AMOUNT = 1
export const MAX_AMOUNT = 3600
// the smallest load above 0 at the stored precision of 0.1 kg
export const MIN_LOAD_KG = 0.1
export const MAX_LOAD_KG = 999.9
export const RPES = [5, 6, 7, 8, 9, 10] as const

/** One step of the load stepper: a plate pair's smallest jump in each unit. */
export const LOAD_STEP: Record<WeightUnit, number> = { KG: 2.5, LB: 5 }

/** What a client has entered for a set: text as typed, so a half-typed number is not lost. */
export type Values = { load: string; amount: string; rpe: number }

export type Problems = { load?: true; amount?: true }

const tenths = (value: number) => Math.round(value * 10) / 10

/** The values of a set as the plan asked for them, or as they were logged, in the person's unit. */
export function valuesOf(
  set: Pick<PlannedSet['target'], 'loadKg' | 'amount' | 'rpe'>,
  unit: WeightUnit,
): Values {
  return {
    load: set.loadKg === undefined ? '' : String(fromKg(set.loadKg, unit)),
    amount: String(set.amount),
    rpe: set.rpe,
  }
}

/** The values a set cannot be logged with. An empty load is fine: it means no added weight. */
export function problemsOf(values: Values, unit: WeightUnit): Problems {
  const problems: Problems = {}
  if (!/^\d+$/.test(values.amount.trim())) {
    problems.amount = true
  } else {
    const amount = Number(values.amount)
    if (amount < MIN_AMOUNT || amount > MAX_AMOUNT) problems.amount = true
  }
  if (values.load.trim() !== '') {
    const load = parseWeight(values.load)
    // after conversion, because the limits are in kilograms and a pound is not a tenth of a kilogram
    if (
      load === undefined ||
      toKg(load, unit) <= 0 ||
      toKg(load, unit) > MAX_LOAD_KG
    ) {
      problems.load = true
    }
  }
  return problems
}

/** The load one step up or down; going down to nothing gives an empty load, which is no added weight. */
export function steppedLoad(
  load: string,
  unit: WeightUnit,
  direction: 1 | -1,
): string {
  const current = parseWeight(load) ?? 0
  const next = tenths(current + direction * LOAD_STEP[unit])
  return next <= 0 ? '' : String(next)
}

/** The amount one up or down, within its limits. */
export function steppedAmount(amount: string, direction: 1 | -1): string {
  const current = /^\d+$/.test(amount.trim()) ? Number(amount) : 0
  return String(Math.min(MAX_AMOUNT, Math.max(MIN_AMOUNT, current + direction)))
}

/** The request body for a set; the load goes in kilograms, as it is stored. */
export function bodyOf(
  values: Values,
  unit: WeightUnit,
  place: { entryPosition: number; setNumber: number },
) {
  const load = values.load.trim() === '' ? undefined : parseWeight(values.load)
  return {
    entryPosition: place.entryPosition,
    setNumber: place.setNumber,
    loadKg: load === undefined ? undefined : toKg(load, unit),
    amount: Number(values.amount),
    rpe: values.rpe,
  }
}

/** Whether two entries mean the same set: the same load, amount and RPE, however the load was typed. */
export function sameValues(a: Values, b: Values, unit: WeightUnit) {
  const place = { entryPosition: 0, setNumber: 0 }
  const x = bodyOf(a, unit, place)
  const y = bodyOf(b, unit, place)
  return x.loadKg === y.loadKg && x.amount === y.amount && x.rpe === y.rpe
}
