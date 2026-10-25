import { describe, expect, it } from 'vitest'
import { fromKg, parseWeight, toKg } from './weight'

describe('parseWeight', () => {
  it.each([
    ['62.5', 62.5],
    ['62,5', 62.5],
    [' 137 ', 137],
    ['62.55', undefined],
    ['62kg', undefined],
    ['', undefined],
  ])('reads %j as %s', (text, weight) => {
    expect(parseWeight(text)).toBe(weight)
  })
})

describe('converting weights', () => {
  it('keeps kilograms as they are, to 0.1 kg', () => {
    expect(toKg(62.5, 'KG')).toBe(62.5)
    expect(fromKg(62.5, 'KG')).toBe(62.5)
  })

  it('turns pounds into kilograms and back to one decimal', () => {
    expect(toKg(137.8, 'LB')).toBe(62.5)
    expect(fromKg(62.5, 'LB')).toBe(137.8)
  })

  it('puts the body weight bounds of 30 and 250 kg at 66.1 and 551.2 lb', () => {
    expect(fromKg(30, 'LB')).toBe(66.1)
    expect(fromKg(250, 'LB')).toBe(551.2)
    expect(toKg(66.1, 'LB')).toBe(30)
    expect(toKg(66, 'LB')).toBe(29.9)
    expect(toKg(551.2, 'LB')).toBe(250)
    expect(toKg(551.3, 'LB')).toBe(250.1)
  })

  it('gives back every weight from 30.0 to 250.0 kg after showing it in pounds', () => {
    const changed: number[] = []
    for (let tenth = 300; tenth <= 2500; tenth++) {
      const kg = tenth / 10
      if (toKg(fromKg(kg, 'LB'), 'LB') !== kg) changed.push(kg)
    }

    expect(changed).toEqual([])
  })
})
