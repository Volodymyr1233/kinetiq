import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { components } from '@/lib/api/schema'
import { json, problem, renderAppAt, SIGNED_IN } from '@/test/page'
import type { Plan } from '../api/plans'

type Client = components['schemas']['ClientResponse']

const OLA: Client = {
  id: '00000000-0000-4000-8000-000000000005',
  name: 'Ola Kowalska',
  goal: 'GET_STRONGER',
  planState: 'DRAFT',
}

const squat = {
  exerciseId: 'id-squat',
  exerciseName: 'Back squat',
  measure: 'REPS',
  sets: 3,
  amount: 5,
  loadKg: 100,
  rpe: 8,
  restSeconds: 120,
} as const
const plank = {
  exerciseId: 'id-plank',
  exerciseName: 'Plank',
  measure: 'SECONDS',
  sets: 3,
  amount: 45,
  rpe: 7,
  restSeconds: 60,
} as const

const DRAFT: Plan = {
  status: 'DRAFT',
  goal: 'LOSE_FAT',
  weeks: 8,
  version: 3,
  sessions: [
    { name: 'Lower', day: 'MONDAY', entries: [squat, plank] },
    { name: 'Upper', day: 'THURSDAY', entries: [plank] },
  ],
}

const PUBLISHED: Plan = {
  ...DRAFT,
  status: 'PUBLISHED',
  firstWeek: '2026-09-28',
  currentWeek: 6,
  weeks: 16,
}

type Calls = { saves: Plan[]; publishes: { version: number }[] }

/** A backend holding one plan; `refuse` can answer a call with a problem instead. */
function backend(
  start: Plan | null,
  refuse: (call: string) => Response | undefined = () => undefined,
) {
  let stored = start
  const calls: Calls = { saves: [], publishes: [] }
  const reply = async (request: Request) => {
    const { pathname } = new URL(request.url)
    const address = `/api/v1/clients/${OLA.id}`
    if (pathname === '/api/v1/clients') return json({ clients: [OLA] })
    if (pathname === address) return json(OLA)
    const refusal = refuse(`${request.method} ${pathname}`)
    if (refusal) return refusal
    if (pathname === `${address}/plan/publication`) {
      calls.publishes.push(await request.json())
      stored = {
        ...stored!,
        status: 'PUBLISHED',
        firstWeek: '2026-11-02',
        currentWeek: 1,
        version: stored!.version + 1,
      }
      return json(stored)
    }
    if (pathname === `${address}/plan` && request.method === 'PUT') {
      const body = await request.json()
      calls.saves.push(body)
      stored = {
        ...stored,
        // the first save makes the plan the trainer's
        automaticDraft: undefined,
        status: stored?.status ?? 'DRAFT',
        goal: body.goal,
        weeks: body.weeks,
        version: (stored?.version ?? -1) + 1,
        sessions: body.sessions.map((session: Plan['sessions'][number]) => ({
          ...session,
          entries: session.entries.map((entry) => ({
            ...entry,
            exerciseName:
              entry.exerciseId === 'id-squat' ? 'Back squat' : 'Plank',
            measure: entry.exerciseId === 'id-squat' ? 'REPS' : 'SECONDS',
          })),
        })),
      }
      return json(stored)
    }
    if (pathname === `${address}/plan`) {
      return stored
        ? json(stored)
        : problem(404, { status: 404, code: 'plan.not-found' })
    }
    if (pathname === '/api/v1/sign-ins/current') {
      return new Response(null, { status: 204 })
    }
    return problem(404, { status: 404, code: 'request.not-found' })
  }
  return { reply, calls }
}

function open(reply: (request: Request) => Promise<Response>) {
  return renderAppAt(
    `/trainer/clients/${OLA.id}/plan`,
    SIGNED_IN.TRAINER,
    reply,
  )
}

const setsOf = (name: string, row: number) =>
  screen.getByLabelText(`Sets for ${name}, row ${row}`)

