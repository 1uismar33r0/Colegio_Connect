# ADR 0001 — Hosting selection after resource measurements

**Status: PROPOSED / PENDING APPROVAL.** Updated 2026-11-06; provider research refreshed 2026-11-04. Owner: Gabriel; reviewers: Cristian and Luis; budget/account owner: not assigned. Related [Issue #4](https://github.com/1uismar33r0/Colegio_Connect/issues/4), aliases and exact acceptance in [traceability](../project/traceability.md).

No provider is selected or approved. No recurring payment is authorized. Gabriel reports no provider resources provisioned; this task did not inventory provider accounts. The earlier **$44/month Render recommendation is superseded by a measurement-led decision process**, not by approval of a cheaper configuration.

## November 4 provider refresh and recommendation

**Research result, not an approved deployment decision.** The approved 30-page proposal was rechecked with SHA-256 `88038171ef23ffbd42bcbe38cd0ec5177240e42ab010fb6fbed96107b1b8f98c`. Its near-zero budget, provider-independent email, PostgreSQL 18 baseline, HTTPS/privacy boundary, 25-VU / 5,000-listing workload, and I16 recovery target remain governing constraints. Issue #4 additionally requires an HTTPS deployment and documented CPU/memory.

Keep **Render paid web service plus paid Render Postgres** and **Railway application plus private PostgreSQL** as the two candidates for measured comparison. Neither is approved or provisioned. Render is the provisional first comparison candidate because its documentation describes managed Postgres, private networking, paid-database point-in-time recovery, Docker-compatible web services, and managed TLS. Railway remains a viable comparison because its PostgreSQL service is private by default and exposes service connection variables, but the default single-node service requires an explicit decision about backups, recovery, pooling, and availability.

Do **not** select Fly Managed Postgres for the current proposal: its official Managed Postgres documentation identifies PostgreSQL 16, while the project migration uses PostgreSQL 18 `uuidv7()`, and its Basic managed-database floor is $38/month before storage and application compute. Self-managed Fly Postgres is not a substitute unless the team explicitly accepts ownership of backups, upgrades, recovery, and PostgreSQL 18 verification.

| Candidate | Current official evidence | Decision status |
| --- | --- | --- |
| Render paid web service + paid Postgres | Paid Postgres has point-in-time recovery; free web services sleep after 15 minutes and free Postgres expires after 30 days. [Pricing](https://render.com/pricing), [service types](https://render.com/docs/service-types), [FAQ](https://render.com/docs/faq). | Compare after measurements; free tiers do not meet dependable semester persistence/recovery. |
| Railway application + private PostgreSQL | PostgreSQL is private by default and provides service connection variables; major upgrades provide preflight, backup, and revert. [PostgreSQL](https://docs.railway.com/databases/postgresql), [cost control](https://docs.railway.com/pricing/cost-control). | Compare after confirming PostgreSQL 18, backup/PITR, team access, and connection limits. |
| Fly Managed Postgres | Basic starts at $38/month plus $0.28/GB storage and official docs identify PostgreSQL 16. [Pricing](https://fly.io/pricing-update/), [Managed Postgres](https://fly.io/docs/mpg/). | Excluded unless the team/advisor approves an exception. |

Before selection, record source SHA and cold-start/idle CPU/RAM/connection/storage measurements; full monthly cost including storage, egress, backups, team access, email and tax; provider/account/billing owner; region; PostgreSQL 18 confirmation; TLS/private-network/secrets configuration; transactional email owner; recovery point/restore ownership; and a timed synthetic-data recovery rehearsal. The local React/authentication preview is not deployed acceptance evidence.

## Context and requirements

The approved proposal requires a near-zero budget (pp.11,19), Java 25/Spring Boot and PostgreSQL 18 (p.13), HTTPS/privacy, CI verification, and recovery within five minutes with no data loss (I16). Its final workload is at least 5,000 listings, 1,000 users, 50 organizations and 25 concurrent virtual users. The team has not measured the resources needed to meet those targets.

The existing `backend/Dockerfile` packages Java 25 and runs as a non-root user. PR #88's migration uses `uuidv7()`, so the intended PostgreSQL 18 environment must be verified rather than silently replaced with an older major version. PR #89's CI is merged and builds/tests the backend and container, but it does not deploy them. See [deployment prerequisites](../deployment.md).

## Candidates and cost assumptions

USD monthly planning estimates, observed/refreshed October 26, 2026. These are not quotes or full bills. Taxes, database storage, backups where charged, egress/build overages, workspace/team access, email, domains and funded duration must be included before approval. No academic credits are assumed available.

| Candidate | Cost basis | Advantages / unresolved limitations |
| --- | --- | --- |
| Render, smaller paid configurations | App 512 MB/0.5 CPU $7 + database 256 MB $6 = **$13 compute/month**; using a 1 GB database at $19 gives **$26 compute/month**. Storage and other charges are additional. | Predictable candidate sizes and PostgreSQL 18 support. Java startup/load and DB memory sufficiency are unmeasured. |
| Render, larger app | Earlier app 2 GB/1 CPU $25 + 1 GB database $19 = **$44 compute/month**; with 256 MB DB, $31. | Historical comparison retained for context. No measurement justifies selecting it now. Reconfirm current quote before any purchase. |
| Railway | Hobby $5 minimum including $5 usage. Service RAM $10/GB-month, CPU $20/vCPU-month, volume $0.15/GB-month, egress $0.05/GB. | Example assumptions: 0.75 GB combined RAM, 0.1 vCPU, 1 GB volume, 1 GB egress → $9.70 before backups/other charges. These are not measured usage. Workspace collaboration can require Pro ($20 minimum); confirm team-access needs. |
| Fly.io + Managed Postgres | Managed Postgres Basic $38/month plus $0.28/GB storage; application Machine, network and any other charges additional. Previous research's regional 1 GB Machine example was $8.78, not freshly re-quoted here. | Managed DB cost is relatively high for this budget. PostgreSQL 18 availability for the specific managed offering remains unconfirmed; the current overview could not be retrieved in this check. Do not substitute self-managed PostgreSQL without explicitly owning backups, upgrades and recovery. |

Render amounts are supported by [official pricing](https://render.com/pricing?trk=article-ssr-frontend-pulse_little-text-block), [official comparison pricing](https://render.com/articles/render-vs-netlify) and [compute allocations](https://render.com/docs/compute-plans). Its main pricing page's extracted body omitted some dynamic tables, so the earlier $25 app quote remains explicitly historical pending a final quote. The [workspace plan change](https://render.com/docs/new-workspace-plans) must also be checked for team-access charges.

Railway rates and minimums come from [service pricing](https://docs.railway.com/pricing/plans); the service rates above are not its separately priced Sandbox rates. [Fly's October 1 pricing update](https://fly.io/pricing-update/) confirms the managed-database rates, not PostgreSQL 18 compatibility.

## Compatibility, networking and operations

Render documents PostgreSQL 18, same-region private connections and paid database recovery features. Its free database expires after 30 days and has no backups; free web services sleep after inactivity. That is not a dependable semester persistence plan. Close external database access as part of configuration verification. Sources: [PostgreSQL connection guidance](https://render.com/docs/postgresql-creating-connecting), [free-service limits](https://render.com/docs/free).

Railway's [PostgreSQL HA documentation](https://docs.railway.com/databases/postgresql-ha) lists version 18 among supported pinned official images for conversion. This supports considering it, but does not prove that a chosen single-node template, backup arrangement or HA service has been provisioned or tested. Validate private networking, database maintenance ownership and restore behavior for the actual candidate.

For every provider, require HTTPS for clients, a private app/database path, runtime secrets outside Git, explicit credential owners and rotation, and a real transactional email service. Do not assume developer Mailpit is suitable for deployment. Use synthetic records until account/privacy controls are verified. The provider's region should be selected with measured latency and cost, not assumed from the developer's location.

## Proposed decision process

1. Integrate and verify CI/database prerequisites. In parallel, measure existing container startup/idle RAM, CPU, database connections and baseline storage on a documented machine and SHA. These measurements inform sizing; they do not establish I3 performance compliance.
2. Compare the smallest viable Render and Railway options using recorded measurements, service limits, collaboration/access requirements, backup costs and a complete monthly estimate. Retain Fly.io only if its capabilities/cost are justified and PostgreSQL 18 is confirmed.
3. Obtain the team's provider approval and the budget owner's explicit ceiling, payment/funding period, billing/account ownership and shutdown/handoff plan. If no viable option fits, record hosted acceptance as blocked; do not silently authorize spending.
4. After authorization, implement Issue #4 and write the tested runbook. Keep provider autodeploy disabled unless its actual gating semantics guarantee the approved policy. Prefer an explicit release step after required jobs succeed, deploy the tested SHA, poll completion and verify the deployed build.
5. Re-measure with real discovery endpoints and the approved workload when #21/#25 exist. Revise capacity through a documented decision; do not retrofit proposal targets to fit a cheap instance.

## Migration, rollback and recovery risks

Flyway startup migration and application rollout are separate outcomes. An unhealthy release may have changed the shared schema already; reverting the application does not undo that schema. Require compatibility review, a tested previous-app path, disposable migration-failure tests and a distinct restore procedure. Record backup retention/recovery point, who can restore, data-loss exposure and a timed restart with unchanged sentinel data. A provider rollout timeout is not evidence of I16's five-minute recovery target.

## Approval record to complete with evidence

Pending: measured resource report; exact provider/service/version/region; full monthly cost and limits; funded period; account/billing owner; team review; private database configuration; backup/restore plan; deployment credential scope; environment/repository protection availability; security scope of the initial release. Record approver, date and reference here only after a real decision. No implementation or deployment is accepted by this ADR.
