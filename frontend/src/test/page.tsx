import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { StrictMode, type ReactNode } from 'react'
import { MemoryRouter } from 'react-router'
import { vi } from 'vitest'

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
