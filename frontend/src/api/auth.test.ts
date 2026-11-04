import { afterEach, describe, expect, it, vi } from 'vitest'
import { initializeCsrf, login, logout, register } from './auth'

describe('authentication API client', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    document.cookie = 'XSRF-TOKEN=; Max-Age=0; Path=/'
  })

  it('initializes the CSRF cookie before sending a login request with its header', async () => {
    document.cookie = 'XSRF-TOKEN=csrf-value; Path=/'
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(Response.json({
        id: 'b9b2fbf6-a523-49dc-a2b0-4858bfe82275',
        email: 'student@upr.edu',
        accountType: 'UPRM_MEMBER',
      }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)

    await initializeCsrf()
    await login({ email: 'student@upr.edu', password: 'password123' })

    expect(fetchMock.mock.calls[0]?.[0]).toBe('/api/auth/csrf')
    expect(fetchMock.mock.calls[0]?.[1]).toMatchObject({ credentials: 'include' })
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/api/auth/login', {
      method: 'POST',
      credentials: 'include',
      headers: expect.any(Headers),
      body: JSON.stringify({ email: 'student@upr.edu', password: 'password123' }),
    })
    const loginOptions = fetchMock.mock.calls[1]?.[1]
    expect(loginOptions).toBeDefined()
    expect(((loginOptions as RequestInit).headers as Headers).get('X-XSRF-TOKEN')).toBe('csrf-value')
    expect(fetchMock.mock.calls[2]?.[0]).toBe('/api/auth/csrf')
    expect(fetchMock.mock.calls[2]?.[1]).toMatchObject({ credentials: 'include' })
  })

  it('sends the CSRF header when logout has no request body', async () => {
    document.cookie = 'XSRF-TOKEN=logout-token; Path=/'
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)

    await logout()

    const logoutOptions = fetchMock.mock.calls[0]?.[1]
    expect(logoutOptions).toMatchObject({ method: 'POST', credentials: 'include' })
    expect(logoutOptions).toBeDefined()
    expect(((logoutOptions as RequestInit).headers as Headers).get('X-XSRF-TOKEN')).toBe('logout-token')
  })

  it('accepts an empty 201 response from registration', async () => {
    document.cookie = 'XSRF-TOKEN=registration-token; Path=/'
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 201 })))

    await expect(register({ email: 'student@upr.edu', password: 'password123' })).resolves.toBeUndefined()
  })
})
