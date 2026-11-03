# Issue #15 Audit Foundation Design

## Intent

Provide the append-only audit foundation required for future role changes,
organization approvals, moderation, and sponsorship actions. Every successful
high-risk action must record its actor, action, target, and timestamp in the
same database transaction. A failed transaction must leave no audit row.

## Goals

- Persist a minimal, typed audit event with no arbitrary secret-bearing payload.
- Require callers to already be inside the business transaction.
- Provide a moderator-only, paginated read endpoint.
- Establish reusable action and target taxonomies for later issues.
- Prove commit and rollback behavior against PostgreSQL 18.6.

## Non-goals

- No role-assignment, organization-approval, moderation, or sponsorship action
  is invented by this issue.
- No dashboard, export, retention service, or generalized logging platform.
- No failed-login or access-denial audit row: Issue #15 requires failed business
  actions to leave no row, and authentication-event policy is not yet approved.
- No free-form metadata, request body, cookie, CSRF token, verification code,
  password, password hash, or session identifier is stored.

## Architecture

Flyway V5 creates `audit_log` with UUIDv7 identity, actor foreign key, typed
action and target strings, target UUID, and timestamp. `AuditService.record`
uses `Propagation.MANDATORY`; it joins the caller's transaction and cannot be
called as an independent best-effort write. Rollback of the business action
therefore rolls back its audit row.

`AuditQueryService` returns a bounded newest-first page. `AuditController`
exposes `GET /api/admin/audit-events` and uses
`@authorizationPolicy.canModerate(authentication)`, which checks current
persisted account and role state rather than session-only authorities.

## Alternatives considered

1. **Mandatory same-transaction persistence (selected).** Meets the live issue
   requirement and makes missing audit rows a transaction failure.
2. `REQUIRES_NEW` best-effort persistence. Rejected because it can retain a row
   for a failed action and contradicts the acceptance test.
3. Asynchronous event logging. Rejected because delivery can be lost and is
   unnecessary for the current scale.

## Security and privacy

Only identifiers and controlled enums cross the audit API. There is no details
field into which secrets can be copied. The read response exposes actor UUID,
action, target type, target UUID, and timestamp; it does not expose credentials,
email addresses, request headers, or authentication artifacts. Page size is
bounded to 100.

## Dependencies

This work is stacked on PR #98 (`a4d7afd`) to use the canonical V4 schema and
persisted `ADMIN_MODERATOR` authorization check. The next migration is V5.
