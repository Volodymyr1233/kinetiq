import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it } from 'vitest'
import i18n from '@/lib/i18n'
import type { components } from '@/lib/api/schema'
import { json, problem, renderAppAt, SIGNED_IN, type Reply } from '@/test/page'
import type { ClientOverview, ClientSession } from '../api/client-training'

type Client = components['schemas']['ClientResponse']

const ANNA: Client = {
  id: '00000000-0000-4000-8000-000000000003',
  name: 'Anna Kowalska',
  goal: 'BUILD_MUSCLE',
  planState: 'PUBLISHED',
}

const session = (
  date: string,
  day: ClientSession['day'],
  name: string,
  rest: Partial<ClientSession>,
): ClientSession => ({
  date,
  day,
  name,
  locked: false,
  setsLogged: 0,
  setsPlanned: 15,
  ...rest,
})

const MONDAY = session('2026-11-02', 'MONDAY', 'Upper body', {
  state: 'DONE',
  setsLogged: 15,
  averageRpe: 8.2,
  sessionLogId: '00000000-0000-4000-9300-000000000002',
})
const WEDNESDAY = session('2026-11-04', 'WEDNESDAY', 'Lower body', {
  state: 'IN_PROGRESS',
  setsLogged: 6,
  averageRpe: 7.5,
  sessionLogId: '00000000-0000-4000-9300-000000000004',
})

// a Wednesday: Monday finished, Wednesday started, Friday still to come
const OVERVIEW: ClientOverview = {
  today: '2026-11-04',
  week: {
    number: 2,
    startsOn: '2026-11-02',
    sessions: [
      MONDAY,
      WEDNESDAY,
      session('2026-11-06', 'FRIDAY', 'Full body', { state: 'PLANNED' }),
    ],
  },
  recentSessions: [
    WEDNESDAY,
    MONDAY,
    session('2026-10-30', 'FRIDAY', 'Full body', {
      state: 'DONE',
      locked: true,
      setsLogged: 15,
      averageRpe: 8.6,
      sessionLogId: '00000000-0000-4000-9300-000000000001',
    }),
    session('2026-10-28', 'WEDNESDAY', 'Lower body', { state: 'MISSED' }),
  ],
  quizAnswers: {
    goal: 'BUILD_MUSCLE',
    experience: 'YEARS_1_TO_2',
    level: 'INTERMEDIATE',
    age: 29,
    heightCm: 168,
    weightKg: 62.5,
    likes: ['WEIGHTS', 'YOGA_PILATES'],
    daysPerWeek: 3,
    notes: 'Left knee gets stiff on deep squats',
    submittedAt: '2026-10-01T10:00:00Z',
  },
}

// the progress section's own figures are tested with it; here only where it stands
const PROGRESS = {
  goal: 'BUILD_MUSCLE',
  week: 2,
  finished: false,
  sessionsDone: 0,
  sessionsPlanned: 6,
  weeksInARow: 0,
  weeks: [],
  exercises: [],
}

function backend(client: Client, overview: ClientOverview): Reply {
  return (request) => {
    const { pathname } = new URL(request.url)
    if (pathname === `/api/v1/clients/${client.id}`) return json(client)
    if (pathname === `/api/v1/clients/${client.id}/overview`)
      return json(overview)
    if (pathname === `/api/v1/clients/${client.id}/progress`)
      return json(PROGRESS)
    return problem(404, { status: 404, code: 'client.not-found' })
  }
}

function openOverview(
  client: Client = ANNA,
  overview: ClientOverview = OVERVIEW,
  reply: Reply = backend(client, overview),
) {
  return renderAppAt(`/trainer/clients/${client.id}`, SIGNED_IN.TRAINER, reply)
}

const card = (name: string) => screen.findByRole('region', { name })

