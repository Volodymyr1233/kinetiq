import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { json, problem, SIGNED_IN, stubApi, type Reply } from '@/test/page'
import { currentAccountQuery } from '../api/account'
import { SettingsPage } from './settings-page'

const saved = (): Reply => async (request) =>
  json({ ...SIGNED_IN.TRAINER, ...(await request.json()) })

function renderSettings(reply: Reply) {
  const requests = stubApi(reply)
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  // the trainer area's guard has loaded the account before the page renders
  client.setQueryData(currentAccountQuery.queryKey, SIGNED_IN.TRAINER)
  render(
    <QueryClientProvider client={client}>
      <SettingsPage />
    </QueryClientProvider>,
  )
  return { requests, client, user: userEvent.setup() }
}

const saveButton = () => screen.getByRole('button', { name: 'Save changes' })
const patches = (requests: Request[]) =>
  requests.filter((request) => request.method === 'PATCH')

async function rename(user: ReturnType<typeof userEvent.setup>, name: string) {
  const field = screen.getByLabelText('Name')
  await user.clear(field)
  await user.type(field, name)
}

describe('SettingsPage', () => {
  it('shows the stored values with nothing to save', () => {
    renderSettings(saved())

    expect(screen.getByLabelText('Name')).toHaveValue('Marek Nowak')
    expect(screen.getByLabelText('Email')).toHaveValue('marek@example.com')
    expect(screen.getByLabelText('Email')).toHaveAttribute('readonly')
    expect(screen.getByRole('radio', { name: 'kg' })).toBeChecked()
    expect(
      screen.getByRole('combobox', { name: 'Language' }),
    ).toHaveTextContent('English')
    expect(screen.getByRole('radio', { name: 'System' })).toBeChecked()
    expect(saveButton()).toBeDisabled()
  })

  it('sends only what changed and confirms the save', async () => {
    const { requests, client, user } = renderSettings(saved())

    await rename(user, 'Marek Kowalski')
    await user.click(screen.getByRole('radio', { name: 'Dark' }))
    expect(saveButton()).toBeEnabled()
    await user.click(saveButton())

    expect(await screen.findByText('Settings saved.')).toBeInTheDocument()
    expect(await patches(requests)[0].json()).toEqual({
      name: 'Marek Kowalski',
      theme: 'DARK',
    })
    expect(saveButton()).toBeDisabled()
    expect(client.getQueryData(currentAccountQuery.queryKey)).toMatchObject({
      name: 'Marek Kowalski',
      theme: 'DARK',
    })
  })

  it('drops the confirmation once something changes again', async () => {
    const { user } = renderSettings(saved())
    await user.click(screen.getByRole('radio', { name: 'lb' }))
    await user.click(saveButton())
    await screen.findByText('Settings saved.')

    await user.click(screen.getByRole('radio', { name: 'kg' }))

    expect(screen.queryByText('Settings saved.')).not.toBeInTheDocument()
    expect(saveButton()).toBeEnabled()
  })

  it('refuses a blank name without sending anything', async () => {
    const { requests, user } = renderSettings(saved())

    await rename(user, '   ')
    await user.click(saveButton())

    expect(await screen.findByText('Enter your name.')).toBeInTheDocument()
    expect(screen.getByLabelText('Name')).toHaveAttribute(
      'aria-invalid',
      'true',
    )
    expect(patches(requests)).toHaveLength(0)
  })

  it("shows the name message for the server's refusal", async () => {
    const { user } = renderSettings(() =>
      problem(400, {
        status: 400,
        code: 'validation.failed',
        errors: [{ field: 'name', code: 'size' }],
      }),
    )

    await rename(user, 'Marek Kowalski')
    await user.click(saveButton())

    expect(
      await screen.findByText('Use 100 characters or fewer.'),
    ).toBeInTheDocument()
  })

  it('keeps the changes when saving fails', async () => {
    const { user } = renderSettings(() =>
      problem(500, { status: 500, code: 'internal.error' }),
    )

    await rename(user, 'Marek Kowalski')
    await user.click(saveButton())

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Something went wrong on our side. Try again in a moment.',
    )
    expect(screen.getByLabelText('Name')).toHaveValue('Marek Kowalski')
    expect(saveButton()).toBeEnabled()
  })
})
