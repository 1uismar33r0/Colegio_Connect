# Authorization policy matrix

Issue #9. Source: approved proposal, Table 2, Figure 3 and Access model (p. 14).

## Role names

The database uses the names agreed in V1. They map to the proposal like this:

| Code (V1) | Proposal role |
| --- | --- |
| `STUDENT` | Student |
| `INSTITUTIONAL` | UPRM Faculty / Office / Department |
| `COMPANY` | External Company |
| `MODERATOR` | Admin / Moderator |

A user can hold more than one role. Roles are loaded as `ROLE_*` authorities at login. On the Issue #10 branch, login and `/api/auth/me` also return distinct current role names; `/me` reloads them from the database. Sensitive administrator endpoints use the persisted `@authz.canModerate(authentication)` check, so a stale session authority does not preserve administrator access after revocation.

## Shared checks

Defined in `security/AuthorizationPolicy` and used from `@PreAuthorize` as `@authz.*`.

| Check | Method | Purpose |
| --- | --- | --- |
| Account status | `isActive(authentication)` | Account exists and is not suspended. Applied to every authenticated request in `SecurityConfig`, so a user suspended after login is blocked right away. |
| Organization membership | `hasRoleIn(authentication, role, orgId)` | User has the role inside that organization. |
| Approval status | `isApproved(orgId)` | An administrator approved the organization. |
| Moderator actions | `canModerate(authentication)` | Account remains active, verified, and a UPRM member; a global persisted `MODERATOR` assignment still exists. Used for audit-history reading, pending-company queue access, company decisions, and role administration, so revocation denies the next request even with an existing session. |
| Ownership | Planned for M2 | User owns the listing. Listings do not exist yet. |

## Matrix

Every action requires `isActive`.

| Action | Allowed role | Extra checks | Milestone |
| --- | --- | --- | --- |
| Browse, search and open listings | Any signed-in role | — | M2 |
| Save listings | `STUDENT` | — | M2 |
| Respond to a listing | `STUDENT` | — | M3 |
| Report a listing | `STUDENT`, `INSTITUTIONAL`, `COMPANY` | — | M3 |
| Publish institutional listings | `INSTITUTIONAL` | Membership in the organization | M2 |
| Publish jobs, internships, COOP, capstone calls | `COMPANY` | Membership + organization approved | M2 |
| View respondents | `INSTITUTIONAL`, `COMPANY` | Ownership of the listing | M3 |
| Publish a student capstone project | `STUDENT` | Team membership | M4 |
| Accept or decline Express Interest | `STUDENT` | Team membership | M4 |
| Send Express Interest | `COMPANY` | Membership + organization approved | M4 |
| Approve companies and assign roles | `MODERATOR` | — | M1 (#10), M2 |
| Review reports, hide or remove listings, restrict accounts | `MODERATOR` | — | M3 |
| Review audit history | `MODERATOR` | — | M3 |

## Privacy rule

Companies cannot search or browse students. A company sees a student's data only when
that student responds to its listing, or a student team accepts its Express Interest.

## Issue #122 dashboard reads (stacked branch, not yet merged)

| Read | Current persisted entitlement | Returned scope |
| --- | --- | --- |
| Discovery feed | Active authenticated account; publisher eligibility is checked in the query | PUBLIC, published, unexpired records only; organization display name, no publisher contact |
| Student activity | Verified UPRM account with current global `STUDENT` assignment | Only the session user's response and saved counts |
| Office overview | Verified UPRM account with current `INSTITUTIONAL` assignment in the selected office/department | That organization only; omission allowed only for one eligible membership |
| Company dashboard | Verified external account with current `COMPANY` assignment in an approved company | That company only; a separate pending applicant ownership link allows a read-only pending status, with no listings or responses |
| Review queue | Verified UPRM account with current global `MODERATOR` assignment | Pending companies and distinct open reported listings; no reporter identity or report detail |

The dashboard branch creates a pending applicant ownership *read* link, but external registration does not yet write it. This link never grants `COMPANY` authority. Current status and assignments are rechecked on every request; an ambiguous company or office selection returns 409. See [dashboard contract](../roles/dashboard-api-contract.md) for remaining data-flow dependencies.

## Issue #10 administrator boundary

The role API searches only a complete normalized email and returns bounded, minimal results. `STUDENT` and `MODERATOR` are global; `INSTITUTIONAL` is scoped to a UPRM office or department; `COMPANY` is scoped to an approved external company. Assignment requires an active, verified account of the correct account type. Revocation removes only the specified assignment; moderators cannot revoke their own moderator role or the last eligible moderator. Mutations require the existing session and CSRF protection and write same-transaction audit events. Startup bootstrap is a separate one-time system action; it never grants roles through registration. These controls do not relax the student consent rule above. See [ADR 0002](../decisions/0002-roles-and-admin-access.md) and the [API contract](../roles/api-contract.md).
