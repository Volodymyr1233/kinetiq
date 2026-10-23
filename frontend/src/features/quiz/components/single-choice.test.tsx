import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { SingleChoice } from './single-choice'

const OPTIONS = [
  { value: 'BUILD_MUSCLE', title: 'Build muscle', description: 'More volume' },
  { value: 'GET_STRONGER', title: 'Get stronger' },
  { value: 'LOSE_FAT', title: 'Lose fat' },
] as const

function Step({ onChange = () => {} }: { onChange?: (value: string) => void }) {
  const [value, setValue] = useState<string>()
  return (
    <>
      <h1 id="heading">What do you train for?</h1>
      <SingleChoice
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

describe('SingleChoice', () => {
  it('is a radio group named by the heading with one radio per option', () => {
    render(<Step />)

    const group = screen.getByRole('radiogroup', {
      name: 'What do you train for?',
    })
    expect(group).toBeInTheDocument()
    expect(screen.getAllByRole('radio')).toHaveLength(3)
    expect(screen.getByText('More volume')).toBeInTheDocument()
    screen
      .getAllByRole('radio')
      .forEach((radio) => expect(radio).not.toBeChecked())
  })

  it('checks the chosen card and clears the previous choice', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    render(<Step onChange={onChange} />)

    await user.click(screen.getByText('Build muscle'))
    await user.click(screen.getByText('Lose fat'))

    expect(screen.getByRole('radio', { name: /Lose fat/ })).toBeChecked()
    expect(
      screen.getByRole('radio', { name: /Build muscle/ }),
    ).not.toBeChecked()
    expect(onChange).toHaveBeenLastCalledWith('LOSE_FAT')
  })
})
