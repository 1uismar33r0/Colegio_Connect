# Issue #44 local recovery rehearsal

## Status

**Two local rehearsals passed on 2026-11-07.** The earlier three-trial run and a separate three-trial verification of the finalized PR #114 script both met the local five-minute target, authenticated their respective synthetic student, and matched each run's original database count and checksum. Hosted backup restoration, disaster recovery, teammate review, and final I16 acceptance remain incomplete.

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

The script follows the backend's CSRF and cookie-based authentication flow, with the local Secure-cookie probe limitation documented below:

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

## Test environment

- Repository baseline: verified `main` revision `e5cc181b3ebcf7b5cb1050547c1d82922068c064`.
- Earlier measured script revision: `1f2d5b1` on `gabriel/issue-44-recovery`.
- Finalized-script verification revision: `c259a70`; the script's last functional change was failure-path hardening in `fcc633f`.
- Host orchestration: PowerShell 7.6.9 on Windows.
- Containers: Docker Desktop engine and client 29.5.2; Docker Compose 5.1.3.
- Images and services: PostgreSQL 18.6, Mailpit 1.31.5, and the existing Spring Boot backend image built from the tested branch.
- Isolated Compose project: `colegio-connect-issue-44-recovery`.
- Poll interval: 1,000 milliseconds.
- Per-trial ceiling: 300 seconds from immediately before the restart command.

The script selected unused loopback ports at runtime, generated credentials in memory, and created a new `colegio-connect-issue-44-recovery_postgres_data` volume. It did not publish the synthetic email, password, verification code, cookies, or CSRF token.

## Commands used

From the repository root:

```powershell
git rev-parse HEAD
& scripts/gabriel/issue-44-recovery.ps1 -ValidateOnly
& scripts/gabriel/issue-44-recovery.ps1
docker ps -a --filter 'label=com.docker.compose.project=colegio-connect-issue-44-recovery'
docker volume ls --filter 'label=com.docker.compose.project=colegio-connect-issue-44-recovery'
docker network ls --filter 'label=com.docker.compose.project=colegio-connect-issue-44-recovery'
docker ps --filter 'label=com.docker.compose.project=colegio-connect-dev'
docker volume inspect colegio-connect-dev_postgres_data
```

Validation mode rendered and inspected the resolved Compose configuration without starting services. The normal command performed setup, baseline creation, exactly three trials, sanitized JSON output, cleanup, and preservation checks.

## Script walkthrough

### Constants and generated inputs

The first section fixes the isolated project name, three-trial count, 300-second target, one-second poll interval, allowed service names, and expected resource names. It generates a database password, student password, synthetic UPRM email, and four free loopback ports at runtime. Existing process environment values are restored in `finally`.

### Docker and Compose boundary

`Invoke-Docker` and `Invoke-Compose` execute commands through a bounded native process, capture exit codes, terminate a command that exceeds its declared deadline, and fail closed unless a probe explicitly allows failure. The script pins the repository's `compose.yaml` and project directory so ambient `COMPOSE_FILE` overrides cannot redirect the rehearsal. Collision checks refuse any pre-existing container, network, or volume with the rehearsal identity. Configuration validation requires PostgreSQL, Mailpit, and backend services; loopback-only published ports; `postgres:5432` as the backend datasource; healthy backend dependencies; the non-external project-scoped volume mounted at `/var/lib/postgresql`; and the non-external project-scoped default network.

### Registration, verification, and session flow

The HTTP helpers create a new cookie session, obtain a CSRF token, and echo it for POST requests. Mailpit polling selects only the message addressed to the synthetic account and extracts the code without printing it. The script registers and verifies the account, then performs a fresh login and `/api/auth/me` request for the expected ID, email, and account type.

The backend sets `JSESSIONID` as `Secure`. PowerShell does not automatically send that cookie to a plain-HTTP loopback URL, so the probe explicitly replays the newly issued session cookie only for `/api/auth/me`. This preserves a fresh backend-created session without changing Spring Boot configuration or application source.

This manual forwarding verifies backend authentication, session creation, database lookup, and the authenticated `/api/auth/me` response. It does **not** verify how a real browser accepts or sends a Secure cookie over local HTTP. Browser cookie behavior requires a separate browser-based HTTPS or appropriately configured local-environment test.

### Database signature

`Invoke-PsqlScalar` runs bounded, noninteractive queries inside only the isolated PostgreSQL service with connection, statement, and Docker-process deadlines. `Get-DataSignature` combines the synthetic user's stable user fields and related email-code fields in a fixed order, then returns a row count and MD5 integrity signature. The MD5 value is an equality checksum, not a password-security mechanism. `failed_login_attempts` and `locked_until` remain excluded because successful authentication may reset them legitimately.

### Trial timing and verdict

`Invoke-RecoveryTrial` reads the old PostgreSQL start time, starts one stopwatch immediately before `docker compose restart postgres`, and never restarts the backend. It polls both database and authentication behavior from that same origin. Database recovery requires a successful query from a different PostgreSQL start time. Each login attempt creates a new cookie session and CSRF token. After recovery it checks actuator health and compares the count and checksum with the original baseline. Any missing condition makes that recorded trial fail. The three-trial coordinator catches unexpected per-trial failures, immediately emits each sanitized trial result, and continues until exactly three results exist; a later failure therefore cannot erase an earlier result.

