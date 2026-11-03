# Issue #15 Audit Foundation Verification — 2026-11-03

## Revision and environment

- Branch: `gabriel/issue-15-audit-logging`
- Stack base: PR #98 head `a4d7afd`
- Design commit: `76275c6`
- Transaction foundation commit: `76761ed`
- Protected query commit: `2311ae3`
- Test-isolation correction: `ea81705`
- Java: `25.0.4.1`
- Spring Boot: `4.1.2`
- Database: PostgreSQL `18.6` through Testcontainers

## TDD evidence

`AuditServiceIntegrationTest` was first run before production audit classes
existed and failed compilation. After V5 and the command service were added:

```text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

`AuditControllerIntegrationTest` was first run before the endpoint existed and
all four initial requests returned 404. After the query API and further
negative cases were added:

```text
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
```

Verified behavior includes committed persistence, rollback removal, mandatory
transaction enforcement, moderator-only access, anonymous denial, immediate
suspension and role-revocation denial for an existing session, newest-first
pagination, input bounds, and response data minimization.

## Full regression verification

Command executed from `backend` with Java 25:

```powershell
mvn clean verify
```

The first integrated run found a fixture-isolation defect: audit tests left an
actor-referenced row, while the later authorization test class deleted users
without deleting audit fixtures. The production foreign key was preserved.
`AuthorizationIntegrationTest.cleanDatabase` now removes audit fixtures first.
The exact ordered reproduction then passed 18/18 tests.

The clean full rerun completed with:

```text
SUREFIRE tests=41 failures=0 errors=0 skipped=0
MVN_EXIT=0
```

This is local execution evidence. The stacked branch had not yet been pushed or
checked by GitHub Actions when this document was written.

## Remaining acceptance work

No role-change, organization-approval, listing-moderation, account-suspension,
or sponsorship mutation service exists in this stack. Their owning issues must
integrate `AuditService` in the same business transaction and add success plus
rollback tests. Therefore this PR makes the audit foundation reviewable but
does not, by itself, prove every future administrative action is audited.
