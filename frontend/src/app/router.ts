import { createBrowserRouter, type RouteObject } from 'react-router'
import { HomePage } from '@/app/home-page'
import { NotFoundPage } from '@/app/not-found-page'

// area chunks are tiny, so a blank frame while one loads beats a flashing spinner
const BlankWhileLoading = () => null

// each area and the account pages are their own chunks, downloaded on the first visit
export const routes: RouteObject[] = [
  { path: '/', Component: HomePage },
  {
    path: '/sign-up',
    HydrateFallback: BlankWhileLoading,
    lazy: {
      Component: async () =>
        (await import('@/features/account/pages/sign-up-page')).SignUpPage,
    },
  },
  {
    path: '/confirm-email',
    HydrateFallback: BlankWhileLoading,
    lazy: {
      Component: async () =>
        (await import('@/features/account/pages/confirm-email-page'))
          .ConfirmEmailPage,
    },
  },
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
