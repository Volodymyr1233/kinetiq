import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { json, problem, renderAppAt, SIGNED_IN, type Reply } from '@/test/page'

async function renderTrainerAt(path: string) {
  renderAppAt(path, SIGNED_IN.TRAINER)
  return within(await screen.findByRole('navigation', { name: 'Trainer area' }))
}

describe('TrainerArea', () => {
  describe('unread count on the Messages item', () => {
    const counting =
      (count: number): Reply =>
      (request) =>
        new URL(request.url).pathname === '/api/v1/conversations/unread-count'
          ? json({ count })
          : new Response(null, { status: 204 })

    it('shows the total and announces it as part of the name', async () => {
      renderAppAt('/trainer/clients', SIGNED_IN.TRAINER, counting(120))

      expect(
        await screen.findByRole('link', { name: 'Messages, 120 unread' }),
      ).toBeInTheDocument()
      expect(screen.getByText('99+')).toBeInTheDocument()
    })

    it('shows nothing at 0', async () => {
      const nav = await renderTrainerAt('/trainer/clients')

      expect(nav.getByRole('link', { name: 'Messages' })).toBeInTheDocument()
    })

    it('hides the count when the request fails', async () => {
      renderAppAt('/trainer/clients', SIGNED_IN.TRAINER, (request) =>
        new URL(request.url).pathname === '/api/v1/conversations/unread-count'
          ? problem(500, { status: 500, code: 'internal.error' })
          : new Response(null, { status: 204 }),
      )

      expect(
        await screen.findByRole('link', { name: 'Messages' }),
      ).toBeInTheDocument()
    })
  })

  it('keeps the sections behind a button on a narrow screen', async () => {
    const user = userEvent.setup()
    vi.stubGlobal('innerWidth', 375)
    renderAppAt('/trainer/clients', SIGNED_IN.TRAINER)

    await user.click(await screen.findByRole('button', { name: 'Open menu' }))

    const menu = within(await screen.findByRole('dialog', { name: 'Menu' }))
    expect(menu.getByRole('link', { name: 'Clients' })).toBeInTheDocument()
    await user.click(menu.getByRole('link', { name: 'Settings' }))
    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    )
  })

  it('links every section from the sidebar', async () => {
    const nav = await renderTrainerAt('/trainer/clients')

    expect(
      nav.getAllByRole('link').map((link) => link.getAttribute('href')),
    ).toEqual([
      '/trainer/clients',
      '/trainer/approvals',
      '/trainer/messages',
      '/trainer/exercises',
      '/trainer/settings',
    ])
  })

  it('opens Clients when the area is entered without a section', async () => {
    const nav = await renderTrainerAt('/trainer')

    expect(nav.getByRole('link', { name: 'Clients' })).toHaveAttribute(
      'aria-current',
      'page',
    )
  })

  it('moves the highlight and the title to the section that was clicked', async () => {
    const nav = await renderTrainerAt('/trainer/clients')

    await userEvent.click(nav.getByRole('link', { name: 'Messages' }))

    expect(
      await screen.findByRole('heading', { name: 'Messages' }),
    ).toBeInTheDocument()
    expect(nav.getByRole('link', { name: 'Messages' })).toHaveAttribute(
      'aria-current',
      'page',
    )
    expect(nav.getByRole('link', { name: 'Clients' })).not.toHaveAttribute(
      'aria-current',
    )
    expect(screen.getByRole('banner')).toHaveTextContent('Messages')
  })

  it('shows who is signed in and signs out to the sign-in page, clearing the tab', async () => {
    sessionStorage.setItem('kinetiq.quiz', '{}')
    renderAppAt('/trainer/clients', SIGNED_IN.TRAINER)
    const user = userEvent.setup()

    expect(await screen.findByText('Marek Nowak')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Sign out' }))

    expect(
      await screen.findByRole('heading', { name: 'Sign in' }),
    ).toBeInTheDocument()
    expect(sessionStorage.length).toBe(0)
  })

  it('stays signed in with the generic message when sign-out fails', async () => {
    renderAppAt('/trainer/clients', SIGNED_IN.TRAINER, () =>
      problem(500, { status: 500, code: 'internal.error' }),
    )
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Sign out' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Something went wrong on our side. Try again in a moment.',
    )
    expect(
      screen.getByRole('navigation', { name: 'Trainer area' }),
    ).toBeInTheDocument()
  })
})
