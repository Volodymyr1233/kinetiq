import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, describe, expect, it } from 'vitest'
import { createRoutes } from '@/app/router'

function renderAt(path: string) {
  render(
    <RouterProvider
      router={createMemoryRouter(createRoutes(), { initialEntries: [path] })}
    />,
  )
}

describe('routes', () => {
  afterEach(() => sessionStorage.clear())

  it.each([
    ['/trainer', 'Clients'],
    ['/app', 'Today'],
    ['/app/quiz', 'What do you train for?'],
  ])('loads the area at %s', async (path, heading) => {
    renderAt(path)

    expect(
      await screen.findByRole('heading', { name: heading }),
    ).toBeInTheDocument()
  })

  it('opens the quiz review once every answer is in', async () => {
    sessionStorage.setItem(
      'kinetiq.quiz',
      JSON.stringify({
        answers: {
          goal: 'BUILD_MUSCLE',
          experience: 'YEARS_1_TO_2',
          level: 'INTERMEDIATE',
          age: '29',
          height: '168',
          weight: '62.5',
          likes: ['WEIGHTS'],
          days: 3,
          notes: '',
        },
        reached: 7,
      }),
    )
    renderAt('/app/quiz/review')

    expect(
      await screen.findByRole('heading', { name: 'Check your answers' }),
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
