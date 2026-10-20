import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it } from 'vitest'
import { createRoutes } from '@/app/router'

async function renderTrainerAt(path: string) {
  render(
    <RouterProvider
      router={createMemoryRouter(createRoutes(), { initialEntries: [path] })}
    />,
  )
  return within(await screen.findByRole('navigation', { name: 'Trainer area' }))
}

describe('TrainerArea', () => {
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
})
