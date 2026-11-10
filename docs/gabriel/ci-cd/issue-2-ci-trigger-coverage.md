# Issue #2 CI trigger coverage

**Status:** CI verified on [PR #107](https://github.com/1uismar33r0/Colegio_Connect/pull/107) after normal merge `351fd89` incorporated current main `e5cc181` (#111/#112). Push and PR runs at that head passed; teammate approval and enforceable required checks remain. **Branch:** `gabriel/issue-2-ci-trigger-coverage`. **Trigger implementation:** `242046d`. Older `a128824` run counts below remain dated history.

## Purpose and proposal relationship

GitHub Actions runs the same repeatable checks on GitHub-hosted machines whenever the configured repository events occur. Colegio Connect needs that shared environment because Luis develops the React frontend, Cristian develops the Spring Boot backend and database behavior, and Gabriel maintains the integration pipeline. A common pipeline catches incompatible changes before integration rather than relying on each developer's workstation.

The approved proposal maps CI to Objective O5 and Indicator I13. CI is supporting engineering evidence, not full I13 acceptance: I13 also requires at least 20 deployed core workflow scenarios, zero unhandled server errors, and all required automated tests passing. A green workflow also does not prove branch protection, security, performance, accessibility, recovery, or complete application functionality.

## Trigger configuration

```yaml
on:
  push: {}
  pull_request: {}
  workflow_dispatch: {}
```

### Push trigger

- **What it does:** starts CI for every pushed branch, including feature branches and `main`.
- **Why it is needed:** developers receive feedback before opening or updating a PR, and a `main` push can package the integration image.
- **How it works:** the empty `push` mapping intentionally has no branch filter. The container image is still saved and uploaded only when the ref is `refs/heads/main`.
- **Team support:** Luis sees frontend failures on his branch; Cristian sees backend, database, and container failures on his branch.
- **How it is tested:** push a named feature-branch commit and confirm all four jobs use that exact SHA.
- **Demonstrated result:** feature-branch push run [42325561434](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/42325561434) passed all four jobs at `678c43e` while PR #107 targeted `main`.

### Pull-request trigger

- **What it does:** starts CI for every pull request, regardless of its base branch.
- **Why it is needed:** normal and stacked PRs both need integration checks; restricting the base to `main` previously omitted stacked work.
- **How it works:** the empty `pull_request` mapping uses GitHub's normal PR activity types without a base-branch filter.
- **Team support:** Luis and Cristian can review a shared green or failing result for the proposed merge, not only the contributor's branch state.
- **How it is tested:** open or synchronize a PR and confirm the workflow checks the PR's current head in the proposed merge context.
- **Demonstrated result:** PR run [42325565946](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/42325565946) passed all four jobs at `678c43e` with PR #107 based on `main`.

### Manual dispatch

- **What it does:** lets a collaborator start CI from the Actions page for a selected branch.
- **Why it is needed:** a deliberate rerun can verify a stable revision when no new push or PR event exists.
- **How it works:** `workflow_dispatch: {}` exposes the standard GitHub manual-run control without custom inputs or elevated permissions.
- **Team support:** any collaborator can request the same full pipeline while investigating an integration result.
- **How it is tested:** select the workflow and branch in GitHub, dispatch it, and verify the recorded event and SHA.
- **Demonstrated result:** GitHub accepts the workflow containing this trigger, but no manual-dispatch run has been recorded. Manual execution is useful operational evidence, not part of Issue #2 AC1's required push/PR event pair.

## Verification jobs

### Backend

The Backend job checks out the revision without persisting credentials, installs Temurin Java 25, caches Maven dependencies, and runs `./mvnw --batch-mode --no-transfer-progress verify`. Maven discovers the current JUnit and Testcontainers tests, so Cristian does not need CI-specific database test code. The job is successful only when compilation, test execution, and Maven verification all succeed. PR #107's November 7 run reported 19 tests at its then-current base; current main has more tests, so that count is historical.

### Frontend

The Frontend job installs Node 24 and the exact locked dependencies with `npm ci`. It runs linting, Prettier checking, TypeScript checking, 22 Vitest tests, generated OpenAPI type drift checking, and the production build. This gives Luis one result covering source quality, types, unit behavior, API snapshot consistency, and buildability. It does not prove that the committed OpenAPI snapshot matches a currently running backend.

### Container

The Container job first parses the Compose configuration using `.env.example`, then builds the backend image. That detects invalid Compose wiring and Dockerfile/build failures for Gabriel and Cristian. Saving and uploading the seven-day image artifact is deliberately limited to a push to `main`; feature and PR runs build the image but skip publication.

### CI Gate and failure reporting

`CI Gate` declares Backend, Frontend, and Container as dependencies and uses `if: always()` so it still runs when an earlier job fails or is cancelled. Its success step requires all three dependency results to equal `success`; otherwise its failure step exits with status 1. GitHub reports the failing job and step directly, while `CI Gate` provides one stable aggregate check that future branch protection can require.

The workflow itself cannot block a merge. The repository owner must configure GitHub rules or branch protection to require `CI Gate`, and the team must demonstrate a safe rejected merge before Issue #2 AC2 can pass.

## Security and concurrency safeguards

- `permissions: contents: read` gives the workflow token read-only repository access.
- Checkout disables persisted credentials, and external actions are pinned to full commit SHAs.
- The workflow does not use `pull_request_target` or deployment secrets.
- Pull requests share a concurrency group by PR number and cancel superseded runs. Pushes use the run ID, so separate pushes do not cancel one another.
- Job timeouts bound stuck executions. Required jobs are not hidden behind path filters.

## Evidence and interpretation

| Evidence | Revision | Actual result | Limitation |
| --- | --- | --- | --- |
| PR #106 run [42276697893](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/42276697893) | `bcea2f4` | Backend: 19 tests; Frontend: 22 tests in 5 files; Container and CI Gate passed | Verifies the four inherited jobs before the trigger commit |
| Main-based feature push [42325561434](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/42325561434) | `678c43e` | Backend, Frontend, Container, and CI Gate passed | Main-only image save/upload correctly skipped on the feature branch |
| Main-based PR [42325565946](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/42325565946) | `678c43e` | Backend: 19 tests; Frontend: 22 tests in 5 files; Container and CI Gate passed | Verifies the proposed merge context against `main`; not branch-protection evidence |
| Feature push [42319769681](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/42319769681) | `da47445` | Backend, Frontend, Container, and CI Gate passed | Predates PR #107's base change to `main` |
| Stacked PR [42319774193](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/42319774193) | `da47445` | Backend: 19 tests; Frontend: 22 tests in 5 files; Container and CI Gate passed | PR base was the PR #106 branch |
| Local structural checks | `2eb4b03` | All 15 trigger/job/gate/permission/concurrency invariants, documentation links, scope, whitespace, and credential-pattern checks passed | Static validation supplements GitHub execution |
| Current audited head push [42326710752](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/42326710752) and PR [42326716528](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/42326716528) | `15d0469` | Backend, Frontend, Container, and CI Gate succeeded in both events | Both runs predate #111/#112 on current main; neither is a manual dispatch |
| Current-main push [42895871782](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/42895871782) and PR [42895882167](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/42895882167) | `351fd89` | Backend, Frontend, Container, and CI Gate succeeded in both events; local Java 25 suite passed 30/30 and frontend passed 22/22 | Main `e5cc181` is an ancestor of this normal merge; no manual dispatch run has been claimed |

At the original November 7 checkpoint, local `actionlint` and Java 25 were unavailable and Docker was not running in that session. Hosted GitHub Actions supplied Java 25 and Docker. Later local environments have Java 25 and Docker; their availability does not retroactively change the recorded CI runs. Windows `core.autocrlf=true` can make the exact Prettier command see CRLF locally; Linux CI is authoritative for the unchanged frontend sources.

## Overlap with older CI PRs

Original [PR #103](https://github.com/1uismar33r0/Colegio_Connect/pull/103) used the superseded #97 frontend base but established dated push/PR trigger evidence in `docs/testing/2026-11-04-ci-trigger-coverage.md`. PR #107 preserves its unrestricted push/PR behavior and adds the same manual dispatch offered by draft [PR #121](https://github.com/1uismar33r0/Colegio_Connect/pull/121). #121 alone is narrower: it does not expand automatic branch/stacked-PR coverage. If the team accepts #107 and its final current-main checks, #103 and #121 need no separate workflow merge; preserve #103's dated test evidence before any team-authorized closure. Neither older PR is closed by this documentation update.

The #103 source record at `0538e54f8ffcc366113b17b4cce5d83fa70d42d3` reports `actionlint` in a disposable Docker run and `docker compose --env-file .env.example config --quiet`, both exit 0 on its November 4 CI-trigger branch. Those results are historical source evidence, not validation of #107's current head. Its unrestricted triggers are present in #107; this paragraph preserves the unique dated check before any #103 closure decision.

Draft #121 at `f7a2bd50a5992e7bebef57c9cf578c938811c759` records `git diff --check`, a `js-yaml` parse, and structural assertions for its manual-only workflow change; it did not record a hosted manual run. Its source notes that GitHub needs `workflow_dispatch` on the default branch and that the selected target ref must itself contain the reviewed workflow before manual verification of that ref. #107 includes the trigger, but a future manual run on another existing branch still needs that branch updated and its checkout SHA checked. This retains #121's useful operating guidance without a second workflow merge.

## Remaining work

- Obtain another team member's formal review of PR #107 after Cristian's documentation feedback; the current-main push and PR CI runs above passed.
- Have the repository owner enforce `CI Gate` and demonstrate that a failing required check blocks merging.
- Resolve the separate backend role and OpenAPI publication contracts before treating frontend authorization or generated-client behavior as integrated.
