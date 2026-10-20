# Environment configuration

The shared local environment has not been set up yet. Gabriel will add Docker and PostgreSQL setup instructions here when that work is implemented. Cristian and Luis will document the configuration their applications require as they initialize them.

## Team rules

- Keep real credentials and machine-specific values outside Git.
- Commit only example configuration with safe placeholders when an application needs it.
- Document each required setting, its purpose, and how to obtain a local development value before asking another team member to run the application.
- Use separate development values from any hosted environment values. Never reuse production credentials locally.
- If a secret is exposed in a commit or pull request, notify the team and rotate it; deleting the text from a later commit is not enough.

No environment variable names, ports, database accounts, or external providers have been selected in this repository yet.
