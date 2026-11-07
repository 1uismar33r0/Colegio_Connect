# Frontend Docker Compose integration

## Purpose

This change adds Luis's React frontend to the existing local Docker Compose environment. A teammate can use one Compose model for the frontend, Massini's Spring Boot backend, PostgreSQL, and Mailpit instead of installing and connecting each service independently.

The work extends Issue #3's one-command local environment. The [traceability register](../../project/traceability.md) maps Issue #3 to Objective O5, indicators I13 and I16, Figure 1, and the infrastructure constraints in the [approved proposal](../../project/sources/approved-proposal.pdf). This checkpoint improves the shared development environment; it does not demonstrate the full I13 scenario target, I16 recovery target, production deployment, or Issue #3 acceptance.

## What Docker Compose does

Docker Compose reads `compose.yaml` and creates the services, private network, port mappings, dependencies, and named volumes described there. The `build` section tells Docker which directory and Dockerfile supply an image. Starting the Compose project then creates a container from that image.

For the frontend, Docker builds a Node 24 development image. The container starts Luis's existing `npm run dev` script and passes Vite `--host 0.0.0.0 --port 5173`. Binding to all container interfaces makes Vite reachable through the loopback-only host port. This is a Docker image build that installs source and dependencies; it is not the Vite `npm run build` production bundle and it does not add Nginx or another production server.

## Configuration sections

### `frontend/Dockerfile`

| Section | What happens | Why it is needed |
| --- | --- | --- |
| `FROM node:24-alpine` | Selects the Node 24 Alpine development image. | Matches the frontend CI runtime and keeps the image straightforward. |
| `WORKDIR /app` | Makes `/app` the container working directory. | Gives npm and Vite one predictable location. |
| Manifest copy and `npm ci` | Copies `package.json` and `package-lock.json`, then installs exactly the locked dependency tree. | Reproducible installation and useful Docker caching when application files change. |
| Source copy | Copies the remaining non-ignored frontend files. | Makes Luis's Vite application available to the container. |
| `EXPOSE 5173` | Describes Vite's container port. | Documents the port that Compose publishes. |
| `CMD` | Runs Vite on `0.0.0.0:5173`. | Allows the host-port mapping to reach the development server. |

### `frontend/.dockerignore`

The ignore file keeps dependencies, Vite output, coverage, test output, editor metadata, Git metadata, logs, local override files, and `.env` files out of the Docker build context. The build does not need a frontend environment file because Compose supplies `VITE_API_TARGET` directly. Excluding `.env` and `.env.*` prevents local secrets and machine-specific configuration from being copied into the image context.

### `frontend` service in `compose.yaml`

| Setting | Value and execution behavior |
| --- | --- |
| Build context | `./frontend` with `frontend/Dockerfile`. |
| Proxy environment | `VITE_API_TARGET=http://backend:8080`. Vite reads this existing variable from Luis's `vite.config.ts`. |
| Host access | `127.0.0.1:${FRONTEND_PORT:-5173}:5173`, so the site defaults to `http://localhost:5173` and is not exposed to the local network. |
| Dependency | `backend` with `condition: service_healthy`, so Compose does not start the frontend until Massini's backend passes its Actuator health check. |
| Restart policy | `unless-stopped`, matching the existing development services. |

No PostgreSQL, Mailpit, backend, or named-volume definition was changed. The existing `postgres_data` volume remains the canonical development database volume.

## How frontend-to-backend requests work

Luis's API client uses relative paths such as `/api/auth/csrf`. The browser sends that request to the same host and port that served the frontend. Vite receives it and uses the existing proxy configuration to forward it to `http://backend:8080` inside the Compose network.

The name `backend` works inside Compose service networking; the browser does not resolve it directly. Keeping the browser request on the frontend origin also supports Massini's session and CSRF cookie flow without adding a second browser-visible API origin.

Luis benefits from a repeatable Node 24 environment and one documented host URL. Massini benefits because frontend API requests use the backend service and its existing health gate instead of hard-coded host networking. Both can work against the same PostgreSQL and Mailpit services.

## Running the environment

Create the ignored local environment file once:

```powershell
Copy-Item .env.example .env
```

Replace the example database password, then build and start the environment:

```powershell
docker compose up --build
```

Default host endpoints are:

| Service | URL or port |
| --- | --- |
| Frontend | `http://localhost:5173` |
| Backend health | `http://localhost:8080/actuator/health` |
| Mailpit | `http://localhost:8025` |
| PostgreSQL | `localhost:5432` |

Set `FRONTEND_PORT` and the other documented port variables in `.env` when a default port is occupied. Source changes require rebuilding the selected reproducible image because this design intentionally has no source bind mount.

## Verification on November 6, 2026

### Revision and environment

- Base: synchronized `main` at `e5cc181b3ebcf7b5cb1050547c1d82922068c064`.
- Infrastructure commit: `39a8527102a709dcd1e943a98a90337782f3a19a` on `gabriel/issue-3-frontend-compose`.
- Host: Windows PowerShell; Docker client 29.5.2; Docker Compose 5.1.3; Node package manager 11.13.0.
- Data: configuration checks only; no student data or application records were used.

### Commands and actual results

