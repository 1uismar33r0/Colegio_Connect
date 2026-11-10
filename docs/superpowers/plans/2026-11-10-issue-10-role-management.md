# Issue #10 Backend Role Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a secure, tested backend API for current-role retrieval, administrator search, role assignment/revocation, audit, and one-time bootstrap.

**Architecture:** Extend the existing authentication response and persisted authorization policy. Add one role administration package with bounded DTOs and transaction services. Use V1 uniqueness and a V6 singleton lock/bootstrap row; reuse V4 audit.

**Tech Stack:** Java 25, Spring Boot 4.1.2, Spring Security, Spring Data JPA, Flyway, PostgreSQL 18.6 Testcontainers, JUnit/MockMvc.

**Spec:** [Issue #10 design](../specs/2026-11-10-issue-10-role-management-design.md).

## Global Constraints

- Backend implementation only; do not edit frontend sources or teammate branches.
- Base is `origin/main` `5ff9974`, including #119's audit V4 and company V5.
- Preserve V1 role identifiers, CSRF, server sessions, and persisted `@authz` checks.
- Synthetic data only; no developer database mutation or populated environment file in Git.

## Review Focus

- A moderator revoked after login must lose the next administrative request.
- Two simultaneous moderator removals must not leave zero eligible administrators.
- Duplicate global assignments must fail even when requests race.
- An audit-write failure must roll back the role mutation.
- Startup after deliberate moderator revocation must not restore access.

## Task 1: Current roles in authentication

**Files:** `UserResponse`, `AuthController`, `LoginService`, authentication integration tests.

- [ ] Add failing tests for no roles, distinct scoped roles, and changed assignments visible through the same session.
- [ ] Run focused tests and confirm the expected failure.
- [ ] Implement database-backed role response for login and `/me`.
- [ ] Run focused authentication and authorization tests; commit.

## Task 2: Durable role administration and search

**Files:** V6 migration, user/assignment repositories, `roles` controller/service/DTOs, PostgreSQL/MockMvc tests.

- [ ] Add failing tests for bounded minimal search, access control, scope/type validation, 204/404/409, audit, rollback, concurrency, and last-admin protection.
- [ ] Run focused tests and confirm the expected failures.
- [ ] Implement V6 state/index and current persisted administrative endpoints using the existing audit service.
- [ ] Run focused and migration-upgrade tests; commit.

## Task 3: One-time bootstrap

**Files:** bootstrap runner/service, state persistence, integration tests.

- [ ] Add failing tests for disabled, eligible, ineligible, repeated, concurrent, and revoked-after-bootstrap cases.
- [ ] Run focused tests and confirm the expected failures.
- [ ] Implement startup bootstrap with an existing verified active UPRM account and durable completion marker.
- [ ] Run bootstrap, audit, and migration tests; commit.

## Task 4: Documentation and full review

**Files:** ADR 0002, policy matrix, environment, traceability, development log, test evidence, midterm index, API docs/annotations.

- [ ] Record the exact API contract, privacy boundary, bootstrap semantics, frontend regeneration command, and actual test evidence.
- [ ] Run `backend/.\mvnw.cmd verify`, clean and V1–V3 upgrade migration checks, OpenAPI checks, and relevant smoke tests without touching development data.
- [ ] Inspect backend-only diff, commit, push, create one PR, request Cristian's review, and record hosted CI.
