import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import {
  json,
  problem,
  quizStatus,
  renderAppAt,
  SIGNED_IN,
  type Reply,
} from '@/test/page'

// a client with a published plan gets the area itself
const published = (reply?: Reply) => quizStatus('PLAN_PUBLISHED', reply)

async function renderClientAt(path: string) {
  renderAppAt(path, SIGNED_IN.CLIENT, published())
  return within(await screen.findByRole('navigation', { name: 'Client area' }))
}

async function openSheet(reply?: Reply) {
  const { requests } = renderAppAt(
    '/app/today',
    SIGNED_IN.CLIENT,
    published(reply),
  )
  const user = userEvent.setup()
  await user.click(await screen.findByRole('button', { name: 'Settings' }))
  const sheet = within(await screen.findByRole('dialog', { name: 'Settings' }))
  const option = (group: string, name: string) =>
    within(sheet.getByRole('radiogroup', { name: group })).getByRole('radio', {
      name,
    })
  const updates = () =>
    Promise.all(
      requests
        .filter((request) => request.method === 'PATCH')
        .map((request) => request.clone().json()),
    )
  return { sheet, option, updates, user }
}

// the server keeps what it was sent and answers with the whole account
function accountServer(
  before: (body: object) => Promise<void> = async () => {},
) {
  let stored: object = SIGNED_IN.CLIENT
  return (async (request) => {
    if (request.method !== 'PATCH') return new Response(null, { status: 204 })
    const body = await request.json()
    await before(body)
    stored = { ...stored, ...body }
    return json(stored)
  }) satisfies Reply
}

