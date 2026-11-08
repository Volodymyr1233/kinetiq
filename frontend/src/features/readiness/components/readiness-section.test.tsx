import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { json, problem, renderPage, stubApi, type Reply } from '@/test/page'
import { ReadinessSection } from './readiness-section'

const DATE = '2026-11-09'
const CHECK_IN = {
  sleep: 4,
  soreness: 2,
  stress: 1,
  score: 83,
  band: 'READY',
  savedAt: '2026-11-09T07:00:00Z',
}

const open = (date = DATE) => json({ date, open: true })

// answers GET with `today` and PUT with the saved check-in
function server(today: () => Response): Reply {
  return (request) =>
    request.method === 'PUT'
      ? json({ date: DATE, open: true, checkIn: CHECK_IN })
      : today()
}

const value = (question: string, number: number) =>
  within(screen.getByRole('radiogroup', { name: question })).getByRole(
    'radio',
    { name: String(number) },
  )

async function checkIn(user: ReturnType<typeof userEvent.setup>) {
  await screen.findByRole('heading', { name: 'How do you feel today?' })
  await user.click(value('Sleep', 4))
  await user.click(value('Soreness', 2))
  await user.click(value('Stress', 1))
}

describe('ReadinessSection', () => {
  beforeEach(() => localStorage.clear())
  afterEach(() => localStorage.clear())

  it('saves the three answers with one request once the third is chosen', async () => {
    const user = userEvent.setup()
    const requests = stubApi(server(open))
    renderPage(<ReadinessSection />)

    await screen.findByRole('heading', { name: 'How do you feel today?' })
    await user.click(value('Sleep', 4))
    await user.click(value('Soreness', 2))
    expect(requests.filter((r) => r.method === 'PUT')).toHaveLength(0)
    await user.click(value('Stress', 1))

    expect(await screen.findByText(/Readiness 83/)).toBeInTheDocument()
    const puts = requests.filter((r) => r.method === 'PUT')
    expect(puts).toHaveLength(1)
    expect(await puts[0].json()).toEqual({ sleep: 4, soreness: 2, stress: 1 })
  })

  it('shows the saved readiness instead of the check-in', async () => {
    stubApi(() => json({ date: DATE, open: true, checkIn: CHECK_IN }))
    renderPage(<ReadinessSection />)

    expect(await screen.findByText(/Readiness 83/)).toBeInTheDocument()
    expect(
      screen.getByText('Sleep 4 · soreness 2 · stress 1'),
    ).toBeInTheDocument()
    expect(screen.queryByRole('radiogroup')).toBeNull()
  })

  it('keeps the answers and sends them again after a failed save', async () => {
    const user = userEvent.setup()
    let fail = true
    const requests = stubApi((request) =>
      request.method === 'PUT' && fail
        ? problem(500, { code: 'internal.error' })
        : server(open)(request),
    )
    renderPage(<ReadinessSection />)

    await checkIn(user)
    expect(
      await screen.findByText(
        'Something went wrong on our side. Try again in a moment.',
      ),
    ).toBeInTheDocument()
    expect(value('Sleep', 4)).toHaveAttribute('aria-checked', 'true')

    fail = false
    await user.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByText(/Readiness 83/)).toBeInTheDocument()
    expect(requests.filter((r) => r.method === 'PUT')).toHaveLength(2)
  })

  it('skips without sending anything, for the rest of that day on this device', async () => {
    const user = userEvent.setup()
    const requests = stubApi(server(open))
    const { unmount } = renderPage(<ReadinessSection />)

    await screen.findByRole('heading', { name: 'How do you feel today?' })
    await user.click(value('Sleep', 4))
    await user.click(screen.getByRole('button', { name: 'Skip' }))

    expect(
      screen.getByText('Check-in skipped. Your session is as planned.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Check in' })).toHaveFocus()
    expect(requests.filter((r) => r.method === 'PUT')).toHaveLength(0)

    unmount()
    renderPage(<ReadinessSection />)
    expect(
      await screen.findByText('Check-in skipped. Your session is as planned.'),
    ).toBeInTheDocument()
  })

  it('offers the check-in again on another day', async () => {
    const user = userEvent.setup()
    stubApi(server(open))
    const { unmount } = renderPage(<ReadinessSection />)
    await user.click(await screen.findByRole('button', { name: 'Skip' }))
    unmount()

    stubApi(server(() => open('2026-11-10')))
    renderPage(<ReadinessSection />)

    expect(
      await screen.findByRole('heading', { name: 'How do you feel today?' }),
    ).toBeInTheDocument()
  })

  it('brings the check-in back with nothing chosen', async () => {
    const user = userEvent.setup()
    stubApi(server(open))
    renderPage(<ReadinessSection />)

    await screen.findByRole('heading', { name: 'How do you feel today?' })
    await user.click(value('Sleep', 4))
    await user.click(screen.getByRole('button', { name: 'Skip' }))
    await user.click(screen.getByRole('button', { name: 'Check in' }))

    expect(
      screen.getByRole('heading', { name: 'How do you feel today?' }),
    ).toHaveFocus()
    expect(screen.queryByRole('radio', { checked: true })).toBeNull()
  })

  it('shows nothing when no check-in can be given and none was', async () => {
    stubApi(() => json({ date: DATE, open: false }))
    const { container } = renderPage(<ReadinessSection />)

    await waitFor(() => expect(container).toBeEmptyDOMElement())
  })

  it('shows a failed read with a way to try again', async () => {
    stubApi(() => problem(500, { code: 'internal.error' }))
    renderPage(<ReadinessSection />)

    expect(
      await screen.findByText(
        'Something went wrong on our side. Try again in a moment.',
      ),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Try again' }),
    ).toBeInTheDocument()
  })
})
