# Browser authentication integration evidence

**Date:** 2026-11-02

**Branch:** `gabriel/issue-8-browser-auth-integration`

**Base:** `origin/feat/issue-8-login` at `081722692cbafe357ad82b98c8d767725627f046`

## Verified root cause

Spring Security `7.1.2` is resolved by the Maven build. Its `CsrfConfigurer.spa()` implementation configures both `CookieCsrfTokenRepository.withHttpOnlyFalse()` and the SPA token request handler. No explicit repository configuration is required.

MockMvc did not expose the `XSRF-TOKEN` cookie for the existing `204` endpoint in the attempted cookie assertions. A real embedded-server HTTP request did expose the cookie. This is a test-harness boundary, not a production CSRF defect. The production `SecurityConfig` and the `GET /api/auth/csrf` response contract remain unchanged.

## Added coverage

- Real HTTP `GET /api/auth/csrf` returns `204` and emits a readable `XSRF-TOKEN` cookie with `Path=/`.
- Login without the `X-XSRF-TOKEN` header is rejected with `403`.
- Login with the cookie token in `X-XSRF-TOKEN` succeeds and issues an HttpOnly, SameSite=Lax session cookie.
- Production defaults issue the session cookie with HttpOnly, Secure, and SameSite=Lax attributes.
- The authenticated session persists for `GET /api/auth/me`.
- CSRF remains available after login and after logout.
- Logout invalidates the authenticated session.
- A pre-login session ID is rotated during login.

Existing registration, verification, suspended/unverified-account, invalid-credential, lockout, and logout tests remain part of the full suite.

## Execution evidence

Environment: Java `25.0.4.1`, Docker Desktop `29.5.2`, PostgreSQL Testcontainers `18.6`, Spring Boot `4.1.2`.

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress -Dtest=BrowserCsrfIntegrationTest test
```

Result: 2 tests, 0 failures, 0 errors, 0 skipped; build success.

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

Result: 23 tests, 0 failures, 0 errors, 0 skipped; Flyway V1-V3 applied to PostgreSQL 18.6; build success. Surefire logged a delayed fork-JVM shutdown warning after the tests, but Maven completed successfully.

## API contract

No API contract changed. `GET /api/auth/csrf` continues to return `204 No Content` and initializes the `XSRF-TOKEN` cookie. Browser clients send that cookie value in `X-XSRF-TOKEN` for state-changing requests and include credentials for session persistence.
