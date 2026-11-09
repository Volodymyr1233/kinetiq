import { useIsRestoring } from '@tanstack/react-query'
import { PersistQueryClientProvider } from '@tanstack/react-query-persist-client'
import { useMemo } from 'react'
import { Outlet } from 'react-router'
import { useCachedAccount } from '@/features/account/api/account'
import { registerLogSetDefaults } from '@/features/sessionlog/api/session-log'
import {
  createPendingLogsPersister,
  discardOtherAccounts,
  pendingLogsDehydrateOptions,
  replayPendingLogs,
} from '@/lib/pending-logs'
import { queryClient } from '@/lib/query-client'

// a restored send needs to know how to run before anything restores, and `lib/` cannot import a feature
registerLogSetDefaults(queryClient)

// nothing renders until the kept logs are back, so a row never shows a set as unlogged and then logs it again under
// a new id
function AfterRestore() {
  return useIsRestoring() ? null : <Outlet />
}

/**
 * Wraps every client route, the logger included, so the logs made on the phone are kept for the signed-in account and
 * sent when the app is open and online, wherever the client opens the app (ADR 0003).
 */
export function PendingLogsRoute() {
  const accountId = useCachedAccount()?.id
  const persister = useMemo(
    () =>
      accountId === undefined
        ? undefined
        : createPendingLogsPersister(accountId),
    [accountId],
  )
  // signing out empties the cache for a moment before the page leaves
  if (accountId === undefined || persister === undefined) return null

  return (
    <PersistQueryClientProvider
      // another account restores its own logs
      key={accountId}
      client={queryClient}
      persistOptions={{
        persister,
        dehydrateOptions: pendingLogsDehydrateOptions,
      }}
      onSuccess={() => {
        discardOtherAccounts(accountId)
        replayPendingLogs(queryClient)
      }}
    >
      <AfterRestore />
    </PersistQueryClientProvider>
  )
}
