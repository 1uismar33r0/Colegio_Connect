# Colegio Connect

Colegio Connect is a three-developer university capstone project. The planned platform connects students, university offices, and external organizations through publishing, discovery, responses, moderation, and capstone sponsorship.

## Development status

Stage 1 is beginning. This repository currently provides a shared workspace and contribution guidance; the applications, database, local services, and CI have not been initialized yet. The full capstone proposal is not stored here. The architecture below reflects the agreed project plan and should be checked against the proposal when it becomes available.

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
| `backend/` | Spring Boot application and backend tests, to be initialized by Cristian |
| `frontend/` | React application and frontend tests, to be initialized by Luis |
| `docs/` | Shared project and development documentation |
| `infra/` | Docker and deployment configuration, to be added by Gabriel |
| `e2e/` | End-to-end tests when the application flows exist |
| `load-tests/` | Load tests when there are endpoints to measure |

These directories contain placeholders until their owners add the corresponding projects. There are no application run or test commands yet. Each owner should document commands when their project is initialized; Gabriel will document the combined local environment when it exists.

## Getting started

1. Clone the repository and create a branch from the current `main` branch.
2. Read [CONTRIBUTING.md](CONTRIBUTING.md) for the review and pull request workflow.
3. Read [docs/environment.md](docs/environment.md) before adding local configuration.

Do not commit passwords, tokens, connection strings containing credentials, or populated local environment files.
