# Issue #15 Audit Foundation Reconciliation Design

## Intent

Recover Gabriel's audit foundation from open PR #99 onto the authoritative authorization model on current `main`. The foundation must record actor, action, target, and time in the same transaction as a future business action, expose only a bounded moderator read API, and avoid sensitive free-form payloads. This branch does not implement role changes, approvals, moderation, restrictions, or sponsorship actions owned by other issues.

## Verified baseline and history

- Authoritative baseline: `origin/main` at `e5cc181b3ebcf7b5cb1050547c1d82922068c064`, containing Massini's merged PRs #111 and #112.
- Original work: open PR #99, head `2e57200f37a461f9f74fcf79b26c56fab1060770`, with five Gabriel-authored commits from `76275c6441b9f03871122f7bfd419a5a542a993f` through the head.
- Dependency: PR #99 is based directly on open PR #98 head `a4d7afd741a4ae6024241c3e7caad37b8da21c96`. Its `ADMIN_MODERATOR`, V5 numbering, and `authorizationPolicy.canModerate` references therefore cannot be copied unchanged.
- Remote audit sweep: no newer audit implementation exists. Only PR #99 and CI-only validation branch `769e61668bf9278b4d8e96ce3c0eeb17be77c54b` contain audit production code.
- PR #115 head `3eb0a11974d23fe06ffe16b650d3069e7450e844` writes masked email delivery outcomes to SLF4J. It does not persist administrative audit events and does not overlap this schema.

## Requirements and constraints

- Proposal objective O3 and indicator I8 require every listed administrative and sponsorship action to create an audit record containing actor, action, target, and time.
- Issue #15 requires a migration, same-transaction `AuditService.record`, paginated admin read endpoint, and success/rollback tests.
- Current `main` uses roles `STUDENT`, `INSTITUTIONAL`, `MODERATOR`, and `COMPANY`; Massini's production authorization remains authoritative.
- Current login sessions snapshot `ROLE_*` authorities. Audit read access must therefore use a persisted moderator check if role revocation is to affect an existing session immediately.
- Store synthetic identifiers and controlled enums only. Do not store email, password/hash, verification code, cookie, CSRF token, session ID, request body/header, or free-form details.
- Existing PRs remain open. No application action is integrated merely to manufacture audit evidence.

## Approaches considered

### 1. Cherry-pick PR #99 unchanged

Rejected. It depends on PR #98's obsolete role enum, V4 migration, and authorization methods.

### 2. Recover only the command-side service and defer reading

Rejected. The live Issue #15 task explicitly includes a paginated admin read endpoint, and the proposal assigns audit-history review to moderators.

### 3. Main-based semantic reconciliation (selected)

Port the minimal audit model, migration, transaction-bound service, pagination DTOs/query, and controller. Renumber the migration to V4 and adapt authorization to current `MODERATOR` with a persisted global-role check. Preserve the original rollback and data-minimization behaviors, add the exact existing-session revocation case at the real read endpoint, and leave event emission to owning issues.

## Design

### Persistence

Flyway V4 creates `audit_log` with UUIDv7 `id`, non-null `actor_id` referencing `users`, constrained action and target strings, target UUID, and server timestamp. No details column exists. `AuditEvent` maps these fields as non-updatable. The repository supports newest-first pagination.

The actor foreign key preserves referential integrity and means a user referenced by audit history cannot currently be physically deleted. No user-deletion contract exists on the baseline; retention/anonymization must be decided before one is added. The schema is append-only by application contract, not by a database trigger; this limitation is documented rather than expanding Issue #15 into retention administration.

### Transaction contract

`AuditService.record(actorId, action, targetType, targetId)` uses `Propagation.MANDATORY`. A caller outside a transaction fails, a successful containing transaction persists the event, and a rollback removes it. Future business services must call it after authorization and inside their own mutation transaction.

### Read authorization

`AuthorizationPolicy.canModerate(authentication)` checks both current active account state and a persisted global `MODERATOR` assignment. `RoleAssignmentRepository.existsByUserIdAndRoleAndOrganizationIsNull` distinguishes the platform moderator role from organization-scoped assignments. `AuditController` uses this policy through `@PreAuthorize`, so deleting the assignment denies the next request even when the same authenticated session still contains a stale `ROLE_MODERATOR` authority.

### Read API and privacy

`GET /api/admin/audit-events?page=0&size=20` returns newest-first pages with a maximum size of 100. Responses include only event ID, actor UUID, action, target type, target UUID, and timestamp. The dynamic Springdoc document must include the endpoint; no checked-in static OpenAPI file exists.

### Test isolation

Audit integration tests remove audit rows before referenced users both before and after every test. This avoids modifying unrelated existing test classes solely because test-class order is unspecified.

## Future integrations

The following owners add event emission when their contracts stabilize:

- Issue #10: role assignment and revocation.
- Issue #16: organization approval and rejection.
- Issues #19, #37, and #39: listing state changes, reports, moderation, and account restrictions.
- Issue #52: sponsorship acceptance/contact release, including required pair evidence.

Each integration needs success plus rollback/no-row coverage. Foundation completion is not final I8 acceptance.

## Documentation and review

A scoped Gabriel record will preserve original and reconciled commit SHAs, real excerpts, attribution, tests, PR/review/merge state, limitations, and a section titled **Changes to Recheck When Other PRs Merge**. Traceability, test evidence, development log, and midterm evidence index will link to that authoritative record rather than duplicate it.
