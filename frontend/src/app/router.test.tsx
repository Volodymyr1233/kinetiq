import { screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderAppAt, SIGNED_IN } from '@/test/page'

describe('routes', () => {
  it.each([
    ['/trainer', SIGNED_IN.TRAINER, 'Clients'],
    ['/app', SIGNED_IN.CLIENT, 'Today'],
    ['/app/quiz', SIGNED_IN.CLIENT, 'What do you train for?'],
  ])('loads the area at %s', async (path, account, heading) => {
    renderAppAt(path, account)

    expect(
      await screen.findByRole('heading', { name: heading }),
    ).toBeInTheDocument()
  })

  it.each([
    ['/nowhere', null],
    ['/trainer/nowhere', SIGNED_IN.TRAINER],
    ['/app/nowhere', SIGNED_IN.CLIENT],
  ])(
    'shows the not-found page with a way back at %s',
    async (path, account) => {
      renderAppAt(path, account)

      expect(
        await screen.findByRole('heading', { name: 'Page not found' }),
      ).toBeInTheDocument()
      expect(
        screen.getByRole('link', { name: 'Go to the start page' }),
      ).toHaveAttribute('href', '/')
    },
  )
})
