# Issue #44 local recovery rehearsal

## Status

**Design approved; implementation and measurements not yet run.** This record must not claim a result until the script has executed all three trials.

## Objective

Answer one local reliability question: after the isolated PostgreSQL service restarts, how long does it take before PostgreSQL answers a query and a previously registered synthetic student can sign in again through the existing backend? The same student and verification records must survive all three restarts.

The approved proposal maps this work to Objective O5 and indicator I16: after a restart, the service must be healthy within five minutes with no data loss. Issue #44 requires a restart procedure, persistence evidence, a recovery measurement, review, and CI. This rehearsal tests local persistent-volume restart behavior only. It does not prove hosted backup restoration, disaster recovery, or final production acceptance.

## Why it matters

Data persistence means records remain stored after a process or container stops. Docker stores PostgreSQL's database files in a named volume outside the container's temporary writable layer. Restarting PostgreSQL replaces its running process while reconnecting it to the same files.

Database readiness alone is insufficient. Massini's backend must reconnect and authenticate the existing student through PostgreSQL. Luis's frontend depends on that backend login behavior, so a successful fresh login is a teammate-facing recovery check rather than only a database port check.

## Approved approach

Use one readable PowerShell script at `scripts/gabriel/issue-44-recovery.ps1`. It orchestrates the existing `postgres`, `mailpit`, and `backend` services with Docker Compose. It adds no application feature, framework, dependency, or test service.

### Approaches considered

1. A host PowerShell orchestrator using existing Compose, PostgreSQL `psql`, Mailpit HTTP, and backend HTTP. Selected because the development environment is Windows-based, every required tool already exists, and it is the smallest reproducible solution.
2. A separate test-runner container. Rejected because it adds another image and more networking/configuration than this local rehearsal needs.
3. A GitHub Actions-only test. Rejected because Issue #44 asks for a local controlled restart and the canonical development project must remain observable and untouched.

## Baseline and isolation

- Git base: verified `main` revision `e5cc181b3ebcf7b5cb1050547c1d82922068c064`.
- Branch: `gabriel/issue-44-recovery`.
- Compose project: a fixed Issue #44 test name that cannot match `colegio-connect-dev`.
- Services started: `postgres`, `mailpit`, and `backend` only.
- Networking: free loopback host ports selected at runtime.
- Credentials: synthetic database and student credentials generated in memory and never printed or written to evidence.
- Storage: a newly created project-scoped `postgres_data` volume, distinct from every pre-existing volume.

Before startup, the script renders the Compose JSON and verifies the selected ports, datasource service name, required services, health dependencies, and resolved project volume. It refuses to run if containers or a volume already use the isolated project identity.

The script snapshots all pre-existing container IDs and volume names. Cleanup targets only resources carrying the isolated Compose project name and labels. It then proves that every pre-existing container and volume, including `colegio-connect-dev_postgres_data`, remains present.

## Existing API flow used for the baseline

The script follows the backend's real browser security flow:

1. Start a new cookie session.
2. `GET /api/auth/csrf` and read the `XSRF-TOKEN` cookie.
3. Echo that token in the `X-XSRF-TOKEN` header for each POST request.
4. Register a unique synthetic `@upr.edu` student through `POST /api/auth/register`.
5. Poll the isolated Mailpit API, select the message addressed to that synthetic student, and extract its six-digit verification code without printing it.
6. Verify the student through `POST /api/auth/verify`.
7. Start a fresh cookie session, obtain a new CSRF token, and sign in through `POST /api/auth/login`.
8. Call `GET /api/auth/me` with the new authenticated session and require the expected student ID, email, and account type.

Fresh authentication reads PostgreSQL. [`LoginService`](../../../backend/src/main/java/com/colegioconnect/accounts/service/LoginService.java) calls `UserRepository.findByEmail()`, checks the stored password hash and account state, and reads role assignments. [`AuthController`](../../../backend/src/main/java/com/colegioconnect/accounts/controller/AuthController.java) returns the login result and its `/me` endpoint calls `UserRepository.findById()`. No authentication cache is present in this path.

## Data integrity baseline

After registration, verification, and the first successful login, `psql` selects the synthetic user's `users` row and related `email_codes` rows. It records:

- Total selected row count.
- Deterministic MD5 checksum of normalized stable fields.
- Synthetic user UUID used to verify every later login response.

Stable user fields are ordered as ID, email, password hash, account type, verified flag, and status. Stable email-code fields are ordered by code ID and include user ID, code hash, creation/expiration/use timestamps, and attempt count. UTC timestamps use a fixed textual format before hashing.

`failed_login_attempts` and `locked_until` are excluded because successful login legitimately resets those authentication-control fields. Excluding them prevents an expected login side effect from being mistaken for lost persistent data. The baseline is captured once and reused unchanged for all three trials.

## Three-trial procedure

For each trial, the script:

1. Reads PostgreSQL's current `pg_postmaster_start_time()`.
2. Starts one stopwatch immediately before issuing `docker compose restart postgres` for the isolated project.
3. Leaves Spring Boot running.
4. Polls at a documented one-second interval with bounded HTTP request timeouts.
5. Records the first time PostgreSQL answers a query and requires a changed server start time.
6. Repeatedly starts a fresh cookie session, gets its own CSRF token, and attempts login until the response identifies the baseline student.
7. Requires backend health HTTP 200 with status `UP`.
8. Recomputes the selected row count and checksum and compares both with the single original baseline.
9. Records errors and timeouts without replacing a failed trial.

The stopwatch is shared by database and login measurements. Database recovery time and fresh-login recovery time therefore use the same restart origin.

## Pass and fail criteria

Each trial passes only when all conditions are true:

- PostgreSQL answers a query in less than 300 seconds.
- `pg_postmaster_start_time()` differs from its pre-restart value.
- A fresh API login identifies the original student in less than 300 seconds.
- Backend health returns HTTP 200 with `UP`.
- Selected record count equals the original baseline.
- Deterministic checksum equals the original baseline.

Any timeout, unchanged PostgreSQL start time, authentication failure, unhealthy backend, count mismatch, or checksum mismatch makes that trial fail. A failed trial remains in the evidence table and is never replaced with a favorable rerun.

## Planned result table

| Trial | Database recovery seconds | Fresh-login seconds | PostgreSQL start changed | Backend health | Count/checksum match | Verdict |
| --- | ---: | ---: | --- | --- | --- | --- |
| 1 | Not run | Not run | Not run | Not run | Not run | Not run |
| 2 | Not run | Not run | Not run | Not run | Not run | Not run |
| 3 | Not run | Not run | Not run | Not run | Not run | Not run |

## Safety precautions

- Never run an unqualified Compose restart, down, or volume command.
- Start and restart services only with the fixed isolated project name.
- Restart only the isolated PostgreSQL service during trials.
- Never stop, restart, recreate, or attach to `colegio-connect-dev`.
- Verify Compose labels before cleanup.
- Remove only the newly created isolated project resources and volume.
- Never publish database credentials, student password, cookies, CSRF tokens, or verification codes.
- Keep PRs #104, #107, and #113 and their branches unchanged.

## Limitations

This experiment uses one synthetic verified student and a local Docker volume. It tests process/container restart and backend reconnection, not host loss, storage corruption, point-in-time recovery, hosted snapshots, or restore into an empty volume. Backup restoration remains a separate Issue #44 follow-up, so this rehearsal alone cannot close the issue or establish final I16 acceptance.
