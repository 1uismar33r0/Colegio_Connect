# Midterm report evidence index

**Updated / evidence observed: 2026-11-10.** Coordinator: Luis; Gabriel and Cristian supply their engineering evidence. This is a preparation index, not the submitted report. Current implementation baseline: [status](../../project/status.md); exact requirements: [traceability](../../project/traceability.md).

## Confirmed course instructions and provenance

**November 10 dashboard note:** The main-based Issue #122 backend [contract](../../roles/dashboard-api-contract.md) and [TE-15](../../testing/test-evidence.md#te-15) are branch-local read API evidence. Luis's latest `47f3420` student, office, and company pages still mock data in component tests; his moderator administration pages use the existing typed main endpoints. The database foundations and a green build are not evidence of working publication, student response, or measured performance. Keep the progress report's implementation and result claims bounded to observed runs.

The locally supplied **`Captone  Deliverables Fall.pdf`**, titled *CAPSTONE Activities & Deliverables Fall 2026*, is one page, SHA-256 `d4ca4c5ffca7fad502ebe114da717abe3da2c2a74f9a09c6af1e77b0afa83ad4`. It was read on October 26. It requires:

- **Progress report: November 10, 2026**, 15–20 pages, submitted through the Moodle Capstone page (30%). Clearly address proposal reviews, updated design elements, preliminary performance results, and project documentation including code repository and project webpage.
- **Final report: December 12, 2026**, 20–30 pages (60%), updated designs and performance results; presentation slides, elevator-pitch video and code repository available through the project webpage.
- **Final presentation and demo: December 12, 2026.**

`Capstone Reports Guidelines.docx` was also read. It is general report/template/rubric guidance covering motivation, objectives, design, constraints, standards, team tasks, testing, results and documentation. Its 120-point rubric is not assumed to be a separate midterm-specific rubric. `Fall 2026- Capstone Guidelines PPT.pdf` was available for course context. The local course files are not copied into this repository; Luis should preserve the authoritative Moodle links/version and confirm any newer instructions. No actual proposal-review comments were found in the supplied materials.

The approved project scope is the repository's [30-page proposal](../../project/sources/approved-proposal.pdf), not an alternative DOCX. Its original four figures are approved designs, not proof that those systems are implemented.

## Report assembly matrix

The following is a proposed organization within 15–20 pages; headings/page allocation are guidance, not invented course requirements.

| Proposed section / required information | Supporting GitHub evidence | Supporting source files | Figure or screenshot needed | Availability / missing evidence | Responsible developer / next action |
| --- | --- | --- | --- | --- | --- |
| Problem, users, motivation and approved scope | Canonical issue map | Proposal pp.1–11; [traceability](../../project/traceability.md) O1–O5/I1–I17 | Optional scope overview attributed to proposal | Available; approval date missing | All; Luis confirms date and scope context |
| Responses to proposal reviews | Actual review discussion/link when obtained | Comment-response register below | Annotated changed diagram only if feedback requires it | Missing actual comments; no fabricated responses | Luis obtains reviews from advisor/Moodle; all answer assigned points |
| Updated architecture and implemented boundary | PRs #85/#86/#87; open #88/#89; wireframe commit `b456f77` | Proposal Figure 1 p.12; backend/Docker/Compose; status | Current architecture with implemented vs proposed components and revision/date | Approved figure/code available; updated as-built diagram not yet authored/reviewed | Gabriel infrastructure, Cristian API/data, Luis UI; produce reviewed update |
| Data and access design | #6/#9; PR #88 | Proposal Figure 2 p.15; V1/JPA on database branch; Figure 3 p.16 | Updated identity ER model and agreed role mapping, clearly marked branch-only until merged | Code/design available; role contract, migration CI and peer review missing | Cristian + Gabriel; reconcile identifiers and verify empty DB |
| UI and workflow progress | #13/#14; wireframe branch; future UI PR | Luis's HTML design; proposal Figures 3–4 pp.16–17 | Genuine wireframe screenshots labeled mockups; later actual React screens | HTML source available; screenshots and real React flow evidence missing | Luis, with Cristian contract support; do not present mockups as working UI |
| Implementation and contributions | PR #85 Gabriel; #86 Cristian/Gabriel review; #87 Gabriel/Cristian confirmation; #88 Cristian; #89 Gabriel; open #99 and its [main-based Issue #15 reconciliation](../../gabriel/security/issue-15-audit-foundation.md); Luis wireframe commit | [Development log](../../progress/development-log.md) | Small dated roadmap/status table | Audit foundation is locally verified on a branch; review, CI/merge, and action integrations remain missing. Unobserved activity/hours are not inferred | Each member reviews their contribution summary and supplies missing work links |
| CI and infrastructure validation | #89 [push run](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/40261670993), [PR run](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/40261679810); #87 runtime comment | [Test evidence](../../testing/test-evidence.md), environment/Docker files | Actual Actions job page; dated healthy Compose/health/OpenAPI output with SHA | Logs and historical results available; screenshots and integrated main/database CI still missing | Gabriel captures redacted evidence after integration; Cristian verifies schema |
| Preliminary performance results | Future #25 measurements; #4 sizing investigation | [Hosting ADR](../../decisions/0001-hosting.md); test evidence | Real RAM/CPU/startup/latency plots with environment/sample details | **No substantive load-test results available.** No discovery endpoints or k6 suite established | Gabriel + Luis, Cristian API support; collect legitimate preliminary measures below |
| Testing against objectives | Issues mapped to I1–I17; Issue #12 / historical PR #100 | Test evidence indicator table; approved targets p.7; [Gabriel's Issue #12 engineering record](../../gabriel/security/issue-12-negative-authorization.md) | Coverage matrix showing passed/blocked/not-run scope and current negative-case walkthroughs | Issue #12 branch has 20 focused and 30 full-suite passing tests; merge/review/CI are pending and full product I6/security/privacy/accessibility/recovery/usability acceptance remains absent | Each feature owner supplies stable endpoint evidence; Gabriel grows the harness without inflating final I6 acceptance |
| Challenges, fixes and decisions | #87/#89; #88 dependency comments; branch protection observation | [Bug log](../../bugs/bug-log.md), ADR, deployment prerequisites | Optional before/after evidence from actual incident | Tooling history and risks documented; some original diagnostics/root causes unavailable | Gabriel explains confirmed workarounds and explicit limitations |
| Repository and project webpage documentation | Repository and this documentation PR | README, AGENTS, contributor docs and evidence index | Real project webpage and navigation/access screenshot | Documentation available on branch; webpage URL/access and professor access unverified; repo is private | Luis arranges publication/access; Gabriel checks links and evidence exposure |
| Remaining work, schedule and blockers | Open canonical backlog, #88/#89 integration | Status roadmap and M1–M4 | Dependency roadmap, no invented calendar sprint dates | Roadmap available; proposal approval date and owner/budget decisions missing | All agree next actions and record decisions before report submission |

