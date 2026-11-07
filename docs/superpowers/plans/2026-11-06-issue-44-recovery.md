# Issue #44 Local Recovery Rehearsal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and execute a three-trial isolated PostgreSQL restart experiment that measures database query recovery, fresh API login recovery, backend health, and synthetic student data integrity.

**Architecture:** One PowerShell script orchestrates the existing PostgreSQL, Mailpit, and Spring Boot Compose services under a fixed isolated project name. It establishes a verified synthetic student through the real API, records one count/checksum baseline, restarts only PostgreSQL three times, measures database and login recovery from the same stopwatch, and removes only label-verified disposable resources.

**Tech Stack:** PowerShell 7, Docker Compose 5, PostgreSQL 18.6 and `psql`, Spring Boot Actuator/authentication APIs, Mailpit HTTP API

**Spec:** `docs/gabriel/reliability/issue-44-recovery-rehearsal.md`

## Global Constraints

- Work only in the existing checkout and branch `gabriel/issue-44-recovery` from base `e5cc181b3ebcf7b5cb1050547c1d82922068c064`.
- Do not modify React or Spring Boot application source, PRs #104/#107/#113, or teammate branches.
- Never stop, restart, recreate, or attach to `colegio-connect-dev` or its volume.
- Use synthetic in-memory credentials, free loopback ports, a fixed isolated project name, and a new project-scoped PostgreSQL volume.
- Start every stopwatch before the PostgreSQL restart command and run exactly three recorded trials.
- Never replace a failed trial; never claim hosted backup restoration, disaster recovery, or full I16 acceptance.
- Keep the PowerShell code free of explanatory comments and explain sections in Markdown.

## Review Focus

- A stale or colliding isolated project could cause cleanup to delete someone else's resources; the script must refuse any pre-existing matching container or volume and verify Compose labels before deletion.
- A database query answered by the old PostgreSQL process could understate recovery; each trial must require `pg_postmaster_start_time()` to change.
- A reused session could make login appear recovered without fresh authentication; every attempt must create a new cookie session and obtain its own CSRF token.
- Login legitimately changes lockout fields; the checksum must exclude those fields while covering the persistent student and verification records in a fixed order.
- A passing container health state without working authentication could overstate backend recovery; the trial must require login plus `/api/auth/me` for the original student.

---

### Task 1: Recovery script and safety contract

**Files:**
- Create: `scripts/gabriel/issue-44-recovery.ps1`
- Modify: `docs/gabriel/reliability/issue-44-recovery-rehearsal.md`

**Interfaces:**
- Consumes: `compose.yaml`, `.env.example`, registration/authentication endpoints, Mailpit API, PostgreSQL schema, Docker labels.
- Produces: one command that prints a sanitized baseline and exactly three structured trial results, returning nonzero when setup, a trial, integrity, or cleanup fails.

- [x] **Step 1: Run the missing-script test and verify RED**

Run PowerShell syntax parsing and required-function/constant assertions against `scripts/gabriel/issue-44-recovery.ps1`.

Expected: FAIL because the script does not exist.

- [x] **Step 2: Implement the minimal orchestrator**

Use focused functions for Docker invocation, free-port selection, Compose rendering, CSRF session creation, bounded HTTP requests, Mailpit code retrieval, `psql` scalar queries, baseline/count/checksum collection, fresh login verification, one restart trial, and resource identity/cleanup checks. The main flow must run setup, baseline, trials 1-3, sanitized result output, and cleanup in `finally`.

- [x] **Step 3: Run syntax and static safety assertions and verify GREEN**

Require successful PowerShell parsing; fixed isolated project name; explicit three-trial loop; 300-second ceiling; one-second poll interval; `restart postgres`; no unqualified `down`; `down --volumes` only after label checks; no hard-coded student/database passwords, cookies, CSRF tokens, or verification codes.

- [x] **Step 4: Validate the rendered isolated Compose configuration without startup**

Use the script's validation path or equivalent PowerShell assertions to require free loopback ports, backend datasource `postgres:5432`, service health dependencies, and project-scoped `postgres_data`, while comparing the live `colegio-connect-dev` container IDs and volume before/after.

- [x] **Step 5: Commit the implementation checkpoint**

Commit the script plus approved pre-test design/plan as `test(recovery): add isolated restart rehearsal`.

### Task 2: Execute and preserve the three trials

**Files:**
- Modify only for a reproduced script defect: `scripts/gabriel/issue-44-recovery.ps1`

