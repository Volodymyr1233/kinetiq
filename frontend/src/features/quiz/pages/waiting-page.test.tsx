import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import i18n from '@/lib/i18n'
import { WeightUnitContext } from '@/lib/weight'
import { json, problem, stubApi, type Reply } from '@/test/page'
import { QuizPage } from './quiz-page'
import { WaitingPage } from './waiting-page'

function openWaiting(reply: Reply, trainerName?: string) {
  stubApi(reply)
  // the same addresses as in app/router.ts; a feature does not import the app (frontend/CLAUDE.md)
  const router = createMemoryRouter(
    [
      {
        path: '/app/quiz/waiting',
        element: <WaitingPage trainerName={trainerName} />,
      },
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

const avatar = () => document.querySelector('[data-slot="avatar"]')

describe('WaitingPage', () => {
  afterEach(() => i18n.changeLanguage('en'))

  describe("with the trainer's name", () => {
    it('shows their avatar and names them in the text, under the same heading', async () => {
      openWaiting(statusIs('WAITING_FOR_PLAN'), 'Marek Nowak')

      expect(
        await screen.findByRole('heading', {
          name: 'Your trainer is reviewing your plan',
        }),
      ).toHaveFocus()
      expect(screen.getByText('MN')).toBeInTheDocument()
      expect(
        screen.getByText(
          "Marek checks it before you see it. You get a notice here when it's ready.",
        ),
      ).toBeInTheDocument()
    })

    it('keeps the name as it is in Polish', async () => {
      await i18n.changeLanguage('pl')
      openWaiting(statusIs('WAITING_FOR_PLAN'), 'Marek Nowak')

      expect(
        await screen.findByText(
          'Marek sprawdza plan, zanim go zobaczysz. Gdy będzie gotowy, dostaniesz tu powiadomienie.',
        ),
      ).toBeInTheDocument()
    })

    it('leaves the building state as it is', async () => {
      openWaiting(statusIs('DRAFTING_PLAN'), 'Marek Nowak')

      await screen.findByRole('heading', { name: 'Building your plan' })

      expect(avatar()).toBeNull()
      expect(
        screen.getByText(
          'Sessions, exercises and starting weights from your answers. This can take a few minutes, and you can close the app.',
        ),
      ).toBeInTheDocument()
    })
  })

  describe('while the plan is being built', () => {
    afterEach(() => {
      vi.useRealTimers()
    })

    const reviewingTitle = 'Your trainer is reviewing your plan'

    /** Answers `DRAFTING_PLAN` until `end` is called; `failing` makes a check fail instead. */
    function drafting() {
      const backend = { status: 'DRAFTING_PLAN', failing: false, checks: 0 }
      const reply = () => {
        backend.checks += 1
        return backend.failing
          ? problem(500, { status: 500, code: 'internal.error' })
          : json({ status: backend.status })
      }
      return { backend, reply }
    }

    const nextCheck = () =>
      act(async () => {
        await vi.advanceTimersByTimeAsync(5000)
      })

    it('shows the building state with a skeleton hidden from assistive technology, and focus on its heading', async () => {
      const { reply } = drafting()
      openWaiting(reply)

      expect(
        await screen.findByRole('heading', { name: 'Building your plan' }),
      ).toHaveFocus()
      expect(
        screen.getByText(
          'Sessions, exercises and starting weights from your answers. This can take a few minutes, and you can close the app.',
        ),
      ).toBeInTheDocument()
      expect(
        document.querySelector('[aria-hidden="true"] [data-slot="skeleton"]'),
      ).toBeInTheDocument()
    })

    it('speaks Polish when the UI does', async () => {
      await i18n.changeLanguage('pl')
      const { reply } = drafting()
      openWaiting(reply)

      expect(
        await screen.findByRole('heading', { name: 'Budujemy Twój plan' }),
      ).toBeInTheDocument()
    })

    it('moves to reviewing by itself, leaving focus where it is and announcing the change', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
      const { backend, reply } = drafting()
      openWaiting(reply)
      await screen.findByRole('heading', { name: 'Building your plan' })
      const elsewhere = document.createElement('button')
      document.body.append(elsewhere)
      elsewhere.focus()
      expect(screen.getByRole('status')).toBeEmptyDOMElement()

      backend.status = 'WAITING_FOR_PLAN'
      await nextCheck()

      expect(
        await screen.findByRole('heading', { name: reviewingTitle }),
      ).toBeInTheDocument()
      expect(elsewhere).toHaveFocus()
      expect(screen.getByRole('status')).toHaveTextContent(reviewingTitle)
      expect(document.querySelector('[data-slot="skeleton"]')).toBeNull()
      elsewhere.remove()
    })

    it('stops asking once the plan is with the trainer', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
      const { backend, reply } = drafting()
      openWaiting(reply)
      await screen.findByRole('heading', { name: 'Building your plan' })
      backend.status = 'WAITING_FOR_PLAN'
      await nextCheck()
      await screen.findByRole('heading', { name: reviewingTitle })
      const checks = backend.checks

      await nextCheck()
      await nextCheck()

      expect(backend.checks).toBe(checks)
    })

    it('keeps the state when a check fails, and goes on checking', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
      const { backend, reply } = drafting()
      openWaiting(reply)
      await screen.findByRole('heading', { name: 'Building your plan' })

      backend.failing = true
      const checks = backend.checks
      await nextCheck()
      await waitFor(() => expect(backend.checks).toBeGreaterThan(checks))
      expect(
        screen.getByRole('heading', { name: 'Building your plan' }),
      ).toBeInTheDocument()

      backend.failing = false
      backend.status = 'WAITING_FOR_PLAN'
      await nextCheck()

      expect(
        await screen.findByRole('heading', { name: reviewingTitle }),
      ).toBeInTheDocument()
    })

    it('leaves for the app when a plan is published', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
      const { backend, reply } = drafting()
      const router = openWaiting(reply)
      await screen.findByRole('heading', { name: 'Building your plan' })

      backend.status = 'PLAN_PUBLISHED'
      await nextCheck()

      expect(
        await screen.findByRole('heading', { name: 'Today' }),
      ).toBeInTheDocument()
      expect(router.state.location.pathname).toBe('/app')
    })

    it('keeps the action to message the trainer in both states', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
      const { backend, reply } = drafting()
      stubApi(reply)
      const message = <button>Message your trainer</button>
      const router = createMemoryRouter(
        [
          {
            path: '/app/quiz/waiting',
            element: <WaitingPage message={message} />,
          },
        ],
        { initialEntries: ['/app/quiz/waiting'] },
      )
      render(
        <QueryClientProvider client={new QueryClient()}>
          <RouterProvider router={router} />
        </QueryClientProvider>,
      )
      const button = await screen.findByRole('button', {
        name: 'Message your trainer',
      })

      backend.status = 'WAITING_FOR_PLAN'
      await nextCheck()
      await screen.findByRole('heading', { name: reviewingTitle })

      // the same element, so a sheet opened from it stays open
      expect(screen.getByRole('button', { name: 'Message your trainer' })).toBe(
        button,
      )
    })
  })

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
    expect(avatar()).toBeNull()
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
