import { redirect, type LoaderFunctionArgs } from 'react-router'
import {
  claimTabStorage,
  currentAccountQuery,
  refreshTimeZone,
  statusOf,
  type CurrentAccount,
} from '@/features/account/api/account'
import { landingFor } from '@/features/account/landing'
import { queryClient } from '@/lib/query-client'

/**
 * Lets only the given role into an area. Nobody signed in goes to sign-in and comes back to this page; the other
 * role goes to its own area. Any other failure reaches the route's error page. What the tab keeps then belongs to
 * the account let in, before any page reads it.
 */
export function requireRole(role: CurrentAccount['role']) {
  return async ({ request }: LoaderFunctionArgs) => {
    let account: CurrentAccount
    try {
      account = await queryClient.ensureQueryData(currentAccountQuery)
    } catch (error) {
      if (statusOf(error) !== 401) throw error
      const { pathname, search } = new URL(request.url)
      throw redirect(`/sign-in?next=${encodeURIComponent(pathname + search)}`)
    }
    if (account.role !== role) throw redirect(landingFor(account.role))
    claimTabStorage(account)
    void refreshTimeZone(account)
    return null
  }
}

/** A signed-in person has nothing to do on the sign-in page. Any failure just shows the page. */
export async function redirectIfSignedIn() {
  try {
    const account = await queryClient.fetchQuery(currentAccountQuery)
    return redirect(landingFor(account.role))
  } catch {
    return null
  }
}
