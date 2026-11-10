# Project status

**Dashboard reconciliation checkpoint (2026-11-10):** Main `18ae2ba` now includes Cristian's #124 role API and #125 current roles in login/`/me`. The main-based `gabriel/issue-122-role-dashboard-main` branch adds five dashboard read APIs without importing Gabriel's competing #123 role service, bootstrap, or V6 migration. This is **In progress** on a feature branch, not merged or accepted. Read foundations do not supply listing/response/report write journeys, company applicant registration, or a browser rendering result. See [TE-15](../testing/test-evidence.md#te-15), [contract](../roles/dashboard-api-contract.md), and [role reconciliation](../roles/reconciliation-123-124-125.md). The older main snapshot below is historical.

**Last updated:** 2026-11-04. **Original evidence observed:** 2026-10-26 (UTC; collection began approximately 04:35Z). **Update evidence observed:** 2026-11-04 (live GitHub PR and issue records). **Current baseline:** [`fc06e936a66921afa78c37e65b5be8dfe815a98d`](https://github.com/1uismar33r0/Colegio_Connect/commit/fc06e936a66921afa78c37e65b5be8dfe815a98d), `origin/main` fetched on November 4. The October 26 statements below remain dated historical observations unless explicitly updated here.

## Purpose and current condition

Colegio Connect is an independent UPRM student prototype for discovering six opportunity categories, publishing through verified institutions and approved companies, controlled student responses, moderation, and two-sided capstone sponsorship. Companies must never browse the student population; disclosure requires a student's response or a team's accepted interest request. The [approved proposal and complete requirement map](traceability.md) define the scope.

**Current milestone: M1 — Access foundation, In progress.** Infrastructure foundations exist, but M1 is not complete: no working registration, verification, login, authorization, or React application has been established on main. No hosted application is verified. No proposal indicator is accepted at its full target. Calendar sprint boundaries cannot be established because the proposal approval date is unknown.

## Evidence and interpretation

The checkpoint uses authenticated live GitHub REST/CLI queries for all Issues, PRs, comments, reviews, branches, Actions runs/jobs/logs, plus freshly fetched Git objects and source inspection. All five prior exports (`issues.json`, `pull-requests.json`, `pr-89.json`, `actions.json`, `branches.json`) were readable JSON; their local modification timestamps were October 25 at 23:38 UTC-04. Those timestamps are provenance clues, not proof of API capture time. Live queries supersede them here. Historical local reports are explicitly labeled in [test evidence](../testing/test-evidence.md) and [incident history](../bugs/bug-log.md).

Live backlog: **84 open Issues, 42 exact-title pairs**, with identical acceptance text within each pair. The lower-numbered issue is the documentation's canonical reference, not a GitHub deduplication action. The [register](traceability.md) preserves every alias and criterion. Open issue state does not prove that all its work is absent.

| Engineering state | Meaning |
| --- | --- |
| Not started | No implementation found in inspected evidence; do not infer private work does not exist. |
| In progress | Partial implementation or planning activity exists. |
| Implemented but unverified | Relevant implementation exists without adequate executed verification. |
| Verified on branch | The stated checks passed on a specific branch/revision; scope and remaining criteria are still explicit. |
| Merged; acceptance pending | Work is on main, but one or more acceptance/DoD items remain unverified. |
| Accepted | Every criterion and DoD item has linked evidence and review. |
| Blocked | The named dependency prevents the next acceptance step. |

Always store location separately: main, feature branch, documentation branch, deployed, or none. These states describe the named item; a verified build does not make its whole issue accepted.

## Implemented and verified baseline

| Area / canonical Issue | Engineering state | Location | Evidence and remaining acceptance |
| --- | --- | --- | --- |
| Repository foundation [#1](https://github.com/1uismar33r0/Colegio_Connect/issues/1) | Merged; acceptance pending | main, PR #85 | Monorepo, templates, CODEOWNERS, contributor/environment guidance. Protection and review enforcement remain unverified/incomplete. |
| Backend foundation [#5](https://github.com/1uismar33r0/Colegio_Connect/issues/5) | Merged; acceptance pending | main, PR #86 | Java 25, Spring Boot 4.1.2, module packages, error handling, Actuator and OpenAPI. The issue is closed and later merged CI runs verify the backend; the remaining recorded acceptance evidence is publication of the OpenAPI CI artifact. |
| Docker development [#3](https://github.com/1uismar33r0/Colegio_Connect/issues/3) | Merged; acceptance pending | main, PR #87 | Non-root Java 25 container; PostgreSQL 18.6; Mailpit; health checks; persistent volume. PR documents startup, database connection and restart checks; Cristian independently confirmed all services healthy. Full DoD/CI integration still needs reconciliation. |
| CI / Dependabot [#2](https://github.com/1uismar33r0/Colegio_Connect/issues/2) | Merged; acceptance pending | main, PR #89 | Backend, Container and CI Gate passed on the merged PR; the workflow is now present on main. Frontend checks, demonstrated main image artifact retention, and enforceable branch protection remain separate acceptance work. |
| Identity database [#6](https://github.com/1uismar33r0/Colegio_Connect/issues/6) | Implemented | main, PR #88 | V1 creates users, organizations, role assignments and email codes; JPA and PostgreSQL 18.6 Testcontainers are merged. PR #88's Backend, Container and CI Gate checks passed; the closed issue records migration application to an empty database and Hibernate validation. |
| Frontend [#13](https://github.com/1uismar33r0/Colegio_Connect/issues/13) | In progress | `Issue-13-Wireframes` | Luis's HTML wireframe is design exploration. `frontend/` on main remains a placeholder, with no React manifest, router or API client. No wireframe PR found. |
| Hosting [#4](https://github.com/1uismar33r0/Colegio_Connect/issues/4) | In progress | research and documentation | [Proposed decision](../decisions/0001-hosting.md); measurements, budget, owner, provider and deployment evidence remain unresolved. No provisioned resources reported by Gabriel; provider accounts were not independently inventoried. |
| Registration, login, authorization, notifications, audit; publishing/discovery; responses/moderation/sponsorship | Not started | no business implementation found on main or inspected feature branches | Package names and dependencies are scaffolding. They do not establish functioning product flows. See per-criterion register. |
| A1 documentation foundation | Verified on branch | `docs/a1-verified-project-baseline` | This change supplies the source, register, logs and policy. Documentation checks pass and independent review findings are corrected, as recorded in the [A1 execution record](../superpowers/plans/2026-10-26-a1-documentation-baseline.md); team review/integration remain required. |

## PRs, branches and integration

| PR | Author | Live state / revision | Review and checks |
| --- | --- | --- | --- |
| [#85](https://github.com/1uismar33r0/Colegio_Connect/pull/85) | Gabriel | Merged 2026-10-20; merge `638efe8` | No recorded formal GitHub review; Cristian's merge comment exists. Does not complete #1. |
| [#86](https://github.com/1uismar33r0/Colegio_Connect/pull/86) | Cristian | Merged 2026-10-21; merge `39f6624` | Gabriel's APPROVED review at head `8e51f1f`; no contemporaneous hosted CI. |
| [#87](https://github.com/1uismar33r0/Colegio_Connect/pull/87) | Gabriel | Merged 2026-10-21; merge `1714e5e` | Cristian's runtime verification comment; no formal GitHub review object. Body retains obsolete stacked/draft wording; live merged state governs. |
| [#88](https://github.com/1uismar33r0/Colegio_Connect/pull/88) | Cristian | Merged 2026-10-30; merge `ff504e1` | Backend, Container and CI Gate succeeded on run `41129604255`; no formal GitHub review object is recorded. |
| [#89](https://github.com/1uismar33r0/Colegio_Connect/pull/89) | Gabriel | Merged 2026-10-30; merge `76918a8` | Backend, Container and CI Gate succeeded on run `40600375253`; this workflow is present on main. |
| [#92](https://github.com/1uismar33r0/Colegio_Connect/pull/92) | Dependabot | Merged 2026-10-30; merge `afc10c6` | Spring Boot parent updated to 4.1.2; Backend, Container and CI Gate succeeded on run `41130212161`. |
| [#93](https://github.com/1uismar33r0/Colegio_Connect/pull/93) | Dependabot | Merged 2026-10-30; merge `ad5fc2a` | springdoc updated to 3.2.0; Backend, Container and CI Gate succeeded on run `41130751172`. |

Other remote tips: repository foundation `28f4b68`, backend foundation `8e51f1f`, Docker `233485c`, wireframe `b456f77`. They are visible contributions, not evidence that their authors are currently coding on those branches. Local `main` was older (`638efe8`); A1 uses verified `origin/main`, not that stale local branch. Original Docker and CI worktrees were clean before A1 and are preserved.

Gabriel's [October 26 UTC comment on #88](https://github.com/1uismar33r0/Colegio_Connect/pull/88#issuecomment-6408234838) was accurate at that time: CI was implemented on #89 and review/merge were pending. The November 4 update above supersedes only its then-pending merge status.

## Architecture, security and integration risks

| Risk or gap | Consequence / next verification | Owner |
| --- | --- | --- |
| Role and schema evolution after the merged V1 baseline | Preserve V1 immutably; validate any new migration, JPA mapping, and role contract against an empty PostgreSQL 18 database. | Cristian + Gabriel |
| #9 role identifiers differ from #88 SQL/Java enum | Agree mapping before authorization/API contracts. Current enum: `STUDENT`, `INSTITUTIONAL`, `MODERATOR`, `COMPANY`; issue uses different institutional/company/admin identifiers. This is an integration risk, not a proven runtime failure. | Cristian + Gabriel; Luis consumes contract |
| Branch endpoint reports `protected: false` | CI reporting does not block merges by itself. Rulesets query returned HTTP 403 (plan/visibility restriction); protection query returned 404; Gabriel lacks admin permission. Luis must establish effective review/check enforcement and available plan features. | Luis + Gabriel, #1 |
| No implemented authentication/authorization | Sprint 1 and any release with real users/data are incomplete. Specify sessions, CSRF, verification limits and negative tests. | Cristian + Gabriel |
| React/API contracts absent | Agree account, registration, verification, login, role/error DTOs and OpenAPI examples so Luis can build against a stable contract. | Cristian + Luis |
| Provider sizing, budget, owner and rollback unapproved | Hosting cannot be represented as delivered; migrations can outlive a failed application release. | Gabriel + team/budget owner |
| Advisor comments, approval date and project webpage access absent | Cannot date sprints, answer proposal reviews, or claim complete midterm documentation. Private repository access for the professor also needs arranging. | Luis coordinates; all contribute |

## Sprint progress against the approved milestone outcomes

| Sprint | Milestone | State | Evidence / missing outcomes |
| --- | --- | --- | --- |
| 1, Weeks 1–2 | M1: Access foundation | In progress | Foundations merged; CI/database on separate branches; exploratory wireframes. Running React, integrated database, email verification, login, roles, first negative tests and hosting are incomplete. |
| 2, Weeks 3–4 | M2: Publishing and discovery | Not started | Company approval, six-category publishing/lifecycle, search/filter/save, k6/ZAP/axe results not established. Contract/test design may proceed while access work finishes. |
| 3, Weeks 5–6 | M3: Responses and moderation | Not started | Responses, privacy, moderation/audit, notifications, company capstone route and usability preparation remain. Historical infrastructure restart evidence is not milestone acceptance. |
| 4, Weeks 7–8 | M4: Sponsorship and final acceptance | Not started | Student projects, consent-based interest, all I1–I17 results, user guide, final report and presentation remain. |

Week 1 starts on proposal approval (PDF p.23). No calendar sprint dates or completion percentages are inferred.

## Team contributions and current visible work

| Developer | Established contribution | Current visible work | Next responsibility |
| --- | --- | --- | --- |
| Gabriel (`DCGabriel10`) | PR #85 repository foundation; PR #87 Docker; recorded review of #86; merged PR #89 CI/Dependabot; hosting investigation and A1 documentation | A1 documentation; #4 remains planning | Support security review and negative testing, coordinate protection, measure hosting candidates and gather report evidence. |
| Cristian (`Massinl`) | Backend foundation #86; Java 21→25 project setting; merged database baseline #88; merges/integration of #85–#87 and Docker runtime confirmation | Review A1; define account/role API with Luis; implement #7/#8/#9 and support #10/#11. |
| Luis (`1uismar33r0`) | Initial repository commit; HTML wireframe `b456f77`; #88 CI dependency comment; repository ownership | #13 exploratory wireframes; no verified React implementation | Review A1/#89; establish repository rules/access; implement #13/#14 against agreed API; coordinate professor feedback, report and webpage. |

These are observed artifacts and assigned responsibilities, not claims about unobserved work or hours. Proposal Table 17 and issue assignments sometimes differ; [traceability](traceability.md) records the discrepancy rather than silently reassigning work.

## Ordered next actions

These are proposed task priorities within the approved scope, not new assignments imposed on teammates. Lower numbers are dependency order; explicitly parallel work can start together.

| Priority / action | Owner / canonical Issue | Why next and dependencies | Completion and evidence required | Current blocker / parallel work |
| --- | --- | --- | --- | --- |
| P0.1 Review A1 | Gabriel; Cristian/Luis review; #1 and #60 documentation support | Gives all work one source/evidence baseline | Documentation-only diff, PDF checksum, complete criterion coverage, link checks and team review | Team review pending; parallel with all code work |
| P0.2 Extend CI coverage | Gabriel; Cristian/Luis review; #2 | Shared verification is on main | Frontend checks, an OpenAPI artifact, image-artifact evidence, and enforceable repository checks where available | Existing CI merged; further coverage and owner-level enforcement remain |
| P0.3 Evolve database safely | Cristian; Gabriel reviews/tests; #6 | Accounts depend on the merged schema | New migrations, role-contract review, and current PostgreSQL 18 verification when schema changes | V1 baseline is merged; later authorization changes require coordinated review |
| P0.4 Enforce repository workflow | Luis owner; Gabriel support; #1/#2 | Passing checks alone do not enforce safe integration | Evidence of required PR/review/CI Gate, stale approval handling where supported, force-push/deletion restrictions, and a negative enforcement check | Admin permission/plan capability; investigate parallel with CI review |
| P0.5 Agree initial API contract | Cristian + Luis; Gabriel security review; #5/#7/#8/#9/#13/#14 | Unblocks React without waiting for all business logic | Reviewed OpenAPI/schema/examples covering account types, role mapping, registration/code errors, sessions, CSRF and permissions | Shared contract not agreed; proceed now alongside integrations |
| P1.1 Complete access workflows | Cristian #7/#8/#9; Luis #13/#14; Gabriel #12 and security review | Required M1 outcomes; depends on schema and API contract | Working register/verify/login/screens; protected routes with role/ownership/status checks; invalid/expired code and negative-authorization evidence | APIs/security absent; UI mocks and test design can proceed in parallel |
| P1.2 Select and verify hosting | Gabriel #4; Cristian DB support; Luis review | Deploy only a demonstrable baseline; depends on CI/database, measured sizing, approved owner/budget and security scope | Approved ADR; deployed tested SHA; HTTPS/private DB/secrets verified; migration, startup and recovery evidence; detailed runbook | No approved provider/budget/resources; measurements and research parallel now |
| P1.3 Assemble midterm evidence | Luis coordinates #60; all supply artifacts | November 10 report needs verifiable results and review responses | [Evidence index](../reports/midterm/evidence-index.md), actual advisor comments/responses, updated designs, test records, genuine preliminary measurements, screenshots and accessible webpage/repo | Feedback/measurements/webpage missing; collection proceeds immediately |
| P2.1 Begin publishing/discovery implementation | Gabriel #16/#17/#19; Cristian #18/#20; Luis #21/#22/#23; team #25/#26/#28 | Depends on access control, organizations and contracts; M1 is not assumed complete | Company approval and authorized publishing/search/filter/save, with relevant I1–I4/I6/I7 evidence and first scans | Access foundations incomplete; design and datasets can proceed in parallel |

## Decisions and information still needed

Obtain the proposal approval date, actual advisor/professor review comments, agreed API/role identifiers, hosting and email ownership, monthly spending ceiling and funded period, region, resource measurements, repository enforcement capability, and professor access to the private repository/project webpage. See [deployment prerequisites](../deployment.md) and [midterm evidence index](../reports/midterm/evidence-index.md) for concrete evidence to collect.
