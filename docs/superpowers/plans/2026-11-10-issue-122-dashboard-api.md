# Issue #122 Backend Dashboard API Plan

**Base:** `origin/main` `5ff997463a73a0f17f54fb4a4a4c7ec67b0f6f2e`; stack `gabriel/issue-122-role-dashboard-api` on PR #123 `f25ff1b49cfa2530a927dd209962c0666055cfaf`. **Requirements:** November 10 five-endpoint task, approved proposal pp. 4–7 and 12–17, canonical Issue #122, and [design/dependency map](../specs/2026-11-10-issue-122-dashboard-api-design.md).

The unchanged stacked baseline passed `backend/.\mvnw.cmd verify` 88/88 with Java 25 and PostgreSQL 18.6 Testcontainers on November 10. This does not verify any dashboard API.

## 1. Confirm upstream contracts and ownership

- Locate Luis's `roleData.ts`, `contracts.ts`, mock and tests; capture exact revision. If still unavailable, use the task DTOs only as provisional and identify the missing gate.
- Locate Cristian's listing/response branch if one exists outside fetched refs. Compare its migration, entities and APIs before creating any model. Reconcile PR #123 versus PR #124 with Cristian before integration; do not silently combine them.
- Decide the meaning of `PROFILE_VERIFICATION` and pending-company read behavior against the approved scope and Luis's actual client. Document any required contract change and compatibility test.

## 2. Implement real data foundations, in dependency order

- Add only canonical versioned persistence needed by the approved listing core, lifecycle, saves, responses, reports and review records; preserve V1–V6 and existing organizations. If Cristian supplies equivalent unmerged work, base on it or reuse it instead.
- Include constraints, ownership references, status/category values for all six approved categories, timestamp semantics, indexes and upgrade tests. Do not fill a result with literal demonstration data.
- A persistence table alone is not a publishing/response workflow. Expose only statuses and counts backed by actual rows; label any missing write workflow as a dependency. Keep private student fields out of dashboard DTOs.

## 3. Implement the five reads with failing tests first

- `GET /api/listings/feed`: published, visible, unexpired, trustworthy sources, server filters, bounded deterministic results, ISO timestamps, no private fields.
- `GET /api/me/activity`: authenticated current student, active-response and saved counts from their own rows, empty/populated/isolation tests.
- `GET /api/institution/overview`: explicit organization context, current membership, accurate status/unread response counts, draft `null`, bounded recent rows and multi-membership denial tests.
- `GET /api/company/dashboard`: current approved company membership, selected organization, accurate aggregates, deadlines, defined profile completeness and pending behavior; no respondent details.
- `GET /api/admin/review-queue`: current moderator access, real pending company/report/verification records only, accurate category counts, deterministic bounded items and no personal data.

## 4. Contract and integration verification

- Assert generated `/v3/api-docs` paths, parameter rules, exact DTO field names/types/enums/nullability; generate frontend types in an isolated integration worktree. Do not modify Luis's branch.
- Combine the latest backend branch, PR #123, and Luis's actual #122 frontend revision in a disposable worktree. Run backend `verify`, frontend `npm ci`, `api:fetch`, `api:gen`, typecheck, lint, tests and build. Use isolated Compose project/ports/volumes and synthetic records for HTTP and browser flows; preserve current preview and development data.
- Record actual commands, revisions, counts, failures and limits in the next available test-evidence entry, plus API/traceability/development-log updates. Request Cristian's review; publish a focused stacked PR or dependency PR series. Do not merge to main.
