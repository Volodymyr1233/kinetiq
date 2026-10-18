import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { ConfirmEmailPage } from './confirm-email-page'
import { pathsOf, problem, renderPage, stubApi } from '@/test/page'

describe('ConfirmEmailPage', () => {
  beforeEach(() => {
    window.history.replaceState(null, '', '/confirm-email#token=abc')
  })

  it('shows "Confirming your email" and removes the token from the address while waiting', async () => {
    stubApi(() => new Promise<Response>(() => {}))
    renderPage(<ConfirmEmailPage />)

    expect(
      screen.getByRole('heading', { name: 'Confirming your email' }),
    ).toBeInTheDocument()
    expect(window.location.hash).toBe('')
  })

  it('confirms once, even though StrictMode runs effects twice, and offers sign-in', async () => {
    const requests = stubApi(() => new Response(null, { status: 204 }))
    renderPage(<ConfirmEmailPage />)

    expect(
      await screen.findByRole('heading', { name: 'Email confirmed' }),
    ).toHaveFocus()
    expect(screen.getByRole('link', { name: 'Sign in' })).toHaveAttribute(
      'href',
      '/sign-in',
    )
    expect(pathsOf(requests)).toEqual(['/api/v1/email-confirmations'])
    expect(await requests[0].json()).toEqual({ token: 'abc' })
  })

  it('offers a new link when the link no longer works, then shows "Check your email"', async () => {
    const requests = stubApi((request) =>
      request.url.endsWith('/email-confirmations')
        ? problem(400, { code: 'account.confirmation-link-invalid' })
        : new Response(null, { status: 202 }),
    )
    const user = userEvent.setup()
    renderPage(<ConfirmEmailPage />)

    expect(
      await screen.findByRole('heading', { name: 'This link no longer works' }),
    ).toHaveFocus()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    await user.type(screen.getByLabelText('Email'), 'marek@example.com')
    await user.click(screen.getByRole('button', { name: 'Send new link' }))

    expect(
      await screen.findByRole('heading', { name: 'Check your email' }),
    ).toHaveFocus()
    expect(pathsOf(requests)).toEqual([
      '/api/v1/email-confirmations',
      '/api/v1/confirmation-links',
    ])
  })

  it('offers a new link without a request when the address has no token', async () => {
    window.history.replaceState(null, '', '/confirm-email')
    const requests = stubApi(() => new Response(null, { status: 204 }))
    renderPage(<ConfirmEmailPage />)

    expect(
      await screen.findByRole('heading', { name: 'This link no longer works' }),
    ).toBeInTheDocument()
    expect(requests).toHaveLength(0)
  })
})
