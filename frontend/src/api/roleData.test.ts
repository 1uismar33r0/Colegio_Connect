import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from './problem'
import { getReviewQueue, getStudentFeed } from './roleData'

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('roleData (proposed endpoints)', () => {
  it('sends only the filters that are set, with the session cookie', async () => {
    const fetchMock = vi.fn<(url: string, init?: RequestInit) => Promise<Response>>(async () =>
      json([]),
    )
    vi.stubGlobal('fetch', fetchMock)

    await getStudentFeed({ keyword: 'data', type: undefined, field: '' })

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toBe('/api/listings/feed?keyword=data')
    expect(init.credentials).toBe('include')
  })

  it('calls the bare path when there are no filters', async () => {
    const fetchMock = vi.fn<(url: string, init?: RequestInit) => Promise<Response>>(async () =>
      json([]),
    )
    vi.stubGlobal('fetch', fetchMock)
    await getStudentFeed()
    expect(fetchMock.mock.calls[0][0]).toBe('/api/listings/feed')
  })

  it('throws ApiError with the problem body on a non-2xx response', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => json({ status: 403, title: 'Forbidden' }, 403)),
    )
    const error = await getReviewQueue().catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 403, problem: { title: 'Forbidden' } })
  })

  it('throws ApiError when the error body is not JSON', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response('gateway down', { status: 502 })),
    )
    await expect(getReviewQueue()).rejects.toMatchObject({ status: 502 })
  })
})
