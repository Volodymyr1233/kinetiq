import { render, screen } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import i18n from '@/lib/i18n'
import { ChartContainer, ChartTooltipContent } from './chart'

beforeEach(() => {
  // jsdom measures every box as 0 × 0, and Recharts renders nothing in an empty container
  vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockReturnValue(
    new DOMRect(0, 0, 320, 200),
  )
})

afterEach(async () => {
  vi.restoreAllMocks()
  await i18n.changeLanguage('en')
})

function renderTooltip() {
  render(
    <ChartContainer config={{ rpe: { label: 'RPE' } }}>
      <ChartTooltipContent
        active
        payload={[
          {
            dataKey: 'rpe',
            name: 'rpe',
            value: 7.8,
            payload: {},
            graphicalItemId: 'rpe',
          },
        ]}
      />
    </ChartContainer>,
  )
}

// both languages, so one of them differs from the machine's own locale
it('formats tooltip numbers in Polish when Polish is active', async () => {
  await i18n.changeLanguage('pl')
  renderTooltip()

  expect(screen.getByText('7,8')).toBeInTheDocument()
})

it('formats tooltip numbers in English when English is active', async () => {
  await i18n.changeLanguage('en')
  renderTooltip()

  expect(screen.getByText('7.8')).toBeInTheDocument()
})
