// Mirrors backend organizations/entity/Role.java. Update if PR #98 renames roles.
export const ROLES = ['STUDENT', 'INSTITUTIONAL', 'MODERATOR', 'COMPANY'] as const
export type Role = (typeof ROLES)[number]

const isRole = (value: unknown): value is Role =>
  typeof value === 'string' && (ROLES as readonly string[]).includes(value)

/** /api/auth/me does not return roles yet; until it does this yields []. */
export function rolesOf(user: object | null | undefined): Role[] {
  const roles: unknown = user && 'roles' in user ? user.roles : undefined
  return Array.isArray(roles) ? roles.filter(isRole) : []
}

export const hasAnyRole = (userRoles: readonly Role[], required: readonly Role[]) =>
  required.some((role) => userRoles.includes(role))
