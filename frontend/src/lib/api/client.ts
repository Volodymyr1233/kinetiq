import createClient from 'openapi-fetch'
import { redirectToSignIn } from '@/lib/api/sign-in-redirect'
import i18n from '@/lib/i18n'
import type { paths } from './schema'

const STATE_CHANGING = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])
const CSRF_COOKIE = 'XSRF-TOKEN='
// their callers handle a 401 themselves: a wrong password, and the guards finding nobody signed in
const OWN_401 = ['POST /api/v1/sign-ins', 'GET /api/v1/accounts/me']

// Typed against the backend's OpenAPI document. Regenerate schema.d.ts with `npm run gen:api`.
export const api = createClient<paths>({
  baseUrl: window.location.origin,
  // looked up per call, so tests can stub fetch after this module loads
  fetch: (request) => fetch(request),
})

function csrfToken() {
  return document.cookie
    .split('; ')
    .find((cookie) => cookie.startsWith(CSRF_COOKIE))
    ?.slice(CSRF_COOKIE.length)
}

api.use({
  async onRequest({ request }) {
    // the backend writes emails and errors in the language the UI is shown in
    request.headers.set('Accept-Language', i18n.language)
    if (STATE_CHANGING.has(request.method)) {
      // a fresh page has made no backend call yet, so it has no cookie
      if (!csrfToken()) await api.GET('/api/v1/csrf')
      const token = csrfToken()
      if (token) request.headers.set('X-XSRF-TOKEN', decodeURIComponent(token))
    }
    return request
  },
  onResponse({ request, response }) {
    const call = `${request.method} ${new URL(request.url).pathname}`
    if (response.status === 401 && !OWN_401.includes(call)) redirectToSignIn()
  },
})
