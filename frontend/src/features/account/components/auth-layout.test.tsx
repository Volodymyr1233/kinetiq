import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { AuthCard, AuthLayout } from './auth-layout'

describe('AuthLayout', () => {
  it('shows the brand in the page banner, apart from the card', () => {
    render(
      <AuthLayout>
        <AuthCard title="Sign in" />
      </AuthLayout>,
    )

    expect(
      within(screen.getByRole('banner')).getByText('Kinetiq'),
    ).toBeInTheDocument()
    expect(
      within(screen.getByRole('main')).getByRole('heading', {
        name: 'Sign in',
      }),
    ).toBeInTheDocument()
  })
})
