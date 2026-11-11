import type { components } from './schema'
import { api, unwrap } from './client'

// Real moderator endpoints on main (#10, #15, #16). All require an active MODERATOR; the backend
// rechecks that on every request. Types come from the generated OpenAPI schema.

type Schemas = components['schemas']
export type PendingCompanyPage = Schemas['PendingCompanyPageResponse']
export type PendingCompany = Schemas['PendingCompanyResponse']
export type AuditPage = Schemas['AuditPageResponse']
export type AuditEvent = Schemas['AuditEventResponse']
export type RoleAssignment = Schemas['RoleAssignmentResponse']
export type RoleAssignmentRequest = Schemas['RoleAssignmentRequest']

// The assignable roles live in auth/roles.ts (ASSIGNABLE_ROLES), so tests that vi.mock() this
// module still see the real list.
const PAGE_SIZE = 20

export const getPendingCompanies = (page = 0) =>
  unwrap(
    api.GET('/api/admin/organizations/pending', {
      params: { query: { page, size: PAGE_SIZE } },
    }),
  )

export const getAuditEvents = (page = 0) =>
  unwrap(api.GET('/api/admin/audit-events', { params: { query: { page, size: PAGE_SIZE } } }))

export const getRoleAssignments = (userId: string) =>
  unwrap(api.GET('/api/admin/role-assignments', { params: { query: { userId } } }))

export const assignRole = (body: RoleAssignmentRequest) =>
  unwrap(api.POST('/api/admin/role-assignments', { body }))

export const revokeRole = (id: string) =>
  unwrap(api.DELETE('/api/admin/role-assignments/{id}', { params: { path: { id } } }))
