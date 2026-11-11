import { screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import i18n from '@/lib/i18n'
import { renderPage } from '@/test/page'
import type { ClientOverview } from '../api/client-training'
import { ReadinessStrip } from './readiness-strip'

type Day = ClientOverview['readiness'][number]

// 22 October to Wednesday 4 November, with check-ins only on the days given
function fourteenDays(scored: Record<string, Omit<Day, 'date'>>): Day[] {
  return Array.from({ length: 14 }, (_, index) => {
    const date = new Date(Date.UTC(2026, 9, 22 + index))
      .toISOString()
      .slice(0, 10)
    return { date, ...scored[date] }
  })
}

const OVERVIEW: ClientOverview = {
  today: '2026-11-04',
  recentSessions: [],
  readiness: fourteenDays({
    '2026-10-22': { score: 83, band: 'READY' },
    '2026-10-26': { score: 33, band: 'RECOVERY_DAY' },
    '2026-10-30': { score: 50, band: 'TAKE_IT_EASIER' },
    '2026-11-01': { score: 33, band: 'RECOVERY_DAY' },
    '2026-11-04': { score: 0, band: 'RECOVERY_DAY' },
  }),
  recoveryDays: [
    {
      date: '2026-11-04',
      score: 0,
      causes: ['SLEEP', 'SORENESS', 'STRESS'],
      targets: 'ORIGINAL',
      session: {
        date: '2026-11-04',
        day: 'WEDNESDAY',
        name: 'Lower body',
        state: 'TODAY',
        locked: false,
        setsLogged: 0,
        setsPlanned: 15,
      },
    },
    {
      date: '2026-11-01',
      score: 33,
      causes: ['SLEEP', 'SORENESS'],
    },
    {
      date: '2026-10-26',
      score: 33,
      causes: ['SLEEP', 'SORENESS'],
      targets: 'LIGHTER',
      session: {
        date: '2026-10-26',
        day: 'MONDAY',
        name: 'Upper body',
        state: 'DONE',
        locked: true,
        setsLogged: 15,
        setsPlanned: 15,
        averageRpe: 7.1,
        sessionLogId: '00000000-0000-4000-9300-000000000002',
      },
    },
  ],
}

const card = () =>
  within(screen.getByRole('region', { name: 'Readiness, last 14 days' }))

describe('ReadinessStrip', () => {
  afterEach(() => i18n.changeLanguage('en'))

  it('shows a column for each of the 14 days, today last, a day without a check-in empty and a score of 0 as a bar', () => {
    renderPage(<ReadinessStrip overview={OVERVIEW} />)

    const days = within(card().getAllByRole('list')[1]).getAllByRole('listitem')
    expect(days).toHaveLength(14)
    expect(days[0]).toHaveTextContent('Thu, Oct 22: 83, Ready')
    expect(days[1]).toHaveTextContent('Fri, Oct 23: no check-in')
    expect(days[1]).not.toHaveTextContent(/\d+,/)
    expect(days[8]).toHaveTextContent('Fri, Oct 30: 50, Take it easier')
    expect(days[13]).toHaveTextContent('Wed, Nov 4: 0, Recovery day, Today')
  })

  it('names the three bands in a legend', () => {
    renderPage(<ReadinessStrip overview={OVERVIEW} />)

    const legend = within(card().getByRole('list', { name: 'Readiness bands' }))
    expect(
      legend.getAllByRole('listitem').map((item) => item.textContent),
    ).toEqual(['Ready70+', 'Take it easier45–69', 'Recovery day0–44'])
  })

  it('lists each recovery day newest first, with its reason, which targets the client had and its session', () => {
    renderPage(<ReadinessStrip overview={OVERVIEW} />)

    const entries = within(
      card().getByRole('region', { name: 'Recovery days' }),
    ).getAllByRole('listitem')
    expect(entries.map((entry) => entry.textContent)).toEqual([
      'Wed, Nov 4Readiness 0Poor sleep, high soreness, and high stressOriginal targetsLower bodyToday',
      'Sun, Nov 1Readiness 33Poor sleep and high sorenessNo lighter targets',
      'Mon, Oct 26Readiness 33Poor sleep and high sorenessLighter targetsUpper bodyLocked',
    ])
    // only a started session has a log to open
    expect(within(entries[0]).queryByRole('link')).not.toBeInTheDocument()
    expect(
      within(entries[2]).getByRole('link', { name: 'Upper body' }),
    ).toHaveAttribute(
      'href',
      '/logs?session=00000000-0000-4000-9300-000000000002',
    )
  })

  it('says when there is no recovery day, under the days', () => {
    renderPage(<ReadinessStrip overview={{ ...OVERVIEW, recoveryDays: [] }} />)

    expect(
      card().getByText('No recovery days in the last 14 days.'),
    ).toBeInTheDocument()
    expect(card().getAllByRole('listitem')).toHaveLength(3 + 14)
  })

  it('says when the client has not checked in in 14 days, and shows no days', () => {
    renderPage(
      <ReadinessStrip
        overview={{
          ...OVERVIEW,
          readiness: fourteenDays({}),
          recoveryDays: [],
        }}
      />,
    )

    expect(
      card().getByText('No check-ins in the last 14 days.'),
    ).toBeInTheDocument()
    expect(card().queryByRole('list')).not.toBeInTheDocument()
  })

  it('reads in Polish, with the reason as a Polish list', async () => {
    await i18n.changeLanguage('pl')
    renderPage(<ReadinessStrip overview={OVERVIEW} />)

    const region = within(
      screen.getByRole('region', { name: 'Gotowość, ostatnie 14 dni' }),
    )
    expect(
      region.getByText('Słaby sen, duża bolesność mięśni i wysoki stres'),
    ).toBeInTheDocument()
    expect(region.getByText('Cele z planu')).toBeInTheDocument()
    expect(region.getByText('Bez lżejszych celów')).toBeInTheDocument()
  })
})
