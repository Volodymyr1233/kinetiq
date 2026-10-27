import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it } from 'vitest'
import { JoinPage } from './join-page'
import {
  json,
  pathsOf,
  problem,
  SIGNED_IN,
  stubApi,
  type Reply,
} from '@/test/page'

const INVITE = { email: 'anna@example.com', trainerName: 'Marek Nowak' }

// the check answers with the invite unless a test says otherwise; the join with the new client
function api({ check, join }: { check?: Reply; join?: Reply } = {}): Request[] {
  return stubApi((request) => {
    if (request.url.endsWith('/invite-checks')) {
      return check ? check(request) : json(INVITE)
    }
    return join ? join(request) : json(SIGNED_IN.CLIENT, 201)
  })
}

function renderJoin() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  const router = createMemoryRouter(
    [
      { path: '/join', Component: JoinPage },
      { path: '/sign-in', Component: () => <h1>Sign-in page</h1> },
      { path: '/app/quiz', Component: () => <h1>Quiz page</h1> },
    ],
    { initialEntries: ['/join'] },
  )
  render(
    <QueryClientProvider client={client}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
  return router
}

async function fillForm(user: ReturnType<typeof userEvent.setup>) {
  await user.type(await screen.findByLabelText('Name'), 'Anna Kowalska')
  await user.type(screen.getByLabelText('Password'), 'Kinetiq-2026')
  await user.click(screen.getByRole('checkbox', { name: 'I am 16 or older' }))
  await user.click(
    screen.getByRole('checkbox', { name: /processes my health data/ }),
  )
}

function submit(user: ReturnType<typeof userEvent.setup>) {
  return user.click(screen.getByRole('button', { name: 'Create account' }))
}

