import { createContext, useContext } from 'react'

export const THEMES = ['light', 'dark', 'system'] as const
export type Theme = (typeof THEMES)[number]

export const THEME_STORAGE_KEY = 'kinetiq-theme'

type ThemeContextValue = { theme: Theme; setTheme: (theme: Theme) => void }

export const ThemeContext = createContext<ThemeContextValue | null>(null)

export function useTheme() {
  const value = useContext(ThemeContext)
  if (!value) throw new Error('useTheme must be used inside ThemeProvider')
  return value
}
