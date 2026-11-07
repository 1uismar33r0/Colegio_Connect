# Issue #2 CI trigger coverage

**Status:** Draft [PR #107](https://github.com/1uismar33r0/Colegio_Connect/pull/107) targets `main`; post-retarget verification is pending. **Branch:** `gabriel/issue-2-ci-trigger-coverage`. **Trigger implementation:** `242046d`. **Previous verified head:** `da47445`. **Main baseline:** PR #106 merged as `a128824` on 2026-11-07.

## Purpose and proposal relationship

GitHub Actions runs the same repeatable checks on GitHub-hosted machines whenever the configured repository events occur. Colegio Connect needs that shared environment because Luis develops the React frontend, Massini develops the Spring Boot backend and database behavior, and Gabriel maintains the integration pipeline. A common pipeline catches incompatible changes before integration rather than relying on each developer's workstation.

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
- **Team support:** Luis sees frontend failures on his branch; Massini sees backend, database, and container failures on his branch.
- **How it is tested:** push a named feature-branch commit and confirm all four jobs use that exact SHA.
- **Demonstrated result:** run [42319769681](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/42319769681) passed at `da47445` before the PR was retargeted. A new exact-revision run is required after this documentation update.

### Pull-request trigger

- **What it does:** starts CI for every pull request, regardless of its base branch.
- **Why it is needed:** normal and stacked PRs both need integration checks; restricting the base to `main` previously omitted stacked work.
- **How it works:** the empty `pull_request` mapping uses GitHub's normal PR activity types without a base-branch filter.
- **Team support:** Luis and Massini can review a shared green or failing result for the proposed merge, not only the contributor's branch state.
- **How it is tested:** open or synchronize a PR and confirm the workflow checks the PR's current head in the proposed merge context.
- **Demonstrated result:** run [42319774193](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/42319774193) passed at `da47445` while PR #107 was stacked on PR #106. A new run against `main` is required.

### Manual dispatch

- **What it does:** lets a collaborator start CI from the Actions page for a selected branch.
- **Why it is needed:** a deliberate rerun can verify a stable revision when no new push or PR event exists.
- **How it works:** `workflow_dispatch: {}` exposes the standard GitHub manual-run control without custom inputs or elevated permissions.
- **Team support:** any collaborator can request the same full pipeline while investigating an integration result.
- **How it is tested:** select the workflow and branch in GitHub, dispatch it, and verify the recorded event and SHA.
- **Demonstrated result:** GitHub accepts the workflow containing this trigger, but no manual-dispatch run has been recorded. Manual execution is useful operational evidence, not part of Issue #2 AC1's required push/PR event pair.

## Verification jobs

### Backend

The Backend job checks out the revision without persisting credentials, installs Temurin Java 25, caches Maven dependencies, and runs `./mvnw --batch-mode --no-transfer-progress verify`. Maven discovers the current JUnit and Testcontainers tests, so Massini does not need CI-specific database test code. The job is successful only when compilation, test execution, and Maven verification all succeed. The last verified PR run reported 19 tests with no failures, errors, or skips.

### Frontend

The Frontend job installs Node 24 and the exact locked dependencies with `npm ci`. It runs linting, Prettier checking, TypeScript checking, 22 Vitest tests, generated OpenAPI type drift checking, and the production build. This gives Luis one result covering source quality, types, unit behavior, API snapshot consistency, and buildability. It does not prove that the committed OpenAPI snapshot matches a currently running backend.

### Container

The Container job first parses the Compose configuration using `.env.example`, then builds the backend image. That detects invalid Compose wiring and Dockerfile/build failures for Gabriel and Massini. Saving and uploading the seven-day image artifact is deliberately limited to a push to `main`; feature and PR runs build the image but skip publication.

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
| Feature push [42319769681](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/42319769681) | `da47445` | Backend, Frontend, Container, and CI Gate passed | Predates PR #107's base change to `main` |
| Stacked PR [42319774193](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/42319774193) | `da47445` | Backend: 19 tests; Frontend: 22 tests in 5 files; Container and CI Gate passed | PR base was the PR #106 branch |
| Local structural checks | `da47445` | All 15 trigger/job/gate/permission/concurrency invariants, documentation links, scope, whitespace, and credential-pattern checks passed | Static validation does not replace GitHub execution |

Local `actionlint` and Java 25 are unavailable, and the local Docker daemon is not running. Hosted GitHub Actions supplies Java 25 and Docker. Windows `core.autocrlf=true` also makes the exact Prettier command see CRLF locally; Linux CI is the authoritative required-format execution for the unchanged frontend sources.

## Remaining work

- Run push and PR CI on the new documentation head while PR #107 targets `main`, then record the exact SHA and run URLs.
- Obtain another team member's review of PR #107.
- Have the repository owner enforce `CI Gate` and demonstrate that a failing required check blocks merging.
- Resolve the separate backend role and OpenAPI publication contracts before treating frontend authorization or generated-client behavior as integrated.
