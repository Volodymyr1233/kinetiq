import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { json, problem, renderPage, stubApi } from '@/test/page'
import type { Overview, OverviewClient } from '../api/overview'
import { OverviewPage } from './overview-page'

const id = (n: number) => `00000000-0000-4000-8000-0000000000${n}`

const ANNA: OverviewClient = {
  id: id(10),
  name: 'Anna Kowalska',
  goal: 'BUILD_MUSCLE',
  planState: 'PUBLISHED',
  planWeek: 6,
  planWeeks: 8,
  planEndsOn: '2026-11-29',
  status: 'AT_RISK',
  reasons: ['LOW_COMPLETION', 'HIGH_RPE'],
  sessionsDone: 5,
  sessionsDue: 12,
  averageRpe: 8.4,
  lastLoggedAt: '2026-11-11T17:00:00Z',
  lighterTargets: false,
}
const BARTEK: OverviewClient = {
  ...ANNA,
  id: id(11),
  name: 'Bartek Nowak',
  status: 'WATCH',
  reasons: ['HIGH_RPE'],
  sessionsDone: 12,
  averageRpe: 8.6,
  readiness: 8,
  readinessBand: 'RECOVERY_DAY',
  lighterTargets: true,
}
const CELINA: OverviewClient = {
  ...ANNA,
  id: id(12),
  name: 'Celina Wójcik',
  planWeek: 8,
  planEndsOn: '2026-11-15',
  status: 'ON_TRACK',
  reasons: [],
  sessionsDone: 12,
  averageRpe: 7.4,
}
const DAWID: OverviewClient = {
  id: id(13),
  name: 'Dawid Lis',
  planState: 'DRAFT_FAILED',
  status: 'PLAN_TO_REVIEW',
  reasons: [],
  lighterTargets: false,
}
const EWA: OverviewClient = {
  id: id(14),
  name: 'Ewa Mazur',
  planState: 'PUBLISHED',
  planWeeks: 4,
  planEndsOn: '2026-11-01',
  status: 'NO_PLAN',
  reasons: [],
  lighterTargets: false,
}
const FILIP: OverviewClient = {
  ...CELINA,
  id: id(15),
  name: 'Filip Zając',
  planWeek: 6,
  lastLoggedAt: '2026-11-05T17:00:00Z',
}

const OVERVIEW: Overview = {
  activeClients: 4,
  totalClients: 6,
  sessionsDone: 7,
  sessionsPlanned: 12,
  averageRpe: 7.8,
  averageRpeChange: 0.2,
  draftsWaiting: 1,
  planChangesWaiting: 1,
  alerts: [
    {
      kind: 'AT_RISK',
      clientId: ANNA.id,
      planChangeId: '00000000-0000-4000-9600-000000000001',
      planChangeTitle: 'Cut volume by 10%',
      waitingFor: 'TRAINER',
    },
    { kind: 'WATCH', clientId: BARTEK.id },
    {
      kind: 'NO_LOG',
      clientId: FILIP.id,
      daysWithoutLog: 7,
      since: '2026-11-05',
      lastSessionName: 'Lower body',
    },
    {
      kind: 'PLAN_CHANGE',
      clientId: CELINA.id,
      planChangeId: '00000000-0000-4000-9600-000000000002',
      planChangeTitle: 'Back squat: raise load by 5%',
      waitingFor: 'BOTH',
    },
    { kind: 'DRAFT', clientId: DAWID.id, draft: 'FAILED' },
    { kind: 'PLAN_ENDING', clientId: CELINA.id },
    { kind: 'PLAN_ENDING', clientId: EWA.id },
    { kind: 'RECOVERY_DAY', clientId: BARTEK.id },
  ],
  clients: [ANNA, BARTEK, CELINA, DAWID, EWA, FILIP],
}

function show(overview: Overview = OVERVIEW) {
  const requests = stubApi(() => json(overview))
  renderPage(<OverviewPage />)
  return requests
}

async function alerts() {
  const region = await screen.findByRole('region', { name: 'Needs attention' })
  return within(region).getAllByRole('listitem')
}

