import { createContext, useContext } from 'react'
import type { components } from '@/lib/api/schema'

export type WeightUnit =
  components['schemas']['CurrentAccountResponse']['units']

/** One pound in kilograms, exactly. */
export const KG_PER_LB = 0.45359237

const tenths = (value: number) => Math.round(value * 10) / 10

/** A weight typed with a dot or a comma and at most one decimal; anything else gives `undefined`. */
export function parseWeight(text: string): number | undefined {
  const trimmed = text.trim()
  return /^\d+([.,]\d)?$/.test(trimmed)
    ? Number(trimmed.replace(',', '.'))
    : undefined
}

/** A weight in the given unit as kilograms, kept to 0.1 kg as weights are stored. */
export function toKg(value: number, unit: WeightUnit): number {
  return tenths(unit === 'LB' ? value * KG_PER_LB : value)
}

/** A weight stored in kilograms, in the given unit to one decimal. */
export function fromKg(kg: number, unit: WeightUnit): number {
  return tenths(unit === 'LB' ? kg / KG_PER_LB : kg)
}

/** The signed-in person's unit; kilograms when nobody is signed in. */
export const WeightUnitContext = createContext<WeightUnit>('KG')

export const useWeightUnit = () => useContext(WeightUnitContext)
