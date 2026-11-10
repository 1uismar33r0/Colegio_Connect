import axe from 'axe-core'
import { expect, vi } from 'vitest'
import type {
  CompanyDashboard,
  FeedItem,
  OfficeOverview,
  ReviewQueue,
  StudentActivity,
} from '../api/contracts'
import { ApiError } from '../api/problem'
import * as roleData from '../api/roleData'

// MOCK API for the role home pages (#122). The real endpoints are proposed in
// src/api/contracts.ts and not implemented yet. When the backend lands, update these fixtures
// to match its real responses (or capture them from the OpenAPI examples).
//
// Usage in a test file:
//   vi.mock('../../api/roleData')   // path relative to the test file; must be at top level
//   beforeEach(() => mockRoleApi())  // every endpoint succeeds with the fixtures below
//   mockRoleApi({ getReviewQueue: () => apiFailure(500) })  // override one endpoint
//
// All data is synthetic placeholder wording from the core wireframes, not real records.

export const fixtures = {
  feed: [
    {
      id: 'feed-1',
      title: 'Community Health Data Project',
      source: 'University Office',
      postedAt: '2026-11-08T16:00:00Z',
      type: 'Capstone',
      summary:
        'Collaborate with a local team to build a practical data dashboard for community health outreach.',
      tags: ['Data science', 'Team project'],
    },
    {
      id: 'feed-2',
      title: 'Summer Software Internship',
      source: 'Northstar Labs',
      postedAt: '2026-11-05T16:00:00Z',
      type: 'Internship',
      summary:
        'Join a supervised product team for a summer placement focused on accessible web applications.',
      tags: ['Software', 'Summer'],
    },
  ] satisfies FeedItem[],

  activity: { activeResponses: 3, saved: 5 } satisfies StudentActivity,

  officeOverview: {
    stats: { published: 8, newResponses: 12, drafts: 2 },
    recent: [
      {
        id: 'office-1',
        title: 'Community Health Data Project',
        type: 'Capstone',
        newResponses: 6,
        status: 'PUBLISHED',
      },
      {
        id: 'office-2',
        title: 'Student Research Assistant',
        type: 'Research',
        newResponses: 3,
        status: 'PUBLISHED',
      },
      {
        id: 'office-3',
        title: 'Fall Volunteer Orientation',
        type: 'Event',
        newResponses: null,
        status: 'DRAFT',
      },
    ],
  } satisfies OfficeOverview,

  companyDashboard: {
    stats: { activeListings: 4, responses: 17, profileStatus: 'COMPLETE' },
    active: [
      {
        id: 'company-1',
        title: 'Summer Software Internship',
        type: 'Internship',
        closesAt: '2027-06-14T16:00:00Z',
        responses: 9,
      },
      {
        id: 'company-2',
        title: 'Nonprofit Website Redesign',
        type: 'Capstone project',
        closesAt: '2027-05-30T16:00:00Z',
        responses: 5,
      },
      {
        id: 'company-3',
        title: 'Community Tech Workshop',
        type: 'Event',
        closesAt: '2027-05-22T16:00:00Z',
        responses: 1,
      },
    ],
    attention: { newResponses: 4 },
  } satisfies CompanyDashboard,

  reviewQueue: {
    stats: { awaitingReview: 7, reportedListings: 2, pendingVerification: 3 },
    items: [
      {
        id: 'review-1',
        submission: 'Community Garden Volunteer Day',
        submittedBy: 'Green Block Network',
        reason: 'NEW_ORGANIZATION',
        receivedAt: '2026-11-10T16:00:00Z',
      },
      {
        id: 'review-2',
        submission: 'Student Innovation Challenge',
        submittedBy: 'Campus Innovation Office',
        reason: 'REPORTED_LISTING',
        receivedAt: '2026-11-09T16:00:00Z',
      },
      {
        id: 'review-3',
        submission: 'Bright Futures Foundation',
        submittedBy: 'External organization',
        reason: 'PROFILE_VERIFICATION',
        receivedAt: '2026-10-21T16:00:00Z',
      },
    ],
  } satisfies ReviewQueue,
}

type RoleApi = typeof roleData
type Overrides = { [K in keyof RoleApi]?: RoleApi[K] }

/** A rejected call shaped like a backend problem+json error. */
export const apiFailure = (status: number): Promise<never> =>
  Promise.reject(new ApiError(status, { status, title: 'Synthetic test failure' }))

/** A call that never settles, for asserting loading states. */
export const pending = (): Promise<never> => new Promise(() => {})

/** Requires vi.mock('<path>/api/roleData') in the calling test file. */
export function mockRoleApi(overrides: Overrides = {}) {
  const defaults: Required<Overrides> = {
    getStudentFeed: async () => fixtures.feed,
    getStudentActivity: async () => fixtures.activity,
    getOfficeOverview: async () => fixtures.officeOverview,
    getCompanyDashboard: async () => fixtures.companyDashboard,
    getReviewQueue: async () => fixtures.reviewQueue,
  }
  const impl = { ...defaults, ...overrides }
  for (const name of Object.keys(impl) as (keyof RoleApi)[]) {
    const fn = roleData[name]
    if (!vi.isMockFunction(fn)) {
      throw new Error('mockRoleApi needs vi.mock() of src/api/roleData in this test file.')
    }
    vi.mocked(fn).mockReset()
    vi.mocked(fn).mockImplementation(impl[name] as never)
  }
  return vi.mocked(roleData)
}

/** jsdom can't render colors; contrast is verified in a real browser instead. */
export async function expectNoAxeViolations() {
  const results = await axe.run(document.body, {
    rules: { 'color-contrast': { enabled: false } },
  })
  expect(results.violations).toEqual([])
}
