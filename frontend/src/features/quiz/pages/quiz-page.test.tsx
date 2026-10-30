import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import i18n from '@/lib/i18n'
import { WeightUnitContext, type WeightUnit } from '@/lib/weight'
import { json, stubApi } from '@/test/page'
import type { QuizDraft } from '../quiz-schema'
import { QuizPage } from './quiz-page'
import { ReviewPage } from './review-page'
import { WaitingPage } from './waiting-page'

const ANSWERED: QuizDraft['answers'] = {
  goal: 'BUILD_MUSCLE',
  experience: 'YEARS_1_TO_2',
  level: 'INTERMEDIATE',
  age: '29',
  height: '168',
  weight: '62,5',
  likes: ['CARDIO'],
  days: 3,
  notes: '',
}

function store(
  answers: Partial<QuizDraft['answers']>,
  reached: number,
  weightUnit?: WeightUnit,
) {
  sessionStorage.setItem(
    'kinetiq.quiz',
    JSON.stringify({
      answers: { ...ANSWERED, ...answers },
      weightUnit,
      reached,
    }),
  )
}

function openQuiz(...paths: string[]) {
  return openQuizIn('KG', ...paths)
}

function openQuizIn(unit: WeightUnit, ...paths: string[]) {
  return openQuizAs('NOT_SUBMITTED', unit, ...paths)
}

function openQuizAs(status: string, unit: WeightUnit, ...paths: string[]) {
  stubApi(() => json({ status }))
  // the same addresses as in app/router.ts; a feature does not import the app (frontend/CLAUDE.md)
  const router = createMemoryRouter(
    [
      { path: '/app/quiz/review', Component: ReviewPage },
      { path: '/app/quiz/waiting', Component: WaitingPage },
      { path: '/app/quiz/:step?', Component: QuizPage },
    ],
    { initialEntries: paths },
  )
  render(
    <QueryClientProvider client={new QueryClient()}>
      <WeightUnitContext value={unit}>
        <RouterProvider router={router} />
      </WeightUnitContext>
    </QueryClientProvider>,
  )
  return router
}

const heading = (name: string) => screen.findByRole('heading', { name })
const continueButton = () =>
  screen.getByRole('button', { name: /^(Continue|Skip and continue)$/ })

