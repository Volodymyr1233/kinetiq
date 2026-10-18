import { createBrowserRouter, type RouteObject } from 'react-router'
import { HomePage } from '@/app/home-page'
import { NotFoundPage } from '@/app/not-found-page'

// area chunks are tiny, so a blank frame while one loads beats a flashing spinner
const BlankWhileLoading = () => null

// each area is its own chunk, downloaded on the first visit
export const routes: RouteObject[] = [
  { path: '/', Component: HomePage },
  {
    path: '/trainer',
    HydrateFallback: BlankWhileLoading,
    lazy: {
      Component: async () => (await import('@/app/trainer-area')).TrainerArea,
    },
  },
  {
    path: '/app',
    HydrateFallback: BlankWhileLoading,
    lazy: {
      Component: async () => (await import('@/app/client-area')).ClientArea,
    },
  },
  { path: '*', Component: NotFoundPage },
]

export const router = createBrowserRouter(routes)
