import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ThemeProvider } from '@/components/theme-provider'
import { THEME_STORAGE_KEY, useTheme } from '@/lib/theme'

function stubOsPreference(prefersDark: boolean) {
  vi.stubGlobal('matchMedia', () => ({
    matches: prefersDark,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  }))
}

function DarkSwitch() {
  const { setTheme } = useTheme()
  return <button onClick={() => setTheme('dark')}>dark</button>
}

const isDark = () => document.documentElement.classList.contains('dark')

describe('ThemeProvider', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.classList.remove('dark')
  })

  it('follows the OS preference when no theme was chosen', () => {
    stubOsPreference(true)

    render(<ThemeProvider>content</ThemeProvider>)

    expect(isDark()).toBe(true)
  })

  it('keeps the stored choice over the OS preference', () => {
    stubOsPreference(true)
    localStorage.setItem(THEME_STORAGE_KEY, 'light')

    render(<ThemeProvider>content</ThemeProvider>)

    expect(isDark()).toBe(false)
  })

  it('applies and remembers the theme the user picks', async () => {
    stubOsPreference(false)
    render(
      <ThemeProvider>
        <DarkSwitch />
      </ThemeProvider>,
    )

    await userEvent.click(screen.getByRole('button', { name: 'dark' }))

    expect(isDark()).toBe(true)
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark')
  })
})
