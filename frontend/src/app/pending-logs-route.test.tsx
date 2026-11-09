import { onlineManager } from '@tanstack/react-query'
import { act, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { pendingLogsKey } from '@/lib/pending-logs'
import {
  json,
  quizStatus,
  renderAppAt,
  SIGNED_IN,
  type Reply,
} from '@/test/page'

const SESSION = '00000000-0000-4000-a000-000000000001'
const SET_LOG = '00000000-0000-4000-b000-000000000001'
const OTHER = '00000000-0000-4000-8000-000000000099'

const session = () =>
  json(
    {
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
          restSeconds: 0,
          sets: [{ number: 1, target: { loadKg: 100, amount: 5, rpe: 8 } }],
        },
      ],
    },
    201,
  )

// the server stores a set and answers it as it was sent
function server(): { reply: Reply; puts: string[] } {
  const puts: string[] = []
  const reply: Reply = async (request) => {
    const { pathname } = new URL(request.url)
    if (pathname === '/api/v1/session-logs') return session()
    if (pathname === '/api/v1/session-states')
      return json({ today: '2026-11-07', sessions: [] })
    if (request.method === 'PUT' && pathname.includes('/set-logs/')) {
      puts.push(pathname.split('/').pop()!)
      const body = await request.clone().json()
      return json({
        id: puts.at(-1),
        loggedAt: '2026-11-07T10:05:00Z',
        ...body,
      })
    }
    return new Response(null, { status: 204 })
  }
  return { reply, puts }
}

const kept = () =>
  JSON.stringify({
    timestamp: Date.now(),
    buster: '',
    clientState: {
      queries: [],
      mutations: [
        {
          mutationKey: ['log-set', SESSION, 0, 1],
          scope: { id: `log-set:${SET_LOG}` },
          state: {
            status: 'pending',
            isPaused: true,
            submittedAt: Date.now(),
            variables: {
              sessionId: SESSION,
              setLogId: SET_LOG,
              body: { entryPosition: 0, setNumber: 1, amount: 5, rpe: 8 },
            },
          },
        },
      ],
    },
  })

describe('the logs kept on the phone, wherever the client opens the app', () => {
  beforeEach(() => localStorage.clear())
  afterEach(() => {
    onlineManager.setOnline(true)
    localStorage.clear()
  })

  it('keeps a set logged without a connection in the logger, which sits outside the tabs', async () => {
    const { reply, puts } = server()
    renderAppAt(
      '/app/session',
      SIGNED_IN.CLIENT,
      quizStatus('PLAN_PUBLISHED', reply),
    )
    const tick = await screen.findByRole('button', {
      name: 'Log set 1 of Back squat as planned',
    })

    act(() => onlineManager.setOnline(false))
    await userEvent.click(tick)

    await waitFor(() =>
      expect(
        localStorage.getItem(pendingLogsKey(SIGNED_IN.CLIENT.id)),
      ).not.toBeNull(),
    )

    // sent when the connection returns, so no send is left waiting for the next test (the query client is shared)
    act(() => onlineManager.setOnline(true))
    await waitFor(() => expect(puts).toHaveLength(1))
  })

  it('sends a kept set when the client opens the app on a tab, and removes the logs of another account', async () => {
    localStorage.setItem(pendingLogsKey(SIGNED_IN.CLIENT.id), kept())
    localStorage.setItem(pendingLogsKey(OTHER), kept())
    const { reply, puts } = server()

    renderAppAt(
      '/app/today',
      SIGNED_IN.CLIENT,
      quizStatus('PLAN_PUBLISHED', reply),
    )

    await waitFor(() => expect(puts).toEqual([SET_LOG]))
    await waitFor(() =>
      expect(localStorage.getItem(pendingLogsKey(OTHER))).toBeNull(),
    )
    await waitFor(() =>
      expect(
        localStorage.getItem(pendingLogsKey(SIGNED_IN.CLIENT.id)),
      ).toBeNull(),
    )
    expect(puts).toHaveLength(1)
  })
})
