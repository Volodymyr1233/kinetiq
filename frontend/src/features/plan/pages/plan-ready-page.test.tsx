import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, describe, expect, it } from 'vitest'
import i18n from '@/lib/i18n'
import {
  json,
  problem,
  stubApi,
  withOpenPlanChange,
  type Reply,
} from '@/test/page'
import { ClientPlanPage } from './client-plan-page'
import { PlanReadyPage } from './plan-ready-page'

const PLAN = {
  goal: 'LOSE_FAT',
  weeks: 12,
  currentWeek: 1,
  finished: false,
  seen: false,
  week: { number: 1, startsOn: '2026-11-02' },
  sessions: [
    { name: 'Upper body', day: 'MONDAY', entries: [] },
    { name: 'Lower body', day: 'THURSDAY', entries: [] },
  ],
}

function openReady(reply: Reply) {
  const requests = stubApi(withOpenPlanChange(null, reply))
  // the same addresses as in app/router.ts; a feature does not import the app (frontend/CLAUDE.md)
  const router = createMemoryRouter(
    [
      { path: '/app/plan-ready', Component: PlanReadyPage },
      { path: '/app/plan', Component: ClientPlanPage },
      { path: '/app', Component: () => <h1>Today</h1> },
    ],
    { initialEntries: ['/app/plan-ready'] },
  )
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
  return { requests, router }
}

const acknowledgements = (requests: Request[]) =>
  requests.filter(
    (request) =>
      request.method === 'POST' &&
      new URL(request.url).pathname === '/api/v1/plan/acknowledgement',
  )

describe('PlanReadyPage', () => {
  afterEach(() => i18n.changeLanguage('en'))

  it('tells the client the plan is ready, with what it holds, and focuses the heading', async () => {
    openReady(() => json(PLAN))

    expect(
      await screen.findByRole('heading', { name: 'Your plan is ready' }),
    ).toHaveFocus()
    expect(
      screen.getByText('Lose fat · 12 weeks · 2 sessions a week'),
    ).toBeInTheDocument()
  })

  it('opens the Plan tab, which marks the plan seen', async () => {
    const user = userEvent.setup()
    let seen = false
    const { requests, router } = openReady((request) => {
      if (request.method === 'POST') {
        seen = true
        return new Response(null, { status: 204 })
      }
      return json({ ...PLAN, seen })
    })

    await user.click(await screen.findByRole('link', { name: 'View my plan' }))

    expect(await screen.findByText('Week 1 of 12')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/app/plan')
    await waitFor(() => expect(acknowledgements(requests)).toHaveLength(1))
  })

  it('does not show again for a plan that was seen', async () => {
    openReady(() => json({ ...PLAN, seen: true }))

    expect(
      await screen.findByRole('heading', { name: 'Today' }),
    ).toBeInTheDocument()
  })

  it('sends a client without a published plan on to the app', async () => {
    openReady(() => problem(404, { status: 404, code: 'plan.not-found' }))

    expect(
      await screen.findByRole('heading', { name: 'Today' }),
    ).toBeInTheDocument()
  })

  it('speaks Polish when the UI does', async () => {
    await i18n.changeLanguage('pl')
    openReady(() => json({ ...PLAN, weeks: 8 }))

    expect(
      await screen.findByRole('heading', { name: 'Twój plan jest gotowy' }),
    ).toBeInTheDocument()
    expect(
      screen.getByText('Redukcja tłuszczu · 8 tygodni · 2 sesje w tygodniu'),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: 'Zobacz mój plan' }),
    ).toBeInTheDocument()
  })

  it('offers to try again when the plan cannot be loaded', async () => {
    const user = userEvent.setup()
    let failing = true
    openReady(() =>
      failing
        ? problem(429, { status: 429, code: 'request.too-many' })
        : json(PLAN),
    )

    const retry = await screen.findByRole('button', { name: 'Try again' })
    failing = false
    await user.click(retry)

    expect(
      await screen.findByRole('heading', { name: 'Your plan is ready' }),
    ).toBeInTheDocument()
  })
})
