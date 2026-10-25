import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Toaster } from 'sonner'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { json, problem, renderPage, stubApi, type Reply } from '@/test/page'
import type { Invite } from '../api/invites'
import { InvitesPage } from './invites-page'

const NOW = new Date('2026-10-25T12:00:00Z')
const DAY = 24 * 60 * 60 * 1000

function invite(email: string, sentDaysAgo: number): Invite {
  const sentAt = NOW.getTime() - sentDaysAgo * DAY
  return {
    id: `00000000-0000-4000-8000-${String(email.length).padStart(12, '0')}`,
    email,
    sentAt: new Date(sentAt).toISOString(),
    expiresAt: new Date(sentAt + 7 * DAY).toISOString(),
  }
}

const JULIA = invite('julia@example.com', 0)
const ADAM = invite('adam@example.com', 6.5)
const LINK = 'http://localhost:5173/join#token=new'

type Route = (
  request: Request,
) => Response | undefined | Promise<Response | undefined>

// answers the list from `invites`, then whatever `route` handles; everything else is a 500
function api(invites: Invite[], route: Route = () => undefined): Reply {
  return async (request) => {
    const call = `${request.method} ${new URL(request.url).pathname}`
    const answer = await route(request)
    if (answer) return answer
    if (call === 'GET /api/v1/invites') return json({ invites })
    return problem(500, { code: 'internal.error' })
  }
}

function callsOf(requests: Request[], method: string) {
  return requests
    .filter((request) => request.method === method)
    .map((request) => new URL(request.url).pathname)
}

function renderInvites(reply: Reply) {
  const requests = stubApi(reply)
  renderPage(
    <>
      <InvitesPage />
      <Toaster />
    </>,
  )
  return requests
}

let writeText: ReturnType<typeof vi.fn>

// user-event puts its own clipboard in place, so the stub goes in after it
function setupUser() {
  const user = userEvent.setup()
  Object.defineProperty(navigator, 'clipboard', {
    configurable: true,
    value: { writeText },
  })
  return user
}

beforeEach(() => {
  vi.useFakeTimers({ now: NOW, toFake: ['Date'] })
  writeText = vi.fn(async () => {})
  // the plain-text path: jsdom has no ClipboardItem
  vi.stubGlobal('ClipboardItem', undefined)
})

afterEach(() => vi.useRealTimers())

