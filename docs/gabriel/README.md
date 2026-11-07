# Gabriel engineering documentation

This directory explains Gabriel's infrastructure, CI/CD, hosting, reliability, security, performance, and integration work. It is an accessible navigation layer over the canonical project records; it does not replace the approved proposal, the [traceability register](../project/traceability.md), or [test evidence](../testing/test-evidence.md).

## Evidence states used here

- **Implemented on branch:** the change exists at a named commit but is not on `main`.
- **Locally verified:** a recorded command passed on the named workstation and revision.
- **CI verified:** a linked GitHub Actions run passed on the named revision.
- **Merged:** the change is present on `main`.
- **Blocked:** a named dependency or external decision prevents the next acceptance step.

A green build or an open pull request is not issue acceptance. The project must still satisfy review, integration, and the exact acceptance criteria recorded in the traceability register.

## Current records

| Area | Record | Current scope |
| --- | --- | --- |
| CI/CD | [Issue #2 CI trigger coverage](ci-cd/issue-2-ci-trigger-coverage.md) | All-branch push, all-PR, and manual GitHub Actions triggers stacked on Luis's PR #106 |
| Integration | [Team dependencies](integration/team-dependencies.md) | Verified frontend/backend contract and repository-owner blockers |

## Related canonical records

- [Project status](../project/status.md)
- [Deployment prerequisites](../deployment.md)
- [Development log](../progress/development-log.md)
- [Test evidence](../testing/test-evidence.md)
- [Midterm evidence index](../reports/midterm/evidence-index.md)
- [Hosting decision](../decisions/0001-hosting.md)
