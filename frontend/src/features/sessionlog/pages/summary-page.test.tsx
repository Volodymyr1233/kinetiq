import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, describe, expect, it } from 'vitest'
import i18n from '@/lib/i18n'
import { WeightUnitContext, type WeightUnit } from '@/lib/weight'
import { json, problem, stubApi, type Reply } from '@/test/page'
import { SummaryPage } from './summary-page'

const ID = '00000000-0000-4000-a000-000000000001'

const summary = (extra: object = {}) => ({
  sessionLogId: ID,
  name: 'Lower body',
  date: '2026-11-07',
  startedAt: new Date().toISOString(),
  setsLogged: 3,
  setsPlanned: 4,
  volumeKg: 1512.5,
  averageRpe: 8.5,
  minutes: 52,
  ...extra,
})

function openSummary(reply: Reply, unit: WeightUnit = 'KG') {
  const requests = stubApi(reply)
  const router = createMemoryRouter(
    [
      {
        path: '/app/summary/:id',
        Component: () => (
          <SummaryPage
            sessionId={ID}
            trainerName="Marek"
            notFound={<p>Not found page</p>}
          />
        ),
      },
      { path: '/app/session', Component: () => <p>The logger</p> },
    ],
    { initialEntries: [`/app/summary/${ID}`] },
  )
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <WeightUnitContext value={unit}>
        <RouterProvider router={router} />
      </WeightUnitContext>
    </QueryClientProvider>,
  )
  return requests
}

const noteBox = () => screen.findByRole('textbox', { name: 'Note for Marek' })

describe('the session done screen', () => {
  afterEach(() => i18n.changeLanguage('en'))

  it('shows the figures as received', async () => {
    openSummary(() => json(summary()))

    expect(
      await screen.findByRole('heading', { name: 'Lower body done' }),
    ).toBeInTheDocument()
    expect(screen.getByText('3 of 4')).toBeInTheDocument()
    expect(screen.getByText('1,512.5 kg')).toBeInTheDocument()
    expect(screen.getByText('8.5')).toBeInTheDocument()
    expect(screen.getByText('52 min')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to today' })).toHaveAttribute(
      'href',
      '/app/today',
    )
    expect(screen.getByRole('link', { name: 'Message Marek' })).toHaveAttribute(
      'href',
      '/app/messages',
    )
  })

  it('converts the volume for a client in pounds', async () => {
    openSummary(() => json(summary({ volumeKg: 100 })), 'LB')

    expect(await screen.findByText('220.5 lb')).toBeInTheDocument()
  })

  it('shows a dash, not zero, when no volume counts', async () => {
    openSummary(() => json(summary({ volumeKg: undefined })))

    expect(await screen.findByText('—')).toBeInTheDocument()
    expect(screen.queryByText(/0 kg/)).not.toBeInTheDocument()
  })

  it('formats the figures for Polish', async () => {
    await i18n.changeLanguage('pl')
    openSummary(() => json(summary()))

    expect(await screen.findByText('1512,5 kg')).toBeInTheDocument()
    expect(screen.getByText('8,5')).toBeInTheDocument()
    expect(screen.getByText('Czas sesji')).toBeInTheDocument()
  })

  it('saves a note, confirms it, and clears it when saved empty', async () => {
    const requests = openSummary((request) =>
      request.method === 'PUT'
        ? new Response(null, { status: 204 })
        : json(summary()),
    )
    const user = userEvent.setup()
    const box = await noteBox()

    await user.type(box, '  Felt strong ')
    await user.click(screen.getByRole('button', { name: 'Save note' }))

    expect(await screen.findByText('Note saved')).toBeInTheDocument()
    const put = requests.find((request) => request.method === 'PUT')
    expect(await put?.json()).toEqual({ text: '  Felt strong ' })

    await user.clear(box)
    await user.click(screen.getByRole('button', { name: 'Save note' }))
    expect(requests.filter((request) => request.method === 'PUT')).toHaveLength(
      2,
    )
  })

  it('does not offer to save an unchanged note, or one of 501 characters', async () => {
    openSummary(() => json(summary({ note: 'Tired' })))
    const user = userEvent.setup()
    const box = await noteBox()
    const save = screen.getByRole('button', { name: 'Save note' })

    expect(box).toHaveValue('Tired')
    expect(save).toBeDisabled()

    await user.clear(box)
    await user.click(box)
    await user.paste('a'.repeat(501))

    expect(save).toBeDisabled()
    expect(
      screen.getByText(/A note can have up to 500 characters/),
    ).toBeInTheDocument()
  })

  it('makes the note read-only when the session is locked', async () => {
    openSummary(() =>
      json(
        summary({
          startedAt: new Date(Date.now() - 25 * 3600 * 1000).toISOString(),
        }),
      ),
    )

    expect(await noteBox()).toHaveAttribute('readonly')
    expect(
      screen.getByText(/This session is locked. The note can no longer/),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Save note' }),
    ).not.toBeInTheDocument()
  })

  it('shows the not-found page for a session that is not the clients', async () => {
    openSummary(() =>
      problem(404, { status: 404, code: 'session-log.not-found' }),
    )

    expect(await screen.findByText('Not found page')).toBeInTheDocument()
  })

  it('goes to the logger for a session that is not finished', async () => {
    openSummary(() =>
      problem(409, { status: 409, code: 'session-log.not-finished' }),
    )

    expect(await screen.findByText('The logger')).toBeInTheDocument()
  })

  it('offers to try again when the summary cannot be read', async () => {
    let calls = 0
    openSummary(() =>
      ++calls === 1
        ? problem(500, { status: 500, code: 'internal.error' })
        : json(summary()),
    )
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Try again' }))

    expect(
      await screen.findByRole('heading', { name: 'Lower body done' }),
    ).toBeInTheDocument()
  })
})
