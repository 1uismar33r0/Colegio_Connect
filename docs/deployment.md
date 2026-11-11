# Deployment prerequisites and verification plan

**November 10 local staging update:** Approved `main` `f0c4efe` includes merged #128 role dashboards. The isolated Issue #4 Render Free preparation branch now has a combined production-style image, a proposed Free PostgreSQL 18 Blueprint and local evidence in the [Render staging runbook](deployment/render-staging.md) and [TE-16](testing/test-evidence.md#te-16). The dated checklist below is the October 26 baseline and should not be read as current source status. No Colegio Connect Render resource, public HTTPS URL or hosted recovery result exists; team review, authenticated Blueprint validation and deployment authorization remain required.

**Updated / observed: 2026-10-26. Status: Blocked for hosted acceptance.** No hosted application was verified. Gabriel reports no provider resources provisioned; A1 did not inspect provider accounts or provision anything. This is a prerequisite checklist for [Issue #4](https://github.com/1uismar33r0/Colegio_Connect/issues/4), not an operational runbook for an existing deployment.

The provider/budget decision remains [PROPOSED / PENDING APPROVAL](decisions/0001-hosting.md). Existing local setup is in [environment.md](environment.md); current PR dependencies are in [project status](project/status.md).

## Reusable implementation and unresolved dependencies

- `backend/Dockerfile`: Java 25 multi-stage build with a non-root runtime user; context `backend/`. Image packaging skips tests, so successful Maven CI must precede deployment.
- `compose.yaml`: local PostgreSQL 18.6 and Mailpit services, health ordering, loopback ports and persistent database volume. It is a development stack, not an approved hosted topology. Mailpit is not a production email service.
- `backend/src/main/resources/application.properties`: Actuator health with details hidden and Flyway enabled. Main has no migrations; PR #88 adds V1 and JPA validation. Startup success on zero migrations does not validate that V1.
- PR #89 supplies Backend, Container and CI Gate but remains open. No deployment workflow/provider integration exists on main. Its main-only image artifact is also unverified until an actual main push executes it.
- Security-sensitive product endpoints and real-user data must wait for the agreed authentication, authorization and privacy checks. An early synthetic-data infrastructure demonstration must explicitly describe its limited scope.

## Required before provisioning or release

| Prerequisite | Owner | Evidence needed |
| --- | --- | --- |
| Review/integrate #89; verify main CI | Gabriel; Cristian/Luis review | Passing run tied to main SHA, image artifact, failures/cancellations cannot pass gate |
| Review/integrate #88 | Cristian; Gabriel support | Fresh PostgreSQL 18 migration/JPA CI, schema/role contract review |
| Repository/deployment controls | Luis + Gabriel | Effective branch rules and available environment protection; secrets inaccessible to PR jobs |
| Measurements and provider decision | Gabriel + budget/account owner | Idle/startup/load RAM/CPU, connection use, approved region/service sizes, costs/limits, account/billing/recovery owner |
| Runtime configuration and email | Gabriel + Cristian | Secret names, private network configuration, database version, email provider/sender requirements and delivery tests |
| API/browser security | Cristian + Gabriel + Luis | Sessions/cookie/CSRF/CORS/HTTPS behavior appropriate to actual frontend topology; negative tests |
| Rollback and recovery | Gabriel + Cristian | Compatible previous app, migration failure handling, backup/restore ownership, timed recovery with sentinel data |

## Configuration inventory — names only

| Name or setting | Existing support | Hosted decision still needed |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | Compose supplies a JDBC URL | Private PostgreSQL 18 endpoint, TLS mode and provider-specific JDBC form; no credential values in docs |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Compose supplies local values | Runtime secret store, access/rotation and minimum database privileges |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT` | Compose points to Mailpit | Actual mail integration/provider, credentials and sender; current variables alone do not implement email |
| `SERVER_PORT` / provider port setting | Spring Boot defaults to 8080 | Verify provider listener routing; do not assume its generic `PORT` variable is consumed automatically |
| Provider deployment credential/service identifier | No configuration implemented | Least privilege, owner, branch-restricted environment, expiry/rotation; choose names only with provider decision |
| Frontend API base URL / allowed origins / cookie settings | No React/auth implementation yet | Agree exact names and values with frontend/API contract; do not invent runtime settings as if supported |

`POSTGRES_*` and host-port values in `.env.example` describe local Compose. Do not copy local passwords or Mailpit settings to hosting. Record only names and locations of secrets, never values or credential-bearing URLs.

## Intended deployment and failure behavior

The proposed release path is a reviewed main commit → successful required jobs → explicit deployment of that tested SHA → provider completion check → HTTPS/application/database verification. Provider autodeploy and gate semantics must be reviewed so skipped, cancelled or failed jobs cannot deploy. A deployment API accepting a request is not proof of a healthy release.

Flyway executes at startup. A failed app rollout can leave committed schema changes behind; application rollback does not reverse them. Require backward-compatible migrations or a separately reviewed recovery plan. Never automatically run destructive down migrations or restore over shared data. A restored database may lose writes after the recovery point; the team must state the recovery window and validate a restore on an isolated target first.

## Issue #4 evidence checklist for the eventual runbook

1. Record provider/account owner, approved spending cap and duration, region, CPU/RAM/storage, PostgreSQL version, image/source SHA and deploy ID.
2. Demonstrate HTTPS health and expected application readiness; record time and sanitized response. Validate database connectivity and V1 migration history separately on a fresh disposable database.
3. Demonstrate persistent synthetic records across a controlled restart. Record elapsed time to healthy and data checks; I16 requires health within five minutes with no data loss at final acceptance.
4. Prove public database access is disabled/restricted as designed; app uses private connection. Record redacted configuration evidence and secret-management/access controls.
5. Exercise a failed release and document that it cannot falsely count as success; verify previous-app compatibility after schema changes. Test backup restore separately from app rollback.
6. Record actual commands, URLs, logs, results, limitations and reviewer in [test evidence](testing/test-evidence.md); update [traceability](project/traceability.md), status and the [midterm index](reports/midterm/evidence-index.md).

Detailed deploy/rollback/restore commands must be written after the provider and resources exist and the procedures have been tested. No command in this document authorizes recurring spending or provisioning.
