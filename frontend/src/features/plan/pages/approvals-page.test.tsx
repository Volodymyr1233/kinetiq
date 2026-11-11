import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it } from 'vitest'
import i18n from '@/lib/i18n'
import { json, problem, renderPage, stubApi } from '@/test/page'
import type { DraftedPlan } from '../api/drafted-plans'
import type { PlanChangeApproval } from '../api/plan-change-approvals'
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

const change = (
  id: string,
  clientName: string,
  patch: Partial<PlanChangeApproval> = {},
): PlanChangeApproval => ({
  id: `00000000-0000-4000-9600-0000000000${id}`,
  clientId: '00000000-0000-4000-8000-000000000009',
  clientName,
  title: `Title ${id}`,
  reason: `Reason ${id}`,
  state: 'PENDING',
  acceptedByTrainer: false,
  acceptedByClient: false,
  effectiveWeek: '2026-11-16',
  createdAt: hoursAgo(10),
  ...patch,
})

/** Answers each feed of the page with its own items. */
function show(drafts: DraftedPlan[], changes: PlanChangeApproval[] = []) {
  const requests = stubApi((request) =>
    new URL(request.url).pathname === '/api/v1/approvals/plan-changes'
      ? json({ items: changes })
      : json({ items: drafts }),
  )
  renderPage(<ApprovalsPage />)
  return requests
}

async function rows() {
  const list = await screen.findByRole('list')
  return within(list).getAllByRole('link')
}

