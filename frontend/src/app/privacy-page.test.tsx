import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { renderAppAt } from '@/test/page'

describe('PrivacyPage', () => {
  it('shows the notice to someone who is not signed in, without asking who they are', async () => {
    const { requests } = renderAppAt('/privacy', null)

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Privacy notice' }),
    ).toBeInTheDocument()
    expect(screen.getByText(/^Last updated /)).toBeInTheDocument()
    expect(requests).toHaveLength(0)
  })

  it('has every section of the notice, and a contact link', async () => {
    renderAppAt('/privacy', null)

    const headings = (await screen.findAllByRole('heading', { level: 2 })).map(
      (heading) => heading.textContent,
    )
    expect(headings).toEqual([
      'Who is responsible',
      'What we keep about trainers',
      'What we keep about clients',
      'Why we keep it',
      'Who receives it',
      'How long we keep it',
      'How we protect it',
      'Age',
      'Deleting your account',
      'Your rights',
    ])
    const responsible = screen.getByRole('region', {
      name: 'Who is responsible',
    })
    expect(
      within(responsible).getByRole('link', { name: 'privacy@kinetiq.page' }),
    ).toHaveAttribute('href', 'mailto:privacy@kinetiq.page')
  })
})
