import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { json, renderAppAt, SIGNED_IN } from '@/test/page'

const DRAFT = {
  answers: {
    goal: 'BUILD_MUSCLE',
    experience: 'YEARS_1_TO_2',
    level: 'INTERMEDIATE',
    age: '29',
    height: '168',
    weight: '62,5',
    likes: [],
    days: 3,
    notes: '',
  },
  weightUnit: 'KG',
  reached: 4,
}

// the server knows the client's quiz status, ends a sign-in, and keeps the settings it is sent
function server(status: string) {
  let account: object = SIGNED_IN.CLIENT
  return async (request: Request) => {
    const call = `${request.method} ${new URL(request.url).pathname}`
    if (call === 'GET /api/v1/quiz') return json({ status })
    if (call === 'PATCH /api/v1/accounts/me') {
      account = { ...account, ...(await request.json()) }
      return json(account)
    }
    return new Response(null, { status: 204 })
  }
}

async function openSettings() {
  const user = userEvent.setup()
  await user.click(await screen.findByRole('button', { name: 'Settings' }))
  const sheet = within(await screen.findByRole('dialog', { name: 'Settings' }))
  return { sheet, user }
}

describe('quiz area', () => {
  it.each([
    ['a quiz step', '/app/quiz/4', 'NOT_SUBMITTED'],
    ['the waiting state', '/app/quiz/waiting', 'WAITING_FOR_PLAN'],
  ])(
    'opens the settings sheet on %s and signs out, clearing the answers in the tab',
    async (_, path, status) => {
      sessionStorage.setItem('kinetiq.quiz', JSON.stringify(DRAFT))
      const { requests, router } = renderAppAt(
        path,
        SIGNED_IN.CLIENT,
        server(status),
      )

      const { sheet, user } = await openSettings()
      expect(sheet.getByText('Anna Kowalska')).toBeInTheDocument()
      await user.click(sheet.getByRole('button', { name: 'Sign out' }))

      expect(
        await screen.findByRole('heading', { name: 'Sign in' }),
      ).toBeInTheDocument()
      expect(router.state.location.pathname).toBe('/sign-in')
      expect(sessionStorage.length).toBe(0)
      expect(
        requests.some(
          (request) =>
            request.method === 'DELETE' &&
            new URL(request.url).pathname === '/api/v1/sign-ins/current',
        ),
      ).toBe(true)
    },
  )

  it('shows a typed body weight in the unit chosen in the sheet', async () => {
    sessionStorage.setItem('kinetiq.quiz', JSON.stringify(DRAFT))
    renderAppAt('/app/quiz/4', SIGNED_IN.CLIENT, server('NOT_SUBMITTED'))
    expect(
      await screen.findByRole('textbox', { name: 'Body weight' }),
    ).toHaveValue('62,5')

    const { sheet, user } = await openSettings()
    await user.click(
      within(sheet.getByRole('radiogroup', { name: 'Units' })).getByRole(
        'radio',
        { name: 'lb' },
      ),
    )
    await user.keyboard('{Escape}')

    expect(
      await screen.findByRole('textbox', { name: 'Body weight' }),
    ).toHaveValue('137,8')
    expect(JSON.parse(sessionStorage.getItem('kinetiq.quiz')!)).toMatchObject({
      answers: { weight: '137,8' },
      weightUnit: 'LB',
    })
  })
})
