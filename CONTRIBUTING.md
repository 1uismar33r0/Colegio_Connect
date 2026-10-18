# Contributing to Colegio Connect

This is a three-person capstone project. Keep changes small enough for another team member to review, and coordinate changes that cross backend, frontend, and infrastructure ownership.

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

CI workflows and required GitHub checks are still to be added. A pull request should state clearly when a check is not yet available.

## Working across areas

Cristian owns backend and database initialization; Luis owns frontend initialization; Gabriel owns the shared environment, security, and CI/CD setup. Agree on shared interfaces and configuration needs in a pull request or issue discussion before depending on them. Do not add application dependencies, database entities, or API conventions in another owner's area without coordinating with that owner.
