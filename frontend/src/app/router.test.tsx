import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it } from 'vitest'
import { createRoutes } from '@/app/router'

function renderAt(path: string) {
  render(
    <RouterProvider
      router={createMemoryRouter(createRoutes(), { initialEntries: [path] })}
    />,
  )
}

describe('routes', () => {
  it.each([
    ['/trainer', 'Clients'],
    ['/app', 'Today'],
  ])('loads the area at %s', async (path, heading) => {
    renderAt(path)

    expect(
      await screen.findByRole('heading', { name: heading }),
    ).toBeInTheDocument()
  })

  it.each(['/nowhere', '/trainer/nowhere', '/app/nowhere'])(
    'shows the not-found page with a way back at %s',
    async (path) => {
      renderAt(path)

      expect(
        await screen.findByRole('heading', { name: 'Page not found' }),
      ).toBeInTheDocument()
      expect(
        screen.getByRole('link', { name: 'Go to the start page' }),
      ).toHaveAttribute('href', '/')
    },
  )
})
