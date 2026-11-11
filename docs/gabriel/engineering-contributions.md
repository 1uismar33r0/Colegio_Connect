# Gabriel engineering contributions

## PR #97: early React authentication implementation

**Evidence reviewed:** November 7, 2026. **Repository baseline:** `main` at
`e5cc181b3ebcf7b5cb1050547c1d82922068c064`. This record distinguishes
historical implementation from merged code. It does not claim that PR #97 or
its source files were merged, deployed, or retained verbatim.

### Purpose and authorship

Gabriel (`DCGabriel10`) opened [PR #97](https://github.com/1uismar33r0/Colegio_Connect/pull/97)
to replace the placeholder frontend with an initial React authentication
application. The branch `gabriel/issue-14-react-auth-foundation` was based on
`main` revision `fc06e936a66921afa78c37e65b5be8dfe815a98d` and contains three
Gabriel-authored commits:

| Commit | Date | Contribution |
| --- | --- | --- |
| [`8bd0263`](https://github.com/1uismar33r0/Colegio_Connect/commit/8bd02638978654fc0e3678d96a4dc6427c895063) | 2026-11-02 | Vite/React/TypeScript frontend, authentication forms, session restoration, CSRF-aware API requests, Problem Detail handling, and initial tests |
| [`14d32dd`](https://github.com/1uismar33r0/Colegio_Connect/commit/14d32ddd7dbe64ddce0697290b434f84fb251cb3) | 2026-11-03 | Frontend CI job and CI Gate dependency |
| [`2276c53`](https://github.com/1uismar33r0/Colegio_Connect/commit/2276c5372a24db2ac6c4c06cf7a3bdcf3f717c91) | 2026-11-03 | Empty-success-response parser fix and registration regression test |

PR #97 remains open and conflicting. Its successful historical checks apply to
head `2276c53`; they do not establish correctness against current `main`.

### Original implementation

The first commit created `frontend/src/App.tsx`, a hand-written
`frontend/src/api/auth.ts` client, CSS, a Vite proxy, and Vitest tests. The UI
supported registration, six-digit email verification, resend, login, logout,
and session restoration. The client sent cookies on API requests and copied
Spring Security's readable CSRF cookie into the `X-XSRF-TOKEN` header for
unsafe methods.

The central request function was intentionally small:

```ts
const response = await fetch(path, { ...init, credentials: 'include', headers })
if (!response.ok) throw new ApiError(await problemFrom(response))
if (response.status === 204 || !(response.headers.get('content-type') || '').includes('json')) {
  return undefined as T
}
return response.json() as Promise<T>
```

This code:

1. included the browser session cookie;
2. converted non-success responses into a Problem Detail error;
3. treated `204` and successful non-JSON responses as bodyless; and
4. parsed JSON only when the response declared JSON content.

The original CSRF logic was also explicit:

```ts
if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
  const token = csrfToken()
  if (token) headers.set('X-XSRF-TOKEN', decodeURIComponent(token))
}
```

This established the same high-level browser contract later implemented by
the merged typed client: safe methods omit the CSRF header, while unsafe
methods echo the decoded cookie value.

### Empty `201 Created` bug discovery

Gabriel discovered a concrete integration defect while exercising registration
with the real backend. Spring returned `201 Created` with an empty body and no
JSON content type. The initial client called `response.json()` for every
successful response except `204`, turning a successful registration into
`Unexpected end of JSON input`.

The original browser-integration evidence is commit
[`c3c2fea`](https://github.com/1uismar33r0/Colegio_Connect/commit/c3c2fea6dc75322ea2b02bb575deb9970c847f34),
`test: verify real browser authentication integration`. That commit added a
Playwright authentication flow, screenshots, and the focused parser correction.
Only the parser correction and unit regression were transferred into PR #97 in
`2276c53`; the temporary combined-stack and Playwright artifacts were not.

The transferred regression test modeled the backend precisely:

```ts
vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 201 })))

await expect(
  register({ email: 'student@upr.edu', password: 'password123' }),
).resolves.toBeUndefined()
```

This is Gabriel's unique and reusable contribution from PR #97: the failure
scenario and its regression requirement, rather than the retired hand-written
parser implementation.

### Historical tests and limitations

At PR #97 head `2276c53`, Gabriel recorded four Vitest tests across two files,
plus lint, typecheck, build, and hosted CI run
[`41721009629`](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/41721009629).
The tests covered:

- CSRF initialization and login headers;
- CSRF on bodyless logout;
- empty `201` registration success; and
- rendering a backend Problem Detail login error.

The separate `c3c2fea` integration commit exercised register, login, restored
session, and logout in a real browser. It was evidence-generating work, not a
commit merged into `main`. PR #97's application tests otherwise used controlled
fetch mocks, and its single-file UI did not include the later router, role-aware
guards, field-error mapping, resend cooldown, or broad accessibility suite.

### Comparison with the preferred implementation

Luis's [PR #106](https://github.com/1uismar33r0/Colegio_Connect/pull/106)
merged as `a1288247f5810446a837ac5d270fd7b08144fc17`. Cristian's
[PR #112](https://github.com/1uismar33r0/Colegio_Connect/pull/112) merged as
`e5cc181b3ebcf7b5cb1050547c1d82922068c064`. Luis's
[PR #116](https://github.com/1uismar33r0/Colegio_Connect/pull/116) was still open
at unchanged head `7e50c29f133201dfabd68278ee6ad9b84ed56f21` when this comparison was
performed.

| Gabriel PR #97 feature | Classification | Current implementation and disposition |
| --- | --- | --- |
| Vite, React, TypeScript application | **Functionally equivalent** | PR #106 supplies the merged foundation. Do not transfer PR #97's parallel scaffold. |
| Hand-written authentication fetch client | **Obsolete or incompatible** | PRs #106/#112 use generated OpenAPI types and `openapi-fetch`. Retaining both clients would duplicate request, CSRF, and error behavior. |
| CSRF cookie/header handling | **Already included** | `frontend/src/api/client.ts` uses middleware to decode `XSRF-TOKEN` and attach `X-XSRF-TOKEN`; PR #112 also refreshes CSRF once after logout. |
| Empty `201` parser fix | **Already included** | `openapi-fetch` returns a successful response with undefined data for the backend's empty `201`; `unwrap` returns that data without calling `response.json()` directly. The old parser code is unnecessary. |
| Empty `201` regression scenario | **Unique and useful** | Preserve the scenario and authorship. An adapted typed-client regression test would strengthen unit coverage, but should be added only after PR #116 is stable and without copying the old client. |
| Registration form | **Functionally equivalent** | PR #116 adds normalized UPRM email checks, UTF-8 bcrypt limits, confirmation, field errors, focus management, and routed navigation. Do not transfer the earlier form. |
| Verification and resend form | **Functionally equivalent** | PR #116 uses controlled React state, code normalization, neutral account-existence behavior, cooldown, `429` handling, and separate status/error messages. This supersedes `document.querySelector` and the earlier success-via-error callback. |
| Login and session restoration | **Already included** | PR #106 supplies `AuthProvider` and route guards; PR #116 refreshes `/me` after login and returns to a validated application path. |
| Problem Detail presentation | **Functionally equivalent** | The current implementation maps backend field errors, hides `5xx` detail, and separates network failures. It is more complete than the single alert in PR #97. |
| Authentication navigation | **Obsolete or incompatible** | PR #97 switched local `view` state in one component. The current React Router structure and role-aware guards are authoritative. |
| Form/CSS accessibility basics | **Functionally equivalent** | PR #106 provides reusable labelled fields, status/alert components, skip link, route focus, titles, and contrast tokens; PR #116 adds form-state tests and axe coverage. |
| Frontend CI job | **Already included** | PR #106's merged job also runs Prettier and OpenAPI drift checks. PR #97's separate workflow should not be transferred. |
| Oxlint and unused starter assets | **Unique but unnecessary** | The current ESLint/Prettier configuration is the team standard; PR #97's alternate linter and unused assets add no demonstrated benefit. |
| PR #97 verification document | **Unique and useful as history** | Its dated claims remain branch-scoped evidence. This document preserves the durable contribution without representing the branch as merged. |

### Reconciliation verification

The November 7 comparison reran PR #97 head `2276c53` in a detached
worktree: 4/4 Vitest tests passed, and lint, typecheck, and production build
completed successfully. PR #116 head `7e50c29` independently passed 51/51
Vitest tests. These results verify each inspected branch separately; they do
not make PR #97 compatible with current `main` or mark PR #116 as accepted.

A direct probe of the current `openapi-fetch` dependency supplied an empty
`201` response to `POST /api/auth/register`. The client returned an OK response
with undefined data, confirming that Gabriel's original failure mode does not
occur in Luis's current client. The preferred implementation has the correct
behavior, while a focused unit regression remains useful documentation of why
that behavior matters.

### Reconciliation decision

Do not rebase, merge, or cherry-pick PR #97 into the current frontend. Do not
replace Luis's routing, typed client, authentication context, or components.
The old branch would introduce a second application architecture and restore
issues already addressed in review: dense single-file components, DOM querying
for form state, success text sent through an error callback, unused starter
assets, and a less complete test suite.

No application integration PR is justified while PR #116 is still under
review. After PR #116 reaches a stable reviewed revision, the team may add one
small typed-client regression test for an empty `201` registration response.
That test should credit the scenario to Gabriel's `c3c2fea`/`2276c53` work but
should use the current `openapi-fetch` client and current test conventions.

The proposed repository disposition is to mark PR #97 as superseded by merged
PRs #106/#112 and the eventual accepted revision of PR #116. Closing or editing
the pull request remains a repository-owner action; this record neither closes
nor merges it.
