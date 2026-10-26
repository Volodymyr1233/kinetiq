import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { ResetPasswordPage } from './reset-password-page'
import { pathsOf, problem, renderPage, stubApi } from '@/test/page'

describe('ResetPasswordPage', () => {
  beforeEach(() => {
    window.history.replaceState(null, '', '/reset-password')
  })

  describe('without a token', () => {
    it('asks for the email and says a link was sent, whether or not the account exists', async () => {
      const requests = stubApi(() => new Response(null, { status: 202 }))
      const user = userEvent.setup()
      renderPage(<ResetPasswordPage />)

      await user.type(screen.getByLabelText('Email'), 'marek@example.com')
      await user.click(screen.getByRole('button', { name: 'Send reset link' }))

      expect(
        await screen.findByRole('heading', { name: 'Check your email' }),
      ).toHaveFocus()
      expect(screen.getByText('marek@example.com')).toBeInTheDocument()
      expect(screen.getByRole('link', { name: 'Sign in' })).toHaveAttribute(
        'href',
        '/sign-in',
      )
      expect(pathsOf(requests)).toEqual(['/api/v1/password-reset-links'])
      expect(await requests[0].json()).toEqual({ email: 'marek@example.com' })
    })

    it('does not send a request for a malformed email', async () => {
      const requests = stubApi(() => new Response(null, { status: 202 }))
      const user = userEvent.setup()
      renderPage(<ResetPasswordPage />)

      await user.type(screen.getByLabelText('Email'), 'marek')
      await user.click(screen.getByRole('button', { name: 'Send reset link' }))

      expect(
        await screen.findByText('Enter an email like name@example.com.'),
      ).toBeInTheDocument()
      expect(requests).toHaveLength(0)
    })
  })

  describe('with a token', () => {
    beforeEach(() => {
      window.history.replaceState(null, '', '/reset-password#token=abc')
    })

    it('removes the token from the address, then sets the password and offers sign-in', async () => {
      const requests = stubApi(() => new Response(null, { status: 204 }))
      const user = userEvent.setup()
      renderPage(<ResetPasswordPage />)

      expect(window.location.hash).toBe('')
      await user.type(screen.getByLabelText('New password'), 'Kinetiq-2026')
      await user.click(screen.getByRole('button', { name: 'Set password' }))

      expect(
        await screen.findByRole('heading', { name: 'Password changed' }),
      ).toHaveFocus()
      expect(screen.getByRole('link', { name: 'Sign in' })).toHaveAttribute(
        'href',
        '/sign-in',
      )
      expect(pathsOf(requests)).toEqual(['/api/v1/password-resets'])
      expect(await requests[0].json()).toEqual({
        token: 'abc',
        password: 'Kinetiq-2026',
      })
    })

    it('does not send a password that breaks the rules', async () => {
      const requests = stubApi(() => new Response(null, { status: 204 }))
      const user = userEvent.setup()
      renderPage(<ResetPasswordPage />)

      await user.type(screen.getByLabelText('New password'), 'kinetiq')
      await user.click(screen.getByRole('button', { name: 'Set password' }))

      expect(screen.getByLabelText('New password')).toBeInvalid()
      expect(requests).toHaveLength(0)
    })

    it('offers a new link when the link no longer works', async () => {
      const requests = stubApi((request) =>
        request.url.endsWith('/password-resets')
          ? problem(400, { code: 'account.password-reset-link-invalid' })
          : new Response(null, { status: 202 }),
      )
      const user = userEvent.setup()
      renderPage(<ResetPasswordPage />)

      await user.type(screen.getByLabelText('New password'), 'Kinetiq-2026')
      await user.click(screen.getByRole('button', { name: 'Set password' }))

      expect(
        await screen.findByRole('heading', {
          name: 'This link no longer works',
        }),
      ).toHaveFocus()
      await user.type(screen.getByLabelText('Email'), 'marek@example.com')
      await user.click(screen.getByRole('button', { name: 'Send reset link' }))

      expect(
        await screen.findByRole('heading', { name: 'Check your email' }),
      ).toBeInTheDocument()
      expect(pathsOf(requests)).toEqual([
        '/api/v1/password-resets',
        '/api/v1/password-reset-links',
      ])
    })

    it('keeps the form and says so when the request fails for another reason', async () => {
      stubApi(() => problem(500, { code: 'internal.error' }))
      const user = userEvent.setup()
      renderPage(<ResetPasswordPage />)

      await user.type(screen.getByLabelText('New password'), 'Kinetiq-2026')
      await user.click(screen.getByRole('button', { name: 'Set password' }))

      expect(await screen.findByRole('alert')).toHaveTextContent(
        'Something went wrong on our side. Try again in a moment.',
      )
      expect(screen.getByLabelText('New password')).toBeInTheDocument()
    })
  })
})
