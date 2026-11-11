import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it } from 'vitest'
import i18n from '@/lib/i18n'
import type { components } from '@/lib/api/schema'
import { json, problem, renderAppAt, SIGNED_IN, type Reply } from '@/test/page'
import type { ClientSession, ClientSessionLog } from '../api/client-training'

type Client = components['schemas']['ClientResponse']

const ANNA: Client = {
  id: '00000000-0000-4000-8000-000000000003',
  name: 'Anna Kowalska',
  goal: 'BUILD_MUSCLE',
  planState: 'PUBLISHED',
}

const WEDNESDAY_LOG = '00000000-0000-4000-9300-000000000004'
const MONDAY_LOG = '00000000-0000-4000-9300-000000000002'
const FRIDAY_LOG = '00000000-0000-4000-9300-000000000001'

const SESSIONS: ClientSession[] = [
  {
    date: '2026-11-04',
    day: 'WEDNESDAY',
    name: 'Lower body',
    state: 'IN_PROGRESS',
    locked: false,
    setsLogged: 1,
    setsPlanned: 2,
    averageRpe: 9,
    sessionLogId: WEDNESDAY_LOG,
  },
  {
    date: '2026-11-02',
    day: 'MONDAY',
    name: 'Upper body',
    state: 'DONE',
    locked: false,
    setsLogged: 3,
    setsPlanned: 3,
    averageRpe: 8,
    sessionLogId: MONDAY_LOG,
  },
  {
    date: '2026-10-30',
    day: 'FRIDAY',
    name: 'Full body',
    state: 'DONE',
    locked: true,
    setsLogged: 1,
    setsPlanned: 1,
    averageRpe: 8,
    sessionLogId: FRIDAY_LOG,
  },
  {
    date: '2026-10-28',
    day: 'WEDNESDAY',
    name: 'Lower body',
    state: 'MISSED',
    locked: false,
    setsLogged: 0,
    setsPlanned: 2,
  },
]

const squat = (
  sets: ClientSessionLog['entries'][number]['sets'],
): ClientSessionLog['entries'][number] => ({
  position: 0,
  exerciseId: '00000000-0000-4000-9100-000000000001',
  exerciseName: 'Back squat',
  measure: 'REPS',
  restSeconds: 120,
  sets,
})

const logged = (loadKg: number | undefined, amount: number, rpe: number) => ({
  id: crypto.randomUUID(),
  loadKg,
  amount,
  rpe,
  loggedAt: '2026-11-04T08:10:00Z',
})

const DETAILS: Record<string, ClientSessionLog> = {
  [WEDNESDAY_LOG]: {
    ...SESSIONS[0],
    sessionLogId: WEDNESDAY_LOG,
    state: 'IN_PROGRESS',
    startedAt: '2026-11-04T08:00:00Z',
    entries: [
      squat([
        {
          number: 1,
          target: { loadKg: 100, amount: 5, rpe: 8 },
          log: logged(102.5, 5, 9),
        },
        { number: 2, target: { loadKg: 100, amount: 5, rpe: 8 } },
      ]),
    ],
  },
  [MONDAY_LOG]: {
    ...SESSIONS[1],
    sessionLogId: MONDAY_LOG,
    state: 'DONE',
    startedAt: '2026-11-02T17:00:00Z',
    finishedAt: '2026-11-02T17:48:00Z',
    minutes: 48,
    note: 'Knee felt fine.\n<b>Last set</b> was hard.',
    entries: [
      squat([
        {
          number: 1,
          target: { loadKg: 100, amount: 5, rpe: 8 },
          log: logged(100, 5, 8),
        },
        {
          number: 2,
          target: { loadKg: 100, amount: 5, rpe: 8 },
          log: logged(100, 4, 7),
        },
      ]),
      {
        position: 1,
        exerciseId: '00000000-0000-4000-9100-000000000026',
        exerciseName: 'Plank',
        measure: 'SECONDS',
        restSeconds: 60,
        sets: [
          {
            number: 1,
            target: { amount: 45, rpe: 7 },
            log: logged(undefined, 50, 7),
          },
        ],
      },
    ],
  },
  [FRIDAY_LOG]: {
    ...SESSIONS[2],
    sessionLogId: FRIDAY_LOG,
    state: 'DONE',
    startedAt: '2026-10-30T17:00:00Z',
    finishedAt: '2026-10-30T17:50:00Z',
    minutes: 50,
    entries: [
      squat([
        {
          number: 1,
          target: { loadKg: 100, amount: 5, rpe: 8 },
          log: logged(100, 5, 8),
        },
      ]),
    ],
  },
}

