import {
  queryOptions,
  useMutation,
  useQueryClient,
  type QueryClient,
} from '@tanstack/react-query'
import { useSyncExternalStore } from 'react'
import { useNavigate } from 'react-router'
import { api } from '@/lib/api/client'
import type { components } from '@/lib/api/schema'
import { clearPendingLogs } from '@/lib/pending-logs'
import { queryClient } from '@/lib/query-client'
import { browserTimeZone } from '../time-zone'

type Schemas = components['schemas']
export type CurrentAccount = Schemas['CurrentAccountResponse']

// openapi-fetch returns errors instead of throwing; throw the problem detail so the mutation fails with its code
async function send<T>(call: Promise<{ error?: T }>) {
  const { error } = await call
  if (error !== undefined) throw error
}

export async function receive<D, E>(call: Promise<{ data?: D; error?: E }>) {
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

const TAB_OWNER = 'kinetiq.account'

/** The tab keeps data for one account: a tab that another account used here starts empty for this one. */
export function claimTabStorage(account: CurrentAccount) {
  try {
    const owner = sessionStorage.getItem(TAB_OWNER)
    if (owner !== null && owner !== account.id) sessionStorage.clear()
    sessionStorage.setItem(TAB_OWNER, account.id)
  } catch {
    // storage can be unavailable (private mode); then the tab keeps nothing to protect
  }
}

export const currentAccountQuery = queryOptions({
  queryKey: ['current-account'],
  queryFn: () => receive(api.GET('/api/v1/accounts/me')),
  // the guards wait on it, so a failure shows "Try again" at once instead of after several retries
  retry: false,
})

/**
 * The account as the cache holds it, for what sits above the routes. It never fetches, because public pages must not
 * ask, and it follows signing out, which a query observer mounted this high would miss: `clear` does not tell observers.
 */
export function useCachedAccount() {
  const queryClient = useQueryClient()
  return useSyncExternalStore(
    (changed) => queryClient.getQueryCache().subscribe(changed),
    () => queryClient.getQueryData(currentAccountQuery.queryKey),
  )
}

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

export type AccountUpdate = Schemas['UpdateAccountRequest']

export const updateAccountKey = ['update-account']

/** Changes the fields sent and puts the answer in the cache, so every screen and the app's language and theme follow. */
export function useUpdateAccount() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationKey: updateAccountKey,
    // one after another in the order made, so the last of several quick choices is what stays stored
    scope: { id: 'update-account' },
    mutationFn: (body: AccountUpdate) =>
      receive(api.PATCH('/api/v1/accounts/me', { body })),
    onSuccess: (account) => {
      queryClient.setQueryData(currentAccountQuery.queryKey, account)
    },
  })
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

export function useRequestPasswordReset() {
  return useMutation({
    mutationFn: (email: string) =>
      send(api.POST('/api/v1/password-reset-links', { body: { email } })),
  })
}

export function useSetPassword() {
  return useMutation({
    mutationFn: (body: Schemas['SetPasswordRequest']) =>
      send(api.POST('/api/v1/password-resets', { body })),
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

/** Who sent the invite and to which email; changes nothing, so opening the link twice is harmless. */
export const inviteCheckQuery = (token: string) =>
  queryOptions({
    queryKey: ['invite-check', token],
    queryFn: () =>
      receive(api.POST('/api/v1/invite-checks', { body: { token } })),
  })

/** Creates the client account and signs it in here, replacing any earlier sign-in in this browser. */
export function useJoin() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: Schemas['AcceptInviteRequest']) =>
      receive(api.POST('/api/v1/invite-acceptances', { body })),
    onSuccess: (account) => {
      clearTabStorage()
      queryClient.setQueryData(currentAccountQuery.queryKey, account)
    },
  })
}

/** Forgets everything this browser keeps for the account that was signed in here. */
function forgetAccount(cache: QueryClient) {
  clearTabStorage()
  // nothing of this account may stay on the phone; without a cached account every kept log goes
  clearPendingLogs(cache.getQueryData(currentAccountQuery.queryKey)?.id)
  cache.clear()
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
      forgetAccount(queryClient)
      void navigate('/sign-in', { replace: true })
    },
  })
}

/** A trainer's number of clients and a client's trainer's name, which the delete confirmation needs. */
export const accountDeletionQuery = queryOptions({
  queryKey: ['account-deletion'],
  queryFn: () => receive(api.GET('/api/v1/accounts/me/deletion')),
})

/** The sign-in page's state after a deletion, so it can say the account is gone. */
export type SignInState = { accountDeleted?: boolean }

/** Deletes the signed-in account after its password; the server ends every sign-in, this browser forgets it. */
export function useDeleteAccount() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  return useMutation({
    mutationFn: (password: string) =>
      send(api.POST('/api/v1/accounts/me/deletion', { body: { password } })),
    onSuccess: () => {
      forgetAccount(queryClient)
      const state: SignInState = { accountDeleted: true }
      void navigate('/sign-in', { replace: true, state })
    },
  })
}
