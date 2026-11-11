import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from './problem'

type FetchMock = (request: Request) => Promise<Response>

// openapi-fetch keeps the global fetch it sees when the client is created, so install a
// delegating stub before the client module loads (same approach as admin.test.ts).
const fetchMock = vi.fn<FetchMock>()
vi.stubGlobal('fetch', (request: Request) => fetchMock(request))

vi.mock('./client', async (importOriginal) => {
  const original = await importOriginal<typeof import('./client')>()
  return { ...original, api: original.createApiClient('http://localhost') }
})

const { getCompanyDashboard, getOfficeOverview, getStudentActivity, getStudentFeed } =
  await import('./roleData')

const ORG = '0190bbbb-0000-7000-8000-000000000001'

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

function stubFetch(response: () => Response) {
  fetchMock.mockImplementation(async () => response())
  return () => fetchMock.mock.calls[0][0]
}

afterEach(() => {
  fetchMock.mockReset()
})

describe('role dashboard API (real endpoints)', () => {
  it('sends only the approved filters that are set, plus paging', async () => {
    const request = stubFetch(() => json([]))
    await getStudentFeed(
      {
        keyword: 'data',
        category: 'INTERNSHIP',
        sourceLabel: 'APPROVED_EXTERNAL_COMPANY',
        deadlineOnOrBefore: '2026-11-30',
        organizationId: '',
      },
      2,
    )
    const url = new URL(request().url)
    expect(url.pathname).toBe('/api/listings/feed')
    expect(Object.fromEntries(url.searchParams)).toEqual({
      keyword: 'data',
      category: 'INTERNSHIP',
      sourceLabel: 'APPROVED_EXTERNAL_COMPANY',
      deadlineOnOrBefore: '2026-11-30',
      page: '2',
      size: '20',
    })
    // Never the removed field filter or the deprecated type alias.
    expect(url.searchParams.has('field')).toBe(false)
    expect(url.searchParams.has('type')).toBe(false)
  })

  it('reads the first feed page with no filters', async () => {
    const request = stubFetch(() => json([]))
    await getStudentFeed()
    expect(request().url).toBe('http://localhost/api/listings/feed?page=0&size=20')
  })

  it('reads the student activity counts', async () => {
    const request = stubFetch(() => json({ activeResponses: 1, saved: 2 }))
    await expect(getStudentActivity()).resolves.toEqual({ activeResponses: 1, saved: 2 })
    expect(request().url).toBe('http://localhost/api/me/activity')
  })

  it('lets the backend choose the organization unless one is selected', async () => {
    let request = stubFetch(() => json({}))
    await getOfficeOverview()
    expect(request().url).toBe('http://localhost/api/institution/overview')

    fetchMock.mockReset()
    request = stubFetch(() => json({}))
    await getCompanyDashboard(ORG)
    expect(request().url).toBe(`http://localhost/api/company/dashboard?organizationId=${ORG}`)
  })

  it('throws ApiError with the backend detail when an organization must be chosen', async () => {
    stubFetch(() =>
      json({ status: 409, detail: 'Select an organizationId for this dashboard.' }, 409),
    )
    const error = await getOfficeOverview().catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 409 })
  })

  it('throws ApiError when the error body is not JSON', async () => {
    stubFetch(() => new Response('gateway down', { status: 502 }))
    await expect(getCompanyDashboard()).rejects.toMatchObject({ status: 502 })
  })
})
