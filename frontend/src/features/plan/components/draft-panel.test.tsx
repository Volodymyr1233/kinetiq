import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { json, problem, renderPage, stubApi } from '@/test/page'
import type { Plan } from '../api/plans'
import type { QuizAnswers } from '../api/quiz-answers'
import { DraftPanel } from './draft-panel'

const CLIENT = { id: '00000000-0000-4000-8000-000000000005', name: 'Ola' }
const ANSWERS: QuizAnswers = {
  goal: 'BUILD_MUSCLE',
  experience: 'YEARS_1_TO_2',
  level: 'INTERMEDIATE',
  age: 29,
  heightCm: 168,
  weightKg: 62.5,
  likes: ['WEIGHTS', 'YOGA_PILATES'],
  daysPerWeek: 3,
  notes: 'Left knee gets stiff\non deep squats <b>always</b>',
  submittedAt: '2026-11-05T10:00:00Z',
}
const BUILT: Plan = {
  status: 'DRAFT',
  goal: 'BUILD_MUSCLE',
  weeks: 12,
  version: 2,
  automaticDraft: 'BUILT',
  sessions: [],
}
const FAILED_MESSAGE =
  "Couldn't build this plan automatically. Try again or build it yourself."

function show(plan: Plan, answers: Response) {
  stubApi(() => answers)
  renderPage(
    <DraftPanel
      client={CLIENT}
      plan={plan}
      drafting={false}
      onBuildAgain={() => {}}
    />,
  )
}

async function value(label: string) {
  const term = await screen.findByText(label, { selector: 'dt' })
  return term.nextElementSibling
}

describe('DraftPanel', () => {
  it('shows what the client answered, in words', async () => {
    show(BUILT, json(ANSWERS))

    expect(
      screen.getByRole('heading', { name: "Ola's answers" }),
    ).toBeInTheDocument()
    expect(await value('Goal')).toHaveTextContent('Build muscle')
    expect(await value('Training for')).toHaveTextContent('1–2 years')
    expect(await value('Level')).toHaveTextContent('Intermediate')
    expect(await value('Age')).toHaveTextContent('29 years')
    expect(await value('Height')).toHaveTextContent('168 cm')
    expect(await value('Body weight')).toHaveTextContent('62.5 kg')
    expect(await value('Likes')).toHaveTextContent(
      'Weights and Yoga and Pilates',
    )
    expect(await value('Days a week')).toHaveTextContent('3')
  })

  it('shows the notes as the client typed them, as text', async () => {
    show(BUILT, json(ANSWERS))

    const notes = await screen.findByText(/Left knee gets stiff/)
    expect(notes.textContent).toBe(
      'Left knee gets stiff\non deep squats <b>always</b>',
    )
    expect(notes.querySelector('b')).toBeNull()
  })

  it('says the client wrote no notes', async () => {
    show(BUILT, json({ ...ANSWERS, notes: undefined }))

    expect(await screen.findByText('Ola wrote no notes.')).toBeInTheDocument()
  })

  it('says the answers are unavailable instead of failing', async () => {
    show(BUILT, problem(404, { status: 404, code: 'quiz.not-found' }))

    expect(
      await screen.findByText("Ola's quiz answers aren't available."),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Build again' }),
    ).toBeInTheDocument()
  })

  it('says drafting failed, with Build again, for a failed draft', async () => {
    show({ ...BUILT, automaticDraft: 'FAILED' }, json(ANSWERS))

    expect(await screen.findByRole('note')).toHaveTextContent(FAILED_MESSAGE)
    expect(
      screen.getByRole('button', { name: 'Build again' }),
    ).toBeInTheDocument()
  })

  it.each([
    ['to review', 'BUILT' as const],
    ['saved by the trainer', undefined],
  ])(
    'offers Build again with no failure, for a draft %s',
    async (_, automaticDraft) => {
      show({ ...BUILT, automaticDraft }, json(ANSWERS))

      expect(
        await screen.findByRole('button', { name: 'Build again' }),
      ).toBeInTheDocument()
      expect(screen.queryByText(FAILED_MESSAGE)).not.toBeInTheDocument()
    },
  )
})
