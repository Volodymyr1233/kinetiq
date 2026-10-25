import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { afterEach, describe, expect, it } from 'vitest'
import i18n from '@/lib/i18n'
import { json, problem, stubApi, type Reply } from '@/test/page'
import { SignInPage } from './sign-in-page'

const TRAINER = {
  id: '00000000-0000-4000-8000-000000000001',
  name: 'Marek Nowak',
  role: 'TRAINER',
  timeZone: 'Europe/Warsaw',
}

function renderSignIn(reply: Reply) {
  const requests = stubApi(reply)
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  const router = createMemoryRouter(
    [
      { path: '/sign-in', element: <SignInPage /> },
      { path: '/trainer', element: <h1>Trainer area</h1> },
      { path: '/app', element: <h1>Client app</h1> },
    ],
    { initialEntries: ['/sign-in'] },
  )
  render(
    <QueryClientProvider client={client}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
  return { requests, user: userEvent.setup() }
}

async function signIn(
  user: ReturnType<typeof userEvent.setup>,
  email = 'marek@example.com',
  password = 'Kinetiq-2026',
) {
  await user.type(screen.getByLabelText('Email'), email)
  await user.type(screen.getByLabelText('Password'), password)
  await user.click(screen.getByRole('button', { name: 'Sign in' }))
}

describe('SignInPage', () => {
  afterEach(async () => {
    sessionStorage.clear()
    await i18n.changeLanguage('en')
  })

  it.each([
    ['en', 'Create an account'],
    ['pl', 'Załóż konto'],
  ])('links new trainers to sign-up in %s', async (language, name) => {
    await i18n.changeLanguage(language)
    renderSignIn(() => json(TRAINER))

    expect(screen.getByRole('link', { name })).toHaveAttribute(
      'href',
      '/sign-up',
    )
  })

  it('asks for both fields and sends nothing when they are empty', async () => {
    const { requests, user } = renderSignIn(() => json(TRAINER))

    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByText('Enter your email.')).toBeInTheDocument()
    expect(screen.getByText('Enter your password.')).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toHaveFocus()
    expect(requests).toHaveLength(0)
  })

  it('lands a trainer in the trainer area and clears what the tab kept', async () => {
    sessionStorage.setItem('kinetiq.quiz', '{"goal":"STRENGTH"}')
    const { requests, user } = renderSignIn(() => json(TRAINER))

    await signIn(user, ' Marek@Example.com ', ' Kinetiq-2026')

    expect(
      await screen.findByRole('heading', { name: 'Trainer area' }),
    ).toBeInTheDocument()
    expect(await requests[0].json()).toEqual({
      email: 'Marek@Example.com',
      password: ' Kinetiq-2026',
    })
    expect(sessionStorage.length).toBe(0)
  })

  it('lands a client in the client app', async () => {
    const { user } = renderSignIn(() => json({ ...TRAINER, role: 'CLIENT' }))

    await signIn(user)

    expect(
      await screen.findByRole('heading', { name: 'Client app' }),
    ).toBeInTheDocument()
  })

  it('keeps the email, clears the password and focuses it when the credentials are wrong', async () => {
    const { user } = renderSignIn(() =>
      problem(401, { status: 401, code: 'account.credentials-invalid' }),
    )

    await signIn(user)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Email or password is wrong.Check both and try again.',
    )
    expect(screen.getByLabelText('Email')).toHaveValue('marek@example.com')
    expect(screen.getByLabelText('Password')).toHaveValue('')
    expect(screen.getByLabelText('Password')).toHaveFocus()
  })

  it.each([
    [841, '15 minutes'],
    [840, '14 minutes'],
    [30, '1 minute'],
  ])(
    'tells how long a lock lasts, rounded up (%i s)',
    async (seconds, text) => {
      const { user } = renderSignIn(() =>
        problem(429, {
          status: 429,
          code: 'account.sign-in-locked',
          retryAfterSeconds: seconds,
        }),
      )

      await signIn(user)

      expect(await screen.findByRole('alert')).toHaveTextContent(
        `Too many attempts.Try again in ${text}.`,
      )
      expect(screen.getByRole('button', { name: 'Sign in' })).toBeEnabled()
    },
  )

  it.each([
    [1, 'minutę'],
    [2, 'minuty'],
    [3, 'minuty'],
    [5, 'minut'],
    [12, 'minut'],
    [14, 'minut'],
  ])('uses the Polish plural for %i minutes', async (minutes, word) => {
    await i18n.changeLanguage('pl')
    const { user } = renderSignIn(() =>
      problem(429, {
        status: 429,
        code: 'account.sign-in-locked',
        retryAfterSeconds: minutes * 60,
      }),
    )

    await user.type(screen.getByLabelText('E-mail'), 'marek@example.com')
    await user.type(screen.getByLabelText('Hasło'), 'Kinetiq-2026')
    await user.click(screen.getByRole('button', { name: 'Zaloguj się' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      `Spróbuj ponownie za ${minutes} ${word}.`,
    )
  })

  it('names the email and resends the link when a trainer has not confirmed it', async () => {
    const { requests, user } = renderSignIn((request) =>
      new URL(request.url).pathname === '/api/v1/sign-ins'
        ? problem(403, { status: 403, code: 'account.email-not-confirmed' })
        : new Response(null, { status: 202 }),
    )

    await signIn(user)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'We sent a link to marek@example.com. Open it, then sign in.',
    )
    await user.click(screen.getByRole('button', { name: 'Resend link' }))

    expect(await screen.findByText('Link sent again.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Resend link' })).toBeDisabled()
    expect(await requests.at(-1)!.json()).toEqual({
      email: 'marek@example.com',
    })
  })

  it('blocks a second submit while the first is under way', async () => {
    let answer: (response: Response) => void = () => {}
    const { requests, user } = renderSignIn(
      () => new Promise<Response>((resolve) => (answer = resolve)),
    )

    await signIn(user)
    const button = screen.getByRole('button', { name: 'Sign in' })
    expect(button).toBeDisabled()
    expect(screen.getByLabelText('Email')).toHaveAttribute('readonly')
    await user.click(button)

    expect(requests).toHaveLength(1)
    answer(json(TRAINER))
    expect(
      await screen.findByRole('heading', { name: 'Trainer area' }),
    ).toBeInTheDocument()
  })

  it('shows the generic message when the server cannot be reached', async () => {
    const { user } = renderSignIn(() => {
      throw new TypeError('Failed to fetch')
    })

    await signIn(user)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Something went wrong. Try again.',
    )
  })
})
