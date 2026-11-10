# Issue #12 Negative-Authorization Reconciliation Design

## Intent

Complete the reusable negative-authorization integration-test harness assigned to Gabriel without importing the superseded production authorization implementation from PR #98. The deliverable must preserve Gabriel's original PR #100 contribution, run against Massini's merged authorization model, avoid duplicate denial tests, and provide presentation-ready engineering evidence.

## Requirements and evidence baseline

- Canonical Issue #12 requires a reusable Testcontainers plus MockMvc/REST integration-test base, role/status/organization fixtures, initial anonymous/wrong-role/suspended/unverified cases, CI execution, use by at least three cases, and contributor instructions.
- Alias #47 remains a historical traceability reference, but the live GitHub query on 2026-11-07 returned no issue or pull request for that number.
- Approved proposal SHA-256: `88038171ef23ffbd42bcbe38cd0ec5177240e42ab010fb6fbed96107b1b8f98c`; 30 pages. Objective O2 and indicator I6 require server-side authorization and at least ten negative-authorization cases over the project lifetime.
- Latest authoritative baseline: `main` at `e5cc181b3ebcf7b5cb1050547c1d82922068c064`, containing merged PRs #111 and #112.
- Original PR #100 head: `52352763d0f01e1b8bb9f87e1ec843bb357c2c6b`, stacked on superseded PR #98 head `a4d7afd741a4ae6024241c3e7caad37b8da21c96`.

## Goals

- Recover Gabriel's reusable persisted-fixture design and contributor workflow.
- Use the canonical merged roles (`STUDENT`, `INSTITUTIONAL`, `MODERATOR`, `COMPANY`) and Massini's `AuthorizationPolicy` methods.
- Make existing authoritative tests consume the harness instead of copying their assertions into a competing test class.
- Preserve exact GitHub, authorship, compatibility, and test evidence in a separate Gabriel security document.

## Non-goals and anti-goals

- No production authorization, API, database migration, frontend, or OpenAPI changes.
- No cherry-pick, merge, rebase, force-push, closure, or rewrite of PRs #98-#102 or teammates' PRs.
- No speculative tests for listing, sponsorship, ownership, consent, moderation, or audit endpoints that do not exist on the selected baseline.
- No claim that Issue #12, PR #100, or the new reconciliation PR is accepted or merged before team review and integration.

## Approaches considered

### 1. Cherry-pick PR #100

Rejected. Its test class references obsolete role identifiers and an authorization method that is absent from the merged model. Its parent also carries Gabriel's superseded production authorization and migration work from PR #98.

### 2. Add PR #100's standalone test class unchanged in spirit

Rejected. Main already tests anonymous access, unverified login, suspended login/session behavior, cross-organization roles, pending organizations, and incorrect roles. Repeating those assertions would duplicate Massini's authoritative tests.

### 3. Main-based fixture reconciliation (selected)

Add a test-only abstract harness adapted to current entities and policies. Refactor the existing `AuthIntegrationTest` and `AuthorizationPolicyIntegrationTest` to inherit the fixtures and request helpers while retaining their existing assertions and ownership. This satisfies harness reuse without changing production behavior or duplicating cases.

## Design

`NegativeAuthorizationTestHarness` is a public abstract test class under `com.colegioconnect.security`. It owns dependency injection, foreign-key-safe cleanup, synthetic user and organization creation, role assignment, principal construction, and real `/api/auth/login` session helpers. Protected methods allow test classes in the accounts and security packages to reuse it.

`AuthIntegrationTest` keeps its endpoint assertions but replaces private repository/setup/request helpers with the harness. `AuthorizationPolicyIntegrationTest` keeps its policy assertions but replaces private persisted fixtures with the harness. No application class changes.

The Gabriel topic document is independent of PR #117's unmerged `docs/gabriel/engineering-contributions.md`. It records original and reconciled code separately and leaves the contribution-index link as a follow-up if PR #117 has not merged.

## Verification

- RED: compile the two tests after declaring the missing harness dependency and confirm failure is specifically the missing type/helpers.
- GREEN: run `AuthIntegrationTest` and `AuthorizationPolicyIntegrationTest` with Java 25 and PostgreSQL 18.6 Testcontainers.
- Run the complete backend `mvnw.cmd clean verify` suite.
- Run documentation link/reference/whitespace checks and inspect the complete diff against `origin/main`.
- Push a focused branch, open a main-targeted PR, inspect its live diff, request teammate review, and record actual CI conclusions.

## Future compatibility

- PR #115 may change registration notification fixtures but does not currently alter login or authorization contracts.
- PR #116 is frontend-only apart from evidence documents and does not currently alter backend security contracts.
- PR #99's provisional audit migration references users; if audit-producing denial cases later extend this harness, cleanup must remove audit rows before users.
- Future listing, organization-approval operations, moderation, and sponsorship endpoints should add distinct endpoint-level denial cases to this harness until the proposal's I6 threshold reaches at least ten supported cases.
