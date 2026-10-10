import createClient from 'openapi-fetch'
import type { paths } from './schema'

// Typed against the backend's OpenAPI document. Regenerate schema.d.ts with `npm run gen:api`.
export const api = createClient<paths>()
