# CI and database integration checkpoint

Observed 2026-10-26 UTC using authenticated GitHub CLI as Gabriel (`DCGabriel10`), native Git fetch, source inspection and the commands below. This follow-up is based on documentation PR #90; it does not modify that PR's branch. The earlier A1 documents remain dated historical checkpoints.

## Plan and scope

INSPECT → ALIGN WITH PROPOSAL → CHECK ISSUES → PLAN → IMPLEMENT → TEST → DOCUMENT → REVIEW.

Combine existing CI and database histories on an isolated validation branch; run the unchanged workflow; record actual results and remaining acceptance. Proposal M1 and Figure 2 identity requirements apply. Issue #6/#34 requires “Migrations apply cleanly to an empty DB in CI” and “Schema matches Figure 2 (identity block)”. This check addresses migration execution; schema design, role contracts and team review remain separate gates. Issue #2/#27 owns CI, #3/#33 owns the Docker development stack.

Ruling: native PowerShell/Git replaces the previously problematic Superpowers Windows helpers. Existing implementation is integrated unchanged, so no new behavior or artificial failing test is introduced. The existing tests are the verification target.

## Starting references and preserved work

All three original worktrees were clean before integration. Fetch succeeded; no remote tip advanced from the preceding A1 checkpoint. Fetch makes team commits locally available; it does not merge them into a working branch.

| Reference / worktree | Starting revision | Outcome |
| --- | --- | --- |
| `origin/main` | `1714e5e0f000f9ad4a2ff6a00bf71c2e14fc5b18` | Integration base; unchanged |
| Local `main` | `638efe8522a0df0aec90411beaf9a58829c78428` | Older cached local branch; preserved, not used as baseline |
| Docker: `Colegio_Connect`, `feat/issue-3-docker-development` | `233485c2458885456a9dcc593b927e5382d5689e` | Preserved |
| CI: temporary-directory worktree `Colegio_Connect-issue-2-ci`, `feat/issue-2-ci` | `58089c4d4fdcef14d827dc64727ba02afcb76a22` | Preserved; whole history merged into validation |
| Database: `origin/issue-6-database-baseline` (no local worktree) | `af24f8987df1a4048af4b9b4789a9582f816fc85` | Preserved; whole history merged into validation |
| Wireframe: `origin/Issue-13-Wireframes` (no local worktree) | `b456f772ac7a307f165a0379be025af3d8d2b00d` | Preserved; not integrated |
| A1: `Colegio_Connect-A1-docs`, `docs/a1-verified-project-baseline` | `cf89e737f2849bf8b67198464fb4cc2be05c6aba` | Preserved; base of separate documentation follow-up |

