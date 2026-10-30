import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import i18n from '@/lib/i18n'
import { WeightUnitContext, type WeightUnit } from '@/lib/weight'
import { json, problem, stubApi, type Reply } from '@/test/page'
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
  likes: ['WEIGHTS', 'YOGA_PILATES'],
  days: 3,
  notes: '',
}

function store(
  answers: Partial<QuizDraft['answers']> = {},
  reached = 7,
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

/** A server that keeps one client's status: a submit, or a refusal as already submitted, makes it waiting. */
function server(submit: () => Response | Promise<Response> = submitted) {
  let status = 'NOT_SUBMITTED'
  return async (request: Request) => {
    if (request.method !== 'POST') return json({ status })
    const response = await submit()
    if (response.ok || response.status === 409) status = 'WAITING_FOR_PLAN'
    return response
  }
}

const submitted = () => json({ status: 'WAITING_FOR_PLAN' }, 201)

function openReviewWith(reply: Reply, unit: WeightUnit = 'KG') {
  const requests = stubApi(reply)
  // the same addresses as in app/router.ts; a feature does not import the app (frontend/CLAUDE.md)
  const router = createMemoryRouter(
    [
      { path: '/app/quiz/review', Component: ReviewPage },
      { path: '/app/quiz/waiting', Component: WaitingPage },
      { path: '/app/quiz/:step?', Component: QuizPage },
    ],
    { initialEntries: ['/app/quiz/review'] },
  )
  render(
    <QueryClientProvider client={new QueryClient()}>
      <WeightUnitContext value={unit}>
        <RouterProvider router={router} />
      </WeightUnitContext>
    </QueryClientProvider>,
  )
  return { router, requests }
}

const openReview = (unit: WeightUnit = 'KG') =>
  openReviewWith(server(), unit).router

const reviewHeading = () =>
  screen.findByRole('heading', { name: 'Check your answers' })

const reviewingHeading = () =>
  screen.findByRole('heading', { name: 'Your trainer is reviewing your plan' })

const buildButton = () => screen.getByRole('button', { name: 'Build my plan' })

describe('ReviewPage', () => {
  beforeEach(() => sessionStorage.clear())
  afterEach(() => i18n.changeLanguage('en'))

  it('lists the seven answers in step order', async () => {
    store()
    openReview()

    expect(await reviewHeading()).toHaveFocus()
    expect(screen.getByText('Review')).toBeInTheDocument()
    expect(screen.getAllByRole('term').map((term) => term.textContent)).toEqual(
      [
        'Goal',
        'Training for',
        'Level',
        'About you',
        'Likes',
        'Days a week',
        'Notes',
      ],
    )
    for (const value of [
      'Build muscle',
      '1–2 years',
      'Intermediate',
      '29 years · 168 cm · 62.5 kg',
      'Weights, Yoga and Pilates',
      '3',
      'No notes',
    ]) {
      expect(screen.getByText(value)).toBeInTheDocument()
    }
  })

  it('shows the body weight in pounds when the unit is lb', async () => {
    store({ weight: '137.8' })
    openReview('LB')
    await reviewHeading()

    expect(screen.getByText('29 years · 168 cm · 137.8 lb')).toBeInTheDocument()
  })

  it('shows a weight typed in lb in kg once the unit is kg', async () => {
    store({ weight: '137.8' }, 7, 'LB')
    openReview('KG')
    await reviewHeading()

    expect(screen.getByText('29 years · 168 cm · 62.5 kg')).toBeInTheDocument()
  })

  it('lists likes in screen order, whatever order they were stored in', async () => {
    store({ likes: ['YOGA_PILATES', 'CARDIO', 'WEIGHTS'] })
    openReview()
    await reviewHeading()

    expect(
      screen.getByText('Weights, Cardio, Yoga and Pilates'),
    ).toBeInTheDocument()
  })

  it.each([
    ['empty', '', 'No notes'],
    ['only spaces', '   ', 'No notes'],
    ['typed', '  Left knee  ', 'Left knee'],
  ])('shows %s notes as %j', async (_, notes, shown) => {
    store({ notes })
    openReview()
    await reviewHeading()

    expect(screen.getByText(shown)).toBeInTheDocument()
  })

  it('speaks Polish with Polish numbers and plural forms', async () => {
    await i18n.changeLanguage('pl')
    store({ age: '22' })
    openReview()

    expect(
      await screen.findByRole('heading', { name: 'Sprawdź swoje odpowiedzi' }),
    ).toBeInTheDocument()
    expect(screen.getByText('22 lata · 168 cm · 62,5 kg')).toBeInTheDocument()
    expect(screen.getByText('Ciężary, Joga i pilates')).toBeInTheDocument()
  })

  it('says "lat" for an age that takes it in Polish', async () => {
    await i18n.changeLanguage('pl')
    store()
    openReview()

    expect(
      await screen.findByText('29 lat · 168 cm · 62,5 kg'),
    ).toBeInTheDocument()
  })

  it('names each Change link for its row and opens its step', async () => {
    const user = userEvent.setup()
    store()
    const router = openReview()
    await reviewHeading()

    const names = [
      'Change goal',
      'Change training for',
      'Change level',
      'Change about you',
      'Change likes',
      'Change days a week',
      'Change notes',
    ]
    names.forEach((name, index) => {
      expect(screen.getByRole('link', { name })).toHaveAttribute(
        'href',
        `/app/quiz/${index + 1}`,
      )
    })

    await user.click(screen.getByRole('link', { name: 'Change level' }))

    expect(
      await screen.findByRole('heading', { name: 'Which fits you best?' }),
    ).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/app/quiz/3')
    expect(screen.getByRole('radio', { name: /Intermediate/ })).toBeChecked()
  })

  it('opens step 7 with Back', async () => {
    const user = userEvent.setup()
    store()
    const router = openReview()
    await reviewHeading()

    await user.click(screen.getByRole('button', { name: 'Back' }))

    expect(
      await screen.findByRole('heading', {
        name: 'Anything your trainer should know?',
      }),
    ).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/app/quiz/7')
  })

  it.each([
    ['before step 7 is reached', {}, 6, '/app/quiz/6'],
    ['while an earlier answer is not valid', { age: '' }, 7, '/app/quiz/4'],
  ])(
    'sends the client to the step that may open %s',
    async (_, answers, reached, shown) => {
      store(answers, reached)
      const router = openReview()

      await screen.findByRole('heading', { level: 1 })
      expect(router.state.location.pathname).toBe(shown)
    },
  )

  it('sends the answers once, the weight in kilograms, and shows the reviewing state', async () => {
    const user = userEvent.setup()
    store({ weight: '137.8', notes: '  Left knee ' })
    const { router, requests } = openReviewWith(server(), 'LB')
    await reviewHeading()
    expect(
      screen.getByText('You can change them later with your trainer.'),
    ).toBeInTheDocument()

    await user.click(buildButton())

    expect(await reviewingHeading()).toHaveFocus()
    expect(
      screen.getByText(
        "Your trainer checks it before you see it. You get a notice here when it's ready.",
      ),
    ).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/app/quiz/waiting')
    const posts = requests.filter((request) => request.method === 'POST')
    expect(posts).toHaveLength(1)
    expect(await posts[0]!.json()).toEqual({
      goal: 'BUILD_MUSCLE',
      experience: 'YEARS_1_TO_2',
      level: 'INTERMEDIATE',
      age: 29,
      heightCm: 168,
      weightKg: 62.5,
      likes: ['WEIGHTS', 'YOGA_PILATES'],
      daysPerWeek: 3,
      notes: '  Left knee ',
    })
    expect(sessionStorage.getItem('kinetiq.quiz')).toBeNull()
  })

  it('shows the building state, and no review, while the answers are sent', async () => {
    const user = userEvent.setup()
    let answer!: () => void
    const answered = new Promise<void>((resolve) => (answer = resolve))
    store({ days: 4 })
    openReviewWith(
      server(async () => {
        await answered
        return submitted()
      }),
    )
    await reviewHeading()

    await user.click(buildButton())

    expect(
      await screen.findByRole('heading', {
        name: 'Building your plan from 7 answers…',
      }),
    ).toHaveFocus()
    expect(
      screen.getByText(
        'Sessions, exercises and starting weights for 4 days a week.',
      ),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Build my plan' }),
    ).not.toBeInTheDocument()

    answer()

    expect(await reviewingHeading()).toBeInTheDocument()
  })

  it('builds in Polish with Polish plural forms', async () => {
    const user = userEvent.setup()
    await i18n.changeLanguage('pl')
    store()
    openReviewWith(server(() => new Promise<Response>(() => {})))
    await screen.findByRole('heading', { name: 'Sprawdź swoje odpowiedzi' })

    await user.click(screen.getByRole('button', { name: 'Zbuduj mój plan' }))

    expect(
      await screen.findByText(
        'Sesje, ćwiczenia i ciężary startowe na 3 dni w tygodniu.',
      ),
    ).toBeInTheDocument()
  })

  it('shows the waiting state when the answers were stored before', async () => {
    const user = userEvent.setup()
    store()
    const { router } = openReviewWith(
      server(() =>
        problem(409, { status: 409, code: 'quiz.already-submitted' }),
      ),
    )
    await reviewHeading()

    await user.click(buildButton())

    expect(await reviewingHeading()).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/app/quiz/waiting')
    expect(sessionStorage.getItem('kinetiq.quiz')).toBeNull()
  })

  it('keeps the review with a message and Try again when sending fails', async () => {
    const user = userEvent.setup()
    let failures = 1
    store()
    openReviewWith(
      server(() =>
        failures-- > 0
          ? problem(500, { status: 500, code: 'internal.error' })
          : submitted(),
      ),
    )
    await reviewHeading()

    await user.click(buildButton())

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Something went wrong on our side. Try again in a moment.',
    )
    expect(sessionStorage.getItem('kinetiq.quiz')).not.toBeNull()

    await user.click(screen.getByRole('button', { name: 'Try again' }))

    expect(await reviewingHeading()).toBeInTheDocument()
  })

  it('sends a client who submitted to the waiting state', async () => {
    store()
    const { router } = openReviewWith(() =>
      json({ status: 'WAITING_FOR_PLAN' }),
    )

    expect(await reviewingHeading()).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/app/quiz/waiting')
  })
})
