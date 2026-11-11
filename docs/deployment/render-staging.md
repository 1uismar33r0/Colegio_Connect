# Render Free staging runbook (pre-provisioning)

**State:** implementation and local verification in progress on `gabriel/issue-4-render-staging`, based on `main` `309ffa93b7403ae2269251374aeb73ad38e81b34`. No Colegio Connect Render resource or public URL has been created. Do not apply the eventual Blueprint until team review and explicit deployment authorization. [ADR 0002](../decisions/0002-render-free-staging.md) explains the architecture trial; [TE-16](../testing/test-evidence.md#te-16) records executed checks.

## Intended request and process path

Browser → Render HTTPS → Spring Boot on `$PORT` → private PostgreSQL 18. Vite compiles React once; Spring serves the assets and routes `/api` separately. Spring Security retains server sessions, a Secure/HttpOnly/SameSite=Lax session cookie, and the existing XSRF-TOKEN/X-XSRF-TOKEN flow. Render's proxy headers are interpreted by the staging profile. Mailpit SMTP and UI bind only to `127.0.0.1` inside the web container; no public Mailpit route or port is configured.

`backend/Dockerfile` and `compose.yaml` remain the local development path. The planned root `Dockerfile.render` builds both applications and includes the Mailpit binary. It does not run a Node development server at runtime. `application-render-staging.properties` binds Spring to the Render port, enables a small Hikari pool, exposes status-only health/readiness checks, turns off public Swagger/OpenAPI, and disables registration/resend by default. Local Compose uses the default profile and retains its registration flow.

## Public access boundary

The public React shell and known browser routes may load anonymously; React handles navigation and Spring still guards every `/api` business route. `/actuator/health` and `/actuator/health/readiness` return status only. API/admin routes, verification codes, Mailpit UI/API/SMTP, database credentials and logs are not published. With a private inbox and no ordinary Free shell access, remote registration and verification are **blocked** until a separately reviewed secure method exists. No code is returned through the public application and verification is not bypassed. A local synthetic test may explicitly enable registration; never enable it in a public staging service without a safe delivery/retrieval mechanism.

## Current limits and next steps

- Render Free web: 512 MB/0.1 CPU, idle spin-down after 15 minutes and ephemeral filesystem. Session and Mailpit messages in process/local storage may be lost on spin-down.
- Render Free PostgreSQL: 256 MB/0.1 CPU, 1 GB, one active Free instance per workspace, expiry after 30 days, no managed backups. The team must plan a fresh staging database or authorized upgrade before the December 12 demonstration.
- The connected workspace's two PurpleGlass Free web services and paid database are unrelated and must remain untouched. Remaining Free hours and a Colegio database slot require a final dashboard check at authorized provisioning.
- The local container measurement, Blueprint validation, database recovery, browser checks and current-head CI results will be added here as each phase completes. Do not infer a public HTTPS result from local tests.

## Reference sources

[Render Free limits](https://render.com/docs/free.md), [compute plans](https://render.com/docs/compute-plans), [Blueprint schema](https://render.com/docs/blueprint-spec.md), and [Postgres private connection guidance](https://render.com/docs/postgresql-creating-connecting). These were read on 2026-11-10 local; confirm current account limits when provisioning.
