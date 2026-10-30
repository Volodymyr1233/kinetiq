/// <reference types="node" />
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import { SERIES, type SeriesMeaning } from './chart-series'

// Vitest hands CSS imports back empty, so the tokens are read from the file (tests run from frontend/)
const css = readFileSync(resolve(process.cwd(), 'src/index.css'), 'utf8')

type Oklch = { l: number; c: number; h: number }

const MEANINGS = Object.keys(SERIES) as SeriesMeaning[]

// the theme blocks of index.css, by selector; each closes with a brace in the first column
function themeBlock(selector: string) {
  const block = css.match(
    new RegExp(`^${selector}\\s*\\{([\\s\\S]*?)^\\}`, 'm'),
  )
  if (!block) throw new Error(`no ${selector} block in index.css`)
  return block[1]
}

function token(block: string, name: string): Oklch {
  const found = block.match(new RegExp(`${name}:\\s*oklch\\(([^)]+)\\)`))
  if (!found) throw new Error(`no oklch value for ${name}`)
  const [l, c, h] = found[1].split('/')[0].trim().split(/\s+/).map(Number)
  return { l, c, h }
}

// OKLCH to linear sRGB, clamped to the gamut (Ottosson's matrices)
function linearRgb({ l, c, h }: Oklch) {
  const a = c * Math.cos((h * Math.PI) / 180)
  const b = c * Math.sin((h * Math.PI) / 180)
  const l_ = (l + 0.3963377774 * a + 0.2158037573 * b) ** 3
  const m_ = (l - 0.1055613458 * a - 0.0638541728 * b) ** 3
  const s_ = (l - 0.0894841775 * a - 1.291485548 * b) ** 3
  return [
    4.0767416621 * l_ - 3.3077115913 * m_ + 0.2309699292 * s_,
    -1.2684380046 * l_ + 2.6097574011 * m_ - 0.3413193965 * s_,
    -0.0041960863 * l_ - 0.7034186147 * m_ + 1.707614701 * s_,
  ].map((value) => Math.min(1, Math.max(0, value)))
}

function luminance(color: Oklch) {
  const [r, g, b] = linearRgb(color)
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

// WCAG 2 contrast ratio
function contrast(a: Oklch, b: Oklch) {
  const [high, low] = [luminance(a), luminance(b)].sort((x, y) => y - x)
  return (high + 0.05) / (low + 0.05)
}

// distance in OKLab, where about 0.02 is the smallest difference an eye notices
function distance(a: Oklch, b: Oklch) {
  const lab = ({ l, c, h }: Oklch) => [
    l,
    c * Math.cos((h * Math.PI) / 180),
    c * Math.sin((h * Math.PI) / 180),
  ]
  const [x, y] = [lab(a), lab(b)]
  return Math.hypot(x[0] - y[0], x[1] - y[1], x[2] - y[2])
}

// hue difference on the colour circle, in degrees
function hueGap(a: number, b: number) {
  const gap = Math.abs(a - b) % 360
  return Math.min(gap, 360 - gap)
}

describe.each([
  ['light', ':root'],
  ['dark', '\\.dark'],
])('chart colours in %s', (_theme, selector) => {
  const block = themeBlock(selector)
  const card = token(block, '--card')
  const colours = Object.fromEntries(
    MEANINGS.map((meaning) => [meaning, token(block, SERIES[meaning].token)]),
  ) as Record<SeriesMeaning, Oklch>

  it.each(MEANINGS)('%s reaches 3:1 against the card', (meaning) => {
    expect(contrast(colours[meaning], card)).toBeGreaterThanOrEqual(3)
  })

  it('tells the five meanings apart', () => {
    for (const [index, first] of MEANINGS.entries()) {
      for (const second of MEANINGS.slice(index + 1)) {
        expect(
          distance(colours[first], colours[second]),
          `${first} and ${second}`,
        ).toBeGreaterThan(0.1)
      }
    }
  })

  it('keeps RPE orange, never close to the red of an error', () => {
    const destructive = token(block, '--destructive')

    expect(colours.rpe.h).toBeGreaterThanOrEqual(40)
    expect(hueGap(colours.rpe.h, destructive.h)).toBeGreaterThanOrEqual(15)
  })
})
