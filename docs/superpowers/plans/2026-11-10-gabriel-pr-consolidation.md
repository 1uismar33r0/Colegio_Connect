# Gabriel PR Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task by task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Consolidate Gabriel's current authorization tests, audit, and company-decision work into PR #119 while retaining a separate CI PR and accurate contribution evidence.

**Architecture:** Keep Cristian's merged authorization and V1 role identifiers. Merge the test-only #118 lineage and the #120 lineage into Gabriel's main-based #119 branch without rewriting their commits; retain audit V4 and company-decision V5. Correct #107 documentation on its own branch. Keep independent infrastructure/recovery work separately reviewable.

**Tech Stack:** Java 25, Spring Boot 4.1.2, PostgreSQL 18.6 Testcontainers, Maven, Node 24, Docker Compose, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-11-07-issue-12-negative-authorization-design.md`, `docs/superpowers/specs/2026-11-08-issue-15-audit-reconciliation-design.md`, `docs/gabriel/security/issue-16-company-decision.md`, and the November 10 Gabriel PR consolidation request.

## Global Constraints

- Preserve all existing worktrees and local configuration; do not merge into `main`, force-push, close PRs, or alter teammate branches.
- Preserve #111 authorization, V1 role names, and #112's hidden-CSRF contract.
- Keep publishing, company registration, and `/api/auth/me` role-response changes with their owning team members.
- Record actual test revisions and do not substitute old standalone CI for combined verification.

## Review Focus

- Existing-session moderator revocation must deny audit and company-decision access.
- Company decision and its audit row must commit or roll back together under concurrent requests.
- V4 audit and V5 decision migrations must work on fresh and V1–V3 databases.
- Authentication harness cleanup must not violate audit-log foreign keys.
- Frontend role navigation must be reported as blocked while `/api/auth/me` omits roles.

### Task 1: Preserve the authorization harness on PR #119

**Files:** #118's three backend test files and linked Issue #12 evidence documents; shared evidence registers where merge conflicts occur.

**Interfaces:** Consume current `AuthorizationPolicy`; produce `NegativeAuthorizationTestHarness` used by existing auth and policy tests.

- [ ] Confirm #118 is based on current main and has no production authorization or migration changes.
- [ ] Merge #118 into #119 without rewriting commits; resolve only overlapping evidence records.
- [ ] Run `AuthIntegrationTest`, `AuthorizationPolicyIntegrationTest`, and audit tests with Java 25 and PostgreSQL Testcontainers.
- [ ] Inspect the resulting diff and commit the integration merge.

### Task 2: Bring company decisions and queue into PR #119

**Files:** #120's company controller/service/DTO, repository, V5 migration, tests, and Issue #16 evidence; #119's audit implementation and records.

**Interfaces:** Consume `AuditService.record` and `AuthorizationPolicy.canModerate`; produce the decision and pending-queue API contracts.

- [ ] Merge #120 ancestry into #119 without rewriting its commits or changing V4/V5 numbering.
- [ ] Review test isolation, 403 behavior, row locking, rollback, pagination, and evidence statements.
- [ ] Run focused auth, audit, company-decision, rollback, and queue tests.
- [ ] Run complete backend verification, frontend checks, Compose validation, backend image build, and migration checks where available.
- [ ] Correct consolidated documentation and record exact combined revision/results.
- [ ] Inspect `origin/main...HEAD`, then push only the verified #119 branch and inspect hosted CI and PR diff.

### Task 3: Finish the existing CI PR

**Files:** PR #107's `docs/gabriel/integration/team-dependencies.md`, `docs/project/status.md`, and affected CI evidence documents. No workflow change unless inspection shows a defect.

**Interfaces:** Preserve `push: {}`, `pull_request: {}`, and `workflow_dispatch: {}` with existing jobs and permissions.

- [ ] Correct hidden-CSRF/OpenAPI, email verification, #98/#111, responsibility, and name statements.
- [ ] Verify workflow triggers, permissions, pins, image conditions, documentation links, and diff against main.
- [ ] Commit and push Gabriel's #107 branch; confirm new hosted checks and request teammate review.
- [ ] Record #103 and #121 as redundant only after replacement functionality and evidence are verified.

### Task 4: Reconcile independent evidence and disposition

**Files:** Canonical `docs/gabriel/engineering-contributions.md` in #117; independent #104, #113, and #114 PRs.

- [ ] Inspect whether the local #98 disposition commit descends from #117 and contains unique content.
- [ ] Incorporate the verified unique contribution update into #117 without creating another index.
- [ ] Keep #104, #113, and #114 independent unless a shared acceptance criterion makes a safe combined review.
- [ ] Record the remaining role-response and publishing-policy decisions; prepare, but do not post, closure dispositions.

## Final Review

- [ ] Verify exact local and hosted results, final PR diffs, migration order, no duplicated teammate code, and clean tracked status.
- [ ] Request teammate review on the updated Gabriel PRs. Do not merge or close PRs.
