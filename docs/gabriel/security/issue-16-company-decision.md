# Issue #16 — company approval decision backend

## Scope and dependencies

Gabriel (`DCGabriel10`) implements the admin approval/rejection slice of canonical [Issue #16](https://github.com/1uismar33r0/Colegio_Connect/issues/16) (alias [#55](https://github.com/1uismar33r0/Colegio_Connect/issues/55)). The approved proposal assigns company approval to administrators and indicator I8 requires an audit record with actor, action, target, and time. [Issue #15's audit foundation](https://github.com/1uismar33r0/Colegio_Connect/pull/119) supplies that write contract; this record covers only its first real production caller.

The selected base is local `4f7b526bb2988d5eac164fcd533e45814f0ac690`, whose parent history contains the Issue #15 implementation (`132413bb8076371285cdee5a7bd18f6a92871278` and `c0b01a7afc6c8b61eb4ec1cc44259c78658378d8`) on `main` `e5cc181b3ebcf7b5cb1050547c1d82922068c064`. The remote PR #119 branch was still `26d3d108c1d138d3d3c7ee912966256e2f6a0ffc` when checked on 2026-11-09; this local follow-up and Issue #16 code are unmerged. Massini's authorization model is already on the selected `main`. No Issue #10 role-management operation was imported. GitHub API access returned 404, so live issue comments, reviews, and CI were unavailable; the issue text and proposal mapping were taken from the repository's traceability register and approved PDF.

## Gabriel's backend contribution

Commit `d43afff1fc2660b8cc5191401c669caaa6084eaf` adds `CompanyDecisionController.decide`, `CompanyDecisionService.decide`, and `CompanyDecisionRequest`. `POST /api/admin/organizations/{id}/decision` accepts `APPROVED` or `REJECTED`; rejection requires a nonblank reason of at most 500 characters, and an optional approval reason is also bounded. A V5 migration adds `organizations.decision_reason`; the reason stays out of the minimal audit row. `OrganizationRepository.findByIdForUpdate` locks the target row. Only a pending `EXTERNAL_COMPANY` can transition; missing IDs return 404, other organization types or invalid input return 400, and any repeated or conflicting decision returns 409.

A request passes Spring session authentication and CSRF, then `@authz.canModerate(authentication)` rechecks the actor's active account and persisted global `MODERATOR` role. The controller derives the actor UUID from that server-side authentication. The service locks the company, changes its status and reason, and calls `AuditService.record` with `ORGANIZATION_APPROVED` or `ORGANIZATION_REJECTED`, target type `ORGANIZATION`, and the actual organization ID. Both changes share the service's transaction; the audit service generates the event time. If the audit call throws, the company state rolls back too. Concurrent requests serialize on the row lock, leaving one decision and one audit row.

The implementation uses Gabriel's Issue #15 audit service and Massini's merged authorization policy. Test fixtures create pending companies and moderator accounts; they are not production registration or role-management workflows.

## Verification and limits

On 2026-11-09, Windows, Temurin Java 25.0.5+8, Docker Desktop 29.5.2, and PostgreSQL 18.6 through Testcontainers, the first focused test failed 1/1 with HTTP 404 before the endpoint existed. From `backend`, `.\mvnw.cmd -Dtest=CompanyDecisionIntegrationTest,CompanyDecisionRollbackIntegrationTest test` then passed 11/11; `.\mvnw.cmd clean verify` passed 52/52 with zero failures, errors, or skips on code tree `735de74b50a5cab42d50ed2086994850a6c0d798` (commit `d43afff`). The ten ordinary integration cases cover approval/rejection state and audit identity/action/target/time, persisted reason, anonymous and non-moderator denial, CSRF, missing and non-company targets, invalid input, repeat/conflict handling, same-session role revocation, concurrent decisions, and OpenAPI discovery. The eleventh test replaces only `AuditService` with a throwing test bean to verify that an audit-write failure rolls back the company change. State and audit rows are read after the request in separate repository transactions; the test class is not transaction-wrapped.

The local Maven log and nine Surefire XML reports were SHA-256 verified in non-synced `C:\Users\hrval\AppData\Local\Colegio_Connect_Evidence\2026-11-09-issue16-company-decision`. The raw log contains synthetic test inputs and was checked for common secret patterns before local retention. This is a local run, not hosted CI or teammate review.

Issue #16 AC2 (decision audited) is implemented and locally verified for this backend operation, but is not accepted until review and integration. AC1 (approved company can publish; pending/rejected cannot) remains open because company registration, the admin queue and frontend form, publishing, and decision notification are not implemented here. Issue #15 and proposal indicator I8 remain open for role changes, account restrictions, moderation actions, sponsorship acceptances, and their owning workflows.

## Changes to Recheck When Other PRs Merge

| Dependency | Recheck |
| --- | --- |
| PR #119 audit foundation | Retain `AuditService.record` in the same decision transaction, migration ordering V4 then V5, and the audit read policy. |
| Issue #10 role management | Keep moderator grants/revocations in Cristian's owning workflow; verify decision access after role changes with the same session. |
| Company registration and Luis's screens | Use the `EXTERNAL_COMPANY` and `PENDING` contract, show the stored rejection reason only to authorized viewers, and preserve CSRF and Problem Detail handling. |
| Listing publishing | Enforce company membership and `APPROVED` in the publishing service, with negative tests for pending and rejected companies before claiming AC1. |
| Notifications | Send decision notices after commit; an email delivery failure must not create a false audit decision or undo a committed choice without an agreed policy. |