describe('ClientArea', () => {
  it('links every tab from the tab bar', async () => {
    const tabs = await renderClientAt('/app/today')

    expect(
      tabs.getAllByRole('link').map((link) => link.getAttribute('href')),
    ).toEqual(['/app/today', '/app/plan', '/app/progress', '/app/messages'])
  })

  it('opens Today when the area is entered without a tab', async () => {
    const tabs = await renderClientAt('/app')

    expect(tabs.getByRole('link', { name: 'Today' })).toHaveAttribute(
      'aria-current',
      'page',
    )
    expect(screen.getByRole('heading', { name: 'Today' })).toBeInTheDocument()
  })

  it('moves the highlight and the title to the tab that was tapped', async () => {
    const tabs = await renderClientAt('/app/today')

    await userEvent.click(tabs.getByRole('link', { name: 'Progress' }))

    expect(
      await screen.findByRole('heading', { name: 'Progress' }),
    ).toBeInTheDocument()
    expect(tabs.getByRole('link', { name: 'Progress' })).toHaveAttribute(
      'aria-current',
      'page',
    )
    expect(tabs.getByRole('link', { name: 'Today' })).not.toHaveAttribute(
      'aria-current',
    )
  })

  it('signs out from the settings sheet, clearing unfinished quiz answers', async () => {
    sessionStorage.setItem('kinetiq.quiz', '{"goal":"STRENGTH"}')
    const { requests } = renderAppAt(
      '/app/today',
      SIGNED_IN.CLIENT,
      published(),
    )
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Settings' }))
    const sheet = await screen.findByRole('dialog', { name: 'Settings' })
    expect(within(sheet).getByText('Anna Kowalska')).toBeInTheDocument()
    await user.click(within(sheet).getByRole('button', { name: 'Sign out' }))

    expect(
      await screen.findByRole('heading', { name: 'Sign in' }),
    ).toBeInTheDocument()
    expect(sessionStorage.length).toBe(0)
    expect(
      requests.some(
        (request) =>
          request.method === 'DELETE' &&
          new URL(request.url).pathname === '/api/v1/sign-ins/current',
      ),
    ).toBe(true)
  })

  it('shows the email and the stored display settings in the sheet', async () => {
    const { sheet, option } = await openSheet()

    expect(sheet.getByText('anna@example.com')).toBeInTheDocument()
    expect(option('Units', 'kg')).toBeChecked()
    expect(option('Language', 'English')).toBeChecked()
    expect(option('Theme', 'System')).toBeChecked()
    expect(sheet.getByText('Changes save right away.')).toBeInTheDocument()
  })

  it('saves a choice at once, sending only that setting', async () => {
    const { option, updates, user } = await openSheet(accountServer())

    await user.click(option('Language', 'Polski'))

    expect(option('Language', 'Polski')).toBeChecked()
    await waitFor(async () =>
      expect(await updates()).toEqual([{ language: 'PL' }]),
    )
  })

  it('stores two quick choices in the order made and keeps both chosen', async () => {
    let answerFirst = () => {}
    const firstAnswered = new Promise<void>((resolve) => {
      answerFirst = resolve
    })
    const { sheet, option, updates, user } = await openSheet(
      accountServer(async (body) => {
        if ('units' in body) await firstAnswered
      }),
    )

    await user.click(option('Units', 'lb'))
    await user.click(option('Theme', 'Dark'))

    expect(option('Units', 'lb')).toBeChecked()
    expect(option('Theme', 'Dark')).toBeChecked()
    expect(
      sheet.getByText('Weights in your plan and logs show in lb.'),
    ).toBeInTheDocument()
    expect(await updates()).toEqual([{ units: 'LB' }])

    answerFirst()

    await waitFor(async () =>
      expect(await updates()).toEqual([{ units: 'LB' }, { theme: 'DARK' }]),
    )
    expect(option('Units', 'lb')).toBeChecked()
    expect(option('Theme', 'Dark')).toBeChecked()
  })

  it('shows the stored option again with the message when a choice fails', async () => {
    const { sheet, option, user } = await openSheet((request) =>
      request.method === 'PATCH'
        ? problem(500, { status: 500, code: 'internal.error' })
        : new Response(null, { status: 204 }),
    )

    await user.click(option('Theme', 'Dark'))

    expect(await sheet.findByRole('alert')).toHaveTextContent(
      'Something went wrong on our side. Try again in a moment.',
    )
    expect(option('Theme', 'System')).toBeChecked()
    expect(option('Theme', 'Dark')).not.toBeChecked()
  })

  it('keeps the sheet open with the generic message when sign-out fails', async () => {
    renderAppAt(
      '/app/today',
      SIGNED_IN.CLIENT,
      published(() => problem(500, { status: 500, code: 'internal.error' })),
    )
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Settings' }))
    const sheet = await screen.findByRole('dialog', { name: 'Settings' })
    await user.click(within(sheet).getByRole('button', { name: 'Sign out' }))

    expect(await within(sheet).findByRole('alert')).toHaveTextContent(
      'Something went wrong on our side. Try again in a moment.',
    )
  })

  it('asks an unconfirmed client to confirm the email on a tab page', async () => {
    renderAppAt(
      '/app/plan',
      { ...SIGNED_IN.CLIENT, emailConfirmed: false },
      published(),
    )

    expect(
      await screen.findByText(
        'Confirm your email: we sent a link to anna@example.com.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Resend' })).toBeInTheDocument()
  })

  it('shows no banner to a confirmed client', async () => {
    await renderClientAt('/app/today')

    expect(screen.queryByText(/Confirm your email/)).not.toBeInTheDocument()
  })

  it.each(['/app', '/app/today', '/app/plan', '/app/messages'])(
    'sends a client who has not submitted the quiz from %s to the quiz',
    async (path) => {
      const { router } = renderAppAt(
        path,
        SIGNED_IN.CLIENT,
        quizStatus('NOT_SUBMITTED'),
      )

      expect(
        await screen.findByRole('heading', { name: 'What do you train for?' }),
      ).toBeInTheDocument()
      expect(router.state.location.pathname).toBe('/app/quiz/1')
      expect(
        screen.queryByRole('navigation', { name: 'Client area' }),
      ).not.toBeInTheDocument()
    },
  )

  it('sends a client waiting for a plan to the waiting state', async () => {
    const { router } = renderAppAt(
      '/app/today',
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

  it('shows only a loading status while the quiz status is read', async () => {
    renderAppAt('/app/today', SIGNED_IN.CLIENT, (request) =>
      new URL(request.url).pathname === '/api/v1/quiz'
        ? new Promise<Response>(() => {})
        : new Response(null, { status: 204 }),
    )

    expect(await screen.findByRole('status')).toHaveTextContent('Loading')
    expect(
      screen.queryByRole('navigation', { name: 'Client area' }),
    ).not.toBeInTheDocument()
  })

  it('shows the message and Try again when the quiz status cannot be read, then the area', async () => {
    const user = userEvent.setup()
    let failures = 1
    renderAppAt('/app/today', SIGNED_IN.CLIENT, (request) =>
      new URL(request.url).pathname !== '/api/v1/quiz'
        ? new Response(null, { status: 204 })
        : failures-- > 0
          ? problem(500, { status: 500, code: 'internal.error' })
          : json({ status: 'PLAN_PUBLISHED' }),
    )

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Something went wrong on our side. Try again in a moment.',
    )
    expect(
      screen.queryByRole('navigation', { name: 'Client area' }),
    ).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Try again' }))

    expect(
      await screen.findByRole('navigation', { name: 'Client area' }),
    ).toBeInTheDocument()
  })
})
