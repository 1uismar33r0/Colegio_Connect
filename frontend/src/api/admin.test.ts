import { afterEach, describe, expect, it, vi } from 'vitest'

type FetchMock = (request: Request) => Promise<Response>

// openapi-fetch keeps the global fetch it sees when the client is created, so install a
// delegating stub before the client module loads; each test then sets fetchMock's behaviour.
const fetchMock = vi.fn<FetchMock>()
vi.stubGlobal('fetch', (request: Request) => fetchMock(request))

// Run the real admin module against the real typed client, with an absolute base URL because
// Node's Request needs one. Only fetch is stubbed, so paths, methods and CSRF are the app's.
vi.mock('./client', async (importOriginal) => {
  const original = await importOriginal<typeof import('./client')>()
  return { ...original, api: original.createApiClient('http://localhost') }
})

const { assignRole, getAuditEvents, getPendingCompanies, getRoleAssignments, revokeRole } =
  await import('./admin')

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

function stubFetch(response: () => Response) {
  fetchMock.mockImplementation(async () => response())
  return () => fetchMock.mock.calls[0][0]
}

afterEach(() => {
  fetchMock.mockReset()
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT'
})

describe('admin API (real endpoints)', () => {
  it('pages the pending-company queue', async () => {
    const request = stubFetch(() => json({ items: [], page: 2 }))
    await expect(getPendingCompanies(2)).resolves.toMatchObject({ page: 2 })
    expect(request().method).toBe('GET')
    expect(request().url).toBe('http://localhost/api/admin/organizations/pending?page=2&size=20')
  })

  it('pages the audit history', async () => {
    const request = stubFetch(() => json({ items: [] }))
    await getAuditEvents()
    expect(request().url).toBe('http://localhost/api/admin/audit-events?page=0&size=20')
  })

  it("lists a user's role assignments", async () => {
    const request = stubFetch(() => json([]))
    await getRoleAssignments('0190aaaa-0000-7000-8000-000000000002')
    expect(request().url).toBe(
      'http://localhost/api/admin/role-assignments?userId=0190aaaa-0000-7000-8000-000000000002',
    )
  })

  it('assigns with a JSON body and the CSRF header', async () => {
    document.cookie = 'XSRF-TOKEN=tok%3D1'
    const request = stubFetch(() => json({ id: 'new' }, 201))
    const body = { userId: 'u', role: 'MODERATOR' as const }
    await expect(assignRole(body)).resolves.toEqual({ id: 'new' })
    expect(request().method).toBe('POST')
    expect(request().url).toBe('http://localhost/api/admin/role-assignments')
    expect(request().headers.get('X-XSRF-TOKEN')).toBe('tok=1')
    expect(await request().json()).toEqual(body)
  })

  it('revokes by assignment id and accepts an empty 204', async () => {
    document.cookie = 'XSRF-TOKEN=tok'
    const request = stubFetch(() => new Response(null, { status: 204 }))
    await revokeRole('abc')
    expect(request().method).toBe('DELETE')
    expect(request().url).toBe('http://localhost/api/admin/role-assignments/abc')
    expect(request().headers.get('X-XSRF-TOKEN')).toBe('tok')
  })

  it('throws ApiError with the backend detail on refusal', async () => {
    stubFetch(() => json({ status: 409, detail: 'The user already has this role.' }, 409))
    await expect(assignRole({ userId: 'u', role: 'MODERATOR' })).rejects.toMatchObject({
      status: 409,
      message: 'The user already has this role.',
    })
  })
})
