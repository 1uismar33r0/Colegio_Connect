# Colegio Connect

Colegio Connect is a three-developer university capstone project. The planned platform connects students, university offices, and external organizations through publishing, discovery, responses, moderation, and capstone sponsorship.

## Development status

The backend (Spring Boot) has been initialized. The frontend, database, local services, and CI have not been initialized yet. The full capstone proposal is not stored here. The architecture below reflects the agreed project plan and should be checked against the proposal when it becomes available.

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

## Backend

Requirements: Java 25 (Temurin). Check with `java -version`.

Run from the `backend/` folder:

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