describe('InvitesPage', () => {
  describe('list', () => {
    it('shows each open invite with when it was sent and when it expires', async () => {
      renderInvites(api([JULIA, ADAM]))

      const julia = (await screen.findByText('julia@example.com')).closest(
        'tr',
      )!
      expect(within(julia).getByText('Today')).toBeInTheDocument()
      expect(within(julia).getByText('In 7 days')).toBeInTheDocument()
      const adam = screen.getByText('adam@example.com').closest('tr')!
      expect(within(adam).getByText('In 1 day')).toBeInTheDocument()
    })

    it('says what will appear and offers to invite when there are no open invites', async () => {
      const user = setupUser()
      renderInvites(api([]))

      expect(
        await screen.findByText(
          'No open invites. Invites you send show up here until the client joins.',
        ),
      ).toBeInTheDocument()
      const buttons = screen.getAllByRole('button', { name: 'Invite client' })
      await user.click(buttons[buttons.length - 1])

      expect(
        await screen.findByRole('dialog', { name: 'Invite a client' }),
      ).toBeInTheDocument()
    })

    it('offers to try again when the list cannot be loaded', async () => {
      const user = setupUser()
      let failing = true
      const requests = renderInvites(
        api([JULIA], () =>
          failing ? problem(500, { code: 'internal.error' }) : undefined,
        ),
      )

      await user.click(await screen.findByRole('button', { name: 'Try again' }))
      failing = false
      await user.click(screen.getByRole('button', { name: 'Try again' }))

      expect(await screen.findByText('julia@example.com')).toBeInTheDocument()
      expect(callsOf(requests, 'GET').length).toBeGreaterThanOrEqual(2)
    })
  })

  describe('invite', () => {
    async function openDialog() {
      const user = setupUser()
      await user.click(
        (await screen.findAllByRole('button', { name: 'Invite client' }))[0],
      )
      return { user, field: await screen.findByLabelText("Client's email") }
    }

    it('sends nothing and says how to fix a malformed email', async () => {
      const requests = renderInvites(api([]))
      const { user, field } = await openDialog()

      await user.type(field, 'julia@')
      await user.click(screen.getByRole('button', { name: 'Send invite' }))

      expect(
        await screen.findByText('Enter an email like name@example.com.'),
      ).toBeInTheDocument()
      expect(field).toHaveFocus()
      expect(callsOf(requests, 'POST')).toEqual([])
    })

    it('shows on the field that the email has an account, until it is edited', async () => {
      renderInvites(
        api([], (request) =>
          request.method === 'POST'
            ? problem(409, { code: 'invite.account-exists' })
            : undefined,
        ),
      )
      const { user, field } = await openDialog()

      await user.type(field, 'ola@example.com')
      await user.click(screen.getByRole('button', { name: 'Send invite' }))

      expect(
        await screen.findByText('This email already has an account.'),
      ).toBeInTheDocument()
      expect(field).toHaveAttribute('aria-invalid', 'true')

      await user.type(field, 'x')

      expect(
        screen.queryByText('This email already has an account.'),
      ).not.toBeInTheDocument()
    })

    it('explains the hourly limit apart from the field', async () => {
      renderInvites(
        api([], (request) =>
          request.method === 'POST'
            ? problem(429, { code: 'invite.limit-reached' })
            : undefined,
        ),
      )
      const { user, field } = await openDialog()

      await user.type(field, 'julia@example.com')
      await user.click(screen.getByRole('button', { name: 'Send invite' }))

      expect(await screen.findByRole('alert')).toHaveTextContent(
        "You've sent many invites. Try again later.",
      )
    })

    it('shows the link to copy and lists the new invite', async () => {
      let invites: Invite[] = []
      const requests = renderInvites(
        api([], async (request) => {
          if (request.method === 'POST') {
            const { email } = await request.json()
            expect(email).toBe('Julia@Example.com')
            invites = [JULIA]
            return json({ invite: JULIA, link: LINK }, 201)
          }
          if (request.method === 'GET') return json({ invites })
        }),
      )
      const { user, field } = await openDialog()

      await user.type(field, ' Julia@Example.com ')
      await user.click(screen.getByRole('button', { name: 'Send invite' }))

      const heading = await screen.findByRole('heading', {
        name: 'Invite sent to julia@example.com',
      })
      expect(heading).toHaveFocus()
      expect(screen.getByLabelText('Invite link')).toHaveValue(LINK)

      await user.click(screen.getByRole('button', { name: 'Copy link' }))

      expect(writeText).toHaveBeenCalledWith(LINK)
      expect(screen.getByRole('button', { name: 'Copied' })).toBeInTheDocument()
      expect(screen.getByRole('status')).toHaveTextContent('Link copied')

      await user.click(screen.getByRole('button', { name: 'Done' }))

      expect(await screen.findByText('julia@example.com')).toBeInTheDocument()
      expect(callsOf(requests, 'POST')).toEqual(['/api/v1/invites'])
    })

    it('says the link could not be copied and selects it for copying by hand', async () => {
      writeText.mockRejectedValue(new DOMException('denied', 'NotAllowedError'))
      renderInvites(
        api([], (request) =>
          request.method === 'POST'
            ? json({ invite: JULIA, link: LINK }, 201)
            : undefined,
        ),
      )
      const { user, field } = await openDialog()
      await user.type(field, 'julia@example.com')
      await user.click(screen.getByRole('button', { name: 'Send invite' }))

      await user.click(await screen.findByRole('button', { name: 'Copy link' }))

      expect(await screen.findByRole('alert')).toHaveTextContent(
        "Couldn't copy. Select the link and copy it.",
      )
      expect(screen.getByLabelText('Invite link')).toHaveFocus()
    })
  })

  describe('copy new link', () => {
    it('copies a new link for the invite and confirms it', async () => {
      const user = setupUser()
      const requests = renderInvites(
        api([JULIA], (request) =>
          request.method === 'POST' ? json({ link: LINK }, 201) : undefined,
        ),
      )

      await user.click(
        await screen.findByRole('button', {
          name: 'Copy new link',
          description: 'The link you copied before stops working.',
        }),
      )

      await waitFor(() => expect(writeText).toHaveBeenCalledWith(LINK))
      expect(await screen.findByText('New link copied')).toBeInTheDocument()
      expect(callsOf(requests, 'POST')).toEqual([
        `/api/v1/invites/${JULIA.id}/links`,
      ])
    })

    it('shows the new link to copy by hand when the clipboard refuses', async () => {
      const user = setupUser()
      writeText.mockRejectedValue(new DOMException('denied', 'NotAllowedError'))
      renderInvites(
        api([JULIA], (request) =>
          request.method === 'POST' ? json({ link: LINK }, 201) : undefined,
        ),
      )

      await user.click(
        await screen.findByRole('button', { name: 'Copy new link' }),
      )

      expect(
        await screen.findByRole('dialog', {
          name: 'New link for julia@example.com',
        }),
      ).toBeInTheDocument()
      expect(screen.getByLabelText('Invite link')).toHaveValue(LINK)
    })
  })

  describe('revoke', () => {
    function revokeButton() {
      return screen.findByRole('button', {
        name: 'Revoke the invite for julia@example.com',
      })
    }

    it('asks first and changes nothing when cancelled', async () => {
      const user = setupUser()
      const requests = renderInvites(api([JULIA]))

      await user.click(await revokeButton())
      const dialog = await screen.findByRole('alertdialog', {
        name: 'Revoke the invite for julia@example.com?',
      })
      expect(
        within(dialog).getByRole('button', { name: 'Cancel' }),
      ).toHaveFocus()
      await user.click(within(dialog).getByRole('button', { name: 'Cancel' }))

      await waitFor(() =>
        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument(),
      )
      expect(callsOf(requests, 'DELETE')).toEqual([])
    })

    it('revokes after confirming and confirms it', async () => {
      const user = setupUser()
      let invites = [JULIA]
      const requests = renderInvites(
        api([], (request) => {
          if (request.method === 'DELETE') {
            invites = []
            return new Response(null, { status: 204 })
          }
          if (request.method === 'GET') return json({ invites })
        }),
      )

      await user.click(await revokeButton())
      await user.click(
        await screen.findByRole('button', { name: 'Revoke invite' }),
      )

      expect(await screen.findByText('Invite revoked')).toBeInTheDocument()
      await waitFor(() =>
        expect(screen.queryByText('julia@example.com')).not.toBeInTheDocument(),
      )
      expect(callsOf(requests, 'DELETE')).toEqual([
        `/api/v1/invites/${JULIA.id}`,
      ])
    })

    it('says the invite is no longer open and refreshes the list', async () => {
      const user = setupUser()
      let invites = [JULIA]
      const requests = renderInvites(
        api([], (request) => {
          if (request.method === 'DELETE') {
            invites = []
            return problem(404, { code: 'invite.not-found' })
          }
          if (request.method === 'GET') return json({ invites })
        }),
      )

      await user.click(await revokeButton())
      await user.click(
        await screen.findByRole('button', { name: 'Revoke invite' }),
      )

      expect(
        await screen.findByText('This invite is no longer open.'),
      ).toBeInTheDocument()
      await waitFor(() => expect(callsOf(requests, 'GET')).toHaveLength(2))
    })

    it('cannot send a second revoke while the first is running', async () => {
      const user = setupUser()
      let finish = () => {}
      const requests = renderInvites(
        api([JULIA], (request) =>
          request.method === 'DELETE'
            ? new Promise<Response>((resolve) => {
                finish = () => resolve(new Response(null, { status: 204 }))
              })
            : undefined,
        ),
      )

      await user.click(await revokeButton())
      const confirm = await screen.findByRole('button', {
        name: 'Revoke invite',
      })
      await user.click(confirm)
      await user.click(confirm)

      expect(confirm).toBeDisabled()
      expect(callsOf(requests, 'DELETE')).toHaveLength(1)
      finish()
    })
  })
})
