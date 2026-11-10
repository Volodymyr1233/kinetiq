import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import {
  json,
  quizStatus,
  renderAppAt,
  SIGNED_IN,
  type Reply,
} from '@/test/page'

const CLIENT = SIGNED_IN.CLIENT.id
const MESSAGES = `/api/v1/conversations/${CLIENT}/messages`
const SESSION = '00000000-0000-4000-a000-000000000001'
const SET_LOG = '00000000-0000-4000-b000-000000000001'
const CHIP = /Lower body · .+ · Back squat, set 1 · RPE 9/

const loggedSet = (number: number, id: string) => ({
  number,
  target: { loadKg: 100, amount: 5, rpe: 8 },
  log: { id, loadKg: 100, amount: 5, rpe: 9, loggedAt: '2026-11-07T10:05:00Z' },
})

const server: Reply = quizStatus('PLAN_PUBLISHED', async (request) => {
  const { pathname } = new URL(request.url)
  if (pathname === '/api/v1/session-logs') {
    return json({
      id: SESSION,
      date: '2026-11-07',
      name: 'Lower body',
      startedAt: new Date().toISOString(),
      entries: [
        {
          position: 0,
          exerciseId: '00000000-0000-4000-9100-000000000011',
          exerciseName: 'Back squat',
          measure: 'REPS',
          restSeconds: 120,
          sets: [
            loggedSet(1, SET_LOG),
            loggedSet(2, '00000000-0000-4000-b000-000000000002'),
          ],
        },
      ],
    })
  }
  if (pathname === '/api/v1/conversations') {
    return json({
      conversations: [
        { clientId: CLIENT, name: 'Marek Nowak', unreadCount: 0 },
      ],
    })
  }
  if (pathname === MESSAGES && request.method === 'GET') {
    return json({ messages: [], hasMore: false })
  }
  if (pathname === MESSAGES && request.method === 'POST') {
    const { id, text } = await request.json()
    return json({ id, text, mine: true, sentAt: new Date().toISOString() }, 201)
  }
  return new Response(null, { status: 204 })
})

const sent = (requests: Request[]) =>
  Promise.all(
    requests
      .filter(
        (request) =>
          request.method === 'POST' &&
          new URL(request.url).pathname === MESSAGES,
      )
      .map((request) => request.clone().json()),
  )

const ask = (set: number) =>
  screen.findByRole('button', {
    name: `Ask your trainer about set ${set} of Back squat`,
  })

const REMOVE = 'Remove the set from this message'

describe('LoggerRoute', () => {
  it('opens the conversation over the logger with the set on the next message, and sends it with that message only', async () => {
    const user = userEvent.setup()
    const { requests } = renderAppAt('/app/session', SIGNED_IN.CLIENT, server)

    await user.click(await ask(1))

    const sheet = await screen.findByRole('dialog', { name: 'Messages' })
    const field = await within(sheet).findByRole('textbox', {
      name: 'Message Marek',
    })
    expect(within(sheet).getByText(CHIP)).toBeInTheDocument()
    await waitFor(() => expect(field).toHaveFocus())
    expect(
      within(sheet).getByRole('button', { name: 'Send message' }),
    ).toBeDisabled()

    await user.type(field, 'Was this too heavy?{Enter}')

    await waitFor(async () =>
      expect(await sent(requests)).toMatchObject([
        { text: 'Was this too heavy?', aboutSetLogId: SET_LOG },
      ]),
    )
    expect(
      within(sheet).queryByRole('button', { name: REMOVE }),
    ).not.toBeInTheDocument()

    await user.type(field, 'Thanks{Enter}')

    await waitFor(async () => expect(await sent(requests)).toHaveLength(2))
    expect((await sent(requests))[1]).not.toHaveProperty('aboutSetLogId')
  })

  it('sends an ordinary message after the set is removed', async () => {
    const user = userEvent.setup()
    const { requests } = renderAppAt('/app/session', SIGNED_IN.CLIENT, server)
    await user.click(await ask(1))
    const sheet = await screen.findByRole('dialog', { name: 'Messages' })

    await user.click(await within(sheet).findByRole('button', { name: REMOVE }))
    await user.type(
      within(sheet).getByRole('textbox', { name: 'Message Marek' }),
      'Hello{Enter}',
    )

    await waitFor(async () => expect(await sent(requests)).toHaveLength(1))
    expect((await sent(requests))[0]).not.toHaveProperty('aboutSetLogId')
    expect(within(sheet).queryByText(CHIP)).not.toBeInTheDocument()
  })

  it('leaves the session as it was when the conversation closes, and keeps what was not sent for the next time', async () => {
    const user = userEvent.setup()
    renderAppAt('/app/session', SIGNED_IN.CLIENT, server)
    await user.click(
      await screen.findByRole('button', {
        name: /^Change set 1 of Back squat/,
      }),
    )
    expect(
      screen.getByRole('button', { name: 'Update set' }),
    ).toBeInTheDocument()

    await user.click(await ask(1))
    const sheet = await screen.findByRole('dialog', { name: 'Messages' })
    await within(sheet).findByText(CHIP)
    await user.type(
      within(sheet).getByRole('textbox', { name: 'Message Marek' }),
      'Half written',
    )
    await user.click(within(sheet).getByRole('button', { name: 'Close' }))

    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    )
    // the sheet has no trigger, so the row's button gets the focus back
    expect(await ask(1)).toHaveFocus()
    expect(
      screen.getByRole('heading', { name: 'Lower body' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Update set' }),
    ).toBeInTheDocument()

    await user.click(await ask(2))

    // the text stays, and the set asked about now takes the place of the one before
    const again = await screen.findByRole('dialog', { name: 'Messages' })
    expect(
      await within(again).findByText(/Back squat, set 2 · RPE 9/),
    ).toBeInTheDocument()
    expect(within(again).queryByText(CHIP)).not.toBeInTheDocument()
    expect(
      within(again).getByRole('textbox', { name: 'Message Marek' }),
    ).toHaveValue('Half written')
  })
})
