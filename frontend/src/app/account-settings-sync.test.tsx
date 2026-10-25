import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, render, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AccountSettingsSync } from '@/app/account-settings-sync'
import { ThemeProvider } from '@/components/theme-provider'
import { currentAccountQuery } from '@/features/account/api/account'
import i18n, { LANGUAGE_STORAGE_KEY } from '@/lib/i18n'
import { THEME_STORAGE_KEY } from '@/lib/theme'
import { SIGNED_IN } from '@/test/page'

const isDark = () => document.documentElement.classList.contains('dark')

function renderSync() {
  const client = new QueryClient()
  render(
    <ThemeProvider>
      <QueryClientProvider client={client}>
        <AccountSettingsSync />
      </QueryClientProvider>
    </ThemeProvider>,
  )
  return client
}

describe('AccountSettingsSync', () => {
  beforeEach(async () => {
    localStorage.setItem(THEME_STORAGE_KEY, 'light')
    await i18n.changeLanguage('en')
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(async () => {
    await i18n.changeLanguage('en')
    localStorage.clear()
    document.documentElement.classList.remove('dark')
  })

  it("shows the account's language and theme over the device's", async () => {
    const client = renderSync()

    act(() => {
      client.setQueryData(currentAccountQuery.queryKey, {
        ...SIGNED_IN.CLIENT,
        language: 'PL',
        theme: 'DARK',
      })
    })

    await waitFor(() => expect(isDark()).toBe(true))
    expect(i18n.language).toBe('pl')
    expect(localStorage.getItem(LANGUAGE_STORAGE_KEY)).toBe('pl')
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark')
  })

  it('keeps them on the device after sign-out', async () => {
    const client = renderSync()
    act(() => {
      client.setQueryData(currentAccountQuery.queryKey, {
        ...SIGNED_IN.CLIENT,
        language: 'PL',
        theme: 'DARK',
      })
    })
    await waitFor(() => expect(isDark()).toBe(true))

    act(() => client.clear())

    expect(i18n.language).toBe('pl')
    expect(isDark()).toBe(true)
  })

  it('asks the server nothing while nobody is signed in', () => {
    renderSync()

    expect(fetch).not.toHaveBeenCalled()
    expect(i18n.language).toBe('en')
    expect(isDark()).toBe(false)
  })
})
