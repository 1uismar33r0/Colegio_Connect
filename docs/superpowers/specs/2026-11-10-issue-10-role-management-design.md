# Issue #10 Backend Role Management Design

**Source:** Canonical [Issue #10](https://github.com/1uismar33r0/Colegio_Connect/issues/10), alias #43, Issue #9 follow-up, the approved proposal pp. 12–16, and the November 10 backend-only task. Baseline `main` is `5ff997463a73a0f17f54fb4a4a4c7ec67b0f6f2e`; PR #119 is merged as `2267d52`.

## Existing contracts

The V1 database and Java enum use `STUDENT`, `INSTITUTIONAL`, `COMPANY`, and `MODERATOR`. V1's `UNIQUE NULLS NOT DISTINCT (user_id, role, organization_id)` already prevents duplicate global and scoped assignments. The authorization policy's `canModerate` checks the persisted global role on each protected request. Audit V4 has `ROLE_ASSIGNED` and `ROLE_REVOKED` and requires an enclosing transaction. Keep those implementations and the V4/V5 migration order.

## API and eligibility

- Return distinct, sorted current roles from the database in login and `/api/auth/me` responses. An unassigned account gets `[]`. The session's existing authorities remain an authentication detail; new protected endpoints use `@authz.canModerate(authentication)`.
- `GET /api/admin/users?email=...&page=0&size=20` requires an exact, case-normalized email. Require a syntactically plausible email of at most 254 characters, bound size to 1–100, and return the existing `items/page/size/totalElements/totalPages` envelope. Use an indexed `lower(email)` lookup and one batched query for that page's assignments.
- `POST` and `DELETE /api/admin/users/{id}/roles` take `{role, organizationId}`. Both require an active persisted global moderator. Global `STUDENT`/`MODERATOR` have no organization; `INSTITUTIONAL` needs a UPRM office/department; `COMPANY` needs an approved external company. Only active, verified UPRM members receive Student, Institutional, or Moderator; only active, verified external accounts receive Company. Missing user/organization/assignment yields 404, duplicate or lockout yields 409, invalid combinations yield 400. Successful changes return 204.
- Refuse self-revocation of `MODERATOR` and removal of the last active, verified, eligible global moderator. Each successful administrator change and its audit event share one transaction. The audit target is the affected user. A single durable lock row serializes role changes and bootstrap, while V1 uniqueness remains the database backstop.

## Bootstrap

V6 adds a singleton `role_admin_state` row and an indexed normalized-email expression. At startup, nonblank `APP_BOOTSTRAP_ADMIN_EMAIL` is matched exactly to an existing active, verified UPRM member. The transactional bootstrap locks the singleton row. It grants one global moderator only if no eligible global moderator exists, then marks the row completed. If no eligible account exists, it leaves the row pending for a later startup. Once completed, including when an administrator existed already, it never regrants after revocation. No public endpoint or configured email is logged. The state row records the system initialization separately because audit V4 requires a human user FK as actor; administrator-initiated API changes use audit V4 directly.

## Boundaries

The moderator role permits these administrative endpoints, not unrestricted access to student data, organization-owned content, or consent-gated contact details. Luis will regenerate the frontend client after API review; no frontend source changes belong in this branch. Cristian remains the backend lead and requested reviewer. No listing or sponsorship policy is invented here.
