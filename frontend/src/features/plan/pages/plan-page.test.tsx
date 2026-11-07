import { focusManager } from '@tanstack/react-query'
import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Toaster } from 'sonner'
import { afterEach, describe, expect, it, vi } from 'vitest'
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

  describe('while a plan is being drafted', () => {
    const DRAFTING: Client = { ...WAITING, planState: 'PLAN_DRAFTING' }
    const LINE =
      "Drafting a plan from Ola Kowalska's quiz answers. This can take a few minutes."

    afterEach(() => {
      vi.useRealTimers()
    })

    it('says so and offers no way to build, when there is no plan yet', async () => {
      open(DRAFTING, backend(DRAFTING, null))

      expect(await screen.findByText(LINE)).toHaveAttribute('role', 'status')
      expect(
        screen.queryByRole('button', { name: 'Build plan' }),
      ).not.toBeInTheDocument()
      expect(screen.queryByLabelText('Session name')).not.toBeInTheDocument()
      expect(
        screen.queryByRole('button', { name: 'Build again' }),
      ).not.toBeInTheDocument()
    })

    it('moves on to the editor and the new badge by itself when drafting ends', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
      let client = DRAFTING
      let plan: Plan | null = null
      open(DRAFTING, async (request) => backend(client, plan)(request))
      await screen.findByText(LINE)
      expect(screen.getByText('Plan being drafted')).toBeInTheDocument()

      client = { ...WAITING, planState: 'PLAN_TO_REVIEW' }
      plan = { ...DRAFT, automaticDraft: 'BUILT' }
      await act(async () => {
        await vi.advanceTimersByTimeAsync(5000)
      })

      expect(
        await screen.findByRole('tab', { name: /Lower/ }),
      ).toBeInTheDocument()
      expect(screen.queryByText(LINE)).not.toBeInTheDocument()
      expect(screen.getAllByRole('status')[0]).toHaveTextContent(
        'Plan to review',
      )
      expect(screen.getByRole('note')).toHaveTextContent(
        'Built automatically from the quiz answers.',
      )
    })

    it('does not ask while the tab is hidden', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
      const { requests } = open(DRAFTING, backend(DRAFTING, null))
      await screen.findByText(LINE)
      const hidden = vi.spyOn(document, 'hidden', 'get').mockReturnValue(true)
      const asks = () =>
        requests.filter(
          (request) =>
            new URL(request.url).pathname === `/api/v1/clients/${DRAFTING.id}`,
        ).length
      const asked = asks()

      await act(async () => {
        await vi.advanceTimersByTimeAsync(15000)
      })

      expect(asks()).toBe(asked)
      hidden.mockRestore()
    })

    it('says a save is refused during drafting and keeps the edits', async () => {
      const user = userEvent.setup()
      const healthy = backend(WAITING, null)
      open(WAITING, async (request) =>
        request.method === 'PUT'
          ? problem(409, { status: 409, code: 'plan.drafting' })
          : healthy(request),
      )
      await startBuilding(user)
      await user.type(await screen.findByLabelText('Session name'), 'Lower')

      await user.click(screen.getByRole('button', { name: 'Save plan' }))

      expect(
        await screen.findByText(
          'This plan is still being drafted. Try again in a few minutes.',
        ),
      ).toBeInTheDocument()
      expect(screen.getByLabelText('Session name')).toHaveValue('Lower')
    })

    it('keeps an editor open when drafting starts while the trainer builds by hand', async () => {
      const user = userEvent.setup()
      let client = WAITING
      open(WAITING, async (request) => backend(client, null)(request))
      await startBuilding(user)
      await user.type(await screen.findByLabelText('Session name'), 'Lower')

      client = DRAFTING
      act(() => {
        focusManager.setFocused(false)
        focusManager.setFocused(true)
      })

      expect(await screen.findByText('Plan being drafted')).toBeInTheDocument()
      expect(screen.queryByText(LINE)).not.toBeInTheDocument()
      expect(screen.getByLabelText('Session name')).toHaveValue('Lower')
      focusManager.setFocused(undefined)
    })
  })

  describe('building a draft again', () => {
    const ANSWERS = {
      goal: 'LOSE_FAT',
      experience: 'YEARS_1_TO_2',
      level: 'INTERMEDIATE',
      age: 29,
      heightCm: 168,
      weightKg: 62.5,
      likes: ['WEIGHTS'],
      daysPerWeek: 3,
      notes: 'Left knee gets stiff',
      submittedAt: '2026-11-05T10:00:00Z',
    }
    const BUILT: Plan = { ...DRAFT, automaticDraft: 'BUILT' }
    const LINE =
      "Building a new draft from Ola Kowalska's answers. This can take a few minutes."
    const KEPT = "Couldn't build a new draft. Your previous one is kept."

    afterEach(() => {
      vi.useRealTimers()
    })

    // the server's side: Build again starts a drafting, unless `refusal` says why not
    function server(refusal?: () => Response) {
      const state = {
        client: WAITING,
        plan: BUILT as Plan,
        posts: 0,
      }
      const reply = async (request: Request) => {
        const { pathname } = new URL(request.url)
        if (pathname === `/api/v1/clients/${WAITING.id}/quiz-answers`)
          return json(ANSWERS)
        if (pathname === `/api/v1/clients/${WAITING.id}/plan/drafting`) {
          state.posts++
          if (refusal) return refusal()
          state.client = { ...WAITING, planState: 'PLAN_DRAFTING' }
          return new Response(null, { status: 202 })
        }
        return backend(state.client, state.plan)(request)
      }
      return { state, reply }
    }

    async function accept(user: ReturnType<typeof userEvent.setup>) {
      await user.click(
        await screen.findByRole('button', { name: 'Build again' }),
      )
      await user.click(
        await screen.findByRole('button', { name: 'Replace draft' }),
      )
    }

    function drafted(state: { client: Client; plan: Plan }, plan: Plan) {
      state.client = { ...WAITING, planState: 'PLAN_TO_REVIEW' }
      state.plan = plan
    }

    it('asks first, with Cancel focused, and does nothing until accepted', async () => {
      const user = userEvent.setup()
      const { state, reply } = server()
      open(WAITING, reply)

      await user.click(
        await screen.findByRole('button', { name: 'Build again' }),
      )

      const dialog = await screen.findByRole('alertdialog', {
        name: "Build Ola Kowalska's plan again?",
      })
      expect(dialog).toHaveTextContent(
        'The current draft, including your edits, will be replaced by a new automatic one if it can be built.',
      )
      const cancel = within(dialog).getByRole('button', { name: 'Cancel' })
      expect(cancel).toHaveFocus()
      await user.click(cancel)
      expect(state.posts).toBe(0)
      expect(screen.getByRole('button', { name: 'Build again' })).toHaveFocus()
    })

    it('shows the drafting above the previous draft, which can be read but not changed', async () => {
      const user = userEvent.setup()
      const { state, reply } = server()
      open(WAITING, reply)

      await accept(user)

      expect(await screen.findByText(LINE)).toHaveAttribute('role', 'status')
      expect(state.posts).toBe(1)
      expect(screen.getByLabelText('Session name')).toBeDisabled()
      expect(screen.getByLabelText('Length')).toBeDisabled()
      expect(screen.getByRole('button', { name: 'Save plan' })).toBeDisabled()
      expect(
        screen.getByRole('button', { name: 'Publish plan' }),
      ).toBeDisabled()
      expect(
        screen.getByRole('button', { name: 'Build again' }),
      ).toHaveAttribute('aria-disabled', 'true')
      // the other sessions can still be read
      await user.click(screen.getByRole('tab', { name: /Upper/ }))
      expect(screen.getByLabelText('Session name')).toHaveValue('Upper')
    })

    it('moves on to the new draft by itself, with no message, when one was built', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      const { state, reply } = server()
      open(WAITING, reply)
      await accept(user)
      await screen.findByText(LINE)

      drafted(state, {
        ...BUILT,
        version: 4,
        sessions: [{ name: 'Full body', day: 'TUESDAY', entries: [] }],
      })
      await act(async () => {
        await vi.advanceTimersByTimeAsync(5000)
      })

      expect(
        await screen.findByRole('tab', { name: /Full body/ }),
      ).toBeInTheDocument()
      expect(screen.queryByText(LINE)).not.toBeInTheDocument()
      expect(screen.queryByText(KEPT)).not.toBeInTheDocument()
      expect(screen.getByLabelText('Session name')).toBeEnabled()
      expect(screen.getAllByRole('status')[0]).toHaveTextContent(
        'Plan to review',
      )
    })

    it('says the previous draft is kept, and offers Build again, when none could be built', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
      const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
      const { state, reply } = server()
      open(WAITING, reply)
      await accept(user)
      await screen.findByText(LINE)

      drafted(state, BUILT)
      await act(async () => {
        await vi.advanceTimersByTimeAsync(5000)
      })

      expect(await screen.findByRole('alert')).toHaveTextContent(KEPT)
      expect(screen.getByRole('tab', { name: /Lower/ })).toBeInTheDocument()
      expect(
        screen.getByRole('button', { name: 'Build again' }),
      ).toHaveAttribute('aria-disabled', 'false')
    })

    // with no drafting service a rebuild ends at once, before the tab ever sees it running
    it('says the previous draft is kept, when the drafting ended before the tab saw it', async () => {
      const user = userEvent.setup()
      const { reply } = server(() => new Response(null, { status: 202 }))
      open(WAITING, reply)

      await accept(user)

      expect(await screen.findByRole('alert')).toHaveTextContent(KEPT)
      expect(screen.getByLabelText('Session name')).toBeEnabled()
    })

    it('shows the new draft, when the drafting ended before the tab saw it', async () => {
      const user = userEvent.setup()
      const { state, reply } = server(() => {
        state.plan = {
          ...BUILT,
          version: 4,
          sessions: [{ name: 'Full body', day: 'TUESDAY', entries: [] }],
        }
        return new Response(null, { status: 202 })
      })
      open(WAITING, reply)

      await accept(user)

      expect(
        await screen.findByRole('tab', { name: /Full body/ }),
      ).toBeInTheDocument()
      expect(screen.queryByText(KEPT)).not.toBeInTheDocument()
    })

    it('says when it can be used again, and stays unavailable, once the day’s builds are used', async () => {
      const user = userEvent.setup()
      const { state, reply } = server(() =>
        problem(429, {
          status: 429,
          code: 'plan.rebuild-limit-reached',
          availableAt: '2026-11-08T09:30:00Z',
        }),
      )
      open(WAITING, reply)

      await accept(user)

      expect(
        await screen.findByText(
          /You've used today's automatic builds\. You can build again from .*2026/,
        ),
      ).toBeInTheDocument()
      const button = screen.getByRole('button', { name: 'Build again' })
      expect(button).toHaveAttribute('aria-disabled', 'true')
      await user.click(button)
      expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
      expect(state.posts).toBe(1)
    })

    it('moves to the drafting already running, when the request is refused for it', async () => {
      const user = userEvent.setup()
      const { state, reply } = server(() => {
        state.client = { ...WAITING, planState: 'PLAN_DRAFTING' }
        return problem(409, { status: 409, code: 'plan.drafting' })
      })
      open(WAITING, reply)

      await accept(user)

      expect(await screen.findByText(LINE)).toBeInTheDocument()
      expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    })

    it('offers no panel for a published plan', async () => {
      const { state, reply } = server()
      state.plan = {
        ...DRAFT,
        status: 'PUBLISHED',
        firstWeek: '2026-11-02',
        currentWeek: 1,
      }
      open(WAITING, reply)

      await screen.findByRole('tab', { name: /Lower/ })
      expect(
        screen.queryByRole('button', { name: 'Build again' }),
      ).not.toBeInTheDocument()
      expect(
        screen.queryByText("Ola Kowalska's answers"),
      ).not.toBeInTheDocument()
    })
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
    expect(
      screen.getByLabelText('Repetitions for Back squat, row 1'),
    ).toHaveValue('5')
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
    expect(screen.getByRole('button', { name: 'Save plan' })).toHaveAttribute(
      'aria-disabled',
      'true',
    )
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
      await dialog.findByRole('button', { name: /^Add Back squat, / }),
    ).toBeInTheDocument()

    await user.type(dialog.getByLabelText('Search exercises'), 'pla')
    await waitFor(() =>
      expect(
        dialog.queryByRole('button', { name: /^Add Back squat, / }),
      ).not.toBeInTheDocument(),
    )
    expect(
      dialog.getByRole('button', { name: /^Add Plank, / }),
    ).toBeInTheDocument()

    await user.clear(dialog.getByLabelText('Search exercises'))
    await user.click(dialog.getByRole('radio', { name: 'Chest' }))
    await waitFor(() =>
      expect(
        dialog.queryByRole('button', { name: /^Add Plank, / }),
      ).not.toBeInTheDocument(),
    )
    expect(
      dialog.getByRole('button', { name: /^Add Bench press, / }),
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
      await dialog.findByRole('button', { name: /^Add Plank, / }),
    ).toBeInTheDocument()
  })

  it('adds a row with the exercise’s defaults, stays open, announces it and returns focus when done', async () => {
    const user = userEvent.setup()
    open(WAITING, backend(WAITING, null))
    await startBuilding(user)

    const trigger = screen.getByRole('button', { name: 'Add exercise' })
    await user.click(trigger)
    const dialog = within(await screen.findByRole('dialog'))
    await user.click(
      await dialog.findByRole('button', { name: /^Add Plank, / }),
    )

    expect(dialog.getByRole('status')).toHaveTextContent(
      'Plank added to New session.',
    )
    await user.click(dialog.getByRole('button', { name: /^Add Back squat, / }))
    await user.click(dialog.getByRole('button', { name: 'Done' }))

    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    )
    expect(trigger).toHaveFocus()
    const rows = screen.getAllByRole('row')
    // a timed exercise shows seconds, and every row starts at RPE 8 with no load
    expect(
      within(rows[1]).getByLabelText('Seconds for Plank, row 1'),
    ).toHaveValue('45')
    expect(within(rows[1]).getByText('s')).toBeInTheDocument()
    expect(
      within(rows[2]).getByLabelText('Repetitions for Back squat, row 2'),
    ).toHaveValue('10')
    expect(within(rows[2]).getByText('reps')).toBeInTheDocument()
    expect(screen.getByLabelText('Target RPE for Plank, row 1')).toHaveValue(
      '8',
    )
    expect(screen.getByLabelText('Load in kg for Plank, row 1')).toHaveValue('')
    expect(screen.getByText('Unsaved changes')).toBeVisible()
  })

  it('stops the picker at the limit of a session, saying why, and still returns focus', async () => {
    const user = userEvent.setup()
    const entry = DRAFT.sessions[0].entries[0]
    open(
      WAITING,
      backend(WAITING, {
        ...DRAFT,
        sessions: [
          {
            name: 'Long',
            day: 'MONDAY',
            entries: Array.from({ length: 29 }, () => entry),
          },
        ],
      }),
    )
    const trigger = await screen.findByRole('button', { name: 'Add exercise' })
    await user.click(trigger)
    const dialog = within(await screen.findByRole('dialog'))

    await user.click(
      await dialog.findByRole('button', { name: /^Add Plank, / }),
    )
    await user.click(dialog.getByRole('button', { name: /^Add Plank, / }))

    expect(dialog.getByRole('status')).toHaveTextContent(
      'A session holds up to 30 exercises.',
    )
    await user.click(dialog.getByRole('button', { name: 'Done' }))
    await waitFor(() => expect(trigger).toHaveFocus())
    // the rows and their one header
    expect(screen.getAllByRole('row')).toHaveLength(31)

    await user.click(trigger)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
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
    await user.click(
      await dialog.findByRole('button', { name: /^Add Plank, / }),
    )
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
