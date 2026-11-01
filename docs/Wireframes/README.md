# Colegio Connect: Core Wireframes

**Milestone:** M1 - Access foundation · **Lead:** Luis · **Refs:** Table 8
**Status:** Draft, for team and advisor review

Low-fidelity wireframes of the core screen for each of the four roles. They show layout and intent only. All labels, counts, listings and organizations are illustrative placeholders, not approved requirements or real records.

## Files

| File | What it is |
| --- | --- |
| `lowfi-sketches.html` | Grayscale, hand-drawn style sketches. Gray bars are text, boxes with an X are images, `##` is a number. This is the version for review. |
| `hifi-wireframes.html` | A cleaner wireframe with sample copy and "screen intent" notes. Use it as a reference for wording. |

Open either file in a browser and use the role buttons at the top to switch screens. Only the role switcher is interactive.

## Screens

### 1. Student: Discover
- **Goal:** make opportunities easy to browse, understand, save and respond to.
- **Layout:** top bar with search; sidebar (Discover, My responses, Saved, Profile); filter row (keyword, type, field); opportunity cards; "My activity" panel.
- **Card shows:** title, source and posted date, type tag, short summary, topic tags, View details and Save actions.

### 2. University office: Overview
- **Goal:** a clear path to publish, update and track opportunities and student responses.
- **Layout:** sidebar (Overview, Opportunities, Responses, Profile); three stat tiles (published, new responses, drafts); "Recent opportunities" table with a Manage or Edit action; Create opportunity button.

### 3. External organization: Dashboard
- **Goal:** let outside partners create trusted listings, review responses and maintain their presence.
- **Layout:** sidebar (Dashboard, Opportunities, Responses, Organization profile); stat tiles (active listings, responses, profile status); active opportunities list with response counts and closing dates; "Needs attention" panel; Post an opportunity button.

### 4. Administrator / moderator: Review queue
- **Goal:** a focused queue for verification and reports, with review context before any moderation action.
- **Layout:** sidebar (Review queue, Organizations, Accounts, Reports); stat tiles (awaiting review, reported listings, pending verification); table of submissions with submitter, reason, received date, and Review / More actions.

## Shared patterns
- Same top bar on every role: logo, search, account avatar with role label.
- Same sidebar pattern, with the current section highlighted.
- Stat tiles for counts, tables for management lists, cards for browsing.
- Primary action sits at the top right of each page heading.

## Accessibility notes carried into the wireframes
- Role switcher uses buttons with `aria-pressed`, and focus moves to the page heading after switching.
- Visible focus outline on all interactive elements.
- Table headers use `scope="col"`.
- Layout collapses to one column below 760px, and the sidebar becomes a horizontal strip.

## Open questions for review
1. Should the student view include a detail screen and response form in M1, or only discovery?
2. Which "More" actions does the moderator need (suspend, request changes, escalate)?
3. Does a university office also see the student-style Discover view?
4. What does "profile status" mean for an external organization (verified, complete, or both)?

## Review checklist
- [ ] Team review
- [ ] Advisor review
- [ ] Comments folded back into the wireframes
