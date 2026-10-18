import { z } from 'zod'

// same rules as the backend's SignUpRequest, Email and RawPassword; lengths in UTF-16 units on both sides
export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function isValidPassword(value: string) {
  return (
    value.length >= 8 &&
    value.length <= 128 &&
    /[a-z]/.test(value) &&
    /[A-Z]/.test(value) &&
    /[0-9]/.test(value) &&
    /[^a-zA-Z0-9]/.test(value)
  )
}

// messages are translation keys, rendered with t()
export const emailSchema = z
  .string()
  .trim()
  .max(254, 'account.errors.email')
  .regex(EMAIL_PATTERN, 'account.errors.email')

export const signUpSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, 'account.errors.name')
    .max(100, 'account.errors.nameLength')
    .refine((name) => !/\p{Cc}/u.test(name), 'account.errors.name'),
  email: emailSchema,
  password: z.string().refine(isValidPassword, 'account.passwordRules'),
  adult: z.boolean().refine((adult) => adult, 'account.errors.adult'),
})

export type SignUpValues = z.input<typeof signUpSchema>