describe('JoinPage', () => {
  beforeEach(() => {
    window.history.replaceState(null, '', '/join#token=secret')
  })

  it('shows who invited the person, the invited email read-only and empty fields', async () => {
    const requests = api()
    renderJoin()

    expect(
      await screen.findByRole('heading', { name: 'Join Marek on Kinetiq' }),
    ).toHaveFocus()
    expect(screen.getByText('Marek Nowak invited you')).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toHaveValue('anna@example.com')
    expect(screen.getByLabelText('Email')).toHaveAttribute('readonly')
    expect(screen.getByLabelText('Name')).toHaveValue('')
    expect(screen.getByLabelText('Password')).toHaveValue('')
    expect(
      screen.getByRole('checkbox', { name: 'I am 16 or older' }),
    ).not.toBeChecked()
    expect(
      screen.getByRole('link', {
        name: /^Privacy notice ?\(opens in a new tab\)$/,
      }),
    ).toHaveAttribute('target', '_blank')
    // the secret travels in the body, and the address keeps it so a reload shows the form again
    expect(await requests[0].json()).toEqual({ token: 'secret' })
    expect(window.location.hash).toBe('#token=secret')
  })

  it('shows a message next to each field and sends nothing when the form is empty', async () => {
    const requests = api()
    const user = userEvent.setup()
    renderJoin()
    await screen.findByLabelText('Name')

    await submit(user)

    expect(await screen.findByText('Enter your name.')).toBeInTheDocument()
    expect(
      screen.getByText('Confirm that you are 16 or older.'),
    ).toBeInTheDocument()
    expect(
      screen.getByText(
        'Agree to this to join. Your plan depends on this data.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('Password')).toHaveAttribute(
      'aria-invalid',
      'true',
    )
    expect(screen.getByLabelText('Name')).toHaveFocus()
    expect(pathsOf(requests)).toEqual(['/api/v1/invite-checks'])
  })

  it('keeps the consent box as it was when the privacy notice link is chosen', async () => {
    api()
    const user = userEvent.setup()
    renderJoin()
    const consent = await screen.findByRole('checkbox', {
      name: /processes my health data/,
    })

    await user.click(
      screen.getByRole('link', {
        name: /^Privacy notice ?\(opens in a new tab\)$/,
      }),
    )

    expect(consent).not.toBeChecked()
  })

  it('shows the field message for an error the server found', async () => {
    api({
      join: () =>
        problem(400, {
          code: 'validation.failed',
          errors: [{ field: 'name', code: 'size' }],
        }),
    })
    const user = userEvent.setup()
    renderJoin()
    await fillForm(user)

    await submit(user)

    expect(
      await screen.findByText('Use 100 characters or fewer.'),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('Name')).toHaveFocus()
  })

  it('keeps the form busy and sends one request while joining', async () => {
    const requests = api({ join: () => new Promise<Response>(() => {}) })
    const user = userEvent.setup()
    renderJoin()
    await fillForm(user)

    await submit(user)
    await submit(user)

    const button = screen.getByRole('button', { name: 'Create account' })
    expect(button).toBeDisabled()
    expect(button).toHaveAttribute('aria-busy', 'true')
    expect(screen.getByLabelText('Name')).toHaveAttribute('readonly')
    expect(pathsOf(requests)).toEqual([
      '/api/v1/invite-checks',
      '/api/v1/invite-acceptances',
    ])
  })

  it('shows "This link no longer works" when the check refuses the link, and sign-in from it', async () => {
    api({ check: () => problem(400, { code: 'invite.link-invalid' }) })
    const user = userEvent.setup()
    const router = renderJoin()

    expect(
      await screen.findByRole('heading', { name: 'This link no longer works' }),
    ).toHaveFocus()
    expect(screen.queryByText('anna@example.com')).not.toBeInTheDocument()
    await user.click(screen.getByRole('link', { name: 'Sign in instead' }))

    expect(router.state.location.pathname).toBe('/sign-in')
  })

  it('shows "This link no longer works" when the link stopped working before joining', async () => {
    api({ join: () => problem(400, { code: 'invite.link-invalid' }) })
    const user = userEvent.setup()
    renderJoin()
    await fillForm(user)

    await submit(user)

    expect(
      await screen.findByRole('heading', { name: 'This link no longer works' }),
    ).toHaveFocus()
  })

  it('shows "This link no longer works" without asking the server when the address has no token', async () => {
    window.history.replaceState(null, '', '/join')
    const requests = api()
    renderJoin()

    expect(
      await screen.findByRole('heading', { name: 'This link no longer works' }),
    ).toBeInTheDocument()
    expect(requests).toHaveLength(0)
  })

  it('shows the generic message with "Try again" when the check fails otherwise', async () => {
    let failing = true
    const requests = api({
      check: () =>
        failing ? problem(500, { code: 'internal.error' }) : json(INVITE),
    })
    const user = userEvent.setup()
    renderJoin()

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Something went wrong on our side. Try again in a moment.',
    )
    failing = false
    await user.click(screen.getByRole('button', { name: 'Try again' }))

    expect(
      await screen.findByRole('heading', { name: 'Join Marek on Kinetiq' }),
    ).toBeInTheDocument()
    expect(pathsOf(requests)).toEqual([
      '/api/v1/invite-checks',
      '/api/v1/invite-checks',
    ])
  })

  it('shows "You\'re in" after joining and leads to the quiz', async () => {
    const requests = api()
    const user = userEvent.setup()
    const router = renderJoin()
    await fillForm(user)

    await submit(user)

    expect(
      await screen.findByRole('heading', { name: "You're in, Anna" }),
    ).toHaveFocus()
    expect(
      screen.getByText(
        'Answer 7 questions. Marek uses them to build your plan.',
      ),
    ).toBeInTheDocument()
    const body = await requests[1].json()
    expect(body).toMatchObject({
      token: 'secret',
      name: 'Anna Kowalska',
      password: 'Kinetiq-2026',
      atLeastSixteen: true,
      healthDataConsent: true,
    })
    expect(body.timeZone).toEqual(expect.any(String))
    await user.click(screen.getByRole('link', { name: 'Start the quiz' }))

    expect(router.state.location.pathname).toBe('/app/quiz')
  })
})