describe('ClientOverviewTab', () => {
  afterEach(() => i18n.changeLanguage('en'))

  it('opens with the client’s progress for 8 weeks, above the cards', async () => {
    const { requests } = openOverview()

    const progress = await screen.findByRole('region', { name: 'Progress' })
    expect(
      within(progress).getByRole('radio', { name: '8 weeks' }),
    ).toBeChecked()
    expect(
      progress.compareDocumentPosition(await card('This week')) &
        Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy()
    const asked = requests.find(
      (request) =>
        new URL(request.url).pathname === `/api/v1/clients/${ANNA.id}/progress`,
    )
    expect(new URL(asked!.url).searchParams.get('weeks')).toBe('8')
  })

  it('shows this week’s sessions with their labels and the seven days', async () => {
    openOverview()

    const week = within(await card('This week'))
    const rows = week
      .getAllByRole('listitem')
      .filter((item) => item.textContent?.includes('·'))
    expect(rows.map((row) => row.textContent)).toEqual([
      'Upper body · MonRPE 8.2Done',
      'Lower body · WedRPE 7.5In progress',
      'Full body · FriPlanned',
    ])
    expect(week.getByText('Upper body, Done')).toBeInTheDocument()
    expect(week.getByText('Full body, Planned')).toBeInTheDocument()
    expect(week.getAllByText('Rest day')).toHaveLength(4)
    const today = week.getByRole('listitem', { current: 'date' })
    expect(today).toHaveTextContent('Wed')
    expect(today).toHaveTextContent('Lower body, In progress')
    // the letters follow the week's order, as in the plan editor
    expect(today).toHaveTextContent('B')
  })

  it('lists the recent sessions newest first, with their figures and labels', async () => {
    openOverview()

    const recent = within(await card('Recent sessions'))
    const rows = recent.getAllByRole('listitem')
    expect(rows).toHaveLength(4)
    expect(rows[0]).toHaveTextContent('Lower body')
    expect(rows[0]).toHaveTextContent('6/15 sets · RPE 7.5')
    expect(rows[0]).toHaveTextContent('In progress')
    expect(rows[1]).toHaveTextContent('Mon, Nov 2')
    expect(rows[1]).toHaveTextContent('15/15 sets · RPE 8.2')
    expect(rows[1]).toHaveTextContent('Done')
    expect(rows[2]).toHaveTextContent('Fri, Oct 30')
    expect(rows[2]).toHaveTextContent('Locked')
    // a missed session that never started has its planned sets and no RPE
    expect(rows[3]).toHaveTextContent('0/15 sets')
    expect(rows[3]).not.toHaveTextContent('RPE')
    expect(rows[3]).toHaveTextContent('Missed')
  })

  it('shows the quiz answers', async () => {
    openOverview()

    const answers = within(await card('Quiz answers'))
    expect(answers.getByText('Build muscle')).toBeInTheDocument()
    expect(answers.getByText('62.5 kg')).toBeInTheDocument()
    expect(
      answers.getByText('Left knee gets stiff on deep squats'),
    ).toBeInTheDocument()
  })

  it('opens the Logs tab from All logs', async () => {
    const user = userEvent.setup()
    const { router } = openOverview()

    await user.click(
      within(await card('Recent sessions')).getByRole('link', {
        name: 'All logs',
      }),
    )

    await waitFor(() =>
      expect(router.state.location.pathname).toBe(
        `/trainer/clients/${ANNA.id}/logs`,
      ),
    )
    await waitFor(() =>
      expect(screen.getByRole('tab', { name: 'Logs' })).toHaveAttribute(
        'aria-selected',
        'true',
      ),
    )
  })

  it('says there is no published plan yet, and still shows the quiz answers', async () => {
    const draft: Client = { ...ANNA, planState: 'DRAFT' }
    const { requests } = openOverview(draft, {
      today: OVERVIEW.today,
      recentSessions: [],
      quizAnswers: OVERVIEW.quizAnswers,
    })

    expect(
      within(await card('This week')).getByText('No published plan yet.'),
    ).toBeInTheDocument()
    expect(
      within(await card('Recent sessions')).getByText('No published plan yet.'),
    ).toBeInTheDocument()
    expect(
      within(await card('Quiz answers')).getByText('Build muscle'),
    ).toBeInTheDocument()
    // a client without a published plan has no progress to read
    expect(
      screen.queryByRole('region', { name: 'Progress' }),
    ).not.toBeInTheDocument()
    expect(requests.some((request) => request.url.endsWith('/progress'))).toBe(
      false,
    )
  })

  it('says the plan has finished, and still lists the last sessions', async () => {
    openOverview(ANNA, { ...OVERVIEW, week: undefined })

    expect(
      within(await card('This week')).getByText('The plan has finished.'),
    ).toBeInTheDocument()
    expect(
      within(await card('Recent sessions')).getAllByRole('listitem'),
    ).toHaveLength(4)
  })

  it('says when there are no sessions and no quiz answers yet', async () => {
    openOverview(ANNA, {
      today: OVERVIEW.today,
      week: OVERVIEW.week,
      recentSessions: [],
    })

    expect(
      within(await card('Recent sessions')).getByText('No sessions yet.'),
    ).toBeInTheDocument()
    expect(
      within(await card('Quiz answers')).getByText('No quiz answers yet.'),
    ).toBeInTheDocument()
  })

  // a 5xx is retried with a pause by design, so the failure here is one that is not
  it('offers to try again when the overview cannot be loaded', async () => {
    const user = userEvent.setup()
    let failing = true
    const working = backend(ANNA, OVERVIEW)
    openOverview(ANNA, OVERVIEW, (request) =>
      failing && new URL(request.url).pathname.endsWith('/overview')
        ? problem(429, { status: 429, code: 'request.too-many' })
        : working(request),
    )

    const retry = await screen.findByRole('button', { name: 'Try again' })
    // the progress has its own answer, so it stays
    expect(
      await screen.findByRole('region', { name: 'Progress' }),
    ).toBeInTheDocument()
    failing = false
    await user.click(retry)

    expect(await card('This week')).toBeInTheDocument()
  })

  it('speaks Polish to a trainer who reads Polish', async () => {
    await i18n.changeLanguage('pl')
    openOverview()

    const recent = within(await card('Ostatnie sesje'))
    expect(recent.getAllByRole('listitem')[1]).toHaveTextContent(
      'Serie: 15/15 · RPE 8,2',
    )
    expect(recent.getAllByRole('listitem')[2]).toHaveTextContent('Zablokowana')
    expect(
      within(await card('Ten tydzień')).getAllByText('Dzień odpoczynku'),
    ).toHaveLength(4)
    expect(await card('Odpowiedzi z quizu')).toBeInTheDocument()
  })
})
