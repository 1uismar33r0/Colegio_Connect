# Issue #12 Negative-Authorization Reconciliation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver Gabriel's reusable negative-authorization test harness on the merged authorization baseline without changing production behavior or duplicating existing denial assertions.

**Architecture:** A test-only abstract harness centralizes synthetic persisted fixtures, cleanup, authentication principals, and MockMvc login/session helpers. Existing authentication and authorization-policy integration tests inherit it, retaining the authoritative assertions already merged from Massini's work.

**Tech Stack:** Java 25, Spring Boot 4.1.2, Spring Security Test, JUnit 5, MockMvc, PostgreSQL 18.6 Testcontainers, Maven Wrapper.

**Spec:** `docs/superpowers/specs/2026-11-07-issue-12-negative-authorization-design.md`

## Global Constraints

- Base all implementation on `main` SHA `e5cc181b3ebcf7b5cb1050547c1d82922068c064`.
- Preserve Massini's production authorization and Luis's frontend ownership; modify test and documentation files only.
- Use synthetic data and preserve foreign-key-safe cleanup.
- Do not merge, close, rebase, or rewrite existing PRs.
- Keep PR #117 content separate and record its index link as follow-up if still unmerged.
- Java commands must use Java 25; backend integration tests must use PostgreSQL 18.6 Testcontainers.

## Review Focus

- Suspended sessions must be denied by persisted status, not stale session authorities; owned by Task 1's existing-session test.
- Role revocation and cross-organization membership must fail closed from persisted assignments; owned by Task 1's policy tests.
- Unverified users must be rejected after correct credential validation; owned by Task 1's authentication test.
- Cleanup must delete role assignments and organizations before users; owned by Task 1's focused and full-suite runs.
- Future audit rows may reference users and require cleanup extension; documented by Task 2 without importing unmerged audit code.

---

### Task 1: Reconcile the reusable negative-authorization harness

**Files:**
- Create: `backend/src/test/java/com/colegioconnect/security/NegativeAuthorizationTestHarness.java`
- Modify: `backend/src/test/java/com/colegioconnect/accounts/AuthIntegrationTest.java`
- Modify: `backend/src/test/java/com/colegioconnect/security/AuthorizationPolicyIntegrationTest.java`

**Interfaces:**
- Consumes: current `User`, `Organization`, `RoleAssignment`, repositories, `AuthorizationPolicy`, `/api/auth/login`, and MockMvc contracts from main.
- Produces: protected `user`, `activeVerifiedUser`, `organization`, `assign`, `authenticationFor`, `login`, and `loginJson` helpers plus inherited cleanup.

- [x] **Step 1: Write the failing harness-consumer change**

Make both current integration-test classes extend `NegativeAuthorizationTestHarness` and replace one private fixture use in each with the intended protected API before the harness class exists.

- [x] **Step 2: Run focused tests to verify RED**

Run: `mvnw.cmd -Dtest=AuthIntegrationTest,AuthorizationPolicyIntegrationTest test`

Expected: test compilation fails because `NegativeAuthorizationTestHarness` and its helpers do not exist.

- [x] **Step 3: Implement the test-only harness and finish the fixture refactor**

Create `public abstract class NegativeAuthorizationTestHarness` with protected injected dependencies, `@BeforeEach` cleanup in foreign-key order, canonical-role fixtures, a UUID principal, and real CSRF-protected login helpers. Remove only the now-redundant private helpers and injections from the two consumer tests; preserve their assertions and test names.

- [x] **Step 4: Run focused tests to verify GREEN**

Run: `mvnw.cmd -Dtest=AuthIntegrationTest,AuthorizationPolicyIntegrationTest test`

Expected: 20 tests pass with zero failures/errors/skips.

- [x] **Step 5: Run the complete backend suite**

Run: `mvnw.cmd clean verify`

Expected: all backend tests pass; production source diff remains empty.

- [x] **Step 6: Commit the test infrastructure**

Stage only the three test files and commit with `test(authz): reconcile negative authorization harness`.

### Task 2: Preserve engineering and compatibility evidence

**Files:**
- Create: `docs/gabriel/security/issue-12-negative-authorization.md`
- Modify: `docs/project/traceability.md`
- Modify: `docs/testing/test-evidence.md`
- Modify: `docs/progress/development-log.md`
- Modify: `docs/reports/midterm/evidence-index.md`
- Create: `docs/superpowers/specs/2026-11-07-issue-12-negative-authorization-design.md`
- Create: `docs/superpowers/plans/2026-11-07-issue-12-negative-authorization.md`

**Interfaces:**
- Consumes: exact GitHub metadata, original PR #100 commits, selected main baseline, Task 1 code, and fresh verification output.
- Produces: contributor instructions, Capstone-ready implementation walkthroughs, test evidence, compatibility recheck list, and traceability links.

- [x] **Step 1: Write the Gabriel topic document from verified code and Git evidence**

Include purpose, exact authorship, original-versus-current classification, real excerpts, inputs/outputs/interactions, representative test walkthroughs, engineering decisions, GitHub evidence, limitations, and a section titled `Changes to Recheck When Other PRs Merge`.

- [x] **Step 2: Update affected project evidence records**

Link the scoped topic document from Issue #12 traceability, add a dated test-evidence record using a non-conflicting evidence ID, add a concise development-log entry, and add one midterm evidence-index reference. Do not edit PR #117's unmerged contribution index.

- [x] **Step 3: Validate documentation**

Run reference/link searches, `git diff --check`, inspect every claim against source/commit/test output, and confirm all documentation is English.

- [x] **Step 4: Commit documentation**

Stage only the intended documentation files and commit with `docs(gabriel): preserve Issue 12 security evidence`.

### Task 3: Review and publish the focused change

**Files:**
- Review all files changed by Tasks 1 and 2; create no new application files.

**Interfaces:**
- Consumes: clean commits, full test output, and the original request.
- Produces: a reviewable GitHub PR and final evidence report.

- [x] **Step 1: Perform a requirement and diff review**

Confirm no production code, migration, frontend, PR #117 document, secret, generated target output, or unrelated file is included. Reconcile every Issue #12 criterion and list remaining future endpoint coverage separately from current acceptance.

- [x] **Step 2: Run final verification**

Run Java 25 focused tests, Java 25 `clean verify`, documentation checks, `git status`, and `git diff origin/main...HEAD` fresh at the final SHA.

- [x] **Step 3: Push and open the PR**

Push `gabriel/issue-12-negative-auth-reconciliation`, open a documentation-and-test-only PR targeting `main`, link Issue #12 and historical PR #100, explain that no production authorization changed, and request another team member's security review.

- [x] **Step 4: Verify the live PR**

Inspect the actual GitHub file list and commit SHA, wait for CI conclusions, and report review/merge state without merging or closing any existing PR.
