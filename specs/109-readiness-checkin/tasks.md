---
description: "Tasks for story #109, Client checks in and sees readiness"
---

# Tasks: Client checks in and sees readiness

**Input**: `specs/109-readiness-checkin/` (spec.md, plan.md, research.md, data-model.md, contracts/check-in-api.md, quickstart.md), building on `specs/18-sign-in-out`

**Tests**: Required by the Definition of Done (`CLAUDE.md`) and principle V. Tests sit in the same phase as the code they cover.

**Organization**: Phases follow the GitHub tasks, not the spec's user stories, because each task is its own pull request, squash merged (`CLAUDE.md`, Workflow). #111 builds the calculation (US1), #110 the stored check-in (US2, back end), #112 the check-in questions on Today (US2, front end), #113 the readiness card (US3). **Only #111 is built on the branch `feat/111-readiness-score`**; the phases of #110, #112 and #113 stay unchecked and are carried out in their own pull requests, when their blockers are closed (#110 waits for #90, #112 for #110 and #91, #113 for #111 and #112).

**Paths**: `B` = `backend/src/main/java/com/kinetiq/readiness`, `BT` = `backend/src/test/java/com/kinetiq/readiness`, `R` = `backend/src/main/resources`, `F` = `frontend/src`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: spec user story (US1 readiness from three answers, US2 client checks in, US3 client sees readiness)

---

## Phase 1: Setup

- [X] T001 Commit `specs/109-readiness-checkin/` (spec, checklist, plan, research, data model, contract, quickstart, tasks) with the code of task #111, so the spec is reviewed in that pull request. `CONTEXT.md` needs no new term: Check-in, Readiness and Readiness band exist

---

## Phase 2: task #111, readiness score and band (US1)

**Goal**: Pure domain calculation of the score and the band, and nothing else.

**Independent Test**: `./gradlew test --tests '*ReadinessTest'` passes; sleep 2, soreness 4, stress 3 gives 33 and sleep 4, soreness 2, stress 1 gives 83.

- [X] T002 [US1] Create the enum `ReadinessBand` (`READY`, `TAKE_IT_EASIER`, `RECOVERY_DAY`) in `B/domain/model/ReadinessBand.java`
- [X] T003 [US1] Create the record `Readiness(int score)` in `B/domain/model/Readiness.java`: score refused outside 0 to 100 (FR-001); `fromCheckIn(sleep, soreness, stress)` refuses an answer outside 1 to 5 (FR-003) and scores `round(sum × 100 / 12)` with `sum = (sleep − 1) + (5 − soreness) + (5 − stress)` (FR-001, research.md); `band()`: 70 and above Ready, 45 to 69 Take it easier, below 45 Recovery day (FR-002)
- [X] T004 [US1] Test in `BT/domain/model/ReadinessTest.java`: the issue's examples and both extremes; all 125 combinations in range 0 to 100 and strictly monotonic per answer (SC-001); band edges on scores 44, 45, 69, 70 and on real answers 42, 50, 58, 67, 75; refusal of answers outside 1 to 5 and of scores outside 0 to 100
- [X] T005 [US1] Mutation check: change the soreness term to `soreness − 1`, and `>=` to `>` at 70; `ReadinessTest` MUST fail for each change; restore the code afterwards (quickstart step 4)
- [X] T006 [US1] Run `./gradlew test --tests '*ReadinessTest' --tests '*HexagonalArchitectureTest'` (quickstart steps 1 and 2); both pass and the `readiness` domain depends only on the JDK and `common`
- [X] T007 [US1] Run `./gradlew check` in `backend/` (quickstart step 3); `BUILD SUCCESSFUL`
- [X] T008 [US1] Run `/speckit-converge` for this task; commit the code, tests and spec locally as one commit `feat(backend): add readiness score and band`

**Checkpoint**: #111 is done when T001 to T008 are checked; no API, no migration and no frontend change, so no `schema.d.ts`.

---

## Phase 3: task #110, check-in model and API (US2) — later pull request

**Goal**: A client stores and reads their check-in for today; replacing it while it is changeable.

- [ ] T009 [US2] Re-plan #110 in this folder when it starts: confirm [contracts/check-in-api.md](contracts/check-in-api.md) (`PUT` and `GET /api/v1/check-ins/today`) and extend `quickstart.md`
- [ ] T010 [US2] Migration `R/db/migration/V<next free>__create_check_ins.sql`: table `check_ins` as in [data-model.md](data-model.md), named constraints, unique (`client_id`, `day`); demo data for one demo client in the local-only Flyway location
- [ ] T011 [US2] Domain in `B/domain`: aggregate `CheckIn` with typed id, use case with a command, repository port, a port asking whether the client logged a set today (a fake in tests until the logging model #93 exists), exception `check-in.locked`; day taken from an injected `Clock` in the client's time zone
- [ ] T012 [US2] Infrastructure in `B/infrastructure`: entity extending the base entity, mapper, repository adapter, controller with request validation (`errors: [{field, code}]`), wiring; endpoints open to role CLIENT only in `SecurityConfig`
- [ ] T013 [US2] Tests: domain unit tests (one check-in per day, replacement, lock, day in the client's time zone at 23:50), `@WebMvcTest` (status codes, validation, trainer refused, no session refused), `@DataJpaTest` persistence test with Testcontainers; a client never reads or changes another's check-in (`404` and a test that proves it)
- [ ] T014 [US2] Regenerate `frontend/src/lib/api/schema.d.ts` (`npm run gen:api` with the backend running) in the same pull request

---

## Phase 4: task #112, check-in on the Today screen (US2) — later pull request

- [ ] T015 [US2] Query and mutation for the check-in in `F/features/readiness/api/` (one hook per operation; errors by `code`, never backend text)
- [ ] T016 [US2] Question components in `F/features/readiness/components/` for sleep, soreness and stress (1 to 5, 48 px targets, keyboard, chosen answer shown by more than colour); saves on the third answer without a button; a skip action that stores nothing; shown on the client Today page as on the board "Today before check-in"
- [ ] T017 [US2] Texts in `F/locales/en.json` and `pl.json`; tests with Testing Library (saves on the third answer, answers in any order, skip stores nothing, locked state); check in the browser, light and dark, at 375 px, including keyboard focus

---

## Phase 5: task #113, readiness card (US3) — later pull request

- [ ] T018 [US3] Card in `F/features/readiness/components/` showing the score, the band name with an icon, the three answers and, for Take it easier, one line of advice; never red; as on the board "Today ready"
- [ ] T019 [US3] Texts in English and Polish (advice line in plain, non-alarming words); tests for each band; check in the browser, light and dark, at 375 px

---

## Dependencies and order

- #111 has no blocker and is built first (this branch). #113 and, through the API, #110 use it.
- #110 waits for #90 (Today API); #112 for #110 and #91 (Today screen); #113 for #111 and #112.
- Within #111: T002 → T003 → T004 (done), then T005 → T006 → T007 → T008. T001 is committed together with T008.
- No parallel work inside #111: one small class pair and one test class.

## Implementation strategy

MVP of this branch is US1 (#111): the calculation, fully tested. Stop after T008; the pull request follows.
