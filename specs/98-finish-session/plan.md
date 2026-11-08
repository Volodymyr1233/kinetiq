# Implementation Plan: Client finishes a session

**Branch**: `feat/98-finish-session` | **Date**: 2026-11-08 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/98-finish-session/spec.md`

## Summary

A client finishes a started session and sees a summary; every session on the Plan tab and on Today shows a state. #99 (backend) adds `SessionLog.finish`, the summary, and a read of the states of a plan week. The states live in the `sessionlog` feature, which already depends on `plan`, so `plan` never depends on `sessionlog` and the feature graph stays free of cycles (research R1). The states need the day the plan was published, which the plan does not store yet: `plans.published_on` (V19) closes that gap (R2). #100 (backend and frontend) adds the optional note (V20), the finish action in the logger with its confirmation, the session done screen at `/app/summary/:id`, the state badges and week marks on the Plan tab, and the Done state on Today. "Start session" on Today is not touched (another person's task). The summary and the states are derived on read and never stored; the only new stored data are the publish day and the note.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1) backend; TypeScript, React 19 frontend

**Primary Dependencies**: Existing only: Spring MVC, Spring Data JPA, Flyway, Lombok, MapStruct; React Router, TanStack Query, react-i18next, shadcn (`alert-dialog`, `badge`, `button`, `card`, `textarea`, `field`, `skeleton`, `alert` are present)

**Storage**: PostgreSQL. V19 adds `plans.published_on`; V20 adds `session_logs.note`. Summary and states are not stored.

**Testing**: JUnit 5 + AssertJ (aggregate, state rules, summary and services with fake ports and a fixed clock), `@WebMvcTest` (controllers), `@DataJpaTest` + Testcontainers (`*IT`, the new repository queries), Vitest + Testing Library (frontend). Docker must be running for the backend gate.

**Target Platform**: Browser, client screens at 375 px; same-origin `/api/v1`

**Project Type**: Web application (backend + frontend in one repository)

**Performance Goals**: A plan week reads at most 7 session-log headers in one query; the summary reads one session log. No target beyond the existing screens.

**Constraints**: Server time decides; another client's session answers 404 before any lock or state rule; a repeated finish is safe; `plan` does not import `sessionlog` (backend, constitution I) and `features/plan` does not import `features/sessionlog` (frontend); every string in English and Polish; no sideways scroll at 375 px and 320 px; the note never leaves the database except to its author.

**Scale/Scope**: One session log a day per client; a plan week has at most 6 sessions.

## Constitution Check

*GATE: passes before research; re-checked after design.*

| Principle | Check | Result |
|---|---|---|
| I. Hexagonal boundaries | Finish, summary and states are methods and value objects of the `sessionlog` domain; new ports are use-case interfaces in `domain.input` and one query on `SessionLogRepository`. `sessionlog` calls `plan.domain.input` (as today); `plan` gains only a stored date. Optimistic-lock failures are translated in the output adapter to a domain exception. `HexagonalArchitectureTest` (cycle rule included) runs in `check`. | Pass |
| II. One contract | New endpoints and fields come from controller records with `@NotNull`; `schema.d.ts` is regenerated in the commit of each task that changes the API (#99 and #100). Errors carry stable codes; the frontend translates `errors.session-log.*` and never shows backend text. | Pass |
| III. Secure by default | All new endpoints sit under `/api/v1/session-logs/**` or `/api/v1/session-states`, which need role CLIENT (the latter is added to the security rules and tested). The client is taken from the sign-in. Ownership is checked first, so a state, summary, finish or note never tells that another client's session exists. The note is free text, bounded to 500 characters, rendered as text, never logged and never sent to an LLM. | Pass |
| IV. Explainable plan changes | Not touched. Finished sessions and states give later rules (stories #104, #124) logged facts to read. | n/a |
| V. Tests verify behaviour | State rules at every boundary with a fixed clock; summary figures against hand-computed values (seconds, bodyweight, single set, change after finish); finish idempotence, 404-before-lock, nothing logged, locked; a page test for finish with and without missing sets, for the done screen and for the badges. No getter tests. | Pass |
| VI. Simplicity | Nothing stored that can be derived: no finished flag beside `finished_at`, no stored summary, no stored state. One new column each for the two things that cannot be derived (the publish day, the note). No new dependency. | Pass |
| VII. One design system | Built from Kinetiq tokens and shadcn components (`AlertDialog`, `Badge`, `Card`, `Textarea`); state is told by text and an icon, never by colour alone; checked in the browser, light and dark, at 375 px; EN and PL strings. | Pass |

No violations, so no Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/98-finish-session/
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
backend/src/main/java/com/kinetiq/
├── plan/
│   ├── domain/model/Plan.java                           # #99: publishedOn, set by publish
│   └── infrastructure/output/{PlanEntity,PlanMapper,PlanRepositoryImpl}.java   # #99: published_on
├── sessionlog/
│   ├── domain/
│   │   ├── model/SessionLog.java                        # #99: finish; #100: note, writeNote
│   │   ├── model/SessionState.java                      # #99: the five states and the rule
│   │   ├── model/SessionLogHeader.java                  # #99: id, date, startedAt, finishedAt
│   │   ├── model/SessionSummary.java                    # #99: figures derived from a log
│   │   ├── model/EditWindow.java                        # #99: the 24 hours, shared by log and header
│   │   ├── exception/                                   # #99: NothingLogged, NotFinished, Changed; #100: NoteInvalid
│   │   ├── input/{LogSessionUseCase,ViewSessionStatesUseCase,SessionStates}.java   # #99; #100: saveNote
│   │   ├── output/SessionLogRepository.java             # #99: headersBetween
│   │   └── service/{SessionLogService,SessionStateService}.java
│   └── infrastructure/
│       ├── config/SessionLogConfig.java                 # #99: the new service bean
│       ├── input/{SessionLogController,SessionStateController,SessionSummaryResponse,SessionStatesResponse,SaveNoteRequest}.java
│       └── output/{SessionLogEntity,JpaSessionLogRepository,SessionLogRepositoryImpl}.java   # #99: optimistic lock translated; #100: note
└── common/infrastructure/config/SecurityConfig.java     # #99: /api/v1/session-states/** needs CLIENT
backend/src/main/resources/db/
├── migration/V19__add_plan_published_on.sql             # #99
├── migration/V20__add_session_log_note.sql              # #100
└── dev-data/R__demo_plans.sql, R__demo_session_logs.sql # #99, #100: published_on and past logs for Demo Client Daily
backend/src/test/java/com/kinetiq/{plan,sessionlog}/...  # per task

frontend/src/
├── features/sessionlog/
│   ├── api/{session-summary,session-log}.ts             # #100: finish, summary, note; finishedAt on the log
│   ├── components/finish-footer.tsx                     # #100: "Finish session" / "View summary" and the confirmation
│   └── pages/{logger-page,summary-page}.tsx             # #100
├── features/plan/
│   ├── api/session-states.ts                            # #100: states of a plan week
│   ├── components/{session-state-badge,session-card,week-strip}.tsx   # #100
│   └── pages/{client-plan-page,today-page}.tsx          # #100
├── app/router.ts                                        # #100: /app/summary/:id
├── lib/api/schema.d.ts                                  # #99, #100: regenerated
└── locales/{en,pl}.json                                 # #100

CONTEXT.md                                               # #99: Session state, Session summary, Missed session
```

**Structure Decision**: Backend logic stays in `sessionlog`; `plan` only stores one more date. On the frontend the states are shown on Plan and Today, so their query and badge live in `features/plan` (a feature does not import another), and the summary and finish live in `features/sessionlog`. They share nothing but URLs.

## Phases

- **Spec commit** (first commit of the pull request): `docs: add spec for finishing a session (#98)`.
- **#99 (backend)**: V19 and `Plan.publishedOn`; `SessionLog.finish`; `SessionState` rule and `SessionSummary`; finish, summary and states endpoints; optimistic-lock translation; `CONTEXT.md`; demo `published_on`; regenerated `schema.d.ts`; tests.
- **#100 (backend and frontend)**: V20 and the note; the logger's footer and confirmation; the done screen; the badges, week marks and week line on Plan; the Done state on Today; demo past logs; strings; tests; regenerated `schema.d.ts`; browser check. Blocked by #99.

One pull request, one commit per task, merged with rebase or a merge commit (CLAUDE.md).

## Open items

None. "Start session" on Today, "Edit log" and "View log" (#256) are other people's work; the plan leaves the card slots they use free (`sessionLogId` and `state` are in the answer for them).
