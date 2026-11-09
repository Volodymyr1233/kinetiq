import { vi } from 'vitest'

/** Waits for a check to pass, as long as Testing Library's own waits do (`vi.waitFor` alone gives up after 1 s). */
export const eventually = (check: () => unknown) =>
  vi.waitFor(check, { timeout: 8000 })
