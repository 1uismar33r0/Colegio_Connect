# Environment configuration

Docker Compose provides the shared backend, PostgreSQL 18, and Mailpit development environment. Copy `.env.example` to the ignored `.env` file and replace the sample database password before starting it. Never commit `.env`.

## Team rules

- Keep real credentials and machine-specific values outside Git.
- Commit only example configuration with safe placeholders when an application needs it.
- Document each required setting, its purpose, and how to obtain a local development value before asking another team member to run the application.
- Use separate development values from any hosted environment values. Never reuse production credentials locally.
- If a secret is exposed in a commit or pull request, notify the team and rotate it; deleting the text from a later commit is not enough.

## Docker Compose variables

| Variable | Required | Purpose |
| --- | --- | --- |
| `POSTGRES_DB` | Yes | Local database name |
| `POSTGRES_USER` | Yes | Local database role |
| `POSTGRES_PASSWORD` | Yes | Local-only database password; replace the example value |
| `POSTGRES_PORT` | No | Host PostgreSQL port; defaults to `5432` |
| `BACKEND_PORT` | No | Host backend port; defaults to `8080` |
| `MAILPIT_SMTP_PORT` | No | Host SMTP port; defaults to `1025` |
| `MAILPIT_UI_PORT` | No | Host Mailpit UI port; defaults to `8025` |

Ports bind to `127.0.0.1` so development services are not exposed to the local network. PostgreSQL data is stored in the `postgres_data` named volume. PostgreSQL 18 uses `/var/lib/postgresql` as its persistent volume mount.

## Backend

- Java 25 is required.
- The API runs on port 8080 (Spring Boot default).
- In Docker, Compose supplies `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` to the backend.
- To run Spring Boot directly on the host, set those same variables. Use `jdbc:postgresql://localhost:5432/<POSTGRES_DB>` as the URL and reuse the local username and password from `.env`.
- Compose also supplies `SPRING_MAIL_HOST=mailpit` and `SPRING_MAIL_PORT=1025`. Outside Docker, the backend uses `localhost:1025` (Mailpit on the host).
- Flyway is enabled and applies the versioned migrations in `backend/src/main/resources/db/migration`. The merged identity baseline begins with `V1__identity_baseline.sql`; do not modify an applied migration. Add a new versioned migration for later schema changes.
- `app.mail.from` sets the sender address for emails (default `no-reply@colegioconnect.local`).
- `app.mail.provider` selects the email adapter. The packaged application only contains `smtp` (the default; Mailpit in development). `fake` keeps emails in memory and exists only in test sources (`src/test/.../FakeEmailSender`), so setting it on a deployed app leaves no email sender and startup fails instead of silently discarding mail. Supporting a new vendor only takes a new `EmailSender` implementation in `notifications/`.
- Verification emails are sent after the registration or resend transaction commits (`VerificationEmailListener`). If delivery fails, the request still succeeds, an `Email delivery FAILURE` line is logged with a masked address, and the user recovers with **Resend code** after the 60 second cooldown. There is no automatic retry.


## Verification and related documentation

The following setup facts were rechecked against GitHub on November 4, 2026. PR #89 (CI) and PR #88 (Flyway identity baseline and PostgreSQL 18.6 Testcontainers) merged into `main` on October 30, followed by the Spring Boot 4.1.2 and springdoc 3.2.0 dependency updates in PRs #92 and #93. The merged CI workflow runs `./mvnw --batch-mode --no-transfer-progress verify`, validates Compose, and builds the backend image. Docker must therefore be available for the applicable Testcontainers integration tests and Compose checks.

The local host must actually select Java 25; changing the Maven project version does not install a JDK. Docker's Java 25 build is an alternative when the engine is available. Port overrides documented above can avoid conflicts with existing services. Choose the local database password before first initialization; changing `.env` later does not reset an existing PostgreSQL volume's credentials.

The October 26 baseline observations remain historical records in [test evidence](testing/test-evidence.md) and [project status](project/status.md); they are not evidence that the current branch has been retested. Hosted configuration, private networking, secrets, and recovery remain prerequisites in [deployment.md](deployment.md). Follow [AGENTS.md](../AGENTS.md) when changing setup or recording new verification.