function backend(sessions: ClientSession[] = SESSIONS): Reply {
  return (request) => {
    const { pathname } = new URL(request.url)
    const base = `/api/v1/clients/${ANNA.id}`
    if (pathname === base) return json(ANNA)
    if (pathname === `${base}/session-logs`) return json({ sessions })
    const id = pathname.split('/').at(-1) ?? ''
    if (pathname === `${base}/session-logs/${id}` && DETAILS[id])
      return json(DETAILS[id])
    return problem(404, { status: 404, code: 'client.not-found' })
  }
}

function openLogs(
  reply: Reply = backend(),
  account: object = SIGNED_IN.TRAINER,
  search = '',
) {
  return renderAppAt(
    `/trainer/clients/${ANNA.id}/logs${search}`,
    account,
    reply,
  )
}

const marked = () =>
  screen
    .queryAllByRole('row')
    .filter((row) => row.getAttribute('aria-current') === 'true')

const sessionList = async () =>
  within(await screen.findByRole('list', { name: 'Sessions' }))

describe('ClientLogsTab', () => {
  afterEach(() => i18n.changeLanguage('en'))

  it('shows the session and marks the set an address names, as a message about it links to', async () => {
    openLogs(backend(), SIGNED_IN.TRAINER, `?session=${MONDAY_LOG}&set=0-2`)

    expect(
      await screen.findByRole('heading', { name: 'Upper body · Mon, Nov 2' }),
    ).toBeInTheDocument()
    await screen.findByRole('table')
    expect(marked()).toHaveLength(1)
    expect(marked()[0]).toHaveTextContent(/Back squat.*2/)
    expect(marked()[0]).toHaveFocus()
    expect(
      (await sessionList()).getByRole('button', { name: /^Upper body/ }),
    ).toHaveAttribute('aria-current', 'true')
  })

  it('shows the newest session when the address names an unknown one', async () => {
    openLogs(backend(), SIGNED_IN.TRAINER, '?session=nothing&set=0-1')

    expect(
      await screen.findByRole('heading', { name: 'Lower body · Wed, Nov 4' }),
    ).toBeInTheDocument()
    await screen.findByRole('table')
    expect(marked()).toHaveLength(0)
  })

  it('drops the mark when another session is chosen', async () => {
    const { router } = openLogs(
      backend(),
      SIGNED_IN.TRAINER,
      `?session=${MONDAY_LOG}&set=0-2`,
    )
    await screen.findByRole('table')

    await userEvent.click(
      (await sessionList()).getByRole('button', { name: /^Full body/ }),
    )

    expect(
      await screen.findByRole('heading', { name: 'Full body · Fri, Oct 30' }),
    ).toBeInTheDocument()
    await screen.findByRole('table')
    expect(router.state.location.search).toBe(`?session=${FRIDAY_LOG}`)
    expect(marked()).toHaveLength(0)
  })

  it('lists every session newest first, each labelled, and shows the newest', async () => {
    openLogs()

    const buttons = (await sessionList()).getAllByRole('button')
    expect(buttons.map((button) => button.textContent)).toEqual([
      'Lower bodyWed, Nov 4 · RPE 9.0In progress',
      'Upper bodyMon, Nov 2 · RPE 8.0Done',
      'Full bodyFri, Oct 30 · RPE 8.0Locked',
      'Lower bodyWed, Oct 28 · No setsMissed',
    ])
    expect(buttons[0]).toHaveAttribute('aria-current', 'true')
    expect(
      await screen.findByRole('heading', { name: 'Lower body · Wed, Nov 4' }),
    ).toBeInTheDocument()
  })

  it('shows every planned set against its target, and the sets not logged yet', async () => {
    openLogs()

    expect(
      await screen.findByText('1 of 2 sets · average RPE 9.0'),
    ).toBeInTheDocument()
    const rows = screen.getAllByRole('row').slice(1)
    expect(rows.map((row) => row.textContent)).toEqual([
      'Back squat15 × 100102.559',
      'Back squat25 × 100Not logged',
    ])
    expect(screen.getByRole('columnheader', { name: 'kg' })).toBeInTheDocument()
    expect(
      screen.getByText(/^Anna can still change these logs until /),
    ).toBeInTheDocument()
  })

  it('shows another session when it is chosen, with seconds for a timed exercise', async () => {
    const user = userEvent.setup()
    openLogs()

    const list = await sessionList()
    await user.click(list.getByRole('button', { name: /Upper body/ }))

    expect(
      await screen.findByText('3 of 3 sets · 48 min · average RPE 8.0'),
    ).toBeInTheDocument()
    expect(list.getByRole('button', { name: /Upper body/ })).toHaveAttribute(
      'aria-current',
      'true',
    )
    expect(
      screen.getByRole('columnheader', { name: 'Reps / s' }),
    ).toBeInTheDocument()
    expect(screen.getAllByRole('row').at(-1)).toHaveTextContent(
      'Plank145 s–No load50 s7',
    )
  })

  it('shows the note the client wrote as plain text, and nothing for a session without one', async () => {
    const user = userEvent.setup()
    openLogs()

    expect(
      await screen.findByText('1 of 2 sets · average RPE 9.0'),
    ).toBeInTheDocument()
    expect(screen.queryByText(/^Note from/)).not.toBeInTheDocument()

    await user.click(
      (await sessionList()).getByRole('button', { name: /Upper body/ }),
    )

    expect(await screen.findByText('Note from Anna')).toBeInTheDocument()
    // the client's text is shown as typed, markup included, never as markup
    const note = screen.getByText(/Knee felt fine\./)
    expect(note.textContent).toBe('Knee felt fine.\n<b>Last set</b> was hard.')
    expect(note.querySelector('b')).toBeNull()
  })

  it('says nothing about changes once a log is locked', async () => {
    const user = userEvent.setup()
    openLogs()

    await user.click(
      (await sessionList()).getByRole('button', { name: /Full body/ }),
    )

    expect(
      await screen.findByText('1 of 1 sets · 50 min · average RPE 8.0'),
    ).toBeInTheDocument()
    expect(screen.queryByText(/can still change/)).not.toBeInTheDocument()
  })

  it('says a missed session was not started', async () => {
    const user = userEvent.setup()
    openLogs()

    await user.click(
      (await sessionList()).getByRole('button', { name: /No sets/ }),
    )

    expect(
      await screen.findByRole('heading', { name: 'Lower body · Wed, Oct 28' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Not started')).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('shows loads in pounds to a trainer who uses pounds', async () => {
    openLogs(backend(), { ...SIGNED_IN.TRAINER, units: 'LB' })

    expect(
      await screen.findByRole('columnheader', { name: 'lb' }),
    ).toBeInTheDocument()
    // 100 kg is 220.5 lb and 102.5 kg is 226 lb
    expect(screen.getAllByRole('row')[1]).toHaveTextContent(
      'Back squat15 × 220.522659',
    )
  })

  // a 5xx is retried with a pause by design, so the failure here is one that is not
  it('offers to try again when the sessions cannot be loaded', async () => {
    const user = userEvent.setup()
    let failing = true
    const working = backend()
    openLogs((request) =>
      failing && new URL(request.url).pathname.endsWith('/session-logs')
        ? problem(429, { status: 429, code: 'request.too-many' })
        : working(request),
    )

    const retry = await screen.findByRole('button', { name: 'Try again' })
    failing = false
    await user.click(retry)

    expect(await sessionList()).toBeTruthy()
  })

  it('speaks Polish to a trainer who reads Polish', async () => {
    await i18n.changeLanguage('pl')
    openLogs()

    const buttons = (
      await screen.findByRole('list', { name: 'Sesje' })
    ).querySelectorAll('button')
    expect(buttons[2]).toHaveTextContent('Zablokowana')
    expect(buttons[3]).toHaveTextContent('Brak serii')
    expect(
      await screen.findByText('Serie: 1 z 2 · średnie RPE 9,0'),
    ).toBeInTheDocument()
    expect(screen.getByText('Nie zapisano')).toBeInTheDocument()
  })
})
