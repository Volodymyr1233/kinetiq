---
description: "Tasks for story #101, Client edits a log within 24 hours (tasks #102, #103, #256)"
---

# Tasks: Client edits a log within 24 hours

**Input**: `specs/101-log-edit-window/` (spec.md, plan.md, research.md, data-model.md, contracts/session-log-api.md, quickstart.md)

**Tests**: Required by the Definition of Done and principle V; each test sits with the code it covers.

**Organization**: Phases follow the GitHub tasks, in order: #102 (backend), #103 (frontend), #256 (backend and frontend). One pull request per task, squash merged.

**Paths**: `B` = `backend/src/main/java/com/kinetiq`, `BT` = `backend/src/test/java/com/kinetiq`, `F` = `frontend/src`.

## Phase 1: Setup

- [X] T001 Commit `specs/101-log-edit-window/` with the code of task #102, so the spec is reviewed in that pull request. No migration and no demo data: nothing new is stored (data-model.md)

## Phase 2: task #102, 24-hour rule (US1)

**Blocked by**: nothing open.

**Goal**: a set can be logged or changed until 24 hours after its session started, and the API refuses later changes with `409 session-log.locked` (spec FR-001 to FR-007; contract: `contracts/session-log-api.md`).

- [X] T002 [US1] `B/sessionlog/domain/exception/SessionLogLockedException.java`: code `session-log.locked`, kind `CONFLICT`, a log-only message, as `NoSessionTodayException` (research R4)
- [X] T003 [US1] `B/sessionlog/domain/model/SessionLog.java`: a constant `EDIT_WINDOW` of `Duration.ofHours(24)`; `logSet` throws `SessionLogLockedException` when `now` is not before `startedAt.plus(EDIT_WINDOW)`. The check comes after the place check, the id-at-another-place conflict and the answer to the same values, and before a change or a new set, so a repeat that changes nothing still answers as the first time (research R2, R3). `finishedAt` plays no part (FR-006). The Javadoc of `logSet` says so
- [X] T004 [US1] Aggregate tests in `BT/sessionlog/domain/model/SessionLogTest.java`: one nanosecond before the lock moment a new set and a change are stored; at the lock moment both are refused with `session-log.locked`, and the set keeps its values and its `loggedAt`; the same set with the same values at and after the lock answers as the first time; a session rebuilt with a `finishedAt` takes a change before its lock moment
- [X] T005 [US1] Service tests in `BT/sessionlog/domain/service/SessionLogServiceTest.java`, with its fixed clock at `NOW`: a session log stored with a start of `NOW` minus 24 hours plus one second takes a set; one stored with a start of exactly `NOW` minus 24 hours refuses it with `session-log.locked` and stores nothing; another client's session that is locked answers `session-log.not-found` (FR-005)
- [X] T006 [US1] Javadoc: `logSet` in `B/sessionlog/domain/input/LogSessionUseCase.java` names `SessionLogLockedException`; the `PUT` in `B/sessionlog/infrastructure/input/SessionLogController.java` lists `409` with `session-log.locked` from 24 hours after the session started
- [X] T007 [US1] Controller test addition in `BT/sessionlog/infrastructure/input/SessionLogControllerTest.java`: `SessionLogLockedException` from the use case answers `409 session-log.locked`, beside `set-log.place-taken`
- [X] T008 [US1] `CONTEXT.md`: add "Locked" under Training: a session log whose 24 hours since it started have passed, whose sets can be read but no longer logged or changed; _Avoid_: Closed, frozen
- [X] T009 `./gradlew check` passes (Docker running), including `HexagonalArchitectureTest`; `npm run gen:api` with the backend running leaves `F/lib/api/schema.d.ts` unchanged, because no request or answer changed; follow `quickstart.md` section 1; commit `feat(backend): lock a session log 24 hours after it started`

## Phase 3: task #103, locked logger state (US2)

**Blocked by**: #102.

**Goal**: a session past its 24 hours shows read-only with the reason, and nothing in its rows takes focus (spec FR-008 to FR-012, FR-016, FR-017).

- [X] T010 [US2] `F/features/sessionlog/components/set-row.tsx`: a `locked` prop. A locked row shows its values as text, without the row button, the tick, the panel or the error line, so nothing in it is focusable: a logged row keeps its values and its logged mark, and an unlogged row shows its targets with "Not logged". The row takes the design system's locked style ("locked: dashed, read-only", 6.4) with Kinetiq tokens only, and the state is told by the text, not by the line alone. An `onLocked` callback is called when a log is refused with `session-log.locked` (`problemCode` from `@/lib/api/problem`), and the row keeps what it showed before (FR-012)
- [X] T011 [US2] `F/features/sessionlog/pages/logger-page.tsx`: a constant `EDIT_WINDOW_MS` of 24 hours, with a comment that it mirrors the backend's rule and the server decides (research R5). The page is locked when `Date.now()` is at or after `Date.parse(session.startedAt)` plus `EDIT_WINDOW_MS`, or after a row reports `onLocked`. While the page is open and the session is not locked yet, a `setTimeout` to the lock moment switches it, and it is cleared on unmount. When locked: the entry panel and the rest timer are not shown, every `SetRow` gets `locked`, and an `Alert` with the title "Session locked" and the reason shows above the exercises; a polite `role="status"` region announces the switch once when it happens while the page is open. Read `npx shadcn@latest docs alert` first
- [X] T012 [US2] Strings `sessionlog.locked.title`, `sessionlog.locked.text` and `sessionlog.row.notLogged` in `F/locales/en.json` and `pl.json`, as in the spec's Texts
- [X] T013 [US2] Tests in `F/features/sessionlog/pages/locked.test.tsx`, beside `logging.test.tsx` and `rest.test.tsx`: a session started more than 24 hours ago shows "Session locked" with the reason, the logged values and "Not logged", no tick and no row button, and Tab reaches only the back link; an open session turns locked at its lock moment with fake timers, with the entry panel closed, the rest timer gone and the switch announced; a log refused with `session-log.locked` turns the page locked and the row keeps its values; the Polish texts show in Polish. The existing tests `logger-page.test.tsx`, `logging.test.tsx` and `rest.test.tsx` take their session's `startedAt` from the current time instead of a fixed date, so they do not lock as the date moves on
- [X] T014 `npm run check` passes; follow `quickstart.md` section 2 at 375 px and 320 px, light and dark, with the keyboard, and compare the locked row with the design system (6.4) and the board "Client · Session logger"; commit `feat(frontend): show a locked session read-only`

