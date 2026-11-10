# Test and verification evidence

Snapshot date: **2026-10-26**. Baseline main SHA: `1714e5e0f000f9ad4a2ff6a00bf71c2e14fc5b18`. Scope is A1 documentation and evidence reconciliation. No application test, load test, scan, database mutation, deployment, or acceptance session was run as part of this documentation change. Historical reports and observed hosted CI are identified separately.

Results use **Pass**, **Fail**, **Blocked**, or **Not run**. A Pass applies only to the stated procedure and expected result. It does not accept the corresponding feature or proposal indicator. Engineering/acceptance states and all 67 exact issue criteria are maintained in [traceability.md](../project/traceability.md). There is no evidence that any complete I1–I17 target has been accepted.

## Evidence register

<a id="te-00"></a>

### TE-00 — Main source inventory

- **Date / SHA:** observed 2026-10-26; `1714e5e0f000f9ad4a2ff6a00bf71c2e14fc5b18`.
- **Environment:** local Git object inspection of fetched remote refs; no runtime.
- **Procedure:** `git rev-parse origin/main`; `git ls-tree -r --name-only origin/main`; inspect backend source/configuration and test files using `git show origin/main:<path>`.
- **Expected:** distinguish implemented code from package and directory placeholders; identify which runtime tests are supportable.
- **Actual:** repository, Spring Boot/error-handler/test scaffolding and Compose/Docker support exist. Frontend, end-to-end and load-test directories are placeholders. Domain packages contain package-info files; authentication, business flows, migration files and Actions workflow are absent on main.
- **Source:** [immutable main tree](https://github.com/1uismar33r0/Colegio_Connect/tree/1714e5e0f000f9ad4a2ff6a00bf71c2e14fc5b18).
- **Result:** **Pass** for inventory. **Limitations:** code presence is not runtime correctness; all referenced unimplemented feature tests are **Not run**. A planned test ID is not a test file.

<a id="te-01"></a>

### TE-01 — Repository foundation integration

- **Date / SHA:** observed 2026-10-26; PR #85 merged 2026-10-20T23:17:17Z; merge `638efe8522a0df0aec90411beaf9a58829c78428`, present in baseline main.
- **Environment / procedure:** inspect PR state/files, main tree, contribution rules, templates and CODEOWNERS.
- **Expected:** merged repository scaffolding with review/test expectations documented.
- **Actual:** scaffolding and instructions are merged; PR explicitly leaves protection, required CI and future security-sensitive CODEOWNERS paths outstanding. PR review/check arrays are empty.
- **Source:** [PR #85](https://github.com/1uismar33r0/Colegio_Connect/pull/85), [foundation commit](https://github.com/1uismar33r0/Colegio_Connect/tree/638efe8522a0df0aec90411beaf9a58829c78428).
- **Result:** **Pass** for integration inventory. **Limitations:** neither #1 acceptance criterion is verified by documents or merge state; see TE-02.

<a id="te-02"></a>

### TE-02 — Branch enforcement visibility

- **Date / SHA:** observed 2026-10-26; main `1714e5e0f000f9ad4a2ff6a00bf71c2e14fc5b18`; policy is repository state, not encoded in this commit.
- **Environment / procedure:** authenticated GitHub API reads for branches, rulesets and main protection. No direct push, bypass or deliberately failing merge attempted.
- **Expected:** direct pushes blocked; PR requires another member's approval and green required CI; failing check blocks merge.
- **Actual:** branch response says `protected=false`; rulesets returns HTTP 403 with plan restriction; main protection returns HTTP 404 and authenticated account lacks administrator access. No enforceable approval/CI configuration was demonstrated.
- **Source:** [branches API](https://api.github.com/repos/1uismar33r0/Colegio_Connect/branches), [rulesets API](https://api.github.com/repos/1uismar33r0/Colegio_Connect/rulesets), [protection API](https://api.github.com/repos/1uismar33r0/Colegio_Connect/branches/main/protection); captured `branches-flat.json`, `rulesets-live.json`, `protection-live.json`.
- **Result:** **Blocked**. **Limitations:** restricted API visibility prevents claiming that every possible rule was verified absent. Administrator/plan resolution and a safe enforcement verification are required for #1 and #2 acceptance.

<a id="te-03"></a>

### TE-03 — Hosted CI on the CI branch

- **Date / SHAs:** 2026-10-26, jobs complete between 02:58:14Z and 02:58:36Z. PR #89 head is `58089c4d4fdcef14d827dc64727ba02afcb76a22`; push run checks out this head. PR run checks out synthetic merge **`b4740d49699e302f9f9c18b24e0b5887173db27a`**, combining that head with base `1714e5e0f000f9ad4a2ff6a00bf71c2e14fc5b18`.
- **Environment:** GitHub-hosted `ubuntu-latest`, Temurin Java 25, Maven wrapper, Docker; workflow pinned action references are in the source.
- **Procedure:** inspect live run/job metadata and downloaded logs. Backend executes `./mvnw --batch-mode --no-transfer-progress verify`; Container executes `docker compose --env-file .env.example config --quiet` and `docker build --tag colegio-connect:<checked-out-SHA> backend`; CI Gate requires Backend and Container success.
- **Expected:** each job succeeds for push and pull_request, backend has no test failures, Compose validates and image builds.
- **Actual:** **six successful check records across two runs**, with Backend, Container and CI Gate on each. Each Backend log reports **3 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**. One context test and two error-handler tests are reported. Container succeeds; no Compose runtime startup is performed by this workflow.
- **Source:** [push run 40261670993](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/40261670993), [PR run 40261679810](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/40261679810), [workflow at branch SHA](https://github.com/1uismar33r0/Colegio_Connect/blob/58089c4d4fdcef14d827dc64727ba02afcb76a22/.github/workflows/ci.yml), [PR #89](https://github.com/1uismar33r0/Colegio_Connect/pull/89); captured run logs and job JSON.
- **Result:** **Pass** for these jobs on these revisions. **Historical limitation (superseded on 2026-11-04):** at the October 26 observation, #89 was open and the workflow was absent from main. PR #89 merged on October 30, and #88's later CI run `41129604255` covered the database revision. No frontend checks, authenticated workflow tests, 20-scenario acceptance suite, deployed load/security/accessibility tests, OpenAPI artifact upload, or enforced required-status proof follows from these October 26 runs. Main-only image artifact steps were not exercised by these two branch/PR runs.

| Run / event | Backend | Container | CI Gate |
| --- | --- | --- | --- |
| 40261670993 / push | [Pass](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/40261670993/job/121375180310) | [Pass](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/40261670993/job/121375180535) | [Pass](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/40261670993/job/121375376724) |
| 40261679810 / pull_request | [Pass](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/40261679810/job/121375206096) | [Pass](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/40261679810/job/121375206004) | [Pass](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/40261679810/job/121375369396) |

<a id="te-04"></a>

### TE-04 — Historical Docker development verification

- **Date / SHA:** historical 2026-10-21 work; observed 2026-10-26. PR #87 head `233485c2458885456a9dcc593b927e5382d5689e`, merge `1714e5e0f000f9ad4a2ff6a00bf71c2e14fc5b18`. The PR identifies the work; each local runtime log is not independently SHA-stamped.
- **Environment:** author-reported Docker Desktop Engine 29.5.2, Linux containers, Java 25 backend, PostgreSQL 18.6, Mailpit 1.31.5; occupied local ports required `POSTGRES_PORT=5433`, `BACKEND_PORT=18080`.
- **Procedure:** author reports Compose config validation, multi-stage image build, startup and health probes, `/actuator/health`, `/v3/api-docs`, database connectivity, database container restart, persistence check, backend recovery, Maven test/package and log review. Local session reconstruction corroborates the described work; this is historical evidence, not an A1 rerun.
- **Expected:** development services healthy; health returns UP; OpenAPI served; PostgreSQL connection and empty Flyway history available; same database persists across restart.
- **Actual:** reported all checks pass; OpenAPI 3.1.0 served; PostgreSQL system identifier `7699155642147938347` unchanged across restart; Flyway history exists with **zero rows / zero migrations**; backend returns UP. Three Maven tests passed. PR discussion includes teammate corroboration of healthy services.
- **Source:** [PR #87 verification and discussion](https://github.com/1uismar33r0/Colegio_Connect/pull/87), captured historical session summaries. PR is merged although its description retains old draft/stacked wording.
- **Result:** **Pass** (**historical author-reported**, with teammate corroboration). **Limitations:** no fresh clean-machine reproduction; no schema migration correctness; no elapsed recovery measurement or representative application-data no-loss proof. Therefore I16 is **Not run** at its full target. The review API has no formal review record for #87; a checked DoD box is not such a record.

<a id="te-05"></a>

### TE-05 — Historical backend tests and recorded review

- **Date / SHA:** formal approval 2026-10-21T16:37:43Z at `8e51f1f6f45f9d5f69d258cf9cc249da201f1423`; PR #86 merged as `39f6624077082be98a4abd98aaf6444c021be80c`. Observed 2026-10-26.
- **Environment / procedure:** reviewer reports Java 25 test/build execution and stacked Docker integration checks. Inspect PR review and historical local summaries; do not rerun application tests for this documentation change.
- **Expected:** skeleton tests/build pass and another team member reviews the current head.
- **Actual:** Gabriel (`DCGabriel10`) records APPROVED with tests **3/3** passing, successful build and Docker/PostgreSQL/health verification. Local Surefire reports dated 2026-10-21 were recovered, but do not encode a source SHA and cannot independently establish the tested revision.
- **Source:** [PR #86 review](https://github.com/1uismar33r0/Colegio_Connect/pull/86), [test sources at reviewed SHA](https://github.com/1uismar33r0/Colegio_Connect/tree/8e51f1f6f45f9d5f69d258cf9cc249da201f1423/backend/src/test), historical summaries and local Surefire files.
- **Result:** **Pass** for recorded approval and historical reviewer-reported tests. **Limitations:** no CI check records on #86; historical Surefire files are not current results; no OpenAPI artifact publishing evidence. Modern observed CI is separately TE-03.

<a id="te-06"></a>

### TE-06 — Database baseline evidence gap

- **Date / SHA:** PR #88 observed 2026-10-26 at `af24f8987df1a4048af4b9b4789a9582f816fc85`; exact local test execution timestamp is not supplied in the PR body.
- **Environment / procedure:** inspect migration, JPA entities/repositories, Testcontainers configuration, `contextLoads`, PR checks and author verification. Author reports `./mvnw test` using PostgreSQL 18.6 Testcontainers, Flyway and Hibernate `ddl-auto=validate`.
- **Expected:** V1 applies to an empty database in CI and schema matches the approved identity model; entities validate against migration.
- **Actual:** source contains V1, four JPA entities, three repository interfaces (User, EmailCode and RoleAssignment), and real-database test configuration. Author reports 3 tests pass and all four entities validate. **No CI checks and no reviews** are returned for PR #88. Role identifier discrepancy appears in TE-07.
- **Source:** [PR #88](https://github.com/1uismar33r0/Colegio_Connect/pull/88), [database branch tree](https://github.com/1uismar33r0/Colegio_Connect/tree/af24f8987df1a4048af4b9b4789a9582f816fc85/backend/src).
- **Result:** **Historical gap resolved on 2026-10-30.** The October 26 record correctly described an unmerged branch. PR #88 later merged with successful Backend, Container, and CI Gate checks in run `41129604255`; the closed Issue #6 records application of V1 to an empty PostgreSQL 18.6 database and Hibernate validation. This record does not prove future migrations or role-contract changes correct; each requires its own evidence.

<a id="te-07"></a>

### TE-07 — Role identifier contract comparison

- **Date / SHA:** observed 2026-10-26; database SHA `af24f8987df1a4048af4b9b4789a9582f816fc85`; issue #9 snapshot same date.
- **Environment / procedure:** compare #9 task enum identifiers with `organizations/entity/Role.java` and migration role CHECK values.
- **Expected:** role naming contract agrees or a reviewed explicit mapping explains the correspondence without changing approved capabilities.
- **Actual:** #9 specifies `STUDENT`, `FACULTY_OFFICE_DEPT`, `EXTERNAL_COMPANY`, `ADMIN_MODERATOR`; database uses `STUDENT`, `INSTITUTIONAL`, `COMPANY`, `MODERATOR`. No reviewed mapping was found.
- **Source:** [issue #9](https://github.com/1uismar33r0/Colegio_Connect/issues/9), [Role.java](https://github.com/1uismar33r0/Colegio_Connect/blob/af24f8987df1a4048af4b9b4789a9582f816fc85/backend/src/main/java/com/colegioconnect/organizations/entity/Role.java).
- **Result:** **Fail** for literal contract agreement. **Limitations:** this is an unresolved naming/requirements discrepancy, not proof of an exploitable application defect. No implemented authorization flow was tested. Preserve two account types and four roles while resolving the identifiers.

<a id="te-08"></a>

### TE-08 — Frontend branch inventory

- **Date / SHA:** observed 2026-10-26; `b456f772ac7a307f165a0379be025af3d8d2b00d`.
- **Environment / procedure:** `git ls-tree -r --name-only origin/Issue-13-Wireframes`; inspect HTML and frontend manifest availability.
- **Expected:** identify exact progress toward #13's CI build and reviewed wireframes criteria.
- **Actual:** `docs/colegio-connect-wireframes.html` exists; `frontend/.gitkeep` remains. No React/TypeScript app/package/build or evidence of team/advisor review is present in inspected sources.
- **Source:** [wireframe HTML at branch SHA](https://github.com/1uismar33r0/Colegio_Connect/blob/b456f772ac7a307f165a0379be025af3d8d2b00d/docs/colegio-connect-wireframes.html).
- **Result:** **Pass** for inventory. **Limitations:** app-build and review acceptance tests are **Not run**; exploratory HTML does not demonstrate WCAG compliance or production behavior.

<a id="te-09"></a>

### TE-09 — Issue #15 audit foundation reconciliation

- **Date / SHA:** 2026-11-08; authoritative base `e5cc181b3ebcf7b5cb1050547c1d82922068c064`; transaction implementation `132413bb8076371285cdee5a7bd18f6a92871278`; protected query implementation `c0b01a7afc6c8b61eb4ec1cc44259c78658378d8`.
- **Environment:** Windows; Temurin Java `25.0.5+8`; Spring Boot `4.1.2`; Docker Desktop `29.5.2`; PostgreSQL `18.6` through Testcontainers.
- **Procedure:** establish a clean 30-test baseline; execute TDD RED/GREEN for `AuditServiceIntegrationTest` and `AuditControllerIntegrationTest`; run focused audit/error-handler tests; run `mvnw.cmd clean verify`. Inspect PR #99's full history, PR #98 dependency, every fetched remote branch for audit code, and PR #115 notification logging.
- **Expected:** controlled actor/action/target/time rows join the caller's transaction; rollback and missing transaction leave no row; only a current persisted moderator can read bounded newest-first pages; same-session suspension/role revocation denies access; response excludes sensitive/free-form data; endpoint appears in OpenAPI.
- **Actual:** transaction RED failed compilation on missing audit types; endpoint RED produced absent-route/OpenAPI failures. The first endpoint GREEN run exposed method-security denials incorrectly mapped to 500; the narrow 403 mapping corrected the root cause. Final focused run: 13 tests, zero failures/errors/skips. Final pre-documentation clean verification: 41 tests, zero failures/errors/skips; `BUILD SUCCESS`.
- **Source:** [Issue #15 engineering record](../gabriel/security/issue-15-audit-foundation.md), implementation commits above, and [Issue #15](https://github.com/1uismar33r0/Colegio_Connect/issues/15).
- **Result:** **Pass** for the branch foundation and current read contract. **Limitations:** PR/team review, hosted CI, and merge remain pending; no owning business action emits an event yet; final I8 and AC1 are not accepted; database-level append-only enforcement and actor-retention policy remain unresolved.

## Indicator evidence plan

The target column reproduces Table 4 (PDF p.7) with whitespace normalized and indicator IDs normalized from `I. 1` to `I1`. Timing is retained. Each `IND-I<n>` is a planned evidence record. For **every row**, date is the 2026-10-26 planning snapshot, execution SHA/environment are **not selected**, actual result is **no complete target evidence**, and result is **Not run**. Once executed, replace those fields with exact deployed/tested SHA, timestamp, environment and immutable logs. TE-03's three skeleton tests do not satisfy any full indicator.

| Test ID / objective / weeks | Exact approved target | Procedure, environment and evidence required | Canonical issue coverage |
| --- | --- | --- | --- |
| IND-I1 / O1 / 4, 6, 8 | All 6 categories can be created, published, found, filtered, and opened. | Seed authorized users for all publisher types; exercise create/publish/find/filter/open for job, internship, COOP, research position, research participation and capstone. Record Playwright assertions, dataset, source labels and every Figure 4 legal/illegal lifecycle transition. | #17–#22, #54 |
| IND-I2 / O1 / 4, 6, 8 | At least 18 predefined search and filter cases return only matching records (100% correct). | Freeze 18+ named input/expected-record cases before run; compare returned IDs for keyword, category, source, organization and deadline combinations with stable pagination. Retain fixtures and assertion report. | #21, #22, #54 |
| IND-I3 / O1 / 4, 6, 8 | 95% of discovery requests complete within 2 seconds, with a server error rate below 1%, under the test workload. | Run k6 against selected host with 25 concurrent VUs and reproducible synthetic 5,000+ listings, 1,000+ users, 50+ organizations. Record CPU, RAM, duration, dataset seed/counts, p95, server error rate and raw output. Issue #21 additionally says p95 < 2 s; retain that stricter issue criterion. | #4, #21, #25, #56 |
| IND-I4 / O1 / 4, 6, 8 | Save, unsave, and revisit work in all 6 categories. | Verified student saves/unsaves/revisits each category; assert ownership, persisted state and closed/hidden handling. Retain automated report and fixtures. | #23, #54 |
| IND-I5 / O2 / 2, 4, 6, 8 | 100% of registration attempts with non-UPRM emails and invalid or expired codes are rejected, and verification requests are rate limited. | Integration cases for non-UPRM addresses, invalid/expired/reused codes and rate/resend/attempt limits, with deterministic clock and fake email sender. Retain rejection/status and rate-limit assertions; no external identity provider. | #7, #11, #14, #54 |
| IND-I6 / O2 / 2, 4, 6, 8 | 100% of at least 10 negative-authorization tests pass (e.g., unapproved company publishing, cross-organization access, suspended-account actions, non-team member accepting sponsorship). | At least ten named server-side negative cases with role/status/org/ownership/team fixtures; test actual endpoints, unauthenticated/unverified callers and consent gates. Retain case count, expected 403/404 results and database no-change assertions. | #8–#10, #12, #16, #33, #52, #54 |
| IND-I7 / O2 / 4, 6, 8 | 0 private student fields appear in API responses during browsing, searching, saving, or unauthorized requests. | Define private fields; scan every listing/user response for browse/search/save and unauthorized cases across roles/orgs. Include chosen-field response snapshots and no company student-search access. Retain endpoint inventory and zero-leak report. | #21, #31, #33, #35, #54 |
| IND-I8 / O3 / 6, 8 | 100% of approvals, role changes, account restrictions, moderation actions, and sponsorship acceptances create an audit record with actor, action, target, and time. | Trigger every action class; inspect actor/action/target/time in same-transaction audit rows, with rollback/no-row tests for failed actions. Retain action-to-audit matrix and records with synthetic data. | #10, #15–#16, #19, #37, #39, #52, #54 |
| IND-I9 / O3 / 6, 8 | The report → hide → clear or remove workflow works end to end, including in a test environment with no UPRM office or department organization. | E2E branches for clear and remove, report threshold, moderation queue, restrictions and audit; repeat with zero UPRM office/department organizations. Retain fixtures, state assertions and recordings. | #17, #19, #37, #39, #54 |
| IND-I10 / O4 / 6, 8 | Company-originated route works end to end; the company receives only the data the student chose to share. | Approved company publishes call; student responds choosing a subset; assert company sees exactly shared fields and no other student's data. Retain E2E report and demo evidence. | #31, #33, #42, #54 |
| IND-I11 / O4 / 8 | Student-originated route works end to end; 0 private contact fields are exposed before acceptance. | Team publishes reviewed project; approved company expresses interest; scan public/company API payloads before acceptance and after decline; authorized member accepts; verify release/notifications/audit. Deny non-team member. Retain E2E and field-scan evidence. | #48, #50, #52, #54 |
| IND-I12 / O4 / 2, 6, 8 | 100% of required notifications (verification code, Express Interest received, request accepted or declined) are sent in test runs. | Execute each event, inspect fake-sender integration results and provider delivery logs with event/message correlation; count attempted/required and delivered/sent outcomes separately. Include approval notifications required by issue tasks. | #7, #11, #16, #40, #50, #52, #54 |
| IND-I13 / O5 / 8 | At least 20 core workflow scenarios complete with 0 unhandled server errors, and all required automated tests pass in CI. | Define 20+ deployed end-to-end scenarios covering I1–I17 and all roles; correlate server logs with run IDs; retain CI required-check results and reviewer/deployed SHA. Three skeleton tests are insufficient. | #2, #12, #54, #60 |
| IND-I14 / O5 / 4, 6, 8 | 0 unresolved High or Critical findings in the automated security scan. | OWASP ZAP authenticated context against deployed build; cover access paths, triage findings, rerun fixes and retain full report plus zero-open High/Critical list. ASVS L1 review supplements the scan. | #8, #26, #56 |
| IND-I15 / O5 / 4, 6, 8 | 0 unresolved Critical or Serious accessibility findings (WCAG 2.2 AA), and all primary workflows can be completed using only the keyboard. | axe-core on all primary pages/states plus manual keyboard task checklist for four roles, focus, labels, contrast and errors. Retain versions, findings, resolutions and completed manual checklist; automated axe alone is insufficient. | #13–#14, #20, #22, #28, #58 |
| IND-I16 / O5 / 8 | After a restart, the service is healthy within 5 minutes with no data loss. | Populate representative synthetic application records, snapshot counts/checksums, restart selected environment, time until healthy, compare records and backup/restore/persistent-volume behavior. Retain timestamps, health logs and before/after data proof. Issue #44 additionally requires < 5 min. | #3, #4, #44, #56 |
| IND-I17 / O5 / 8 | At least 6 representative users complete role-appropriate tasks with at least 80% overall task success. | Advisor-reviewed plan, consent, at least six representative participants and role task scripts. Record successes/attempts and aggregate ≥80%; anonymize user notes. Retain methodology, counts, issues/fixes, advisor review and role user guide. | #46, #58, #60 |

## Issue acceptance execution records

Every `TC-<canonical>-AC<n>` in [the central register](../project/traceability.md#canonical-issue-register) has the following initial record. This common record explicitly applies to all **67** IDs; it avoids treating issue acceptance as passed merely because a dependency or partial observation passed.

| Field | Initial value / required completion |
| --- | --- |
| Date | 2026-10-26 planning snapshot; record actual execution timestamp when run. |
| SHA | Not selected; pin implementation and test revisions, including synthetic PR merge when applicable. |
| Environment | Not selected; record local/CI/deployed environment, versions, database fixture and configuration. |
| Procedure | Exercise every clause of that ID's exact criterion using its issue tasks and applicable IND-I procedures. For artifact/review/delivery criteria inspect the actual artifact and dated approval/delivery record; for enforcement obtain administrator-visible configuration and safe rejection evidence. |
| Expected | Exact AC text in the register, including every compound clause. DoD additionally requires another member's review, CI success and OpenAPI update if API changed. |
| Actual | No complete, independent acceptance record for this ID. Partial observations TE-01–TE-08 are attributed in the corresponding issue section. |
| Source | Both linked issue bodies, applicable approved PDF pages, immutable implementation links, and the recorded evidence IDs. |
| Result | **Not run** as a complete acceptance procedure; #1/#2 enforcement and #6 CI are **Blocked** as detailed in TE-02/TE-06. |
| Limitations | Must not substitute scaffolding, dependency presence, task checkbox, merge, or an unrelated branch's green checks for the required observation. |

For #5 AC2, explicitly capture `/v3/api-docs` response **and** CI artifact publication/download at the tested SHA. For #6 AC1, start an empty PostgreSQL 18 database in CI and retain V1 application/validation logs. For #13 AC2, retain both team and advisor review records. For #60 AC1, retain report/presentation delivery date before the end of Fall 2026; drafting is not delivery.

<a id="te-09"></a>

## TE-09 — Coverage method and documentation validation

- **Date / SHA:** 2026-10-26; documentation branch based on main `1714e5e0f000f9ad4a2ff6a00bf71c2e14fc5b18`. The commit containing this record identifies the documentation revision; it is distinct from the application baseline and historical test SHAs.
- **Environment:** native PowerShell/text comparison with captured issue JSON and Git; no permanent validation dependency and no Windows helper scripts.
- **Procedure:** fetch all open issue pages; exclude PR entries; group identical titles into pairs; sort each pair by number; extract each body between `### Acceptance criteria` and `### Definition of done`; compare text case-sensitively; count checkbox entries; compare each entry to exactly one numbered register AC and stable test ID. Compare the complete descriptive paragraphs, tasks and DoD separately, retaining checkbox differences and assignees. Verify O1–O5, I1–I17, M1–M4 and exactly four approved figure references against the PDF; inspect links and run `git diff --check` on documentation changes.
- **Expected:** 42 pairs, 84 distinct issues, 67 canonical AC entries / 134 across both copies, 67 stable TC IDs, no omitted acceptance clause, 17 indicator records, unchanged application source.
- **Actual:** all 42 pairs have identical acceptance wording and descriptions; #5/#32 have five task checkbox differences; all other paired bodies match. Only canonical #5/#6 have GitHub assignee `Massinl`. The register contains every exact description, criterion and task. Final checks passed for 42 sections, 67 criteria, 167 tasks, 17 indicators, all required files, local links/anchors, source references, checksum, documentation scope and secret patterns; see the [A1 execution record](../superpowers/plans/2026-10-26-a1-documentation-baseline.md).
- **Source:** dated GitHub issue sources linked in each register section; captured `issue-pairs.json` / `issues-flat.json`; approved PDF checksum in traceability; this documentation diff.
- **Result:** **Pass** for documentation coverage and consistency; independent review findings corrected and checks rerun. Team review/integration remain pending. **Limitations:** this is documentation coverage, not feature correctness or indicator acceptance. Snapshots are working audit inputs; GitHub/PDF links provide the reviewable sources. GitHub metadata may change after capture.

Reproduction outline (run from an audit workspace with a freshly captured issue export; no repository script needs installation):

```powershell
$pages = gh api --paginate --slurp 'repos/1uismar33r0/Colegio_Connect/issues?state=open&per_page=100' | ConvertFrom-Json
$issueData = @($pages | ForEach-Object { $_ | ForEach-Object { $_ } })
$issues = @($issueData | Where-Object { -not $_.pull_request })
$groups = @($issues | Group-Object title)
$criterionCount = 0
foreach ($group in $groups) {
    if ($group.Count -ne 2) { throw "Expected pair: $($group.Name)" }
    $pair = @($group.Group | Sort-Object number)
    $canonical = [regex]::Match($pair[0].body, '(?s)### Acceptance criteria\s*(.*?)\s*### Definition of done').Groups[1].Value
    $alias = [regex]::Match($pair[1].body, '(?s)### Acceptance criteria\s*(.*?)\s*### Definition of done').Groups[1].Value
    if ($canonical -cne $alias) { throw "Acceptance differs: $($pair.number -join ', ')" }
    $criterionCount += [regex]::Matches($canonical, '(?m)^- \[[ x]\] ').Count
}
[pscustomobject]@{ Issues = $issues.Count; Pairs = $groups.Count; CanonicalAC = $criterionCount; BothCopiesAC = 2 * $criterionCount }
```

At the captured snapshot this produces **84 / 42 / 67 / 134**. Recheck before later acceptance because issue bodies and repository state can change. Exact-document comparison must also check all criterion text, not only these counts.

<a id="te-11"></a>

## TE-11 - Issue #12 negative-authorization harness reconciliation

- **Date / SHA:** 2026-11-07; authoritative base `e5cc181b3ebcf7b5cb1050547c1d82922068c064`; harness implementation `92fadc467db5a85dcea5c03fc3456e6d38936999` on `gabriel/issue-12-negative-auth-reconciliation`.
- **Environment:** Windows; Temurin Java `25.0.5+8`; Spring Boot `4.1.2`; Maven Wrapper; Docker Desktop `29.5.2`; PostgreSQL `18.6` through Testcontainers; synthetic users and organizations only.
- **Procedure:** compare original PR #100 head `52352763d0f01e1b8bb9f87e1ec843bb357c2c6b` with current main and merged PR #111/#112 contracts; refactor existing authentication and authorization-policy tests to inherit the test-only harness; verify the missing harness fails compilation first; run `mvnw.cmd -Dtest=AuthIntegrationTest,AuthorizationPolicyIntegrationTest test`; run `mvnw.cmd clean verify`; inspect the production-source diff.
- **Expected:** the two existing consumer classes use reusable user/status/organization/role/session fixtures; all 20 focused tests and the complete backend suite pass; production source, migrations, endpoints, and OpenAPI remain unchanged.
- **Actual:** RED failed for the missing `NegativeAuthorizationTestHarness` and intended helper symbols. After implementation, `AuthIntegrationTest` passed 12 and `AuthorizationPolicyIntegrationTest` passed 8, with zero failures/errors/skips. Clean verification passed 30 tests with zero failures/errors/skips and Maven `BUILD SUCCESS`. Only three backend test files changed in the implementation commit.
- **Source:** [Gabriel's Issue #12 engineering record](../gabriel/security/issue-12-negative-authorization.md), [canonical Issue #12](https://github.com/1uismar33r0/Colegio_Connect/issues/12), [original PR #100](https://github.com/1uismar33r0/Colegio_Connect/pull/100), [reconciliation PR #118](https://github.com/1uismar33r0/Colegio_Connect/pull/118), and implementation commit `92fadc467db5a85dcea5c03fc3456e6d38936999`.
- **Result:** **Pass** for branch-local harness implementation and current supported-case verification. PR #118 run `42445314730` passed Backend, Frontend, Container, and CI Gate at head `203a424`. **Limitations:** AC1 requires merge and teammate review is pending; later documentation-only heads must retain green CI. This does not accept final indicator I6: business endpoints for publishing, ownership, consent, moderation, and sponsorship are not yet available on the inspected baseline.

<a id="te-12"></a>

## TE-12 — Gabriel authorization, audit, and company consolidation

- **Date / revision:** 2026-11-10; `main` base `e5cc181b3ebcf7b5cb1050547c1d82922068c064`; combined #118/#119/#120 code and upgrade test at `5c29336aca5df4804bd84149d6f5b84da96fcfc0`. This is the local surviving #119 branch before publication, not a main merge.
- **Environment:** Windows; Temurin Java `25.0.5+8`; Spring Boot `4.1.2`; Maven Wrapper; Docker Desktop `29.5.2`; PostgreSQL `18.6` through disposable Testcontainers; Node `24.16.0`; synthetic data.
- **Procedure / result:** `backend/.\mvnw.cmd -Dtest=AuditServiceIntegrationTest,AuditControllerIntegrationTest test` passed 11/11 before consolidation; the #118/#119 focused four-class run passed 31/31; the first #118/#119/#120 full `backend/.\mvnw.cmd clean verify` passed 63/63. A new isolated Flyway test applied V1–V3, inserted synthetic identity/organization/role rows, applied V4–V5, and verified unchanged identifiers/roles, pending status, new decision column, and audit insert; its focused run passed 1/1. Final `backend/.\mvnw.cmd clean verify` at `5c29336` passed **64/64**, zero failures/errors/skips, `BUILD SUCCESS`.
- **Frontend and infrastructure:** `npm.cmd ci`, `npm.cmd run lint`, `typecheck`, `test` (22/22), `api:gen`, and `build` exited 0. The generated schema's normalized blob matched the committed schema, so no API type change was staged. `docker compose --env-file .env.example config --quiet` and `docker build --tag colegio-connect:gabriel-consolidation-5c29336 backend` exited 0. The image build skips tests; Maven verification above is separate.
- **Local formatting limitation:** The unmodified `npm.cmd run format:check` failed on Windows line endings and an ignored `frontend/test-results/.last-run.json`. A read-only Prettier check with `--end-of-line auto` and both `.prettierignore` and repository `.gitignore` passed. Linux hosted CI is required for the unchanged canonical formatting command. `npm ci` also reported 11 moderate dependency advisories; no dependency change was made in this consolidation.
- **Coverage and limits:** Tests exercise real server sessions, 403 denial, persisted moderator revocation, audit persistence/rollback, company approval/rejection, row-lock concurrency, pending queue, clean-schema migration through V5, and a V1–V3 upgrade. The audit-failure rollback test substitutes the audit service and does not simulate a database failure after an actual insert. There is no complete company registration/publishing flow or universal role-revocation policy. `/api/auth/me` still omits roles. Hosted CI at the consolidated head and another team member's review are pending; no issue or indicator is accepted by this local run alone.

- **November 10 review correction:** A reviewer found that timestamp-only ordering could repeat or skip events across offset pages when timestamps tie. At code commit `80213929a0253ce99e25b5ddc48733dbcc4eca6c`, a new PostgreSQL/MockMvc test inserted three fixed IDs at an identical timestamp and failed as expected (`...003` expected on page 0, `...001` returned). Ordering by `occurred_at DESC, id DESC` fixed the case. `AuditControllerIntegrationTest` then passed 9/9; `backend/.\mvnw.cmd clean verify` passed **65/65**, zero failures/errors/skips, `BUILD SUCCESS`. Frontend lint, typecheck, 22/22 tests, and production build also passed at this working tree. The earlier `64/64` record remains the pre-fix checkpoint. No throughput or latency measurement follows from these functional tests.
