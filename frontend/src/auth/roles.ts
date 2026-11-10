// Mirrors backend organizations/entity/Role.java.
export const ROLES = ['STUDENT', 'INSTITUTIONAL', 'MODERATOR', 'COMPANY'] as const
export type Role = (typeof ROLES)[number]

/**
 * Team decision (2026-11-07): the administrator can open every page for testing and
 * maintenance. Until an ADR names a separate role, the administrator is MODERATOR.
 */
export const ADMIN_ROLE: Role = 'MODERATOR'

const isRole = (value: unknown): value is Role =>
  typeof value === 'string' && (ROLES as readonly string[]).includes(value)

/** /api/auth/me does not return roles yet; until it does this yields []. */
export function rolesOf(user: object | null | undefined): Role[] {
  const roles: unknown = user && 'roles' in user ? user.roles : undefined
  return Array.isArray(roles) ? roles.filter(isRole) : []
}

export const isAdmin = (userRoles: readonly Role[]) => userRoles.includes(ADMIN_ROLE)

/** UX only: the backend still enforces every permission. Admins pass every check. */
export const hasAnyRole = (userRoles: readonly Role[], required: readonly Role[]) =>
  isAdmin(userRoles) || required.some((role) => userRoles.includes(role))
