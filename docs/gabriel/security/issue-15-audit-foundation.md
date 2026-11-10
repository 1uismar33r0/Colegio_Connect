# Issue #15 - Audit Log Foundation

## Purpose and approved requirement

Issue [#15](https://github.com/1uismar33r0/Colegio_Connect/issues/15) assigns Gabriel the audit foundation used by administrative and sponsorship actions. The approved proposal's objective O3 and indicator I8 require approvals, role changes, account restrictions, moderation actions, and sponsorship acceptances to record actor, action, target, and time. The inspected source is the immutable 30-page proposal with SHA-256 `88038171ef23ffbd42bcbe38cd0ec5177240e42ab010fb6fbed96107b1b8f98c`.

This branch provides the reusable persistence and moderator-read foundation. It does not claim final I8 acceptance because the business actions that must emit these events are owned by later issues and do not exist on the selected baseline.

## Authorship and original PR #99 history

Gabriel authored open PR [#99](https://github.com/1uismar33r0/Colegio_Connect/pull/99), `gabriel/issue-15-audit-logging`, as five consecutive commits:

| Commit | Gabriel-authored contribution |
| --- | --- |
| `76275c6441b9f03871122f7bfd419a5a542a993f` | Defined the original audit-foundation design. |
| `76761ed05d949c13b6af4e4722667ea704d4f073` | Added the constrained audit schema, entity, repository, mandatory-transaction service, and transaction tests. |
| `2311ae33cb2ec2e8144124eaeb0122b70a3bb5cd` | Added bounded pagination, moderator-only reading, response minimization, and endpoint tests. |
| `ea817050aea8c85e42599a390066ea7dbbe24779` | Corrected audit-row cleanup before deleting referenced users in authorization tests. |
| `2e57200f37a461f9f74fcf79b26c56fab1060770` | Recorded the original policy and local verification evidence. This remains PR #99's head. |

PR #99 is based directly on PR #98 head `a4d7afd741a4ae6024241c3e7caad37b8da21c96`, not current `main`. That dependency supplied an obsolete V4 migration, `ADMIN_MODERATOR`, and `authorizationPolicy.canModerate`. PR #99 has no direct check run or teammate review. Its head was included in CI-only PR #101 at `769e61668bf9278b4d8e96ce3c0eeb17be77c54b`, where Backend, Container, and CI Gate passed; that historical combined validation does not make PR #99 merged or compatible with current production authorization.

## Baseline and reconciliation decision

The authoritative baseline is `main` `e5cc181b3ebcf7b5cb1050547c1d82922068c064`, containing Massini's merged PR #111 authorization and PR #112 CSRF client correction. Current `main` uses V1 roles `STUDENT`, `INSTITUTIONAL`, `MODERATOR`, and `COMPANY`, with Flyway migrations through V3.

The selected approach semantically recovers Gabriel's audit design instead of cherry-picking the obsolete stack:

| Original PR #99 element | Classification | Main-based decision |
| --- | --- | --- |
| Minimal actor/action/target/time schema | Compatible | Retained, renumbered from V5 to V4 because current `main` ends at V3. |
| Controlled actions and target types | Compatible | Retained; no free-form details column was introduced. |
| `Propagation.MANDATORY` command service | Compatible | Retained so the audit insert joins the owning business transaction. |
| Paginated newest-first query | Compatible | Retained with page size bounded to 100. |
| `ADMIN_MODERATOR` and PR #98 policy | Incompatible | Replaced with Massini's current `MODERATOR` and `@authz` bean. |
| Immediate role-revocation behavior | Requires a current contract | Defined only for audit-history reading through a persisted global-moderator check. No general session-authority rule was invented. |
| Modification of an older authorization test cleanup | Obsolete test location | Replaced by before/after cleanup in the audit tests so unrelated current tests remain untouched. |
| Future business-service calls | Not yet implementable | Left to Issues #10, #16, #19, #37, #39, and #52. |

Reconciled commits:

| Commit | Contribution |
| --- | --- |
| `8363dc427c54a8dec40b6601edcd16e65d1454f7` | Current-main reconciliation design and plan. |
| `132413bb8076371285cdee5a7bd18f6a92871278` | V4 schema, event model, repository, mandatory-transaction service, and transaction tests. |
| `c0b01a7afc6c8b61eb4ec1cc44259c78658378d8` | Bounded read endpoint, persisted moderator policy, same-session revocation coverage, OpenAPI coverage, and correct 403 mapping for method security. |
| `669d3bb317cba9d900a5fc567c785a42fa701dbf` | Scoped Gabriel engineering record and linked project evidence; first published PR #119 head. |

## Real implementation excerpts

### Same-transaction persistence

```java
@Transactional(propagation = Propagation.MANDATORY)
public void record(UUID actorId, AuditAction action, AuditTargetType targetType, UUID targetId) {
    auditEventRepository.save(new AuditEvent(
            actorId, action, targetType, targetId, Instant.now()));
}
```

`MANDATORY` rejects calls without an existing transaction. A future mutation service must authorize the actor, change business state, and call `record` in one transaction. A rollback removes both the mutation and event; a separate best-effort audit transaction is deliberately not used.

### Data-minimized schema

```sql
CREATE TABLE audit_log (
    id          UUID         PRIMARY KEY DEFAULT uuidv7(),
    actor_id    UUID         NOT NULL REFERENCES users (id),
    action      VARCHAR(40)  NOT NULL,
    target_type VARCHAR(30)  NOT NULL,
    target_id   UUID         NOT NULL,
    occurred_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_audit_log_action CHECK (action IN (...)),
    CONSTRAINT ck_audit_log_target_type CHECK (target_type IN (...))
);
```

The row contains only identifiers, controlled enum strings, and server time. It has no email, password/hash, verification code, cookie, CSRF token, session ID, request body/header, or arbitrary details field. The actor foreign key preserves identity integrity; it also means a future physical user-deletion feature must first define retention or anonymization.

### Persisted moderator permission

```java
public boolean canModerate(Authentication authentication) {
    if (!isActive(authentication)) {
        return false;
    }
    UUID userId = UUID.fromString(authentication.getName());
    return roleAssignmentRepository.existsByUserIdAndRoleAndOrganizationIsNull(
            userId, Role.MODERATOR);
}
```

The login session still carries a snapshot of `ROLE_*` authorities. Audit-history access intentionally does not trust that snapshot: it checks that the account remains active and that a global `MODERATOR` row still exists. Organization-scoped role rows do not satisfy the query.

### Protected bounded read API

```java
@GetMapping
@PreAuthorize("@authz.canModerate(authentication)")
public AuditPageResponse findRecent(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    return auditQueryService.findRecent(page, size);
}
```

`GET /api/admin/audit-events` returns newest events first. Negative pages, zero sizes, and sizes above 100 produce HTTP 400. Anonymous callers receive 401; authenticated callers without current persisted permission receive 403. Springdoc generates the OpenAPI operation from this controller, and the integration test confirms it is present.

### Exact same-session role-revocation test

```java
@Test
void revokingModeratorRoleImmediatelyRevokesAuditAccessForExistingSession() throws Exception {
    User moderator = createUser("audit.revoked@upr.edu");
    RoleAssignment assignment = roleAssignmentRepository.save(
            new RoleAssignment(moderator, Role.MODERATOR, null));
    MockHttpSession session = login(moderator);

    roleAssignmentRepository.delete(assignment);
    roleAssignmentRepository.flush();

    mockMvc.perform(get("/api/admin/audit-events").session(session))
            .andExpect(status().isForbidden());
}
```

This is the exact grant-authenticate-revoke-recheck sequence. It uses the real CSRF-protected login endpoint and reuses its real session. The test would fail if the endpoint were changed to rely only on the stale `ROLE_MODERATOR` authority stored at login.

## Security, privacy, and persistence findings

- Read permissions: current active users with a persisted global `MODERATOR` assignment only.
- Sensitive fields: excluded structurally; API assertions also scan the response for synthetic email and sensitive field names.
- Transaction behavior: committed event persists; containing rollback leaves zero rows; an out-of-transaction call fails and leaves zero rows.
- Ordering and bounds: events are newest-first; page size is capped at 100.
- Method-security errors: the existing catch-all `GlobalExceptionHandler` initially converted `AuthorizationDeniedException` to HTTP 500. The reconciliation adds a narrow `AccessDeniedException` mapping to 403 while retaining generic 500 redaction.
- Append-only limitation: entity fields are non-updatable and no application delete operation is exposed, but PostgreSQL does not yet use a trigger or restricted database role to forbid direct updates/deletes. This branch does not overstate database-enforced immutability.
- Actor lifecycle limitation: the non-null foreign key prevents physical deletion of referenced users until a retention/anonymization policy exists.

## Verification evidence

Observed 2026-11-08 in `America/La_Paz` on Windows with Temurin Java `25.0.5+8`, Spring Boot `4.1.2`, Docker Desktop `29.5.2`, and PostgreSQL `18.6` through Testcontainers.

| Stage | Command / observation | Actual result |
| --- | --- | --- |
| Baseline | `mvnw.cmd clean verify` at main `e5cc181b...` | 30 tests, zero failures/errors/skips; `BUILD SUCCESS`. |
| Transaction RED | `mvnw.cmd -Dtest=AuditServiceIntegrationTest test` before production audit types | Compilation failed on missing `AuditService`, repository, action, and target types. |
| Transaction GREEN | Same focused command after `132413b...` | 3 tests passed. |
| Endpoint RED | `mvnw.cmd -Dtest=AuditControllerIntegrationTest test` before query/controller/policy | 6 of 8 cases failed on absent route/OpenAPI; anonymous and global account-suspension protections already passed. |
| First endpoint GREEN attempt | Both audit classes | 9 audit cases passed; two permission-denial cases returned 500 instead of expected 403. Root cause was the generic error handler catching `AuthorizationDeniedException`. |
| Focused final | `mvnw.cmd -Dtest=AuditServiceIntegrationTest,AuditControllerIntegrationTest,GlobalExceptionHandlerTest test` | 13 tests passed, zero failures/errors/skips. |
| Full final before documentation | `mvnw.cmd clean verify` | 41 tests passed, zero failures/errors/skips; `BUILD SUCCESS`. |

The 11 audit cases cover committed persistence, rollback/no row, mandatory transaction enforcement, moderator reading and response minimization, non-moderator denial, anonymous denial, newest-first pagination, existing-session suspension, existing-session role revocation, invalid pagination, and OpenAPI discovery.

### November 9 local compatibility run with PR #118

On 2026-11-09, a disposable worktree combined current `main` `e5cc181b3ebcf7b5cb1050547c1d82922068c064` with the implementation commits `92fadc467db5a85dcea5c03fc3456e6d38936999` (#118), `132413bb8076371285cdee5a7bd18f6a92871278` and `c0b01a7afc6c8b61eb4ec1cc44259c78658378d8` (#119). The tested code-only tree was `0f5f303cf8d75ca867799169e56abac445133dff`; PR documentation commits were not part of that tree. On Windows with Temurin Java 25.0.5+8, Docker Desktop 29.5.2, and PostgreSQL 18.6 Testcontainers, `cd backend` followed by `.\mvnw.cmd clean verify` completed with 41 tests passed, zero failures, zero errors, and zero skipped. `AuthIntegrationTest` ran 12, `AuthorizationPolicyIntegrationTest` 8, `AuditControllerIntegrationTest` 8, and `AuditServiceIntegrationTest` 3. The audit controller's `revokingModeratorRoleImmediatelyRevokesAuditAccessForExistingSession` case ran and passed: it logs in a moderator, deletes the persisted global moderator assignment, then gets HTTP 403 on the next audit-history request using that same session. It does not prove system-wide role revocation for other endpoints or business-event emission. The Maven log and four Surefire XML reports were SHA-256 verified in local, non-synced `C:\Users\hrval\AppData\Local\Colegio_Connect_Evidence\2026-11-09-pr118-pr119`. This local run is not hosted CI, team review, or acceptance; the published PR heads remain unchanged.

## PR #118 role-revocation gap

PR #118 head `ec2a42ed526f654729faeb5c97f71c8bd9519791` has no test that grants a role, authenticates, revokes the role, and rechecks access with the same session or principal. On its `main` baseline, roles are copied into session authorities at login and only account activity is rechecked globally; no protected role-management endpoint defines whether all existing sessions must immediately lose a revoked role. Therefore PR #118 correctly did not invent a general test expectation.

Issue #15 supplies a narrower stable contract: audit-history reading must check persisted moderator state on each request. The exact test above belongs here, not in the generic Issue #12 harness, until the team defines a system-wide revocation/session contract.

## GitHub and branch state

| Item | State at observation |
| --- | --- |
| Issue #15 | Open, unassigned, no comments; acceptance and Definition of Done remain pending. |
| Original PR #99 | Open at `2e57200f...`, stacked on PR #98, no formal review or direct checks. |
| PR #98 dependency | Open at `a4d7afd...`; superseded production authorization and not imported. |
| Current reconciliation | [PR #119](https://github.com/1uismar33r0/Colegio_Connect/pull/119), `gabriel/issue-15-audit-reconciliation`, based directly on main `e5cc181b...`; open and not merged. |
| Hosted CI | Run `42447499949` at first published head `669d3bb317cba9d900a5fc567c785a42fa701dbf`: Backend, Frontend, Container, and CI Gate passed. This status update is documentation-only and must retain green checks. |
| Review requirement | Massini (`Massinl`) is requested on PR #119; no teammate review had been submitted at this observation. Automated/self-review is supplementary only. |

## Changes to Recheck When Other PRs Merge

| Dependency and inspected head | Contract it could change | Inspection / compatibility evidence | Required follow-up |
| --- | --- | --- | --- |
| PR #98, `a4d7afd741a4ae6024241c3e7caad37b8da21c96` | Obsolete role names, authorization helpers, and V4 migration | Full source dependency inspected; PR #99 stack was historically tested only through CI-only PR #101. | Do not merge its production authorization into this branch. Preserve authorship only. |
| PR #99, `2e57200f37a461f9f74fcf79b26c56fab1060770` | Entire original audit schema/service/query/test contract | All five commits and all changed files inspected in source; compatible behavior was reimplemented and tested on current main in this isolated worktree. | Keep open for team disposition; do not merge both implementations. |
| PR #115, `85e04feee0330523e3c8d2765918e6deded49a39` | After-commit notification delivery, logging, and registration fixtures | Corrected test-only combination with #118/#119 passed 52/52 on code tree `77966809749f1f636b22edf59ad73d4b4a0b055e`; no `audit_log` overlap. | Coordinate the isolated assertion correction `258e28f` with Luis; do not convert email delivery failures into administrative audit events without an approved policy. |
| PR #118, initial reviewed head `ec2a42ed526f654729faeb5c97f71c8bd9519791`; November 9 tip `bcfe5ff512754f5e7a55263f34c84c58ab2756ee` | Negative-authorization harness cleanup and future endpoint test fixtures | The November 9 code-only combination of its implementation commit with #119 passed 41/41 backend tests at tree `0f5f303cf8d75ca867799169e56abac445133dff`; each audit test removes audit rows after itself. | After both settle, consider reusing its test harness and ensure audit rows are deleted before users in any shared cleanup. |
| PR #117, `e9a3951844e8081342861a37a90739c7674fb2c9` | Gabriel's consolidated contribution index | Documentation source inspected; this record does not overwrite or duplicate it. | Add one link after both documentation branches reach final disposition. |
| Issue #10 role management | `ROLE_ASSIGNED` and `ROLE_REVOKED` event emission | No stable endpoint/service exists on baseline. | Add same-transaction success and failed-action/no-row tests in Issue #10. |
| Issue #16 organization decisions | Approval/rejection events and notification coordination | [Gabriel's local decision backend](issue-16-company-decision.md) calls this audit service in the same transaction; 11 focused and 52 full backend tests passed on tree `735de74b50a5cab42d50ed2086994850a6c0d798`. | Recheck after PR #119 merges; decision notification, company registration, and publishing remain separate. |
| Issues #19/#37/#39 moderation | Listing state, reports, hiding/removal, restrictions | No stable models or services exist. | Add every approved action to the I8 matrix and test rollback/no-row behavior. |
| Issue #52 sponsorship acceptance | Acceptance/contact release, consent, and audit | Not implemented; repository policy requires pair evidence and another security reviewer. | Integrate audit only during the paired owning issue; never release contact data before accepted state. |

## November 9 compatibility check with Luis's notification fix

Luis's PR #115 moved from `3eb0a11974d23fe06ffe16b650d3069e7450e844` to `85e04feee0330523e3c8d2765918e6deded49a39` (`fix(notifications): address PR #115 review`). Registration now publishes a verification-code event, and an after-commit listener sends the email. The registration endpoint still returns a bodyless HTTP 201. This is a separate notification change; it does not emit administrative audit events.

A disposable code-only worktree combined Luis's `85e04fe` with Gabriel's #118 `92fadc467db5a85dcea5c03fc3456e6d38936999` and #119 `132413bb8076371285cdee5a7bd18f6a92871278d8` / `c0b01a7afc6c8b61eb4ec1cc44259c78658378d8`. The original `cd backend; .\mvnw.cmd clean verify` run failed 1/52: `RegistrationIntegrationTest.registrationSurvivesEmailFailureAndResendRecovers` asserted that all captured application output excluded the synthetic email, but Spring DEBUG request logging included the input address. The same test failed 1/1 on Luis's `85e04fe` alone. The notification service's own failure line masked the address correctly.

Gabriel's isolated test-only correction `258e28feb987e77ab5129e811768e5856b7b03e4` scopes the redaction assertion to the `Email delivery FAILURE` log line. On the exact combined code tree `77966809749f1f636b22edf59ad73d4b4a0b055e`, Windows, Temurin Java 25.0.5+8, Docker Desktop 29.5.2, and PostgreSQL 18.6 via Testcontainers, `cd backend; .\mvnw.cmd clean verify` passed 52/52 with zero failures, errors, or skips. `AuthIntegrationTest` ran 12/12, `RegistrationIntegrationTest` 11/11, `AuthorizationPolicyIntegrationTest` 8/8, `AuditControllerIntegrationTest` 8/8, and `AuditServiceIntegrationTest` 3/3. The local Maven log and eight Surefire XML reports have verified SHA-256 copies in `C:\Users\hrval\AppData\Local\Colegio_Connect_Evidence\2026-11-09-pr115-pr118-pr119`. This is a local compatibility run on an unpublished tree; it does not establish hosted CI or team acceptance. Luis's PR #115 and Gabriel's PR heads remain untouched.
## Final reconciliation decision

Recover Gabriel's transaction and read foundation on current `main`, adapted to Massini's merged role model. Do not import PR #98 authorization, do not merge PR #99 unchanged, and do not implement future business actions in Issue #15. The branch makes the foundation independently reviewable; Issue #15 and proposal indicator I8 remain incomplete until teammate review, integration, and every listed action's owning issue provides same-transaction success and rollback evidence.
