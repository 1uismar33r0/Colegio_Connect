# Verified team dependencies

**Observed:** 2026-11-07. **Repository baseline:** `main` at `a128824`; frontend [PR #106](https://github.com/1uismar33r0/Colegio_Connect/pull/106) merged at that revision. Draft [PR #107](https://github.com/1uismar33r0/Colegio_Connect/pull/107) now targets `main`. This record distinguishes visible repository evidence from work that may exist outside GitHub.

## What Gabriel provides

- The merged Java 25 backend/container CI foundation from PR #89.
- A stable `CI Gate` aggregation check.
- Main-branch container image packaging evidence from run [41935703633](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/41935703633).
- The trigger-only Issue #2 change in draft PR #107, retargeted to `main` without rebasing, force-pushing, or modifying Luis's merged work.
- Infrastructure and integration review evidence without taking ownership of frontend or backend application code.

## Current dependencies and owners

| Affected work | Verified problem | Owner / required action | Can Gabriel resolve it independently? |
| --- | --- | --- | --- |
| Luis's merged role-aware routes | `GET /api/auth/me` returns `id`, `email`, and `accountType`, but no roles. The merged frontend therefore treats every authenticated user as having `roles: []`. Massini's visible authorization branch also omits roles from `UserResponse`. | **Massini:** define and implement the reviewed backend role response and update the OpenAPI contract. **Luis:** consume the agreed identifiers. | No. Gabriel can verify the contract but must not implement either teammate's application code. |
| Luis's generated OpenAPI client | Springdoc exposes the injected CSRF token as a required query parameter, and generated record fields are optional. The current drift check compares generated types only with the committed snapshot. | **Massini:** correct CSRF/schema publication and provide a current backend OpenAPI export. **Luis:** regenerate and integrate the client after the contract is reviewed. | No. Gabriel may add infrastructure-level contract verification only after the backend contract exists. |
| Frontend setup documentation | The merged `frontend/README.md` is still generic Vite template documentation and does not explain the Colegio Connect proxy, API generation, or local backend integration. | **Luis:** replace it with frontend-owned setup instructions. | No. Gabriel can link the finished instructions from infrastructure documentation. |
| Issue #2 enforcement | Passing CI exists, but branch protection/rulesets are not verified. The private repository returned a plan/visibility restriction for rulesets, and Gabriel has WRITE rather than ADMIN permission. | **Luis/repository owner:** decide on plan/visibility, then require pull requests, one approval, resolved conversations, and `CI Gate`; block direct/force pushes and deletion as supported. | No. Gabriel can supply the checklist and verify evidence after the owner changes settings. |
| Authorization implementation | Massini's `feat/issue-9-authorization` and Gabriel's preserved PR #98 overlap. Gabriel no longer independently implements backend features. | **Team, led by Massini:** select the backend path and explicitly transfer any useful tests/security evidence from #98. Preserve the open work until that decision. | No application implementation. Gabriel can review security evidence after the team selects the code path. |
| Draft PR #107 | PR #106 merged as `a128824`, and #107 was safely retargeted from `Issue#13-frontend-setup` to `main`. Its previous green runs predate the base change, and no teammate review exists. | **Gabriel:** push the documentation checkpoint and verify both new event runs. **Luis or Massini:** review #107. | Partially. Gabriel owns verification but not independent review or merge approval. |

## Integration order

1. Verify PR #107's new exact head through both push and pull-request events against `main`.
2. Luis or Massini reviews PR #107; the user decides whether to merge it.
3. Massini and Luis agree on the role and OpenAPI contracts; Gabriel verifies CI integration without implementing their code.
4. The repository owner enables enforceable review and required-check rules when the repository plan/visibility supports them.

Issue #2 remains open until another member's review and enforceable failing-check behavior are demonstrated. Historical push and pull-request execution is verified, but the `main`-based PR needs a fresh exact-revision run. These results do not establish full Indicator I13 acceptance.
