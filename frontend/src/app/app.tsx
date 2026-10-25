import { QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider } from 'react-router'
import { router } from '@/app/router'
import { ThemeProvider } from '@/components/theme-provider'
import { queryClient } from '@/lib/query-client'

export function App() {
  return (
    <ThemeProvider>
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </ThemeProvider>
  )
}