async function retype(
  user: ReturnType<typeof userEvent.setup>,
  field: HTMLElement,
  text: string,
) {
  await user.clear(field)
  await user.type(field, text)
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('PlanEditor', () => {
  it('keeps edits when switching sessions and clears the note when an edit is undone', async () => {
    const user = userEvent.setup()
    open(backend(DRAFT).reply)
    await screen.findByRole('tab', { name: /Lower/ })

    await retype(user, setsOf('Back squat', 1), '4')
    expect(screen.getByText('Unsaved changes')).toBeVisible()

    await user.click(screen.getByRole('tab', { name: /Upper/ }))
    expect(screen.queryByLabelText(/Back squat/)).not.toBeInTheDocument()
    await user.click(screen.getByRole('tab', { name: /Lower/ }))

    expect(setsOf('Back squat', 1)).toHaveValue('4')
    await retype(user, setsOf('Back squat', 1), '3')
    expect(screen.getByText('No changes to save.')).toBeVisible()
  })

  it('keeps text as typed and shows the load in the trainer’s pounds, sending it unchanged when untouched', async () => {
    const user = userEvent.setup()
    const { reply, calls } = backend({
      ...DRAFT,
      sessions: [
        {
          ...DRAFT.sessions[0],
          entries: [{ ...squat, loadKg: 62.4 }],
        },
        DRAFT.sessions[1],
      ],
    })
    renderAppAt(
      `/trainer/clients/${OLA.id}/plan`,
      { ...SIGNED_IN.TRAINER, units: 'LB' },
      reply,
    )

    const load = await screen.findByLabelText(
      'Load in lb for Back squat, row 1',
    )
    expect(load).toHaveValue('137.6')
    await retype(user, setsOf('Back squat', 1), '4')
    await user.click(screen.getByRole('button', { name: 'Save plan' }))

    await waitFor(() => expect(calls.saves).toHaveLength(1))
    expect(calls.saves[0].sessions[0].entries[0].loadKg).toBe(62.4)
  })

  describe('leaving', () => {
    it('asks before another tab of the client, with focus on Stay, and Stay keeps the edits', async () => {
      const user = userEvent.setup()
      const { router } = open(backend(DRAFT).reply)
      await screen.findByRole('tab', { name: /Lower/ })
      await retype(user, setsOf('Back squat', 1), '4')

      await user.click(screen.getByRole('tab', { name: 'Logs' }))

      const dialog = await screen.findByRole('alertdialog', {
        name: 'Leave without saving?',
      })
      expect(within(dialog).getByRole('button', { name: 'Stay' })).toHaveFocus()
      await user.click(within(dialog).getByRole('button', { name: 'Stay' }))

      // on the selected tab: a tab that takes focus activates, which would ask again
      await waitFor(() =>
        expect(screen.getByRole('tab', { name: 'Plan' })).toHaveFocus(),
      )
      expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
      expect(router.state.location.pathname).toBe(
        `/trainer/clients/${OLA.id}/plan`,
      )
      expect(setsOf('Back squat', 1)).toHaveValue('4')
    })

    it('leaves when told to', async () => {
      const user = userEvent.setup()
      const { router } = open(backend(DRAFT).reply)
      await screen.findByRole('tab', { name: /Lower/ })
      await retype(user, setsOf('Back squat', 1), '4')

      await user.click(screen.getByRole('tab', { name: 'Logs' }))
      const dialog = await screen.findByRole('alertdialog')
      await user.click(within(dialog).getByRole('button', { name: 'Leave' }))

      await waitFor(() =>
        expect(router.state.location.pathname).toBe(
          `/trainer/clients/${OLA.id}/logs`,
        ),
      )
    })

    it('does not ask when nothing changed', async () => {
      const user = userEvent.setup()
      const { router } = open(backend(DRAFT).reply)
      await screen.findByRole('tab', { name: /Lower/ })

      await user.click(screen.getByRole('tab', { name: 'Logs' }))

      await waitFor(() =>
        expect(router.state.location.pathname).toBe(
          `/trainer/clients/${OLA.id}/logs`,
        ),
      )
      expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    })

    it('does not ask for an untouched new plan, and offers to build it again', async () => {
      const user = userEvent.setup()
      open(backend(null).reply)
      await user.click(
        await screen.findByRole('button', { name: 'Build plan' }),
      )

      await user.click(screen.getByRole('tab', { name: 'Logs' }))
      await waitFor(() =>
        expect(screen.getByRole('tab', { name: 'Logs' })).toHaveAttribute(
          'aria-selected',
          'true',
        ),
      )
      await user.click(screen.getByRole('tab', { name: 'Plan' }))

      expect(
        await screen.findByRole('button', { name: 'Build plan' }),
      ).toBeInTheDocument()
    })

    it('uses the browser’s warning on reload only while there are unsaved changes', async () => {
      const user = userEvent.setup()
      open(backend(DRAFT).reply)
      await screen.findByRole('tab', { name: /Lower/ })
      const reload = () => {
        const event = new Event('beforeunload', { cancelable: true })
        window.dispatchEvent(event)
        return event.defaultPrevented
      }

      expect(reload()).toBe(false)
      await retype(user, setsOf('Back squat', 1), '4')
      expect(reload()).toBe(true)
    })

    it('asks before signing out, and does not sign out on Stay', async () => {
      const user = userEvent.setup()
      const { requests } = open(backend(DRAFT).reply)
      await screen.findByRole('tab', { name: /Lower/ })
      await retype(user, setsOf('Back squat', 1), '4')

      await user.click(screen.getByRole('button', { name: 'Sign out' }))
      const dialog = await screen.findByRole('alertdialog')
      await user.click(within(dialog).getByRole('button', { name: 'Stay' }))

      await waitFor(() =>
        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument(),
      )
      expect(requests.map((request) => request.method)).not.toContain('DELETE')
      expect(setsOf('Back squat', 1)).toHaveValue('4')
    })

    it('signs out after Leave', async () => {
      const user = userEvent.setup()
      const { requests, router } = open(backend(DRAFT).reply)
      await screen.findByRole('tab', { name: /Lower/ })
      await retype(user, setsOf('Back squat', 1), '4')

      await user.click(screen.getByRole('button', { name: 'Sign out' }))
      const dialog = await screen.findByRole('alertdialog')
      await user.click(within(dialog).getByRole('button', { name: 'Leave' }))

      await waitFor(() =>
        expect(router.state.location.pathname).toBe('/sign-in'),
      )
      expect(requests.map((request) => request.method)).toContain('DELETE')
    })
  })

  describe('rows', () => {
    it('moves a row from its menu, announces it, keeps focus on its grip and sends the new order', async () => {
      const user = userEvent.setup()
      const { reply, calls } = backend(DRAFT)
      open(reply)
      await screen.findByRole('tab', { name: /Lower/ })

      await user.click(
        screen.getByRole('button', { name: 'Move Back squat, row 1' }),
      )
      expect(
        await screen.findByRole('menuitem', { name: 'Move up' }),
      ).toHaveAttribute('aria-disabled', 'true')
      await user.click(screen.getByRole('menuitem', { name: 'Move down' }))

      expect(screen.getByText('Back squat moved to row 2.')).toBeInTheDocument()
      await waitFor(() =>
        expect(
          screen.getByRole('button', { name: 'Move Back squat, row 2' }),
        ).toHaveFocus(),
      )
      await user.click(screen.getByRole('button', { name: 'Save plan' }))

      await waitFor(() => expect(calls.saves).toHaveLength(1))
      expect(
        calls.saves[0].sessions[0].entries.map((entry) => entry.exerciseId),
      ).toEqual(['id-plank', 'id-squat'])
    })

    it('opens the same menu from the keyboard', async () => {
      const user = userEvent.setup()
      open(backend(DRAFT).reply)
      await screen.findByRole('tab', { name: /Lower/ })

      screen.getByRole('button', { name: 'Move Plank, row 2' }).focus()
      await user.keyboard('{Enter}')
      await screen.findByRole('menu')
      await user.keyboard('{Enter}')

      expect(screen.getByText('Plank moved to row 1.')).toBeInTheDocument()
      await waitFor(() =>
        expect(
          screen.getByRole('button', { name: 'Move Plank, row 1' }),
        ).toHaveFocus(),
      )
    })

    it('moves a row dropped on another, past several, and announces it', async () => {
      const { reply, calls } = backend({
        ...DRAFT,
        sessions: [
          { ...DRAFT.sessions[0], entries: [squat, plank, squat] },
          DRAFT.sessions[1],
        ],
      })
      const user = userEvent.setup()
      open(reply)
      await screen.findByRole('tab', { name: /Lower/ })
      const grip = screen.getByRole('button', {
        name: 'Move Back squat, row 3',
      })
      const dataTransfer = { setData: vi.fn(), setDragImage: vi.fn() }

      fireEvent.dragStart(grip, { dataTransfer })
      const firstRow = screen.getAllByRole('row')[1]
      fireEvent.dragOver(firstRow, { dataTransfer })
      fireEvent.drop(firstRow, { dataTransfer })
      fireEvent.dragEnd(grip, { dataTransfer })

      expect(screen.getByText('Back squat moved to row 1.')).toBeInTheDocument()
      await user.click(screen.getByRole('button', { name: 'Save plan' }))
      await waitFor(() => expect(calls.saves).toHaveLength(1))
      expect(
        calls.saves[0].sessions[0].entries.map((entry) => entry.exerciseId),
      ).toEqual(['id-squat', 'id-squat', 'id-plank'])
    })

    it('leaves the order alone when a drag ends off the rows', async () => {
      open(backend(DRAFT).reply)
      await screen.findByRole('tab', { name: /Lower/ })
      const grip = screen.getByRole('button', { name: 'Move Plank, row 2' })

      fireEvent.dragStart(grip, {
        dataTransfer: { setData: vi.fn(), setDragImage: vi.fn() },
      })
      fireEvent.dragOver(screen.getAllByRole('row')[1], {
        dataTransfer: {},
      })
      fireEvent.dragEnd(grip)

      expect(
        screen.getByRole('button', { name: 'Move Plank, row 2' }),
      ).toBeInTheDocument()
    })

    it('asks before deleting, starting on Cancel, and returns there when cancelled', async () => {
      const user = userEvent.setup()
      open(backend(DRAFT).reply)
      await screen.findByRole('tab', { name: /Lower/ })
      const remove = screen.getByRole('button', {
        name: 'Delete Back squat, row 1',
      })

      await user.click(remove)
      const dialog = await screen.findByRole('alertdialog', {
        name: 'Delete "Back squat" from Lower?',
      })
      expect(
        within(dialog).getByRole('button', { name: 'Cancel' }),
      ).toHaveFocus()
      await user.click(within(dialog).getByRole('button', { name: 'Cancel' }))

      await waitFor(() => expect(remove).toHaveFocus())
      expect(setsOf('Back squat', 1)).toBeInTheDocument()
    })

    it('moves focus to the next row’s delete after deleting, then the previous, then Add exercise', async () => {
      const user = userEvent.setup()
      open(backend(DRAFT).reply)
      await screen.findByRole('tab', { name: /Lower/ })
      const confirmDelete = async (name: string) => {
        await user.click(screen.getByRole('button', { name }))
        const dialog = await screen.findByRole('alertdialog')
        await user.click(
          within(dialog).getByRole('button', { name: 'Delete exercise' }),
        )
        await waitFor(() =>
          expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument(),
        )
      }

      await confirmDelete('Delete Back squat, row 1')
      await waitFor(() =>
        expect(
          screen.getByRole('button', { name: 'Delete Plank, row 1' }),
        ).toHaveFocus(),
      )

      await confirmDelete('Delete Plank, row 1')
      await waitFor(() =>
        expect(
          screen.getByRole('button', { name: 'Add exercise' }),
        ).toHaveFocus(),
      )
      expect(
        screen.getByText('No exercises in this session yet.'),
      ).toBeInTheDocument()
    })
  })

  describe('sessions', () => {
    it('deletes an empty session without asking and selects the next', async () => {
      const user = userEvent.setup()
      open(
        backend({
          ...DRAFT,
          sessions: [
            { name: 'Empty', day: 'MONDAY', entries: [] },
            DRAFT.sessions[1],
          ],
        }).reply,
      )
      await screen.findByRole('tab', { name: /Empty/ })

      await user.click(screen.getByRole('button', { name: 'Delete session' }))

      expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
      const upper = await screen.findByRole('tab', { name: /Upper/ })
      expect(upper).toHaveAttribute('aria-selected', 'true')
      await waitFor(() => expect(upper).toHaveFocus())
    })

    it('asks before deleting a session with exercises and focuses the selected session after', async () => {
      const user = userEvent.setup()
      open(backend(DRAFT).reply)
      await screen.findByRole('tab', { name: /Lower/ })

      await user.click(screen.getByRole('button', { name: 'Delete session' }))
      const dialog = await screen.findByRole('alertdialog', {
        name: 'Delete Lower?',
      })
      expect(
        within(dialog).getByText('Its 2 exercises go with it.'),
      ).toBeInTheDocument()
      await user.click(
        within(dialog).getByRole('button', { name: 'Delete session' }),
      )

      const upper = await screen.findByRole('tab', { name: /Upper/ })
      await waitFor(() => expect(upper).toHaveFocus())
      expect(
        screen.queryByRole('tab', { name: /Lower/ }),
      ).not.toBeInTheDocument()
    })

    it('moves focus to "Add session" after deleting the last one', async () => {
      const user = userEvent.setup()
      open(
        backend({
          ...DRAFT,
          sessions: [{ name: 'Only', day: 'MONDAY', entries: [] }],
        }).reply,
      )
      await screen.findByRole('tab', { name: /Only/ })

      await user.click(screen.getByRole('button', { name: 'Delete session' }))

      await waitFor(() =>
        expect(
          screen.getByRole('button', { name: 'Add session' }),
        ).toHaveFocus(),
      )
    })
  })

  describe('validation', () => {
    it('marks a field on leaving it, and the message goes once it is valid', async () => {
      const user = userEvent.setup()
      open(backend(DRAFT).reply)
      await screen.findByRole('tab', { name: /Lower/ })
      const rpe = screen.getByLabelText('Target RPE for Back squat, row 1')

      await retype(user, rpe, '11')
      expect(rpe).toHaveAttribute('aria-invalid', 'false')
      await user.tab()

      expect(rpe).toHaveAttribute('aria-invalid', 'true')
      expect(rpe).toHaveAccessibleDescription(
        'Enter an RPE from 5 to 10, in whole steps.',
      )
      await retype(user, rpe, '9')
      expect(rpe).toHaveAttribute('aria-invalid', 'false')
    })

    it('blocks a save with an invalid RPE, focuses the summary, marks the session, and its link focuses the field', async () => {
      const user = userEvent.setup()
      const { reply, calls } = backend(DRAFT)
      open(reply)
      await screen.findByRole('tab', { name: /Lower/ })
      await retype(
        user,
        screen.getByLabelText('Target RPE for Back squat, row 1'),
        '11',
      )
      await user.click(screen.getByRole('tab', { name: /Upper/ }))

      await user.click(screen.getByRole('button', { name: 'Save plan' }))

      const link = await screen.findByRole('link', {
        name: 'A · Lower, Back squat: Enter an RPE from 5 to 10, in whole steps.',
      })
      expect(link.closest('[role="alert"]')).toHaveFocus()
      expect(calls.saves).toHaveLength(0)
      expect(screen.getByRole('tab', { name: /Lower/ })).toHaveTextContent(
        'This session has something to fix',
      )

      await user.click(link)

      const rpe = await screen.findByLabelText(
        'Target RPE for Back squat, row 1',
      )
      expect(screen.getByRole('tab', { name: /Lower/ })).toHaveAttribute(
        'aria-selected',
        'true',
      )
      await waitFor(() => expect(rpe).toHaveFocus())
      await retype(user, rpe, '9')
      expect(
        screen.queryByRole('link', { name: /RPE/ }),
      ).not.toBeInTheDocument()
    })

    it('saves an incomplete draft, but publishing shows the summary without a dialog', async () => {
      const user = userEvent.setup()
      const { reply, calls } = backend(null)
      open(reply)
      await user.click(
        await screen.findByRole('button', { name: 'Build plan' }),
      )
      await user.click(screen.getByRole('button', { name: 'Add session' }))
      await user.type(screen.getByLabelText('Session name'), 'Lower')

      await user.click(screen.getByRole('button', { name: 'Save plan' }))
      await waitFor(() => expect(calls.saves).toHaveLength(1))

      await user.click(screen.getByRole('button', { name: 'Publish plan' }))

      expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
      expect(
        await screen.findByRole('link', { name: 'Add at least 2 sessions.' }),
      ).toBeInTheDocument()
      const empty = screen.getByRole('link', {
        name: 'Add at least 1 exercise to A · Lower.',
      })
      await user.click(empty)
      await waitFor(() =>
        expect(
          screen.getByRole('button', { name: 'Add exercise' }),
        ).toHaveFocus(),
      )
      expect(calls.publishes).toHaveLength(0)
    })

    it('names the limit in the trainer’s unit for a load too heavy after conversion', async () => {
      const user = userEvent.setup()
      renderAppAt(
        `/trainer/clients/${OLA.id}/plan`,
        { ...SIGNED_IN.TRAINER, units: 'LB' },
        backend(DRAFT).reply,
      )
      const load = await screen.findByLabelText(
        'Load in lb for Back squat, row 1',
      )

      await retype(user, load, '2205')
      await user.tab()

      expect(load).toHaveAccessibleDescription(
        'Leave the load empty or enter 0.2 to 2204.4 lb, one decimal at most.',
      )
      await retype(user, load, '2204.4')
      expect(load).toHaveAttribute('aria-invalid', 'false')
      await retype(user, load, '0.1')
      expect(load).toHaveAttribute('aria-invalid', 'true')
    })

    it('translates a refusal by its code, keeps the edits and links what it can', async () => {
      const user = userEvent.setup()
      const { reply } = backend(PUBLISHED, (call) =>
        call.startsWith('PUT')
          ? problem(400, {
              status: 400,
              code: 'plan.weeks-before-current',
              currentWeek: 6,
              detail: 'internal text',
            })
          : undefined,
      )
      open(reply)
      await screen.findByRole('tab', { name: /Lower/ })
      const weeks = screen.getByLabelText('Length')
      await retype(user, weeks, '5')

      await user.click(screen.getByRole('button', { name: 'Save plan' }))

      const link = await screen.findByRole('link', {
        name: 'This plan is already in week 6. Enter 6 or more weeks.',
      })
      expect(screen.queryByText('internal text')).not.toBeInTheDocument()
      expect(weeks).toHaveValue('5')
      await user.click(link)
      await waitFor(() => expect(weeks).toHaveFocus())
      await retype(user, weeks, '6')
      expect(
        screen.queryByRole('link', { name: /week 6/ }),
      ).not.toBeInTheDocument()
    })

    it('shows a field refused by the server at its row', async () => {
      const user = userEvent.setup()
      const { reply } = backend(DRAFT, (call) =>
        call.startsWith('PUT')
          ? problem(400, {
              status: 400,
              code: 'validation.failed',
              errors: [{ field: 'sessions[0].entries[1].rpe', code: 'max' }],
            })
          : undefined,
      )
      open(reply)
      await screen.findByRole('tab', { name: /Lower/ })
      await retype(user, setsOf('Back squat', 1), '4')

      await user.click(screen.getByRole('button', { name: 'Save plan' }))

      expect(
        await screen.findByRole('link', {
          name: 'A · Lower, Plank: Enter an RPE from 5 to 10, in whole steps.',
        }),
      ).toBeInTheDocument()
      expect(screen.getByRole('tab', { name: /Lower/ })).toHaveTextContent(
        'This session has something to fix',
      )
    })
  })

  describe('a draft the system built or failed to build', () => {
    const BUILT_NOTE =
      'Built automatically from the quiz answers. Check the exercises and loads. Ola Kowalska sees the plan only after you publish it.'
    const FAILED: Plan = {
      ...DRAFT,
      automaticDraft: 'FAILED',
      sessions: [],
    }

    it('says a draft was built automatically, and the note goes for good on saving', async () => {
      const user = userEvent.setup()
      const { reply } = backend({ ...DRAFT, automaticDraft: 'BUILT' })
      open(reply)
      await screen.findByRole('tab', { name: /Lower/ })
      expect(screen.getByRole('note')).toHaveTextContent(BUILT_NOTE)

      await retype(user, setsOf('Back squat', 1), '4')
      expect(screen.getByRole('note')).toBeVisible()
      await user.click(screen.getByRole('button', { name: 'Save plan' }))

      await waitFor(() =>
        expect(screen.queryByRole('note')).not.toBeInTheDocument(),
      )
    })

    it('keeps the note when the save fails', async () => {
      const user = userEvent.setup()
      const { reply } = backend(
        { ...DRAFT, automaticDraft: 'BUILT' },
        (call) =>
          call.startsWith('PUT')
            ? problem(409, { status: 409, code: 'plan.changed' })
            : undefined,
      )
      open(reply)
      await screen.findByRole('tab', { name: /Lower/ })
      await retype(user, setsOf('Back squat', 1), '4')

      await user.click(screen.getByRole('button', { name: 'Save plan' }))

      await screen.findByText(/This plan was changed somewhere else/)
      expect(screen.getByRole('note')).toHaveTextContent(BUILT_NOTE)
    })

    // the panel beside the editor says it, and findByRole would fail on a second note above the sessions
    it('says drafting failed once, beside the editor, offers "Add session", and publishing lists what is missing', async () => {
      const user = userEvent.setup()
      const { reply, calls } = backend(FAILED)
      open(reply)

      expect(
        await screen.findByRole('button', { name: 'Add session' }),
      ).toBeVisible()
      expect(await screen.findByRole('note')).toHaveTextContent(
        "Couldn't build this plan automatically. Try again or build it yourself.",
      )

      await user.click(screen.getByRole('button', { name: 'Publish plan' }))

      await screen.findByText(/at least 2 sessions/i)
      expect(calls.publishes).toHaveLength(0)
    })

    it('shows no note on a plan with no mark', async () => {
      const { reply } = backend(DRAFT)
      open(reply)
      await screen.findByRole('tab', { name: /Lower/ })

      expect(screen.queryByRole('note')).not.toBeInTheDocument()
    })
  })

  describe('saving', () => {
    it('says the plan changed elsewhere and keeps the edits', async () => {
      const user = userEvent.setup()
      const { reply } = backend(DRAFT, (call) =>
        call.startsWith('PUT')
          ? problem(409, { status: 409, code: 'plan.changed' })
          : undefined,
      )
      open(reply)
      await screen.findByRole('tab', { name: /Lower/ })
      await retype(user, setsOf('Back squat', 1), '4')

      await user.click(screen.getByRole('button', { name: 'Save plan' }))

      const refusal = await screen.findByText(
        /This plan was changed somewhere else/,
      )
      await waitFor(() =>
        expect(refusal.closest('[role="alert"]')).toHaveFocus(),
      )
      expect(setsOf('Back squat', 1)).toHaveValue('4')
      expect(screen.getByText('Unsaved changes')).toBeVisible()
    })

    it('keeps the edits after a failed save and can try again', async () => {
      const user = userEvent.setup()
      let failing = true
      const { reply, calls } = backend(DRAFT, (call) =>
        failing && call.startsWith('PUT')
          ? problem(429, { status: 429, code: 'request.too-many' })
          : undefined,
      )
      open(reply)
      await screen.findByRole('tab', { name: /Lower/ })
      await retype(user, setsOf('Back squat', 1), '4')

      await user.click(screen.getByRole('button', { name: 'Save plan' }))
      await screen.findByText('Something went wrong. Try again.')
      expect(setsOf('Back squat', 1)).toHaveValue('4')

      failing = false
      await user.click(screen.getByRole('button', { name: 'Save plan' }))

      await waitFor(() => expect(calls.saves).toHaveLength(1))
      await waitFor(() =>
        expect(
          screen.queryByText('Something went wrong. Try again.'),
        ).not.toBeInTheDocument(),
      )
      expect(screen.getByText('No changes to save.')).toBeVisible()
    })

    it('sends the version it read, and the new one on the next save', async () => {
      const user = userEvent.setup()
      const { reply, calls } = backend(DRAFT)
      open(reply)
      await screen.findByRole('tab', { name: /Lower/ })

      await retype(user, setsOf('Back squat', 1), '4')
      await user.click(screen.getByRole('button', { name: 'Save plan' }))
      await screen.findByText('No changes to save.')
      await retype(user, setsOf('Back squat', 1), '5')
      await user.click(screen.getByRole('button', { name: 'Save plan' }))
      await waitFor(() => expect(calls.saves).toHaveLength(2))

      expect(calls.saves.map((save) => save.version)).toEqual([3, 4])
    })
  })

  describe('publishing', () => {
    it('asks, starting on Cancel, then publishes with the version it read and focuses Save plan', async () => {
      const user = userEvent.setup()
      const { reply, calls } = backend(DRAFT)
      open(reply)
      await screen.findByRole('tab', { name: /Lower/ })

      await user.click(screen.getByRole('button', { name: 'Publish plan' }))
      const dialog = await screen.findByRole('alertdialog', {
        name: 'Publish this plan for Ola Kowalska?',
      })
      expect(
        within(dialog).getByRole('button', { name: 'Cancel' }),
      ).toHaveFocus()
      expect(calls.publishes).toHaveLength(0)
      await user.click(
        within(dialog).getByRole('button', { name: 'Publish plan' }),
      )

      await screen.findByText('Week 1 of 8')
      expect(calls.publishes).toEqual([{ version: 3 }])
      expect(calls.saves).toHaveLength(0)
      expect(
        screen.queryByRole('button', { name: 'Publish plan' }),
      ).not.toBeInTheDocument()
      await waitFor(() =>
        expect(screen.getByRole('button', { name: 'Save plan' })).toHaveFocus(),
      )
    })

    it('returns focus to "Publish plan" when cancelled', async () => {
      const user = userEvent.setup()
      const { reply, calls } = backend(DRAFT)
      open(reply)
      await screen.findByRole('tab', { name: /Lower/ })
      const publish = screen.getByRole('button', { name: 'Publish plan' })

      await user.click(publish)
      await user.click(await screen.findByRole('button', { name: 'Cancel' }))

      await waitFor(() => expect(publish).toHaveFocus())
      expect(calls.publishes).toHaveLength(0)
    })

    it('saves unsaved changes first, then publishes the new version', async () => {
      const user = userEvent.setup()
      const { reply, calls } = backend(DRAFT)
      open(reply)
      await screen.findByRole('tab', { name: /Lower/ })
      await retype(user, setsOf('Back squat', 1), '4')

      await user.click(screen.getByRole('button', { name: 'Publish plan' }))
      await user.click(
        within(await screen.findByRole('alertdialog')).getByRole('button', {
          name: 'Publish plan',
        }),
      )

      await screen.findByText('Week 1 of 8')
      expect(calls.saves).toHaveLength(1)
      expect(calls.publishes).toEqual([{ version: 4 }])
    })

    it('leaves a saved draft and offers publishing again when the save succeeds and the publish fails', async () => {
      const user = userEvent.setup()
      let failing = true
      const { reply, calls } = backend(DRAFT, (call) =>
        failing && call.endsWith('/publication')
          ? problem(429, { status: 429, code: 'request.too-many' })
          : undefined,
      )
      open(reply)
      await screen.findByRole('tab', { name: /Lower/ })
      await retype(user, setsOf('Back squat', 1), '4')

      await user.click(screen.getByRole('button', { name: 'Publish plan' }))
      await user.click(
        within(await screen.findByRole('alertdialog')).getByRole('button', {
          name: 'Publish plan',
        }),
      )

      await screen.findByText('Something went wrong. Try again.')
      expect(calls.saves).toHaveLength(1)
      expect(screen.getByText('No changes to save.')).toBeVisible()
      expect(setsOf('Back squat', 1)).toHaveValue('4')

      failing = false
      await user.click(screen.getByRole('button', { name: 'Publish plan' }))
      await user.click(
        within(await screen.findByRole('alertdialog')).getByRole('button', {
          name: 'Publish plan',
        }),
      )
      await screen.findByText('Week 1 of 8')
      expect(calls.publishes).toEqual([{ version: 4 }])
    })

    it('publishes nothing when saving first is refused', async () => {
      const user = userEvent.setup()
      const { reply, calls } = backend(DRAFT, (call) =>
        call.startsWith('PUT')
          ? problem(409, { status: 409, code: 'plan.changed' })
          : undefined,
      )
      open(reply)
      await screen.findByRole('tab', { name: /Lower/ })
      await retype(user, setsOf('Back squat', 1), '4')

      await user.click(screen.getByRole('button', { name: 'Publish plan' }))
      await user.click(
        within(await screen.findByRole('alertdialog')).getByRole('button', {
          name: 'Publish plan',
        }),
      )

      await screen.findByText(/This plan was changed somewhere else/)
      expect(calls.publishes).toHaveLength(0)
      expect(setsOf('Back squat', 1)).toHaveValue('4')
    })

    it('says a stale publish was refused and publishes nothing', async () => {
      const user = userEvent.setup()
      const { reply } = backend(DRAFT, (call) =>
        call.endsWith('/publication')
          ? problem(409, { status: 409, code: 'plan.changed' })
          : undefined,
      )
      open(reply)
      await screen.findByRole('tab', { name: /Lower/ })

      await user.click(screen.getByRole('button', { name: 'Publish plan' }))
      await user.click(
        within(await screen.findByRole('alertdialog')).getByRole('button', {
          name: 'Publish plan',
        }),
      )

      await screen.findByText(/This plan was changed somewhere else/)
      expect(screen.getByRole('button', { name: 'Publish plan' })).toBeVisible()
    })
  })

  describe('a published plan', () => {
    it('offers only Save plan, says the change applies from today and shows the week', async () => {
      open(backend(PUBLISHED).reply)

      expect(await screen.findByText('Week 6 of 16')).toBeInTheDocument()
      expect(
        screen.getByText(
          "Saving applies the change from today. Logged sets don't change.",
        ),
      ).toBeInTheDocument()
      expect(
        screen.getByRole('button', { name: 'Save plan' }),
      ).toBeInTheDocument()
      expect(
        screen.queryByRole('button', { name: 'Publish plan' }),
      ).not.toBeInTheDocument()
    })

    it('says a plan past its last week has finished, and still saves', async () => {
      const user = userEvent.setup()
      const { reply, calls } = backend({
        ...PUBLISHED,
        weeks: 4,
        currentWeek: 7,
      })
      open(reply)

      expect(
        await screen.findByText('Finished after week 4'),
      ).toBeInTheDocument()
      await retype(user, setsOf('Back squat', 1), '4')
      await user.click(screen.getByRole('button', { name: 'Save plan' }))

      await waitFor(() => expect(calls.saves).toHaveLength(1))
    })

    it('checks a published plan is complete on saving', async () => {
      const user = userEvent.setup()
      const { reply, calls } = backend({
        ...PUBLISHED,
        sessions: [DRAFT.sessions[0]],
      })
      open(reply)
      await screen.findByRole('tab', { name: /Lower/ })
      await retype(user, setsOf('Back squat', 1), '4')

      await user.click(screen.getByRole('button', { name: 'Save plan' }))

      expect(
        await screen.findByRole('link', { name: 'Add at least 2 sessions.' }),
      ).toBeInTheDocument()
      expect(calls.saves).toHaveLength(0)
    })
  })
})
