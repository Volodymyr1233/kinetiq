import { QueryClient } from '@tanstack/react-query'
import { isWorthRetrying } from '@/lib/api/problem'

// one client for the app and the route loaders, so a guard and a page share what was loaded
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failures, error) => failures < 3 && isWorthRetrying(error),
    },
  },
})
