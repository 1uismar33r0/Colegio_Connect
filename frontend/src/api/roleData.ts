import { api, unwrap } from './client'
import { FEED_PAGE_SIZE } from './listingOptions'
import type { components, operations } from './schema'

// Role dashboard read APIs (#122, PR #127). Types come from the generated OpenAPI schema; tests
// replace this module with the mock API in src/test/mockApi.ts.

type Schemas = components['schemas']
export type FeedItem = Schemas['FeedItem']
export type StudentActivity = Schemas['StudentActivityResponse']
export type OfficeOverview = Schemas['OfficeOverviewResponse']
export type ListingStatus = Schemas['OfficeRecentItem']['status']
export type CompanyDashboard = Schemas['CompanyDashboardResponse']
export type ApprovalStatus = Schemas['CompanyStats']['approvalStatus']

type FeedQuery = NonNullable<operations['find']['parameters']['query']>
/** The approved feed filters. The deprecated `type` alias is intentionally not sent. */
export type StudentFeedFilters = Pick<
  FeedQuery,
  'keyword' | 'category' | 'sourceLabel' | 'organizationId' | 'deadlineOnOrBefore'
>

/** Drops unset and empty filters so the backend applies only the ones the user chose. */
function setFilters(filters: StudentFeedFilters): StudentFeedFilters {
  return Object.fromEntries(Object.entries(filters).filter(([, value]) => value))
}

export const getStudentFeed = (filters: StudentFeedFilters = {}, page = 0) =>
  unwrap(
    api.GET('/api/listings/feed', {
      params: { query: { ...setFilters(filters), page, size: FEED_PAGE_SIZE } },
    }),
  )

export const getStudentActivity = () => unwrap(api.GET('/api/me/activity'))

/**
 * With no organizationId the backend picks the user's only eligible office; with several it
 * answers 409 until one is chosen (see components/OrganizationScope.tsx).
 */
export const getOfficeOverview = (organizationId?: string) =>
  unwrap(api.GET('/api/institution/overview', { params: { query: { organizationId } } }))

/** Same organization selection rules as getOfficeOverview. */
export const getCompanyDashboard = (organizationId?: string) =>
  unwrap(api.GET('/api/company/dashboard', { params: { query: { organizationId } } }))
