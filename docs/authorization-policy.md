# Server-side authorization policy

This policy implements Issue #9's M1 authorization foundation. It preserves the
approved two account types (`UPRM_MEMBER`, `EXTERNAL_ORGANIZATION`) and four
persisted roles. Account type is not an authorization role.

| Role | Server-side policy | Current endpoint coverage |
| --- | --- | --- |
| `STUDENT` | May act only as the active, verified account owner. | `GET /api/auth/me` requires an active account. |
| `FACULTY_OFFICE_DEPT` | May publish for an organization only when assigned to that organization. | Reusable `canPublishForOrganization` check for future listing endpoints. |
| `EXTERNAL_COMPANY` | May publish for an assigned organization only when that organization is `APPROVED`. | Reusable `canPublishForOrganization` check for future listing endpoints. |
| `ADMIN_MODERATOR` | May moderate and is the sole role eligible to assign roles. | Reusable `canModerate` and `canAssignRoles` checks for future moderation/admin endpoints. |

## Shared checks

`AuthorizationPolicy` reads the current database state for each decision:

- Active and verified account status.
- Persisted role assignment, including multi-role users.
- Ownership by user UUID.
- Organization membership.
- Organization approval status.

The login session also receives `ROLE_<ROLE_NAME>` authorities from the
persisted assignments. The policy still consults persisted state so a
suspension, role revocation, or approval-status change takes effect without
requiring a user to log in again.

No role-assignment API is exposed by this foundation. Consequently a
registration request or an ordinary authenticated user cannot self-assign a
role. A future role-management endpoint must use
`@authorizationPolicy.canAssignRoles(authentication)` and record its audit
requirements before it is introduced.

Consent-based student-contact checks cannot be implemented yet: the current
schema has neither a consent model nor a contact-release endpoint. Any such
endpoint must add a persisted consent check alongside the ownership, account,
role, and organization checks above.
