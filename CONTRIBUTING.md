# Contributing to Colegio Connect

This is a three-person capstone project. Keep changes small enough for another team member to review, and coordinate changes that cross backend, frontend, and infrastructure ownership.

Follow the shared [development and documentation policy](AGENTS.md): inspect, align with the approved proposal, check issues/dependencies, plan, implement, test, document and review. Update only the records affected by the change. See [project status](docs/project/status.md) for dated implementation and CI integration evidence; the policy defines how to preserve acceptance, progress, incident and test records without redundant reports.

## Branches and pull requests

1. Start a short-lived branch from the latest `main`. Use a descriptive name such as `chore/issue-1-repository-foundation`.
2. Keep each pull request focused on one issue or a closely related set of changes. Link the issue in the description.
3. Describe what changed, how you verified it, and any remaining work or decisions. Use the pull request template.
4. Request review from at least one other team member. Address review feedback before merging.
5. Do not push directly to `main`. Once branch protection is configured, GitHub will enforce review and passing required checks. Until then, follow this rule as a team convention.

## Definition of Done

- Another team member has reviewed the change.
- Relevant tests or checks pass locally and, once available, in CI.
- Documentation is updated when setup, behavior, or configuration changes.
- OpenAPI documentation is updated when an API contract changes.
- No secrets or populated local environment files are committed.

## Continuous integration

Every pull request targeting `main` runs the backend Maven verification with Java 25, validates the Docker Compose configuration, and builds the backend Docker image before merge. The workflow runs again after integration on pushes to `main`; ordinary feature-branch pushes do not create a duplicate run alongside the pull request check. When a pull request receives a newer commit, its obsolete in-progress run is canceled, while `main` runs are not canceled. Maven's standard `verify` lifecycle discovers the existing backend tests and any compatible tests added later.

Open the pull request's **Checks** tab or the repository's **Actions** tab to see each run and its job logs. A failed check means a build, test, Compose validation, or image build needs investigation; open the failed job to find the failing step before updating the branch.

The stable `CI Gate` check succeeds only when both the backend and container jobs succeed. A push to `main` also stores the built Docker image TAR as a downloadable GitHub Actions artifact for seven days using the upload action's default compression; CI does not publish images to a registry.

Frontend checks and npm Dependabot updates will be added after Issue #13 establishes the frontend package manager, lockfile, and scripts. The OpenAPI artifact requested by Issue #5 also remains separate work. Branch protection is intentionally not configured yet: the team will select `CI Gate` as the required check after this workflow has been reviewed and verified on GitHub.

## Working across areas

Cristian owns backend and database initialization; Luis owns frontend initialization; Gabriel owns the shared environment, security, and CI/CD setup. Agree on shared interfaces and configuration needs in a pull request or issue discussion before depending on them. Do not add application dependencies, database entities, or API conventions in another owner's area without coordinating with that owner.
