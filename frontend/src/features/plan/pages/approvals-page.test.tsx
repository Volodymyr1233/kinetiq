import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { json, problem, renderPage, stubApi } from '@/test/page'
import type { DraftedPlan } from '../api/drafted-plans'
import { ApprovalsPage } from './approvals-page'

const hoursAgo = (hours: number) =>
  new Date(Date.now() - hours * 3_600_000).toISOString()

const OLA: DraftedPlan = {
  clientId: '00000000-0000-4000-8000-000000000003',
  clientName: 'Ola Kowalska',
  kind: 'FAILED',
  createdAt: hoursAgo(50),
  weeks: 12,
  sessionsPerWeek: 0,
}
const JAN: DraftedPlan = {
  clientId: '00000000-0000-4000-8000-000000000005',
  clientName: 'Jan Nowak',
  kind: 'BUILT',
  createdAt: hoursAgo(3),
  weeks: 1,
  sessionsPerWeek: 1,
}
const ANNA: DraftedPlan = {
  clientId: '00000000-0000-4000-8000-000000000007',
  clientName: 'Anna Wiśniewska',
  kind: 'SAVED',
  createdAt: hoursAgo(1),
  weeks: 8,
  sessionsPerWeek: 3,
}

function show(items: DraftedPlan[]) {
  const requests = stubApi(() => json({ items }))
  renderPage(<ApprovalsPage />)
  return requests
}

async function rows() {
  const list = await screen.findByRole('list')
  return within(list).getAllByRole('link')
}

describe('ApprovalsPage', () => {
  it('lists the drafts in the order received, each opening the client’s plan', async () => {
    show([OLA, JAN, ANNA])

    const links = await rows()
    expect(links.map((link) => link.getAttribute('href'))).toEqual([
      `/trainer/clients/${OLA.clientId}/plan`,
      `/trainer/clients/${JAN.clientId}/plan`,
      `/trainer/clients/${ANNA.clientId}/plan`,
    ])
    expect(links[0]).toHaveTextContent('Ola Kowalska')
    expect(links[1]).toHaveTextContent('Jan Nowak')
  })

  it('says the kind of each draft in words', async () => {
    show([OLA, JAN, ANNA])

    const [ola, jan, anna] = await rows()
    expect(ola).toHaveTextContent('Draft failed')
    expect(jan).toHaveTextContent('Plan to review')
    expect(anna).toHaveTextContent('Draft')
    expect(anna).not.toHaveTextContent('Draft failed')
  })

  it('says how long each draft has waited', async () => {
    show([OLA, JAN])

    const [ola, jan] = await rows()
    expect(ola).toHaveTextContent('2 days ago')
    expect(jan).toHaveTextContent('3 hr. ago')
  })

  it('gives the plan’s length and sessions with plural forms', async () => {
    show([JAN, ANNA])

    const [jan, anna] = await rows()
    expect(jan).toHaveTextContent('1 week · 1 session a week')
    expect(anna).toHaveTextContent('8 weeks · 3 sessions a week')
  })

  it('says nothing waits, with a way to the clients, when there are no drafts', async () => {
    show([])

    expect(await screen.findByText('Nothing to approve.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'View clients' })).toHaveAttribute(
      'href',
      '/trainer/clients',
    )
    expect(screen.queryByRole('list')).not.toBeInTheDocument()
  })

  it('shows the error and asks again on retry', async () => {
    const user = userEvent.setup()
    let fail = true
    const requests = stubApi(() =>
      fail
        ? problem(500, { status: 500, code: 'internal.error' })
        : json({ items: [JAN] }),
    )
    renderPage(<ApprovalsPage />)

    expect(await screen.findByRole('alert')).toBeInTheDocument()
    fail = false
    await user.click(screen.getByRole('button', { name: 'Try again' }))

    expect((await rows())[0]).toHaveTextContent('Jan Nowak')
    expect(requests).toHaveLength(2)
  })
})
