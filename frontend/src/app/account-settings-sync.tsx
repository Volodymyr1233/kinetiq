import { useEffect } from 'react'
import { useCachedAccount } from '@/features/account/api/account'
import i18n from '@/lib/i18n'
import { useTheme } from '@/lib/theme'

const THEMES = { LIGHT: 'light', DARK: 'dark', SYSTEM: 'system' } as const

/** Shows the signed-in account's language and theme on every device, whichever way the account was loaded. */
export function AccountSettingsSync() {
  const account = useCachedAccount()
  const { theme, setTheme } = useTheme()
  const language = account?.language.toLowerCase()
  const accountTheme = account && THEMES[account.theme]

  useEffect(() => {
    if (language && language !== i18n.language)
      void i18n.changeLanguage(language)
  }, [language])

  useEffect(() => {
    if (accountTheme && accountTheme !== theme) setTheme(accountTheme)
  }, [accountTheme, theme, setTheme])

  return null
}
