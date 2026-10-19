import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach, vi } from 'vitest'
import '@/lib/i18n'

// jsdom has no matchMedia; components that read breakpoints get a desktop default
window.matchMedia ??= (query) =>
  ({
    matches: false,
    media: query,
    addEventListener: () => {},
    removeEventListener: () => {},
  }) as unknown as MediaQueryList

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})
