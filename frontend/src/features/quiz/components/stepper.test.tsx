import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it } from 'vitest'
import { Stepper } from './stepper'

function Days({ start }: { start: number }) {
  const [value, setValue] = useState(start)
  return (
    <>
      <h1 id="heading">How many days a week can you train?</h1>
      <Stepper
        labelledBy="heading"
        value={value}
        min={2}
        max={6}
        unit="days a week"
        decreaseLabel="Fewer days"
        increaseLabel="More days"
        onChange={setValue}
      />
    </>
  )
}

describe('Stepper', () => {
  it('is a group named by the heading with labelled buttons and a live number', () => {
    render(<Days start={3} />)

    expect(
      screen.getByRole('group', {
        name: 'How many days a week can you train?',
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Fewer days' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'More days' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('3 days a week')
  })

  it('changes the number by one', async () => {
    const user = userEvent.setup()
    render(<Days start={3} />)

    await user.click(screen.getByRole('button', { name: 'More days' }))
    expect(screen.getByRole('status')).toHaveTextContent('4 days a week')

    await user.click(screen.getByRole('button', { name: 'Fewer days' }))
    await user.click(screen.getByRole('button', { name: 'Fewer days' }))
    expect(screen.getByRole('status')).toHaveTextContent('2 days a week')
  })

  it('stops at the lower bound and keeps focus on the button', async () => {
    const user = userEvent.setup()
    render(<Days start={3} />)
    const fewer = screen.getByRole('button', { name: 'Fewer days' })

    await user.click(fewer)
    await user.keyboard('{Enter}')

    expect(screen.getByRole('status')).toHaveTextContent('2 days a week')
    expect(fewer).toHaveAttribute('aria-disabled', 'true')
    expect(fewer).toHaveFocus()
  })

  it('stops at the upper bound', async () => {
    const user = userEvent.setup()
    render(<Days start={6} />)
    const more = screen.getByRole('button', { name: 'More days' })

    await user.click(more)

    expect(screen.getByRole('status')).toHaveTextContent('6 days a week')
    expect(more).toHaveAttribute('aria-disabled', 'true')
    expect(screen.getByRole('button', { name: 'Fewer days' })).toHaveAttribute(
      'aria-disabled',
      'false',
    )
  })
})
