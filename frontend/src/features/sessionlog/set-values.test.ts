import { describe, expect, it } from 'vitest'
import {
  bodyOf,
  problemsOf,
  steppedAmount,
  steppedLoad,
  valuesOf,
} from './set-values'

const place = { entryPosition: 1, setNumber: 2 }

describe('valuesOf', () => {
  it("gives the targets in the person's unit, and no load as empty", () => {
    expect(valuesOf({ loadKg: 100, amount: 5, rpe: 8 }, 'KG')).toEqual({
      load: '100',
      amount: '5',
      rpe: 8,
    })
    expect(valuesOf({ loadKg: 100, amount: 5, rpe: 8 }, 'LB').load).toBe(
      '220.5',
    )
    expect(valuesOf({ amount: 45, rpe: 7 }, 'KG').load).toBe('')
  })
})

describe('problemsOf', () => {
  const ok = { load: '100', amount: '5', rpe: 8 }

  it('accepts a set as planned, an empty load and the limits', () => {
    expect(problemsOf(ok, 'KG')).toEqual({})
    expect(problemsOf({ ...ok, load: '' }, 'KG')).toEqual({})
    expect(problemsOf({ ...ok, amount: '1' }, 'KG')).toEqual({})
    expect(problemsOf({ ...ok, amount: '3600', load: '999.9' }, 'KG')).toEqual(
      {},
    )
  })

  it('refuses no repetitions, an empty amount, a fraction and too many', () => {
    for (const amount of ['0', '', ' ', '2.5', 'many', '3601', '-1']) {
      expect(problemsOf({ ...ok, amount }, 'KG')).toEqual({ amount: true })
    }
  })

  it('refuses a load of 0, a negative, a text and one above 999.9 kg', () => {
    for (const load of ['0', '0.0', '-5', 'heavy', '1000', '999.95']) {
      expect(problemsOf({ ...ok, load }, 'KG')).toEqual({ load: true })
    }
  })

  it('counts the limit of a load in pounds in kilograms, and accepts a comma', () => {
    expect(problemsOf({ ...ok, load: '2204.4' }, 'LB')).toEqual({})
    expect(problemsOf({ ...ok, load: '2204.7' }, 'LB')).toEqual({ load: true })
    expect(problemsOf({ ...ok, load: '0.1' }, 'LB')).toEqual({ load: true })
    expect(problemsOf({ ...ok, load: '102,5' }, 'KG')).toEqual({})
  })
})

describe('steppedLoad', () => {
  it('moves by 2.5 kg or 5 lb', () => {
    expect(steppedLoad('100', 'KG', 1)).toBe('102.5')
    expect(steppedLoad('100', 'KG', -1)).toBe('97.5')
    expect(steppedLoad('100', 'LB', 1)).toBe('105')
    expect(steppedLoad('', 'KG', 1)).toBe('2.5')
  })

  it('goes down to no load, never to a load of 0 or below', () => {
    expect(steppedLoad('2.5', 'KG', -1)).toBe('')
    expect(steppedLoad('', 'KG', -1)).toBe('')
    expect(steppedLoad('1', 'KG', -1)).toBe('')
  })
})

describe('steppedAmount', () => {
  it('moves by one within 1 to 3600', () => {
    expect(steppedAmount('5', 1)).toBe('6')
    expect(steppedAmount('5', -1)).toBe('4')
    expect(steppedAmount('1', -1)).toBe('1')
    expect(steppedAmount('3600', 1)).toBe('3600')
    expect(steppedAmount('', 1)).toBe('1')
  })
})

describe('bodyOf', () => {
  it('sends the load in kilograms from a load typed in pounds', () => {
    const body = bodyOf({ load: '225', amount: '5', rpe: 9 }, 'LB', place)

    expect(body).toEqual({
      entryPosition: 1,
      setNumber: 2,
      loadKg: 102.1,
      amount: 5,
      rpe: 9,
    })
  })

  it('leaves the load out when it is empty', () => {
    const body = bodyOf({ load: '', amount: '45', rpe: 7 }, 'KG', place)

    expect(body.loadKg).toBeUndefined()
    expect(body.amount).toBe(45)
  })
})
