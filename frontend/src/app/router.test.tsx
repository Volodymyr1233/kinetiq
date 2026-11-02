import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { json, problem, quizStatus, renderAppAt, SIGNED_IN } from '@/test/page'

function zoneOfBrowser(zone: string) {
  const options = new Intl.DateTimeFormat().resolvedOptions()
  vi.spyOn(Intl.DateTimeFormat.prototype, 'resolvedOptions').mockReturnValue({
    ...options,
    timeZone: zone,
  })
}

async function signIn(email: string) {
  const user = userEvent.setup()
  await user.type(await screen.findByLabelText('Email'), email)
  await user.type(screen.getByLabelText('Password'), 'Kinetiq-2026')
  await user.click(screen.getByRole('button', { name: 'Sign in' }))
}

describe('routes', () => {
  afterEach(() => sessionStorage.clear())

  it.each([
    ['/trainer', SIGNED_IN.TRAINER, 'PLAN_PUBLISHED', 'Clients'],
    ['/app', SIGNED_IN.CLIENT, 'PLAN_PUBLISHED', 'Today'],
    ['/app/quiz', SIGNED_IN.CLIENT, 'NOT_SUBMITTED', 'What do you train for?'],
  ])('loads the area at %s', async (path, account, status, heading) => {
    renderAppAt(path, account, quizStatus(status))

    expect(
      await screen.findByRole('heading', { name: heading }),
    ).toBeInTheDocument()
  })

  it('reaches the open invites from the clients section, which stays current', async () => {
    const user = userEvent.setup()
    const { router } = renderAppAt('/trainer/clients', SIGNED_IN.TRAINER, () =>
      json({ invites: [] }),
    )

    await user.click(await screen.findByRole('link', { name: 'Open invites' }))

    expect(
      await screen.findByRole('heading', { name: 'Open invites' }),
    ).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/trainer/clients/invites')
    expect(screen.getByRole('link', { name: 'Clients' })).toHaveAttribute(
      'aria-current',
      'page',
    )
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
    renderAppAt(
      '/app/quiz/review',
      SIGNED_IN.CLIENT,
      quizStatus('NOT_SUBMITTED'),
    )

    expect(
      await screen.findByRole('heading', { name: 'Check your answers' }),
    ).toBeInTheDocument()
  })

  it('opens the waiting page, not a step, at /app/quiz/waiting', async () => {
    const { router } = renderAppAt(
      '/app/quiz/waiting',
      SIGNED_IN.CLIENT,
      quizStatus('WAITING_FOR_PLAN'),
    )

    expect(
      await screen.findByRole('heading', {
        name: 'Your trainer is reviewing your plan',
      }),
    ).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/app/quiz/waiting')
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

describe('guards', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    sessionStorage.clear()
  })

  it.each([
    '/trainer/exercises',
    '/app/today',
    '/app/quiz/goal',
    '/app/quiz/review',
  ])(
    'sends a visitor who is not signed in from %s to sign-in',
    async (path) => {
      const { router } = renderAppAt(path, null)

      expect(
        await screen.findByRole('heading', { name: 'Sign in' }),
      ).toBeInTheDocument()
      expect(router.state.location.pathname).toBe('/sign-in')
      expect(router.state.location.search).toBe(
        `?next=${encodeURIComponent(path)}`,
      )
    },
  )

  it('returns to the page asked for, with its query, after signing in', async () => {
    const { router } = renderAppAt(
      '/trainer/exercises?from=mail',
      null,
      (request) =>
        new URL(request.url).pathname === '/api/v1/sign-ins'
          ? json(SIGNED_IN.TRAINER)
          : json({ total: 0, exercises: [] }),
    )

    await signIn('marek@example.com')

    await waitFor(() =>
      expect(router.state.location.pathname).toBe('/trainer/exercises'),
    )
    expect(router.state.location.search).toBe('?from=mail')
  })

  it('lands in the own area when the page asked for belongs to the other role', async () => {
    const { router } = renderAppAt(
      '/trainer/exercises',
      null,
      quizStatus('PLAN_PUBLISHED', (request) =>
        new URL(request.url).pathname === '/api/v1/sign-ins'
          ? json(SIGNED_IN.CLIENT)
          : new Response(null, { status: 204 }),
      ),
    )

    await signIn('anna@example.com')

    await waitFor(() =>
      expect(router.state.location.pathname).toBe('/app/today'),
    )
  })

  it('sends a client who opens the trainer area to the client app', async () => {
    const { router } = renderAppAt(
      '/trainer/clients',
      SIGNED_IN.CLIENT,
      quizStatus('PLAN_PUBLISHED'),
    )

    expect(
      await screen.findByRole('heading', { name: 'Today' }),
    ).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/app/today')
    expect(
      screen.queryByRole('navigation', { name: 'Trainer area' }),
    ).not.toBeInTheDocument()
  })

  it.each(['/app/today', '/app/quiz', '/app/quiz/review'])(
    'sends a trainer who opens %s to the trainer area',
    async (path) => {
      const { router } = renderAppAt(path, SIGNED_IN.TRAINER)

      expect(
        await screen.findByRole('navigation', { name: 'Trainer area' }),
      ).toBeInTheDocument()
      expect(router.state.location.pathname).toBe('/trainer/clients')
    },
  )

  it('sends a signed-in person away from the sign-in page', async () => {
    const { router } = renderAppAt(
      '/sign-in',
      SIGNED_IN.CLIENT,
      quizStatus('PLAN_PUBLISHED'),
    )

    await waitFor(() =>
      expect(router.state.location.pathname).toBe('/app/today'),
    )
  })

  it('offers to try again when it cannot find out who is signed in', async () => {
    renderAppAt('/trainer/clients', SIGNED_IN.TRAINER)
    vi.mocked(fetch).mockImplementation(async () =>
      problem(500, { status: 500, code: 'internal.error' }),
    )

    expect(
      await screen.findByRole('button', { name: 'Try again' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Something went wrong. Try again.',
    )
  })

  it('stores the browser time zone when it differs from the stored one', async () => {
    zoneOfBrowser('Europe/London')
    const { requests } = renderAppAt('/trainer/clients', SIGNED_IN.TRAINER)

    await screen.findByRole('navigation', { name: 'Trainer area' })

    await waitFor(() =>
      expect(
        requests.filter((request) => request.method === 'PUT'),
      ).toHaveLength(1),
    )
    const put = requests.find((request) => request.method === 'PUT')!
    expect(await put.json()).toEqual({ timeZone: 'Europe/London' })
  })

  it('sends no time zone when the browser has the stored one', async () => {
    zoneOfBrowser('Europe/Warsaw')
    const { requests } = renderAppAt('/trainer/clients', SIGNED_IN.TRAINER)

    await screen.findByRole('navigation', { name: 'Trainer area' })

    expect(requests.some((request) => request.method === 'PUT')).toBe(false)
  })

  // an answer to step 1, which lets step 2 open
  const STARTED = JSON.stringify({
    answers: { goal: 'BUILD_MUSCLE' },
    reached: 2,
  })

  it('does not show quiz answers that another account left in the tab', async () => {
    sessionStorage.setItem(
      'kinetiq.account',
      '00000000-0000-4000-8000-000000000005',
    )
    sessionStorage.setItem('kinetiq.quiz', STARTED)
    renderAppAt('/app/quiz/2', SIGNED_IN.CLIENT, quizStatus('NOT_SUBMITTED'))

    expect(
      await screen.findByRole('heading', { name: 'What do you train for?' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('radio', { name: /Build muscle/ }),
    ).not.toBeChecked()
  })

  it('keeps the quiz answers of the account signed in', async () => {
    sessionStorage.setItem('kinetiq.account', SIGNED_IN.CLIENT.id)
    sessionStorage.setItem('kinetiq.quiz', STARTED)
    renderAppAt('/app/quiz/2', SIGNED_IN.CLIENT, quizStatus('NOT_SUBMITTED'))

    expect(
      await screen.findByRole('heading', {
        name: 'How long have you trained regularly?',
      }),
    ).toBeInTheDocument()
  })
})
