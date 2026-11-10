# Issue #15 Audit Foundation Reconciliation Plan

> **Execution:** Follow repository lifecycle and TDD. Base every comparison on `origin/main` `e5cc181b3ebcf7b5cb1050547c1d82922068c064`. Do not merge or close existing PRs.

**Goal:** Reconcile Gabriel's PR #99 audit foundation onto Massini's current authorization baseline and publish it independently for review.

**Spec:** `docs/superpowers/specs/2026-11-08-issue-15-audit-reconciliation-design.md`

**Architecture:** A minimal JPA/Flyway audit module records controlled event fields in the caller's transaction. A bounded read endpoint uses a persisted moderator policy so current database state, not stale session authorities, controls audit access.

**Tech stack:** Java 25, Spring Boot 4.1.2, Spring Security method authorization, JPA, Flyway, PostgreSQL 18.6 Testcontainers, JUnit 5, MockMvc.

## Global constraints

- Preserve PR #99 authorship and history; do not claim it merged.
- Keep PR #118 separate.
- No future business mutation endpoints or speculative integrations.
- Use synthetic data and never store sensitive/free-form audit payloads.
- Run focused audit tests and full `mvnw.cmd clean verify` with Java 25 before publishing.

## File map

- `backend/src/main/resources/db/migration/V4__audit_log_foundation.sql`: constrained audit table and newest-first index.
- `backend/src/main/java/com/colegioconnect/audit/*`: enums, entity, repository, transaction service, query service, DTOs, and controller.
- `backend/src/main/java/com/colegioconnect/security/AuthorizationPolicy.java`: persisted moderator read policy.
- `backend/src/main/java/com/colegioconnect/organizations/repository/RoleAssignmentRepository.java`: global-role existence query.
- `backend/src/test/java/com/colegioconnect/audit/*`: transaction, rollback, permissions, revocation, pagination, privacy, and OpenAPI integration coverage.
- `docs/gabriel/security/issue-15-audit-foundation.md`: authoritative engineering record.
- Project evidence registers: links and current state only.

## Task 1: Recover transaction-bound audit persistence

**Interfaces:** Produces `AuditService.record`, `AuditEventRepository`, V4 schema, and controlled enums for Task 2 and future owning issues.

1. Add `AuditServiceIntegrationTest` first for committed persistence, containing-transaction rollback, and rejection outside a transaction.
2. Run the focused class and confirm RED because audit types/schema do not exist.
3. Add V4, enums, entity, repository, and `AuditService` with `Propagation.MANDATORY`.
4. Run the focused class and confirm all three tests pass.
5. Commit the transaction foundation.

## Task 2: Recover protected paginated reading

**Interfaces:** Consumes Task 1 repository/events. Produces the persisted `canModerate` policy and read contract consumed by documentation and future UI work.

1. Add `AuditControllerIntegrationTest` first for moderator access/data minimization, non-moderator 403, anonymous 401, newest-first pagination, bounds, suspension, role revocation with the same session, and OpenAPI discovery.
2. Run the focused class and confirm RED because the endpoint/policy/query DTOs do not exist.
3. Add `existsByUserIdAndRoleAndOrganizationIsNull` and `AuthorizationPolicy.canModerate` against current `MODERATOR`.
4. Add query DTOs/service/controller with size bounded to 100 and `@PreAuthorize("@authz.canModerate(authentication)")`.
5. Add before/after audit fixture cleanup so unrelated tests do not depend on test-class order.
6. Run both audit test classes and confirm all focused cases pass.
7. Run full Java 25 `mvnw.cmd clean verify` and inspect counts/output.
8. Commit the read foundation and tests.

## Task 3: Preserve engineering evidence

**Interfaces:** Consumes exact code/test SHAs from Tasks 1-2 and produces reviewable records without duplicating PR #117 or PR #118 narratives.

1. Add `docs/gabriel/security/issue-15-audit-foundation.md` with original PR #99 history, dependency analysis, reconciled excerpts, privacy/permission explanations, test evidence, limitations, future ownership, and **Changes to Recheck When Other PRs Merge**.
2. Update Issue #15 traceability, test evidence, development log, and midterm evidence index with links and justified engineering state.
3. Verify local links, references, SHA accuracy, migration consistency, secret patterns, and documentation diff.
4. Run focused audit tests and full Java 25 backend verification again on the documented revision.
5. Commit documentation.

## Task 4: Review and publish

1. Review the complete `origin/main...HEAD` diff against this plan and the user request; correct Critical/Important findings.
2. Confirm only intended files are staged/committed and the branch is clean.
3. Push `gabriel/issue-15-audit-reconciliation` and open a PR targeting current `main` with Issue #15 support, original PR #99 attribution, exact verification, limitations, and a teammate review request.
4. Verify the hosted diff and CI on the final head. Do not merge or close Issue #15/PR #99.
