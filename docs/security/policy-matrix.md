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

A user can hold more than one role. Roles are loaded as `ROLE_*` authorities at login and returned as `roles` by `POST /api/auth/login` and `GET /api/auth/me`, for the UI only. Server checks always use `@authz`.

Login 403 responses include a stable `code` so the frontend does not depend on the message text:

| `code` | Meaning |
| --- | --- |
| `EMAIL_NOT_VERIFIED` | Correct password, but the email is not verified yet |
| `ACCOUNT_SUSPENDED` | Correct password, but the account is suspended |

## Shared checks

Defined in `security/AuthorizationPolicy` and used from `@PreAuthorize` as `@authz.*`.

| Check | Method | Purpose |
| --- | --- | --- |
| Account status | `isActive(authentication)` | Account exists and is not suspended. Applied to every authenticated request in `SecurityConfig`, so a user suspended after login is blocked right away. |
| Organization membership | `hasRoleIn(authentication, role, orgId)` | User has the role inside that organization. |
| Approval status | `isApproved(orgId)` | An administrator approved the organization. |
| Moderator actions | `canModerate(authentication)` | In the dashboard branch, account must be active, email verified, and a UPRM member with a current global persisted `MODERATOR` assignment. Used for audit-history reading, pending-company queue access, role administration and company decisions, so revocation denies the next request even with an existing session. |
| Role management | `canModerate(authentication)` | Only an active moderator can list, assign, or revoke `INSTITUTIONAL` and `MODERATOR` roles (`/api/admin/role-assignments`). Each change writes an audit record. A moderator cannot remove their own moderator role. The first moderator is created at startup from `APP_BOOTSTRAP_ADMIN_EMAIL`, never through the API. |
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

## Issue #122 dashboard reads (main-based branch)

| Read | Current persisted entitlement | Returned scope |
| --- | --- | --- |
| Discovery feed | Active authenticated account; publisher eligibility is checked in the query | PUBLIC, published, unexpired records only; organization display name, no publisher contact |
| Student activity | Verified UPRM account with current global `STUDENT` assignment | Only the session user's response and saved counts |
| Office overview | Verified UPRM account with current `INSTITUTIONAL` assignment in the selected office/department | That organization only; omission allowed only for one eligible membership |
| Company dashboard | Verified external account with current `COMPANY` assignment in an approved company | That company only; a separate pending applicant ownership link allows a read-only pending status, with no listings or responses |
| Review queue | Verified UPRM account with current global `MODERATOR` assignment | Pending companies and distinct open reported listings; no reporter identity or report detail |

The pending applicant ownership link never grants `COMPANY` authority. Current status and assignments are checked on every dashboard request; an ambiguous company or office selection returns 409. Current main lacks a company/student role assignment endpoint and external registration does not yet create applicant links. See [dashboard contract](../roles/dashboard-api-contract.md) and [role reconciliation](../roles/reconciliation-123-124-125.md).
