# Bugs, tooling incidents and verification gaps

**Last updated / reconstruction date: 2026-10-26.** Source baseline and live observation scope: [project status](../project/status.md). Historical local transcripts are secondary evidence; the public-to-collaborators PR/test links below are the durable references. Raw session records, credentials and sensitive diagnostics are intentionally excluded.

Incident status describes a problem, not a feature's engineering acceptance. An observed workaround does not establish that the underlying tool was fixed.

## A. Developer tooling and environment incidents

### TOOL-01 — Docker Desktop startup failure

- **Observed:** 2026-10-21 during #3; reconstructed 2026-10-26. Component: developer workstation, before the application stack started.
- **Symptoms / reproduction:** Docker CLI installed but Linux engine unavailable; historical session reported a `dockerInference` socket/startup failure after launching Docker Desktop from the Codex context. Exact original diagnostic bundle was not preserved in the repository.
- **Cause:** launch-context/AppData virtualization was the historical diagnosis. A controlled causal reproduction is not available; treat that explanation as probable, not a proved Colegio Connect defect.
- **Troubleshooting / workaround:** historical record shows a recommendation to start Docker Desktop manually from the Windows Start menu, outside Codex. A later read-only engine check reported Engine 29.5.2/Linux containers, followed by real stack verification. The record does not establish a permanent Docker fix.
- **Verification:** [PR #87](https://github.com/1uismar33r0/Colegio_Connect/pull/87) records successful build/startup, PostgreSQL connection, Flyway initialization with zero migrations, persistence and health; Cristian independently [confirmed all three services healthy](https://github.com/1uismar33r0/Colegio_Connect/pull/87#issuecomment-6322289283). See [TE-04](../testing/test-evidence.md#te-04).
- **Status:** historical runtime blocker overcome; root-cause fix unverified. If it recurs, check engine availability before debugging the application; do not delete Docker data or terminate unrelated processes.

### TOOL-02 — GitHub authentication differs by execution context

- **Observed:** local CI work blocked October 23; audit/export fallback October 25; verified workaround October 26.
- **Symptoms / reproduction:** `gh auth status` inside the sandbox reports an invalid token, while the same native command in the authenticated Windows user context succeeds. Earlier connector/CLI access could not inspect the repository.
- **Cause:** execution-context credential/access difference is directly observed on October 26; exact earlier token/keyring failure cause is unknown. Do not infer that the user's account is unauthenticated from the sandbox result alone.
- **Troubleshooting:** Gabriel completed native `gh auth login`; saved exports were used when live access failed. For A1, one native authenticated status check outside the sandbox succeeded as `DCGabriel10`, followed by actual repository reads.
- **Verification:** live Issues/PRs/branches/Actions queries and the [posted/verified #88 reply](https://github.com/1uismar33r0/Colegio_Connect/pull/88#issuecomment-6408234838). No tokens or one-time authentication codes are retained here.
- **Status:** working native-context workaround; sandbox authentication limitation remains. Do not repeatedly retry login or expose credentials.

### TOOL-03 — Windows Superpowers helper hang

- **Observed:** October 21; recovery inspection October 23; reconstructed October 26. Related #2.
- **Symptoms / reproduction:** historical execution reports Windows linked-worktree path resolution failure in shell helpers and a blocked command runner. The preserved local CI ledger states Git Bash/WSL helpers could not resolve the Windows linked-worktree gitdir. Do not rerun the failing helper to reproduce it.
- **Cause:** Windows path handling is documented by the contemporary ledger; no upstream fix or isolated regression test is available.
- **Troubleshooting / workaround:** recover from existing commits/ledger; use bounded native PowerShell and Git commands. The CI branch subsequently reached `5060c15`, then `58089c4`, and hosted checks passed. No broad process-kill procedure is endorsed.
- **Verification:** source inspection and [PR #89](https://github.com/1uismar33r0/Colegio_Connect/pull/89); original [CI plan](https://github.com/1uismar33r0/Colegio_Connect/blob/58089c4d4fdcef14d827dc64727ba02afcb76a22/docs/superpowers/plans/2026-10-21-issue-2-ci.md) remains intact. A1 uses native commands successfully.
- **Status:** avoided through a working execution path; helper itself not fixed. Permanent safeguard in [AGENTS.md](../../AGENTS.md).

### TOOL-04 — Local Java 17 versus project Java 25

- **Observed:** October 21 and October 23 historical checks; reconstructed October 26. Related #3/#5/#2.
- **Symptoms / reproduction:** the host selected Java 17 while `backend/pom.xml` requires Java 25; direct host verification could not establish a Java 25 build.
- **Cause:** workstation toolchain mismatch. Cristian's [`f38bea6`](https://github.com/1uismar33r0/Colegio_Connect/commit/f38bea6) changes the project setting **21→25**, not the installed host JDK.
- **Workaround:** historical tests used a temporary Java 25 runtime / Temurin 25 container; the committed Dockerfile and CI select Java 25 explicitly.
- **Verification:** [PR #86 review](https://github.com/1uismar33r0/Colegio_Connect/pull/86), PR #87's Java 25 local test report, and [TE-03 hosted CI](../testing/test-evidence.md#te-03). Historical Surefire output does not encode the current checkout SHA.
- **Status:** Java 25 build path verified; host-wide Java selection not claimed repaired. Check `java -version` in the shell that will run Maven.

### TOOL-05 — CI validator availability and deferred execution

- **Observed:** October 21 actionlint-container pull stalled; October 23 report lacked installed actionlint/yq and a usable Docker daemon. Related #2.
- **Cause:** image/network/engine contribution not conclusively isolated; subsequent helper failure complicated the session.
- **Troubleshooting / workaround:** native configuration inspection continued; final implementation was verified through actual GitHub Actions instead of treating local static inspection as executable validation.
- **Verification:** [TE-03](../testing/test-evidence.md#te-03) confirms Maven, Compose configuration and Docker build jobs. The workflow executed successfully; this is not evidence that actionlint or yq were run.
- **Status:** hosted build/test path works; dedicated validator execution remains unverified. Do not rewrite historical plan checkboxes as passed commands.

### TOOL-06 — Occupied local development ports

- **Observed:** October 21, [PR #87](https://github.com/1uismar33r0/Colegio_Connect/pull/87).
- **Symptoms / cause:** ports 5432 and 8080 were occupied on the validation workstation; owning processes were not preserved as project evidence.
- **Workaround / reproduction:** use documented `POSTGRES_PORT=5433` and `BACKEND_PORT=18080` for that workstation; leave defaults for machines where available. No unrelated service needed to be stopped.
- **Verification:** historical stack health, connection and persistence checks passed with overrides; [TE-04](../testing/test-evidence.md#te-04).
- **Status:** local conflict worked around; no application defect established.

## B. Confirmed application defects

No unresolved product defect with reproducible failing application evidence was established by the October 26 A1 audit. The later Render incident below is a separate observation.

### APP-01 — Render startup rejects a portless PostgreSQL URL

- **Observed:** 2026-11-10 local / 2026-11-11 UTC, Issue #4, deployed branch `gabriel/issue-4-render-staging` at `0f5dace8`. Render service `colegio-connect` repeatedly failed before Java startup with `Private PostgreSQL URL has no host and port.` The Colegio Free PostgreSQL instance was available; no secret value was inspected or recorded.
- **Reproduction and cause:** The old `infra/render/entrypoint.sh` required `*:*` in the URL authority. A network-free local image run with a synthetic `postgresql://user:encoded-password@db.internal/database` URL without `:port` failed with the identical message. A PostgreSQL URI may omit the default 5432 port; the unconditional explicit-port check was the confirmed parser bug. The live secret's exact shape could not be read through the connected Render service metadata, so an additional environment mistake cannot be ruled out until the new deploy starts.
- **Fix and verification:** `infra/render/parse-database-url.sh` validates scheme, host, port and matching database name, defaults an omitted port to 5432, accepts `postgres://` or `postgresql://`, and derives JDBC host/port without logging or decoding URL credentials. `infra/render/test-database-url.mjs` passed 13 synthetic valid/invalid cases, including encoded special characters; output contained no credentials. A capped local app startup against synthetic portless PostgreSQL was tested separately; see [TE-16](../testing/test-evidence.md#te-16).
- **Status:** local parser fix verified; actual Render restart/deploy and hosted readiness require separate observation. No Render environment variable change is indicated by this reproduction if `DATABASE_URL` is the full private URI and `DB_NAME`, `DB_USER`, `DB_PASSWORD` refer to that same database.

When a real defect is found, record: observation/discovery date, revision/environment, issue/component, safe reproduction, expected/actual result, confirmed or unknown cause, attempted fix, regression test result, status and PR/test links. Never turn an unimplemented requirement or a tool failure into a claimed product bug.

## C. Verification and integration gaps

| ID / observed date | Gap and reproduction/source | Cause or dependency | Next verification / owner | Status |
| --- | --- | --- | --- | --- |
| GAP-01 / 2026-10-26 | #88 has no hosted checks; baseline context test excludes datasource/Flyway. Read test annotations and PR checks. | CI and database implementations are on separate open PRs. | Merge reviewed #89, synchronize #88, run its PostgreSQL 18.6 empty-database migration test; Cristian + Gabriel. | Blocked on CI integration; not a failed migration result |
| GAP-02 / 2026-10-26 | Branch endpoint `protected: false`; rulesets HTTP 403 and protection HTTP 404. | Owner/plan capabilities need verification; Gabriel has push but no admin permission. | Luis establishes effective review/check rules and demonstrates rejected merge when requirements fail. | Enforcement not established |
| GAP-03 / 2026-10-26 | #9 naming differs from `Role.java` and V1 SQL in #88. | Contract/enum agreement pending. | Cristian/Gabriel reconcile roles and Luis consumes consistent API identifiers; test every role/status/ownership boundary. | Integration risk; no runtime failure claimed |
| GAP-04 / 2026-10-26 | No hosted app, load test, security scan, accessibility run, usability sessions or full recovery measurement supplied. | Unimplemented flows, unapproved host and missing test work. | Follow [indicator evidence plan](../testing/test-evidence.md) and [midterm index](../reports/midterm/evidence-index.md). | Not run; health checks cannot fill these gaps |
| GAP-05 / 2026-10-26 | #88 review checkbox is checked, but live formal reviews are empty. #85/#87 also have no formal review object. | PR-body assertions and GitHub records differ; comments may contain partial review evidence. | Team records/links actual review and resolves outstanding acceptance; do not infer approval from merge. | Review evidence incomplete |
