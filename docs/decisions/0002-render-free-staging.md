# ADR 0002 — Render Free staging architecture trial

**Status:** Chosen for isolated implementation and measurement; public deployment is not authorized. **Date:** 2026-11-10 local. **Owner:** Gabriel; Cristian must review authentication/database details and Luis must review browser behavior. **Issue:** [#4 / alias #30](../project/traceability.md#issue-4--alias-30-select-hosting--hosted-postgresql-and-set-up-deploy-pipeline). **Source baseline:** `origin/main` `309ffa93b7403ae2269251374aeb73ad38e81b34`.

The user's current staging request selects Render Free and Mailpit for controlled testing. This amends the provider direction in [ADR 0001](0001-hosting.md) for **staging only**; it does not approve spending, production use, resource creation, or publication. The approved proposal's Java 25/PostgreSQL 18, near-zero budget, HTTPS/privacy, and recovery/performance targets remain unchanged.

## Trial design and alternatives

Trial A packages Node 24-built React assets, Java 25 Spring Boot, and Mailpit into one Docker web service. Spring serves both the SPA and `/api` on one origin, retaining existing relative API calls, session cookies, and CSRF behavior. Mailpit binds HTTP and SMTP to loopback; only Spring binds Render's `$PORT`. PostgreSQL 18 is a separate managed Free database in the same region, reached by its internal connection string. A small Hikari pool and bounded JVM heap target the Free web limit of 512 MB and 0.1 CPU. The exact memory/startup result must be measured before marking A viable.

Trial B is a Free static frontend plus Free backend web service and the same Free database. This would reduce backend image work only slightly; it introduces cross-origin credentials/CSRF/CORS configuration and uses the same Java-plus-Mailpit backend memory. Use B only if A fails measured resource or serving tests and B passes its own integrated tests. Do not quietly weaken session security to make B work.

The repository's current frontend calls relative `/api` and the backend does not serve SPA assets. Render-specific packaging and route fallback are therefore required. Compose and its backend Dockerfile remain the local development path. Staging registration/resend must be disabled by default because a loopback Mailpit inbox cannot deliver to real users and Render Free has no ordinary shell access. The mailbox stays private; no public inbox, code endpoint, or verification bypass is planned. Local synthetic tests may opt in to registration. Remote verification remains blocked until a separately reviewed access/delivery design exists.

## Platform and data limits

[Render Free documentation](https://render.com/docs/free.md) states a 15-minute idle spin-down, ephemeral web filesystem, shared 750 Free web instance hours per workspace/month, one Free Postgres per workspace, 1 GB storage, 30-day database expiry, no managed backups, and no shell access. [Compute allocations](https://render.com/docs/compute-plans) give Free web 512 MB/0.1 CPU and Free Postgres 256 MB/0.1 CPU. The connected workspace currently lists only unrelated PurpleGlass resources, including two Free web services and a paid PostgreSQL 18 database. Its actual remaining instance hours and ability to create the Colegio Free database have not been confirmed. None of those resources will be changed or reused.

A database created around November 10 would expire before the December 12 final demonstration. Staging data must be synthetic and disposable; before expiry, the team must approve and test a continuation plan (fresh Free database with verified export/import, or separately authorized paid storage). Neither app rollback nor a health response provides a backup or I16 recovery evidence.

## Acceptance gates

A is selected only after an isolated 512 MB container test reports startup time, heap/process memory, Mailpit overhead, HTTP/SPA/auth/CSRF behavior, database migration/restart behavior, and controlled shutdown. A public URL and report claim require a separate authorized Render provisioning step, signed-out HTTPS/browser checks, and teammate review. No proposal performance or recovery indicator is accepted by this local trial.