describe('QuizPage', () => {
  beforeEach(() => sessionStorage.clear())
  afterEach(() => i18n.changeLanguage('en'))

  it('starts on step 1 with nothing chosen, no Back and Continue disabled', async () => {
    const router = openQuiz('/app/quiz')

    expect(await heading('What do you train for?')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/app/quiz/1')
    expect(screen.getByText('Step 1 of 7')).toBeInTheDocument()
    expect(screen.getAllByRole('radio')).toHaveLength(5)
    screen.getAllByRole('radio').forEach((radio) => {
      expect(radio).not.toBeChecked()
    })
    expect(
      screen.queryByRole('button', { name: 'Back' }),
    ).not.toBeInTheDocument()
    expect(continueButton()).toBeDisabled()
  })

  it('moves forward and back keeping the answer, with focus on the new question', async () => {
    const user = userEvent.setup()
    openQuiz('/app/quiz/1')
    await heading('What do you train for?')

    await user.click(screen.getByText('Get stronger'))
    expect(continueButton()).toBeEnabled()
    await user.click(continueButton())

    const second = await heading('How long have you trained regularly?')
    expect(screen.getByText('Step 2 of 7')).toBeInTheDocument()
    expect(second).toHaveFocus()

    await user.click(screen.getByRole('button', { name: 'Back' }))

    expect(await heading('What do you train for?')).toHaveFocus()
    expect(screen.getByRole('radio', { name: /Get stronger/ })).toBeChecked()
  })

  it('shows a number message after the field is left, until it is valid, also after going back', async () => {
    const user = userEvent.setup()
    store({ age: '', height: '', weight: '' }, 4)
    openQuiz('/app/quiz/4')
    await heading('About you')
    const age = screen.getByRole('textbox', { name: 'Age' })

    await user.click(screen.getByRole('textbox', { name: 'Height' }))
    await user.tab()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()

    await user.type(age, '12')
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    await user.tab()
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Enter an age between 16 and 99.',
    )

    await user.clear(age)
    await user.type(age, '29')
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()

    await user.clear(age)
    await user.type(age, '5')
    await user.tab()
    await user.click(screen.getByRole('button', { name: 'Back' }))
    await heading('Which fits you best?')
    await user.click(continueButton())
    await heading('About you')
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Enter an age between 16 and 99.',
    )
    expect(screen.getByRole('textbox', { name: 'Age' })).toHaveValue('5')
  })

  it('enables Continue on step 4 only when all three numbers are valid', async () => {
    const user = userEvent.setup()
    store({ age: '', height: '', weight: '' }, 4)
    openQuiz('/app/quiz/4')
    await heading('About you')

    await user.type(screen.getByRole('textbox', { name: 'Age' }), '29')
    await user.type(screen.getByRole('textbox', { name: 'Height' }), '168')
    expect(continueButton()).toBeDisabled()

    await user.type(
      screen.getByRole('textbox', { name: 'Body weight' }),
      '62,5',
    )
    expect(continueButton()).toBeEnabled()
  })

  it('asks for the body weight in pounds when the unit is lb', async () => {
    const user = userEvent.setup()
    store({ weight: '' }, 4)
    openQuizIn('LB', '/app/quiz/4')
    await heading('About you')
    const weight = screen.getByRole('textbox', { name: 'Body weight' })

    expect(weight).toHaveAttribute('placeholder', 'e.g. 137.8')
    expect(weight).toHaveAccessibleDescription(expect.stringContaining('lb'))
    expect(
      screen.getByText('Switch to kg later in settings.'),
    ).toBeInTheDocument()

    await user.type(weight, '60')
    await user.tab()
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Enter a weight between 66.1 and 551.2 lb.',
    )

    await user.clear(weight)
    await user.type(weight, '137.8')
    expect(continueButton()).toBeEnabled()
  })

  it.each([
    ['in lb in kg', '137.8', 'LB', 'KG', '62.5'],
    ['in kg in lb', '62.5', 'KG', 'LB', '137.8'],
    [
      'with a comma in the other unit, keeping the comma',
      '62,5',
      'KG',
      'LB',
      '137,8',
    ],
    ['in the person’s unit as typed', '62,5', 'KG', 'KG', '62,5'],
    ['kept before units were kept as typed', '62,5', undefined, 'LB', '62,5'],
  ] as const)(
    'shows a weight typed %s',
    async (_, weight, typedIn, unit, shown) => {
      store({ weight }, 4, typedIn)
      openQuizIn(unit, '/app/quiz/4')
      await heading('About you')

      expect(screen.getByRole('textbox', { name: 'Body weight' })).toHaveValue(
        shown,
      )
    },
  )

  it('keeps a converted weight in the tab with the new unit', async () => {
    store({ weight: '137.8' }, 4, 'LB')
    openQuizIn('KG', '/app/quiz/4')
    await heading('About you')

    expect(
      JSON.parse(sessionStorage.getItem('kinetiq.quiz') ?? '{}'),
    ).toMatchObject({ answers: { weight: '62.5' }, weightUnit: 'KG' })
  })

  it('opens step 5 with nothing chosen and step 6 at 3 days', async () => {
    const user = userEvent.setup()
    store({ likes: [] }, 5)
    openQuiz('/app/quiz/5')
    await heading('What do you like to train?')

    screen.getAllByRole('checkbox').forEach((checkbox) => {
      expect(checkbox).not.toBeChecked()
    })
    expect(continueButton()).toBeDisabled()

    await user.click(screen.getByText('Cardio'))
    await user.click(continueButton())

    await heading('How many days a week can you train?')
    expect(screen.getByRole('status')).toHaveTextContent('3 days a week')
    expect(continueButton()).toBeEnabled()
  })

  it('offers to skip the notes until something is typed', async () => {
    const user = userEvent.setup()
    store({}, 7)
    openQuiz('/app/quiz/7')
    await heading('Anything your trainer should know?')

    expect(continueButton()).toHaveTextContent('Skip and continue')

    await user.type(screen.getByRole('textbox'), '   ')
    expect(continueButton()).toHaveTextContent('Skip and continue')

    await user.type(screen.getByRole('textbox'), 'Left knee')
    expect(continueButton()).toHaveTextContent('Continue')
  })

  it('opens the review after step 7, and Back returns to step 7 without a second review in history', async () => {
    const user = userEvent.setup()
    store({}, 7)
    const router = openQuiz('/app/quiz/6', '/app/quiz/7')
    await heading('Anything your trainer should know?')

    await user.click(continueButton())
    await heading('Check your answers')
    await user.click(screen.getByRole('button', { name: 'Back' }))
    await heading('Anything your trainer should know?')
    await router.navigate(-1)

    expect(
      await heading('How many days a week can you train?'),
    ).toBeInTheDocument()
  })

  it('returns to the review with Continue after Change, showing the new answer', async () => {
    const user = userEvent.setup()
    store({}, 7)
    openQuiz('/app/quiz/review')
    await heading('Check your answers')

    await user.click(screen.getByRole('link', { name: 'Change level' }))
    await heading('Which fits you best?')
    await user.click(screen.getByText('Advanced'))
    await user.click(continueButton())

    expect(await heading('Check your answers')).toHaveFocus()
    expect(screen.getByText('Advanced')).toBeInTheDocument()
  })

  it('returns to the review with Back after Change, also from step 1', async () => {
    const user = userEvent.setup()
    store({}, 7)
    openQuiz('/app/quiz/review')
    await heading('Check your answers')

    await user.click(screen.getByRole('link', { name: 'Change goal' }))
    await heading('What do you train for?')
    await user.click(screen.getByRole('button', { name: 'Back' }))

    expect(await heading('Check your answers')).toBeInTheDocument()
  })

  it('keeps Continue disabled while a step opened with Change is not valid', async () => {
    const user = userEvent.setup()
    store({}, 7)
    openQuiz('/app/quiz/review')
    await heading('Check your answers')

    await user.click(screen.getByRole('link', { name: 'Change about you' }))
    await heading('About you')
    await user.clear(screen.getByRole('textbox', { name: 'Age' }))

    expect(continueButton()).toBeDisabled()
  })

  it('keeps the step and every answer after a reload', async () => {
    const user = userEvent.setup()
    store({ likes: [] }, 5)
    openQuiz('/app/quiz/5')
    await heading('What do you like to train?')
    await user.click(screen.getByText('Weights'))

    cleanup()
    openQuiz('/app/quiz/5')

    await heading('What do you like to train?')
    expect(screen.getByRole('checkbox', { name: /Weights/ })).toBeChecked()
  })

  it.each([
    ['beyond the furthest step reached', 5, '/app/quiz/7', '/app/quiz/5'],
    ['to the notes past the unseen days step', 6, '/app/quiz/7', '/app/quiz/6'],
    ['with no step', 3, '/app/quiz', '/app/quiz/3'],
    ['with a step that does not exist', 2, '/app/quiz/8', '/app/quiz/2'],
  ])(
    'sends an address %s to the step that may open',
    async (_, reached, path, shown) => {
      store({}, reached)
      const router = openQuiz(path)

      await screen.findByRole('heading', { level: 1 })
      expect(router.state.location.pathname).toBe(shown)
    },
  )

  it('stops at an earlier step without a valid answer', async () => {
    store({ age: '' }, 6)
    const router = openQuiz('/app/quiz/6')

    expect(await heading('About you')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/app/quiz/4')
  })

  it.each([
    [
      'an unknown stored answer',
      JSON.stringify({ answers: { goal: 'DANCE' } }),
    ],
    ['broken stored data', 'not json'],
  ])('starts afresh from %s', async (_, stored) => {
    sessionStorage.setItem('kinetiq.quiz', stored)
    openQuiz('/app/quiz/3')

    await heading('What do you train for?')
    screen.getAllByRole('radio').forEach((radio) => {
      expect(radio).not.toBeChecked()
    })
  })

  it('speaks Polish when the UI does', async () => {
    await i18n.changeLanguage('pl')
    store({ weight: '' }, 4)
    openQuiz('/app/quiz/4')

    expect(await heading('O Tobie')).toBeInTheDocument()
    expect(screen.getByText('Krok 4 z 7')).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: 'Masa ciała' })).toHaveAttribute(
      'placeholder',
      'np. 62,5',
    )
  })

  it('sends a client who submitted from a step to the waiting state', async () => {
    const router = openQuizAs('WAITING_FOR_PLAN', 'KG', '/app/quiz/3')

    expect(
      await heading('Your trainer is reviewing your plan'),
    ).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/app/quiz/waiting')
  })
})