describe('OverviewPage', () => {
  it('shows the four tiles across the clients, with the approvals one as a link', async () => {
    show()

    const tiles = await screen.findByRole('region', {
      name: 'This week across your clients',
    })
    expect(tiles).toHaveTextContent('Active clients4of 6 clients')
    expect(
      within(tiles).getByRole('progressbar', {
        name: 'Sessions done this week',
      }),
    ).toHaveAttribute('aria-valuetext', '7 of 12 sessions done')
    expect(tiles).toHaveTextContent('Average RPE7.8')
    expect(tiles).toHaveTextContent('Up 0.2 vs last week')
    expect(
      within(tiles).getByRole('link', { name: 'Waiting for you' }),
    ).toHaveAttribute('href', '/trainer/approvals')
    expect(tiles).toHaveTextContent('21 plan change, 1 new plan')
  })

  it('says so when nothing was logged this week', async () => {
    show({ ...OVERVIEW, averageRpe: undefined, averageRpeChange: undefined })

    expect(
      await screen.findByText('No sets logged this week'),
    ).toBeInTheDocument()
  })

  it('lists every alert most severe first, each with its reason and one action', async () => {
    show()

    const items = await alerts()

    const expected: [string, string, string, string][] = [
      [
        'Anna Kowalska did 5 of 12 sessions in 4 weeks',
        "Anna Kowalska's average RPE is 8.4 over 2 weeks.Cut volume by 10% is waiting for you.",
        'Open plan change, Anna Kowalska',
        '/trainer/plan-changes/00000000-0000-4000-9600-000000000001',
      ],
      [
        "Bartek Nowak's average RPE is 8.6 over 2 weeks",
        '',
        'Open, Bartek Nowak',
        `/trainer/clients/${BARTEK.id}`,
      ],
      [
        "Filip Zając hasn't logged for 7 days",
        'Last session: Lower body, Thu, Nov 5.',
        'Message, Filip Zając',
        `/trainer/messages/${FILIP.id}`,
      ],
      [
        'A plan change for Celina Wójcik is open',
        'Back squat: raise load by 5% is waiting for both of you.',
        'Open plan change, Celina Wójcik',
        '/trainer/plan-changes/00000000-0000-4000-9600-000000000002',
      ],
      [
        "Dawid Lis's plan couldn't be built automatically",
        'Dawid Lis sees it after you publish.',
        'Review plan, Dawid Lis',
        `/trainer/clients/${DAWID.id}/plan`,
      ],
      [
        "Celina Wójcik's plan ends on Sun, Nov 15",
        'Extend it or plan what Celina Wójcik trains next.',
        'Open plan, Celina Wójcik',
        `/trainer/clients/${CELINA.id}/plan`,
      ],
      [
        "Ewa Mazur's plan ended on Sun, Nov 1",
        'Extend it or plan what Ewa Mazur trains next.',
        'Open plan, Ewa Mazur',
        `/trainer/clients/${EWA.id}/plan`,
      ],
      [
        'Bartek Nowak has a recovery day',
        'Readiness 8. Lighter targets for today.',
        'Open, Bartek Nowak',
        `/trainer/clients/${BARTEK.id}`,
      ],
    ]
    expect(items).toHaveLength(expected.length)
    items.forEach((item, index) => {
      const [title, lines, action, href] = expected[index]
      expect(item).toHaveTextContent(`${title}${lines}`)
      expect(within(item).getByRole('link', { name: action })).toHaveAttribute(
        'href',
        href,
      )
    })
  })

  it('names the client who has yet to accept, and a recovery day kept at the original targets', async () => {
    show({
      ...OVERVIEW,
      clients: [ANNA, { ...BARTEK, lighterTargets: false }],
      alerts: [
        {
          kind: 'PLAN_CHANGE',
          clientId: ANNA.id,
          planChangeId: '00000000-0000-4000-9600-000000000001',
          planChangeTitle: 'Cut volume by 10%',
          waitingFor: 'CLIENT',
        },
        { kind: 'RECOVERY_DAY', clientId: BARTEK.id },
        {
          kind: 'NO_LOG',
          clientId: ANNA.id,
          daysWithoutLog: 6,
          since: '2026-11-06',
        },
      ],
    })

    const [planChange, recovery, noLog] = await alerts()
    expect(planChange).toHaveTextContent(
      'Cut volume by 10% is waiting for Anna Kowalska.',
    )
    expect(recovery).toHaveTextContent(
      'Readiness 8. Bartek Nowak kept the original targets.',
    )
    expect(noLog).toHaveTextContent(
      'Nothing logged since the plan was published on Fri, Nov 6.',
    )
  })

  it('says in one line when nothing needs the trainer', async () => {
    show({ ...OVERVIEW, alerts: [] })

    expect(
      await screen.findByText('Nothing needs you right now.'),
    ).toBeInTheDocument()
  })

  it('shows the 8 most urgent clients, then by name, with a link to all of them', async () => {
    const many = Array.from({ length: 9 }, (_, n) => ({
      ...CELINA,
      id: id(20 + n),
      name: `Client ${n}`,
    }))
    show({ ...OVERVIEW, clients: [...many, EWA, ANNA, DAWID, BARTEK] })

    const table = await screen.findByRole('table')
    const names = within(table)
      .getAllByRole('row')
      .slice(1)
      .map((row) => within(row).getAllByRole('cell')[0].textContent)
    expect(names).toEqual([
      'AKAnna Kowalska',
      'BNBartek Nowak',
      'DLDawid Lis',
      'EMEwa Mazur',
      'C0Client 0',
      'C1Client 1',
      'C2Client 2',
      'C3Client 3',
    ])
    expect(screen.getByRole('link', { name: 'View all 13' })).toHaveAttribute(
      'href',
      '/trainer/clients/all',
    )
  })

  it('shows skeletons while loading', () => {
    stubApi(() => new Promise<Response>(() => {}))
    renderPage(<OverviewPage />)

    expect(screen.getByRole('status')).toHaveTextContent('Loading the overview')
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('offers to invite a client when there is none', async () => {
    show({ ...OVERVIEW, clients: [], alerts: [] })

    expect(await screen.findByText('No clients yet')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Invite client' })).toHaveAttribute(
      'href',
      '/trainer/clients/invites',
    )
  })

  it('tells what went wrong and tries again', async () => {
    const user = userEvent.setup()
    let failing = true
    stubApi(() =>
      failing
        ? problem(500, { status: 500, code: 'internal.error' })
        : json(OVERVIEW),
    )
    renderPage(<OverviewPage />)

    const retry = await screen.findByRole('button', { name: 'Try again' })
    failing = false
    await user.click(retry)

    expect(await alerts()).toHaveLength(8)
  })
})