### Cleanup and preservation

Cleanup eligibility is established before Compose startup, so a partial `up` failure still enters cleanup. Cleanup inspects Compose project and service labels before issuing the project-qualified `down --volumes --remove-orphans`. It then requires the isolated containers, network, and volume to be absent and every pre-existing container ID and volume name to remain. It separately requires the original `colegio-connect-dev` project to remain running and its PostgreSQL volume to remain present.

## Actual results

### Earlier run — script revision `1f2d5b1`

The baseline contained two selected records: one verified student row and one used verification-code row. Its deterministic checksum was `eae1891050f25773acc0cb30cb5bddae`. The same baseline was reused for every trial.

| Trial | Database recovery seconds | Fresh-login seconds | PostgreSQL start changed | Backend health | Count/checksum match | Verdict |
| --- | ---: | ---: | --- | --- | --- | --- |
| 1 | 1.013 | 1.094 | Yes | UP | 2 records; checksum matched | PASS |
| 2 | 0.954 | 1.029 | Yes | UP | 2 records; checksum matched | PASS |
| 3 | 0.981 | 1.057 | Yes | UP | 2 records; checksum matched | PASS |

Every database and login measurement was below 300 seconds. Fresh sign-in followed database readiness by 0.081, 0.075, and 0.076 seconds respectively. Those small gaps are observed request-processing differences; the experiment does not isolate their internal causes.

Post-run inspection found no container, network, or volume labeled for the isolated project. The original four `colegio-connect-dev` containers retained their pre-run IDs and remained running, and `colegio-connect-dev_postgres_data` retained its Compose project label.

### Finalized-script verification — revision `c259a70`

This second run used new synthetic credentials, a new synthetic student, new loopback ports, and a newly created disposable PostgreSQL volume. The baseline again contained one verified student row and one used verification-code row. Its distinct deterministic checksum was `237fe030f4f5596c4b9e1c573042f188`.

| Trial | Database recovery seconds | Fresh-login seconds | PostgreSQL start changed | Backend health | Count/checksum match | Verdict |
| --- | ---: | ---: | --- | --- | --- | --- |
| 1 | 1.025 | 1.105 | Yes | UP | 2 records; checksum matched | PASS |
| 2 | 0.981 | 1.054 | Yes | UP | 2 records; checksum matched | PASS |
| 3 | 1.038 | 1.111 | Yes | UP | 2 records; checksum matched | PASS |

Fresh authentication followed database readiness by 0.080, 0.073, and 0.073 seconds. No trial recorded an error or timeout. Each PostgreSQL start time changed, each health probe returned `UP`, and the same final-run baseline checksum survived all three restarts.

The final script emitted each result immediately and then emitted the complete three-trial summary. Controlled checks also reconfirmed that it pins the repository Compose file and isolated volume, enables cleanup before partial startup can fail, preserves three ordered results when a simulated middle trial fails, and terminates a bounded Docker command without stopping its target container.

Post-run inspection again found no isolated container, network, or volume. The original development containers retained IDs `8530db457ac5`, `f10ea7e36f63`, `2a4bbce1895e`, and `aac930bc226b`; all remained running, and `colegio-connect-dev_postgres_data` remained present with its original Compose label.

## Observed pre-trial failure

The first runtime attempt stopped before Trial 1: registration, verification, and login succeeded, but `/api/auth/me` returned HTTP 401 because PowerShell did not automatically replay the backend's `Secure` session cookie over HTTP. The disposable project was removed and the development project remained intact. This was not counted as a trial and no failed trial was replaced. After the cookie probe was corrected and committed as `1f2d5b1`, the next experiment produced the three immutable results above.

## GitHub evidence

- Canonical issue: [Issue #44](https://github.com/1uismar33r0/Colegio_Connect/issues/44), left open.
- Branch: `gabriel/issue-44-recovery`.
- Implementation checkpoint: `7c09313`.
- Verified cookie-probe correction: `1f2d5b1`.
- Failure-path hardening: `fcc633f`.
- Finalized-script verification revision: `c259a70`.
- Test record: [TE-10](../../testing/test-evidence.md#te-10).
- Review handoff: [PR #114](https://github.com/1uismar33r0/Colegio_Connect/pull/114), open against `main`; Cristian (`Massinl`) and Luis (`1uismar33r0`) were requested as reviewers. No merge or Issue #44 closure was performed.

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

This experiment uses one synthetic verified student and a local Docker volume. It tests process/container restart and backend reconnection, not host loss, storage corruption, point-in-time recovery, hosted snapshots, or restore into an empty volume. The fixture covers identity and verification data, not a representative full production dataset. Backup restoration, hosted-environment testing, teammate review, and applicable CI remain separate Issue #44 work, so this rehearsal alone cannot close the issue or establish final I16 acceptance.
