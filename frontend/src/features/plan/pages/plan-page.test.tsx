import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Toaster } from 'sonner'
import { describe, expect, it } from 'vitest'
import type { components } from '@/lib/api/schema'
import type { Exercise } from '@/lib/exercise-library'
import { json, problem, renderAppAt, SIGNED_IN } from '@/test/page'
import type { Plan } from '../api/plans'

type Client = components['schemas']['ClientResponse']

const WAITING: Client = {
  id: '00000000-0000-4000-8000-000000000005',
  name: 'Ola Kowalska',
  goal: 'GET_STRONGER',
  planState: 'PLAN_TO_REVIEW',
}
const NO_QUIZ: Client = {
  id: '00000000-0000-4000-8000-000000000003',
  name: 'Jan Nowak',
  planState: 'NO_QUIZ',
}

const exercise = (
  name: string,
  category: Exercise['category'],
  measure: Exercise['measure'] = 'REPS',
): Exercise => ({
  id: `id-${name}`,
  name,
  category,
  equipment: '',
  measure,
  sets: 3,
  amount: measure === 'SECONDS' ? 45 : 10,
  restSeconds: 60,
  cues: '',
  plansUsing: 0,
  starter: true,
})
const LIBRARY = [
  exercise('Back squat', 'LEGS'),
  exercise('Bench press', 'CHEST'),
  exercise('Plank', 'CORE', 'SECONDS'),
]

const DRAFT: Plan = {
  status: 'DRAFT',
  goal: 'LOSE_FAT',
  weeks: 8,
  version: 3,
  sessions: [
    {
      name: 'Lower',
      day: 'MONDAY',
      entries: [
        {
          exerciseId: 'id-Back squat',
          exerciseName: 'Back squat',
          measure: 'REPS',
          sets: 3,
          amount: 5,
          loadKg: 100,
          rpe: 8,
          restSeconds: 120,
        },
      ],
    },
    { name: 'Upper', day: 'THURSDAY', entries: [] },
  ],
}

const SIX_SESSIONS: Plan = {
  ...DRAFT,
  sessions: (
    [
      'MONDAY',
      'TUESDAY',
      'WEDNESDAY',
      'THURSDAY',
      'FRIDAY',
      'SATURDAY',
    ] as const
  ).map((day) => ({ name: day, day, entries: [] })),
}

function backend(client: Client, plan: Plan | null, sent: object[] = []) {
  return async (request: Request) => {
    const { pathname, searchParams } = new URL(request.url)
    if (pathname === `/api/v1/clients/${client.id}`) return json(client)
    if (pathname === `/api/v1/clients/${client.id}/plan`) {
      if (request.method === 'PUT') {
        const body = await request.json()
        sent.push(body)
        const stored: Plan = {
          status: 'DRAFT',
          goal: body.goal,
          weeks: body.weeks,
          version: 0,
          sessions: body.sessions.map((session: Plan['sessions'][number]) => ({
            ...session,
            entries: session.entries.map((entry) => ({
              ...entry,
              exerciseName: entry.exerciseId.replace('id-', ''),
              measure: 'REPS',
            })),
          })),
        }
        return json(stored)
      }
      return plan
        ? json(plan)
        : problem(404, { status: 404, code: 'plan.not-found' })
    }
    if (pathname === '/api/v1/exercises') {
      const name = searchParams.get('name')?.toLowerCase() ?? ''
      const category = searchParams.get('category')
      const exercises = LIBRARY.filter(
        (entry) =>
          entry.name.toLowerCase().includes(name) &&
          (!category || entry.category === category),
      )
      return json({ total: LIBRARY.length, exercises })
    }
    return problem(404, { status: 404, code: 'request.not-found' })
  }
}

function open(client: Client, reply: (request: Request) => Promise<Response>) {
  return renderAppAt(
    `/trainer/clients/${client.id}/plan`,
    SIGNED_IN.TRAINER,
    reply,
  )
}

async function startBuilding(user: ReturnType<typeof userEvent.setup>) {
  await user.click(await screen.findByRole('button', { name: 'Build plan' }))
  await user.click(screen.getByRole('button', { name: 'Add session' }))
}

