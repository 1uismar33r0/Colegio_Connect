# Frontend Docker Compose Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Luis's Vite frontend to the existing Docker Compose development stack with reproducible Node 24 installation and working proxy access to Massini's backend.

**Architecture:** A minimal frontend image installs the committed npm lockfile and runs the existing Vite development server. Compose publishes Vite on a configurable loopback host port and sets the existing `VITE_API_TARGET` variable to the backend service name on the internal network.

**Tech Stack:** Docker Compose, Dockerfile, Node.js 24 Alpine, npm, Vite 8, React 19, Spring Boot

**Spec:** `docs/superpowers/specs/2026-11-06-frontend-compose-design.md`

## Global Constraints

- Work only on `gabriel/issue-3-frontend-compose` in the existing repository.
- Do not change React components, Spring Boot source, existing service behavior, or named-volume definitions.
- Do not remove canonical volumes, stop unrelated containers, or create another repository or worktree.
- Keep Docker and YAML files free of explanatory comments.
- Use Node 24, `npm ci`, Vite container port 5173, and `VITE_API_TARGET=http://backend:8080`.
- Record only observed test results and keep Issue #3 acceptance separate from branch implementation.

## Review Focus

- A build context containing local `.env` files must not send those secrets to the Docker builder; verify ignore rules match `.env` and `.env.*` while allowing `.env.example` only if needed.
- Vite must listen on `0.0.0.0`, not only container localhost; verify the host-published port returns HTTP.
- Browser `/api` traffic must reach `backend:8080` through the existing Vite proxy; verify a supported endpoint through the frontend host port.
- The frontend must wait for a healthy backend without changing PostgreSQL or Mailpit dependency behavior; inspect rendered dependencies and runtime health.
- Isolated verification must not reuse or delete the canonical `colegio-connect_postgres_data` volume; compare project resources before and after cleanup.

---

### Task 1: Reproducible frontend container and Compose service

**Files:**
- Create: `frontend/Dockerfile`
- Create: `frontend/.dockerignore`
- Modify: `compose.yaml`
- Modify: `.env.example`

**Interfaces:**
- Consumes: `frontend/package.json`, `frontend/package-lock.json`, the `dev` script, and `VITE_API_TARGET` read by `frontend/vite.config.ts`.
- Produces: a `frontend` Compose service available on `${FRONTEND_PORT:-5173}` and proxying `/api` to the `backend` service.

- [x] **Step 1: Run the missing-service assertion and verify RED**

Run a PowerShell assertion over `docker compose --env-file .env.example config --format json` that requires `services.frontend`, port target `5173`, `VITE_API_TARGET=http://backend:8080`, and a healthy-backend dependency.

Expected: FAIL because `services.frontend` is absent.

- [x] **Step 2: Create the minimal frontend Dockerfile**

Use `node:24-alpine`, `/app`, separate package-manifest copy, `npm ci`, source copy, `EXPOSE 5173`, and `npm run dev -- --host 0.0.0.0 --port 5173`.

- [x] **Step 3: Create the frontend Docker ignore rules**

Exclude `.git`, `.github`, `.vscode`, `node_modules`, `dist`, coverage and test output, logs, local editor files, `.env`, and `.env.*`; retain only inputs required by the selected build.

- [x] **Step 4: Add the frontend Compose service and host-port example**

Build from `./frontend`, set `VITE_API_TARGET=http://backend:8080`, publish `127.0.0.1:${FRONTEND_PORT:-5173}:5173`, require `backend` health, and use `restart: unless-stopped`. Add `FRONTEND_PORT=5173` to `.env.example`.

- [x] **Step 5: Run the rendered-model assertion and verify GREEN**

Expected: PASS for service, build context, command/image behavior, port, environment, dependency, existing services, and unchanged `postgres_data` declaration.

- [x] **Step 6: Run static frontend checks**

Run `npm run lint`, `npm run format:check`, `npm run typecheck`, `npm test`, and `npm run build` from `frontend`.

Expected: all commands pass without source changes.

- [x] **Step 7: Commit the infrastructure increment**

Commit the Dockerfile, ignore file, Compose change, and environment example as `feat(docker): add frontend compose build`.

### Task 2: Isolated runtime verification

**Files:**
- Modify only if required by a reproduced configuration defect: files owned by Task 1

**Interfaces:**
- Consumes: the frontend image and Compose service from Task 1.
- Produces: measured build/startup/HTTP/health/proxy evidence for Task 3.

- [x] **Step 1: Check Docker engine availability**

