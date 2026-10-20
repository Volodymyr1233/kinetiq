import { createBrowserRouter, redirect, type RouteObject } from 'react-router'
import { HomePage } from '@/app/home-page'
import { NotFoundPage } from '@/app/not-found-page'

// area chunks are tiny, so a blank frame while one loads beats a flashing spinner
const BlankWhileLoading = () => null

const trainerPlaceholder = (path: string): RouteObject => ({
  path,
  lazy: {
    Component: async () =>
      (await import('@/app/trainer-area')).TrainerPlaceholder,
  },
})

const clientPlaceholder = (path: string): RouteObject => ({
  path,
  lazy: {
    Component: async () =>
      (await import('@/app/client-area')).ClientPlaceholder,
  },
})

// each area and the account pages are their own chunks, downloaded on the first
// visit; a router consumes the lazy objects it loads, so every router needs fresh routes
export const createRoutes = (): RouteObject[] => [
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
    children: [
      { index: true, loader: () => redirect('/trainer/clients') },
      trainerPlaceholder('clients'),
      trainerPlaceholder('approvals'),
      trainerPlaceholder('messages'),
      trainerPlaceholder('exercises'),
      trainerPlaceholder('settings'),
    ],
  },
  {
    path: '/app',
    HydrateFallback: BlankWhileLoading,
    lazy: {
      Component: async () => (await import('@/app/client-area')).ClientArea,
    },
    children: [
      { index: true, loader: () => redirect('/app/today') },
      clientPlaceholder('today'),
      clientPlaceholder('plan'),
      clientPlaceholder('progress'),
      clientPlaceholder('messages'),
    ],
  },
  { path: '*', Component: NotFoundPage },
]

export const router = createBrowserRouter(createRoutes())