**November 10 integration evidence update:** Gabriel's local #119 branch now combines #118's authorization harness, #119's audit foundation, and #120's company decision/queue code. [TE-12](../../testing/test-evidence.md#te-12) records 64/64 local backend tests, an isolated V1–V3-to-V5 preservation check, 22/22 frontend tests, Compose validation, and image build at code commit `5c29336`. The older rows above describe their dated snapshots. The combined PR is not yet merged or team-approved; company signup/publishing, `/api/auth/me` roles, hosted deployment, and final indicator evidence remain missing.

**Audit review follow-up:** At `8021392`, equal-timestamp audit pagination received a unique-ID tie breaker after a failing regression test. The new complete backend suite passed 65/65, and frontend lint, typecheck, 22/22 tests, and build passed locally. These are functional checks, not preliminary load or latency measurements. Hosted CI and team review remain required before integration.

## Advisor / professor comments — source required

**Current availability: not collected.** Advisor named by the proposal: Fernando Torre. Do not infer feedback from the proposal rubric, an AI review, or teammates' GitHub comments.

Luis should obtain the actual proposal evaluation/review from Moodle, email or the advisor and retain the source/date. Record each real comment in the following fields: comment ID; exact comment or faithful linked excerpt; source/date; responsible developer; response; changed issue/file/figure; verification; unresolved item; reviewer confirmation. Until obtained, report explicitly that review-response preparation is blocked on receiving the feedback, not that there were no comments.

## Legitimate preliminary measurements to collect

These are planned experiments, not completed tests. Use synthetic data and record date, exact SHA/image, host CPU/RAM/OS, tool/version, procedure, repetitions/sample count, raw result location, and limitations.

| Measurement | What it can support | What it cannot establish | Owner / dependency |
| --- | --- | --- | --- |
| Java container build time/image size; cold startup to health; idle and peak startup memory/CPU | Infrastructure progress and initial hosting sizing | Discovery latency, production capacity or I3 | Gabriel; working Docker and stable baseline |
| Fresh PostgreSQL 18 migration duration and JPA startup with controlled DB | Database integration progress when #88 actually runs | Authentication, privacy or representative application load | Cristian + Gabriel; integrated #88/#89 |
| Small repeatable health/OpenAPI request sample with disclosed latency/error counts | Endpoint smoke baseline and method demonstration | I3's 5,000 listings/1,000 users/50 orgs/25-VU discovery workload | Gabriel; clearly label low-load infrastructure smoke test |
| CI duration, executed test count and reproducible pass/failure results | Build/test automation evidence | 20-scenario I13 acceptance or negative-authorization I6 coverage | Gabriel; capture run links, SHAs and logs |
| Wireframe walkthrough / API contract review | Design progress and integration readiness | Usability I17 or WCAG 2.2 AA I15 acceptance | Luis + Cristian; label design review, not user study |

When discovery exists, collect real k6 results for the full documented workload and report p95 latency, server error rate, duration, dataset and environment. Until then, state the missing results and dependency honestly. Do not replace the required performance discussion with a green health endpoint.

## Evidence preservation and report gate

Preserve run URLs, source SHAs and a sanitized summary promptly; downloadable CI artifacts/logs may expire. Store only intentional redacted screenshots/reports with date/SHA and clear captions. Exclude tokens, private student information, credentials and live database URLs. A screenshot must show the actual system/run, not a reconstruction.

Before submission, all three members should confirm their contributions, actual review responses, implemented/proposed diagram labels, indicator limitations, report length/date, working repository/webpage access, and every claimed result's evidence. A1 makes the report assembleable; it does not make the midterm report complete or prove milestone acceptance.
