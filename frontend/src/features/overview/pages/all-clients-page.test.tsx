import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { json, problem, renderPage, stubApi } from '@/test/page'
import type { OverviewClient } from '../api/overview'
import { AllClientsPage } from './all-clients-page'

const id = (n: number) => `00000000-0000-4000-8000-0000000000${n}`

function client(
  n: number,
  name: string,
  fields: Partial<OverviewClient> = {},
): OverviewClient {
  return {
    id: id(n),
    name,
    planState: 'PUBLISHED',
    planWeek: 3,
    planWeeks: 8,
    planEndsOn: '2026-12-27',
    status: 'ON_TRACK',
    reasons: [],
    lighterTargets: false,
    ...fields,
  }
}

const OLA = client(10, 'Ola Kowalska', {
  goal: 'BUILD_MUSCLE',
  status: 'WATCH',
  reasons: ['HIGH_RPE'],
  sessionsDone: 11,
  sessionsDue: 12,
  averageRpe: 8.4,
  lastLoggedAt: '2026-11-12T08:00:00Z',
  readiness: 34,
  readinessBand: 'RECOVERY_DAY',
  lighterTargets: true,
})
const JAN = client(11, 'Jan Nowak', {
  status: 'AT_RISK',
  reasons: ['LOW_COMPLETION'],
  sessionsDone: 5,
  sessionsDue: 12,
  averageRpe: 7.1,
  lastLoggedAt: '2026-11-06T08:00:00Z',
})
const KONRAD = client(12, 'Konrad Kowal', {
  planState: 'DRAFT_FAILED',
  planWeek: undefined,
  planWeeks: undefined,
  planEndsOn: undefined,
  status: 'PLAN_TO_REVIEW',
})
const EWA = client(13, 'Ewa Mazur', {
  planWeek: undefined,
  planWeeks: 4,
  status: 'NO_PLAN',
  lastLoggedAt: '2026-10-30T08:00:00Z',
})
const ZOFIA = client(14, 'Zofia Woźniak', {
  sessionsDone: 12,
  sessionsDue: 12,
  averageRpe: 7.6,
  lastLoggedAt: '2026-11-11T08:00:00Z',
})

function show(clients: OverviewClient[] = [OLA, JAN, KONRAD, EWA, ZOFIA]) {
  stubApi(() =>
    json({
      activeClients: 3,
      totalClients: clients.length,
      sessionsDone: 0,
      sessionsPlanned: 0,
      draftsWaiting: 0,
      planChangesWaiting: 0,
      alerts: [],
      clients,
    }),
  )
  renderPage(<AllClientsPage />)
}

function names() {
  return screen
    .getAllByRole('row')
    .slice(1)
    .map((row) => within(row).getAllByRole('link')[0].textContent?.slice(2))
}

function filter(name: string) {
  return screen.getByRole('radio', { name: new RegExp(`^${name} \\d+$`) })
}

