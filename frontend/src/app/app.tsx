import { QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider } from 'react-router'
import { AccountSettingsSync } from '@/app/account-settings-sync'
import { router } from '@/app/router'
import { WeightUnitProvider } from '@/app/weight-unit-provider'
import { ThemeProvider } from '@/components/theme-provider'
import { queryClient } from '@/lib/query-client'

export function App() {
  return (
    <ThemeProvider>
      <QueryClientProvider client={queryClient}>
        <AccountSettingsSync />
        <WeightUnitProvider>
          <RouterProvider router={router} />
        </WeightUnitProvider>
      </QueryClientProvider>
    </ThemeProvider>
  )
}
