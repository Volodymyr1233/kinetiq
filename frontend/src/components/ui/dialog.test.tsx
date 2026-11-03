import { render, screen } from '@testing-library/react'
import { afterEach, expect, it } from 'vitest'
import i18n from '@/lib/i18n'
import { Dialog, DialogContent, DialogTitle } from './dialog'

afterEach(async () => {
  await i18n.changeLanguage('en')
})

it('names the close button in the active language', async () => {
  await i18n.changeLanguage('pl')
  render(
    <Dialog open>
      <DialogContent aria-describedby={undefined}>
        <DialogTitle>Zaproś klienta</DialogTitle>
      </DialogContent>
    </Dialog>,
  )

  expect(screen.getByRole('button', { name: 'Zamknij' })).toBeInTheDocument()
})
