# Audit Policy

## Purpose

Colegio Connect records successful high-risk administrative and sponsorship
actions in an append-only audit log. The log supports accountability without
copying credentials, request bodies, or authentication artifacts into a second
data store.

## Transaction contract

Business services call:

```java
auditService.record(actorId, action, targetType, targetId);
```

The caller must already have an active transaction. `AuditService` uses
`Propagation.MANDATORY`, so the audit insert joins the business transaction.
If the business operation fails or rolls back, the audit insert also rolls
back. Calling the service outside a transaction fails and writes nothing.

## Recorded fields

Each event contains only:

- UUIDv7 event identifier
- actor user UUID
- controlled action enum
- controlled target-type enum
- target UUID
- server timestamp

The current action taxonomy is `ROLE_ASSIGNED`, `ROLE_REVOKED`,
`ORGANIZATION_APPROVED`, `ORGANIZATION_REJECTED`, `LISTING_HIDDEN`,
`LISTING_REMOVED`, `ACCOUNT_SUSPENDED`, and `SPONSORSHIP_ACCEPTED`.

Passwords, password hashes, email addresses, cookies, CSRF tokens,
verification codes, session identifiers, request headers, request bodies, and
free-form details must not be stored in this table.

## Read API

`GET /api/admin/audit-events?page=0&size=20` returns newest events first.
`page` must be non-negative and `size` must be between 1 and 100. The response
contains `items`, `page`, `size`, `totalElements`, and `totalPages`.

The endpoint requires a currently active, verified user with a persisted
`ADMIN_MODERATOR` role. It rechecks database state on every request, so account
suspension or role revocation immediately denies an existing session. Anonymous
requests receive 401; authenticated users without permission receive 403.

## Integration obligation

This foundation does not invent role, approval, moderation, or sponsorship
mutation endpoints. Each future business service implementing those actions
must call `AuditService.record` inside the same transaction. Issue #15 is not
fully accepted until every applicable production action is wired and its
success/rollback behavior is tested.
