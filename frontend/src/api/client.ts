import createClient, { type Middleware } from 'openapi-fetch'
import type { components, paths } from './schema'
import { toApiError } from './problem'

export type UserResponse = components['schemas']['UserResponse']

const SAFE_METHODS = ['GET', 'HEAD', 'OPTIONS']

function readCsrfToken(): string | undefined {
  const raw = document.cookie
    .split('; ')
    .find((c) => c.startsWith('XSRF-TOKEN='))
    ?.split('=')[1]
  return raw ? decodeURIComponent(raw) : undefined
}

// Spring's csrf.spa() expects the XSRF-TOKEN cookie echoed back in this header.
const csrfMiddleware: Middleware = {
  onRequest({ request }) {
    if (!SAFE_METHODS.includes(request.method)) {
      const token = readCsrfToken()
      if (token) request.headers.set('X-XSRF-TOKEN', token)
    }
    return request
  },
}

export const api = createClient<paths>({ baseUrl: '', credentials: 'include' })
api.use(csrfMiddleware)

/** Returns data on success, throws ApiError on any non-2xx response. */
export async function unwrap<T>(
  call: Promise<{ data?: T; error?: unknown; response: Response }>,
): Promise<T> {
  const { data, error, response } = await call
  if (!response.ok) throw toApiError(response, error)
  return data as T
}

/** Logout is a Spring Security filter, so it is not in the OpenAPI spec. */
export async function logout(): Promise<void> {
  const token = readCsrfToken()
  const response = await fetch('/api/auth/logout', {
    method: 'POST',
    credentials: 'include',
    headers: token ? { 'X-XSRF-TOKEN': token } : {},
  })
  if (!response.ok) throw toApiError(response, null)
}
