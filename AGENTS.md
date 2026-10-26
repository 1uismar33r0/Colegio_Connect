# Colegio Connect development policy

## Required lifecycle

**INSPECT → ALIGN WITH PROPOSAL → CHECK ISSUES → PLAN → IMPLEMENT → TEST → DOCUMENT → REVIEW**

Apply this lifecycle to every task. Keep its records proportionate to the change. A small fix usually needs its PR explanation and relevant test evidence; it does not require a separate report in every documentation file.

## Before implementation

- Read the relevant requirements in [the approved proposal](docs/project/sources/approved-proposal.pdf) and [traceability register](docs/project/traceability.md). The approved source has 30 pages, five objectives, and 17 indicators. Verify its recorded checksum when replacing or importing a copy. Never silently substitute another proposal or change approved scope.
- Inspect current code, documentation, Git status, worktrees, main, relevant branches, Issues, PRs, reviews, and teammates' work. Distinguish live GitHub observations from saved exports and cached Git refs; record dates and exact revisions.
- Use the canonical issue and preserve its alias reference. Check every acceptance criterion, existing implementation, owner, and dependency before adding work. Do not duplicate a teammate's implementation.
- Read [project status](docs/project/status.md), relevant decisions, and existing Superpowers plans. State the intended change, verification, and incomplete dependencies. Existing user authorization remains valid; this policy does not create an extra approval step for already authorized work.
- Work on an isolated feature branch/worktree. Preserve unrelated changes. Never discard, reset, or repurpose another person's worktree.

## During implementation

- Stay within issue scope and coordinate shared interfaces. Cristian leads backend/data; Luis leads frontend/UX; Gabriel leads infrastructure, CI/CD, hosting, assigned security/moderation/testing work. Feature-specific assignments in the proposal and backlog take precedence over broad area labels; resolve discrepancies explicitly.
- Use installed Superpowers investigation, planning, testing, and review guidance where applicable. On Windows use native Git and PowerShell. Do not invoke the previously hanging Superpowers shell execution helpers or broad Git Bash process termination commands.
- Test changed behavior, including failure paths. Documentation-only work needs source, link, coverage, and consistency checks rather than unrelated application changes.
- Preserve server-side role, membership, ownership, approval/account status, and consent checks. Follow the proposal's requirement that authorization and contact-release code be written or reviewed in pairs. Issue #52/#80 additionally requires pair programming for accept/decline and contact-release logic; record the pairing participants and evidence, as well as another member's security review. Use synthetic data; never expose student information or commit secrets, populated environment files, tokens, or sensitive diagnostics.
- Record significant architectural decisions, incidents, and unresolved blockers as they occur. Hosting proposals are not spending or provisioning authorization.

## After implementation

- Run the checks appropriate to the change and inspect their outputs. Record date, revision, environment, procedure, actual result, evidence, and limitations. Never fabricate passing tests or use dependencies, checkboxes, or old build files as proof of current behavior.
- Update affected documents: setup in [environment](docs/environment.md), deployment in [deployment prerequisites](docs/deployment.md), requirement status in [traceability](docs/project/traceability.md), material progress in [development log](docs/progress/development-log.md), incidents in [bug log](docs/bugs/bug-log.md), tests in [test evidence](docs/testing/test-evidence.md), decisions in `docs/decisions/`, and report evidence in [midterm index](docs/reports/midterm/evidence-index.md).
- Major features, significant incidents, architecture/security changes, and deployment work need the corresponding evidence records. Update only documents affected by the change and link to the authoritative record instead of repeating narratives.
- Update milestone/status only when evidence justifies it. Use the engineering states defined in [status](docs/project/status.md), with a separate code-location field. A plan is not implementation; an open PR is not merged; a merge alone is not acceptance; a health response is not a performance or privacy result.
- Identify unfinished acceptance criteria and blockers. Do not close an issue until its criteria and Definition of Done are actually met.
- Prepare a focused reviewable change with issue references, verification, documentation, and remaining work. Another team member must review before integration; relevant CI must pass, and OpenAPI must reflect API changes. An automated review supports but does not replace that team review.

## Language and maintenance

Repository documentation and PR descriptions are in English. A requested coworker message may use another language. Do not translate or alter identifiers or quoted acceptance requirements. Keep the approved PDF immutable; record any advisor-approved scope amendment separately, with its approval evidence.

This policy establishes a workflow, not authorization to merge, publish, change repository controls, provision resources, or spend money. Follow the task's explicit authorization and preserve existing approvals without asking for them again.
