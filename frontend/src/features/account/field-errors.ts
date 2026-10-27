// one message per field and rule, the same whether the form or the server found it
export function serverMessage(field: string, code: string) {
  if (field === 'name') {
    return code === 'size' ? 'account.errors.nameLength' : 'account.errors.name'
  }
  if (field === 'password') return 'account.passwordRules'
  return `account.errors.${field}`
}

/** The server's field errors for the fields the form shows; others, such as the time zone, are left out. */
export function serverFieldErrors<Field extends string>(
  error: unknown,
  fields: readonly Field[],
) {
  const errors =
    typeof error === 'object' && error !== null && 'errors' in error
      ? error.errors
      : undefined
  if (!Array.isArray(errors)) return []
  return errors.filter(
    (fieldError): fieldError is { field: Field; code: string } =>
      fields.includes(fieldError?.field),
  )
}
