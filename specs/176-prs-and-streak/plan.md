# Implementation Plan: Client sees PRs and their week streak

**Branch**: `feat/177-prs-and-streak` | **Date**: 2026-11-14 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/176-prs-and-streak/spec.md`

## Summary

The session done screen shows the PRs of the session and the client's week streak; "PR today" on Progress stops marking an exercise's first log. Everything is derived on read; nothing is stored. The `progress` feature, which already holds the top set order, one row per exercise and session log, and the week streak of spec 156, gains a value `Pr` that works out the PRs of one date, and `ViewProgressUseCase.rewards(client, date)`, which answers them with the streak (#177). "PR today" uses the same `Pr` rule (#304). The session summary of `sessionlog` asks `progress` for the rewards of its date, orders the PRs by the session's exercises and answers them with the streak (#302). The session done screen shows the PR badges (#178) and the streak (#303). No migration is added.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1) backend; TypeScript, React 19 frontend

**Primary Dependencies**: Existing only: Spring MVC (`Locale` as a handler argument), Lombok; React Router, TanStack Query, react-i18next, shadcn `badge` (variants `brand` and `outline`, present). No new dependency.

**Storage**: PostgreSQL, unchanged; no migration

**Testing**: Plain unit tests for `Pr` and the services with hand-written fakes and a fixed clock; `@WebMvcTest` for the controller; Vitest + Testing Library for the session done screen. Docker must be running for the backend gate.

**Target Platform**: Browser, client screens at 375 px; same-origin `/api/v1`

**Project Type**: Web application (backend + frontend in one repository)

**Performance Goals**: The summary adds the reads `own` already does for Progress (the plan with its library, the logged exercises, the finished days): a fixed number of queries per summary, whatever the number of sessions.

**Constraints**: One PR rule for every screen (FR-002, FR-006); another client's session answers 404 like an unknown one; dates are the client's; loads in the reader's unit; reading changes nothing; every string in English and Polish; a feature does not import another feature; one lime element per view; no state by colour alone.

**Scale/Scope**: One client per request; at most 96 sessions per plan and a few hundred rows of logged exercises.

## Constitution Check

*GATE: passes before research; re-checked after design.*

| Principle | Check | Result |
|---|---|---|
| I. Hexagonal boundaries | `Pr` in `progress.domain.model`, `Rewards` and the new method in `progress.domain.input`, no framework in either. `sessionlog`'s service calls `progress` only through `ViewProgressUseCase`; the new edge `sessionlog → progress` makes no cycle (research R2), and `HexagonalArchitectureTest` runs in `check`. | Pass |
| II. One contract | #302 regenerates `schema.d.ts` in its pull request; no new error code. | Pass |
| III. Secure by default | No new endpoint. The summary stays CLIENT only and finds the log by id and client, so another client's session answers 404; the rewards are the signed-in client's own. Nothing reaches an LLM. | Pass |
| IV. Explainable plan changes | Not touched. | n/a |
| V. Tests verify behaviour | `PrTest` for the PR rule with ties, the first log and loads; the services for the rewards, "PR today" and the summary's order; the controller for the shape; the screen for what the client sees. No getter tests, no style tests (research R8). | Pass |
| VI. Simplicity | No stored rewards and no new query: the PR rule reads the rows `progress` already loads; the streak is the existing one; one new value, one port method, two new response fields. | Pass |
| VII. One design system | Badges from the shadcn `Badge` with the design system's `brand` and `outline` variants; checked in the browser in light and dark at 375 px; EN and PL strings. | Pass |

No violations, so no Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/176-prs-and-streak/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── session-summary-api.md
├── checklists/
│   └── requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
backend/src/main/java/com/kinetiq/
├── progress/domain/
│   ├── model/Pr.java                                # #177: an exercise and its top set; Pr.on(date, rows)
│   ├── input/Rewards.java                           # #177: PRs and weeks in a row
│   ├── input/ViewProgressUseCase.java               # #177: + rewards(client, date)
│   └── service/ProgressViewService.java             # #177: rewards; #304: "PR today" from Pr.on
└── sessionlog/
    ├── domain/input/SessionSummaryView.java         # #302: + prs, weeksInARow
    ├── domain/service/SessionLogService.java        # #302: rewards of the log's date, ordered by the session
    ├── infrastructure/config/SessionLogConfig.java  # #302: passes ViewProgressUseCase
    ├── infrastructure/input/SessionLogController.java    # #302: Locale on finish and summary
    ├── infrastructure/input/SessionSummaryResponse.java  # #302: + prs, weeksInARow
    └── infrastructure/input/SessionLogWebMapper.java     # #302: exercise names in the request language
backend/src/test/java/com/kinetiq/
├── progress/domain/model/PrTest.java                # #177
├── progress/domain/service/ProgressViewServiceTest.java   # #177: rewards; #304: PrToday
└── sessionlog/...                                   # #302: SessionLogServiceTest, SessionLogControllerTest
CONTEXT.md                                           # #177: PR

frontend/src/
├── lib/api/schema.d.ts                              # #302: regenerated
├── features/sessionlog/
│   ├── api/session-summary.ts                       # #178: the UI language in the query key
│   └── pages/summary-page.tsx, summary-page.test.tsx   # #178: PR badges; #303: the streak
└── locales/{en,pl}.json                             # #178, #303
```

**Structure Decision**: The PR rule and the streak stay in `progress`, next to the top set order and "In a row", so every screen uses one rule (research R1); `sessionlog` reads them through `progress`'s input port (research R2). The badges live in the session done page of `features/sessionlog`, like its figures, since no other screen shows them.

## Phases

- **#177 (backend)**: `Pr` with `Pr.on`, `Rewards`, `ViewProgressUseCase.rewards` in `ProgressViewService`, `PrTest` and the `rewards` cases of `ProgressViewServiceTest`, "PR" in `CONTEXT.md`. The spec and this plan go in its pull request. Blocked by #157 (closed).
- **#304 (backend)**: "PR today" from `Pr.on(today, …)`; `PrToday` tests for the first log and the agreement with `rewards`. Blocked by #177.
- **#302 (backend + types)**: `SessionSummaryView`, `SessionLogService` and its config, the controller's `Locale`, the response and its mapper, `SessionLogServiceTest` and `SessionLogControllerTest` updated, regenerated `schema.d.ts`. Blocked by #177.
- **#178 (frontend)**: the PR badges under the title, the skeleton's place for them, the language in the summary's query key, strings, tests. Blocked by #302 (and #160, #100, closed).
- **#303 (frontend)**: the streak badge next to the PR badges, strings with Polish plurals, tests. Blocked by #302.

Each task is its own pull request, squash merged.
