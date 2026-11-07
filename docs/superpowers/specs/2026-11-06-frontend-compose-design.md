# Frontend Docker Compose Integration Design

## Intent

Add Luis's React frontend to the existing one-command Docker Compose development environment so teammates can start the browser application, Massini's backend, PostgreSQL, and Mailpit together.

## Goals

- Build a reproducible frontend image with Node 24 and the committed npm lockfile.
- Run the existing Vite development server on container port 5173 and a documented localhost port.
- Forward browser `/api` requests through Vite to `http://backend:8080` by setting the existing `VITE_API_TARGET` variable.
- Preserve the existing backend, PostgreSQL, Mailpit, and `postgres_data` volume behavior.
- Verify configuration and, when Docker is available, build and run the full stack under an isolated Compose project name and host ports.

## Non-goals

- Do not change React components or Spring Boot source.
- Do not create a production frontend image or add a production web server.
- Do not add deployment, hosting, recovery, or Issue #44 work.
- Do not remove or reset canonical volumes or unrelated containers.

## Constraints

- Work only in the existing repository on `gabriel/issue-3-frontend-compose` based on verified `main` revision `e5cc181` or newer.
- Keep Docker and Compose configuration minimal and free of explanatory comments.
- Use `npm ci`, Node 24, the existing `package-lock.json`, Vite port 5173, and Luis's existing `VITE_API_TARGET` setting.
- Put configuration explanations and measured evidence in Markdown.

## Approaches considered

1. Build a reproducible Node 24 development image and copy the frontend source. This is selected because it is simple, lockfile-driven, and consistent across teammate machines. Source changes require rebuilding the image.
2. Add bind mounts and a separate `node_modules` volume. This improves live-edit convenience but adds Windows/Linux filesystem behavior and Compose complexity that the current request does not require.
3. Produce a Vite production build and serve it with Nginx. This is useful for deployment, but it does not run the requested development configuration and would require separate API routing behavior.

## Selected design

`frontend/Dockerfile` uses a Node 24 Alpine base, sets a working directory, copies the package manifests, runs `npm ci`, copies the remaining frontend source, exposes port 5173, and starts the existing `dev` script with Vite bound to `0.0.0.0`.

`frontend/.dockerignore` excludes dependency, build, test-output, editor, and Git artifacts from the build context.

`compose.yaml` adds a `frontend` service built from the frontend directory. It sets `VITE_API_TARGET=http://backend:8080`, maps a configurable loopback-only host port to container port 5173, waits for the backend health check, and follows the existing restart policy. No existing service or volume definition changes.

The browser connects to the frontend host port. The frontend uses relative `/api` URLs, so Vite receives those requests and proxies them over the Compose network to the service name `backend` on port 8080. This keeps the browser on one origin for session and CSRF cookies while connecting the two teammates' implementations.

## Verification

First validate the rendered Compose model using `.env.example`. If Docker can run, use an isolated project name and overridden host ports to build and start the stack, then record build duration, container health, frontend and Mailpit HTTP status codes, backend health response, one supported proxied API response, and evidence that the isolated project uses its own PostgreSQL volume. Stop only the isolated project and retain unrelated projects and volumes.

If the engine remains unavailable, record the successful static checks and the exact runtime limitation, push the tested work, and open the PR as a draft.

## Documentation

Create `docs/gabriel/infrastructure/frontend-compose.md`. PR #107 already creates `docs/gabriel/README.md`, so this branch does not create or edit that overlapping file; the future integrated index can link the infrastructure page without a same-file merge conflict. Update environment setup, development progress, test evidence, and the midterm evidence index only with results observed during this task. Issue #3 remains acceptance-pending until its full criteria and review requirements are satisfied.