describe('AllClientsPage', () => {
  it('shows each client with goal, plan, sessions, RPE, readiness and status', async () => {
    show()

    const ola = (
      await screen.findByRole('link', { name: /Ola Kowalska/ })
    ).closest('tr')!
    expect(ola).toHaveTextContent('Build muscle')
    expect(ola).toHaveTextContent('Week 3 of 8')
    expect(ola).toHaveTextContent('11/12')
    expect(ola).toHaveTextContent('8.4')
    expect(ola).toHaveTextContent('34Lighter targets')
    expect(ola).toHaveTextContent('Watch')
    const konrad = screen.getByRole('link', { name: /Konrad/ }).closest('tr')!
    expect(konrad).toHaveTextContent('Draft failed')
    expect(konrad).toHaveTextContent('No logs yet')
    expect(konrad).toHaveTextContent('No check-in')
    expect(konrad).toHaveTextContent('Plan to review')
    expect(
      screen.getByRole('link', { name: /Ewa Mazur/ }).closest('tr'),
    ).toHaveTextContent('Finished')
    expect(names()).toEqual([
      'Ewa Mazur',
      'Jan Nowak',
      'Konrad Kowal',
      'Ola Kowalska',
      'Zofia Woźniak',
    ])
  })

  describe('filters', () => {
    it('keeps the clients of one status, and each count equals its rows', async () => {
      const user = userEvent.setup()
      show()
      await screen.findByRole('table')

      for (const [name, count] of [
        ['All', 5],
        ['At risk', 1],
        ['Watch', 1],
        ['Plan to review', 1],
        ['No plan', 1],
        ['On track', 1],
      ] as const) {
        await user.click(filter(name))
        expect(filter(name)).toHaveAccessibleName(`${name} ${count}`)
        expect(filter(name)).toBeChecked()
        expect(screen.getAllByRole('row').slice(1)).toHaveLength(count)
      }
    })

    it('counts only what the search leaves', async () => {
      const user = userEvent.setup()
      show()

      await user.type(
        await screen.findByRole('searchbox', {
          name: 'Search clients by name',
        }),
        'kow',
      )

      expect(filter('All')).toHaveAccessibleName('All 2')
      expect(filter('Watch')).toHaveAccessibleName('Watch 1')
      expect(filter('Plan to review')).toHaveAccessibleName('Plan to review 1')
      expect(filter('At risk')).toHaveAccessibleName('At risk 0')
      expect(screen.getByRole('status')).toHaveTextContent('2 of 5 clients')
    })

    it('says nothing matches and clears the search and the filter', async () => {
      const user = userEvent.setup()
      show()

      await user.type(
        await screen.findByRole('searchbox', {
          name: 'Search clients by name',
        }),
        'kow',
      )
      await user.click(filter('At risk'))

      expect(screen.getByText('No client matches')).toBeInTheDocument()
      await user.click(
        screen.getByRole('button', { name: 'Clear search and filter' }),
      )
      expect(screen.getAllByRole('row').slice(1)).toHaveLength(5)
      expect(filter('All')).toBeChecked()
      expect(screen.getByRole('searchbox')).toHaveValue('')
    })
  })

  describe('sorting', () => {
    it('sorts by a column and turns the order around, empty values last both ways', async () => {
      const user = userEvent.setup()
      show()
      const rpe = await screen.findByRole('button', { name: 'Avg RPE, 2 wk' })

      await user.click(rpe)
      expect(rpe.closest('th')).toHaveAttribute('aria-sort', 'descending')
      expect(names()).toEqual([
        'Ola Kowalska',
        'Zofia Woźniak',
        'Jan Nowak',
        'Ewa Mazur',
        'Konrad Kowal',
      ])

      await user.click(rpe)
      expect(rpe.closest('th')).toHaveAttribute('aria-sort', 'ascending')
      expect(names()).toEqual([
        'Jan Nowak',
        'Zofia Woźniak',
        'Ola Kowalska',
        'Ewa Mazur',
        'Konrad Kowal',
      ])
    })

    it('puts the most urgent status, the lowest share of sessions and the newest log first', async () => {
      const user = userEvent.setup()
      show()

      await user.click(await screen.findByRole('button', { name: 'Status' }))
      expect(names()).toEqual([
        'Jan Nowak',
        'Ola Kowalska',
        'Konrad Kowal',
        'Ewa Mazur',
        'Zofia Woźniak',
      ])

      await user.click(screen.getByRole('button', { name: 'Sessions, 4 wk' }))
      expect(names().slice(0, 3)).toEqual([
        'Jan Nowak',
        'Ola Kowalska',
        'Zofia Woźniak',
      ])

      await user.click(screen.getByRole('button', { name: 'Last log' }))
      expect(names()).toEqual([
        'Ola Kowalska',
        'Zofia Woźniak',
        'Jan Nowak',
        'Ewa Mazur',
        'Konrad Kowal',
      ])
    })
  })

  it('offers to invite a client when the trainer has none, and no search', async () => {
    show([])

    expect(await screen.findByText('No clients yet')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Invite client' })).toHaveAttribute(
      'href',
      '/trainer/clients/invites',
    )
    expect(screen.queryByRole('searchbox')).not.toBeInTheDocument()
  })

  it('offers to try again when the clients cannot be loaded', async () => {
    stubApi(() => problem(500, { status: 500, code: 'internal.error' }))
    renderPage(<AllClientsPage />)

    expect(
      await screen.findByRole('button', { name: 'Try again' }),
    ).toBeInTheDocument()
  })
})
