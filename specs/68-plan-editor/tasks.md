# Tasks: Trainer builds, edits and publishes a plan

**Input**: Design documents from `specs/68-plan-editor/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api.md, quickstart.md

**Tests**: Included. The Definition of Done and constitution V ask for them; each is a behaviour test, not a coverage test.

**Organization**: One phase per GitHub task, because each phase ends in one commit of the story's pull request (`CLAUDE.md`, one pull request per story). Tasks that serve one user story carry its label.

## Format: `- [ ] [ID] [P?] [Story?] Description`

- **[P]**: can run in parallel (different files, no dependency on unfinished tasks)
- **[US1]…[US6]**: the user story in spec.md

---

## Phase 1: Plan model and migration (#69)

**Purpose**: Storage and a domain model without rules. Blocks everything else. Commit: `feat(backend): add plan model and migration (#69)`, with the Spec Kit files and the `backend/CLAUDE.md` 3NF rule.

- [X] T001 Write `backend/src/main/resources/db/migration/V13__create_plans.sql`: `plans` (`client_id` unique `uq_plans_client`, FK `accounts`; `goal` check of the quiz's five goals; `weeks` check 4–16; `first_week` null, check it is a Monday), `plan_sessions` (`name` varchar(60) not blank; `day_of_week` check `MONDAY`…`SUNDAY`; unique `uq_plan_sessions_plan_day` on `(plan_id, day_of_week)`), `plan_session_entries` (primary key `(session_id, position)`; `exercise_id` FK and index; `sets` 1–20; `amount` 1–3600; `load_kg` numeric(4,1) null, above 0; `rpe` 5–10; `rest_seconds` 0–600)
- [X] T002 Add `PlanId`, `ExerciseEntry` and `Session` records and class `Plan` (`draft`, getters, `isPublished`, version) in `backend/src/main/java/com/kinetiq/plan/domain/model/`
- [X] T003 Add `PlanRepository` (`findByClient`, `findByClientForUpdate`, `save` returning the plan with its new version) in `backend/src/main/java/com/kinetiq/plan/domain/output/PlanRepository.java` and `PlanChangedException` (`plan.changed`, conflict) in `backend/src/main/java/com/kinetiq/plan/domain/exception/`
- [X] T004 Add `PlanEntity`, `PlanSessionEntity`, `PlanEntryEmbeddable` (`@ElementCollection` + `@OrderColumn`), `PlanMapper`, `JpaPlanRepository` and `PlanRepositoryImpl` in `backend/src/main/java/com/kinetiq/plan/infrastructure/output/`: save clears sessions, flushes, adds new ones, force-increments the version with `PESSIMISTIC_FORCE_INCREMENT`, and maps a violation of `uq_plans_client` to `PlanChangedException`
- [X] T005 Write `backend/src/test/java/com/kinetiq/plan/infrastructure/output/PlanRepositoryImplIT.java`: round trip keeps entry order and an empty load; second plan for a client is `PlanChangedException`; re-saving with the same days works; changing only an entry raises the version
- [X] T006 [P] Add two demo clients to `backend/src/main/resources/db/dev-data/R__demo_accounts.sql`, their answers to `R__demo_quiz.sql`, and in `R__demo_plans.sql` a draft and a published 16-week plan whose first week is 5 weeks back
- [X] T007 Run `./gradlew check` in `backend/`; commit with `specs/68-plan-editor/` and `backend/CLAUDE.md`

---

## Phase 2: Plan rules in the domain (#70)

**Purpose**: Every rule of FR-005 to FR-007 in the domain. Commit: `feat(backend): add plan rules to the domain (#70)`.

- [X] T008 Add `PlanRuleViolationException` carrying its code, and `PlanWeeksBeforeCurrentException` with `currentWeek` as a property, in `backend/src/main/java/com/kinetiq/plan/domain/exception/`
- [X] T009 Enforce entry rules in `ExerciseEntry`: 1–20 sets, amount 1–3600, load empty or above 0 up to 999.9 with at most one decimal, RPE 5–10, rest 0–600; in `Session`: name 1–60 after trimming, at most 30 exercises; in `Plan`: 4–16 weeks, at most 6 sessions, distinct days, sessions sorted by day
- [X] T010 Add `requireComplete()` (2–6 sessions, each with an exercise), `edit(goal, weeks, sessions, today)` (when published: complete, and refused only when the length gets shorter and falls below the current week) and `currentWeek(today)` (at least 1) to `backend/src/main/java/com/kinetiq/plan/domain/model/Plan.java`; `publish(today)` follows in #72
- [X] T011 Write `backend/src/test/java/com/kinetiq/plan/domain/model/PlanTest.java`, one test per rule at its boundaries; `currentWeek` across a year's end and before week 1; a finished plan saves with its length unchanged
- [X] T012 Run `./gradlew check`; commit

---

## Phase 3: Get and save plan API (#71)

**Goal**: A trainer reads and saves a client's plan; nobody else can. Commit: `feat: add get and save plan api (#71)`.

**Independent test**: `PlanControllerTest`, `PlanManagementServiceTest`; a save and reload through the API.

- [ ] T013 [US6] Add `timeZone` to `ClientSummary`, read by `JdbcClientDirectory`; `DRAFT` and `PUBLISHED` to `PlanState`; left-join `plans` there, goal `coalesce(plan, quiz)`, in `backend/src/main/java/com/kinetiq/client/`; extend its tests
- [ ] T014 [US1] Add `ListExercisesUseCase.library(trainer)` and `ExerciseRepository.countPlansUsing(trainer)`, a native query counting only that trainer's clients' plans; use the count in `ExerciseListingService` and in `ExerciseManagementService.update`; a new exercise stays at 0; test in `ExerciseRepositoryImplIT`, proving another trainer's plan is not counted
- [ ] T015 [US1] Add `ManagePlanUseCase`, `SavePlanCommand` and `PlanDetails` in `backend/src/main/java/com/kinetiq/plan/domain/input/`, `PlanNotFoundException`, `PlanQuizNotSubmittedException` and `PlanExerciseNotInLibraryException` in `domain/exception/`, and `PlanManagementService` (`get`, `save`) in `domain/service/`: ownership through `getClient`, version check under the row lock, quiz first on a first save, library check
- [ ] T016 [US1] Register the service in `backend/src/main/java/com/kinetiq/plan/infrastructure/config/PlanConfig.java`, and check `HexagonalArchitectureTest` covers the new feature
- [ ] T017 [US1] Write `backend/src/test/java/com/kinetiq/plan/domain/service/PlanManagementServiceTest.java` with fakes: first save creates a draft; no quiz refused; stale and absent versions refused; published save keeps its first week and is checked complete; foreign client refused and nothing stored; another trainer's exercise and an unknown one refused alike
- [ ] T018 [US6] Add `PlanController`, `SavePlanRequest` (Bean Validation with the domain's constants, `@Size(max)` on lists), `PlanResponse` and `PlanWebMapper` in `backend/src/main/java/com/kinetiq/plan/infrastructure/input/`
- [ ] T019 [US6] Write `backend/src/test/java/com/kinetiq/plan/infrastructure/input/PlanControllerTest.java`: 200 with fields; no session 401; client role 403; foreign client 404; no plan 404 `plan.not-found`; invalid values with field errors; unreadable body `request.invalid`; each `plan.*` refusal's status and code; no internal text
- [ ] T020 Regenerate `frontend/src/lib/api/schema.d.ts` with `npm run gen:api`; add `errors.plan.*` and `errors.client.*` where missing to `frontend/src/locales/{en,pl}.json`; `clients.planStates.DRAFT` and `.PUBLISHED`
- [ ] T021 Run `./gradlew check` and `npm run check`; commit

---

## Phase 4: Publish plan API (#72)

**Goal**: Publishing makes the plan visible; week 1 follows the client's time zone. Commit: `feat(backend): add publish plan api (#72)`.

**Independent test**: Publish at both sides of the Sunday-to-Monday boundary with a fixed clock; publish twice.

- [ ] T022 [US3] Add `publish(today)` to `Plan` and `PlanAlreadyPublishedException` (`plan.already-published`); add unit tests to `PlanTest.java`: complete only, first week is the Monday, twice refused
- [ ] T023 [US3] Add `publish(trainer, client, version)` to `ManagePlanUseCase` and `PlanManagementService`, using the client's time zone and `Clock`; test the time zone boundary in both directions, stale version, no plan, foreign client in `PlanManagementServiceTest.java`
- [ ] T024 [US3] Add `POST /api/v1/clients/{id}/plan/publication` and `PublishPlanRequest`; test in `PlanControllerTest.java`
- [ ] T025 [US3] Add `QuizSubmissionRepository.hasPublishedPlan` (native query) and answer `PLAN_PUBLISHED` from `QuizSubmissionService.status`; update `InMemoryQuizSubmissionRepository`, `QuizSubmissionServiceTest` and `QuizSubmissionRepositoryImplIT`
- [ ] T026 Regenerate `frontend/src/lib/api/schema.d.ts`; run `./gradlew check` and `npm run check`; commit

---

## Phase 5: Plan editor: sessions and exercises (#73)

**Goal**: The Plan tab builds a plan's structure from the library and saves it. Commit: `feat(frontend): add plan editor sessions and exercises (#73)`.

**Independent test**: `plan-page.test.tsx`: all Plan tab states; add a session and an exercise through the picker.

- [ ] T027 [US1] Move `useExercises`, `CATEGORIES`, `Exercise` and `Category` to `frontend/src/lib/exercise-library.ts`; update `frontend/src/features/exercise/`
- [ ] T028 [US1] Pass the client through `<Outlet context={client}>` in `frontend/src/features/client/pages/client-page.tsx`; route the `plan` child to `features/plan/pages/plan-page` in `frontend/src/app/router.ts`; give `PlanStateBadge` the new states
- [ ] T029 [US1] Add `usePlan`, `useSavePlan` in `frontend/src/features/plan/api/plans.ts`, invalidating `clients` and `exercises` after a save
- [ ] T030 [US1] Add `frontend/src/features/plan/plan-draft.ts` and its test: draft from a plan, new draft, add and remove session, first free day, entry defaults from an exercise with RPE 8 and empty load, request from a draft, `dirty` (untouched new plan is clean, undoing an edit clears it), totals, estimated time (FR-024)
- [ ] T031 [US1] Add `frontend/src/features/plan/pages/plan-page.tsx`: skeleton, error with "Try again", no quiz, no plan with "Build plan", editor
- [ ] T032 [US1] Add `plan-editor.tsx` (goal, length, state, week N of M or finished, session tabs with letter, name and day, first session selected, Add session with limit and focus on the new name, empty line) and `session-panel.tsx` (name, day with taken days unavailable, delete with confirmation when it holds exercises and selection and focus after it, summary, empty line) in `frontend/src/features/plan/components/`
- [ ] T033 [US1] Add `entries-table.tsx` showing rows with the unit of the exercise's measure, and `exercise-picker.tsx` (`Dialog`, search, category chips, skeleton, failure with "Try again", empty state, stays open after a pick, announces each addition, "Done", focus back on "Add exercise") in `frontend/src/features/plan/components/`
- [ ] T034 [US1] Add `plan.*` texts to `frontend/src/locales/en.json` and `pl.json` from the spec's Texts section, with Polish plural forms
- [ ] T035 [US1] Write `frontend/src/features/plan/pages/plan-page.test.tsx`: each state; picker search and filter; an added exercise's defaults; timed exercise shows seconds; first save sends the request; a seventh session cannot be added
- [ ] T036 Run `npm run check`; commit

---

## Phase 6: Plan editor: targets, save and publish (#74)

**Goal**: Targets are edited in place, work is safe, and the plan can be published. Commit: `feat(frontend): add plan editor targets, save and publish (#74)`.

**Independent test**: `plan-editor.test.tsx`: edit, switch session, reload the query; guard; publish confirmation.

- [ ] T037 [US2] Make the targets editable in `entries-table.tsx` with text kept as typed, a load in the trainer's unit that is sent unchanged when untouched and limited after conversion, accessible names per field with exercise and row number, field errors on leaving and on Save
- [ ] T038 [US2] Add Move up and Move down, unavailable at the ends, with focus moving to the other arrow when needed and a live region; add delete with confirmation and focus after it (FR-037), in `entries-table.tsx`
- [ ] T039 [US2] Add validation to `plan-draft.ts` at both levels, with a problem per field, and its tests; add `validation-summary.tsx`, focused on a failed attempt, updating as problems are fixed, with links to fields, "Add session" and "Add exercise" (FR-029); mark sessions with problems by icon and accessible text
- [ ] T040 [US5] Add `leave-guard.tsx`: `useBlocker` letting `/sign-in` through, `AlertDialog`, focus on Stay, `beforeunload`; add `frontend/src/lib/leave-guard.ts` and await it in `frontend/src/features/account/components/sign-out-button.tsx` (research R13)
- [ ] T041 [US3] Add `usePublishPlan` in `api/plans.ts`; add Save plan and Publish plan in `plan-editor.tsx`: validate, confirm, save if dirty, publish; a failed publish after a good save leaves a saved draft and shows the failure; stale publish keeps edits; loading labels; server refusals translated into the summary; focus on Save plan after publishing
- [ ] T042 [US4] Show for a published plan only Save plan, the note that saving applies from today, "week N of M", and "Finished after week M"
- [ ] T043 [US5] Show `plan.changed`, keeping the edits on screen; a failed save keeps edits and allows retrying
- [ ] T044 [US2] Add the remaining `plan.*` texts to `frontend/src/locales/{en,pl}.json`
- [ ] T045 [US5] Write `frontend/src/features/plan/components/plan-editor.test.tsx`: edits survive switching sessions; guard on another tab, Stay and Leave; sign out asks first and does not sign out on Stay; an untouched new plan leaves without a question; delete asks first and focus lands as FR-037 says; reorder is sent; invalid RPE blocks saving, focus goes to the summary, its link focuses the field; publish asks, then sends; publish with unsaved changes saves first; save succeeding and publish failing leaves a saved draft; incomplete draft saves but does not publish, showing the summary without a dialog; published plan has no Publish; `plan.changed` keeps edits
- [ ] T046 Run `npm run check`; commit

---

## Phase 7: Polish

**Purpose**: The whole story, checked for real.

- [ ] T047 Run `quickstart.md` in the built-in browser at 1280 px, 375 px and 320 px, light and dark, English and Polish; fix what it finds
- [ ] T048 Review the editor with the `ui-ux-pro-max` skill for WCAG 2.2 AA and UX; the design system stays authoritative
- [ ] T049 Run a security review of the diff: ownership, 404s, leaks in `plansUsing`, error bodies
- [ ] T050 Check `master` and open pull requests for a migration taking `V13`
- [ ] T051 Run `./gradlew check` and `npm run check`; open the story's pull request with `Closes #68`, `Closes #69` … `Closes #74`

---

## Dependencies & Execution Order

Phases run in order: #69 → #70 → #71 → #72 and #73 → #74, the blockers on GitHub. Phase 7 last. Tasks marked [P] share no files with the tasks around them. One writer works through the phases, so each commit stays whole.

## Implementation Strategy

Phases 1 to 3 and 5 are the MVP: a trainer can build and save a draft. Phases 4 and 6 complete publishing and safety. Each commit must pass the gate of every side it touches.

## Requirement coverage

| Requirement | Tasks |
|---|---|
| FR-001–004 | T001–T005 |
| FR-005–008 | T009–T011 |
| FR-009–014 | T015, T017–T019 |
| FR-015–017 | T022–T025 |
| FR-018 | T015, T019, T023 |
| FR-019–024 | T030–T033, T037–T038 |
| FR-025–028 | T030, T037–T041 |
| FR-029–031 | T031, T039, T041 |
| FR-037 | T032, T033, T038, T041 |
| FR-032 | T013, T028 |
| FR-033 | T014 |
| FR-034–035 | T020, T034, T044, T047 |
| FR-036 | T006 |
