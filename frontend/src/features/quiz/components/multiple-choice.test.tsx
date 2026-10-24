import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { MultipleChoice } from './multiple-choice'

const OPTIONS = [
  { value: 'WEIGHTS', title: 'Weights', description: 'Barbells, dumbbells' },
  { value: 'CARDIO', title: 'Cardio' },
  { value: 'HIIT', title: 'HIIT and circuits' },
] as const

function Step({
  onChange = () => {},
}: {
  onChange?: (value: string[]) => void
}) {
  const [value, setValue] = useState<string[]>([])
  return (
    <>
      <h1 id="heading">What do you like to train?</h1>
      <MultipleChoice
        labelledBy="heading"
        options={[...OPTIONS]}
        value={value}
        onChange={(next) => {
          setValue(next)
          onChange(next)
        }}
      />
    </>
  )
}

describe('MultipleChoice', () => {
  it('is a group named by the heading with one checkbox per option', () => {
    render(<Step />)

    expect(
      screen.getByRole('group', { name: 'What do you like to train?' }),
    ).toBeInTheDocument()
    expect(screen.getAllByRole('checkbox')).toHaveLength(3)
    expect(screen.getByText('Barbells, dumbbells')).toBeInTheDocument()
  })

  it('toggles options independently and keeps the screen order', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    render(<Step onChange={onChange} />)

    await user.click(screen.getByText('HIIT and circuits'))
    await user.click(screen.getByText('Weights'))

    expect(screen.getByRole('checkbox', { name: /Weights/ })).toBeChecked()
    expect(screen.getByRole('checkbox', { name: /HIIT/ })).toBeChecked()
    expect(screen.getByRole('checkbox', { name: /Cardio/ })).not.toBeChecked()
    expect(onChange).toHaveBeenLastCalledWith(['WEIGHTS', 'HIIT'])
  })

  it('reports an empty list when the last option is cleared', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    render(<Step onChange={onChange} />)

    await user.click(screen.getByText('Cardio'))
    await user.click(screen.getByText('Cardio'))

    expect(onChange).toHaveBeenLastCalledWith([])
  })
})
