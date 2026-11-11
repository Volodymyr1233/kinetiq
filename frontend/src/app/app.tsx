import { QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider } from 'react-router'
import { AccountSettingsSync } from '@/app/account-settings-sync'
import { ConversationEventsSync } from '@/app/conversation-events-sync'
import { router } from '@/app/router'
import { WeightUnitProvider } from '@/app/weight-unit-provider'
import { ThemeProvider } from '@/components/theme-provider'
import { Toaster } from '@/components/ui/sonner'
import { queryClient } from '@/lib/query-client'

export function App() {
  return (
    <ThemeProvider>
      <QueryClientProvider client={queryClient}>
        <AccountSettingsSync />
        <ConversationEventsSync />
        <WeightUnitProvider>
          <RouterProvider router={router} />
        </WeightUnitProvider>
        {/* design system: bottom-right on desktop; client screens move it to bottom-centre when they first need it */}
        {/* on a phone the client's tab bar takes the bottom 3.5 rem, so messages sit above it */}
        <Toaster
          position="bottom-right"
          mobileOffset={{
            bottom: 'calc(3.5rem + 1px + env(safe-area-inset-bottom) + 16px)',
          }}
        />
      </QueryClientProvider>
    </ThemeProvider>
  )
}
