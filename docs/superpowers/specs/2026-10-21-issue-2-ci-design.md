# Issue #2 CI and Dependabot Design

## Goal

Add a secure, reliable GitHub Actions quality gate and dependency-update configuration without changing application, database, or frontend-owned code.

## Scope

The CI workflow runs on every branch push and every pull request targeting `main`. It verifies the Java 25 backend with Maven, validates Docker Compose, builds the backend container, and exposes one stable `CI Gate` check for later branch protection. A push to `main` also uploads the built image as a downloadable artifact retained for seven days.

Dependabot covers Maven in `/backend`, GitHub Actions, the backend Dockerfile, and root Docker Compose images. npm checks and npm Dependabot are deferred until Issue #13 supplies a frontend manifest, lockfile, package manager, and scripts. Gradle is not configured because the repository has no Gradle project.

## Workflow design

`.github/workflows/ci.yml` contains three jobs:

1. `backend` installs Eclipse Temurin 25, caches Maven dependencies, and runs `./mvnw --batch-mode --no-transfer-progress verify` from `backend/`. Maven discovery ensures Cristian's current and future backend tests, including Testcontainers tests, run without CI-specific database logic.
2. `container` validates `compose.yaml` with `.env.example`, builds `backend/Dockerfile`, and, only for a push to `main`, saves and uploads the image as a compressed artifact. It does not publish to a registry.
3. `ci-gate` always runs after both jobs and fails unless both succeeded. Branch protection will eventually require only its stable display name, `CI Gate`.

The workflow grants only `contents: read`, uses no secrets, avoids `pull_request_target`, pins external actions to full commit SHAs, cancels superseded runs, and sets job timeouts. Required jobs are not path-filtered.

## Dependency updates

`.github/dependabot.yml` schedules weekly updates with small pull-request limits for:

- Maven at `/backend`.
- GitHub Actions at `/`.
- Docker at `/backend` for the Dockerfile.
- Docker at `/` for Compose images.

Dependabot updates target `main`. npm is intentionally absent until Issue #13; this deferral is documented rather than represented by a fake passing check.

## Documentation

`CONTRIBUTING.md` will describe the checks, the stable gate, the frontend deferral, and the container artifact. It will also record that Issue #5's OpenAPI CI artifact remains outstanding and is not silently claimed by Issue #2.

## Verification

Before pushing, validate the workflow syntax with `actionlint`, validate parsed Dependabot structure, run Maven `verify` under Java 25, validate Compose, build the Docker image, and run `git diff --check`. After opening the PR, inspect every actual GitHub Actions job and its logs. Do not merge or close Issue #2.

## Out of scope

- Frontend tooling or source changes.
- npm Dependabot before Issue #13.
- Database migrations, entities, repositories, or Issue #6 branch changes.
- GHCR, deployment, or hosting decisions from Issue #4.
- Branch-protection configuration.
- Producing the Issue #5 OpenAPI artifact.
