import { render, screen } from '@testing-library/react'
import { afterEach, expect, it } from 'vitest'
import i18n from '@/lib/i18n'
import { Sheet, SheetContent, SheetTitle } from './sheet'

afterEach(async () => {
  await i18n.changeLanguage('en')
})

it('names the close button in the active language', async () => {
  await i18n.changeLanguage('pl')
  render(
    <Sheet open>
      <SheetContent aria-describedby={undefined}>
        <SheetTitle>Ustawienia</SheetTitle>
      </SheetContent>
    </Sheet>,
  )

  expect(screen.getByRole('button', { name: 'Zamknij' })).toBeInTheDocument()
})
