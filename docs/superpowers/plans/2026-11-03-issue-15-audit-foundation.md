# Issue #15 Audit Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a transactional, append-only audit log and moderator-only paginated read endpoint for future administrative and sponsorship actions.

**Architecture:** Flyway V5 and a JPA entity persist typed audit events. A mandatory-transaction command service records events, while a separate query service and persisted-policy-protected controller expose bounded read access.

**Tech Stack:** Java 25, Spring Boot 4.1.2, Spring Data JPA, Spring Security 7, Flyway, PostgreSQL 18.6, JUnit, MockMvc, Testcontainers.

**Spec:** `docs/superpowers/specs/2026-11-03-issue-15-audit-foundation-design.md`

## Global Constraints

- Branch from PR #98 head `a4d7afd`; do not modify PR #98 history.
- Use Flyway V5; never edit published V1-V4 migrations.
- Audit writes must join the caller's transaction and roll back with it.
- Store no passwords, hashes, cookies, CSRF tokens, verification codes, session identifiers, or free-form request data.
- Do not invent business mutation endpoints solely to exercise auditing.
- Use Java 25 and PostgreSQL 18.6 Testcontainers for verification.

## Review Focus

- A caller invokes `record` without a transaction: reject the call and write nothing.
- A business transaction throws after recording: roll back the audit row.
- A non-moderator or suspended moderator reads the log: return 403.
- A client requests an invalid or oversized page: return 400 and never issue an unbounded query.
- An audit response is serialized: expose identifiers and taxonomy only, never user credentials or authentication artifacts.

---

### Task 1: Persistent audit event and mandatory transaction service

**Files:**
- Create: `backend/src/main/resources/db/migration/V5__audit_log_foundation.sql`
- Create: `backend/src/main/java/com/colegioconnect/audit/AuditAction.java`
- Create: `backend/src/main/java/com/colegioconnect/audit/AuditTargetType.java`
- Create: `backend/src/main/java/com/colegioconnect/audit/AuditEvent.java`
- Create: `backend/src/main/java/com/colegioconnect/audit/AuditEventRepository.java`
- Create: `backend/src/main/java/com/colegioconnect/audit/AuditService.java`
- Test: `backend/src/test/java/com/colegioconnect/audit/AuditServiceIntegrationTest.java`

**Interfaces:**
- Produces: `void AuditService.record(UUID actorId, AuditAction action, AuditTargetType targetType, UUID targetId)`.
- Produces: append-only `AuditEvent` getters for query mapping.

- [ ] Write integration tests for committed persistence, rollback removal, and mandatory transaction rejection.
- [ ] Run the focused test and confirm RED because the audit types/service do not exist.
- [ ] Add V5, enums, entity, repository, and the minimal `Propagation.MANDATORY` service.
- [ ] Run the focused test and confirm GREEN.
- [ ] Commit as `feat(audit): introduce transactional audit foundation`.

### Task 2: Moderator-only paginated read endpoint

**Files:**
- Create: `backend/src/main/java/com/colegioconnect/audit/AuditEventResponse.java`
- Create: `backend/src/main/java/com/colegioconnect/audit/AuditPageResponse.java`
- Create: `backend/src/main/java/com/colegioconnect/audit/AuditQueryService.java`
- Create: `backend/src/main/java/com/colegioconnect/audit/AuditController.java`
- Test: `backend/src/test/java/com/colegioconnect/audit/AuditControllerIntegrationTest.java`

**Interfaces:**
- Produces: `AuditPageResponse AuditQueryService.findRecent(int page, int size)` with `1 <= size <= 100` and `page >= 0`.
- Produces: `GET /api/admin/audit-events?page=0&size=20`.

- [ ] Write MockMvc tests for moderator success, non-moderator 403, suspended moderator 403, response data minimization, and invalid pagination 400.
- [ ] Run the focused test and confirm RED because the endpoint does not exist.
- [ ] Implement response records, bounded query service, controller, OpenAPI summary, and persisted policy annotation.
- [ ] Run the focused test and confirm GREEN.
- [ ] Commit as `feat(audit): expose protected audit history`.

### Task 3: Verification and evidence

**Files:**
- Create: `docs/audit-policy.md`
- Create: `docs/testing/2026-11-03-issue-15-audit-foundation.md`

**Interfaces:**
- Documents: transaction contract, taxonomy, privacy exclusions, API contract, dependency chain, commands, SHAs, and remaining integration work.

- [ ] Run focused audit tests.
- [ ] Run `mvn clean verify` and record exact counts.
- [ ] Inspect staged files, `git diff --check`, and scan changes for secret-like data.
- [ ] Commit as `docs(audit): record policy and verification evidence`.
- [ ] Push the branch and open a PR stacked on `gabriel/issue-9-authorization-foundation`; do not merge.
