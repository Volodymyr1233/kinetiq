import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import '@/lib/i18n'
import { CheckInCard } from './check-in-card'

function renderCard() {
  const onComplete = vi.fn()
  const onSkip = vi.fn()
  const view = render(
    <CheckInCard
      onComplete={onComplete}
      onSkip={onSkip}
      saving={false}
      error={null}
      focusTitle={false}
    />,
  )
  return { onComplete, onSkip, ...view }
}

const group = (name: string) => screen.getByRole('radiogroup', { name })
const value = (question: string, number: number) =>
  within(group(question)).getByRole('radio', { name: String(number) })

describe('CheckInCard', () => {
  it('asks three questions of 1 to 5, each with the meaning of its ends', () => {
    renderCard()

    expect(
      screen.getByRole('heading', { name: 'How do you feel today?' }),
    ).toBeInTheDocument()
    for (const [question, scale] of [
      ['Sleep', '1 bad · 5 great'],
      ['Soreness', '1 none · 5 very sore'],
      ['Stress', '1 calm · 5 high'],
    ]) {
      expect(group(question)).toHaveAccessibleDescription(scale)
      expect(within(group(question)).getAllByRole('radio')).toHaveLength(5)
    }
    expect(screen.getByText('Saves after the third answer')).toBeInTheDocument()
  })

  it('sends nothing for two answers, and the three answers once the third is chosen', async () => {
    const user = userEvent.setup()
    const { onComplete } = renderCard()

    await user.click(value('Stress', 1))
    await user.click(value('Sleep', 4))
    expect(onComplete).not.toHaveBeenCalled()
    expect(value('Sleep', 4)).toHaveAttribute('aria-checked', 'true')

    await user.click(value('Soreness', 2))
    expect(onComplete).toHaveBeenCalledTimes(1)
    expect(onComplete).toHaveBeenCalledWith({
      sleep: 4,
      soreness: 2,
      stress: 1,
    })
  })

  it('moves through the values with the arrow keys without choosing, and chooses with Space', async () => {
    const user = userEvent.setup()
    const { onComplete } = renderCard()
    await user.click(value('Sleep', 4))
    await user.click(value('Soreness', 2))

    await user.tab()
    expect(value('Stress', 1)).toHaveFocus()
    await user.keyboard('{ArrowRight}{ArrowRight}{ArrowRight}')
    expect(value('Stress', 4)).toHaveFocus()
    expect(
      within(group('Stress')).queryByRole('radio', { checked: true }),
    ).toBeNull()
    expect(onComplete).not.toHaveBeenCalled()

    await user.keyboard(' ')
    expect(value('Stress', 4)).toHaveAttribute('aria-checked', 'true')
    expect(onComplete).toHaveBeenCalledWith({
      sleep: 4,
      soreness: 2,
      stress: 4,
    })
  })

  it('keeps an answer when its value is chosen again', async () => {
    const user = userEvent.setup()
    renderCard()

    await user.click(value('Sleep', 3))
    await user.click(value('Sleep', 3))

    expect(value('Sleep', 3)).toHaveAttribute('aria-checked', 'true')
  })

  it('says it is saving, keeps the answers and the focus, and takes no choice while saving', async () => {
    const user = userEvent.setup()
    const { onComplete, rerender } = renderCard()
    await user.click(value('Sleep', 4))
    await user.click(value('Soreness', 2))
    await user.click(value('Stress', 1))

    rerender(
      <CheckInCard
        onComplete={onComplete}
        onSkip={vi.fn()}
        saving
        error={null}
        focusTitle={false}
      />,
    )

    expect(screen.getByRole('status')).toHaveTextContent('Saving…')
    expect(value('Stress', 1)).toHaveFocus()
    expect(value('Stress', 1)).toHaveAttribute('aria-checked', 'true')
    await user.click(value('Stress', 5))
    expect(value('Stress', 1)).toHaveAttribute('aria-checked', 'true')
    expect(onComplete).toHaveBeenCalledTimes(1)
  })

  it('keeps the answers after a failed save, and sends them again with Try again', async () => {
    const user = userEvent.setup()
    const { onComplete, rerender } = renderCard()
    await user.click(value('Sleep', 4))
    await user.click(value('Soreness', 2))
    await user.click(value('Stress', 1))

    rerender(
      <CheckInCard
        onComplete={onComplete}
        onSkip={vi.fn()}
        saving={false}
        error={new TypeError('Failed to fetch')}
        focusTitle={false}
      />,
    )
    expect(
      screen.getByText('Something went wrong. Try again.'),
    ).toBeInTheDocument()
    expect(value('Sleep', 4)).toHaveAttribute('aria-checked', 'true')

    await user.click(screen.getByRole('button', { name: 'Try again' }))
    expect(onComplete).toHaveBeenCalledTimes(2)
    expect(onComplete).toHaveBeenLastCalledWith({
      sleep: 4,
      soreness: 2,
      stress: 1,
    })
  })

  it('skips', async () => {
    const user = userEvent.setup()
    const { onSkip, onComplete } = renderCard()

    await user.click(screen.getByRole('button', { name: 'Skip' }))

    expect(onSkip).toHaveBeenCalledTimes(1)
    expect(onComplete).not.toHaveBeenCalled()
  })
})
