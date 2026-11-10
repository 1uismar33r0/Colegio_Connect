import type { Role } from './roles'

/** One entry per role area: where it lives, its nav label and the account label for the role. */
export const AREAS: { to: string; label: string; role: Role; accountLabel: string }[] = [
  { to: '/student', label: 'Student', role: 'STUDENT', accountLabel: 'Student' },
  {
    to: '/institution',
    label: 'Institution',
    role: 'INSTITUTIONAL',
    accountLabel: 'University office',
  },
  { to: '/company', label: 'Company', role: 'COMPANY', accountLabel: 'External organization' },
  { to: '/moderation', label: 'Moderation', role: 'MODERATOR', accountLabel: 'Administrator' },
]

/** Labels for the roles a user actually holds, in AREAS order. */
export const accountLabels = (roles: readonly Role[]) =>
  AREAS.filter((a) => roles.includes(a.role)).map((a) => a.accountLabel)
