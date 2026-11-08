# Issue #12 - Negative-Authorization Test Harness

## A. Purpose

Issue [#12](https://github.com/1uismar33r0/Colegio_Connect/issues/12) assigns Gabriel a reusable integration-test base for forbidden and unauthenticated scenarios. The harness exists to prove that security rules fail closed for synthetic users, roles, account states, and organizations while exercising the real Spring Security and PostgreSQL configuration.

This work supports approved proposal objective O2 and indicator I6. The proposal requires server-side checks for role, ownership, organization approval, account status, and consent, with at least ten negative-authorization tests over the completed product. The current application does not yet implement listing, response, moderation, ownership, consent, or sponsorship endpoints. This document therefore distinguishes the completed Issue #12 harness from the still-incomplete final I6 product coverage.

The approved proposal inspected for this reconciliation is the 30-page PDF with SHA-256 `88038171ef23ffbd42bcbe38cd0ec5177240e42ab010fb6fbed96107b1b8f98c`.

## B. Gabriel's implementation

### Original PR #100 work

Gabriel authored the original harness on `gabriel/issue-12-negative-auth-harness`:

| Commit | Authored contribution |
| --- | --- |
| `c90fb8d462b7e04d9de400d61a16f815d899c31d` | Added `NegativeAuthorizationTestHarness`, four initial denial tests, persisted fixtures, login/session helpers, and foreign-key-safe cleanup. |
| `52352763d0f01e1b8bb9f87e1ec843bb357c2c6b` | Documented the fixtures, contributor workflow, original local results, and limitations. |

PR [#100](https://github.com/1uismar33r0/Colegio_Connect/pull/100) remains open. It is stacked on Gabriel's PR #98 head `a4d7afd741a4ae6024241c3e7caad37b8da21c96`, not on current main. It has no direct review or check run. Its code was included in CI-only validation PR #101 at `769e61668bf9278b4d8e96ce3c0eeb17be77c54b`, where Backend, Container, and CI Gate passed. That historical validation does not make PR #100 merged or compatible with the later production authorization model.

### Reconciled main-based work

The final implementation branch is `gabriel/issue-12-negative-auth-reconciliation`, based on main `e5cc181b3ebcf7b5cb1050547c1d82922068c064`. Gabriel's test-infrastructure design was adapted in commit `92fadc467db5a85dcea5c03fc3456e6d38936999`:

- `NegativeAuthorizationTestHarness` owns synthetic persisted fixtures, cleanup, Spring Security principals, and real MockMvc login/session helpers.
- `AuthIntegrationTest` inherits the harness and retains Massini's endpoint assertions for anonymous, suspended, unverified, CSRF, lockout, logout, role-loading, and post-login suspension behavior.
- `AuthorizationPolicyIntegrationTest` inherits the harness and retains Massini's persisted policy assertions for account status, missing users, cross-organization access, incorrect roles, and organization approval.
- No production source, endpoint, migration, role enum, or OpenAPI contract changed.

### Reconciliation classification

| PR #100 component | Classification | Reconciliation decision |
| --- | --- | --- |
| User fixtures for active/suspended and verified/unverified states | Compatible and reusable | Retained with current `User`, `UserStatus`, and password encoder. |
| Canonical-role fixture loop | Requires adaptation | Uses current `STUDENT`, `INSTITUTIONAL`, `MODERATOR`, and `COMPANY` identifiers. |
| Organization and approval fixtures | Compatible and reusable | Retained against current `Organization`, `OrgType`, and `ApprovalStatus`. |
| Role assignment and UUID principal helpers | Compatible and reusable | Retained against Massini's merged `AuthorizationPolicy`. |
| MockMvc login and session helpers | Compatible and reusable | Retained with CSRF and the real `/api/auth/login` endpoint. |
| Foreign-key-safe cleanup | Compatible and reusable | Retained in role-assignment, organization, email-code, user order. |
| PR #102 fixture-isolation fix `695fe3abbc5951777a09d34ca4727ffebb296b8b` | Already present in authoritative behavior | The old login branch needed role assignments deleted before organizations. Current main already follows that order, and the reconciled harness preserves it without cherry-picking PR #102. |
| Standalone PR #100 four-case test class | Redundant on current main | Not copied; equivalent assertions already exist in authoritative merged tests. |
| `FACULTY_OFFICE_DEPT`, `EXTERNAL_COMPANY`, and `canPublishForOrganization` references | Superseded or incompatible | Not imported; those contracts are absent from the merged model. |
| PR #98 production authorization and V4 role migration | Superseded | Massini's merged PR #111 remains authoritative. |
| Original contributor instructions | Requires adaptation | Rewritten below for the current harness and current endpoint boundary. |

## C. Real code examples

All excerpts in this section come from implementation commit `92fadc467db5a85dcea5c03fc3456e6d38936999`.

### Synthetic account and role fixtures

```java
protected User user(String email, UserStatus status, boolean verified, Role... roles) {
    User user = new User(email, passwordEncoder.encode(TEST_PASSWORD), AccountType.UPRM_MEMBER);
    user.setStatus(status);
    user.setEmailVerified(verified);
    User saved = userRepository.save(user);
    for (Role role : roles) {
        assign(saved, role, null);
    }
    return saved;
}
```

Inputs are an email address, persisted account status, verification flag, and zero or more canonical roles. The method hashes the synthetic password through the application's configured encoder, saves the user in PostgreSQL, persists each requested role, and returns the managed user. Tests can create valid or intentionally invalid security states without bypassing JPA constraints.

### Organization and assignment fixtures

```java
protected Organization organization(String name, OrgType type, ApprovalStatus status) {
    Organization organization = new Organization(name, type);
    organization.setApprovalStatus(status);
    return organizationRepository.save(organization);
}

protected RoleAssignment assign(User user, Role role, Organization organization) {
    return roleAssignmentRepository.save(new RoleAssignment(user, role, organization));
}
```

The first method receives an organization name, type, and approval state and returns a persisted organization. The second receives a user, canonical role, and optional organization and returns the persisted relationship. Passing a different organization to the policy check is how tests model cross-organization denial.

### Real login and session creation

```java
protected MockHttpSession login(String email, String password, ResultMatcher expectedStatus)
        throws Exception {
    MvcResult result = mockMvc.perform(post("/api/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson(email, password)))
            .andExpect(expectedStatus)
            .andReturn();
    return (MockHttpSession) result.getRequest().getSession(false);
}
```

This helper does not fabricate a Spring Security context. It sends a JSON request through the real controller, login service, password check, CSRF filter, and session persistence. The caller supplies the expected HTTP matcher; successful authentication returns the actual session, while rejected requests normally return `null`.

### Database cleanup

```java
@BeforeEach
protected void cleanAuthorizationFixtures() {
    roleAssignmentRepository.deleteAll();
    organizationRepository.deleteAll();
    emailCodeRepository.deleteAll();
    userRepository.deleteAll();
}
```

Every test starts from a known state. Dependents are deleted before users so PostgreSQL foreign keys continue to enforce integrity rather than being disabled for tests.

### Cross-organization denial

```java
User user = user(UserStatus.ACTIVE);
Organization myOrg = organization(ApprovalStatus.APPROVED);
Organization otherOrg = organization(ApprovalStatus.APPROVED);
assign(user, Role.INSTITUTIONAL, myOrg);

assertThat(authorizationPolicy.hasRoleIn(
        authenticationFor(user), Role.INSTITUTIONAL, otherOrg.getId())).isFalse();
```

The UUID principal matches the real login principal shape. `AuthorizationPolicy` queries the persisted assignment for the requested organization, so a role in `myOrg` cannot authorize access to `otherOrg`.

## D. Test walkthroughs

### Unverified account login

1. Inherited cleanup removes prior assignments, organizations, email codes, and users.
2. `user(EMAIL, ACTIVE, false)` stores a valid-password account whose email is not verified.
3. No authenticated session exists before the request.
4. `login(user, status().isForbidden())` posts through the real CSRF-protected login endpoint.
5. `LoginService` validates the password and then rejects the verification state with HTTP 403.
6. MockMvc asserts the exact status; no session is accepted.
7. The next test repeats cleanup, preventing fixture leakage.

### Existing session after suspension

1. Cleanup establishes an empty database.
2. `activeVerifiedUser(EMAIL)` creates an allowed account.
3. The real login endpoint creates a session.
4. The persisted user status changes to `SUSPENDED` after login.
5. A request to `/api/auth/me` reuses the existing session.
6. `SecurityConfig` calls `AuthorizationPolicy.isActive` on every authenticated request and returns HTTP 403.
7. Cleanup deletes the synthetic user before the next case.

### Incorrect organization or role

1. Cleanup removes prior organization state.
2. The test creates an active user and one or two approved organizations.
3. It assigns either the correct role to another organization or a different role to the target organization.
4. It constructs the same UUID principal shape created at login.
5. `hasRoleIn` queries the persisted user, role, and organization tuple.
6. AssertJ requires `false`; the query cannot succeed through a role-name or organization mismatch.
7. Cleanup removes assignments before organizations and users.

### Pending organization

1. Cleanup starts with no organizations.
2. `organization(PENDING)` persists a UPRM department in the pending state.
3. No endpoint is invented; the test invokes the current public policy boundary.
4. `isApproved` loads the organization by UUID.
5. The expected result is `false` because only `APPROVED` is accepted.
6. AssertJ verifies the denial.
7. Cleanup removes the organization.

## Adding a denial case

1. Add the assertion to the focused integration-test class that owns the real endpoint or policy boundary. Extend `NegativeAuthorizationTestHarness` if the class does not already do so.
2. Create only the persisted state required by the scenario with `user`, `activeVerifiedUser`, `organization`, and `assign`.
3. Prefer MockMvc against a real endpoint. Use direct `AuthorizationPolicy` calls only for its current public methods; do not invent an endpoint to satisfy a planned scenario.
4. Assert the safe HTTP status or `false` result. For an attempted write, also assert that protected database state did not change.
5. Keep one security behavior per test and give it a denial-specific name.
6. Run the focused class with Java 25 and PostgreSQL 18.6 Testcontainers, then run `mvnw.cmd clean verify`.
7. Add new cases only when a stable endpoint or service contract exists. Link the owning PR/SHA in this document's compatibility section.

## E. Engineering decisions

### Reusable fixtures instead of copied setup

The merged authentication and policy tests previously repeated repository injection, cleanup, user construction, organization construction, principal creation, and login JSON. Centralizing these mechanics makes new cases smaller and keeps security-state creation consistent. Assertions remain in the owning test classes, so the harness does not hide expected behavior.

### Massini's production authorization remains authoritative

PR #111 was merged through `3ff4708055a412616a6b25f880ea5aefc4e9c098`, and PR #112 produced current main `e5cc181b3ebcf7b5cb1050547c1d82922068c064`. PR #98 is an older, unmerged implementation with different role identifiers and policy methods. This reconciliation imports no production code or migration from PR #98.

### Existing tests were refactored, not copied

Current main already contained the four initial Issue #12 behaviors: unauthenticated access, wrong role/organization, suspended user/session, and unverified login. The standalone PR #100 test class was therefore not copied. The existing authoritative tests now consume Gabriel's harness, satisfying reuse without running equivalent assertions twice.

### Current boundary and limitations

The two consumer classes contain 14 named denial or restriction cases across authentication, session, CSRF, role, account-status, organization, and approval behavior. They include the initial Issue #12 categories, but they do not establish final proposal indicator I6. Unapproved-company publishing, ownership, consent, moderation, non-team sponsorship acceptance, state-no-change assertions for business writes, and privacy-response scanning require endpoints and models that do not exist on the inspected baseline.

No test in PR #118 grants a role, authenticates the user, revokes that role, and retries access with the same session or principal. This is an intentional recorded gap, not a passing claim. At the inspected baseline, login copies roles into session authorities, while the global request policy rechecks only current account activity. No role-management endpoint or system-wide contract defines whether deleting an assignment must invalidate every existing session immediately, so Issue #12 does not invent that expectation. PR #119 defines the narrower audit-history contract: its read policy checks a persisted global `MODERATOR` assignment on every request, and its focused integration test performs the exact grant-login-delete-reuse-session sequence and expects HTTP 403.

## F. GitHub evidence

Evidence observed through 2026-11-08 in `America/La_Paz`; GitHub UTC timestamps may fall on 2026-11-08.

| Field | Evidence |
| --- | --- |
| Canonical issue | [#12 - Negative-authorization test harness](https://github.com/1uismar33r0/Colegio_Connect/issues/12), open, no assignee, no comments at observation time. |
| Preserved alias | #47 remains the historical traceability alias from the 2026-10-26 issue export; a live query on 2026-11-07 returned no current issue or PR for that number. |
| Original PR and branch | [PR #100](https://github.com/1uismar33r0/Colegio_Connect/pull/100), `gabriel/issue-12-negative-auth-harness`, open, head `52352763d0f01e1b8bb9f87e1ec843bb357c2c6b`, stacked on PR #98. |
| Authoritative production baseline | Merged PR #111, merged PR #112, main `e5cc181b3ebcf7b5cb1050547c1d82922068c064`. |
| Final implementation branch | `gabriel/issue-12-negative-auth-reconciliation`, based directly on the authoritative main SHA. |
| Harness implementation commit | `92fadc467db5a85dcea5c03fc3456e6d38936999`. |
| Documentation commit | `9ca3b84853e5d5771f85786f9ded99aea774f1bd`. |
| Reconciliation PR | [PR #118](https://github.com/1uismar33r0/Colegio_Connect/pull/118), opened against main at initial head `9ca3b84853e5d5771f85786f9ded99aea774f1bd`; this evidence-only follow-up records the live PR number. |
| Local test environment | Windows, Temurin Java `25.0.5+8`, Spring Boot `4.1.2`, Docker Desktop `29.5.2`, PostgreSQL `18.6` Testcontainers. |
| Focused result | `AuthIntegrationTest`: 12 passed; `AuthorizationPolicyIntegrationTest`: 8 passed; total 20, zero failures/errors/skips. |
| Full result | `mvnw.cmd clean verify`: 30 passed, zero failures/errors/skips; Maven `BUILD SUCCESS`. |
| Hosted CI | PR #118 run `42445314730` at head `203a4243111ab26ddfde67fb5bd5b559ad5b0f72`: Backend, Frontend, Container, and CI Gate passed. The plan-completion update after that head is documentation-only and must retain green checks. |
| Review status | Massini was requested as the security reviewer on PR #118; no teammate review had been submitted when this record was updated. Automated review does not replace the required team security review. |
| Merge status | The reconciliation is not merged. PR #100 also remains open and unmerged. |

OpenAPI was not changed because this task adds test infrastructure and documentation only.

## G. Future compatibility

### Changes to Recheck When Other PRs Merge

Compatibility method: the exact heads listed below were inspected directly in fetched source and Git history. PR #100's historical stack was exercised only through CI-only PR #101. PRs #115, #116, #117, #99, #102, and #98 were not combined with PR #118 in a disposable merge worktree; the table does not claim otherwise. PR #119 was independently implemented and tested from the same `main` baseline in its own isolated worktree, not merge-tested with PR #118.

| Dependency and inspected SHA | Owner / provisional state | Contract or files to recheck | Current impact and follow-up |
| --- | --- | --- | --- |
| PR #115, `3eb0a11974d23fe06ffe16b650d3069e7450e844` | Luis; open, green CI, no formal review at observation | `RegistrationService`, notification provider, `RegistrationIntegrationTest` | No login or authorization contract change found. Re-run the full backend suite after merge because application context and registration fixtures change. |
| PR #116, `7e50c29f133201dfabd68278ee6ad9b84ed56f21` | Luis; open, green CI, review comments pending | Frontend auth pages, validation, traceability, test evidence | No backend security code change found. Recheck documentation evidence IDs and auth-flow wording if it merges before this branch. |
| PR #117, `e9a3951844e8081342861a37a90739c7674fb2c9` | Gabriel; open, green CI, no teammate review | `docs/gabriel/engineering-contributions.md` | Do not duplicate or overwrite it. After both PRs settle, add one index link from that contribution document to this topic document. |
| PR #119, initial review head `669d3bb317cba9d900a5fc567c785a42fa701dbf` | Gabriel; open, Massini review requested, CI running at observation | Persisted moderator check, `audit_log` foreign key, audit-test cleanup, and same-session revocation behavior | Its source and local Java 25 evidence were inspected. It supplies the exact role-revocation test only for audit-history reading. If it merges, add `auditEventRepository.deleteAll()` before users in shared harness cleanup when audit rows are created by a denial scenario. |
| PR #99, `2e57200f37a461f9f74fcf79b26c56fab1060770` | Gabriel; open, stacked on superseded PR #98, provisional | Audit entity/repository and V5 `audit_log.actor_id` foreign key | Not imported. If a later authoritative audit implementation records rows during denial cases, delete audit rows before users in harness cleanup and add failed-action/no-audit assertions. |
| PR #102, `695fe3abbc5951777a09d34ca4727ffebb296b8b` | Gabriel; open against the old login branch, no direct CI or review | `AuthIntegrationTest` cleanup ordering | Current main already deletes role assignments before organizations. The reconciled harness retains that dependency order; no future action unless new foreign keys are introduced. |
| PR #98, `a4d7afd741a4ae6024241c3e7caad37b8da21c96` | Gabriel; open, superseded production approach | Role identifiers, V4 migration, old authorization helpers | Do not merge into this harness. Preserve as historical Gabriel authorship only; Massini's merged model remains the baseline. |
| Future publishing/approval, listing, response, moderation, and sponsorship PRs | Cristian/Massini backend with feature-specific owners; not yet stable | New authenticated endpoints, ownership checks, organization approval operations, consent/contact release, audit writes | Add endpoint-level denials only after contracts stabilize. Include database no-change assertions and grow the supported I6 matrix without speculative endpoints. |

## Final reconciliation decision

Recover Gabriel's test-infrastructure contribution, adapted to current main, but do not recover PR #98's production authorization or copy PR #100's now-duplicate test class. The implementation is reviewable as a test-and-documentation-only change. Issue #12 cannot be closed until the harness is merged, CI remains green on the final PR head, another team member completes the required security review, and the team confirms the remaining acceptance interpretation. Final I6 acceptance remains future work as protected business endpoints are implemented.
