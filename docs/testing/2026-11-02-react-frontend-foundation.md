# React frontend foundation verification — 2026-11-02

## Scope

Issue #13 infrastructure and Issue #14 authentication UI foundation on `gabriel/issue-14-react-auth-foundation`, based on `origin/main` commit `fc06e936a66921afa78c37e65b5be8dfe815a98d`.

## Contract implemented

- `GET /api/auth/csrf` returns `204`; its readable `XSRF-TOKEN` cookie is sent as `X-XSRF-TOKEN` for unsafe requests.
- `POST /api/auth/register`, `/verify`, `/resend`, `/login`, and `/logout` use JSON and browser credentials.
- `GET /api/auth/me` restores a session without inferring roles from `accountType`.
- Problem Detail responses are rendered through an accessible alert.

## Executed evidence

From `frontend`:

| Command | Result |
| --- | --- |
| `npm.cmd install` | passed; 0 audit vulnerabilities |
| `npm.cmd test -- src/api/auth.test.ts` | passed; 2 tests |
| `npm.cmd test` | passed; 3 tests across 2 files |
| `npm.cmd run build` | passed |
| `npm.cmd run lint` | passed |
| `npm.cmd run typecheck` | passed |

## Empty 201 registration regression — 2026-11-03

Real browser integration showed that Spring registration returns a successful
`201 Created` response with an empty body and no JSON content type. The generic
API client previously called `response.json()` and rejected the successful
registration with `Unexpected end of JSON input`.

Commit `c3c2fea` originally proved the fix in the isolated integration branch.
Only the focused response-parser change and its unit regression test were
transferred to this PR branch; Playwright dependencies, screenshots, and the
combined integration stack were not copied.

Fresh verification after the transfer:

| Command | Result |
| --- | --- |
| `npm.cmd test` | passed; 4 tests across 2 files |
| `npm.cmd run lint` | passed |
| `npm.cmd run typecheck` | passed |
| `npm.cmd run build` | passed |

## Limits and follow-up

Tests use controlled `fetch` mocks because login/session endpoints remain in open PR #95. No end-to-end browser/backend claim is made. Production session cookies are `Secure`; a real local browser flow requires HTTPS or an approved backend development profile, without changing production cookie settings.
