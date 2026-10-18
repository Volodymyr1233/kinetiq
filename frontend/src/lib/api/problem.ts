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
