# Colegio Connect

Colegio Connect is a three-developer university capstone project. The planned platform connects students, university offices, and external organizations through publishing, discovery, responses, moderation, and capstone sponsorship.

## Development status

As rechecked on November 4, 2026, `main` contains the Spring Boot backend foundation, Docker development environment, CI/Dependabot, and the Flyway identity database baseline from merged PRs #88 and #89. PRs #92 and #93 updated Spring Boot to 4.1.2 and springdoc to 3.2.0. Luis's exploratory HTML wireframes remain separate from `main`; no React application is established there. Authentication and business workflows remain incomplete on `main`.

Start with the [project status and ordered roadmap](docs/project/status.md), [approved proposal and requirement/issue traceability](docs/project/traceability.md), and [verified test evidence](docs/testing/test-evidence.md). The status page records observation dates, exact revisions, contributions and blockers. The architecture below describes the intended system, not completed functionality.

The [development policy](AGENTS.md) makes inspection, proposal alignment, testing, documentation and review part of every task. Historical progress and incidents are linked from the dashboard; the [midterm evidence index](docs/reports/midterm/evidence-index.md) tracks the November 10 report. Hosting remains a [pending decision](docs/decisions/0001-hosting.md); [deployment prerequisites](docs/deployment.md) describe the work still needed.

## Planned architecture

- Frontend: React and TypeScript (Luis)
- Backend and REST API: Java and Spring Boot (Cristian)
- Database: PostgreSQL with Flyway migrations (Cristian)
- Authentication and authorization: Spring Security (Gabriel and Cristian)
- Local development and deployment: Docker (Gabriel)
- CI/CD: GitHub Actions (Gabriel)

## Repository layout

| Path | Purpose |
| --- | --- |
| `backend/` | Spring Boot application and backend tests (Cristian) |
| `frontend/` | React application and frontend tests; `main` currently contains a placeholder |
| `docs/` | Shared project and development documentation |
| `infra/` | Docker and deployment configuration, to be added by Gabriel |
| `e2e/` | End-to-end tests when the application flows exist |
| `load-tests/` | Load tests when there are endpoints to measure |

Directories without a project contain placeholders until their owners add them. Each owner documents their commands when their project is initialized; Gabriel will document the combined local environment when it exists.

## Getting started

1. Clone the repository and create a branch from the current `main` branch.
2. Read [CONTRIBUTING.md](CONTRIBUTING.md) for the review and pull request workflow.
3. Read [docs/environment.md](docs/environment.md) before adding local configuration.

Do not commit passwords, tokens, connection strings containing credentials, or populated local environment files.

## Run the complete development stack

Requirements: Docker Desktop with Docker Compose.

1. Copy `.env.example` to `.env`.
2. Replace `POSTGRES_PASSWORD` in `.env` with a local-only password.
3. From the repository root, run `docker compose up --build --wait`.
4. Confirm that `docker compose ps` reports all three services as healthy. Use `docker compose logs -f backend` to follow backend logs when needed.

The stack exposes:

- Backend health: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/swagger-ui.html
- PostgreSQL: `localhost:5432`
- Mailpit inbox: http://localhost:8025
- Mailpit SMTP: `localhost:1025`

Stop the services with `docker compose down`. The named `postgres_data` volume keeps database data across container restarts and recreation. To deliberately remove local database data, run `docker compose down --volumes`.

## Backend

Requirements: Java 25 (Temurin). Check with `java -version`.

To run the backend outside Docker, first start its dependencies from the repository root with `docker compose up -d postgres mailpit`. Export the datasource variables documented in [docs/environment.md](docs/environment.md), then run from the `backend/` folder:

| Command | What it does |
| --- | --- |
| `./mvnw spring-boot:run` | Start the API at http://localhost:8080 |
| `./mvnw verify` | Run the Maven verification suite, including applicable Testcontainers integration tests |

Useful URLs while the app is running:
- Health: http://localhost:8080/actuator/health
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Swagger UI: http://localhost:8080/swagger-ui.html

### Package structure

The backend is organized by module (`accounts`, `listings`, ...). Each module
creates its own layers when needed: `controller/`, `service/`, `repository/`,
`entity/`, `dto/`. Shared code goes in `common/`.


### Registration (issue #7)

| Endpoint | What it does | Success |
| --- | --- | --- |
| `POST /api/auth/register` | Creates an account with a `@upr.edu` email and sends a 6-digit code | 201 |
| `POST /api/auth/verify` | Verifies the email with the code | 204 |
| `POST /api/auth/resend` | Sends a new code (max 1 per minute, 5 per hour) | 202 |

Codes expire in 10 minutes, can be used once, and are blocked after 5 wrong attempts.
Passwords must be 15 to 64 characters (NIST 800-63B-4).

In development, verification emails go to Mailpit: http://localhost:8025


### Login (issue #8)

| Endpoint | What it does | Success |
| --- | --- | --- |
| `GET /api/auth/csrf` | Sets the `XSRF-TOKEN` cookie. Call it once when the app loads | 204 |
| `POST /api/auth/login` | Starts a session (HttpOnly, Secure, SameSite=Lax cookie) | 200 |
| `GET /api/auth/me` | Returns the logged-in user | 200 |
| `POST /api/auth/logout` | Ends the session | 204 |

Every `POST` must send the `XSRF-TOKEN` cookie value in the `X-XSRF-TOKEN` header.
After 5 wrong passwords, the account is locked for 15 minutes.
Suspended accounts and unverified emails cannot log in.
All other endpoints require a logged-in user (401 otherwise).
