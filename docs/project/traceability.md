# Requirements and implementation traceability

Observation date: **2026-10-26**. Baseline: [`main` at `1714e5e0f000f9ad4a2ff6a00bf71c2e14fc5b18`](https://github.com/1uismar33r0/Colegio_Connect/tree/1714e5e0f000f9ad4a2ff6a00bf71c2e14fc5b18). This register records implementation evidence separately from acceptance and code location. All 84 issues were open at observation time. A merged pull request does not accept its related issue.

## Authority and scope

**Issue #122 dashboard checkpoint (2026-11-10):** PR #127 supplies persisted reads for discovery, student activity, office/company summaries and moderator review. Revised V6 supports organization and capstone-team ownership with three approved trust labels; 18 deterministic backend feed-filter cases cover category, source, organization, Puerto Rico deadline, keyword and combinations. This partially supports O1/O2/O3 but does not accept I1–I9: listing publishing/write flows, student project review and sponsorship, external applicant registration, complete privacy/performance evidence and team review remain. Frontend filter integration is on `feat/issue-122-role-pages` ([TE-14](../testing/test-evidence.md#te-14) run 3). The six approved categories are preserved; `Event` is excluded. See [dashboard contract](../roles/dashboard-api-contract.md) and [TE-15](../testing/test-evidence.md#te-15).

The authoritative source is the approved, 30-page **Colegio Connect Proposal.pdf**, preserved as [approved-proposal.pdf](sources/approved-proposal.pdf), SHA-256 `88038171ef23ffbd42bcbe38cd0ec5177240e42ab010fb6fbed96107b1b8f98c`. The alternative DOCX is not the baseline. Page references below mean PDF page numbers. Live issue bodies and PR metadata were captured on 2026-10-26; links identify the remote sources, which may change after that date.

The approved scope has **two account types** (`UPRM_MEMBER`, `EXTERNAL_ORGANIZATION`), **four roles** (Student, Faculty/Office/Department, External Company, Admin/Moderator), and **six opportunity categories** (job, internship, COOP, research position, research participation, capstone). Each person has an individual login; organizations are separate records; users may hold multiple roles. Company capabilities require approval. Student data is shared only through consent; companies cannot browse students. Sponsorship contact release requires team acceptance. These are requirements, not implemented security guarantees.

The constraints include a near-zero budget, three developers, eight weeks from approval, no required UPRM IT integration, unofficial prototype status, limited moderation capacity, and provider-independent email. Accessibility targets **WCAG 2.2 AA**, zero unresolved Critical/Serious findings, and keyboard completion of all primary workflows. Performance uses at least 5,000 synthetic listings, 1,000 users, 50 organizations and 25 concurrent virtual users; record host CPU, memory, duration, response times and error rate. No real student data is needed for this workload.

Exactly **four approved figures** define design references: Figure 1, System architecture (p.12); Figure 2, Simplified entity-relationship model (p.15); Figure 3, Use cases by role (p.16); Figure 4, Listing lifecycle state diagram (p.17). No additional figure is claimed as approved. Figure mappings added below are explanatory traceability mappings; original issue references are preserved separately.

## Objectives and milestones

| ID | Approved objective (Table 3, p.6; whitespace normalized) | Deadline | Indicators |
| --- | --- | --- | --- |
| O1 | Unified discovery. Implement one feed where signed-in users can browse, search, filter, and open listings from all six opportunity categories, and verified students can save them, with discovery requests answered within 2 seconds under the test workload. | Week 4 | I1–I4 |
| O2 | Access control and privacy. Implement email verification and server-side authorization so that every protected action checks the user's role, ownership, approval status, account status, and consent, with zero unauthorized disclosures of student data. | Week 6 | I5–I7 |
| O3 | Moderation and accountability. Implement reporting, listing review, hiding and removal of listings, account restrictions, and an audit log that records every administrative action, working even when no UPRM office participates. | Week 6 | I8–I9 |
| O4 | Two-sided capstone sponsorship. Implement the company-originated route (company call, student response) and the student-originated route (team project, Express Interest, accept or decline), with no private contact information released before acceptance. | Week 8 | I10–I12 |
| O5 | Reliability, security, and accessibility. Deliver a deployed build that passes the final acceptance suite, security scan, accessibility checks, recovery test, and usability test with the targets in Section 3(b). | Week 8 | I13–I17 |

| Milestone (Table 16, p.23) | Approved deliverables | Canonical issues |
| --- | --- | --- |
| M1 – Access foundation; Weeks 1–2 | Running frontend, API, and database; working login and role checks; first authorization tests. | #1–#15 |
| M2 – Publishing and discovery; Weeks 3–4 | Authorized users publish listings; students browse, search, filter, and save; first performance, security, and accessibility results. | #16–#23, #25, #26, #28 |
| M3 – Responses and moderation; Weeks 5–6 | Response flow, moderation and audit flow, notifications, and company capstone route; updated test results and usability plan. | #31, #33, #35, #37, #39, #40, #42, #44, #46 |
| M4 – Sponsorship and final acceptance; Weeks 7–8 | Two-sided sponsorship workflow; final results for I1–I17; user guide, final report, and presentation. | #48, #50, #52, #54, #56, #58, #60 |

The complete, exact I1–I17 targets and executable evidence requirements are in [test-evidence.md](../testing/test-evidence.md#indicator-evidence-plan). A milestone is not achieved merely because its infrastructure exists.

## User stories from the approved backlog

Table 15, PDF pp.22–23; whitespace normalized. These are requirements, not implemented user journeys. Individual issue sections below map their corresponding story IDs.

| Story | Approved user story | Sprint |
| --- | --- | --- |
| US1 | As a UPRM member, I want to register with my institutional email and verify it with a code, so that I receive UPRM permissions. | 1 |
| US2 | As an administrator, I want to assign institutional and moderator roles, so that only authorized people publish or moderate. | 1 |
| US3 | As a company, I want to register my organization and request approval, so that I can publish opportunities. | 2 |
| US4 | As an administrator, I want to approve or reject company requests, so that only legitimate organizations publish. | 2 |
| US5 | As a publisher, I want to create a listing in any of the six categories, so that students can find it. | 2 |
| US6 | As a student, I want to search and filter listings by category, keyword, source, organization, and deadline, so that I find relevant opportunities quickly. | 2 |
| US7 | As a student, I want to save listings, so that I can return to them later. | 2 |
| US8 | As a student, I want to respond to a listing and choose which profile fields to share, so that I control my information. | 3 |
| US9 | As a publisher, I want to see the students who responded to my listing, so that I can contact them. | 3 |
| US10 | As a user, I want to report a suspicious listing, so that moderators can review it. | 3 |
| US11 | As a moderator, I want to hide or remove listings and restrict accounts, with each action recorded, so that the platform stays safe and accountable. | 3 |
| US12 | As a company, I want to publish a capstone project call, so that student teams can respond. | 3 |
| US13 | As a student team, I want to publish our project and the support we need, so that sponsors can find us. | 4 |
| US14 | As a company, I want to send Express Interest to a student project, so that I can offer support. | 4 |
| US15 | As a team member, I want to accept or decline a company's interest, so that our contact information is shared only if we agree. | 4 |

## State and evidence rules

Engineering state uses exactly **Not started**, **In progress**, **Implemented but unverified**, **Verified on branch**, **Merged; acceptance pending**, **Accepted**, or **Blocked**, as defined in [status](status.md). Location is separate. The acceptance note describes unresolved criteria and blockers without promoting the implementation state. No issue is marked Accepted in this snapshot.

The issue register contains **42 title pairs / 84 issues / 67 canonical acceptance checklist entries / 134 entries across both copies**. Compound acceptance entries retain every clause. Each pair's exact descriptive paragraph is also preserved, including requirements outside task/acceptance checklists. Canonical means the lower issue number; the higher number is an alias, not an additional feature. All pairs have identical acceptance wording. #5/#32 differ only in the five task checkbox states; #5 marks them checked and #32 does not. Those checks are source claims, not acceptance evidence. GitHub assignees are distinct from the body Lead; only canonical #5 and #6 are assigned to `Massinl`. Every alias is unassigned.

Each numbered `ACn` below reproduces the acceptance text of **both** linked issues exactly (only the checkbox and list prefix are removed). `TC-<canonical>-AC<n>` is a stable **planned** test reference, not an existing automated test. The common engineering state, acceptance state, location, evidence and blocker statement in a section apply to **every AC in that section**, unless the section explicitly narrows an observation. Exact tasks retain operational requirements outside the acceptance heading. The three-item Definition of Done is identical in all 84 issues: (1) Code reviewed by another member; (2) Tests pass in CI; (3) OpenAPI document updated (if API changed). It applies to every section and remains to be evidenced for full issue acceptance.

The Lead field reproduces the issue body and is distinct from GitHub assignees. Support below follows proposal Table 17 (p.24) where the feature is explicitly grouped; otherwise it is labeled proposed coordination. The issue body does not override the approved proposal. #10 names Cristian for institutional-role assignment while Table 17 groups institutional roles under Gabriel in Sprint 2 (and foundation roles under Cristian in Sprint 1); #25 names Gabriel for k6 while Table 17 groups k6 setup under Luis. Resolve these planning differences with the team without rewriting either source. The register adds objective/figure mappings for infrastructure enablers; these do not introduce new approved requirements.

## Current integration limits

- PRs [#85](https://github.com/1uismar33r0/Colegio_Connect/pull/85), [#86](https://github.com/1uismar33r0/Colegio_Connect/pull/86), and [#87](https://github.com/1uismar33r0/Colegio_Connect/pull/87) are merged. Main contains repository scaffolding, backend scaffolding, and Docker development support. It has no authentication or business flows and no workflow file.
- [PR #88](https://github.com/1uismar33r0/Colegio_Connect/pull/88) is open at `af24f8987df1a4048af4b9b4789a9582f816fc85`; its check and review arrays are empty. Its author reports three local tests passing; that is not observed CI migration evidence.
- [PR #89](https://github.com/1uismar33r0/Colegio_Connect/pull/89) is open at `58089c4d4fdcef14d827dc64727ba02afcb76a22`; six successful check records represent Backend, Container and CI Gate on two runs, not six independent workflows. There are no recorded reviews. These checks do not include #88's migration changes or frontend checks.
- `Issue-13-Wireframes` at `b456f772ac7a307f165a0379be025af3d8d2b00d` contains exploratory HTML. It is not a React/TypeScript SPA and has no evidence of team/advisor acceptance.
- Branch metadata reports `main.protected=false`. The rulesets request returned 403 with a plan restriction; the protection request returned 404 and the account lacks administrator access. Enforcement is unverified; these responses do not prove every possible repository rule is absent. No destructive push/merge attempt was used to test the policy.
- #9 requires `STUDENT`, `FACULTY_OFFICE_DEPT`, `EXTERNAL_COMPANY`, `ADMIN_MODERATOR`; #88 implements `STUDENT`, `INSTITUTIONAL`, `COMPANY`, `MODERATOR`. These identifiers require an explicit reviewed mapping or correction before the role contract can be accepted. No server-side authorization is established by the enum alone.

## Canonical issue register

### Issue #1 / alias #24: Repository setup, branch protection and contribution workflow

Sources: [canonical #1](https://github.com/1uismar33r0/Colegio_Connect/issues/1) and [alias #24](https://github.com/1uismar33r0/Colegio_Connect/issues/24); both open. Lead: **Gabriel**. Support: Cristian (database), Luis (CI checks), per Table 17 environment/CI group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M1 - Access foundation**. Original issue references: C2, Section 6(a). Proposal mapping: **O5; I13; infrastructure enabler; Figure 1**.

Engineering state: **Merged; acceptance pending**. Acceptance note: blocked by dependencies below. Location: main; .github/, CONTRIBUTING.md, README.md.

Evidence / blockers (apply to every AC below): **TE-01, TE-02** in [test evidence](../testing/test-evidence.md). PR #85 merges scaffolding only. Direct-push rejection and required approval/CI are not demonstrated; administrator verification is needed.

Exact source description shared by both issues:

> Set up the repo structure, PR rules and docs so every later change follows the Definition of Done.

Acceptance text shared by both issues:

- **AC1** / `TC-1-AC1`: Direct push to main is blocked
- **AC2** / `TC-1-AC2`: A PR cannot merge without another member's approval and green CI

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Monorepo layout: /backend, /frontend, /docs, /load-tests, /e2e
- [ ] README (setup, run, test), CONTRIBUTING (branching, PR rules, Definition of Done)
- [ ] PR template with DoD checklist (reviewed, tests pass in CI, OpenAPI updated)
- [ ] Protect `main`: require 1 approval + passing CI, no direct pushes
- [ ] CODEOWNERS for security-sensitive paths (authorization, contact release)
- [ ] Issue templates (user story, bug, task)

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #2 / alias #27: CI pipeline (GitHub Actions) and Dependabot

Sources: [canonical #2](https://github.com/1uismar33r0/Colegio_Connect/issues/2) and [alias #27](https://github.com/1uismar33r0/Colegio_Connect/issues/27); both open. Lead: **Gabriel**. Support: Cristian (database), Luis (CI checks), per Table 17 environment/CI group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M1 - Access foundation**. Original issue references: I13, Table 14. Proposal mapping: **O5; I13; infrastructure enabler; Figure 1**.

Engineering state: **Verified on branch**. Acceptance note: blocked by dependencies below. Location: feat/issue-2-ci; .github/workflows/ci.yml and .github/dependabot.yml.

Evidence / blockers (apply to every AC below): **TE-03, TE-02** in [test evidence](../testing/test-evidence.md). PR #89 remains open without reviews. AC1 has observed push and PR runs, but no main workflow. AC2 has no enforced required-check proof. Frontend checks remain deferred to #13; no failure-blocking merge experiment was performed.

Exact source description shared by both issues:

> Build and test every push/PR; failing checks block merging.

Acceptance text shared by both issues:

- **AC1** / `TC-2-AC1`: CI runs on every push and PR
- **AC2** / `TC-2-AC2`: Failing check blocks merge

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Backend job: build + JUnit tests (Testcontainers)
- [ ] Frontend job: lint, typecheck, unit tests, build
- [ ] Cache dependencies
- [ ] Dependabot for Maven/Gradle, npm, GitHub Actions, Docker
- [ ] Package container image on main

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #3 / alias #29: Docker development environment with PostgreSQL

Sources: [canonical #3](https://github.com/1uismar33r0/Colegio_Connect/issues/3) and [alias #29](https://github.com/1uismar33r0/Colegio_Connect/issues/29); both open. Lead: **Gabriel**. Support: Cristian (database), Luis (CI checks), per Table 17 environment/CI group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M1 - Access foundation**. Original issue references: Table 8. Proposal mapping: **O5; I13, I16; infrastructure enabler; Figure 1**.

Engineering state: **Merged; acceptance pending**. Acceptance note: criteria/DoD still pending. Location: main; backend/Dockerfile, compose.yaml, .env.example, docs/environment.md.

Evidence / blockers (apply to every AC below): **TE-04** in [test evidence](../testing/test-evidence.md). PR #87 author reports local stack startup and health using port overrides. A clean-machine reproduction is not independently recorded here; no formal GitHub review record is returned for #87.

Exact source description shared by both issues:

> One-command local environment so all three members run the same stack.

Acceptance text shared by both issues:

- **AC1** / `TC-3-AC1`: `docker compose up` starts a working backend + DB on a clean machine

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Backend Dockerfile (Java 25)
- [ ] docker-compose: backend + PostgreSQL 18 (+ mail catcher for dev)
- [ ] .env.example with all required variables (no secrets committed)
- [ ] Document in README

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #4 / alias #30: Select hosting + hosted PostgreSQL and set up deploy pipeline

Sources: [canonical #4](https://github.com/1uismar33r0/Colegio_Connect/issues/4) and [alias #30](https://github.com/1uismar33r0/Colegio_Connect/issues/30); both open. Lead: **Gabriel**. Support: Cristian (database), Luis (CI checks), per Table 17 environment/CI group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M1 - Access foundation**. Original issue references: Section 3(b), C3 (deadline: end of Week 2). Proposal mapping: **O1, O5; I3, I13, I16; infrastructure enabler; Figure 1**.

Engineering state: **In progress**. Acceptance note: local staging preparation is verified on `gabriel/issue-4-render-staging`, but AC1 and hosted AC2 remain unaccepted. Location: deployment feature branch; no deployed service verified.

Evidence / blockers (apply to every AC below): **TE-16** in [test evidence](../testing/test-evidence.md) records the combined image and isolated Free-sized local checks; [staging ADR 0002](../decisions/0002-render-free-staging.md) and the [Render runbook](../deployment/render-staging.md) record the provisional provider choice and limits. The older [multi-provider ADR](../decisions/0001-hosting.md) remains historical. Team review, authenticated Blueprint validation, provisioning authorization, public HTTPS deployment, and actual Render CPU/memory evidence are absent. Local Docker sizing does not satisfy hosted AC2 or proposal I3/I16.

Exact source description shared by both issues:

> Choose a free/low-cost container host and managed PostgreSQL able to carry the test workload; record specs for the final report.

Acceptance text shared by both issues:

- **AC1** / `TC-4-AC1`: Deployed build reachable over HTTPS
- **AC2** / `TC-4-AC2`: CPU/memory of environment documented

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Compare 2-3 hosts (CPU/RAM, cost, cold starts, Postgres limits)
- [ ] Decision recorded in /docs/decisions
- [ ] Deploy workflow from main
- [ ] Secrets in host/GitHub secrets
- [ ] Health endpoint for recovery test

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #5 / alias #32: Backend skeleton: Spring Boot, layered packages, OpenAPI

Sources: [canonical #5](https://github.com/1uismar33r0/Colegio_Connect/issues/5) and [alias #32](https://github.com/1uismar33r0/Colegio_Connect/issues/32); both open. Lead: **Cristian**. Support: Gabriel (security review), Luis (screens), per Table 17 access/data group. GitHub assignees: canonical **Massinl**; alias **unassigned**.

Milestone: **M1 - Access foundation**. Original issue references: Section 5(a). Proposal mapping: **O1–O5; I13; API foundation; Figure 1**.

Engineering state: **Merged; acceptance pending**. Acceptance note: criteria/DoD still pending. Location: main; backend/pom.xml, backend/src/main/java/com/colegioconnect/, backend/src/main/resources/application.properties.

Evidence / blockers (apply to every AC below): **TE-04, TE-05, TE-03** in [test evidence](../testing/test-evidence.md). PR #86 merges skeleton; #87 reports local Docker boot and #89 observes backend CI on its branch. No OpenAPI CI artifact publication evidence; CI is unmerged. Package placeholders do not implement domain behavior.

Exact source description shared by both issues:

> Spring Boot app organized as controllers / security / services / repositories with springdoc-openapi.

Acceptance text shared by both issues:

- **AC1** / `TC-5-AC1`: App boots in Docker and CI
- **AC2** / `TC-5-AC2`: OpenAPI doc served and published as CI artifact

Exact source tasks (checkboxes describe the issue snapshot):

- [x] Project init (Java 25, Spring Boot)
- [x] Package structure per module (accounts, organizations, listings, discovery, responses, capstone, moderation, notifications, audit)
- [x] Global error format and validation handling (RFC 9110 status codes)
- [x] springdoc-openapi at /v3/api-docs + Swagger UI
- [x] Health endpoint

Alias #32 has the same five task texts, all unchecked; canonical #5 has them checked. All acceptance and Definition of Done checkboxes remain unchecked in both.

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #6 / alias #34: Database baseline with Flyway (users, roles, orgs, email codes)

Sources: [canonical #6](https://github.com/1uismar33r0/Colegio_Connect/issues/6) and [alias #34](https://github.com/1uismar33r0/Colegio_Connect/issues/34); both open. Lead: **Cristian**. Support: Gabriel (security review), Luis (screens), per Table 17 access/data group. GitHub assignees: canonical **Massinl**; alias **unassigned**.

Milestone: **M1 - Access foundation**. Original issue references: Figure 2 (blocks A, D). Proposal mapping: **O2; I5, I6; US1–US2; Figure 2 blocks A, D**.

Engineering state: **Implemented but unverified**. Acceptance note: blocked by dependencies below. Location: issue-6-database-baseline; backend/src/main/resources/db/migration/V1__identity_baseline.sql, accounts/entity/, accounts/repository/, organizations/entity/, organizations/repository/, TestcontainersConfiguration.java.

Evidence / blockers (apply to every AC below): **TE-06, TE-07** in [test evidence](../testing/test-evidence.md). PR #88 remains open with no checks/reviews. Local three-test claim has no attached machine-readable artifact. Empty-DB migration must run on the database change in CI; resolve role identifier mismatch with #9.

Exact source description shared by both issues:

> First versioned migration for identity and organization tables.

Acceptance text shared by both issues:

- **AC1** / `TC-6-AC1`: Migrations apply cleanly to an empty DB in CI
- **AC2** / `TC-6-AC2`: Schema matches Figure 2 (identity block)

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] V1 migration: user, organization, role_assignment, email_code
- [ ] Constraints, unique email, enums for account_type/status/approval_status
- [ ] JPA entities + repositories
- [ ] Testcontainers migration test

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #7 / alias #36: Registration with institutional email and verification code

Sources: [canonical #7](https://github.com/1uismar33r0/Colegio_Connect/issues/7) and [alias #36](https://github.com/1uismar33r0/Colegio_Connect/issues/36); both open. Lead: **Cristian**. Support: Gabriel (security review), Luis (screens), per Table 17 access/data group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M1 - Access foundation**. Original issue references: US1, I5, I12, NIST 800-63B-4. Proposal mapping: **O2, O4; I5, I12; US1; Figures 2–3**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Register, receive a short-lived code, verify; only UPRM institutional emails become UPRM_MEMBER.

Acceptance text shared by both issues:

- **AC1** / `TC-7-AC1`: 100% non-UPRM emails and invalid/expired codes rejected (I5)
- **AC2** / `TC-7-AC2`: Verification requests rate limited

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] POST register / verify / resend endpoints
- [ ] Validate UPRM email domain server-side
- [ ] Hash codes, expiry, single use, attempt limit
- [ ] Resend limits + rate limiting
- [ ] Unit + integration tests: non-UPRM, wrong code, expired code, reused code

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #8 / alias #38: Login, sessions and CSRF with Spring Security

Sources: [canonical #8](https://github.com/1uismar33r0/Colegio_Connect/issues/8) and [alias #38](https://github.com/1uismar33r0/Colegio_Connect/issues/38); both open. Lead: **Cristian**. Support: Gabriel (security review), Luis (screens), per Table 17 access/data group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M1 - Access foundation**. Original issue references: Table 8, ASVS L1. Proposal mapping: **O2, O5; I6, I14; US1; Figures 1, 3**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Server-side sessions, password hashing and CSRF protection using Spring Security (no custom crypto).

Acceptance text shared by both issues:

- **AC1** / `TC-8-AC1`: Authenticated session works from the SPA
- **AC2** / `TC-8-AC2`: Suspended accounts rejected
- **AC3** / `TC-8-AC3`: ZAP baseline has no High findings on auth endpoints

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Password policy (length, hashed storage)
- [ ] Login / logout / current-user endpoints
- [ ] Secure cookies (HttpOnly, Secure, SameSite)
- [ ] CSRF for the SPA
- [ ] Login rate limiting / lockout
- [ ] Account status check (suspended cannot log in)

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #9 / alias #41: Role model and server-side authorization framework

Sources: [canonical #9](https://github.com/1uismar33r0/Colegio_Connect/issues/9) and [alias #41](https://github.com/1uismar33r0/Colegio_Connect/issues/41); both open. Lead: **Cristian**. Support: Gabriel (security review), Luis (screens), per Table 17 access/data group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M1 - Access foundation**. Original issue references: US2, I6, Table 2, Access model. Proposal mapping: **O2; I6; US2; Figures 2–3**.

Engineering state: **Not started**. Acceptance note: blocked by dependencies below. Location: main security/package-info.java is a placeholder; #88 has a separate role enum only.

Evidence / blockers (apply to every AC below): **TE-00, TE-07** in [test evidence](../testing/test-evidence.md). Depends on identity, sessions and reviewed policy matrix; reconcile FACULTY_OFFICE_DEPT/EXTERNAL_COMPANY/ADMIN_MODERATOR with INSTITUTIONAL/COMPANY/MODERATOR before integration.

Exact source description shared by both issues:

> Central, reusable checks for role, ownership, organization membership, approval status, account status and consent. Users may hold several roles.

Acceptance text shared by both issues:

- **AC1** / `TC-9-AC1`: Every protected endpoint uses the shared checks
- **AC2** / `TC-9-AC2`: Policy matrix reviewed by Gabriel (security review)

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Role enum: STUDENT, FACULTY_OFFICE_DEPT, EXTERNAL_COMPANY, ADMIN_MODERATOR
- [ ] Method-level security helpers / policy classes
- [ ] Ownership + organization-membership checks
- [ ] Approval-status gate for companies
- [ ] Document the policy matrix in /docs

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #10 / alias #43: Admin: assign and revoke institutional/moderator roles

Sources: [canonical #10](https://github.com/1uismar33r0/Colegio_Connect/issues/10) and [alias #43](https://github.com/1uismar33r0/Colegio_Connect/issues/43); both open. Lead: **Cristian**. Support: Gabriel (security review), Luis (screens); reconcile institutional-role lead/sprint grouping noted above. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M1 - Access foundation**. Original issue references: US2. Proposal mapping: **O2, O3; I6, I8; US2; Figures 2–3**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Only admins can grant Faculty/Office/Dept and Admin/Moderator roles; first admin bootstrapped securely.

Acceptance text shared by both issues:

- **AC1** / `TC-10-AC1`: Non-admins get 403
- **AC2** / `TC-10-AC2`: Role change recorded with actor, action, target, time

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Endpoints to assign/revoke roles (with optional organization_id)
- [ ] Bootstrap first admin via env/seed (not via public API)
- [ ] Write audit record for each change (depends on audit foundation issue)

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #11 / alias #45: Transactional email service and notification abstraction

Sources: [canonical #11](https://github.com/1uismar33r0/Colegio_Connect/issues/11) and [alias #45](https://github.com/1uismar33r0/Colegio_Connect/issues/45); both open. Lead: **Luis**. Support: Cristian (API), Gabriel (privacy tests), per Table 17 notifications group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M1 - Access foundation**. Original issue references: I12, C10. Proposal mapping: **O4; I12; US1; Figure 1**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Provider-agnostic email sender used by verification codes and later notifications; log delivery results.

Acceptance text shared by both issues:

- **AC1** / `TC-11-AC1`: Verification email sent in test runs
- **AC2** / `TC-11-AC2`: Switching provider requires only an adapter change

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] NotificationService interface + provider adapter (vendor not hard-wired)
- [ ] Templates for verification code
- [ ] Delivery result logging
- [ ] Fake sender for tests / dev mail catcher

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #12 / alias #47: Negative-authorization test harness

Sources: [canonical #12](https://github.com/1uismar33r0/Colegio_Connect/issues/12), open on live inspection 2026-11-07, and historical alias #47 from the 2026-10-26 issue export. A live 2026-11-07 query returned no current issue or PR for #47, so its former open state is not carried forward. Lead: **Gabriel**. Support: Cristian (policy/API fixtures), Luis (screen/E2E cases), proposed coordination for the access-control test harness. GitHub assignee on canonical #12: **unassigned**.

Milestone: **M1 - Access foundation**. Original issue references: I6. Proposal mapping: **O2; I6; US1–US2; Figure 3**.

Engineering state: **Verified on branch**. Acceptance note: the main-based harness is used by more than three existing denial cases; its original PR #118 passed CI, and Gabriel consolidated its commits into PR #119 locally on 2026-11-10. AC1's merge clause and teammate security review remain pending. Location: original [PR #118](https://github.com/1uismar33r0/Colegio_Connect/pull/118), implementation `92fadc467db5a85dcea5c03fc3456e6d38936999`; surviving [PR #119](https://github.com/1uismar33r0/Colegio_Connect/pull/119) branch, local merge `36157e6d0ea3f6ea226257131622f2f907feae86` pending publication.

Evidence / blockers (apply to every AC below): **TE-11** and the [November 10 combined run](../testing/test-evidence.md#te-12), plus Gabriel's [Issue #12 engineering record](../gabriel/security/issue-12-negative-authorization.md). The combined PR #119 code passed 64 backend tests locally. Neither branch is merged or teammate-reviewed; final I6 coverage still depends on protected business endpoints.

Exact source description shared by both issues:

> Reusable integration-test base (Testcontainers + MockMvc/RestAssured) for 'forbidden' scenarios; grows every sprint toward 10+ cases.

Acceptance text shared by both issues:

- **AC1** / `TC-12-AC1`: Harness merged and used by at least 3 cases
- **AC2** / `TC-12-AC2`: Documented how to add a case

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Test fixtures for each role/status/org combination
- [ ] First cases: unauthenticated, wrong role, suspended user, unverified email
- [ ] Run in CI

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #13 / alias #49: Frontend setup: React + TypeScript, routing, API client, wireframes

Sources: [canonical #13](https://github.com/1uismar33r0/Colegio_Connect/issues/13) and [alias #49](https://github.com/1uismar33r0/Colegio_Connect/issues/49); both open. Lead: **Luis**. Support: Cristian (API contract), Gabriel (review), per Table 17 frontend group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M1 - Access foundation**. Original issue references: Table 8. Proposal mapping: **O1–O5; I15; all role workflows; Figures 1, 3**.

Engineering state: **In progress**. Acceptance note: criteria/DoD still pending. Location: Issue-13-Wireframes; docs/colegio-connect-wireframes.html; frontend/.gitkeep remains placeholder.

Evidence / blockers (apply to every AC below): **TE-08** in [test evidence](../testing/test-evidence.md). Exploratory HTML exists only on branch. No React/TypeScript build, role-aware router, typed API client, or recorded team/advisor review.

Exact source description shared by both issues:

> SPA skeleton and low-fi wireframes for all four roles.

Acceptance text shared by both issues:

- **AC1** / `TC-13-AC1`: App builds in CI
- **AC2** / `TC-13-AC2`: Wireframes reviewed by team + advisor

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Vite + React + TypeScript, ESLint, Prettier
- [ ] Router with role-aware route guards
- [ ] Typed API client generated from OpenAPI
- [ ] Layout, error and loading components with accessibility basics (labels, focus, contrast)
- [ ] Wireframes for core screens in /docs/wireframes

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #122: Frontend: role home pages from core wireframes (mock API)

Source: [#122](https://github.com/1uismar33r0/Colegio_Connect/issues/122). It was opened on 2026-11-10 and has no alias. It is follow-up work to the #13 core wireframes, not an approved-scope change. Lead: **Luis**. GitHub assignee: **1uismar33r0**.

Milestone: **M1 - Access foundation**. Proposal mapping: **O5; I15; Table 8; Figure 3**. The role features themselves remain with their own issues (#16, #20, #22, #23, #31, #33, #37, #39).

Engineering state: **Verified on branch**. Location: `feat/issue-122-role-pages`, in `frontend/src/pages/{student,institution,company,moderation}/`, `frontend/src/components/{RoleShell,OrganizationScope}.tsx`, `frontend/src/api/roleData.ts` and `frontend/src/api/admin.ts` (generated OpenAPI types on the typed client), and `frontend/src/test/mockApi.ts` (component tests only).

Evidence / blockers: **TE-14** in [test evidence](../testing/test-evidence.md#te-14).
- All four role areas read real, authorized backend data: the dashboard reads from PR #127 (merged 2026-11-11) and the administrator endpoints from #10/#15/#16. Runtime pages no longer use mock data.
- The student feed uses the approved filters (category, keyword, `sourceLabel`, `organizationId`, Puerto Rico `deadlineOnOrBefore`) and shows `sourceLabel` apart from `source`. The company page shows the persisted `approvalStatus`, including the pending owner's read-only view.
- Several eligible organizations: the office and company pages ask for an organization ID after the backend's 409. **Backend dependency:** no endpoint lists a user's organizations, so the user must enter the ID.
- Checked in a real browser (headless Chrome) against a disposable backend seeded with synthetic data.
- Write flows (save, respond, publish, report, review) remain disabled until their endpoints exist. No account can get `STUDENT` or `COMPANY` through the API, and there is no user search.
- Admin = `MODERATOR` has no ADR on `main`. Team review and hosted CI are pending.

### Issue #14: Frontend: register, verify and login screens

Sources: [canonical #14](https://github.com/1uismar33r0/Colegio_Connect/issues/14), open. The alias #51 recorded at the 2026-10-26 snapshot no longer resolves on GitHub (checked 2026-11-08), so #14 is the only live source. Lead: **Luis**. Support: Cristian (API contract), Gabriel (review), per Table 17 frontend group. GitHub assignee: **1uismar33r0** (Luis), checked 2026-11-08.

Milestone: **M1 - Access foundation**. Original issue references: US1, I15. Proposal mapping: **O2, O5; I5, I15; US1; Figure 3**.

Engineering state: **Verified on branch** (2026-11-07). Acceptance note: criteria/DoD still pending team review and PR CI. Location: feature branch `feat/issue-14-auth-screens` (based on main `e5cc181`). Gabriel's earlier `gabriel/issue-14-react-auth-foundation` (PR #97) predates the #13 frontend on main and is superseded by it.

Evidence / blockers (apply to every AC below): **[TE-10](../testing/test-evidence.md#te-10)**. AC1: page tests plus a live API check of every request the screens send; a browser-driven end-to-end run is not yet recorded. AC2: axe-core reports no violations (any impact) in jsdom for each page's initial and error states; colour contrast still needs a real-browser check.

Exact source description shared by both issues:

> UI for registration, code entry, resend, login and error states.

Acceptance text shared by both issues:

- **AC1** / `TC-14-AC1`: Full register -> verify -> login flow works against the API
- **AC2** / `TC-14-AC2`: axe-core: no Critical/Serious findings

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Forms with validation and accessible error messages
- [ ] Keyboard-only flow
- [ ] Handle rate-limit and expired-code responses

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #15 / alias #53: Audit log foundation

Sources: [canonical #15](https://github.com/1uismar33r0/Colegio_Connect/issues/15) and [alias #53](https://github.com/1uismar33r0/Colegio_Connect/issues/53); both open. Lead: **Gabriel**. Support: Luis (end-to-end tests), Cristian (role checks), per Table 17 moderation/audit/recovery group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M1 - Access foundation**. Original issue references: I8, Figure 2 block D. Proposal mapping: **O3; I8; US2, US4, US11, US15; Figure 2 block D**.

Engineering state: **Verified on branch**. Acceptance note: foundation tasks and the first approval/rejection audit caller are implemented and locally verified; teammate review, consolidated-head hosted CI, merge, other administrative actions, and final I8 coverage remain pending. Location: `gabriel/issue-15-audit-reconciliation`, implementation commits `132413bb8076371285cdee5a7bd18f6a92871278`, `c0b01a7afc6c8b61eb4ec1cc44259c78658378d8`, and company decision `d43afff1fc2660b8cc5191401c669caaa6084eaf`.

Evidence / blockers (apply to every AC below): **TE-09** and the [combined verification](../testing/test-evidence.md#te-12), plus Gabriel's [Issue #15 engineering record](../gabriel/security/issue-15-audit-foundation.md). V4 audit, same-transaction service, bounded moderator reads, and Issue #16 company decision audit are present on the branch. Role changes, moderation, and sponsorship still have no audit-producing service, so AC1 and final I8 remain incomplete.

Exact source description shared by both issues:

> AuditService used by every administrative and sponsorship action: actor, action, target, timestamp.

Acceptance text shared by both issues:

- **AC1** / `TC-15-AC1`: Role changes (and later approvals, moderation, sponsorship acceptances) create audit records

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] audit_log migration
- [ ] AuditService.record(...) in same transaction as the action
- [ ] Admin read endpoint (paginated)
- [ ] Tests proving a failed action leaves no audit row and a successful one always does

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #16 / alias #55: Company registration and approval workflow

Sources: [canonical #16](https://github.com/1uismar33r0/Colegio_Connect/issues/16) and [alias #55](https://github.com/1uismar33r0/Colegio_Connect/issues/55); both open. Lead: **Gabriel**. Support: Cristian (role checks), Luis (screens), per Table 17 approval/lifecycle/ZAP group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M2 - Publishing and discovery**. Original issue references: US3, US4, I6. Proposal mapping: **O2, O3; I6, I8; US3–US4; Figures 2–3**.

Engineering state: **Verified on branch for the approval backend slice**. Acceptance note: a synthetic pending-company queue and atomic approval/rejection with an audit row are locally verified; registration, real applicant data, moderator UI, decision notifications, publishing denial, teammate review, and merge remain pending. Location: [PR #119](https://github.com/1uismar33r0/Colegio_Connect/pull/119) branch after local consolidation of former stacked [PR #120](https://github.com/1uismar33r0/Colegio_Connect/pull/120), code commits `d43afff`, `a8981f9`, and `fcd9224`.

Evidence / blockers (apply to every AC below): [TE-12](../testing/test-evidence.md#te-12) and Gabriel's [Issue #16 engineering record](../gabriel/security/issue-16-company-decision.md). V5 follows audit V4; the November 10 combined branch passed 64 backend tests and an isolated V1–V3-to-V5 preservation test. AC2 is locally demonstrated for this decision operation but not yet accepted; AC1 cannot pass without publishing and its authorization tests.

Exact source description shared by both issues:

> EXTERNAL_ORGANIZATION accounts register with org details + business email and gain company capabilities only after admin approval.

Acceptance text shared by both issues:

- **AC1** / `TC-16-AC1`: Approved company can publish; pending/rejected cannot
- **AC2** / `TC-16-AC2`: Decision audited

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Registration endpoint + form
- [ ] Admin approval queue: approve / reject with reason
- [ ] Unapproved companies blocked from publishing (negative test)
- [ ] Audit record + notification on decision

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #17 / alias #57: Source labels and trust/review rules

Sources: [canonical #17](https://github.com/1uismar33r0/Colegio_Connect/issues/17) and [alias #57](https://github.com/1uismar33r0/Colegio_Connect/issues/57); both open. Lead: **Gabriel**. Support: Cristian (role checks), Luis (screens), per Table 17 approval/lifecycle/ZAP group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M2 - Publishing and discovery**. Original issue references: Table 9, C9. Proposal mapping: **O1, O3; I1, I9; US5; Figures 2, 4**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Every listing shows a source label that decides review path.

Acceptance text shared by both issues:

- **AC1** / `TC-17-AC1`: Only new or flagged posters route to review (C9 test)
- **AC2** / `TC-17-AC2`: Label visible on every listing

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Labels: Verified UPRM institution / Approved external company / Verified student project
- [ ] Trusted-poster logic: first N company listings reviewed, then direct publish
- [ ] Flagged-poster override
- [ ] Expose label in API + UI badge

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #18 / alias #59: Listing domain model for six categories

Sources: [canonical #18](https://github.com/1uismar33r0/Colegio_Connect/issues/18) and [alias #59](https://github.com/1uismar33r0/Colegio_Connect/issues/59); both open. Lead: **Cristian**. Support: Luis (components), Gabriel (API review), per Table 17 listing/publisher group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M2 - Publishing and discovery**. Original issue references: US5, I1, Figure 2 block B. Proposal mapping: **O1; I1; US5; Figure 2 block B**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Shared listing core plus per-category required fields for job, internship, COOP, research position, research participation, capstone.

Acceptance text shared by both issues:

- **AC1** / `TC-18-AC1`: All 6 categories can be created and retrieved
- **AC2** / `TC-18-AC2`: Validation rejects missing required fields

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Migration: listing + category-specific fields (decide: typed columns vs. validated JSON)
- [ ] Validation of required fields per category
- [ ] Research participation supports external form link (Google Forms/Qualtrics)
- [ ] Create/edit/get endpoints with ownership checks

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #19 / alias #61: Listing lifecycle state machine (Figure 4)

Sources: [canonical #19](https://github.com/1uismar33r0/Colegio_Connect/issues/19) and [alias #61](https://github.com/1uismar33r0/Colegio_Connect/issues/61); both open. Lead: **Gabriel**. Support: Cristian (role checks), Luis (screens), per Table 17 approval/lifecycle/ZAP group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M2 - Publishing and discovery**. Original issue references: I1, Figure 4. Proposal mapping: **O1, O3; I1, I8; US5, US11; Figure 4**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Draft -> In review -> Published/Rejected; Published <-> Closed; Published -> Hidden -> Removed/cleared. Illegal transitions rejected.

Acceptance text shared by both issues:

- **AC1** / `TC-19-AC1`: State changes match Figure 4 exactly
- **AC2** / `TC-19-AC2`: Admin transitions audited

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] State enum + transition service
- [ ] Deadline auto-close job
- [ ] Owner close / renew
- [ ] Audit admin transitions
- [ ] Unit tests for every transition and illegal transition

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #20 / alias #62: Publisher UI: create, edit and manage listings

Sources: [canonical #20](https://github.com/1uismar33r0/Colegio_Connect/issues/20) and [alias #62](https://github.com/1uismar33r0/Colegio_Connect/issues/62); both open. Lead: **Cristian**. Support: Luis (components), Gabriel (API review), per Table 17 listing/publisher group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M2 - Publishing and discovery**. Original issue references: US5. Proposal mapping: **O1; I1; US5; Figures 3–4**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Per-category forms, draft/submit, my-listings dashboard.

Acceptance text shared by both issues:

- **AC1** / `TC-20-AC1`: Faculty/office and approved company users can publish each allowed category end to end

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Category-specific forms with validation
- [ ] Draft, submit, close, renew actions
- [ ] Status and review messages
- [ ] Accessible form patterns

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #21 / alias #63: Discovery API: feed, keyword search, filters, pagination

Sources: [canonical #21](https://github.com/1uismar33r0/Colegio_Connect/issues/21) and [alias #63](https://github.com/1uismar33r0/Colegio_Connect/issues/63); both open. Lead: **Luis**. Support: Gabriel (queries), Cristian (API review), per Table 17 discovery/k6/axe group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M2 - Publishing and discovery**. Original issue references: US6, I2, I3, C4. Proposal mapping: **O1, O2; I1, I2, I3, I7; US6; Figure 2 block B**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Single feed across six categories with keyword search and filters by category, source, organization and deadline.

Acceptance text shared by both issues:

- **AC1** / `TC-21-AC1`: 18 cases return only matching records (I2)
- **AC2** / `TC-21-AC2`: p95 < 2 s under test workload, errors < 1% (I3)

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Query endpoint with pagination and stable sorting
- [ ] Indexes (incl. full-text) on search/filter fields
- [ ] Only Published listings returned
- [ ] No private student fields in responses (I7)
- [ ] 18 predefined search/filter test cases

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #22 / alias #64: Discovery UI: feed, search, filters, listing detail

Sources: [canonical #22](https://github.com/1uismar33r0/Colegio_Connect/issues/22) and [alias #64](https://github.com/1uismar33r0/Colegio_Connect/issues/64); both open. Lead: **Luis**. Support: Gabriel (queries), Cristian (API review), per Table 17 discovery/k6/axe group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M2 - Publishing and discovery**. Original issue references: US6, I15. Proposal mapping: **O1, O5; I1, I2, I15; US6; Figure 3**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Browse/search/filter experience with source labels and deadline visibility.

Acceptance text shared by both issues:

- **AC1** / `TC-22-AC1`: All primary discovery tasks completable by keyboard
- **AC2** / `TC-22-AC2`: axe-core clean of Critical/Serious

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Feed with filters and URL-synced state
- [ ] Listing detail page + report entry point
- [ ] Empty/loading/error states
- [ ] Keyboard-only navigation

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #23 / alias #65: Saved listings (save, unsave, revisit)

Sources: [canonical #23](https://github.com/1uismar33r0/Colegio_Connect/issues/23) and [alias #65](https://github.com/1uismar33r0/Colegio_Connect/issues/65); both open. Lead: **Luis**. Support: Gabriel (queries), Cristian (API review), per Table 17 discovery/k6/axe group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M2 - Publishing and discovery**. Original issue references: US7, I4. Proposal mapping: **O1; I4; US7; Figures 2–3**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Verified students can save listings in any category and revisit them.

Acceptance text shared by both issues:

- **AC1** / `TC-23-AC1`: Save/unsave/revisit works in all 6 categories (I4)

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] saved_listing table + endpoints
- [ ] Saved page with closed/hidden listing handling
- [ ] Tests across all 6 categories

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #25 / alias #66: Synthetic workload generator and k6 load tests

Sources: [canonical #25](https://github.com/1uismar33r0/Colegio_Connect/issues/25) and [alias #66](https://github.com/1uismar33r0/Colegio_Connect/issues/66); both open. Lead: **Gabriel**. Support: Luis (proposal k6 lead), Cristian (API review); reconcile issue lead with Table 17 before implementation. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M2 - Publishing and discovery**. Original issue references: Section 3(b), I3. Proposal mapping: **O1; I3; US6; Figure 1**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Repeatable dataset (>=5,000 listings, 1,000 users, 50 orgs) and 25-VU discovery load test; results recorded each sprint.

Acceptance text shared by both issues:

- **AC1** / `TC-25-AC1`: Dataset reproducible from one command
- **AC2** / `TC-25-AC2`: Report with p95 and error rate produced

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Seed script (generated data only, no real students)
- [ ] k6 scenarios for feed/search/filter
- [ ] CI/manual job + results file in /docs/results
- [ ] Record CPU, memory, duration, response times, error rate

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #26 / alias #67: OWASP ZAP scan setup

Sources: [canonical #26](https://github.com/1uismar33r0/Colegio_Connect/issues/26) and [alias #67](https://github.com/1uismar33r0/Colegio_Connect/issues/67); both open. Lead: **Gabriel**. Support: Cristian (role checks), Luis (screens), per Table 17 approval/lifecycle/ZAP group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M2 - Publishing and discovery**. Original issue references: I14. Proposal mapping: **O5; I14; all protected workflows; Figure 1**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Automated ZAP baseline/full scan against deployed build with authenticated context.

Acceptance text shared by both issues:

- **AC1** / `TC-26-AC1`: Scan runs end of each sprint; 0 unresolved High/Critical by Week 8

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] ZAP in CI/scheduled workflow
- [ ] Authenticated scan configuration
- [ ] Triage process for findings

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #28 / alias #68: Accessibility testing setup (axe-core + keyboard checklist)

Sources: [canonical #28](https://github.com/1uismar33r0/Colegio_Connect/issues/28) and [alias #68](https://github.com/1uismar33r0/Colegio_Connect/issues/68); both open. Lead: **Luis**. Support: Gabriel (queries), Cristian (API review), per Table 17 discovery/k6/axe group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M2 - Publishing and discovery**. Original issue references: I15, WCAG 2.2 AA. Proposal mapping: **O5; I15; all primary workflows; Figure 3**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Automated axe checks in Playwright plus a manual keyboard-only checklist.

Acceptance text shared by both issues:

- **AC1** / `TC-28-AC1`: 0 unresolved Critical/Serious findings by Week 8

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] axe-core in Playwright
- [ ] Keyboard checklist for primary workflows
- [ ] Results tracked per sprint

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #31 / alias #69: Student responses with selectable shared profile fields

Sources: [canonical #31](https://github.com/1uismar33r0/Colegio_Connect/issues/31) and [alias #69](https://github.com/1uismar33r0/Colegio_Connect/issues/69); both open. Lead: **Cristian**. Support: Gabriel (authorization review), Luis (screens), per Table 17 responses/privacy group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M3 - Responses and moderation**. Original issue references: US8, I7, I10. Proposal mapping: **O2, O4; I7, I10; US8; Figures 2–3**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Students respond to a listing and choose which profile fields to share; consent recorded.

Acceptance text shared by both issues:

- **AC1** / `TC-31-AC1`: Only chosen fields are stored/shared

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] response table with shared_fields snapshot
- [ ] Response endpoint + UI with field selection
- [ ] Duplicate response handling
- [ ] Withdraw response

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #33 / alias #70: Publisher respondent view (own listings only)

Sources: [canonical #33](https://github.com/1uismar33r0/Colegio_Connect/issues/33) and [alias #70](https://github.com/1uismar33r0/Colegio_Connect/issues/70); both open. Lead: **Cristian**. Support: Gabriel (authorization review), Luis (screens), per Table 17 responses/privacy group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M3 - Responses and moderation**. Original issue references: US9, I6, I7. Proposal mapping: **O2; I6, I7; US9; Figures 2–3**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Publishers see only respondents to their own listings, and only shared fields. Companies can never browse students.

Acceptance text shared by both issues:

- **AC1** / `TC-33-AC1`: Cross-org access returns 403/404
- **AC2** / `TC-33-AC2`: 0 private student fields leak in any non-respondent response (I7)

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Endpoint scoped by ownership/org membership
- [ ] Cross-organization access negative tests
- [ ] No student search endpoint exists for companies

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #35 / alias #71: Privacy API-response test suite

Sources: [canonical #35](https://github.com/1uismar33r0/Colegio_Connect/issues/35) and [alias #71](https://github.com/1uismar33r0/Colegio_Connect/issues/71); both open. Lead: **Gabriel**. Support: Cristian (response/data API), Luis (screen/E2E cases), proposed coordination for privacy test evidence. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M3 - Responses and moderation**. Original issue references: I7, C6. Proposal mapping: **O2; I7; US6–US9; Figures 2–3**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Automated tests scanning API responses for private student fields across browse, search, save and unauthorized requests.

Acceptance text shared by both issues:

- **AC1** / `TC-35-AC1`: 0 private fields found; suite runs in CI

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Define private-field list
- [ ] Response-scanning helper
- [ ] Cover every endpoint returning listings/users

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #37 / alias #72: Reporting: users report suspicious listings

Sources: [canonical #37](https://github.com/1uismar33r0/Colegio_Connect/issues/37) and [alias #72](https://github.com/1uismar33r0/Colegio_Connect/issues/72); both open. Lead: **Gabriel**. Support: Luis (end-to-end tests), Cristian (role checks), per Table 17 moderation/audit/recovery group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M3 - Responses and moderation**. Original issue references: US10, Figure 2 block B. Proposal mapping: **O3; I8, I9; US10; Figure 2 block B, Figure 4**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Report with reason; threshold auto-hide; moderator queue input.

Acceptance text shared by both issues:

- **AC1** / `TC-37-AC1`: Report -> hide path works and is audited

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] report table + endpoint (one report per user per listing)
- [ ] Report threshold -> Hidden transition
- [ ] Report dialog UI

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #39 / alias #73: Moderation tools: review queue, hide/remove/clear, account restrictions

Sources: [canonical #39](https://github.com/1uismar33r0/Colegio_Connect/issues/39) and [alias #73](https://github.com/1uismar33r0/Colegio_Connect/issues/73); both open. Lead: **Gabriel**. Support: Luis (end-to-end tests), Cristian (role checks), per Table 17 moderation/audit/recovery group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M3 - Responses and moderation**. Original issue references: US11, I8, I9. Proposal mapping: **O3; I8, I9; US11; Figures 3–4**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Moderators review reports and in-review listings, hide/remove/clear, restrict accounts; works with no UPRM office organization.

Acceptance text shared by both issues:

- **AC1** / `TC-39-AC1`: Report -> hide -> clear/remove works end to end (I9)
- **AC2** / `TC-39-AC2`: 100% admin actions audited (I8)

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Moderation queue UI + endpoints
- [ ] Hide / clear / remove actions per Figure 4
- [ ] Account restriction (suspend/limit) enforced server-side
- [ ] Audit every action
- [ ] E2E test in an environment with no office organization

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #40 / alias #74: Required notification emails

Sources: [canonical #40](https://github.com/1uismar33r0/Colegio_Connect/issues/40) and [alias #74](https://github.com/1uismar33r0/Colegio_Connect/issues/74); both open. Lead: **Luis**. Support: Cristian (API), Gabriel (privacy tests), per Table 17 notifications/company-capstone/usability-preparation group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M3 - Responses and moderation**. Original issue references: I12. Proposal mapping: **O4; I12; US1, US14–US15; Figure 1**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Verification code, Express Interest received, request accepted/declined (plus approval decision).

Acceptance text shared by both issues:

- **AC1** / `TC-40-AC1`: 100% of required notifications sent in test runs (I12)

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Templates + triggers
- [ ] Delivery logging
- [ ] Integration tests with fake sender

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #42 / alias #75: Company capstone project calls

Sources: [canonical #42](https://github.com/1uismar33r0/Colegio_Connect/issues/42) and [alias #75](https://github.com/1uismar33r0/Colegio_Connect/issues/75); both open. Lead: **Luis**. Support: Cristian (API), Gabriel (privacy tests), per Table 17 notifications/company-capstone/usability-preparation group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M3 - Responses and moderation**. Original issue references: US12, I10. Proposal mapping: **O4; I10; US12; Figures 2–3**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Approved companies publish capstone calls; students respond; company receives only data the student chose to share.

Acceptance text shared by both issues:

- **AC1** / `TC-42-AC1`: Company-originated route works end to end (I10)

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Capstone call as a listing category
- [ ] Student response reuses response flow
- [ ] E2E test of company-originated route

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #44 / alias #76: Recovery rehearsal and health checks

Sources: [canonical #44](https://github.com/1uismar33r0/Colegio_Connect/issues/44) and [alias #76](https://github.com/1uismar33r0/Colegio_Connect/issues/76); both open. Lead: **Gabriel**. Support: Luis (end-to-end tests), Cristian (role checks), per Table 17 moderation/audit/recovery group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M3 - Responses and moderation**. Original issue references: I16. Proposal mapping: **O5; I16; infrastructure enabler; Figure 1**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Prove the service is healthy within 5 minutes after restart with no data loss.

Acceptance text shared by both issues:

- **AC1** / `TC-44-AC1`: Documented recovery test result < 5 min, no data loss

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Restart procedure + script
- [ ] Persistent volume/backups verified
- [ ] Measure time to healthy

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #46 / alias #77: Usability test plan and task scripts

Sources: [canonical #46](https://github.com/1uismar33r0/Colegio_Connect/issues/46) and [alias #77](https://github.com/1uismar33r0/Colegio_Connect/issues/77); both open. Lead: **Luis**. Support: Cristian (API), Gabriel (privacy tests), per Table 17 notifications/company-capstone/usability-preparation group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M3 - Responses and moderation**. Original issue references: I17. Proposal mapping: **O5; I17; all role workflows; Figure 3**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Prepare moderated sessions with >=6 volunteers across roles.

Acceptance text shared by both issues:

- **AC1** / `TC-46-AC1`: Plan reviewed with advisor

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Role-appropriate task scripts
- [ ] Success criteria and consent form
- [ ] Recruit volunteers (students, faculty, company rep)

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #48 / alias #78: Student teams and capstone project publishing

Sources: [canonical #48](https://github.com/1uismar33r0/Colegio_Connect/issues/48) and [alias #78](https://github.com/1uismar33r0/Colegio_Connect/issues/78); both open. Lead: **Cristian**. Support: Luis (screens), Gabriel (security review), per Table 17 sponsorship group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M4 - Sponsorship and final acceptance**. Original issue references: US13, Figure 2 block C. Proposal mapping: **O4; I11; US13; Figure 2 block C, Figures 3–4**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Students form a team, publish a project and describe needed support (mentorship, equipment, funding, data, facilities); reviewed before publication.

Acceptance text shared by both issues:

- **AC1** / `TC-48-AC1`: Student project publishes through review; no contact fields in public API

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] capstone_team, team_member, capstone_project migrations
- [ ] Team management endpoints + UI
- [ ] Project listing labeled 'Verified student project'
- [ ] Private contact data never part of public listing

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #50 / alias #79: Express Interest: company -> student project

Sources: [canonical #50](https://github.com/1uismar33r0/Colegio_Connect/issues/50) and [alias #79](https://github.com/1uismar33r0/Colegio_Connect/issues/79); both open. Lead: **Cristian**. Support: Luis (screens), Gabriel (security review), per Table 17 sponsorship group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M4 - Sponsorship and final acceptance**. Original issue references: US14, I12. Proposal mapping: **O4; I11, I12; US14; Figure 2 block C, Figure 3**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Approved companies send Express Interest to a student project; team notified.

Acceptance text shared by both issues:

- **AC1** / `TC-50-AC1`: Request created and team notified; company cannot see contact data

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] interest_request table + endpoint
- [ ] Prevent duplicates/spam (limits)
- [ ] Notification to team members

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #52 / alias #80: Accept/decline interest and consent-based contact release

Sources: [canonical #52](https://github.com/1uismar33r0/Colegio_Connect/issues/52) and [alias #80](https://github.com/1uismar33r0/Colegio_Connect/issues/80); both open. Lead: **Cristian**. Support: Luis (screens), Gabriel (security review), per Table 17 sponsorship group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M4 - Sponsorship and final acceptance**. Original issue references: US15, I6, I8, I11. Proposal mapping: **O2, O3, O4; I6, I8, I11; US15; Figure 2 blocks C, D, Figure 3**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Only team members can decide; contact info is released only after acceptance; decision audited. Pair-program this code.

Acceptance text shared by both issues:

- **AC1** / `TC-52-AC1`: 0 private contact fields before acceptance (I11)
- **AC2** / `TC-52-AC2`: Acceptance audited (I8)

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Accept/decline endpoints with team-membership check
- [ ] Contact release logic gated on accepted status
- [ ] Audit record + notification
- [ ] Negative test: non-team member cannot accept
- [ ] E2E test: 0 contact fields before acceptance

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #54 / alias #81: Final acceptance suite (20 scenarios) and CI gating

Sources: [canonical #54](https://github.com/1uismar33r0/Colegio_Connect/issues/54) and [alias #81](https://github.com/1uismar33r0/Colegio_Connect/issues/81); both open. Lead: **Luis**. Support: Cristian and Gabriel (workflow/API/security evidence), proposed coordination for shared final acceptance. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M4 - Sponsorship and final acceptance**. Original issue references: I13, Table 18. Proposal mapping: **O1–O5; I1–I17; US1–US15; Figures 1–4**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> End-to-end suite covering all indicators I1-I17 against the deployed build.

Acceptance text shared by both issues:

- **AC1** / `TC-54-AC1`: 0 unhandled server errors; all required tests pass in CI

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] 20 core workflow scenarios in Playwright
- [ ] Traceability table indicator -> test
- [ ] Run against deployed build

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #56 / alias #82: Final security, performance and recovery hardening

Sources: [canonical #56](https://github.com/1uismar33r0/Colegio_Connect/issues/56) and [alias #82](https://github.com/1uismar33r0/Colegio_Connect/issues/82); both open. Lead: **Gabriel**. Support: Cristian (fixes), Luis (verification), per Table 17 final hardening group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M4 - Sponsorship and final acceptance**. Original issue references: I3, I14, I16. Proposal mapping: **O1, O5; I3, I14, I16; all protected workflows; Figure 1**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Final ZAP, k6 and recovery runs; fix findings; ASVS L1 checklist pass.

Acceptance text shared by both issues:

- **AC1** / `TC-56-AC1`: Targets I3, I14, I16 met and recorded

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Fix High/Critical ZAP findings
- [ ] Query tuning if p95 > 2 s
- [ ] ASVS L1 checklist review
- [ ] Dependency license review

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #58 / alias #83: Usability sessions, accessibility sign-off and user guide

Sources: [canonical #58](https://github.com/1uismar33r0/Colegio_Connect/issues/58) and [alias #83](https://github.com/1uismar33r0/Colegio_Connect/issues/83); both open. Lead: **Luis**. Support: Cristian and Gabriel, per Table 17 final usability/accessibility/report group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M4 - Sponsorship and final acceptance**. Original issue references: I15, I17. Proposal mapping: **O5; I15, I17; US1–US15; Figure 3**.

Engineering state: **Not started**. Acceptance note: criteria/DoD still pending. Location: No feature implementation located on main; domain package placeholders are not implementation.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). Implement the listed tasks and exercise every acceptance clause; prerequisite access/data/UI workflows and CI integration remain incomplete.

Exact source description shared by both issues:

> Run >=6 sessions, fix issues, final a11y verification, write user guide.

Acceptance text shared by both issues:

- **AC1** / `TC-58-AC1`: At least 80% overall task success (I17)
- **AC2** / `TC-58-AC2`: 0 Critical/Serious a11y findings (I15)

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Run sessions and record task success
- [ ] Fix top issues
- [ ] Final axe + keyboard pass
- [ ] User guide per role

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.

### Issue #60 / alias #84: Final report, demo and presentation

Sources: [canonical #60](https://github.com/1uismar33r0/Colegio_Connect/issues/60) and [alias #84](https://github.com/1uismar33r0/Colegio_Connect/issues/84); both open. Lead: **Luis**. Support: Cristian and Gabriel, per Table 17 final usability/accessibility/report group. GitHub assignees: canonical **unassigned**; alias **unassigned**.

Milestone: **M4 - Sponsorship and final acceptance**. Original issue references: Section 6(b). Proposal mapping: **O1–O5; I1–I17; US1–US15; Figures 1–4**.

Engineering state: **In progress**. Acceptance note: criteria/DoD still pending. Location: A1 documentation branch; report evidence planning only, no final report/presentation delivery.

Evidence / blockers (apply to every AC below): **TE-00** in [test evidence](../testing/test-evidence.md). The [midterm evidence index](../reports/midterm/evidence-index.md) supports report preparation. Final results, handoff decision, demo/slides and actual delivery remain incomplete; midterm planning is not final-report acceptance.

Exact source description shared by both issues:

> Collect final results for I1-I17, hosting specs, handoff decision and demo script.

Acceptance text shared by both issues:

- **AC1** / `TC-60-AC1`: Report and presentation delivered before end of Fall 2026

Exact source tasks (checkboxes describe the issue snapshot):

- [ ] Results tables for I1-I17
- [ ] Handoff owner/cost decision with advisor
- [ ] Demo script for both capstone routes
- [ ] Slides

Definition of Done: the three common requirements above apply; no additional acceptance is inferred from task checks or merges.
