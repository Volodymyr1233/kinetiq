import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import i18n from '@/lib/i18n'
import { quizStatus, renderAppAt, SIGNED_IN } from '@/test/page'

describe('home page', () => {
  afterEach(async () => {
    vi.restoreAllMocks()
    await i18n.changeLanguage('en')
  })

  it.each([
    [SIGNED_IN.TRAINER, '/trainer/clients'],
    [SIGNED_IN.CLIENT, '/app/today'],
  ])('sends a signed-in %s to their own area', async (account, area) => {
    const { router } = renderAppAt('/', account, quizStatus('PLAN_PUBLISHED'))

    await waitFor(() => expect(router.state.location.pathname).toBe(area))
    expect(
      screen.queryByRole('heading', { name: /You design the plan/ }),
    ).not.toBeInTheDocument()
  })

  it('shows a visitor what Kinetiq does, with the way to sign up, sign in and read the privacy notice', async () => {
    const { router } = renderAppAt('/', null)

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: 'You design the plan. Your clients log every set.',
      }),
    ).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/')

    const signUp = screen.getAllByRole('link', {
      name: 'Create trainer account',
    })
    expect(signUp).toHaveLength(2)
    signUp.forEach((link) => expect(link).toHaveAttribute('href', '/sign-up'))
    expect(
      within(screen.getByRole('banner')).getByRole('link', { name: 'Sign in' }),
    ).toHaveAttribute('href', '/sign-in')
    expect(
      screen.getByRole('link', { name: 'Read the privacy notice' }),
    ).toHaveAttribute('href', '/privacy')
    expect(
      within(screen.getByRole('contentinfo')).getByRole('link', {
        name: 'Privacy notice',
      }),
    ).toHaveAttribute('href', '/privacy')
  })

  it('shows the page when it cannot find out who is signed in', async () => {
    renderAppAt('/', null)
    vi.mocked(fetch).mockRejectedValue(new TypeError('Failed to fetch'))

    expect(await screen.findByRole('heading', { level: 1 })).toBeInTheDocument()
  })

  it('opens sign-up from the hero', async () => {
    const user = userEvent.setup()
    const { router } = renderAppAt('/', null)

    await user.click(
      (
        await screen.findAllByRole('link', { name: 'Create trainer account' })
      )[0],
    )

    await waitFor(() => expect(router.state.location.pathname).toBe('/sign-up'))
  })

  it('switches to Polish from the footer', async () => {
    const user = userEvent.setup()
    renderAppAt('/', null)

    await user.click(await screen.findByRole('radio', { name: 'Polski' }))

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: 'Ty układasz plan. Twoi klienci zapisują każdą serię.',
      }),
    ).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: 'Polski' })).toBeChecked()
  })
})
