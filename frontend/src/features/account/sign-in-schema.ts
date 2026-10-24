import { z } from 'zod'

// only emptiness is checked here and on the server; any other wrong value gets the wrong-email-or-password answer
export const signInSchema = z.object({
  email: z.string().trim().min(1, 'account.errors.signInEmail'),
  // never trimmed: spaces may be part of a password
  password: z.string().min(1, 'account.errors.signInPassword'),
})

export type SignInValues = z.input<typeof signInSchema>
