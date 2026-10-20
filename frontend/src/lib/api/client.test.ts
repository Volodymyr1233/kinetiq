import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import i18n from '@/lib/i18n'
import { api } from './client'

function clearCsrfCookie() {
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT'
}

describe('api', () => {
  const requests: Request[] = []

  beforeEach(() => {
    requests.length = 0
    clearCsrfCookie()
    vi.stubGlobal(
      'fetch',
      vi.fn(async (request: Request) => {
        requests.push(request)
        if (request.url.endsWith('/api/v1/csrf')) {
          document.cookie = 'XSRF-TOKEN=token-1'
          return new Response(null, { status: 204 })
        }
        return new Response(null, { status: 202 })
      }),
    )
  })

  afterEach(async () => {
    clearCsrfCookie()
    await i18n.changeLanguage('en')
  })

  it('fetches a CSRF token first when the page has none, then sends it', async () => {
    await api.POST('/api/v1/confirmation-links', {
      body: { email: 'marek@example.com' },
    })

    expect(requests.map((request) => new URL(request.url).pathname)).toEqual([
      '/api/v1/csrf',
      '/api/v1/confirmation-links',
    ])
    expect(requests[1].headers.get('X-XSRF-TOKEN')).toBe('token-1')
  })

  it('reuses the CSRF cookie when the page already has one', async () => {
    document.cookie = 'XSRF-TOKEN=token-2'

    await api.POST('/api/v1/confirmation-links', {
      body: { email: 'marek@example.com' },
    })

    expect(requests).toHaveLength(1)
    expect(requests[0].headers.get('X-XSRF-TOKEN')).toBe('token-2')
  })

  it('sends the UI language', async () => {
    await i18n.changeLanguage('pl')

    await api.GET('/api/v1/csrf')

    expect(requests[0].headers.get('Accept-Language')).toBe('pl')
    expect(requests[0].headers.has('X-XSRF-TOKEN')).toBe(false)
  })
})
