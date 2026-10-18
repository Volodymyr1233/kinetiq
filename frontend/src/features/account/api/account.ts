import { useMutation } from '@tanstack/react-query'
import { api } from '@/lib/api/client'
import type { components } from '@/lib/api/schema'

type Schemas = components['schemas']

// openapi-fetch returns errors instead of throwing; throw the problem detail so the mutation fails with its code
async function send<T>(call: Promise<{ error?: T }>) {
  const { error } = await call
  if (error !== undefined) throw error
}

export function useSignUp() {
  return useMutation({
    mutationFn: (body: Schemas['SignUpRequest']) =>
      send(api.POST('/api/v1/accounts', { body })),
  })
}

export function useConfirmEmail() {
  return useMutation({
    mutationFn: (token: string) =>
      send(api.POST('/api/v1/email-confirmations', { body: { token } })),
  })
}

export function useRequestConfirmationLink() {
  return useMutation({
    mutationFn: (email: string) =>
      send(api.POST('/api/v1/confirmation-links', { body: { email } })),
  })
}
