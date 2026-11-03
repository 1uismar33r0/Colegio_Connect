# Real browser authentication integration — 2026-11-03

## Tested integration revision

`gabriel/auth-frontend-integration-2026-11-03` combines the reviewed heads of PR #95, PR #96, and PR #97. At test time, it was based on `474084c2dd1892ac8342565f0a778d54b505bd12` before the local integration-test and regression-fix commit described below.

The isolated local Compose project used PostgreSQL on `15432`, Spring Boot on `18080`, and Mailpit on `18025`. Vite ran at `http://127.0.0.1:5173` with `VITE_API_TARGET=http://127.0.0.1:18080`.

## Real Chromium verification

`npm.cmd run test:e2e` passed against the live stack. It used a generated `@upr.edu` address, a real Spring Boot registration request, a real Mailpit verification message, and real Chromium cookies. It verified:

- unauthenticated session restoration renders sign-in;
- invalid non-UPRM registration displays the backend Problem Detail error;
- valid registration returns `201` and reaches verification UI;
- an incorrect six-digit code displays the API error;
- the actual code retrieved from Mailpit verifies with `204`;
- wrong-password login displays the API error;
- a logout request without `X-XSRF-TOKEN` is rejected with `403`;
- real login returns `200`, establishes a `Secure`, `HttpOnly`, `SameSite=Lax` `JSESSIONID`, and permits `GET /api/auth/me`;
- a browser refresh restores the authenticated session;
- logout returns `204`; replaying the pre-logout session cookie to `/api/auth/me` returns `401`.

Screenshots from the passing run are in `docs/testing/evidence/2026-11-03-real-browser-auth/`:

- `registration-awaiting-verification.png`
- `authenticated-session.png`
- `after-logout.png`

The generated Playwright HTML report is local at `frontend/playwright-report/index.html` and reports one passed test.

## Defect found and fixed locally

The first real registration returned backend `201 Created` with no response body, as designed. `frontend/src/api/auth.ts` attempted `response.json()` for every successful status other than `204`, causing `Unexpected end of JSON input` and leaving the UI on the registration screen. The regression test `accepts an empty 201 response from registration` now verifies that a successful response without a JSON content type resolves as `undefined`.

This is a frontend client compatibility fix only; it does not change Spring Boot behavior, session configuration, CSRF policy, or role/account semantics.

## Other verification

| Command | Result |
| --- | --- |
| `npm.cmd test` | 4 unit tests passed |
| `npm.cmd run test:e2e` | 1 real Chromium test passed |
| `npm.cmd run lint` | passed |
| `npm.cmd run typecheck` | passed |
| `npm.cmd run build` | passed |
| `backend\\mvnw.cmd --batch-mode --no-transfer-progress verify` (Java 25) | 23 tests, 0 failures, 0 errors, 0 skipped |

## Boundary

This establishes local M1 authentication interoperability, not all Sprint 1 acceptance criteria. It does not cover role-based dashboard navigation, accessibility audit tooling such as axe-core, CI execution of the Playwright stack, or an externally deployed HTTPS environment.
