// PROPOSED contract for the role home pages (#122). These endpoints do not exist yet;
// the backend owner implements them. When they land in the OpenAPI document:
//   1. regenerate src/api/schema.d.ts (npm run api:fetch && npm run api:gen),
//   2. replace these hand-written types with components['schemas'][...] aliases,
//   3. switch src/api/roleData.ts to the typed `api` client,
//   4. update the fixtures in src/test/mockApi.ts to match the real responses.

export const ROLE_ENDPOINTS = {
  studentFeed: '/api/listings/feed',
  studentActivity: '/api/me/activity',
  officeOverview: '/api/institution/overview',
  companyDashboard: '/api/company/dashboard',
  reviewQueue: '/api/admin/review-queue',
} as const

/** ISO-8601 instant, e.g. "2026-11-08T12:00:00Z". */
export type IsoDateTime = string

// ---- Student: Discover ----

export type StudentFeedFilters = {
  keyword?: string
  type?: string
  field?: string
}

export type FeedItem = {
  id: string
  title: string
  /** Publishing office or organization name, shown so students can judge the source. */
  source: string
  postedAt: IsoDateTime
  /** Opportunity category label, e.g. "Internship". */
  type: string
  summary: string
  tags: string[]
}

export type StudentActivity = {
  activeResponses: number
  saved: number
}

// ---- University office: Overview ----

export type ListingStatus = 'PUBLISHED' | 'DRAFT' | 'CLOSED'

export type OfficeListing = {
  id: string
  title: string
  type: string
  /** null for drafts, which cannot receive responses. */
  newResponses: number | null
  status: ListingStatus
}

export type OfficeOverview = {
  stats: { published: number; newResponses: number; drafts: number }
  recent: OfficeListing[]
}

// ---- External organization: Dashboard ----

export type ProfileStatus = 'COMPLETE' | 'INCOMPLETE' | 'PENDING_APPROVAL'

export type CompanyListing = {
  id: string
  title: string
  type: string
  closesAt: IsoDateTime
  responses: number
}

export type CompanyDashboard = {
  stats: { activeListings: number; responses: number; profileStatus: ProfileStatus }
  active: CompanyListing[]
  attention: { newResponses: number }
}

// ---- Administrator / moderator: Review queue ----

export type ReviewReason = 'NEW_ORGANIZATION' | 'REPORTED_LISTING' | 'PROFILE_VERIFICATION'

export type ReviewItem = {
  id: string
  submission: string
  submittedBy: string
  reason: ReviewReason
  receivedAt: IsoDateTime
}

export type ReviewQueue = {
  stats: { awaitingReview: number; reportedListings: number; pendingVerification: number }
  items: ReviewItem[]
}
