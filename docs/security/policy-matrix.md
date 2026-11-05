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

A user can hold more than one role. Roles are loaded as `ROLE_*` authorities at login.

## Shared checks

Defined in `security/AuthorizationPolicy` and used from `@PreAuthorize` as `@authz.*`.

| Check | Method | Purpose |
| --- | --- | --- |
| Account status | `isActive(authentication)` | Account exists and is not suspended. Checked on every request, not only at login. |
| Organization membership | `hasRoleIn(authentication, role, orgId)` | User has the role inside that organization. |
| Approval status | `isApproved(orgId)` | An administrator approved the organization. |
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