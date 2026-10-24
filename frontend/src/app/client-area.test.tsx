import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { problem, renderAppAt, SIGNED_IN } from '@/test/page'

async function renderClientAt(path: string) {
  renderAppAt(path, SIGNED_IN.CLIENT)
  return within(await screen.findByRole('navigation', { name: 'Client area' }))
}

describe('ClientArea', () => {
  it('links every tab from the tab bar', async () => {
    const tabs = await renderClientAt('/app/today')

    expect(
      tabs.getAllByRole('link').map((link) => link.getAttribute('href')),
    ).toEqual(['/app/today', '/app/plan', '/app/progress', '/app/messages'])
  })

  it('opens Today when the area is entered without a tab', async () => {
    const tabs = await renderClientAt('/app')

    expect(tabs.getByRole('link', { name: 'Today' })).toHaveAttribute(
      'aria-current',
      'page',
    )
    expect(screen.getByRole('heading', { name: 'Today' })).toBeInTheDocument()
  })

  it('moves the highlight and the title to the tab that was tapped', async () => {
    const tabs = await renderClientAt('/app/today')

    await userEvent.click(tabs.getByRole('link', { name: 'Progress' }))

    expect(
      await screen.findByRole('heading', { name: 'Progress' }),
    ).toBeInTheDocument()
    expect(tabs.getByRole('link', { name: 'Progress' })).toHaveAttribute(
      'aria-current',
      'page',
    )
    expect(tabs.getByRole('link', { name: 'Today' })).not.toHaveAttribute(
      'aria-current',
    )
  })

  it('signs out from the settings sheet, clearing unfinished quiz answers', async () => {
    sessionStorage.setItem('kinetiq.quiz', '{"goal":"STRENGTH"}')
    const { requests } = renderAppAt('/app/today', SIGNED_IN.CLIENT)
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Settings' }))
    const sheet = await screen.findByRole('dialog', { name: 'Settings' })
    expect(within(sheet).getByText('Anna Kowalska')).toBeInTheDocument()
    await user.click(within(sheet).getByRole('button', { name: 'Sign out' }))

    expect(
      await screen.findByRole('heading', { name: 'Sign in' }),
    ).toBeInTheDocument()
    expect(sessionStorage.length).toBe(0)
    expect(
      requests.some(
        (request) =>
          request.method === 'DELETE' &&
          new URL(request.url).pathname === '/api/v1/sign-ins/current',
      ),
    ).toBe(true)
  })

  it('keeps the sheet open with the generic message when sign-out fails', async () => {
    renderAppAt('/app/today', SIGNED_IN.CLIENT, () =>
      problem(500, { status: 500, code: 'internal.error' }),
    )
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Settings' }))
    const sheet = await screen.findByRole('dialog', { name: 'Settings' })
    await user.click(within(sheet).getByRole('button', { name: 'Sign out' }))

    expect(await within(sheet).findByRole('alert')).toHaveTextContent(
      'Something went wrong on our side. Try again in a moment.',
    )
  })
})
