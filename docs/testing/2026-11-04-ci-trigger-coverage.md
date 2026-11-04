# CI Trigger Coverage — 2026-11-04

Issue #2 requires CI on every push and pull request. The existing workflow
limited both events to `main`, which left valid stacked pull requests without
official checks.

`ci.yml` now declares unrestricted `push` and `pull_request` events plus
`workflow_dispatch`. This preserves the existing Backend, Frontend, Container,
and aggregate CI Gate jobs while allowing exact stacked heads to be checked
without a temporary integration PR after this change is merged.

Local validation from the CI-trigger branch:

```text
docker run --rm -v "${PWD}:/repo" rhysd/actionlint:latest -color /repo/.github/workflows/ci.yml
ACTIONLINT_EXIT=0

docker compose --env-file .env.example config --quiet
COMPOSE_EXIT=0
```

GitHub Actions evidence is recorded on the pull request for this change. The
repository owner must still configure a `main` ruleset or branch protection
requiring the CI Gate and an approving review before Issue #2 can be closed.
