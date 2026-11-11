import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { json, problem, renderAppAt, SIGNED_IN } from '@/test/page'
import type { Client } from '../api/clients'

const OLA: Client = {
  id: '00000000-0000-4000-8000-000000000003',
  name: 'Ola Kowalska',
  goal: 'BUILD_MUSCLE',
  planState: 'PLAN_TO_REVIEW',
}
const JAN: Client = {
  id: '00000000-0000-4000-8000-000000000005',
  name: 'Jan Nowak',
  planState: 'NO_QUIZ',
}

// answers like the backend: a client of this trainer, a list, and 404 for any other id
function backend(request: Request) {
  const { pathname } = new URL(request.url)
  if (pathname === '/api/v1/clients') return json({ clients: [OLA, JAN] })
  const own = [OLA, JAN].find(
    (client) => pathname === `/api/v1/clients/${client.id}`,
  )
  if (own) return json(own)
  if (pathname.endsWith('/overview'))
    return json({ today: '2026-11-04', recentSessions: [] })
  return problem(404, { status: 404, code: 'client.not-found' })
}

function renderClient(client: Client, tab = '') {
  return renderAppAt(
    `/trainer/clients/${client.id}${tab}`,
    SIGNED_IN.TRAINER,
    backend,
  )
}

describe('ClientPage', () => {
  it('shows the name, the goal and where the client stands with the plan, with the four tabs', async () => {
    renderClient(OLA)

    expect(
      await screen.findByRole('heading', { name: 'Ola Kowalska' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Goal: Build muscle')).toBeInTheDocument()
    expect(screen.getByText('Plan to review')).toBeInTheDocument()
    const tabs = within(
      screen.getByRole('tablist', { name: 'Client sections' }),
    )
    expect(tabs.getAllByRole('tab').map((tab) => tab.textContent)).toEqual([
      'Overview',
      'Plan',
      'Logs',
      'Messages',
    ])
    expect(tabs.getByRole('tab', { name: 'Overview' })).toHaveAttribute(
      'aria-selected',
      'true',
    )
    expect(
      await within(screen.getByRole('tabpanel')).findByRole('heading', {
        name: 'This week',
      }),
    ).toBeInTheDocument()
  })

  it('says there is no goal yet when the client has not answered the quiz', async () => {
    renderClient(JAN)

    expect(await screen.findByText('Goal: None yet')).toBeInTheDocument()
    expect(screen.getByText('No quiz yet')).toBeInTheDocument()
  })

  it('gives each tab its own address, which a reload opens on the same tab', async () => {
    const user = userEvent.setup()
    const { router } = renderClient(OLA)

    await user.click(await screen.findByRole('tab', { name: 'Plan' }))

    await waitFor(() =>
      expect(router.state.location.pathname).toBe(
        `/trainer/clients/${OLA.id}/plan`,
      ),
    )
    await waitFor(() =>
      expect(screen.getByRole('tab', { name: 'Plan' })).toHaveAttribute(
        'aria-selected',
        'true',
      ),
    )
    await user.click(screen.getByRole('tab', { name: 'Overview' }))
    await waitFor(() =>
      expect(router.state.location.pathname).toBe(`/trainer/clients/${OLA.id}`),
    )
  })

  it('opens on the tab named in the address', async () => {
    renderClient(OLA, '/messages')

    expect(
      await screen.findByRole('tab', { name: 'Messages' }),
    ).toHaveAttribute('aria-selected', 'true')
  })

  it('moves between tabs with the arrow keys', async () => {
    const user = userEvent.setup()
    const { router } = renderClient(OLA)

    await user.click(await screen.findByRole('tab', { name: 'Overview' }))
    await user.keyboard('{ArrowRight}')

    expect(router.state.location.pathname).toBe(
      `/trainer/clients/${OLA.id}/plan`,
    )
  })

  it.each([
    [
      'an unknown client or another trainer’s',
      '00000000-0000-4000-8000-0000000000ff',
    ],
    ['text that is no id', 'not-an-id'],
  ])('says the client was not found for %s, with a way back', async (_, id) => {
    const user = userEvent.setup()
    renderAppAt(`/trainer/clients/${id}`, SIGNED_IN.TRAINER, (request) =>
      new URL(request.url).pathname === '/api/v1/clients'
        ? json({ clients: [OLA] })
        : problem(id === 'not-an-id' ? 400 : 404, {
            status: id === 'not-an-id' ? 400 : 404,
            code: 'client.not-found',
          }),
    )

    expect(await screen.findByText('Client not found')).toBeInTheDocument()
    expect(screen.queryByRole('tablist')).not.toBeInTheDocument()
    await user.click(screen.getByRole('link', { name: 'Back to all clients' }))
    expect(
      await screen.findByRole('heading', { name: 'Clients' }),
    ).toBeInTheDocument()
  })

  // a 5xx is retried with a pause by design, so the failure here is one that is not
  it('offers to try again when the client cannot be loaded', async () => {
    const user = userEvent.setup()
    let failing = true
    renderAppAt(`/trainer/clients/${OLA.id}`, SIGNED_IN.TRAINER, (request) =>
      failing
        ? problem(429, { status: 429, code: 'request.too-many' })
        : backend(request),
    )

    await user.click(await screen.findByRole('button', { name: 'Try again' }))
    failing = false
    await user.click(screen.getByRole('button', { name: 'Try again' }))

    expect(
      await screen.findByRole('heading', { name: 'Ola Kowalska' }),
    ).toBeInTheDocument()
  })

  it('is opened from the clients table by the client’s name', async () => {
    const user = userEvent.setup()
    const { router } = renderAppAt(
      '/trainer/clients',
      SIGNED_IN.TRAINER,
      backend,
    )

    await user.click(await screen.findByRole('link', { name: 'Jan Nowak' }))

    await waitFor(() =>
      expect(router.state.location.pathname).toBe(`/trainer/clients/${JAN.id}`),
    )
    expect(
      await screen.findByRole('heading', { name: 'Jan Nowak' }),
    ).toBeInTheDocument()
  })

  it('shows the open plan change with a link to its detail, and no card without one', async () => {
    const planChange = {
      id: '00000000-0000-4000-9600-000000000001',
      rule: 'HIGH_RPE',
      state: 'PENDING',
      title: 'Back squat: cut volume by 13%',
      reason: 'Average RPE was 8.4 and 8.6 in the last 2 weeks, above 8.',
      effectiveWeek: '2026-11-16',
    }
    renderAppAt(`/trainer/clients/${OLA.id}`, SIGNED_IN.TRAINER, (request) =>
      new URL(request.url).pathname === `/api/v1/clients/${OLA.id}/plan-change`
        ? json(planChange)
        : backend(request),
    )

    expect(
      await screen.findByRole('heading', { name: planChange.title }),
    ).toBeInTheDocument()
    expect(screen.getByText(planChange.reason)).toBeInTheDocument()
    expect(
      screen.getByText('Takes effect Mon, Nov 16 if accepted.'),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: 'Open plan change' }),
    ).toHaveAttribute('href', `/trainer/plan-changes/${planChange.id}`)
  })

  it('shows no plan change card when the client has none open', async () => {
    renderAppAt(`/trainer/clients/${OLA.id}`, SIGNED_IN.TRAINER, (request) =>
      new URL(request.url).pathname === `/api/v1/clients/${OLA.id}/plan-change`
        ? problem(404, { status: 404, code: 'plan-change.not-found' })
        : backend(request),
    )

    await screen.findByRole('heading', { name: 'Ola Kowalska' })
    await waitFor(() =>
      expect(
        screen.queryByRole('link', { name: 'Open plan change' }),
      ).not.toBeInTheDocument(),
    )
    expect(screen.queryByText('Plan change')).not.toBeInTheDocument()
  })

  describe('plan change state and history', () => {
    const open = {
      id: '00000000-0000-4000-9600-000000000001',
      rule: 'HIGH_RPE',
      state: 'PENDING',
      title: 'Back squat: cut volume by 13%',
      reason: 'Average RPE was 8.4 and 8.6 in the last 2 weeks, above 8.',
      effectiveWeek: '2026-11-16',
      acceptedByTrainer: false,
      acceptedByClient: false,
    }
    const ended = (id: string, patch: object) => ({
      id: `00000000-0000-4000-9600-0000000000${id}`,
      clientId: OLA.id,
      clientName: 'Ola Kowalska',
      title: `Ended ${id}`,
      reason: 'Reason',
      state: 'APPLIED',
      acceptedByTrainer: true,
      acceptedByClient: true,
      effectiveWeek: '2026-10-19',
      createdAt: '2026-10-12T02:00:00Z',
      endedAt: '2026-10-14T09:00:00Z',
      ...patch,
    })

    function renderWith(openChange: object | null, items: object[]) {
      return renderAppAt(
        `/trainer/clients/${OLA.id}`,
        SIGNED_IN.TRAINER,
        (request) => {
          const { pathname } = new URL(request.url)
          if (pathname === `/api/v1/clients/${OLA.id}/plan-change`) {
            return openChange
              ? json(openChange)
              : problem(404, { status: 404, code: 'plan-change.not-found' })
          }
          if (pathname === `/api/v1/clients/${OLA.id}/plan-changes/ended`) {
            return json({ items })
          }
          return backend(request)
        },
      )
    }

    it('says the open plan change waits for both while nobody accepted', async () => {
      renderWith(open, [])

      expect(await screen.findByText('Waiting for both')).toBeInTheDocument()
    })

    it('says the plan change waits for the trainer once the client accepted', async () => {
      renderWith(
        { ...open, state: 'ACCEPTED_BY_ONE', acceptedByClient: true },
        [],
      )

      expect(await screen.findByText('Waiting for you')).toBeInTheDocument()
    })

    it('says who the trainer waits for once they accepted', async () => {
      renderWith(
        { ...open, state: 'ACCEPTED_BY_ONE', acceptedByTrainer: true },
        [],
      )

      expect(await screen.findByText('Waiting for Ola')).toBeInTheDocument()
    })

    it('lists the ended plan changes under Past plan changes, each opening its detail', async () => {
      renderWith(null, [
        ended('04', {}),
        ended('05', {
          state: 'DECLINED',
          declinedBy: 'CLIENT',
          declineReason: 'SCHEDULE',
          endedAt: '2026-10-10T09:00:00Z',
        }),
      ])

      const section = await screen.findByRole('region', {
        name: 'Past plan changes',
      })
      const links = within(section).getAllByRole('link')
      expect(links.map((link) => link.getAttribute('href'))).toEqual([
        '/trainer/plan-changes/00000000-0000-4000-9600-000000000004',
        '/trainer/plan-changes/00000000-0000-4000-9600-000000000005',
      ])
      expect(links[0]).toHaveTextContent('Ended 04')
      expect(links[0]).toHaveTextContent('Applied')
      expect(links[0]).toHaveTextContent('Wed, Oct 14')
      expect(links[1]).toHaveTextContent('Declined')
    })

    it('hides Past plan changes when none ended', async () => {
      renderWith(null, [])

      await screen.findByRole('heading', { name: 'Ola Kowalska' })
      await waitFor(() =>
        expect(
          screen.queryByRole('region', { name: 'Past plan changes' }),
        ).not.toBeInTheDocument(),
      )
    })
  })
})
