# Backend role-management contract

**Branch:** `gabriel/issue-10-role-management-backend`, pending Cristian review and merge. Source of truth is backend OpenAPI at `/v3/api-docs` and the controller/DTO code. This document describes its current branch behavior, not an integrated frontend feature.

All authenticated operations use the existing server session and CSRF policy. Mutation requests require a CSRF token. Administrator routes require a currently active, verified UPRM member with a persisted global moderator assignment; role names retained in a login session do not grant access after revocation.

| Operation | Request | Success | Principal errors |
| --- | --- | --- | --- |
| `GET /api/auth/me` | Existing session | Identity with distinct sorted `roles`, or `[]` | 401 unauthenticated |
| `POST /api/auth/login` | Existing login body and CSRF | Identity with current `roles` | Existing auth errors |
| `GET /api/admin/users?email=student@upr.edu&page=0&size=20` | Complete email, case normalized; page >= 0; size 1–100 | 200, `items`, `page`, `size`, `totalElements`, `totalPages` | 400 invalid search/page; 401/403 access |
| `POST /api/admin/users/{id}/roles` | `{ "role": "STUDENT", "organizationId": null }` | 204 | 400 invalid role/scope, 404 missing user/org, 409 duplicate or ineligible account/company, 401/403 access |
| `DELETE /api/admin/users/{id}/roles` | Same body, identifying the exact assignment | 204 | 400 invalid role/scope, 404 missing user/assignment, 409 self/last moderator lockout, 401/403 access |

An `/me` or login example is `{ "id": "<uuid>", "email": "student@upr.edu", "accountType": "UPRM_MEMBER", "roles": ["STUDENT"] }`. An administrator search item includes only `id`, `email`, `accountType`, `status`, distinct `roles`, and `roleAssignments` such as `{ "role": "INSTITUTIONAL", "organizationId": "<uuid>" }`. Empty search pages return `items: []`; no password, verification code, or session detail is returned. Errors follow the backend's existing structured error handling.

`STUDENT` and `MODERATOR` are global (`organizationId: null`). `INSTITUTIONAL` requires a UPRM office or department ID. `COMPANY` requires an approved external company ID and an `EXTERNAL_ORGANIZATION` account. Assignment targets must be active and email verified. Revocation removes only the specified role and scope. Successful administrator mutations write a `USER` audit event with actor, action, target and timestamp in the same transaction. Initial startup bootstrap writes a `SYSTEM` audit event with `actorId: null`.

After backend integration, Luis should run `cd frontend; npm.cmd run api:fetch; npm.cmd run api:gen` against a running backend (or set `VITE_API_TARGET` to that backend) and review the generated schema diff before implementing frontend calls. The frontend has no role-management UI or generated-client changes in this branch. Organization choice and administrator UX need a separate frontend task.
