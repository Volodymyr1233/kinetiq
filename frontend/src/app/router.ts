import { createBrowserRouter, redirect, type RouteObject } from 'react-router'
import { AreaErrorPage } from '@/app/area-error-page'
import { redirectIfSignedIn, requireRole } from '@/app/guards'
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
    path: '/sign-in',
    loader: redirectIfSignedIn,
    HydrateFallback: BlankWhileLoading,
    lazy: {
      Component: async () =>
        (await import('@/features/account/pages/sign-in-page')).SignInPage,
    },
  },
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
    path: '/join',
    HydrateFallback: BlankWhileLoading,
    lazy: {
      Component: async () =>
        (await import('@/features/account/pages/join-page')).JoinPage,
    },
  },
  {
    path: '/reset-password',
    HydrateFallback: BlankWhileLoading,
    lazy: {
      Component: async () =>
        (await import('@/features/account/pages/reset-password-page'))
          .ResetPasswordPage,
    },
  },
  {
    path: '/trainer',
    loader: requireRole('TRAINER'),
    ErrorBoundary: AreaErrorPage,
    HydrateFallback: BlankWhileLoading,
    lazy: {
      Component: async () => (await import('@/app/trainer-area')).TrainerArea,
    },
    children: [
      { index: true, loader: () => redirect('/trainer/clients') },
      {
        path: 'clients',
        children: [
          {
            index: true,
            HydrateFallback: BlankWhileLoading,
            lazy: {
              Component: async () =>
                (await import('@/features/client/pages/clients-page'))
                  .ClientsPage,
            },
          },
          {
            path: ':id',
            HydrateFallback: BlankWhileLoading,
            lazy: {
              Component: async () =>
                (await import('@/features/client/pages/client-page'))
                  .ClientPage,
            },
            // the tabs are empty until their features land
            children: [undefined, 'plan', 'logs', 'messages'].map(
              (path): RouteObject => ({
                path,
                index: path === undefined,
                lazy: {
                  Component: async () =>
                    (await import('@/features/client/pages/client-page'))
                      .ClientTabEmpty,
                },
              }),
            ),
          },
          {
            path: 'invites',
            HydrateFallback: BlankWhileLoading,
            lazy: {
              Component: async () =>
                (await import('@/features/account/pages/invites-page'))
                  .InvitesPage,
            },
          },
        ],
      },
      trainerPlaceholder('approvals'),
      trainerPlaceholder('messages'),
      {
        path: 'exercises',
        HydrateFallback: BlankWhileLoading,
        lazy: {
          Component: async () =>
            (await import('@/features/exercise/pages/exercise-library-page'))
              .ExerciseLibraryPage,
        },
      },
      {
        path: 'settings',
        HydrateFallback: BlankWhileLoading,
        lazy: {
          Component: async () =>
            (await import('@/features/account/pages/settings-page'))
              .SettingsPage,
        },
      },
    ],
  },
  // the quiz has its own address per step and no tab bar, so it sits beside the client area
  {
    path: '/app/quiz/review',
    loader: requireRole('CLIENT'),
    ErrorBoundary: AreaErrorPage,
    HydrateFallback: BlankWhileLoading,
    lazy: {
      Component: async () => (await import('@/app/quiz-area')).QuizReviewRoute,
    },
  },
  {
    path: '/app/quiz/waiting',
    loader: requireRole('CLIENT'),
    ErrorBoundary: AreaErrorPage,
    HydrateFallback: BlankWhileLoading,
    lazy: {
      Component: async () => (await import('@/app/quiz-area')).QuizWaitingRoute,
    },
  },
  {
    path: '/app/quiz/:step?',
    loader: requireRole('CLIENT'),
    ErrorBoundary: AreaErrorPage,
    HydrateFallback: BlankWhileLoading,
    lazy: {
      Component: async () => (await import('@/app/quiz-area')).QuizStepsRoute,
    },
  },
  {
    path: '/app',
    loader: requireRole('CLIENT'),
    ErrorBoundary: AreaErrorPage,
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
