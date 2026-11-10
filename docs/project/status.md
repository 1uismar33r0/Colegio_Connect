# Project status

**Last updated:** 2026-11-10. **Original evidence observed:** 2026-10-26 (UTC; collection began approximately 04:35Z). **Current baseline:** [`e5cc181b3ebcf7b5cb1050547c1d82922068c064`](https://github.com/1uismar33r0/Colegio_Connect/commit/e5cc181b3ebcf7b5cb1050547c1d82922068c064), `origin/main` fetched November 10. Earlier run counts and issue inventories retain their dated historical meaning.

## Purpose and current condition

Colegio Connect is an independent UPRM student prototype for discovering six opportunity categories, publishing through verified institutions and approved companies, controlled student responses, moderation, and two-sided capstone sponsorship. Companies must never browse the student population; disclosure requires a student's response or a team's accepted interest request. The [approved proposal and complete requirement map](traceability.md) define the scope.

**Current milestone: M1 — Access foundation, In progress.** Main includes registration, `/api/auth/verify`, `/resend`, login/sessions, V2 verification-code limits, integration tests, Cristian's #111 authorization foundation, Luis's React foundation, and #112's CSRF/OpenAPI correction. M1 remains incomplete: `/api/auth/me` omits roles needed by the frontend, the complete register–verify–login browser flow is unverified, and no hosted application is verified. No proposal indicator is accepted at its full target. Calendar sprint boundaries cannot be established because the proposal approval date is unknown.

## Evidence and interpretation

The checkpoint uses authenticated live GitHub REST/CLI queries for all Issues, PRs, comments, reviews, branches, Actions runs/jobs/logs, plus freshly fetched Git objects and source inspection. All five prior exports (`issues.json`, `pull-requests.json`, `pr-89.json`, `actions.json`, `branches.json`) were readable JSON; their local modification timestamps were October 25 at 23:38 UTC-04. Those timestamps are provenance clues, not proof of API capture time. Live queries supersede them here. Historical local reports are explicitly labeled in [test evidence](../testing/test-evidence.md) and [incident history](../bugs/bug-log.md).

The October 26 backlog snapshot contained **84 open Issues and 42 exact-title pairs**. The lower-numbered issue is the documentation's canonical reference, not a GitHub deduplication action. The [register](traceability.md) preserves aliases and criteria; individual live issue states require rechecking before closure.

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
| CI / Dependabot [#2](https://github.com/1uismar33r0/Colegio_Connect/issues/2) | Verified on branch | main foundation plus PR #107 | Backend, Frontend, Container and CI Gate exist; PR #107 adds every-push, every-PR and manual triggers. Push and PR runs passed at `15d0469` before #111/#112 merged; current-main checks, teammate review, and enforceable branch protection remain. |
| Identity database [#6](https://github.com/1uismar33r0/Colegio_Connect/issues/6) | Implemented | main, PR #88 | V1 creates users, organizations, role assignments and email codes; JPA and PostgreSQL 18.6 Testcontainers are merged. PR #88's Backend, Container and CI Gate checks passed; the closed issue records migration application to an empty database and Hibernate validation. |
| Frontend [#13](https://github.com/1uismar33r0/Colegio_Connect/issues/13) | Merged; acceptance pending | main, PR #106 and #112 | Vite/React/TypeScript, router and guards, generated client, accessible layout components, 22 tests, and Frontend CI are merged. #112 corrected the CSRF client and removed the false CSRF query parameter from committed OpenAPI. `/me` role data and full user workflows remain unresolved; this is not I15 acceptance. |
| Hosting [#4](https://github.com/1uismar33r0/Colegio_Connect/issues/4) | In progress | research and documentation | [Proposed decision](../decisions/0001-hosting.md); measurements, budget, owner, provider and deployment evidence remain unresolved. No provisioned resources reported by Gabriel; provider accounts were not independently inventoried. |
| Registration, login, authorization, notifications, audit; later workflows | In progress | registration/verification/login and #111 authorization on main; #115/#118/#119/#120 open | `/verify`, V2 code limits, and registration tests exist on main. #111 adds role authorities, organization/approval policy helpers, and per-request active-status checks. #115 notification changes, Gabriel's audit/company work, and Luis's #116 screens are unmerged. Publishing, discovery, responses, moderation, and sponsorship remain incomplete. |
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
| [#106](https://github.com/1uismar33r0/Colegio_Connect/pull/106) | Luis | Merged 2026-11-07; merge `a128824` | Backend, Frontend, Container and CI Gate passed at head `bcea2f4`; no formal GitHub review is recorded. |
| [#111](https://github.com/1uismar33r0/Colegio_Connect/pull/111) | Cristian | Merged 2026-11-07; merge `3ff4708` | Current production authorization foundation; endpoint-specific ownership/consent checks remain future work. |
| [#112](https://github.com/1uismar33r0/Colegio_Connect/pull/112) | Cristian | Merged 2026-11-07; merge `e5cc181` | Typed CSRF client correction and hidden injected `CsrfToken` in current backend OpenAPI; main CI passed at `e5cc181`. |
| [#107](https://github.com/1uismar33r0/Colegio_Connect/pull/107) | Gabriel | Open at `15d0469`; recorded base `a128824` | Push and PR runs passed at the head before #111/#112 merged; Cristian requested documentation corrections. Current-main CI and teammate approval remain pending. |

Historical remote tips included repository foundation `28f4b68`, backend foundation `8e51f1f`, Docker `233485c`, and wireframe `b456f77`. They show contributions, not current branch activity. The October 26 A1 audit used freshly fetched `origin/main` rather than its then-stale local `main`; this November 10 update uses the newer baseline above.

Gabriel's [October 26 UTC comment on #88](https://github.com/1uismar33r0/Colegio_Connect/pull/88#issuecomment-6408234838) was accurate at that time: CI was implemented on #89 and review/merge were pending. The November 4 update above supersedes only its then-pending merge status.

## Architecture, security and integration risks

| Risk or gap | Consequence / next verification | Owner |
| --- | --- | --- |
| Role and schema evolution after the merged V1 baseline | Preserve V1 immutably; validate any new migration, JPA mapping, and role contract against an empty PostgreSQL 18 database. | Cristian + Gabriel |
| #9 role identifiers differ from #88 SQL/Java enum | Agree mapping before authorization/API contracts. Current enum: `STUDENT`, `INSTITUTIONAL`, `MODERATOR`, `COMPANY`; issue uses different institutional/company/admin identifiers. This is an integration risk, not a proven runtime failure. | Cristian + Gabriel; Luis consumes contract |
| Branch endpoint reports `protected: false` | CI reporting does not block merges by itself. Rulesets query returned HTTP 403 (plan/visibility restriction); protection query returned 404; Gabriel lacks admin permission. Luis must establish effective review/check enforcement and available plan features. | Luis + Gabriel, #1 |
| Authorization foundation is merged; business policy remains incomplete | #111 loads persisted assignments as session authorities and supplies active-account, organization-role, and approval helpers. Future endpoints still need current membership, ownership, consent, and approval enforcement; existing sessions do not universally recheck revoked roles or email verification. | Cristian + Gabriel security review |
| Merged React client uses an incomplete role response | `/api/auth/me` omits roles, so current role navigation resolves to an empty list. #112 hid the injected CSRF token and corrected committed OpenAPI; that issue is no longer open. Agree the persisted-role DTO and regenerate frontend types before claiming role-specific workflows. | Cristian + Luis |
| Provider sizing, budget, owner and rollback unapproved | Hosting cannot be represented as delivered; migrations can outlive a failed application release. | Gabriel + team/budget owner |
| Advisor comments, approval date and project webpage access absent | Cannot date sprints, answer proposal reviews, or claim complete midterm documentation. Private repository access for the professor also needs arranging. | Luis coordinates; all contribute |

## Sprint progress against the approved milestone outcomes

| Sprint | Milestone | State | Evidence / missing outcomes |
| --- | --- | --- | --- |
| 1, Weeks 1–2 | M1: Access foundation | In progress | Backend/database, registration/verification/login API, #111 authorization foundation, React foundation, and four-job CI are merged. `/me` roles, complete access screens/flow, protection enforcement, and hosting remain incomplete. |
| 2, Weeks 3–4 | M2: Publishing and discovery | In progress on branches only | Gabriel's #119/#120 branches contain an audit-backed company-decision/queue backend, not company registration or publishing. Six-category publishing/lifecycle, search/filter/save, k6/ZAP/axe results remain absent. |
| 3, Weeks 5–6 | M3: Responses and moderation | Not started | Responses, privacy, moderation/audit, notifications, company capstone route and usability preparation remain. Historical infrastructure restart evidence is not milestone acceptance. |
| 4, Weeks 7–8 | M4: Sponsorship and final acceptance | Not started | Student projects, consent-based interest, all I1–I17 results, user guide, final report and presentation remain. |

Week 1 starts on proposal approval (PDF p.23). No calendar sprint dates or completion percentages are inferred.

## Team contributions and current visible work

| Developer | Established contribution | Current visible work | Next responsibility |
| --- | --- | --- | --- |
| Gabriel (`DCGabriel10`) | PR #85 repository foundation; PR #87 Docker; review of #86; merged PR #89 CI; current #107 CI trigger work; #118 authorization tests; #119 audit and #120 company backend branches | Consolidating #118/#119/#120 for review; #4 hosting remains planning | Obtain teammate security/CI reviews, verify integration, coordinate protection, and gather report evidence. |
| Cristian (`Massinl`) | Backend foundation #86, Java 25 project setting, database #88, registration/login, merged #111 authorization, and Docker runtime confirmation | Define `/me` persisted-role response with Luis; review Gabriel's audit/company security and migrations | Own backend contract and business-policy completion; review without restoring #98's obsolete role names. |
| Luis (`1uismar33r0`) | Initial repository commit; HTML wireframe `b456f77`; merged React #106; repository ownership and merge of Cristian's #112 | #115 notification abstraction and #116 auth screens open | Agree on role/OpenAPI contract, verify real browser flow, establish repository rules/access, and coordinate professor report/webpage. |

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
| P1.1 Complete access workflows | Cristian #7/#8/#9; Luis #13/#14; Gabriel #12 and security review | Required M1 outcomes; backend foundations and React shell are merged | Real register/verify/login browser flow, current persisted roles in `/me`, protected routes with role/ownership/status checks, invalid/expired code and negative-authorization evidence | #115/#116 review and the shared role response remain; verify behavior across actual API and UI |
| P1.2 Select and verify hosting | Gabriel #4; Cristian DB support; Luis review | Deploy only a demonstrable baseline; depends on CI/database, measured sizing, approved owner/budget and security scope | Approved ADR; deployed tested SHA; HTTPS/private DB/secrets verified; migration, startup and recovery evidence; detailed runbook | No approved provider/budget/resources; measurements and research parallel now |
| P1.3 Assemble midterm evidence | Luis coordinates #60; all supply artifacts | November 10 report needs verifiable results and review responses | [Evidence index](../reports/midterm/evidence-index.md), actual advisor comments/responses, updated designs, test records, genuine preliminary measurements, screenshots and accessible webpage/repo | Feedback/measurements/webpage missing; collection proceeds immediately |
| P2.1 Begin publishing/discovery implementation | Gabriel #16/#17/#19; Cristian #18/#20; Luis #21/#22/#23; team #25/#26/#28 | Depends on access control, organizations and contracts; M1 is not assumed complete | Company approval and authorized publishing/search/filter/save, with relevant I1–I4/I6/I7 evidence and first scans | Access foundations incomplete; design and datasets can proceed in parallel |

## Decisions and information still needed

Obtain the proposal approval date, actual advisor/professor review comments, agreed API/role identifiers, hosting and email ownership, monthly spending ceiling and funded period, region, resource measurements, repository enforcement capability, and professor access to the private repository/project webpage. See [deployment prerequisites](../deployment.md) and [midterm evidence index](../reports/midterm/evidence-index.md) for concrete evidence to collect.
