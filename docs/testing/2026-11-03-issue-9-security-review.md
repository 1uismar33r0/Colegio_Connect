# Issue #9 security review evidence - 2026-11-03

## Revisions reviewed

- PR #98 head before hardening: `610c4f522321c1804ddbacd001f008fcf9d987e9`
- PR #95 / login base: `081722692cbafe357ad82b98c8d767725627f046`
- PR #96 browser-authentication tests: `5b23b3c2667abfe012d2421ecbc724bf3c4229da`
- Java: Temurin 25.0.4.1
- Database: PostgreSQL 18.6 through Testcontainers

## Security checks

Automated tests verify that:

- Login authorities contain exactly the union of persisted roles.
- Removing a persisted role immediately denies its policy permission even when
  the existing `Authentication` object remains authenticated.
- Suspending an account immediately denies ownership, organization membership,
  publishing, moderation, and role-assignment policy checks.
- External-company publishing requires the assigned organization, the
  `EXTERNAL_COMPANY` role, and `APPROVED` status.
- A faculty role cannot publish for an external-company organization.
- An assignment for one organization does not authorize another organization.
- Unknown user and organization records fail closed.
- Only a persisted `ADMIN_MODERATOR` role passes moderation and future
  role-assignment checks.
- Method-security denial on `/api/auth/me` returns HTTP 403 rather than leaking
  an internal error.

No publishing, moderation, or role-management endpoint exists yet. These policy
tests exercise the real persistence and policy services without adding fake
production endpoints. Future endpoints must apply the shared policy at their
service boundary; login-time authorities alone are not sufficient for immediate
revocation.

## Migration evidence

`RoleMigrationIntegrationTest` creates an isolated schema at Flyway V3, inserts
all four legacy role values, runs V4, and verifies that assignment UUIDs, user
links, organization links, row count, and mapped role meaning are preserved.
It also exercises the V4 constraint against PostgreSQL 18.6.

## Commands and results

From `backend/` on PR #98 plus the new review tests:

```text
mvn clean verify
Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Before the review-test commit, an isolated worktree combined PR #98 at
`610c4f5` with PR #96's test commit and ran:

```text
mvn verify
Tests run: 30, Failures: 0, Errors: 0, Skipped: 0
```

A final combined run is required after the review commits are layered into the
temporary integration worktree.

## GitHub evidence and remaining acceptance

At review time PR #98 was open, mergeable, and had no reviews, review requests,
comments, or check runs. The repository CI workflow listens only to pull
requests targeting `main`; PR #98 targets the unmerged login branch, so no CI
run was created. Local verification does not satisfy Issue #9's CI or
independent-review Definition of Done.