Run `docker version` and record client/server versions. If the engine is unavailable, preserve the exact error and skip runtime claims.

- [x] **Step 2: Inventory existing Compose projects, containers, and volumes**

Record read-only identifiers needed to prove that cleanup targets only the isolated test project.

- [x] **Step 3: Build under an isolated project name and measure elapsed time**

Passed: isolated frontend image build completed in 49.988 seconds.

Use `.env.example`, a unique project name, and noncanonical host ports. Record the frontend image build result and elapsed seconds.

- [x] **Step 4: Start the isolated stack and wait for health**

Passed: full `up --build --wait` completed in 31.363 seconds, including backend build and health waits.

Start only the isolated project. Record container states and startup duration without stopping unrelated services.

- [x] **Step 5: Verify HTTP and proxy behavior**

Passed: frontend/backend/Mailpit returned HTTP 200; direct and proxied CSRF returned 204 and the proxied flow set a CSRF cookie.

Require frontend `/` HTTP 200, backend `/actuator/health` HTTP 200 with `UP`, Mailpit UI HTTP 200, and a supported `/api` endpoint through the frontend host port with the same status/body class expected from the backend.

- [x] **Step 6: Verify PostgreSQL health and volume isolation**

Passed: PostgreSQL was healthy and the isolated project volume differed from the canonical volume.

Require healthy PostgreSQL and prove its volume belongs to the isolated project rather than `colegio-connect_postgres_data`.

- [x] **Step 7: Stop only the isolated project**

Use the explicit isolated project name. Do not pass `--volumes`; retain the disposable test volume unless the user separately authorizes deletion. Re-inventory unrelated resources.

Passed: four isolated containers and their network were removed without `--volumes`; zero unrelated containers and zero volumes disappeared.

### Task 3: Documentation and evidence

**Files:**
- Create: `docs/gabriel/infrastructure/frontend-compose.md`
- Modify: `docs/environment.md`
- Modify: `docs/progress/development-log.md`
- Modify: `docs/testing/test-evidence.md`
- Modify: `docs/reports/midterm/evidence-index.md`
- Modify: `docs/superpowers/plans/2026-11-06-frontend-compose.md`

**Interfaces:**
- Consumes: Task 1 configuration and Task 2 observations.
- Produces: teammate instructions, Issue #3 evidence, and report-index references without conflicting with PR #107's unmerged `docs/gabriel/README.md`.

- [x] **Step 1: Write the Gabriel infrastructure record**

Explain the purpose, proposal/Issue #3 mapping, Dockerfile stages, ignore rules, Compose service, `VITE_API_TARGET`, commands, actual measurements, Luis/Massini integration, limitations, and GitHub evidence placeholders.

- [x] **Step 2: Update shared setup and evidence records**

Add `FRONTEND_PORT`, startup instructions, dated revision/environment/results, and midterm evidence links. Do not claim production behavior or full Issue #3 acceptance.

- [x] **Step 3: Avoid the PR #107 README overlap**

Do not create `docs/gabriel/README.md` on this branch. Record that PR #107's index should add a row linking `infrastructure/frontend-compose.md` during integration, which makes the two branches merge without a same-file conflict.

- [x] **Step 4: Run documentation checks**

Run `git diff --check`, verify every relative Markdown link changed or added resolves, scan changed files for secrets, and confirm the evidence matches captured command output.

- [x] **Step 5: Commit the documentation increment**

Commit the design, plan, infrastructure record, and evidence updates as `docs(docker): explain frontend compose integration`.

### Task 4: Branch review, push, and pull request

**Files:**
- No new repository files unless review identifies an in-scope defect.

**Interfaces:**
- Consumes: Tasks 1-3 commits and test outputs.
- Produces: one focused reviewable PR referencing Issue #3 and Luis's request.

- [x] **Step 1: Run final verification from a clean review point**

Re-run Compose model validation, frontend static checks, runtime checks when the engine is available, `git diff --check`, and inspect the complete branch diff against `origin/main`.

- [x] **Step 2: Review scope and history**

Confirm no React/Spring source changes, no volume deletion, no unrelated files, and coherent commits.

- [x] **Step 3: Push without force**

Push `gabriel/issue-3-frontend-compose` and set its upstream.

- [x] **Step 4: Open one focused PR**

Reference Issue #3, request Luis's review, list actual checks and limitations, and mark the PR draft if runtime verification was unavailable or incomplete. Do not merge or close Issue #3.

- [x] **Step 5: Inspect PR and CI state**

Record the PR number, URL, head SHA, review state, and actual Actions results without treating an open PR as merged acceptance.
