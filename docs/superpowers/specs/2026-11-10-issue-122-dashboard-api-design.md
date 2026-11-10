# Issue #122 Role Dashboard Backend: Dependency and Contract Design

**Later live update (2026-11-10 23:09 UTC):** `origin/main` advanced to `f68480c` with #116 and Cristian's #124 merged. PR #123 remains open at `f25ff1b` with green CI but no team review. #124's role API only assigns INSTITUTIONAL/MODERATOR and main's `/api/auth/me` still lacks roles; #123 provides the current-role response and scoped COMPANY/STUDENT administration used by Luis's navigation. This branch remains explicitly stacked on #123. A disposable combined worktree must expose the overlap before integration; #123 and #124 cannot both be presumed mergeable without reconciliation.

**Observation:** 2026-11-10, after `git fetch --all --prune`. `origin/main` is `5ff997463a73a0f17f54fb4a4a4c7ec67b0f6f2e`. This branch starts at PR #123 head `f25ff1b49cfa2530a927dd209962c0666055cfaf` and is separate from Luis's checkout. The approved 30-page proposal (pp. 7, 12–17) and [traceability](../../project/traceability.md) govern category, access, source-label, lifecycle, privacy and moderation semantics.

## Live dependency map

| Capability | Observed source | Decision |
| --- | --- | --- |
| Authentication, organization roles, company approval, audit | `main` `5ff9974` includes merged PR #119; account/organization/audit tables and pending-company queue exist. | Reuse. A pending company queue alone is not a full moderation queue. |
| Current roles, administrator search and bootstrap | PR #123 `f25ff1b` open, hosted CI passed; Cristian review pending. | Stack this backend branch on #123. Its persisted moderator check is the security dependency. |
| Cristian's independent role implementation | PR #124 `819d9cda302f43e46af21670f92742153e6174dc` was open at initial inspection and merged to main at `f68480c` during this task, with different admin routes and bootstrap semantics. | Do not merge the two implementations silently. Cristian must choose/reconcile the Issue #10 contract before integration. |
| Luis's role pages and typed client | After the initial fetch, `origin/feat/issue-122-role-pages` appeared at `94fd9fa5c08fd64aa60d0545fda1e28e47979b47`. Its `roleData.ts`, `contracts.ts`, fixtures, and five pages were inspected. No PR #122 was found at that checkpoint. | Preserve its five paths and body shapes. It calls office/company routes without `organizationId`, uses title-case type filters, and assumes numeric `pendingVerification`. Its `Event` fixture/filter is outside the six approved categories. |
| Listing core, lifecycle, discovery | No implementation on main or fetched unmerged branches; #18/#59 belongs to Cristian, #19/#61 to Gabriel, #21/#63 to Luis. | A canonical persistence model and publishing flow are upstream dependencies for feed, office and company dashboards. Do not create an unrelated duplicate table. |
| Saved listings and student responses | No tables or services; #23/#65 belongs to Luis, #31/#69 to Cristian. | Activity and publisher response counts cannot be fabricated or read from account/role data. |
| Reports and profile verification | No listing reports or profile-verification records; #37/#72 and #39/#73 belong to Gabriel. | Company review can use pending organizations. Other queue categories require actual records and timestamps; do not substitute pending email codes or hardcoded zero counts. |

## Contract decisions to validate with Luis

1. `GET /api/listings/feed` may retain a `FeedItem[]` body while accepting bounded `page`/`size` and returning pagination metadata in headers; the actual client return type must be inspected before fixing this choice.
2. Luis's wrappers omit `organizationId`. Omission resolves only when exactly one eligible organization exists. Multiple memberships return 409; Luis needs an organization selector and must pass `organizationId` for that case. Explicit IDs are checked against current membership on every request.
3. The user clarified that pending owners need a separate read-only view. V9 adds `company_application_owners` as an ownership link; a verified external applicant with this persisted link may receive only `PENDING_APPROVAL` and no listing/response data. The current registration workflow does not yet create external users or this link, so the normal user journey remains blocked by #16. PR #123 still forbids COMPANY assignment before approval.
4. `Organization` has no substantive profile fields. Approved companies return `INCOMPLETE`; a future profile definition and editable fields are needed before `COMPLETE` can be emitted. Approval is not real-world verification.
5. V9 reconstructs historical review time only from UUIDv7 IDs. Legacy non-v7 organizations retain `receivedAt: null`; Luis's current `string` field and `shortDate` call must allow and display an unknown date. Future inserts have a real receipt timestamp.
6. The user clarified that `pendingVerification` means pending company profile checks within the existing approval workflow. It is therefore the persisted pending-company count, while `awaitingReview` includes those companies plus distinct open reported listings. Pending companies appear once as `NEW_ORGANIZATION`; there is no separate `PROFILE_VERIFICATION` item or duplicate approval workflow.
7. `type` responses use approved human-readable labels. The feed accepts both canonical codes and labels such as `Internship` and `Capstone sponsorship`. Luis's `Event` select option must be removed; it returns 400, since Event is not approved. New listing rows default to `RESTRICTED` visibility and only explicit `PUBLIC` records are discoverable.

## Security and data rules

Use a current active account, persisted role and exact organization membership on every request. A moderator may open administrative views but may not bypass student consent or see private student profile fields. Discovery returns only visible, published, unexpired records with an approved/verified source label. Dashboard aggregates use database-side counts, stable ordering and bounded collections. No successful response may include invented listing, response or review data. Existing V1–V6 migrations and audit semantics remain immutable.

## Verification gate

The full task requires source and generated OpenAPI compatibility checks against Luis's actual revision, backend PostgreSQL/Testcontainers tests, a combined isolated worktree, frontend lint/typecheck/tests/build, isolated Compose smoke, HTTP and browser dashboard flows. Until the missing source revisions and data foundations are available, report each unverified gate explicitly rather than claiming the two PRs are integration-ready.
