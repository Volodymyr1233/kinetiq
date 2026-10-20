import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { SignUpPage } from './sign-up-page'
import { pathsOf, problem, renderPage, stubApi } from '@/test/page'

const RULES =
  '8–128 characters with at least one lower-case letter, one upper-case letter, one digit and one special character.'

async function fillForm(
  user: ReturnType<typeof userEvent.setup>,
  { password = 'Kinetiq-2026' } = {},
) {
  await user.type(screen.getByLabelText('Name'), 'Marek Nowak')
  await user.type(screen.getByLabelText('Email'), ' Marek@Example.com ')
  await user.type(screen.getByLabelText('Password'), password)
  await user.click(screen.getByRole('checkbox', { name: 'I am 18 or older' }))
}

function submit(user: ReturnType<typeof userEvent.setup>) {
  return user.click(screen.getByRole('button', { name: 'Create account' }))
}

describe('SignUpPage', () => {
  it('shows a message next to each field and sends nothing when the form is empty', async () => {
    const requests = stubApi(() => new Response(null, { status: 202 }))
    const user = userEvent.setup()
    renderPage(<SignUpPage />)

    await submit(user)

    expect(await screen.findByText('Enter your name.')).toBeInTheDocument()
    expect(
      screen.getByText('Enter an email like name@example.com.'),
    ).toBeInTheDocument()
    expect(
      screen.getByText('Confirm that you are 18 or older.'),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('Password')).toHaveAttribute(
      'aria-invalid',
      'true',
    )
    expect(screen.getByLabelText('Name')).toHaveFocus()
    expect(requests).toHaveLength(0)
  })

  it('turns the password rules into the error when the password breaks them', async () => {
    stubApi(() => new Response(null, { status: 202 }))
    const user = userEvent.setup()
    renderPage(<SignUpPage />)
    await fillForm(user, { password: 'kinetiq-2026' })

    await submit(user)

    expect(await screen.findByRole('alert')).toHaveTextContent(RULES)
    expect(screen.getByLabelText('Password')).toHaveFocus()
  })

  it('shows the field message for an error the server found', async () => {
    stubApi(() =>
      problem(400, {
        code: 'validation.failed',
        errors: [{ field: 'email', code: 'pattern' }],
      }),
    )
    const user = userEvent.setup()
    renderPage(<SignUpPage />)
    await fillForm(user)

    await submit(user)

    expect(
      await screen.findByText('Enter an email like name@example.com.'),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toHaveFocus()
  })

  it('sends one request while the first is in progress', async () => {
    const requests = stubApi(() => new Promise<Response>(() => {}))
    const user = userEvent.setup()
    renderPage(<SignUpPage />)
    await fillForm(user)

    await submit(user)
    await submit(user)

    expect(
      screen.getByRole('button', { name: 'Create account' }),
    ).toBeDisabled()
    expect(screen.getByLabelText('Name')).toHaveAttribute('readonly')
    expect(pathsOf(requests)).toEqual(['/api/v1/accounts'])
  })

  it('keeps what was entered and shows the generic message when the connection fails', async () => {
    stubApi(() => Promise.reject(new TypeError('Failed to fetch')))
    const user = userEvent.setup()
    renderPage(<SignUpPage />)
    await fillForm(user)

    await submit(user)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Something went wrong. Try again.',
    )
    expect(screen.getByLabelText('Name')).toHaveValue('Marek Nowak')
    expect(screen.getByLabelText('Password')).toHaveValue('Kinetiq-2026')
  })

  it('sends the trimmed values with the time zone and moves focus to "Check your email"', async () => {
    const requests = stubApi(() => new Response(null, { status: 202 }))
    const user = userEvent.setup()
    renderPage(<SignUpPage />)
    await fillForm(user)

    await submit(user)

    const heading = await screen.findByRole('heading', {
      name: 'Check your email',
    })
    expect(heading).toHaveFocus()
    expect(screen.getByText('Marek@Example.com')).toBeInTheDocument()
    const body = await requests[0].json()
    expect(body).toMatchObject({
      name: 'Marek Nowak',
      email: 'Marek@Example.com',
      password: 'Kinetiq-2026',
      adult: true,
    })
    expect(body.timeZone).toEqual(expect.any(String))
  })

  it('brings back the form without the password when the email is changed', async () => {
    stubApi(() => new Response(null, { status: 202 }))
    const user = userEvent.setup()
    renderPage(<SignUpPage />)
    await fillForm(user)
    await submit(user)

    await user.click(await screen.findByRole('button', { name: 'Change it' }))

    expect(screen.getByLabelText('Name')).toHaveValue('Marek Nowak')
    expect(screen.getByLabelText('Email')).toHaveValue('Marek@Example.com')
    expect(screen.getByLabelText('Password')).toHaveValue('')
  })

  it('locks Resend link and says the link was sent again', async () => {
    const requests = stubApi(() => new Response(null, { status: 202 }))
    const user = userEvent.setup()
    renderPage(<SignUpPage />)
    await fillForm(user)
    await submit(user)

    await user.click(await screen.findByRole('button', { name: 'Resend link' }))

    expect(
      await screen.findByText(
        'Sent again. You can send another link in a minute.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Resend link' })).toBeDisabled()
    expect(pathsOf(requests)).toEqual([
      '/api/v1/accounts',
      '/api/v1/confirmation-links',
    ])
  })
})
