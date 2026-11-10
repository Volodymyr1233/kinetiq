import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import i18n from '@/lib/i18n'
import { WeightUnitContext } from '@/lib/weight'
import { json, problem, stubApi, type Reply } from '@/test/page'
import type { Progress } from '../api/progress'
import { ClientProgressSection } from './client-progress-section'

const CLIENT_ID = '00000000-0000-4000-8000-000000000003'

// Wednesday of plan week 14, with a PR logged today and four weeks in a row, which the trainer is not shown
const PROGRESS = {
  goal: 'BUILD_MUSCLE',
  week: 14,
  finished: false,
  sessionsDone: 9,
  sessionsPlanned: 12,
  sessionsDoneChange: -1,
  averageRpe: 8.4,
  averageRpeChange: 0.6,
  weeksInARow: 4,
  weeks: [
    { number: 11, startsOn: '2026-10-19', planned: 3, done: 3, running: false },
    { number: 12, startsOn: '2026-10-26', planned: 3, done: 3, running: false },
    { number: 13, startsOn: '2026-11-02', planned: 3, done: 2, running: false },
    { number: 14, startsOn: '2026-11-09', planned: 3, done: 1, running: true },
  ],
  exercises: [
    {
      exerciseId: '00000000-0000-4000-9100-000000000001',
      name: 'Back squat',
      measure: 'REPS',
      best: { loadKg: 102.5, amount: 5, rpe: 8 },
      topSetChange: { loadKg: 2.5 },
      prToday: true,
      weeks: [
        { number: 13, top: { loadKg: 100, amount: 5, rpe: 8 }, averageRpe: 8 },
        {
          number: 14,
          top: { loadKg: 102.5, amount: 5, rpe: 8 },
          averageRpe: 8,
        },
      ],
      recent: [
        { date: '2026-11-11', top: { loadKg: 102.5, amount: 5, rpe: 8 } },
        { date: '2026-11-04', top: { loadKg: 100, amount: 5, rpe: 8 } },
      ],
    },
  ],
} satisfies Progress

function openSection(reply: Reply) {
  const requests = stubApi(reply)
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <WeightUnitContext value="KG">
        <ClientProgressSection clientId={CLIENT_ID} />
      </WeightUnitContext>
    </QueryClientProvider>,
  )
  return requests
}

const weeksAsked = (requests: Request[]) =>
  requests.map((request) => {
    const url = new URL(request.url)
    expect(url.pathname).toBe(`/api/v1/clients/${CLIENT_ID}/progress`)
    return url.searchParams.get('weeks')
  })

describe('ClientProgressSection', () => {
  beforeEach(() => {
    // jsdom measures every box as 0 × 0, and Recharts renders nothing in an empty container
    vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockReturnValue(
      new DOMRect(0, 0, 320, 200),
    )
  })

  afterEach(async () => {
    vi.restoreAllMocks()
    await i18n.changeLanguage('en')
  })

  it('shows the client’s figures and charts for 8 weeks, without the client’s rewards', async () => {
    const requests = openSection(() => json(PROGRESS))

    const section = within(
      await screen.findByRole('region', { name: 'Progress' }),
    )
    expect(weeksAsked(requests)).toEqual(['8'])
    expect(section.getByRole('radio', { name: '8 weeks' })).toBeChecked()
    const tiles = within(
      section.getByRole('region', { name: 'Figures for the period' }),
    )
    expect(
      tiles.getByRole('progressbar', { name: 'Sessions done' }),
    ).toHaveAttribute('aria-valuetext', '9 of 12 sessions')
    expect(tiles.getByText('8.4')).toBeInTheDocument()
    expect(tiles.getByText('Above 7–8 target')).toBeInTheDocument()
    expect(tiles.getByText('Back squat best')).toBeInTheDocument()
    expect(tiles.getByText('102.5')).toBeInTheDocument()
    expect(tiles.getByText('2.5 kg week on week')).toBeInTheDocument()
    expect(tiles.queryByText('PR today')).not.toBeInTheDocument()
    expect(tiles.queryByText('In a row')).not.toBeInTheDocument()
    expect(
      section.getByRole('heading', { name: '2 of 3 weeks at 70% or more' }),
    ).toBeInTheDocument()
    expect(
      within(section.getByRole('region', { name: 'By exercise' })).getByRole(
        'heading',
        { name: 'Top set rose 2.5 kg in 2 weeks' },
      ),
    ).toBeInTheDocument()
  })

  it('asks for 4 weeks when the trainer chooses them', async () => {
    const user = userEvent.setup()
    const requests = openSection(() => json(PROGRESS))

    await user.click(await screen.findByRole('radio', { name: '4 weeks' }))

    await waitFor(() => expect(weeksAsked(requests)).toEqual(['8', '4']))
    expect(screen.getByRole('radio', { name: '4 weeks' })).toBeChecked()
  })

  it('says nothing is logged in the period instead of showing empty charts', async () => {
    openSection(() =>
      json({
        ...PROGRESS,
        sessionsDone: 0,
        sessionsDoneChange: undefined,
        averageRpe: undefined,
        averageRpeChange: undefined,
        exercises: [],
      }),
    )

    expect(
      await screen.findByText('No sessions logged in this period yet.'),
    ).toBeInTheDocument()
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
    expect(
      screen.queryByRole('region', { name: 'By exercise' }),
    ).not.toBeInTheDocument()
  })

  // a 5xx is retried with a pause by design, so the failure here is one that is not
  it('offers to try again when the progress cannot be loaded', async () => {
    const user = userEvent.setup()
    let failing = true
    openSection(() =>
      failing
        ? problem(429, { status: 429, code: 'request.too-many' })
        : json(PROGRESS),
    )

    const retry = await screen.findByRole('button', { name: 'Try again' })
    failing = false
    await user.click(retry)

    expect(
      await screen.findByRole('region', { name: 'Progress' }),
    ).toBeInTheDocument()
  })

  it('speaks Polish to a trainer who reads Polish', async () => {
    await i18n.changeLanguage('pl')
    openSection(() => json({ ...PROGRESS, sessionsDone: 0, exercises: [] }))

    expect(
      await screen.findByRole('region', { name: 'Postępy' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: '8 tygodni' })).toBeChecked()
    expect(
      screen.getByText('W tym okresie nie zapisano jeszcze żadnej sesji.'),
    ).toBeInTheDocument()
  })
})