describe('PlanPage', () => {
  it('says the quiz comes first, asks nothing about a plan and offers no way to build', async () => {
    const { requests } = open(NO_QUIZ, backend(NO_QUIZ, null))

    expect(
      await screen.findByText(
        'Jan Nowak hasn’t answered the quiz yet. You can build a plan once they have.'.replace(
          '’',
          "'",
        ),
      ),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Build plan' }),
    ).not.toBeInTheDocument()
    expect(
      requests.map((request) => new URL(request.url).pathname),
    ).not.toContain(`/api/v1/clients/${NO_QUIZ.id}/plan`)
  })

  it('offers to build a plan when there is none', async () => {
    open(WAITING, backend(WAITING, null))

    expect(await screen.findByText('No plan yet.')).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Build plan' }),
    ).toBeInTheDocument()
  })

  it('shows a message and "Try again" when the plan cannot be loaded', async () => {
    const user = userEvent.setup()
    let failing = true
    const healthy = backend(WAITING, DRAFT)
    open(WAITING, async (request) =>
      failing && new URL(request.url).pathname.endsWith('/plan')
        ? problem(429, { status: 429, code: 'request.too-many' })
        : healthy(request),
    )

    await screen.findByRole('button', { name: 'Try again' })
    failing = false
    await user.click(screen.getByRole('button', { name: 'Try again' }))

    expect(
      await screen.findByRole('tab', { name: /Lower/ }),
    ).toBeInTheDocument()
  })

  it('opens a stored plan on its first session, with its letter, name and day', async () => {
    open(WAITING, backend(WAITING, DRAFT))

    const tabs = await screen.findAllByRole('tab', { name: /·/ })
    expect(tabs.map((tab) => tab.textContent)).toEqual([
      'ALower· Mon',
      'BUpper· Thu',
    ])
    expect(tabs[0]).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByLabelText('Session name')).toHaveValue('Lower')
    expect(screen.getByRole('cell', { name: 'Back squat' })).toBeInTheDocument()
    expect(screen.getByText('5 reps')).toBeInTheDocument()
  })

  it('starts a new plan from the quiz’s goal with 12 weeks, stores nothing and has nothing to save', async () => {
    const user = userEvent.setup()
    const { requests } = open(WAITING, backend(WAITING, null))

    await user.click(await screen.findByRole('button', { name: 'Build plan' }))

    expect(screen.getByLabelText('Goal')).toHaveTextContent('Get stronger')
    expect(screen.getByLabelText('Length')).toHaveValue('12')
    expect(
      screen.getByText('No sessions yet. Add the first one.'),
    ).toBeVisible()
    expect(screen.getByText('No changes to save.')).toBeVisible()
    expect(screen.getByRole('button', { name: 'Save plan' })).toBeDisabled()
    expect(requests.map((request) => request.method)).not.toContain('PUT')
  })

  it('gives a new session the first free day, selects it and moves focus to its name', async () => {
    const user = userEvent.setup()
    open(WAITING, backend(WAITING, DRAFT))
    await screen.findByRole('tab', { name: /Lower/ })

    await user.click(screen.getByRole('button', { name: 'Add session' }))

    const added = screen.getByRole('tab', { name: /New session/ })
    expect(added).toHaveAttribute('aria-selected', 'true')
    expect(added).toHaveTextContent('Tue')
    expect(screen.getByLabelText('Session name')).toHaveFocus()
    expect(screen.getByLabelText('Session name')).toHaveValue('')
  })

  it('does not let a seventh session be added and says why', async () => {
    open(WAITING, backend(WAITING, SIX_SESSIONS))

    expect(
      await screen.findByRole('button', { name: 'Add session' }),
    ).toBeDisabled()
    expect(
      screen.getByText('A plan holds up to 6 sessions.'),
    ).toBeInTheDocument()
  })

  it('searches and filters the library in the picker', async () => {
    const user = userEvent.setup()
    open(WAITING, backend(WAITING, null))
    await startBuilding(user)

    await user.click(screen.getByRole('button', { name: 'Add exercise' }))
    const dialog = within(await screen.findByRole('dialog'))
    expect(
      await dialog.findByRole('button', { name: 'Add Back squat' }),
    ).toBeInTheDocument()

    await user.type(dialog.getByLabelText('Search exercises'), 'pla')
    await waitFor(() =>
      expect(
        dialog.queryByRole('button', { name: 'Add Back squat' }),
      ).not.toBeInTheDocument(),
    )
    expect(
      dialog.getByRole('button', { name: 'Add Plank' }),
    ).toBeInTheDocument()

    await user.clear(dialog.getByLabelText('Search exercises'))
    await user.click(dialog.getByRole('radio', { name: 'Chest' }))
    await waitFor(() =>
      expect(
        dialog.queryByRole('button', { name: 'Add Plank' }),
      ).not.toBeInTheDocument(),
    )
    expect(
      dialog.getByRole('button', { name: 'Add Bench press' }),
    ).toBeInTheDocument()

    await user.type(dialog.getByLabelText('Search exercises'), 'nothing')
    expect(await dialog.findByText('No exercises match')).toBeInTheDocument()
  })

  it('says what went wrong in the picker and tries again', async () => {
    const user = userEvent.setup()
    let failing = true
    const healthy = backend(WAITING, null)
    open(WAITING, async (request) =>
      failing && new URL(request.url).pathname === '/api/v1/exercises'
        ? problem(429, { status: 429, code: 'request.too-many' })
        : healthy(request),
    )
    await startBuilding(user)

    await user.click(screen.getByRole('button', { name: 'Add exercise' }))
    const dialog = within(await screen.findByRole('dialog'))
    await dialog.findByRole('button', { name: 'Try again' })
    failing = false
    await user.click(dialog.getByRole('button', { name: 'Try again' }))

    expect(
      await dialog.findByRole('button', { name: 'Add Plank' }),
    ).toBeInTheDocument()
  })

  it('adds a row with the exercise’s defaults, stays open, announces it and returns focus when done', async () => {
    const user = userEvent.setup()
    open(WAITING, backend(WAITING, null))
    await startBuilding(user)

    const trigger = screen.getByRole('button', { name: 'Add exercise' })
    await user.click(trigger)
    const dialog = within(await screen.findByRole('dialog'))
    await user.click(await dialog.findByRole('button', { name: 'Add Plank' }))

    expect(dialog.getByRole('status')).toHaveTextContent(
      'Plank added to New session.',
    )
    await user.click(dialog.getByRole('button', { name: 'Add Back squat' }))
    await user.click(dialog.getByRole('button', { name: 'Done' }))

    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    )
    expect(trigger).toHaveFocus()
    const rows = screen.getAllByRole('row')
    // a timed exercise shows seconds, and every row starts at RPE 8 with no load
    expect(within(rows[1]).getByText('45 s')).toBeInTheDocument()
    expect(within(rows[2]).getByText('10 reps')).toBeInTheDocument()
    expect(within(rows[1]).getAllByRole('cell')[4]).toHaveTextContent('8')
    expect(within(rows[1]).getAllByRole('cell')[3]).toHaveTextContent('')
    expect(screen.getByText('Unsaved changes')).toBeVisible()
  })

  it('sends the whole plan on a first save, without a version, and confirms it', async () => {
    const user = userEvent.setup()
    const sent: object[] = []
    render(<Toaster />)
    open(WAITING, backend(WAITING, null, sent))
    await startBuilding(user)
    await user.type(screen.getByLabelText('Session name'), '  Lower  ')
    await user.click(screen.getByRole('button', { name: 'Add exercise' }))
    const dialog = within(await screen.findByRole('dialog'))
    await user.click(await dialog.findByRole('button', { name: 'Add Plank' }))
    await user.click(dialog.getByRole('button', { name: 'Done' }))

    await user.click(screen.getByRole('button', { name: 'Save plan' }))

    await screen.findByText('Plan saved')
    expect(sent).toEqual([
      {
        goal: 'GET_STRONGER',
        weeks: 12,
        sessions: [
          {
            name: 'Lower',
            day: 'MONDAY',
            entries: [
              {
                exerciseId: 'id-Plank',
                sets: 3,
                amount: 45,
                rpe: 8,
                restSeconds: 60,
              },
            ],
          },
        ],
      },
    ])
    expect(screen.getByText('No changes to save.')).toBeVisible()
  })
})