Live PRs [#88](https://github.com/1uismar33r0/Colegio_Connect/pull/88), [#89](https://github.com/1uismar33r0/Colegio_Connect/pull/89), and [#90](https://github.com/1uismar33r0/Colegio_Connect/pull/90) were OPEN, not merged. #88 and #89 had no formal reviews. #85–#87 were already merged. No original PR was changed.

## Incorporated changes

New worktree: `Colegio_Connect-integration-ci-db`; branch: `validation/ci-database-2026-10-26`.

1. From freshly fetched main, merge the complete CI branch with `--no-ff`: `d914634a932101a2ec7d9bf1ebdaf441578e31c3`.
2. Merge the complete database branch with `--no-ff`: **`94ca8521165483596e3bf612222ac44464a3c09e`**.

Both merges were conflict-free. The combined backend is byte-for-byte identical to the database branch's tracked backend; `.github` is identical to the CI branch. Existing commits/authorship are retained. No application, migration or workflow fix was needed during integration. The combined change is 22 files relative to main, all inherited from the two branches. Credential-pattern scan found no token/private-key patterns; no populated environment file was added. `git diff --check` passed.

Documentation follow-up: worktree `Colegio_Connect-integration-docs`, branch `docs/ci-database-validation-2026-10-26`, based on `cf89e73`. This new record is additive to avoid conflicts with #90. Incorporate it after #90 through review; do not merge the validation branch as a substitute for reviewing the original PRs.

## Verification

All combined-code results below apply only to `94ca8521165483596e3bf612222ac44464a3c09e`, not main or the unchanged PR #88 head.

| Check | Procedure / environment | Result and limits |
| --- | --- | --- |
| Local Java | `java -version`, Windows | Java 17.0.10; Java 25 Maven verification not run on host |
| Local Docker | `docker version`, `docker ps`, authenticated native context `desktop-linux` | BLOCKED: Docker Desktop Linux engine named pipe absent. Client 29.5.2 installed. No containers or volumes changed; root cause beyond unavailable engine not established |
| Compose syntax | `docker compose --project-directory Colegio_Connect-integration-ci-db --env-file Colegio_Connect-integration-ci-db/.env.example config --quiet` | PASS, exit 0. Configuration validation only; no runtime claim |
| Existing hosted CI | Push validation branch; unchanged `.github/workflows/ci.yml`; [run 40274200394](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/40274200394), push trigger, Java 25.0.4.1 | PASS. Backend, Container and CI Gate each succeeded at tested SHA `94ca8521165483596e3bf612222ac44464a3c09e`. Maven `verify`: 3 tests, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS`. Compose validation and Docker image build succeeded. |
| Real database context | `ColegioConnectApplicationTests.contextLoads` in Backend job | PASS. Testcontainers started `postgres:18.6` (log reports PostgreSQL 18.6). Flyway found an empty `public` schema and successfully applied `V1__identity_baseline.sql`, reaching v1. JPA `EntityManagerFactory` initialized with `ddl-auto=validate`; Spring Boot test context started. This supports #6 AC1 on the validation branch and JPA mapping compatibility for the loaded entities. It does not by itself prove every Figure 2 field or business rule. |
| Deployed/Compose HTTP health | No engine locally; existing CI does not start Compose or call `/actuator/health` | NOT RUN on combined revision. Context initialization is not an HTTP/container health test |

[GitHub Actions run 40274200394](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/40274200394) is the durable test reference. Its exact SHA is the combined revision above. The run completed successfully at 2026-10-26 05:27:13Z. Evidence was downloaded into the operator's `Integration-evidence` directory outside Git. The current workflow does not upload Surefire XML; job logs provide the hosted test summary. The main-only image artifact was skipped as configured for a non-main push. The `ERROR` stack trace in `GlobalExceptionHandlerTest` is deliberately produced by its negative-path test; Maven reports that test class passed (2 tests, 0 failures). No integration failure or code fix was observed.

## Remaining acceptance and safe synchronization

1. Cristian/Luis review #89; integrate via the approved team workflow. Gabriel verifies the ensuing main run and its main-only image artifact. Passing checks alone do not establish repository enforcement.
2. Cristian updates #88 from the resulting main with a normal merge, preserving pushed history; resolve conflicts deliberately. Run CI on that exact PR revision. This validation run supplies cross-branch evidence, not approval or a permanent check on #88.
3. Cristian and Gabriel review Figure 2 schema correspondence and resolve/document the #9 role identifier contract with Luis. The enum difference remains a contract risk, not an observed runtime failure. Context-load tests do not cover CRUD, uniqueness, foreign-key rejection, permissions or consent scenarios.
4. Run combined Compose startup and HTTP health when Docker is available, using a separate project name, isolated synthetic credentials/volume and unused loopback ports. Preserve existing development data. Record revision, Flyway/JPA startup logs and health response.
5. Team reviews #88 and #90; only then use their reviewed main baseline for new feature work. Fetch first, inspect main/PRs, create new branches from current `origin/main`. Update active branches from main only when appropriate to their PR; no reset, force push or indiscriminate cross-feature merges.
6. Gabriel's next actionable task is supporting #89 review/main CI verification and #88's exact-head database verification. Luis handles repository permission/enforcement decisions; Cristian owns database integration. Authentication and API contracts remain M1 dependencies.

For the November 10 midterm, use this record and linked run as infrastructure evidence. It is not performance, accessibility, security, authorization, recovery, deployed application or full M1 acceptance evidence. Advisor feedback, preliminary load measurements and deployment/runtime screenshots remain missing as tracked in the [midterm index](../reports/midterm/evidence-index.md).

## 2026-11-09 manual CI verification follow-up (Issue #2/#27)

The existing `CI` workflow accepted pushes to `main` and pull requests targeting `main`, but had no `workflow_dispatch` trigger. This left no manual hosted verification path for unmerged feature branches such as draft PR #120, whose published head was `950fe4f8a4294848afd662473142cecd64b27761` at the check. Its 63/63 backend result on tree `aab4b58654d64e20c3ba991e8c892b4adb26794b` is **local** evidence, not a hosted run.

This separate Gabriel CI change adds only `workflow_dispatch:` to `.github/workflows/ci.yml`. Existing main push and main-target PR filters, verification jobs, read-only permission, and the main-push-only image save/upload conditions remain in place. Manual runs can execute Backend, Container, Frontend and CI Gate on the selected ref; they cannot activate the image publication steps because those require a push event on `refs/heads/main`.

Local validation: `git diff --check` passed. The repository's installed `js-yaml` parsed the workflow; structural assertions against `origin/main` confirmed the only workflow change is the new trigger, while filters, jobs, permissions, and publication guards are unchanged. No Actions-aware validator was available locally. These checks do not execute GitHub jobs. Hosted verification of #120 remains open until an identified run executes its required jobs on an identified revision.

GitHub requires a `workflow_dispatch` workflow file on the default branch before manual dispatch is available, and the selected branch or tag determines the run's `GITHUB_REF` and `GITHUB_SHA` ([event reference](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#workflow_dispatch), [manual-run instructions](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow)). After team review, integrate this CI change into the default branch. Before dispatching against an existing feature branch, incorporate the reviewed CI change into that actual target ref and inspect the workflow there; integrating main alone does not update #120's branch. Then select that branch for one manual run and inspect the checkout SHA and job results. This local change does not itself close #120's CI gap.