describe('ApprovalsPage', () => {
  afterEach(() => i18n.changeLanguage('en'))

  it('lists the drafts newest first, each opening the client’s plan', async () => {
    show([OLA, JAN, ANNA])

    const links = await rows()
    expect(links.map((link) => link.getAttribute('href'))).toEqual([
      `/trainer/clients/${ANNA.clientId}/plan`,
      `/trainer/clients/${JAN.clientId}/plan`,
      `/trainer/clients/${OLA.clientId}/plan`,
    ])
    expect(links[2]).toHaveTextContent('Ola Kowalska')
    expect(links[1]).toHaveTextContent('Jan Nowak')
  })

  it('says the kind of each draft in words', async () => {
    show([OLA, JAN, ANNA])

    const [anna, jan, ola] = await rows()
    expect(ola).toHaveTextContent('Draft failed')
    expect(jan).toHaveTextContent('Plan to review')
    expect(anna).toHaveTextContent('Draft')
    expect(anna).not.toHaveTextContent('Draft failed')
  })

  it('says how long each draft has waited', async () => {
    show([OLA, JAN])

    const [jan, ola] = await rows()
    expect(ola).toHaveTextContent('2 days ago')
    expect(jan).toHaveTextContent('3 hr. ago')
  })

  it('gives the plan’s length and sessions with plural forms', async () => {
    show([JAN, ANNA])

    const [anna, jan] = await rows()
    expect(jan).toHaveTextContent('1 week · 1 session a week')
    expect(anna).toHaveTextContent('8 weeks · 3 sessions a week')
  })

  it('says nothing waits, with a way to the clients, when there is nothing at all', async () => {
    show([])

    expect(await screen.findByText('Nothing to approve.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'View clients' })).toHaveAttribute(
      'href',
      '/trainer/clients',
    )
    expect(screen.queryByRole('list')).not.toBeInTheDocument()
    expect(
      screen.getByRole('tab', { name: /Waiting for you\s*0/ }),
    ).toBeVisible()
  })

  it('shows the error and asks again on retry', async () => {
    const user = userEvent.setup()
    let fail = true
    const requests = stubApi((request) =>
      new URL(request.url).pathname === '/api/v1/approvals/plan-changes'
        ? json({ items: [] })
        : fail
          ? problem(500, { status: 500, code: 'internal.error' })
          : json({ items: [JAN] }),
    )
    renderPage(<ApprovalsPage />)

    expect(await screen.findByRole('alert')).toBeInTheDocument()
    fail = false
    await user.click(screen.getByRole('button', { name: 'Try again' }))

    expect((await rows())[0]).toHaveTextContent('Jan Nowak')
    // both feeds, then both again
    expect(requests).toHaveLength(4)
  })

  describe('plan changes', () => {
    const WAITING_FOR_YOU = change('01', 'Piotr Zieliński')
    const CLIENT_ACCEPTED = change('02', 'Anna Kowalska', {
      state: 'ACCEPTED_BY_ONE',
      acceptedByClient: true,
      createdAt: hoursAgo(11),
    })
    const WAITING_FOR_CLIENT = change('03', 'Tomasz Lewandowski', {
      state: 'ACCEPTED_BY_ONE',
      acceptedByTrainer: true,
    })
    const APPLIED = change('04', 'Michał Szymański', {
      state: 'APPLIED',
      acceptedByTrainer: true,
      acceptedByClient: true,
      endedAt: hoursAgo(30),
    })
    const DECLINED = change('05', 'Ola Dąbrowska', {
      state: 'DECLINED',
      declinedBy: 'CLIENT',
      declineReason: 'SCHEDULE',
      endedAt: hoursAgo(5),
    })
    const EXPIRED = change('06', 'Paweł Kozłowski', {
      state: 'EXPIRED',
      endedAt: hoursAgo(80),
    })
    const ALL = [
      WAITING_FOR_YOU,
      CLIENT_ACCEPTED,
      WAITING_FOR_CLIENT,
      APPLIED,
      DECLINED,
      EXPIRED,
    ]

    const tab = (name: RegExp) => screen.getByRole('tab', { name })

    it('puts what waits for the trainer, what waits for the client and what ended in their own tabs, with counts', async () => {
      const user = userEvent.setup()
      show([JAN], ALL)

      await screen.findByRole('list')
      expect(tab(/Waiting for you/)).toHaveTextContent('3')
      expect(tab(/Waiting for client/)).toHaveTextContent('1')
      expect(tab(/Decided/)).toHaveTextContent('3')
      // the draft and both plan changes that wait for the trainer, newest first
      expect((await rows()).map((row) => row.textContent)).toEqual([
        expect.stringContaining('Jan Nowak'),
        expect.stringContaining('Piotr Zieliński'),
        expect.stringContaining('Anna Kowalska'),
      ])

      await user.click(tab(/Waiting for client/))
      expect(await rows()).toHaveLength(1)
      expect((await rows())[0]).toHaveTextContent('Tomasz Lewandowski')

      await user.click(tab(/Decided/))
      expect((await rows()).map((row) => row.textContent)).toEqual([
        expect.stringContaining('Ola Dąbrowska'),
        expect.stringContaining('Michał Szymański'),
        expect.stringContaining('Paweł Kozłowski'),
      ])
    })

    it('shows the client, title, reason, badge and when it takes effect, and opens the detail', async () => {
      show([], [WAITING_FOR_YOU, CLIENT_ACCEPTED])

      const [first, second] = await rows()
      expect(first).toHaveTextContent('Piotr Zieliński')
      expect(first).toHaveTextContent('Title 01')
      expect(first).toHaveTextContent('Reason 01')
      expect(first).toHaveTextContent('Waiting for both')
      expect(first).toHaveTextContent('Takes effect Mon, Nov 16')
      expect(first).toHaveAttribute(
        'href',
        `/trainer/plan-changes/${WAITING_FOR_YOU.id}`,
      )
      expect(second).toHaveTextContent('Waiting for you')
    })

    it('says who the trainer waits for once they accepted', async () => {
      const user = userEvent.setup()
      show([], [WAITING_FOR_CLIENT])

      await screen.findByRole('tab', { name: /Waiting for client/ })
      await user.click(tab(/Waiting for client/))

      expect((await rows())[0]).toHaveTextContent('Waiting for Tomasz')
    })

    it('says how an ended one ended: applied, declined by whom and why, expired', async () => {
      const user = userEvent.setup()
      show([], [APPLIED, DECLINED, EXPIRED])
      await screen.findByRole('tab', { name: /Decided/ })
      await user.click(tab(/Decided/))

      const [declined, applied, expired] = await rows()
      expect(declined).toHaveTextContent('Declined')
      expect(declined).toHaveTextContent('Ola kept the current plan: Schedule')
      expect(applied).toHaveTextContent('Applied')
      expect(applied).toHaveTextContent('Takes effect Mon, Nov 16')
      expect(expired).toHaveTextContent('Expired')
    })

    it('says the trainer kept the plan when they declined', async () => {
      const user = userEvent.setup()
      show(
        [],
        [
          change('07', 'Ola Dąbrowska', {
            state: 'DECLINED',
            declinedBy: 'TRAINER',
            declineReason: 'INJURY',
            endedAt: hoursAgo(5),
          }),
        ],
      )
      await screen.findByRole('tab', { name: /Decided/ })
      await user.click(tab(/Decided/))

      expect((await rows())[0]).toHaveTextContent(
        'You kept the current plan: Injury',
      )
    })

    it('says nothing waits for a client, and nothing was decided, in those tabs', async () => {
      const user = userEvent.setup()
      show([JAN], [])
      await screen.findByRole('tab', { name: /Waiting for client/ })

      await user.click(tab(/Waiting for client/))
      expect(
        await screen.findByText('Nothing waits for a client.'),
      ).toBeInTheDocument()
      await user.click(tab(/Decided/))
      expect(
        await screen.findByText('Nothing decided lately.'),
      ).toBeInTheDocument()
    })

    it('reads in Polish', async () => {
      await i18n.changeLanguage('pl')
      show([], [WAITING_FOR_YOU])

      expect(
        await screen.findByRole('tab', { name: /Czeka na Ciebie/ }),
      ).toBeInTheDocument()
      expect(
        screen.getByRole('tab', { name: /Czeka na klienta/ }),
      ).toBeInTheDocument()
      expect(
        screen.getByRole('tab', { name: /Rozstrzygnięte/ }),
      ).toBeInTheDocument()
      expect((await rows())[0]).toHaveTextContent('Czeka na obie strony')
    })
  })
})
