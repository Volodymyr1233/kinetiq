import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import '@/lib/i18n'
import type { CheckIn } from '../api/check-in'
import { ReadinessCard } from './readiness-card'

const READY: CheckIn = {
  sleep: 4,
  soreness: 2,
  stress: 1,
  score: 83,
  band: 'READY',
  savedAt: '2026-11-09T07:00:00Z',
}

function renderCard(checkIn: CheckIn = READY, open = true) {
  const onChangeAnswers = vi.fn()
  render(
    <ReadinessCard
      checkIn={checkIn}
      open={open}
      onChangeAnswers={onChangeAnswers}
      focusHeading={false}
    />,
  )
  return onChangeAnswers
}

describe('ReadinessCard', () => {
  it('shows the score, the band, what it means today and the answers', () => {
    renderCard()

    expect(screen.getByText('Readiness')).toBeInTheDocument()
    expect(screen.getByText('83')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Ready' })).toBeInTheDocument()
    expect(screen.getByText('Train as planned.')).toBeInTheDocument()
    expect(
      screen.getByText('Sleep 4 · soreness 2 · stress 1'),
    ).toBeInTheDocument()
  })

  it.each([
    [
      'TAKE_IT_EASIER',
      50,
      'Take it easier',
      'Same plan. Stop a rep earlier if you need to.',
    ],
    [
      'RECOVERY_DAY',
      33,
      'Recovery day',
      'Same plan. Stop two reps earlier if you need to.',
    ],
  ] as const)('names the band %s and its line', (band, score, name, line) => {
    renderCard({ ...READY, band, score })

    expect(screen.getByRole('heading', { name })).toBeInTheDocument()
    expect(screen.getByText(line)).toBeInTheDocument()
  })

  it('offers to change the answers while they can be changed', async () => {
    const user = userEvent.setup()
    const onChangeAnswers = renderCard()

    await user.click(screen.getByRole('button', { name: 'Change answers' }))

    expect(onChangeAnswers).toHaveBeenCalledTimes(1)
    expect(
      screen.queryByText('Answers are fixed once training has started.'),
    ).toBeNull()
  })

  it('says the answers are fixed once training has started, with no way to change them', () => {
    renderCard(READY, false)

    expect(screen.queryByRole('button', { name: 'Change answers' })).toBeNull()
    expect(
      screen.getByText('Answers are fixed once training has started.'),
    ).toBeInTheDocument()
  })

  it("says today's targets are lighter while the lighter ones are in use", () => {
    renderCard({
      ...READY,
      band: 'RECOVERY_DAY',
      score: 33,
      adjustment: { inUse: true, causes: ['SLEEP'], entries: [] },
    })

    expect(screen.getByText('Today’s targets are lighter.')).toBeInTheDocument()
    expect(
      screen.queryByText('Same plan. Stop two reps earlier if you need to.'),
    ).toBeNull()
  })

  it('keeps the band line once the original targets are in use', () => {
    renderCard({
      ...READY,
      band: 'RECOVERY_DAY',
      score: 33,
      adjustment: { inUse: false, causes: ['SLEEP'], entries: [] },
    })

    expect(
      screen.getByText('Same plan. Stop two reps earlier if you need to.'),
    ).toBeInTheDocument()
  })

  it('says the targets are fixed too once training started on an adjusted day', () => {
    renderCard(
      {
        ...READY,
        band: 'RECOVERY_DAY',
        score: 33,
        adjustment: { inUse: true, causes: ['SLEEP'], entries: [] },
      },
      false,
    )

    expect(
      screen.getByText(
        'Answers and targets are fixed once training has started.',
      ),
    ).toBeInTheDocument()
  })
})
