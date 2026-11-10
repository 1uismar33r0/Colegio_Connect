# Verified team dependencies

**Updated:** 2026-11-10. **Current main:** `e5cc181b3ebcf7b5cb1050547c1d82922068c064`, including Cristian's merged [PR #111](https://github.com/1uismar33r0/Colegio_Connect/pull/111) authorization and [PR #112](https://github.com/1uismar33r0/Colegio_Connect/pull/112) CSRF/OpenAPI correction. Earlier CI evidence retains its original November 7 revision.

## What Gabriel provides

- The merged Java 25 backend/container CI foundation from PR #89.
- A stable `CI Gate` aggregation check.
- Main-branch container image packaging evidence from run [41935703633](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/41935703633).
- [PR #107](https://github.com/1uismar33r0/Colegio_Connect/pull/107) adds unrestricted push and pull-request triggers plus manual dispatch; its existing verification jobs and main-only image publication remain.
- Infrastructure and integration review evidence without taking ownership of frontend or backend application code.

## Current dependencies and owners

| Affected work | Verified problem | Owner / required action | Can Gabriel resolve it independently? |
| --- | --- | --- | --- |
| Luis's merged role-aware routes | `GET /api/auth/me` returns `id`, `email`, and `accountType`, but no roles. The frontend therefore treats real authenticated users as `roles: []`, hiding role navigation and redirecting role routes to Forbidden. Current persisted identifiers are `STUDENT`, `INSTITUTIONAL`, `COMPANY`, and `MODERATOR`. | **Cristian:** agree and implement the current persisted-role response for login and `/me`, then update OpenAPI. **Luis:** regenerate the typed client and verify guards against the real response. Server-side checks remain authoritative. | Gabriel verifies the contract and negative cases without silently taking ownership of teammate code. |
| Luis's generated OpenAPI client | Main's `AuthController.csrf()` marks the injected `CsrfToken` with `@Parameter(hidden = true)`, and the committed OpenAPI snapshot no longer exposes a CSRF query parameter after #112. Generated type drift checks still compare only with that committed snapshot, not a running backend. | **Cristian and Luis:** keep backend OpenAPI and frontend snapshots synchronized as roles and endpoints evolve. | Gabriel can verify the integration contract after review. |
| Email verification | Main includes `/api/auth/register`, `/verify`, and `/resend`, V2 code limits, and registration integration tests. The implementation exists; a complete browser/email-delivery flow and all Issue #7 acceptance evidence remain pending. | **Cristian:** maintain API/security behavior. **Luis:** integrate #115 notification delivery and #116 screens and verify the real flow. | Gabriel checks cross-branch behavior and evidence. |
| Frontend setup documentation | The merged `frontend/README.md` is still generic Vite template documentation and does not explain the Colegio Connect proxy, API generation, or local backend integration. | **Luis:** replace it with frontend-owned setup instructions. | No. Gabriel can link the finished instructions from infrastructure documentation. |
| Issue #2 enforcement | Passing CI exists, but branch protection/rulesets are not verified. The private repository returned a plan/visibility restriction for rulesets, and Gabriel has WRITE rather than ADMIN permission. | **Luis/repository owner:** decide on plan/visibility, then require pull requests, one approval, resolved conversations, and `CI Gate`; block direct/force pushes and deletion as supported. | No. Gabriel can supply the checklist and verify evidence after the owner changes settings. |
| Authorization implementation | Cristian's #111 is the merged production authorization model. [#98](https://github.com/1uismar33r0/Colegio_Connect/pull/98) closed without merge; its obsolete role-renaming V4 must not be restored. Gabriel's #118 test harness, #119 audit V4, and #120 company decision V5 are being consolidated on #119 for review. | **Cristian:** review current-model security and migration behavior. **Luis:** consume approved API contracts. | Gabriel preserves unique #98 regression scenarios without replacing #111. |
| Ready-for-review PR #107 | Its workflow passed push and PR CI at head `15d0469`, but the recorded base predates #111/#112 and there is no formal GitHub approval. Cristian's November 10 feedback requested the documentation corrections in this revision. | **Luis or Cristian:** review the corrected diff and current-main CI. **Repository owner/team:** integrate after required review and checks. | Gabriel responds to findings; #103/#121 disposition waits for verified replacement. |

## Integration order

1. Correct and review PR #107, then verify its current head against current main. PR #121 is a narrower manual-only alternative; the team chooses one CI path.
2. Cristian and Luis agree on the `/api/auth/me` roles/OpenAPI contract; Gabriel verifies the resulting authorization and frontend integration behavior.
3. Cristian reviews the consolidated security/audit/company PR #119 and its V4/V5 migration order; Luis reviews the frontend-facing API contract.
4. The repository owner enables enforceable review and required-check rules when the repository plan/visibility supports them.

Issue #2 remains open until another member's review and enforceable failing-check behavior are demonstrated. Push and pull-request execution was verified at `678c43e` against the then-current base; this does not establish full Indicator I13 acceptance or verification against today's main.
