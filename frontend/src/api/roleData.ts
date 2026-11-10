import {
  ROLE_ENDPOINTS,
  type CompanyDashboard,
  type FeedItem,
  type OfficeOverview,
  type ReviewQueue,
  type StudentActivity,
  type StudentFeedFilters,
} from './contracts'
import { toApiError } from './problem'

// Data access for the role home pages. Tests replace this whole module with the mock API in
// src/test/mockApi.ts. Plain fetch is used because these paths are not in schema.d.ts yet;
// see contracts.ts for the steps to move to the typed client.

async function getJson<T>(path: string, query?: Record<string, string | undefined>): Promise<T> {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value) params.set(key, value)
  }
  const url = params.size ? `${path}?${params}` : path
  const response = await fetch(url, {
    credentials: 'include',
    headers: { Accept: 'application/json' },
  })
  const body: unknown = await response.json().catch(() => null)
  if (!response.ok) throw toApiError(response, body)
  return body as T
}

export const getStudentFeed = (filters: StudentFeedFilters = {}) =>
  getJson<FeedItem[]>(ROLE_ENDPOINTS.studentFeed, filters)

export const getStudentActivity = () => getJson<StudentActivity>(ROLE_ENDPOINTS.studentActivity)

export const getOfficeOverview = () => getJson<OfficeOverview>(ROLE_ENDPOINTS.officeOverview)

export const getCompanyDashboard = () => getJson<CompanyDashboard>(ROLE_ENDPOINTS.companyDashboard)

export const getReviewQueue = () => getJson<ReviewQueue>(ROLE_ENDPOINTS.reviewQueue)
