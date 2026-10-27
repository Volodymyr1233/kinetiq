import { act, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ConfirmEmailBanner } from './confirm-email-banner'
import { json, pathsOf, renderPage, SIGNED_IN, stubApi } from '@/test/page'

const UNCONFIRMED = { ...SIGNED_IN.CLIENT, emailConfirmed: false }

function serve(account: object) {
  return stubApi((request) =>
    request.method === 'GET'
      ? json(account)
      : new Response(null, { status: 202 }),
  )
}

describe('ConfirmEmailBanner', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('asks an unconfirmed client to confirm the email it was sent to', async () => {
    serve(UNCONFIRMED)
    renderPage(<ConfirmEmailBanner />)

    expect(
      await screen.findByText(
        'Confirm your email: we sent a link to anna@example.com.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Resend' })).toBeEnabled()
  })

  it('shows nothing once the email is confirmed', async () => {
    const requests = serve(SIGNED_IN.CLIENT)
    renderPage(<ConfirmEmailBanner />)

    await vi.waitFor(() => expect(requests).toHaveLength(1))
    expect(screen.queryByText(/Confirm your email/)).not.toBeInTheDocument()
  })

  it('sends a new link, says so once, and offers Resend again after a minute', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    const requests = serve(UNCONFIRMED)
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    renderPage(<ConfirmEmailBanner />)

    await user.click(await screen.findByRole('button', { name: 'Resend' }))

    expect(await screen.findByRole('button', { name: 'Sent' })).toBeDisabled()
    expect(screen.getByRole('status')).toHaveTextContent('We sent a new link.')
    expect(pathsOf(requests)).toContain('/api/v1/confirmation-links')
    const resend = requests.find((request) => request.method === 'POST')!
    expect(await resend.json()).toEqual({ email: 'anna@example.com' })

    await act(() => vi.advanceTimersByTimeAsync(60_000))

    expect(screen.getByRole('button', { name: 'Resend' })).toBeEnabled()
    expect(screen.getByRole('status')).toBeEmptyDOMElement()
  })
})
