import type { UserResponse } from '../api/client'
import { hasAnyRole, type Role } from './roles'

export type AccountType = NonNullable<UserResponse['accountType']>

type Area = {
  to: string
  label: string
  role: Role
  accountLabel: string
  /**
   * An account type that may also open the area without the role. Pending company owners get no
   * COMPANY role until approval but can view their read-only dashboard (PR #127); the backend
   * decides what each account actually sees.
   */
  accountType?: AccountType
}

/** One entry per role area: where it lives, its nav label and the account label for the role. */
export const AREAS: Area[] = [
  { to: '/student', label: 'Student', role: 'STUDENT', accountLabel: 'Student' },
  {
    to: '/institution',
    label: 'Institution',
    role: 'INSTITUTIONAL',
    accountLabel: 'University office',
  },
  {
    to: '/company',
    label: 'Company',
    role: 'COMPANY',
    accountLabel: 'External organization',
    accountType: 'EXTERNAL_ORGANIZATION',
  },
  { to: '/moderation', label: 'Moderation', role: 'MODERATOR', accountLabel: 'Administrator' },
]

/** UX only: whether to offer the area. The backend still enforces every permission. */
export const canOpenArea = (
  area: Pick<Area, 'role' | 'accountType'>,
  roles: readonly Role[],
  user: UserResponse | null | undefined,
) =>
  hasAnyRole(roles, [area.role]) ||
  (area.accountType !== undefined && user?.accountType === area.accountType)

/** Labels for the roles a user actually holds, in AREAS order. */
export const accountLabels = (roles: readonly Role[]) =>
  AREAS.filter((a) => roles.includes(a.role)).map((a) => a.accountLabel)
