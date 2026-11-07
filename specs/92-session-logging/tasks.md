---
description: "Tasks for story #92, Client logs sets (tasks #93 to #97)"
---

# Tasks: Client logs sets

**Input**: `specs/92-session-logging/` (spec.md, plan.md, research.md, data-model.md, contracts/session-log-api.md, quickstart.md)

**Tests**: Required by the Definition of Done and principle V; each test sits with the code it covers.

**Organization**: Phases follow the GitHub tasks, in order: #93, #94 (backend), #95, #96, #97 (frontend). One pull request for the story, one commit per task, each ending in `(#<task>)`.

**Paths**: `B` = `backend/src/main/java/com/kinetiq/sessionlog`, `BT` = `backend/src/test/java/com/kinetiq/sessionlog`, `BR` = `backend/src/main/resources`, `F` = `frontend/src`.

## Phase 1: Setup

- [X] T001 Commit `specs/92-session-logging/` with the code of task #93, so the spec is reviewed in that pull request. `CONTEXT.md` needs no new term. Do not commit `backend/bin/`. Before writing the migration run `git fetch` and `gh pr list --json number,files`, and use the next free `V<n>` (V17 at the time of writing)

## Phase 2: task #93, session log and set log model (US1, US2, US5)

**Goal**: the tables and the domain model of a session log and its set logs (data-model.md).