**Interfaces:**
- Consumes: Task 1 script and the existing local Docker engine.
- Produces: one immutable observed baseline and exactly three trial rows, plus sanitized failure details and resource-preservation evidence.

- [x] **Step 1: Snapshot pre-existing Docker resources**

Record the `colegio-connect-dev` project, its four container IDs/states, its PostgreSQL volume identity, and the full pre-existing container/volume sets.

- [x] **Step 2: Execute the script once**

Run the script from the repository root. Do not rerun a failed trial as a replacement. Capture complete sanitized stdout/stderr and exit code in the plan workspace.

- [x] **Step 3: Verify the baseline**

Require successful API registration and verification through Mailpit; fresh login and `/me` for one student UUID; database-backed source path; one stable row count/checksum pair.

- [x] **Step 4: Verify exactly three trial results**

For each trial require database recovery seconds, fresh-login seconds, changed PostgreSQL start time, backend `UP`, matching count/checksum, and explicit PASS/FAIL. Confirm each measurement is below 300 seconds only when the trial says PASS.

- [x] **Step 5: Verify cleanup and resource preservation**

Require zero remaining isolated containers/networks/volumes, unchanged pre-existing resource sets, and the original `colegio-connect-dev` containers still running with its database volume present.

- [x] **Step 6: Fix only reproduced defects with RED-GREEN evidence**

If execution exposes a script defect, preserve the failed output, add the smallest static or integration assertion that reproduces it, correct the script, and rerun the experiment only when the original run did not complete three valid trials. Never replace a completed failed trial.

- [x] **Step 7: Commit any required script correction**

Use a focused `fix(recovery): ...` commit only if Step 6 changed the script.

### Task 3: Actual measurements and Capstone evidence

**Files:**
- Modify: `docs/gabriel/reliability/issue-44-recovery-rehearsal.md`
- Modify: `docs/progress/development-log.md`
- Modify: `docs/testing/test-evidence.md`
- Modify: `docs/reports/midterm/evidence-index.md`
- Modify: `docs/superpowers/plans/2026-11-06-issue-44-recovery.md`

**Interfaces:**
- Consumes: Task 2 sanitized output, tested commit SHA, Docker/PowerShell versions, and source inspection.
- Produces: one authoritative local recovery record with individual trial measurements, commands, environment, behavior explanation, limitations, and evidence links.

- [x] **Step 1: Replace every `Not run` result cell with observed evidence**

Record each database/login duration, PostgreSQL start change, backend health, count/checksum comparison, and verdict. Preserve all failed trials and explain any database-to-login delay using established logs; label unproven causes as hypotheses.

- [x] **Step 2: Explain every important script section**

Document inputs, output, reason, API/CSRF/Mailpit flow, stable checksum fields and exclusions, timer origin, poll interval/timeouts, error behavior, resource ownership, and cleanup.

- [x] **Step 3: Update shared evidence records**

Add a dated Issue #44 development-log entry, a new test-evidence record, and a midterm-index link. State local scope and leave backup restore/full I16/issue closure incomplete.

- [x] **Step 4: Run documentation and secret checks**

Require `git diff --check`, resolving relative links, no published credentials/cookies/codes, consistent trial numbers across records, and no `docs/gabriel/README.md` overlap with PR #107.

- [x] **Step 5: Commit the evidence increment**

Commit as `docs(recovery): record three restart trials`.

### Task 4: Final verification, review, push, and PR

**Files:**
- No new files unless review identifies an in-scope defect.

**Interfaces:**
- Consumes: Tasks 1-3 commits and evidence.
- Produces: one open focused PR referencing Issue #44, with teammate review requested and actual CI status.

- [x] **Step 1: Re-run non-destructive static checks**

Re-run script parsing/safety assertions, Compose rendering, documentation links/secret scan, backend tests as appropriate, and inspect the full diff against `origin/main`.

- [x] **Step 2: Obtain fresh whole-branch review**

Review timer origin, fresh-session login, database-backed probe, count/checksum stability, three-trial immutability, cleanup targeting, evidence accuracy, and application-source non-modification. Fix Critical/Important findings with test-first evidence.

- [x] **Step 3: Push without force and open one PR**

Push `gabriel/issue-44-recovery`, open a focused PR against `main`, reference but do not close Issue #44, and request teammate review. Never merge automatically.

- [ ] **Step 4: Inspect final-head CI and review state**

Record PR number/URL, head SHA, checks and run URLs, requested reviewer, and remaining acceptance work. Keep Issue #44 open.
