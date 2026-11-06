import { afterEach, describe, expect, it, vi } from 'vitest'
import { createApiClient, unwrap } from './client'
import { ApiError } from './problem'

// Same client and CSRF middleware as the app; Node's Request needs an absolute URL.
const api = createApiClient('http://localhost')

type FetchMock = (request: Request) => Promise<Response>

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

afterEach(() => {
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT'
})

describe('api client', () => {
  it('sends X-XSRF-TOKEN (decoded from the cookie) on POST', async () => {
    document.cookie = 'XSRF-TOKEN=abc%3D123'
    const fetch = vi.fn<FetchMock>(async () => json({ id: '1', email: 'a@upr.edu' }))
    await api.POST('/api/auth/login', { body: { email: 'a@upr.edu', password: 'x' }, fetch })
    expect(fetch.mock.calls[0][0].headers.get('X-XSRF-TOKEN')).toBe('abc=123')
  })

  it('does not send the CSRF header on GET', async () => {
    document.cookie = 'XSRF-TOKEN=abc'
    const fetch = vi.fn<FetchMock>(async () => json({ id: '1' }))
    await api.GET('/api/auth/me', { fetch })
    expect(fetch.mock.calls[0][0].headers.has('X-XSRF-TOKEN')).toBe(false)
  })

  it('unwrap throws ApiError with the ProblemDetail on failure', async () => {
    const fetch = vi.fn(async () =>
      json({ title: 'Unauthorized', detail: 'Invalid email or password.' }, 401),
    )
    const error = await unwrap(
      api.POST('/api/auth/login', { body: { email: 'a', password: 'b' }, fetch }),
    ).catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(401)
    expect((error as ApiError).message).toBe('Invalid email or password.')
  })
})
