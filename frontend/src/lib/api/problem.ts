import i18n from '@/lib/i18n'

// the backend's problem detail; only its code is ever shown, as translated text
export function errorMessageKey(error: unknown): string {
  const code =
    typeof error === 'object' &&
    error !== null &&
    'code' in error &&
    typeof error.code === 'string'
      ? error.code
      : undefined
  const key = `errors.${code}`
  return code && i18n.exists(key) ? key : 'errors.generic'
}

/** Tells whether a failed query may succeed when asked again: not for a 4xx problem detail, which answers the same. */
export function isWorthRetrying(error: unknown): boolean {
  return !(
    typeof error === 'object' &&
    error !== null &&
    'status' in error &&
    typeof error.status === 'number' &&
    error.status < 500
  )
}
