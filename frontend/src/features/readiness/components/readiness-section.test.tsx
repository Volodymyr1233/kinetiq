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
const checkedIn = () => json({ date: DATE, open: true, checkIn: CHECK_IN })
const RECOVERY = {
  ...CHECK_IN,
  sleep: 2,
  soreness: 4,
  stress: 3,
  score: 33,
  band: 'RECOVERY_DAY',
}

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

    expect(await screen.findByRole('heading', { name: 'Ready' })).toHaveFocus()
    expect(screen.getByText('Readiness 83: Ready')).toBeInTheDocument()
    const puts = requests.filter((r) => r.method === 'PUT')
    expect(puts).toHaveLength(1)
    expect(await puts[0].json()).toEqual({ sleep: 4, soreness: 2, stress: 1 })
  })

  it('shows the saved readiness instead of the check-in', async () => {
    stubApi(() => json({ date: DATE, open: true, checkIn: CHECK_IN }))
    renderPage(<ReadinessSection />)

    expect(
      await screen.findByRole('heading', { name: 'Ready' }),
    ).not.toHaveFocus()
    expect(screen.getByText('83')).toBeInTheDocument()
    expect(
      screen.getByText('Sleep 4 · soreness 2 · stress 1'),
    ).toBeInTheDocument()
    expect(screen.queryByRole('radiogroup')).toBeNull()
    expect(screen.queryByText('Readiness 83: Ready')).toBeNull()
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
    expect(
      await screen.findByRole('heading', { name: 'Ready' }),
    ).toBeInTheDocument()
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

  it('says why and keeps focus when the first check-in comes after training started', async () => {
    const user = userEvent.setup()
    let started = false
    stubApi((request) => {
      if (request.method === 'PUT') {
        started = true
        return problem(409, { code: 'check-in.training-started' })
      }
      return json({ date: DATE, open: !started })
    })
    renderPage(<ReadinessSection />)

    await checkIn(user)

    const message = await screen.findByText(
      'Training has started, so today’s answers can no longer change.',
    )
    await waitFor(() => expect(screen.queryByRole('radiogroup')).toBeNull())
    expect(message.closest('[role=alert]')).toHaveFocus()
  })

  describe('changing the answers', () => {
    async function openChange(reply: Reply) {
      const user = userEvent.setup()
      const requests = stubApi(reply)
      renderPage(<ReadinessSection />)
      await user.click(
        await screen.findByRole('button', { name: 'Change answers' }),
      )
      return { user, requests }
    }

    const puts = (requests: Request[]) =>
      requests.filter((r) => r.method === 'PUT')

    it('opens the check-in with the saved answers chosen, and saves nothing until Done', async () => {
      const { user, requests } = await openChange((request) =>
        request.method === 'PUT'
          ? json({ date: DATE, open: true, checkIn: RECOVERY })
          : checkedIn(),
      )

      expect(
        screen.getByRole('heading', { name: 'How do you feel today?' }),
      ).toHaveFocus()
      expect(value('Sleep', 4)).toHaveAttribute('aria-checked', 'true')
      expect(value('Soreness', 2)).toHaveAttribute('aria-checked', 'true')
      expect(value('Stress', 1)).toHaveAttribute('aria-checked', 'true')
      expect(screen.queryByRole('button', { name: 'Skip' })).toBeNull()

      await user.click(value('Sleep', 2))
      await user.click(value('Soreness', 4))
      await user.click(value('Stress', 3))
      expect(puts(requests)).toHaveLength(0)

      await user.click(screen.getByRole('button', { name: 'Done' }))

      expect(
        await screen.findByRole('heading', { name: 'Recovery day' }),
      ).toHaveFocus()
      expect(screen.getByText('33')).toBeInTheDocument()
      expect(puts(requests)).toHaveLength(1)
      expect(await puts(requests)[0].json()).toEqual({
        sleep: 2,
        soreness: 4,
        stress: 3,
      })
    })

    it('keeps the saved answers on Cancel, without sending anything', async () => {
      const { user, requests } = await openChange(checkedIn)

      await user.click(value('Sleep', 1))
      await user.click(screen.getByRole('button', { name: 'Cancel' }))

      expect(screen.getByRole('heading', { name: 'Ready' })).toHaveFocus()
      expect(
        screen.getByText('Sleep 4 · soreness 2 · stress 1'),
      ).toBeInTheDocument()
      expect(puts(requests)).toHaveLength(0)
    })

    it('returns the same card on Done with nothing changed', async () => {
      const { user, requests } = await openChange(checkedIn)

      await user.click(screen.getByRole('button', { name: 'Done' }))

      expect(screen.getByRole('heading', { name: 'Ready' })).toHaveFocus()
      expect(puts(requests)).toHaveLength(0)
    })

    it('shows the card without a way to change it when training started meanwhile', async () => {
      let started = false
      const { user } = await openChange((request) => {
        if (request.method === 'PUT') {
          started = true
          return problem(409, { code: 'check-in.training-started' })
        }
        return json({
          date: DATE,
          open: !started,
          checkIn: CHECK_IN,
        })
      })

      await user.click(value('Sleep', 2))
      await user.click(screen.getByRole('button', { name: 'Done' }))

      expect(
        await screen.findByText('Answers are fixed once training has started.'),
      ).toBeInTheDocument()
      expect(
        screen.getByText(
          'Training has started, so today’s answers can no longer change.',
        ),
      ).toBeInTheDocument()
      expect(
        screen.queryByRole('button', { name: 'Change answers' }),
      ).toBeNull()
      expect(
        screen.getByText('Sleep 4 · soreness 2 · stress 1'),
      ).toBeInTheDocument()
    })
  })
})
