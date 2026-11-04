# Stacked Authentication CI Validation — 2026-11-04

## Purpose

The CI workflow runs pull requests targeting `main` only. PRs #96, #98, #99,
and #100 are intentionally stacked on feature branches and therefore receive
no workflow run of their own. This temporary validation branch preserves the
actual PR heads in merge commits and opens a non-mergeable integration PR to
`main` so the existing official CI gate can test the combined revision.

## Combined revision

Local validation was executed at `11472080ad4fa5776b32d145d96a2c5a41ebc13d`.
The following exact remote heads are ancestors of that revision:

- PR #95 login: `081722692cbafe357ad82b98c8d767725627f046`
- PR #96 browser authentication tests: `5b23b3c2667abfe012d2421ecbc724bf3c4229da`
- PR #98 authorization foundation: `a4d7afd741a4ae6024241c3e7caad37b8da21c96`
- PR #99 audit foundation: `2e57200f37a461f9f74fcf79b26c56fab1060770`
- PR #100 negative authorization harness: `52352763d0f01e1b8bb9f87e1ec843bb357c2c6b`

The validation branch contains merge commits only. It does not rewrite or add
application changes to any contributor branch.

## Local verification

Environment:

- Java `25.0.4.1`
- Maven Wrapper `3.10.0`
- PostgreSQL `18.6` through Testcontainers
- Docker Desktop `4.75.0`

Command executed from `backend`:

```powershell
mvn clean verify
```

Result:

```text
SUREFIRE tests=47 failures=0 errors=0 skipped=0
MVN_EXIT=0
```

An initial attempt could not start Testcontainers because the local Docker
Desktop engine was stopped. No application code was changed; Docker Desktop
was launched, and the clean rerun above passed.

## Official CI follow-up

The temporary integration PR targets `main` solely to invoke the existing CI
workflow. It must not be merged. Its GitHub Actions result is the official CI
evidence for this stacked set; normal review and the dependency-aware merge
order remain unchanged.

## CI failure and fixture-isolation correction

The first official run, [41927324073](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/41927324073),
passed Container but failed Backend. It exposed a real test-isolation bug that
the local class order had masked: `NegativeAuthorizationHarnessIntegrationTest`
created a role assignment, then `AuthIntegrationTest.cleanDatabase` attempted
to delete its user without first deleting role assignments.

The correction is PR #102 head `695fe3abbc5951777a09d34ca4727ffebb296b8b`:
test-only cleanup now deletes role assignments before users. The updated
validation merge head `52a790a48313d8d7d068b6d4d1aa5cb3e01ac867` includes
that exact commit plus all five original feature PR heads. A clean local
rerun at that head produced:

```text
SUREFIRE tests=47 failures=0 errors=0 skipped=0
MVN_EXIT=0
```

The updated validation branch is pushed to request a fresh official CI run.
