import '@testing-library/jest-dom/vitest'
import { cleanup, configure } from '@testing-library/react'
import { afterEach, vi } from 'vitest'
import '@/lib/i18n'

// lazy route chunks take seconds to load in a cold, busy run; the default wait is one second
configure({ asyncUtilTimeout: 8000 })

// jsdom has no matchMedia; components that read breakpoints get a desktop default
window.matchMedia ??= (query) =>
  ({
    matches: false,
    media: query,
    addEventListener: () => {},
    removeEventListener: () => {},
  }) as unknown as MediaQueryList

// jsdom has no ResizeObserver; Radix measures some controls with it
globalThis.ResizeObserver ??= class {
  observe() {}
  unobserve() {}
  disconnect() {}
}

// jsdom has no pointer capture; a sonner message captures the pointer when it is pressed, to let it be swiped
Element.prototype.setPointerCapture ??= () => {}
Element.prototype.releasePointerCapture ??= () => {}
Element.prototype.hasPointerCapture ??= () => false

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  // what a tab keeps, such as a message draft, must not leak into the next test
  sessionStorage.clear()
})
