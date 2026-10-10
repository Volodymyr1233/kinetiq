import { useEffect, useState, type ReactNode } from 'react'
import {
  THEME_STORAGE_KEY,
  THEMES,
  ThemeContext,
  type Theme,
} from '@/lib/theme'

function storedTheme(): Theme {
  const stored = localStorage.getItem(THEME_STORAGE_KEY)
  return THEMES.find((theme) => theme === stored) ?? 'system'
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<Theme>(storedTheme)

  useEffect(() => {
    const osPrefersDark = window.matchMedia('(prefers-color-scheme: dark)')
    const apply = () =>
      document.documentElement.classList.toggle(
        'dark',
        theme === 'dark' || (theme === 'system' && osPrefersDark.matches),
      )
    apply()
    osPrefersDark.addEventListener('change', apply)
    return () => osPrefersDark.removeEventListener('change', apply)
  }, [theme])

  const setTheme = (next: Theme) => {
    localStorage.setItem(THEME_STORAGE_KEY, next)
    setThemeState(next)
  }

  return <ThemeContext value={{ theme, setTheme }}>{children}</ThemeContext>
}