- [X] T002 [US1] Migration `BR/db/migration/V17__create_session_logs.sql` as in `data-model.md`: `session_logs` (unique `uq_session_logs_client_date (client_id, log_date)`, `session_name varchar(60)` not blank, `finished_at is null or finished_at >= started_at`), `session_log_entries` (key `(session_log_id, position)`, `sets between 1 and 20`, `amount between 1 and 3600`, `load_kg numeric(4,1)` null and `> 0`, `rpe between 5 and 10`, `rest_seconds between 0 and 600`), `set_logs` (client-generated uuid key, `set_number >= 1`, `amount between 1 and 3600`, `rpe between 5 and 10`, `load_kg` null and `> 0`, the three `target_*` copies, `uq_set_logs_place (session_log_id, entry_position, set_number)`, foreign key `(session_log_id, entry_position)` to the entries, index on `session_log_id`); named constraints
- [X] T003 [US1] Domain model in `B/domain/model/`: `SessionLogId`, `SetLogId` (typed ids over `UUID`; `SessionLogId.random()`), `LoggedEntry` (position, exercise, sets, amount, load, rpe, rest: the copy of a planned entry), `SetValues` (load kg or none, amount, rpe: validates in the compact constructor: amount whole 1 to 3600, rpe whole 5 to 10, load none or above 0 up to 999.9 with at most one decimal), `SetLog` (id, place, `SetValues`, the target copy, loggedAt), `SessionLog` (id, client, date, name, startedAt, finishedAt none, entries, set logs; `start(...)` copies a planned session; `logSet(...)` checks the place is a planned set, is idempotent on the id, replaces on a changed value, refuses an id at another place and a place taken under another id). Exceptions in `B/domain/exception/`: `SetValuesInvalidException` (`set-values.invalid`, carries the `field`, kind `INVALID`), `SetLogOutOfPlanException` (uses `set-values.invalid` with field `place`), `SetLogConflictException` (`set-log.id-in-use` and `set-log.place-taken`, kind `CONFLICT`)
- [X] T004 [US1] Output port `B/domain/output/SessionLogRepository.java`: `findByClientAndDate`, `findByIdAndClient` (empty for another client's), `save` (returns the stored aggregate); and `existsSetLog`-style lookups only if the domain needs them. JPA adapter in `B/infrastructure/output/`: `SessionLogEntity`, `LoggedEntryEmbeddable`/entity, `SetLogEntity` extending `BaseEntity`, `JpaSessionLogRepository`, `SessionLogRepositoryImpl`, `SessionLogMapper` (hand-written like `PlanMapper`); a set log id that belongs to another session is reported to the domain as "not this session's" so the service answers 404
- [X] T005 [US1] Domain unit tests `BT/domain/model/SessionLogTest.java` and `SetValuesTest.java`: a session log copies the planned session and its entries; `SetValues` limits (0 and 3601 amount, rpe 4 and 11, load 0 and 1000 and two decimals); a set at an unplanned place is refused; the same id with the same values is one set; a changed value replaces; the same id at another place and a taken place under another id are conflicts
- [X] T006 [US1] Adapter test `BT/infrastructure/output/SessionLogRepositoryImplIT.java` (`@DataJpaTest` with Testcontainers, accounts and exercises inserted with SQL as in `PlanRepositoryImplIT`): a session log and its entries are stored and read back in order with an empty load kept empty; one session log per client and date (a second is refused by the database); one set log per place; a set log keeps its targets copy; another client's session is not found by `findByIdAndClient`; the database refuses an amount of 0 and an RPE of 4
- [X] T007 `./gradlew check` passes (Docker running), including `HexagonalArchitectureTest`; commit `feat(backend): add session log model (#93)`

## Phase 3: task #94, start session and log set API (US1, US2, US3, US5)

**Goal**: `POST /api/v1/session-logs` and `PUT /api/v1/session-logs/{id}/set-logs/{setLogId}` (contract: `contracts/session-log-api.md`).

- [ ] T008 [US1] Use case and service: `LogSessionUseCase` (`start(client)`, `logSet(LogSetCommand)`) and `LogSetCommand` in `B/domain/input/`; `SessionLogService` in `B/domain/service/`: `start` takes today from `ViewOwnPlanUseCase.today(client)` (plan feature): `REST` and `FINISHED` give `NoSessionTodayException` (`session-log.no-session-today`, kind `CONFLICT`), a draft and no plan stay `PlanNotFoundException`; the date is `TodayView.date()`; an existing session log of that date is returned, not recreated; exercise names for the answer come from the client's trainer's library (`CurrentAccountUseCase.find(client).getTrainerId()` and `ListExercisesUseCase.library`), in the request's language at the web layer. `logSet` finds the session by id and client (`SessionLogNotFoundException`, `session-log.not-found`, kind `NOT_FOUND`, also for a set id that belongs to another session), then `SessionLog.logSet`; wiring in `B/infrastructure/config/SessionLogConfig.java` with a clock
- [ ] T009 [US1] Web in `B/infrastructure/input/`: `SessionLogController` (`POST` answers `201` for a new session and `200` for an existing one; `PUT` answers `200`), `SessionLogResponse` and `LogSetRequest` records (`@NotNull` on required fields; Bean Validation for types), `SessionLogWebMapper`; the client from the sign-in only
- [ ] T010 [US5] `SecurityConfig` (`backend/src/main/java/com/kinetiq/common/infrastructure/config/`): `/api/v1/session-logs/**` for role `CLIENT` only, in the same rule as `/api/v1/quiz/**`, `/api/v1/plan/**` and `/api/v1/today/**`
- [ ] T011 [US1] Service tests `BT/domain/service/SessionLogServiceTest.java` (fake repository, fake plan and account ports, fixed clock): starting on a session day copies the session once; starting twice returns the same; a rest day and a finished plan are `no-session-today`; no plan stays not found; a plan saved after the start does not change the started copy; logging as planned, with changes, and again with the same id stores one set; a changed value replaces; a session of another client and a set id of another session are `session-log.not-found`; a date across a time-zone boundary uses the client's day
- [ ] T012 [US5] Controller test `BT/infrastructure/input/SessionLogControllerTest.java`: `201` then `200` with the response shape (no `loadKg` when empty, `log` only on logged sets); `PUT` `200`; `404 session-log.not-found`; `400 set-values.invalid` with the field and `request.invalid` for a number sent as text; `409` for `no-session-today`, `set-log.id-in-use` and `set-log.place-taken`; names in the request's language; `403` for a trainer and `401` for a visitor on both
- [ ] T013 Regenerate `frontend/src/lib/api/schema.d.ts` (`npm run gen:api` with the backend running, `SERVER_PORT` free) and run `npm run check`
- [ ] T014 `./gradlew check` passes; follow `quickstart.md` step 6 by hand; commit `feat(backend): add start session and log set api (#94)`

## Phase 4: task #95, logger screen with set rows (US1)

**Goal**: the logger at `/app/session` lists today's session with one numbered row per planned set and the targets in grey (spec FR-012, FR-013, FR-020, FR-021, FR-022).

- [ ] T015 [US1] `F/features/sessionlog/api/session-log.ts`: `useStartSession()` (a mutation or a query that calls `POST /api/v1/session-logs` once on load; the language is part of the key so names follow it) and the response types from the generated schema; a `plan.not-found` or `session-log.no-session-today` answer is a result the page handles, not a thrown error
- [ ] T016 [US1] `F/features/sessionlog/components/set-row.tsx`: a numbered row with the grey targets (load in the client's unit through `fromKg` and `useWeightUnit`, amount in repetitions or seconds as the exercise's measure says, RPE), and the logged values with a "logged" icon and text once logged; an accessible name with the exercise and set number
- [ ] T017 [US1] `F/features/sessionlog/pages/logger-page.tsx`: skeleton while loading, translated error with "Try again", the session's name, each entry in order with its rows; for no session today or no plan it goes back to `/app/today` (or `/app`); no tab bar, a back link to Today; long names wrap, no sideways scroll at 375 px and 320 px
- [ ] T018 [US1] Route `/app/session` in `F/app/router.ts`, a sibling of `/app/quiz/...` behind `requireRole('CLIENT')`, lazy; strings `sessionlog.*` in `F/locales/en.json` and `pl.json` (Polish plural forms for set counts)
- [ ] T019 [US1] Page test `F/features/sessionlog/pages/logger-page.test.tsx`: the session with every exercise and one numbered row per planned set with grey targets; seconds for a timed exercise and a load in pounds for a client in pounds; the same exercise twice gives two groups; logged sets shown as logged; loading, error with retry; a rest day or no plan goes back to Today
- [ ] T020 [US1] `npm run check` passes; check the page in a browser as `demo.client.daily` at 375 px and 320 px, light and dark; commit `feat(frontend): add session logger screen (#95)`

## Phase 5: task #96, set entry and tick-to-log (US2, US3)

**Goal**: log a set with one tick or with entered values (spec FR-014 to FR-018).

- [ ] T021 [US3] `F/features/sessionlog/set-values.ts` (+ `set-values.test.ts`): a draft of a set (load text in the client's unit, amount, rpe, and its client-generated id), `draftFromTarget`, load steps (2.5 kg, 5 lb, never below the smallest valid load), the validation of amount (whole, 1 to 3600), RPE (whole, 5 to 10) and load (empty or above 0, up to 999.9 kg after conversion), and the body of the request with the load converted to kilograms with `toKg`
- [ ] T022 [US3] `F/features/sessionlog/api/session-log.ts`: `useLogSet()` for `PUT /api/v1/session-logs/{id}/set-logs/{setLogId}`; the answer updates the cached session so the row shows as logged; a failed send keeps the draft and its id
- [ ] T023 [US2] The tick on an untouched row logs the target values with a new id (`crypto.randomUUID()`), kept in the draft until the server answers; a second tap or a retry sends the same id
- [ ] T024 [US3] `F/features/sessionlog/components/entry-panel.tsx`: opens on a selected row with the targets filled in; load stepper and typed load, repetitions or seconds field, RPE chips 5 to 10, the tick; invalid fields set `aria-invalid` and link their message; every control at least 48 px by 48 px; changing a logged set updates it; failed log keeps input and shows a translated message with a way to try again
- [ ] T025 [US3] Strings for the panel and its messages in `en.json` and `pl.json`
- [ ] T026 [US2] Page tests in `logger-page.test.tsx` and a component test for the panel: a tick on an untouched row sends the targets and the row shows logged; entered values are sent in kilograms (a load typed in pounds is converted); 0 and empty repetitions cannot be logged and say why; RPE chips offer 5 to 10 only; a changed logged set is sent with the same id and the row shows the new values; a failed send keeps the input and a retry sends the same id; a repeated tick sends one id
- [ ] T027 [US3] `npm run check` passes; check in a browser: tick, change values, validation, keyboard, 48 px targets, light and dark, 375 and 320 px, Polish and pounds; commit `feat(frontend): add set entry and tick-to-log (#96)`

## Phase 6: task #97, rest timer (US4)

**Goal**: a countdown of the target rest after each logged set (spec FR-019).

- [ ] T028 [US4] `F/features/sessionlog/components/rest-timer.tsx`: given `endsAt` and the total, shows the seconds left, updated every second from `Date.now()`, "Rest over" at zero (no sound, no vibration), and a dismiss action; announced politely to assistive technology without repeating every second; it never receives focus and never disables anything
- [ ] T029 [US4] Wire it into the logger page: a successful log with a rest above 0 starts it with that entry's rest, another log restarts it, dismissing removes it, a reload shows none; strings in `en.json` and `pl.json`
- [ ] T030 [US4] Tests: the countdown from the exercise's rest (fake timers); another log restarts it; a log works while it runs; dismiss removes it; no countdown when the rest is 0; it ends with "Rest over"
- [ ] T031 [US4] `npm run check` passes; check in a browser; commit `feat(frontend): add rest timer (#97)`

## Phase 7: Close the story

- [ ] T032 Both gates pass (`./gradlew check`, `npm run check`), the quickstart was followed end to end, the README demo table needs no change. Open the pull request (title `feat: let the client log sets (#92)`, `Closes #92`, `Closes #93`, `Closes #94`, `Closes #95`, `Closes #96`, `Closes #97`); merge with rebase so each task keeps its commit

## Dependencies

- T001 first; backend (T002 to T014) before frontend, because the frontend types are generated from it. #95 needs the Today page of #91 only for the follow-up link, so the branch does not depend on it in code.
- Inside #93: T002 and T003 before T004; T003 before T005; T004 before T006.
- Inside #94: T008 before T009 and T011; T009 before T010 and T012; T013 after T009.
- #95 before #96 before #97: each builds on the page.

## Parallel opportunities

- T005 (domain tests) and T006 (adapter test) once T003 and T004 exist.
- T011 (service test) and T012 (controller test) once T008 and T009 exist.
- T016 (row) and T018 (route and strings) beside T015.
- T021 (draft and validation module) beside T022.

## Implementation strategy

Task order, one commit per task when its checks pass. #93 first: Volodymyr's #99 (finish API) needs the model, so it is the first thing to hand over. #94 next, then the three frontend tasks. Nothing is pushed until the user agrees. Scope is the five tasks; finishing (#98), the 24-hour rule (#101), the trainer's view (#104) and the Today button (follow-up under #89) are not here.
