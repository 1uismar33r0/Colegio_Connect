# Negative Authorization Test Harness

Issue #12 uses `NegativeAuthorizationTestHarness` for persisted denial
scenarios. The harness is test-only and does not create production endpoints or
bypass the real security configuration.

## Available fixtures

- `user(email, status, verified, roles...)` creates any account-status,
  verification, and non-organization role combination.
- `activeVerifiedUser(email, roles...)` is the common active fixture.
- `organization(name, type, approvalStatus)` creates institutional or external
  organizations in any approval state.
- `assign(user, role, organization)` creates organization-scoped or global role
  assignments.
- `authenticationFor(user)` creates an authenticated identity for direct
  persisted-policy checks. Authorities are deliberately empty so permission
  comes from database state.
- `login(user)` uses the real login endpoint and returns its session.
- `login(user, expectedStatus)` supports rejected login cases.

The inherited `@BeforeEach` removes role assignments, organizations, email
codes, and users in foreign-key order. A concrete class supplies
`@SpringBootTest`, `@AutoConfigureMockMvc`, and
`@Import(TestcontainersConfiguration.class)`.

## Adding a denial case

1. Add the test to `NegativeAuthorizationHarnessIntegrationTest`, or extend
   `NegativeAuthorizationTestHarness` from another focused integration-test
   class.
2. Build only the persisted state needed for the case. Use a unique test email
   and the role/status/organization helpers above.
3. Exercise a real endpoint with MockMvc when one exists. For a future service
   boundary that does not exist yet, document the dependency instead of adding
   a fake endpoint. Direct `AuthorizationPolicy` tests are appropriate for its
   current public methods.
4. Assert the safe denial status or a `false` policy result. Also assert any
   important state remains unchanged.
5. Run the focused class, then `mvn clean verify` with Java 25 and PostgreSQL
   Testcontainers.

Example:

```java
@Test
void pendingCompanyMemberCannotPublish() {
    User member = activeVerifiedUser("pending.company@upr.edu");
    Organization company = organization(
            "Pending company", OrgType.EXTERNAL_COMPANY, ApprovalStatus.PENDING);
    assign(member, Role.EXTERNAL_COMPANY, company);

    assertThat(authorizationPolicy.canPublishForOrganization(
            authenticationFor(member), company.getId())).isFalse();
}
```

## Verified initial cases

`NegativeAuthorizationHarnessIntegrationTest` covers:

- unauthenticated access: 401
- wrong organization role: policy fails closed
- suspended user reusing an authenticated session: 403
- unverified user login: 403

Two equivalent ad hoc tests in `AuthorizationIntegrationTest` were replaced,
not duplicated. Its nine other multi-role, revocation, ownership, organization,
suspension-policy, and missing-record tests remain intact.

## Verification evidence — 2026-11-03

- Branch: `gabriel/issue-12-negative-auth-harness`
- Base: PR #98 head `a4d7afd`
- Harness commit: `c90fb8d`
- Java: `25.0.4.1`
- PostgreSQL: `18.6` through Testcontainers

Focused verification:

```text
NegativeAuthorizationHarnessIntegrationTest: 4 passed
AuthorizationIntegrationTest: 9 passed
MVN_EXIT=0
```

Clean full verification:

```text
SUREFIRE tests=33 failures=0 errors=0 skipped=0
MVN_EXIT=0
```

This is local evidence. Code review, CI success, merge, and final issue closure
remain external acceptance gates.
