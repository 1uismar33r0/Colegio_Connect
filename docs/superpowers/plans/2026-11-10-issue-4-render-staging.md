# Issue #4 Render Free staging implementation plan

> **For agentic workers:** Execute this plan task by task in the isolated `gabriel/issue-4-render-staging` worktree. Use the repository's testing and review workflow; do not create Render resources or merge.

**Goal:** Produce a reviewable single-origin Render Free staging build and evidence, with a private Mailpit and an explicit remote-verification limit.

**Architecture:** A root multi-stage Dockerfile builds Vite assets and Java 25, then runs Spring and loopback Mailpit under a signal-aware supervisor. Render Free PostgreSQL remains separate. The implementation is tested locally at the Free 512 MB limit before the Blueprint is considered ready.

**Tech stack:** Node 24, Java 25, Spring Boot 4.1.2, PostgreSQL 18.6, Mailpit v1.31.5, Docker, Render Blueprint.

**Spec:** [ADR 0002](../../decisions/0002-render-free-staging.md), [Issue #4](https://github.com/1uismar33r0/Colegio_Connect/issues/4), and the user's Render staging request.

## Global constraints

- Base on `main` `309ffa9`, preserve other worktrees, Compose, Spring authorization, and the approved proposal.
- Keep database, SMTP, and Mailpit UI off public ports; no secrets or verification codes in Git/logs.
- Do not provision, publish, merge, pay, or modify PurpleGlass resources.
- Record only executed measurements and distinguish local checks from hosted evidence.

## Review focus

- Anonymous requests to API/admin paths must stay 401/403, never receive SPA HTML.
- Deep-link refresh must return the SPA, while missing `/assets` returns a real 404.
- A missing database must make readiness fail and recovery must work after reconnection.
- If Mailpit exits or the container receives SIGTERM, both child processes must terminate.
- Public staging registration must be blocked while inbox access is private; normal local registration remains available.

### Task 1: Single-origin SPA and staging gate

**Files:** `backend/src/main/java/com/colegioconnect/web/SpaController.java`, `SecurityConfig.java`, `RegistrationService.java`, focused MockMvc tests, `backend/src/main/resources/application-render-staging.properties`, environment/security documentation.

- [ ] Write failing tests for deep links/API separation and staging registration denial.
- [ ] Run them and record the failure.
- [ ] Implement route handling, proxy/port/readiness settings, and property-driven registration gate without changing local defaults.
- [ ] Run focused and complete backend checks; update docs with actual result and commit.

### Task 2: Reproducible staging image and process lifecycle

**Files:** `Dockerfile.render`, `.dockerignore`, `infra/render/entrypoint.sh`, focused shell/HTTP smoke script, deployment guide.

- [ ] Build frontend with `npm ci`, package Java 25, copy static assets, add Mailpit binary and non-root runtime.
- [ ] Test entrypoint under 512 MB with synthetic Postgres; record startup, RSS/CPU, health, assets, deep links, SMTP and shutdown results.
- [ ] Fix failures and update ADR/runbook/evidence; commit.

### Task 3: Render configuration and full integration

**Files:** `render.yaml`, `docs/deployment/render-staging.md`, `docs/environment.md`, README, test evidence, progress log, report evidence template.

- [ ] Define a Free Docker web service and isolated Free PostgreSQL 18 with internal credential references and auto-deploy off; validate current Blueprint syntax.
- [ ] Exercise fresh/upgraded migrations, database loss/reconnect/restart, auth/CSRF, synthetic email and browser routes on the local production image.
- [ ] Compare measured A with the static/frontend alternative; document limitations, continuation, rollback, exact commands and results; commit.

### Task 4: Reviewable PR and handoff

- [ ] Check source/links/variable names, Git diff, tests, image and 512 MB result on the final head.
- [ ] Push the branch, open one focused PR against `main`, and request Cristian/Luis review. Do not merge or provision.
- [ ] Report actual readiness, hosted gaps, current revisions and PR/CI status.
