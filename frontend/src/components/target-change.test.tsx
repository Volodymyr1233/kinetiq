import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import '@/lib/i18n'
import { TargetChange } from './target-change'

describe('TargetChange', () => {
  it('shows a changed target struck through with the new one, and says both in words', () => {
    const { container } = render(
      <TargetChange planned="100 kg" today="90 kg" />,
    )

    expect(container.querySelector('s')).toHaveTextContent('100 kg')
    expect(screen.getByText('100 kg, today 90 kg')).toBeInTheDocument()
  })

  it('shows the value once when it did not change', () => {
    const { container } = render(<TargetChange planned="RPE 5" today="RPE 5" />)

    expect(container).toHaveTextContent(/^RPE 5$/)
    expect(container.querySelector('s')).toBeNull()
  })
})
