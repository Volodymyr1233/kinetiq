import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, describe, expect, it } from 'vitest'
import i18n from '@/lib/i18n'
import { WeightUnitContext } from '@/lib/weight'
import { json, problem, stubApi, type Reply } from '@/test/page'
import { QuizPage } from './quiz-page'
import { WaitingPage } from './waiting-page'

function openWaiting(reply: Reply) {
  stubApi(reply)
  // the same addresses as in app/router.ts; a feature does not import the app (frontend/CLAUDE.md)
  const router = createMemoryRouter(
    [
      { path: '/app/quiz/waiting', Component: WaitingPage },
      { path: '/app/quiz/:step?', Component: QuizPage },
      { path: '/app', Component: () => <h1>Today</h1> },
    ],
    { initialEntries: ['/app/quiz/waiting'] },
  )
  render(
    <QueryClientProvider client={new QueryClient()}>
      <WeightUnitContext value="KG">
        <RouterProvider router={router} />
      </WeightUnitContext>
    </QueryClientProvider>,
  )
  return router
}

const statusIs = (status: string) => () => json({ status })

describe('WaitingPage', () => {
  afterEach(() => i18n.changeLanguage('en'))

  it('shows the reviewing state to a client who submitted, with focus on its heading', async () => {
    openWaiting(statusIs('WAITING_FOR_PLAN'))

    expect(
      await screen.findByRole('heading', {
        name: 'Your trainer is reviewing your plan',
      }),
    ).toHaveFocus()
    expect(
      screen.getByText(
        "Your trainer checks it before you see it. You get a notice here when it's ready.",
      ),
    ).toBeInTheDocument()
  })

  it('speaks Polish when the UI does', async () => {
    await i18n.changeLanguage('pl')
    openWaiting(statusIs('WAITING_FOR_PLAN'))

    expect(
      await screen.findByRole('heading', { name: 'Trener sprawdza Twój plan' }),
    ).toBeInTheDocument()
  })

  it('sends a client who has not submitted to the quiz', async () => {
    const router = openWaiting(statusIs('NOT_SUBMITTED'))

    expect(
      await screen.findByRole('heading', { name: 'What do you train for?' }),
    ).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/app/quiz/1')
  })

  it('sends a client with a published plan to the app', async () => {
    const router = openWaiting(statusIs('PLAN_PUBLISHED'))

    expect(
      await screen.findByRole('heading', { name: 'Today' }),
    ).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/app')
  })

  it('shows only a loading status while the status is read', async () => {
    openWaiting(() => new Promise<Response>(() => {}))

    expect(await screen.findByRole('status')).toHaveTextContent('Loading')
    expect(screen.queryByRole('heading')).not.toBeInTheDocument()
  })

  it('shows the message for the error and asks again with Try again', async () => {
    const user = userEvent.setup()
    let failures = 1
    openWaiting(() =>
      failures-- > 0
        ? problem(500, { status: 500, code: 'internal.error' })
        : json({ status: 'WAITING_FOR_PLAN' }),
    )

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Something went wrong on our side. Try again in a moment.',
    )

    await user.click(screen.getByRole('button', { name: 'Try again' }))

    expect(
      await screen.findByRole('heading', {
        name: 'Your trainer is reviewing your plan',
      }),
    ).toBeInTheDocument()
  })
})
