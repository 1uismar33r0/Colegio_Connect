# ADR 0002: Role administration and current authorization

**Date:** 2026-11-10. **Status:** Proposed on `gabriel/issue-10-role-management-backend`; Cristian's backend/security review and integration are pending. **Scope:** Issues [#10](https://github.com/1uismar33r0/Colegio_Connect/issues/10)/#43 and [#9](https://github.com/1uismar33r0/Colegio_Connect/issues/9)/#41 follow-up.

## Decision

- `accountType` (`UPRM_MEMBER` or `EXTERNAL_ORGANIZATION`) identifies the account category. Assignments are separate, possibly multiple, and use V1's `STUDENT`, `INSTITUTIONAL`, `COMPANY`, and `MODERATOR` enum values. Registration does not offer privileged role selection.
- `AuthorizationPolicy.canModerate` checks an active, verified UPRM member account and a **persisted global** `MODERATOR` assignment on each sensitive request. Session authorities and `/me` role names are useful to clients but do not authorize the admin API. Existing session access stops on revocation, suspension, or loss of verification.
- A moderator may search by complete normalized email and assign or revoke an exact role/scope. Global `STUDENT` and `MODERATOR` have no organization; `INSTITUTIONAL` needs a UPRM office or department; `COMPANY` needs an approved external company and an external account. Assignment requires an active, verified account. A moderator cannot revoke their own moderator role or remove the last active, verified global moderator.
- The existing V1 unique assignment constraint rejects duplicates. V6's singleton row lock serializes role changes and bootstrap; duplicate application requests return 409. Administrator changes and their V4 audit events share one transaction. V6 represents initial bootstrap with a `SYSTEM` actor in the same audit stream, without falsely attributing the action to a human.
- `APP_BOOTSTRAP_ADMIN_EMAIL` is optional and read only on backend startup. A valid, existing, active, verified UPRM account receives the first global moderator assignment only when no eligible moderator exists. A durable completion marker prevents a later startup from restoring a revoked grant. There is no public bootstrap endpoint, and the configured email is not logged.
- Moderator access to administration does not override student consent. External companies cannot browse students; contact release still requires the separately specified student action and server-side checks.

## Consequences and review points

V6 adds a normalized-email index, bootstrap state, and explicit audit actor type. Existing audit events remain `USER` events. The administrator search is intentionally exact and bounded, not a directory browser. Cristian should review eligibility, lockout and bootstrap policy before merge. Frontend role dashboards, role selection and any organization selector remain Luis's later integration work. This ADR does not assert final acceptance of proposal indicators I6 or I8.

See [API contract](../roles/api-contract.md), [policy matrix](../security/policy-matrix.md), and [TE-13](../testing/test-evidence.md#te-13).
