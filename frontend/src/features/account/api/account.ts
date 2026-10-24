import {
  queryOptions,
  useMutation,
  useQueryClient,
} from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { api } from '@/lib/api/client'
import type { components } from '@/lib/api/schema'
import { queryClient } from '@/lib/query-client'
import { browserTimeZone } from '../time-zone'

type Schemas = components['schemas']
export type CurrentAccount = Schemas['CurrentAccountResponse']

// openapi-fetch returns errors instead of throwing; throw the problem detail so the mutation fails with its code
async function send<T>(call: Promise<{ error?: T }>) {
  const { error } = await call
  if (error !== undefined) throw error
}

async function receive<D, E>(call: Promise<{ data?: D; error?: E }>) {
  const { data, error } = await call
  if (error !== undefined) throw error
  return data as D
}

export function statusOf(error: unknown) {
  return typeof error === 'object' && error !== null && 'status' in error
    ? error.status
    : undefined
}

/** What the tab keeps, such as unfinished quiz answers, must not reach the next person who signs in here. */
export function clearTabStorage() {
  try {
    sessionStorage.clear()
  } catch {
    // storage can be unavailable (private mode); then there is nothing to clear
  }
}

export const currentAccountQuery = queryOptions({
  queryKey: ['current-account'],
  queryFn: () => receive(api.GET('/api/v1/accounts/me')),
  // the guards wait on it, so a failure shows "Try again" at once instead of after several retries
  retry: false,
})

/** Stores the browser's zone when it differs, so weeks follow the person; a failure changes nothing they see. */
export async function refreshTimeZone(account: CurrentAccount) {
  const timeZone = browserTimeZone()
  if (!timeZone || timeZone === account.timeZone) return
  const { error } = await api.PUT('/api/v1/accounts/me/time-zone', {
    body: { timeZone },
  })
  if (error === undefined) {
    queryClient.setQueryData(currentAccountQuery.queryKey, {
      ...account,
      timeZone,
    })
  }
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

export function useSignIn() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: Schemas['SignInRequest']) =>
      receive(api.POST('/api/v1/sign-ins', { body })),
    onSuccess: (account) => {
      clearTabStorage()
      queryClient.setQueryData(currentAccountQuery.queryKey, account)
    },
  })
}

export function useSignOut() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  return useMutation({
    mutationFn: async () => {
      try {
        await send(api.DELETE('/api/v1/sign-ins/current'))
      } catch (error) {
        // a sign-in that already ended is as good as signed out
        if (statusOf(error) !== 401) throw error
      }
    },
    onSuccess: () => {
      clearTabStorage()
      queryClient.clear()
      void navigate('/sign-in', { replace: true })
    },
  })
}
