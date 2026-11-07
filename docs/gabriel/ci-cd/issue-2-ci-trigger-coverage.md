# Issue #2 CI trigger coverage

**Status:** Implemented on branch, verification in progress. **Branch:** `gabriel/issue-2-ci-trigger-coverage`. **Implementation commit:** `242046d`. **Dependency:** open [PR #106](https://github.com/1uismar33r0/Colegio_Connect/pull/106) at `bcea2f4`.

## What GitHub Actions does

GitHub Actions runs repeatable checks on GitHub-hosted machines. The Colegio Connect `CI` workflow uses four jobs:

1. **Backend** installs Java 25 and runs Maven verification, including JUnit and Testcontainers tests discovered by the project.
2. **Container** validates `compose.yaml`, builds the backend image, and uploads a seven-day image artifact only for a push to `main`.
3. **Frontend** installs the locked npm dependencies from PR #106, then runs linting, formatting, TypeScript checks, unit tests, OpenAPI generated-type drift checking, and the production build.
4. **CI Gate** waits for all three jobs and fails if any required job fails, is cancelled, or does not succeed.

The workflow grants only `contents: read`, disables persisted checkout credentials, pins third-party actions to full commit SHAs, and keeps the existing pull-request concurrency safeguard. These controls reduce credential exposure and make `CI Gate` a stable future branch-protection check.

## Why the project needs CI

CI gives Luis and Massini the same automated feedback before changes are integrated. Luis's React changes are checked with the frontend toolchain, while Massini's Spring Boot and database changes are checked with Java 25, Maven, PostgreSQL Testcontainers, Compose validation, and the container build. This catches integration failures earlier than a manual end-of-sprint build.

The approved proposal maps CI to Objective O5 and Indicator I13. This workflow is supporting evidence, but it does **not** satisfy I13 by itself: I13 requires at least 20 deployed core scenarios, zero unhandled server errors, and all required automated tests passing.

## What was missing

PR #106 added the complete Frontend job and connected it to `CI Gate`, but its workflow still ran only for pushes to `main` and pull requests targeting `main`. That omitted:

- pushes to feature branches;
- pull requests targeting non-`main` branches, including stacked PRs;
- intentional manual runs through `workflow_dispatch`.

Gabriel's earlier [PR #103](https://github.com/1uismar33r0/Colegio_Connect/pull/103) proved those triggers on the older PR #97 stack. It must not be merged directly because its base contains an obsolete frontend foundation. This branch transfers only its three trigger declarations onto Luis's verified PR #106 head.

## How the new triggers work

```yaml
on:
  push: {}
  pull_request: {}
  workflow_dispatch: {}
```

- `push: {}` runs CI for every pushed branch.
- `pull_request: {}` runs CI for every pull request regardless of its base branch, so stacked changes receive checks.
- `workflow_dispatch: {}` lets a collaborator start a deliberate verification run from GitHub.

No backend or frontend application source changed. The Backend, Container, Frontend, and CI Gate jobs, permissions, artifact rules, action pins, and concurrency expressions are inherited unchanged from PR #106.

## Verification evidence

| Evidence | Revision | Actual result | Limitation |
| --- | --- | --- | --- |
| PR #106 CI run [42276697893](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/42276697893) | `bcea2f4` | Backend: 19 tests; Frontend: 22 tests in 5 files; Container and CI Gate passed | Verifies the inherited jobs before this trigger-only commit |
| Historical #103 push run [41928707063](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/41928707063) | `0538e54` | Backend, Frontend, Container, and CI Gate passed on a feature-branch push | Older frontend stack; trigger precedent only |
| Historical #103 PR run [41928749008](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/41928749008) | `0538e54` | The same four jobs passed on a stacked pull request | Older frontend stack; trigger precedent only |
| Local trigger invariant check | `242046d` | Required trigger, job, gate, permission, and concurrency lines present | Static validation is not hosted execution |

The exact `242046d` revision still needs both a push-triggered run and a pull-request-triggered run. `actionlint` was not installed in the local environment; GitHub workflow parsing and hosted jobs remain required evidence.

## Remaining blockers

- PR #106 must merge before this branch can be retargeted safely to `main`; until then this is a dependent stack.
- A teammate must review this change, and its exact push and PR runs must pass.
- The private repository does not currently provide verified branch-protection enforcement. The repository owner must resolve the plan/visibility limitation and require `CI Gate` before Issue #2 can meet its second acceptance criterion.
- The OpenAPI check proves generated types match a committed snapshot, not that the snapshot matches the current running backend. Backend OpenAPI export remains a separate integration requirement.

