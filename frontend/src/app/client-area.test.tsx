import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it } from 'vitest'
import { createRoutes } from '@/app/router'

async function renderClientAt(path: string) {
  render(
    <RouterProvider
      router={createMemoryRouter(createRoutes(), { initialEntries: [path] })}
    />,
  )
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
})
