import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { json, problem, renderAppAt, SIGNED_IN, type Reply } from '@/test/page'

const DELETION = '/api/v1/accounts/me/deletion'

/** Trainer Settings with `clients` clients; the deletion answers with `onDelete`, everything else with nothing. */
function settings(
  clients: number,
  onDelete: Reply = () => new Response(null, { status: 204 }),
) {
  const { requests } = renderAppAt(
    '/trainer/settings',
    SIGNED_IN.TRAINER,
    (request) => {
      const { pathname } = new URL(request.url)
      if (pathname === DELETION && request.method === 'GET')
        return json({ clients })
      if (pathname === DELETION) return onDelete(request)
      return json({ items: [] })
    },
  )
  return { requests, user: userEvent.setup() }
}

async function openDialog(user: ReturnType<typeof userEvent.setup>) {
  const section = await screen.findByRole('heading', { name: 'Delete account' })
  // the button comes once the server said no client is left
  const card = within(section.closest('[data-slot=card]') as HTMLElement)
  await user.click(await card.findByRole('button', { name: 'Delete account' }))
  return within(
    await screen.findByRole('alertdialog', { name: 'Delete your account?' }),
  )
}

const deletions = (requests: Request[]) =>
  requests.filter(
    (request) =>
      request.method === 'POST' && new URL(request.url).pathname === DELETION,
  )

describe('DeleteAccountDialog', () => {
  it('says what is erased and asks for the password, with focus on Cancel', async () => {
    const { user } = settings(0)

    const dialog = await openDialog(user)

    expect(
      dialog.getByText(
        "This erases your account, your own exercises and your invites. It can't be undone.",
      ),
    ).toBeInTheDocument()
    expect(dialog.getByLabelText('Password')).toHaveAccessibleDescription(
      'Enter your password to confirm.',
    )
    expect(dialog.getByRole('button', { name: 'Cancel' })).toHaveFocus()
  })

  it('changes nothing on Cancel and gives the focus back to the button', async () => {
    const { requests, user } = settings(0)
    const dialog = await openDialog(user)
    await user.type(dialog.getByLabelText('Password'), 'Kinetiq-2026')

    await user.click(dialog.getByRole('button', { name: 'Cancel' }))

    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    expect(
      screen.getAllByRole('button', { name: 'Delete account' })[0],
    ).toHaveFocus()
    expect(deletions(requests)).toHaveLength(0)
  })

  it('keeps the dialog open with the field message when the password is wrong', async () => {
    const { user } = settings(0, () =>
      problem(400, { status: 400, code: 'account.password-incorrect' }),
    )
    const dialog = await openDialog(user)
    await user.type(dialog.getByLabelText('Password'), 'Wrong-2026')

    await user.click(dialog.getByRole('button', { name: 'Delete account' }))

    expect(
      await dialog.findByText('That password is incorrect.'),
    ).toBeInTheDocument()
    expect(dialog.getByLabelText('Password')).toHaveAttribute(
      'aria-invalid',
      'true',
    )
    expect(screen.getByRole('alertdialog')).toBeInTheDocument()
  })

  it('names the clients left when one joined after the page opened', async () => {
    const { user } = settings(0, () =>
      problem(409, { status: 409, code: 'account.has-clients', clients: 1 }),
    )
    const dialog = await openDialog(user)
    await user.type(dialog.getByLabelText('Password'), 'Kinetiq-2026')

    await user.click(dialog.getByRole('button', { name: 'Delete account' }))

    expect(
      await dialog.findByText(
        'You have 1 client. You can delete your account once none are left.',
      ),
    ).toBeInTheDocument()
  })

  it('sends the password and lands on sign-in, which says the account was deleted', async () => {
    const { requests, user } = settings(0)
    const dialog = await openDialog(user)
    await user.type(dialog.getByLabelText('Password'), 'Kinetiq-2026')

    await user.click(dialog.getByRole('button', { name: 'Delete account' }))

    expect(
      await screen.findByText('Your account was deleted.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
    expect(await deletions(requests)[0].json()).toEqual({
      password: 'Kinetiq-2026',
    })
  })
})

describe('Delete account in trainer Settings', () => {
  it('explains that clients must be gone first, and offers no button', async () => {
    settings(3)

    expect(
      await screen.findByText(
        'You have 3 clients. You can delete your account once none are left.',
      ),
    ).toBeInTheDocument()
    const section = screen.getByRole('heading', { name: 'Delete account' })
    expect(
      within(section.closest('[data-slot=card]') as HTMLElement).queryByRole(
        'button',
      ),
    ).not.toBeInTheDocument()
  })
})
