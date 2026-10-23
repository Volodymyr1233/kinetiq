import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it } from 'vitest'
import { NotesField } from './notes-field'

function Notes({ maxLength = 500 }: { maxLength?: number }) {
  const [value, setValue] = useState('')
  return (
    <NotesField
      id="notes"
      label="Notes for your trainer"
      placeholder="e.g. Left knee gets stiff on deep squats"
      value={value}
      maxLength={maxLength}
      onChange={setValue}
    />
  )
}

describe('NotesField', () => {
  it('is named by its label and described by the counter', () => {
    render(<Notes />)

    expect(
      screen.getByRole('textbox', { name: 'Notes for your trainer' }),
    ).toHaveAccessibleDescription('0 of 500 characters')
  })

  it('counts the characters as they are typed', async () => {
    const user = userEvent.setup()
    render(<Notes />)

    await user.type(screen.getByRole('textbox'), 'Left knee')

    expect(screen.getByText('9 of 500 characters')).toBeInTheDocument()
  })

  it('stops typing at the limit', async () => {
    const user = userEvent.setup()
    render(<Notes maxLength={5} />)

    await user.type(screen.getByRole('textbox'), 'abcdefgh')

    expect(screen.getByRole('textbox')).toHaveValue('abcde')
    expect(screen.getByText('5 of 5 characters')).toBeInTheDocument()
  })

  it('keeps the first characters of a longer paste', async () => {
    const user = userEvent.setup()
    render(<Notes maxLength={5} />)

    await user.click(screen.getByRole('textbox'))
    await user.paste('abcdefgh')

    expect(screen.getByRole('textbox')).toHaveValue('abcde')
  })
})