| Check | Command | Actual result |
| --- | --- | --- |
| Missing-service red check | Parse `docker compose --env-file .env.example config --format json` and require `services.frontend`. | **Expected fail:** `Expected services.frontend to exist`. |
| Compose model | Parse the rendered JSON and assert all four services, frontend target port 5173, loopback binding, proxy target, healthy-backend dependency, restart policy, and `postgres_data`. | **Pass.** Existing services and volume declaration remained present. |
| ESLint | `npm.cmd run lint` | **Pass.** |
| Formatting | Standard `npm.cmd run format:check`, then a tracked-source check with `--end-of-line auto`. | Standard command **failed** on the Windows CRLF checkout and a local ignored test artifact; tracked-source check **passed**. No Luis source was rewritten. |
| TypeScript | `npm.cmd run typecheck` | **Pass.** |
| Unit tests | `npm.cmd test` | **Pass:** 5 files and 22 tests; reported duration 2.79 seconds. |
| Vite production compile smoke check | `npm.cmd run build` | **Pass:** 116 modules; Vite reported 244 milliseconds. This checks compilation only and is not the container's runtime mode. |
| Docker engine | `docker version` | **Pass:** client and server 29.5.2 after the user opened Docker Desktop. |
| Frontend image | `docker compose --project-name colegio-connect-frontend-compose-test --env-file .env.example build frontend` with isolated variables | **Pass:** 49.988 seconds; 264 locked packages installed; 212.26 kB build context. npm reported 11 moderate dependency vulnerabilities. |
| Full isolated stack | `docker compose ... up -d --build --wait --wait-timeout 300` | **Pass:** command completed in 31.363 seconds, including the backend build and service health waits. This is not a pure cold-start measurement. |
| Frontend HTTP | `Invoke-WebRequest http://127.0.0.1:15173/` | **Pass:** HTTP 200 in 138.348 ms; response contained the React root element. |
| Backend health | `Invoke-WebRequest http://127.0.0.1:18080/actuator/health` | **Pass:** HTTP 200 in 10.969 ms with status `UP`. |
| Mailpit HTTP | `Invoke-WebRequest http://127.0.0.1:18025/` | **Pass:** HTTP 200. |
| API proxy | Request `/api/auth/csrf` directly on backend port 18080 and through frontend port 15173. | **Pass:** both returned HTTP 204; the proxied response created one CSRF cookie. |
| Service state | Inspect the four containers. | **Pass:** backend, PostgreSQL, and Mailpit were healthy; frontend was running. |
| Volume isolation | Compare the isolated and canonical volume names and Compose labels. | **Pass:** `colegio-connect-frontend-compose-test_postgres_data` belonged to the isolated project and differed from `colegio-connect_postgres_data`. |
| Scoped cleanup | `docker compose ... down --remove-orphans` without `--volumes` | **Pass:** four isolated containers and their network were removed; zero of eight unrelated containers disappeared; all 11 volumes, including isolated and canonical database volumes, remained. |
| Final cached-image rerun | Repeat `up -d --no-build --wait`, HTTP/proxy/health assertions, and scoped cleanup. | **Pass:** services reached the wait condition in 12.646 seconds; frontend/backend/Mailpit returned 200, proxied CSRF returned 204 with one cookie, and all unrelated containers and volumes remained. |

### Runtime results and limitations

The first automated Docker Desktop launch attempt failed during inference-manager initialization because its local `dockerInference` listener path could not be accessed. The user then opened Docker Desktop, and the engine became healthy. All runtime results above were collected only after both Docker client and server reported version 29.5.2.

The isolated project used these noncanonical host ports:

- Frontend: 15173.
- Backend: 18080.
- PostgreSQL: 55432.
- Mailpit UI: 18025.
- Mailpit SMTP: 11025.

The test used synthetic local database credentials. It did not modify the canonical development project. Cleanup intentionally retained the disposable database volume because the instruction prohibited volume removal; a later explicitly authorized cleanup may remove that isolated volume. The 31.363-second measurement includes image-build work and is not a standalone cold-start-to-health benchmark. The 12.646-second final rerun reused built images and the retained isolated database volume, so it is a warm verification rather than a clean-machine measurement. The npm audit warning belongs to Luis's existing locked dependency tree and was not changed in this infrastructure task.

## GitHub and review evidence

- Branch: `gabriel/issue-3-frontend-compose`.
- Issue: [#3 - Docker development environment with PostgreSQL](https://github.com/1uismar33r0/Colegio_Connect/issues/3).
- Pull request: [#113 - Add frontend Compose service](https://github.com/1uismar33r0/Colegio_Connect/pull/113), with Luis requested as reviewer.
- Initial PR CI: [run 42333630640](https://github.com/1uismar33r0/Colegio_Connect/actions/runs/42333630640) passed Backend, Container, Frontend, and CI Gate on head `fd79dde`.
- Required reviewer: Luis, because this infrastructure runs his frontend and must preserve his development contract.

The PR must not be merged automatically or used to close Issue #3. Luis's review remains required, and each later head must keep CI green.

## Documentation integration

PR #107 already adds `docs/gabriel/README.md`. This branch does not create or edit that same file, avoiding a same-file conflict. When the branches are integrated, the README's Current records table should add an Infrastructure row linking `infrastructure/frontend-compose.md`.

## Remaining work

1. Obtain Luis's review and passing PR CI before integration.
2. Decide separately whether to address the 11 moderate npm audit findings in a dependency-focused task owned with Luis.
3. Add the infrastructure-page link to PR #107's Gabriel README when the branches are integrated.
4. Perform a clean-machine reproduction before treating the broader one-command environment criterion as accepted.

This checkpoint is not Issue #44 recovery testing and does not satisfy proposal indicator I16.
