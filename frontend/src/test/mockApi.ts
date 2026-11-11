import axe from 'axe-core'
import { expect, vi } from 'vitest'
import * as adminApi from '../api/admin'
import type { AuditPage, PendingCompanyPage, RoleAssignment } from '../api/admin'
import { ApiError } from '../api/problem'
import * as roleData from '../api/roleData'
import type { CompanyDashboard, FeedItem, OfficeOverview, StudentActivity } from '../api/roleData'

// MOCK API for component tests of the role pages (#122). Runtime pages always call the real
// backend; these fixtures only stand in for it inside Vitest. Shapes come from the generated
// OpenAPI schema (src/api/schema.d.ts), so a backend contract change fails typecheck here.
//
// Usage in a test file (paths relative to the test file; vi.mock must be at top level):
//   vi.mock('../../api/roleData')    or    vi.mock('../../api/admin')
//   beforeEach(() => mockRoleApi())        beforeEach(() => mockAdminApi())
//   mockRoleApi({ getOfficeOverview: () => apiFailure(500) })   // override one endpoint
//
// All data is synthetic placeholder wording from the core wireframes, not real records. Labels
// (type, sourceLabel) are the backend's display labels.

export const fixtures = {
  feed: [
    {
      id: '0190eeee-0000-7000-8000-000000000001',
      title: 'Community Health Data Project',
      source: 'University Office',
      sourceLabel: 'Verified UPRM institution',
      postedAt: '2026-11-08T16:00:00Z',
      type: 'Capstone sponsorship',
      summary:
        'Collaborate with a local team to build a practical data dashboard for community health outreach.',
      tags: ['Data science', 'Team project'],
    },
    {
      id: '0190eeee-0000-7000-8000-000000000002',
      title: 'Summer Software Internship',
      source: 'Northstar Labs',
      sourceLabel: 'Approved external company',
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
        id: '0190eeee-0000-7000-8000-000000000011',
        title: 'Community Health Data Project',
        type: 'Capstone sponsorship',
        newResponses: 6,
        status: 'PUBLISHED',
      },
      {
        id: '0190eeee-0000-7000-8000-000000000012',
        title: 'Student Research Assistant',
        type: 'Research position',
        newResponses: 3,
        status: 'PUBLISHED',
      },
      {
        id: '0190eeee-0000-7000-8000-000000000013',
        title: 'Fall Research Participation Study',
        type: 'Research participation',
        newResponses: null,
        status: 'DRAFT',
      },
    ],
  } satisfies OfficeOverview,

  companyDashboard: {
    stats: { activeListings: 4, responses: 17, approvalStatus: 'APPROVED' },
    active: [
      {
        id: '0190eeee-0000-7000-8000-000000000021',
        title: 'Summer Software Internship',
        type: 'Internship',
        closesAt: '2027-06-14T16:00:00Z',
        responses: 9,
      },
      {
        id: '0190eeee-0000-7000-8000-000000000022',
        title: 'Nonprofit Website Redesign',
        type: 'Capstone sponsorship',
        closesAt: '2027-05-30T16:00:00Z',
        responses: 5,
      },
      {
        id: '0190eeee-0000-7000-8000-000000000023',
        title: 'Junior Data Analyst',
        type: 'Job',
        closesAt: '2027-05-22T16:00:00Z',
        responses: 1,
      },
    ],
    attention: { newResponses: 4 },
  } satisfies CompanyDashboard,
}

// ---- Administrator pages (src/api/admin.ts; #10, #15, #16) ----
// IDs are synthetic UUIDs.

export const ADMIN_IDS = {
  admin: '0190aaaa-0000-7000-8000-000000000001',
  member: '0190aaaa-0000-7000-8000-000000000002',
  office: '0190bbbb-0000-7000-8000-000000000001',
  pendingCompany: '0190bbbb-0000-7000-8000-000000000002',
} as const

