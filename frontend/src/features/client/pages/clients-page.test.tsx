import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { json, problem, renderPage, stubApi } from '@/test/page'
import type { Client } from '../api/clients'
import { ClientsPage } from './clients-page'

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
const KONRAD: Client = {
  id: '00000000-0000-4000-8000-000000000007',
  name: 'Konrad Kowal',
  goal: 'LOSE_FAT',
  planState: 'PLAN_TO_REVIEW',
}

function renderClients(clients: Client[]) {
  return stubApi(() => json({ clients }))
}

function show(clients: Client[]) {
  const requests = renderClients(clients)
  renderPage(<ClientsPage />)
  return requests
}

function names() {
  return screen
    .getAllByRole('row')
    .slice(1)
    .map((row) => within(row).getAllByRole('cell')[0].textContent)
}

describe('ClientsPage', () => {
  it('shows each client with the goal and where they stand with the plan, in the order received', async () => {
    show([JAN, OLA])

    const ola = (await screen.findByText('Ola Kowalska')).closest('tr')!
    expect(within(ola).getByText('Build muscle')).toBeInTheDocument()
    expect(within(ola).getByText('Plan to review')).toBeInTheDocument()
    const jan = screen.getByText('Jan Nowak').closest('tr')!
    expect(within(jan).getByText('No quiz yet')).toBeInTheDocument()
    expect(names()).toEqual(['Jan Nowak', 'Ola Kowalska'])
    expect(screen.getByRole('status')).toHaveTextContent('2 of 2 clients')
  })

  it('says "None yet" for a goal and a last log that are not there, not a bare dash', async () => {
    show([JAN])

    const jan = (await screen.findByText('Jan Nowak')).closest('tr')!
    // the goal, and the last log that no feature produces yet
    expect(within(jan).getAllByText('None yet')).toHaveLength(2)
  })

  describe('search', () => {
    it('keeps the clients whose name contains the text, whatever the letter case', async () => {
      const user = userEvent.setup()
      show([OLA, JAN, KONRAD])

      await user.type(
        await screen.findByRole('searchbox', {
          name: 'Search clients by name',
        }),
        'KO',
      )

      expect(names()).toEqual(['Ola Kowalska', 'Konrad Kowal'])
      expect(screen.getByRole('status')).toHaveTextContent('2 of 3 clients')
    })

    it('says nothing matches, and shows everyone again when the search is cleared', async () => {
      const user = userEvent.setup()
      show([OLA, JAN])

      await user.type(
        await screen.findByRole('searchbox', {
          name: 'Search clients by name',
        }),
        'zzz',
      )

      expect(
        screen.getByText('No client matches that name'),
      ).toBeInTheDocument()
      expect(screen.queryByRole('table')).not.toBeInTheDocument()
      await user.click(screen.getByRole('button', { name: 'Clear search' }))
      expect(names()).toEqual(['Ola Kowalska', 'Jan Nowak'])
    })

    it('ignores spaces around the text', async () => {
      const user = userEvent.setup()
      show([OLA, JAN])

      await user.type(
        await screen.findByRole('searchbox', {
          name: 'Search clients by name',
        }),
        '  jan ',
      )

      expect(names()).toEqual(['Jan Nowak'])
    })
  })

  it('offers to invite a client when the trainer has none, and no search', async () => {
    show([])

    expect(await screen.findByText('No clients yet')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Invite client' })).toHaveAttribute(
      'href',
      '/invites',
    )
    expect(screen.queryByRole('searchbox')).not.toBeInTheDocument()
  })

  it('offers to try again when the list cannot be loaded', async () => {
    const user = userEvent.setup()
    let failing = true
    stubApi(() =>
      failing
        ? problem(500, { code: 'internal.error' })
        : json({ clients: [OLA] }),
    )
    renderPage(<ClientsPage />)

    await user.click(await screen.findByRole('button', { name: 'Try again' }))
    failing = false
    await user.click(screen.getByRole('button', { name: 'Try again' }))

    await waitFor(() =>
      expect(screen.getByText('Ola Kowalska')).toBeInTheDocument(),
    )
  })
})
