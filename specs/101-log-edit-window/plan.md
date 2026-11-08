# Implementation Plan: Client edits a log within 24 hours

**Branch**: `feat/102-24-hour-rule` | **Date**: 2026-11-08 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/101-log-edit-window/spec.md`

## Summary

A session log can be logged into and changed for 24 hours after it started, then it is locked. #102 puts the rule in the `SessionLog` aggregate: `logSet` refuses a set at or after `startedAt` + 24 hours with `SessionLogLockedException` (`session-log.locked`, 409), except a repeat that changes nothing. The answers of the API keep their shape, so `schema.d.ts` does not change; `CONTEXT.md` gains "Locked". #103 makes the logger read-only: the page works out the moment the session locks from `startedAt`, as `set-values.ts` mirrors the set limits, turns read-only at that moment or on a `session-log.locked` refusal, and says why. #256 lets the logger open an earlier session by its date (`GET /api/v1/session-logs?date=`, `/app/session/:date`) and adds "Edit log" and "View log" to the past session cards on the Plan tab. Nothing is stored and no dependency is added.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1) backend; TypeScript, React 19 frontend

**Primary Dependencies**: Existing only: Spring MVC, Spring Data JPA, Lombok; React Router, TanStack Query, react-i18next, shadcn (`alert`, `button`, both present)

**Storage**: PostgreSQL, unchanged; no migration

**Testing**: JUnit 5 + AssertJ (aggregate and service with fake ports and a fixed clock), `@WebMvcTest` (controller), Vitest + Testing Library with fake timers (frontend). Docker must be running for the backend gate.

**Target Platform**: Browser, client screens at 375 px; same-origin `/api/v1`

**Project Type**: Web application (backend + frontend in one repository)

**Performance Goals**: None beyond the existing logger: the rule is one comparison per logged set, and the locked logger makes no extra request.

**Constraints**: The server's time decides; another client's session answers 404 before any lock; a repeated send stays safe; no control of a locked row is focusable; every string in English and Polish; no sideways scroll at 375 px and 320 px; a feature does not import another feature.

**Scale/Scope**: One session log a day per client; the rule reads only the session log's own start time.

## Constitution Check

*GATE: passes before research; re-checked after design.*

| Principle | Check | Result |
|---|---|---|
| I. Hexagonal boundaries | The rule is a method of the `SessionLog` aggregate with the time passed in; the service passes its clock's time. #256 adds one read to `LogSessionUseCase`. No new feature dependency. `HexagonalArchitectureTest` runs in `check`. | Pass |
| II. One contract | #102 adds an error code and no field, so the generated types do not change. #256 adds one endpoint, and `schema.d.ts` is regenerated in its pull request. The frontend shows the code through `errors.session-log.locked` (#103). | Pass |
| III. Secure by default | `/api/v1/session-logs/**` already needs role CLIENT; ownership is checked before the lock, so a lock never tells that another client's session exists. Nothing reaches an LLM. | Pass |
| IV. Explainable plan changes | Not touched. The lock makes the logs that later rules read stable after 24 hours. | n/a |
| V. Tests verify behaviour | Aggregate tests for the rule's cases, service tests at the boundary with a fixed clock and for 404 before the lock, a controller test for the code, page tests for the locked logger with fake timers. No getter tests. | Pass |
| VI. Simplicity | No stored lock time, no lock field in the answers, no new state: the rule follows from `startedAt` (research R1, R5). | Pass |
| VII. One design system | The locked row follows the design system's locked state (6.4) with Kinetiq tokens; `Alert` and `Button` from shadcn; checked in the browser, light and dark, at 375 px; EN and PL strings. | Pass |

No violations, so no Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/101-log-edit-window/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── session-log-api.md
├── checklists/
│   └── requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
backend/src/main/java/com/kinetiq/sessionlog/
├── domain/
│   ├── model/SessionLog.java                            # #102: the 24 hours in logSet
│   ├── exception/SessionLogLockedException.java         # #102: session-log.locked, CONFLICT
│   ├── input/LogSessionUseCase.java                     # #256: + find(client, date)
│   └── service/SessionLogService.java                   # #256: + find
└── infrastructure/input/SessionLogController.java       # #102: doc of PUT; #256: + GET ?date
backend/src/test/java/com/kinetiq/sessionlog/...         # SessionLogTest, SessionLogServiceTest, SessionLogControllerTest

frontend/src/
├── features/sessionlog/
│   ├── api/session-log.ts                               # #256: the session of a date
│   ├── components/set-row.tsx                           # #103: locked row
│   ├── pages/logger-page.tsx                            # #103: locked state, EDIT_WINDOW_MS; #256: a date from the address
│   └── pages/locked.test.tsx                            # #103
├── features/plan/components/session-card.tsx            # #256: Edit log / View log
├── lib/edit-window.ts                                   # #256: EDIT_WINDOW_MS moved up for the Plan card
├── app/router.ts                                        # #256: /app/session/:date
├── lib/api/schema.d.ts                                  # #256: regenerated
└── locales/{en,pl}.json                                 # #103: sessionlog.locked.*, errors.session-log.locked; #256: plan actions

CONTEXT.md                                               # #102: + Locked
```

**Structure Decision**: Everything stays in the `sessionlog` feature, which owns the session log and its rule, except the Plan card's action, which links to the logger's address and imports nothing from `sessionlog`.

## Phases

- **#102 (backend)**: the rule in `SessionLog.logSet`, `SessionLogLockedException`, tests at the boundary with a fixed clock, the controller's doc and test, `CONTEXT.md`.
- **#103 (frontend)**: the lock moment in the logger page, the locked rows and message, the switch at the lock moment and on a refusal, strings, tests. Blocked by #102.
- **#256 (backend and frontend)**: the read of an earlier session log by date, the logger's address with a date, the Plan card's actions, strings, tests, regenerated `schema.d.ts`. Blocked by #103 and #100.

Each task is its own pull request, squash merged.

## Open items

- #256: how the Plan card learns that a past day has a session log, and when that session started (for "Edit log" or "View log"). The session states of #100 may say it, but a Missed day can have a log too (spec, User Story 3, scenario 3). Settled when #100 is merged, before #256 starts (tasks.md, T015).
