import { QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider } from 'react-router'
import { AccountSettingsSync } from '@/app/account-settings-sync'
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
        <WeightUnitProvider>
          <RouterProvider router={router} />
        </WeightUnitProvider>
        {/* design system: bottom-right on desktop; client screens move it to bottom-centre when they first need it */}
        <Toaster position="bottom-right" />
      </QueryClientProvider>
    </ThemeProvider>
  )
}
