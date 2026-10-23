import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { NumberField } from './number-field'

function Age({
  message,
  note,
  onChange = () => {},
  onBlur = () => {},
}: {
  message?: string
  note?: string
  onChange?: (value: string) => void
  onBlur?: () => void
}) {
  const [value, setValue] = useState('')
  return (
    <NumberField
      id="age"
      label="Age"
      unit="years"
      placeholder="e.g. 29"
      inputMode="numeric"
      value={value}
      onChange={(next) => {
        setValue(next)
        onChange(next)
      }}
      onBlur={onBlur}
      message={message}
      note={note}
    />
  )
}

describe('NumberField', () => {
  it('is named by its label and described by its unit', () => {
    render(<Age />)

    const field = screen.getByRole('textbox', { name: 'Age' })
    expect(field).toHaveAccessibleDescription('years')
    expect(field).toHaveAttribute('inputmode', 'numeric')
    expect(field).not.toHaveAttribute('aria-invalid')
  })

  it('shows a given message, linked to the field and marked invalid', () => {
    render(<Age message="Enter an age between 16 and 99." note="unused" />)

    const field = screen.getByRole('textbox', { name: 'Age' })
    expect(field).toHaveAttribute('aria-invalid', 'true')
    expect(field).toHaveAccessibleDescription(
      'years Enter an age between 16 and 99.',
    )
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Enter an age between 16 and 99.',
    )
    expect(screen.queryByText('unused')).not.toBeInTheDocument()
  })

  it('shows the note when there is no message', () => {
    render(<Age note="Switch to lb later in settings." />)

    expect(
      screen.getByRole('textbox', { name: 'Age' }),
    ).toHaveAccessibleDescription('years Switch to lb later in settings.')
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('passes the text as typed and reports leaving the field', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    const onBlur = vi.fn()
    render(<Age onChange={onChange} onBlur={onBlur} />)

    await user.type(screen.getByRole('textbox', { name: 'Age' }), '62,5')
    await user.tab()

    expect(onChange).toHaveBeenLastCalledWith('62,5')
    expect(onBlur).toHaveBeenCalledOnce()
  })
})