export const adminFixtures = {
  pendingCompanies: {
    items: [
      { id: ADMIN_IDS.pendingCompany, name: 'Green Block Network', approvalStatus: 'PENDING' },
      {
        id: '0190bbbb-0000-7000-8000-000000000003',
        name: 'Bright Futures Foundation',
        approvalStatus: 'PENDING',
      },
    ],
    page: 0,
    size: 20,
    totalElements: 2,
    totalPages: 1,
  } satisfies PendingCompanyPage,

  auditEvents: {
    items: [
      {
        id: '0190cccc-0000-7000-8000-000000000002',
        actorId: ADMIN_IDS.admin,
        action: 'ROLE_ASSIGNED',
        targetType: 'USER',
        targetId: ADMIN_IDS.member,
        occurredAt: '2026-11-10T16:00:00Z',
      },
      {
        id: '0190cccc-0000-7000-8000-000000000001',
        actorId: ADMIN_IDS.admin,
        action: 'ORGANIZATION_APPROVED',
        targetType: 'ORGANIZATION',
        targetId: ADMIN_IDS.office,
        occurredAt: '2026-11-09T16:00:00Z',
      },
    ],
    page: 0,
    size: 20,
    totalElements: 2,
    totalPages: 1,
  } satisfies AuditPage,

  memberRoles: [
    {
      id: '0190dddd-0000-7000-8000-000000000001',
      userId: ADMIN_IDS.member,
      role: 'STUDENT',
    },
    {
      id: '0190dddd-0000-7000-8000-000000000002',
      userId: ADMIN_IDS.member,
      role: 'INSTITUTIONAL',
      organizationId: ADMIN_IDS.office,
    },
  ] satisfies RoleAssignment[],
}

type AdminApi = Pick<
  typeof adminApi,
  'getPendingCompanies' | 'getAuditEvents' | 'getRoleAssignments' | 'assignRole' | 'revokeRole'
>
type AdminOverrides = { [K in keyof AdminApi]?: AdminApi[K] }

/** Requires vi.mock('<path>/api/admin') in the calling test file. */
export function mockAdminApi(overrides: AdminOverrides = {}) {
  const defaults: Required<AdminOverrides> = {
    getPendingCompanies: async () => adminFixtures.pendingCompanies,
    getAuditEvents: async () => adminFixtures.auditEvents,
    getRoleAssignments: async () => adminFixtures.memberRoles,
    assignRole: async (body) => ({ id: '0190dddd-0000-7000-8000-0000000000ff', ...body }),
    revokeRole: async () => undefined,
  }
  return installMocks(adminApi, { ...defaults, ...overrides }, 'src/api/admin')
}

function installMocks<M extends object>(module: M, impl: Partial<M>, path: string) {
  for (const name of Object.keys(impl) as (keyof M)[]) {
    const fn = module[name]
    if (!vi.isMockFunction(fn)) throw new Error(`Mock helpers need vi.mock() of ${path}.`)
    vi.mocked(fn).mockReset()
    vi.mocked(fn).mockImplementation(impl[name] as never)
  }
  return vi.mocked(module, { deep: false })
}

type RoleApi = typeof roleData
type Overrides = { [K in keyof RoleApi]?: RoleApi[K] }

/** A rejected call shaped like a backend problem+json error. */
export const apiFailure = (status: number, detail?: string): Promise<never> =>
  Promise.reject(new ApiError(status, { status, title: 'Synthetic test failure', detail }))

/** A call that never settles, for asserting loading states. */
export const pending = (): Promise<never> => new Promise(() => {})

/** Requires vi.mock('<path>/api/roleData') in the calling test file. */
export function mockRoleApi(overrides: Overrides = {}) {
  const defaults: Required<Overrides> = {
    getStudentFeed: async () => fixtures.feed,
    getStudentActivity: async () => fixtures.activity,
    getOfficeOverview: async () => fixtures.officeOverview,
    getCompanyDashboard: async () => fixtures.companyDashboard,
  }
  return installMocks(roleData, { ...defaults, ...overrides }, 'src/api/roleData')
}

/** jsdom can't render colors; contrast is verified in a real browser instead. */
export async function expectNoAxeViolations() {
  const results = await axe.run(document.body, {
    rules: { 'color-contrast': { enabled: false } },
  })
  expect(results.violations).toEqual([])
}
