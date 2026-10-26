# Colegio Connect

Colegio Connect is a three-developer university capstone project. The planned platform connects students, university offices, and external organizations through publishing, discovery, responses, moderation, and capstone sponsorship.

## Development status

As observed on October 26, 2026, the Spring Boot backend foundation and Docker development environment are on `main`. CI/Dependabot is implemented with passing branch checks in open PR #89; the identity database is in open PR #88. Luis has exploratory HTML wireframes, but no React application is established on `main`. Authentication and business workflows remain incomplete.

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
| `frontend/` | React application and frontend tests, to be initialized by Luis |
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
| `./mvnw test` | Run the tests |

Useful URLs while the app is running:
- Health: http://localhost:8080/actuator/health
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Swagger UI: http://localhost:8080/swagger-ui.html

### Package structure

The backend is organized by module (`accounts`, `listings`, ...). Each module
creates its own layers when needed: `controller/`, `service/`, `repository/`,
`entity/`, `dto/`. Shared code goes in `common/`.
