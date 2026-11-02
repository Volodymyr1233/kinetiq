import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { StrictMode, type ReactNode } from 'react'
import { createMemoryRouter, MemoryRouter, RouterProvider } from 'react-router'
import { vi } from 'vitest'
import { createRoutes } from '@/app/router'
import { WeightUnitProvider } from '@/app/weight-unit-provider'
import { queryClient } from '@/lib/query-client'

export type Reply = (request: Request) => Promise<Response> | Response

/** Stubs `fetch` and records every request the page sends. Sets a CSRF cookie so no token request interferes. */
export function stubApi(reply: Reply) {
  document.cookie = 'XSRF-TOKEN=token'
  const requests: Request[] = []
  vi.stubGlobal(
    'fetch',
    vi.fn(async (request: Request) => {
      requests.push(request.clone())
      return reply(request)
    }),
  )
  return requests
}

export function problem(status: number, body: object) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  })
}

export function renderPage(page: ReactNode) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  return render(
    <StrictMode>
      <QueryClientProvider client={client}>
        <MemoryRouter>{page}</MemoryRouter>
      </QueryClientProvider>
    </StrictMode>,
  )
}

export function pathsOf(requests: Request[]) {
  return requests.map((request) => new URL(request.url).pathname)
}

export const SIGNED_IN = {
  TRAINER: {
    id: '00000000-0000-4000-8000-000000000001',
    name: 'Marek Nowak',
    email: 'marek@example.com',
    role: 'TRAINER',
    timeZone: 'Europe/Warsaw',
    units: 'KG',
    language: 'EN',
    theme: 'SYSTEM',
    emailConfirmed: true,
  },
  CLIENT: {
    id: '00000000-0000-4000-8000-000000000003',
    name: 'Anna Kowalska',
    email: 'anna@example.com',
    role: 'CLIENT',
    timeZone: 'Europe/Warsaw',
    units: 'KG',
    language: 'EN',
    theme: 'SYSTEM',
    emailConfirmed: true,
  },
} as const

export function json(body: object, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

/** Answers the quiz status read with the given status and every other call with `reply`. */
export function quizStatus(
  status: string,
  reply: Reply = () => new Response(null, { status: 204 }),
): Reply {
  return (request) =>
    request.method === 'GET' && new URL(request.url).pathname === '/api/v1/quiz'
      ? json({ status })
      : reply(request)
}

/**
 * Renders the whole app at a path with the app's own query client, as the guards use it. The current account is
 * the given one, or nobody when `null`, until a sign-in or sign-out through `reply` changes it.
 */
export function renderAppAt(
  path: string,
  account: object | null,
  reply: Reply = () => new Response(null, { status: 204 }),
) {
  queryClient.clear()
  let current = account
  const requests = stubApi(async (request) => {
    const call = `${request.method} ${new URL(request.url).pathname}`
    if (call === 'GET /api/v1/accounts/me') {
      return current
        ? json(current)
        : json({ status: 401, code: 'request.unauthenticated' }, 401)
    }
    const response = await reply(request)
    // the server ends or starts the sign-in and keeps changed settings, so later checks answer accordingly
    if (call === 'DELETE /api/v1/sign-ins/current' && response.ok)
      current = null
    if (
      (call === 'POST /api/v1/sign-ins' ||
        call === 'PATCH /api/v1/accounts/me') &&
      response.ok
    ) {
      current = await response.clone().json()
    }
    return response
  })
  const router = createMemoryRouter(createRoutes(), { initialEntries: [path] })
  // the weight unit follows the account, as in app.tsx
  render(
    <QueryClientProvider client={queryClient}>
      <WeightUnitProvider>
        <RouterProvider router={router} />
      </WeightUnitProvider>
    </QueryClientProvider>,
  )
  return { requests, router }
}