## Phase 4: task #256, a past session from the Plan tab (US3)

**Blocked by**: #103 and #100. How the Plan card learns which past days have a session log, and when each started, is settled by T015 after #100 is merged (plan.md, Open items).

**Goal**: the logger opens an earlier session by its date, and the past session cards on the Plan tab offer "Edit log" and "View log" (spec FR-013 to FR-017; contract: `contracts/session-log-api.md`).

- [ ] T015 [US3] Read how #100 shows the session states on the Plan tab and what its answer holds. Record in `research.md` how the card learns that a past day has a session log and when it started, remove the open item from `plan.md`, and update T021 to match, before any code of this phase
- [ ] T016 [US3] `B/sessionlog/domain/input/LogSessionUseCase.java`: `find(AccountId client, LocalDate date)`, returning `StartedSession` with `created` false; `B/sessionlog/domain/service/SessionLogService.java`: the client's session log of that date with the trainer's library, or `SessionLogNotFoundException`; nothing is started (research R7)
- [ ] T017 [US3] Service tests in `BT/sessionlog/domain/service/SessionLogServiceTest.java`: the client's own session log of an earlier date is returned and today's session is not started; a date without a session log and another client's session log both answer `session-log.not-found`
- [ ] T018 [US3] `GET /api/v1/session-logs?date=` in `B/sessionlog/infrastructure/input/SessionLogController.java`: a required ISO date, the answer through `SessionLogWebMapper` in the request's language; controller tests in `BT/sessionlog/infrastructure/input/SessionLogControllerTest.java`: `200` in the shape of starting a session, `404 session-log.not-found`, `400 request.invalid` for a missing or malformed date, `403 request.not-allowed` for a trainer and `401` for a visitor
- [ ] T019 Regenerate `F/lib/api/schema.d.ts` (`npm run gen:api`, backend running) and run `npm run check`
- [ ] T020 [US3] Logger by date: `useSession(date?)` in `F/features/sessionlog/api/session-log.ts` reads `GET` by date when given one and starts with `POST` as before otherwise, with the date in the query key that `useLogSet` writes to; route `/app/session/:date` in `F/app/router.ts` beside `/app/session`, lazy, behind `requireRole('CLIENT')`; `F/features/sessionlog/pages/logger-page.tsx` takes the date from the address, goes to `/app/plan` on `session-log.not-found` for a date, and its back link returns to the address in the router state when there is one, otherwise to Today. `EDIT_WINDOW_MS` moves to `F/lib/edit-window.ts`, because the Plan card needs it too and a feature does not import another feature
- [ ] T021 [US3] `F/features/plan/components/session-card.tsx` (with `week-strip.tsx`, which knows each day's date): the opened card of a past session with a session log shows a `Button` with a `Link` to `/app/session/<date>` that carries the Plan's address in the router state: "Edit log" before the session's lock moment, "View log" from it on; nothing for a past day without a session log or a day still to come; a 48 px target; nothing imported from `features/sessionlog`. The data comes as T015 settled
- [ ] T022 [US3] Strings for "Edit log" and "View log" under `plan.client` in `F/locales/en.json` and `pl.json`, as in the spec's Texts
- [ ] T023 [US3] Tests: in `F/features/plan/pages/client-plan-page.test.tsx`, a past session with a session log within its 24 hours offers "Edit log" linking to its date, after them "View log", and a past day without a log and a day to come offer neither; in a logger test, a date in the address reads that session and starts nothing, a session past its 24 hours shows locked, `session-log.not-found` goes to the Plan tab, and the back link returns to the Plan's week
- [ ] T024 `./gradlew check` and `npm run check` pass; follow `quickstart.md` section 3 at 375 px and 320 px, light and dark, with the keyboard; commit `feat: open a past session from the plan`

## Phase 5: Close the story

- [ ] T025 On the branch of task #256, before its pull request is opened, run `/speckit-converge` and turn its findings into new tasks under #101; the pull request of #256 ends with `Closes #256` and `Closes #101`

## Dependencies

- T001 goes with #102. #103 needs the code `session-log.locked` of #102. #256 needs #103's locked logger and the session states of #100.
- Inside #102: T002 before T003; T003 before T004 and T005; T006 to T008 at any point; T009 last.
- Inside #103: T010 before T011; T012 beside them; T013 after T011.
- Inside #256: T015 first; T016 before T017 and T018; T018 before T019; T019 before T020; T020 before T021; T022 beside them; T023 after T021.

## Parallel opportunities

- T004 and T005 can be written together once T003 exists; T006, T007 and T008 touch different files.
- T012 can be written beside T010 and T011.
- T017 and T018 can be written together once T016 exists; T022 beside T020 and T021.

## Implementation strategy

Build in task order: the rule and its refusal first, since every later part relies on the server deciding; then the locked logger; then the way to an earlier session. Each task is one pull request, opened when its checks pass and the user agrees.
