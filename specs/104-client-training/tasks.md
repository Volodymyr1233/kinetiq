---
description: "Tasks for story #104, Trainer sees a client's training (tasks #105, #106, #107, #261, #108)"
---

# Tasks: Trainer sees a client's training

**Input**: `specs/104-client-training/` (spec.md, plan.md, research.md, data-model.md, contracts/client-training-api.md, quickstart.md)

**Tests**: Required by the Definition of Done and principle V; each test sits with the code it covers.

**Organization**: Phases follow the GitHub tasks, in order: #105 (backend), #106 (frontend), #107 (backend and frontend), #261 (backend and frontend), #108 (frontend, Iteration 4). One pull request per task, squash merged.

**Paths**: `B` = `backend/src/main/java/com/kinetiq`, `BT` = `backend/src/test/java/com/kinetiq`, `F` = `frontend/src`.

## Phase 1: Setup

- [X] T001 Commit `specs/104-client-training/` with the code of task #105, so the spec is reviewed in that pull request. No migration and no demo data: nothing new is stored (data-model.md)

## Phase 2: task #105, client overview API (US1, US3)

**Blocked by**: #99 and #55 (both closed).

**Goal**: a trainer reads one of their own clients' week, four recent sessions and quiz answers in one answer; another trainer's client answers `404 client.not-found` (spec FR-001 to FR-005, FR-007, FR-014, FR-015; contract: `contracts/client-training-api.md`; research R1 to R8).

- [X] T002 [US1] `B/sessionlog/domain/model/SessionLog.java`: `setsPlanned()`, the sum of the entries' sets, and `averageRpe()`, an `Optional<BigDecimal>` with the mean of the logged sets' RPE to one decimal, half up, empty while no set is logged (research R7). `B/sessionlog/domain/model/SessionSummary.java` uses both instead of its own lines; its answers do not change (`SessionSummaryTest` stays green)
- [X] T003 [US1] Tests in `BT/sessionlog/domain/model/SessionLogTest.java`: an unfinished session with RPE 8, 8 and 9 logged has an average RPE of 8.3 and its planned sets; a session with no set logged has no average RPE
- [X] T004 [US1] Repository: `findAllByClient(AccountId client)` in `B/sessionlog/domain/output/SessionLogRepository.java`, "every session log of the client with its entries and set logs, newest date first"; `findByClientIdOrderByLogDateDesc(UUID clientId)` in `B/sessionlog/infrastructure/output/JpaSessionLogRepository.java`; `SessionLogRepositoryImpl` maps it with `SessionLogMapper`; `@Fetch(FetchMode.SUBSELECT)` on `entries` and `setLogs` in `SessionLogEntity.java`, so N logs load in three queries (research R6). The two test fakes, `BT/sessionlog/domain/service/SessionLogRepositoryStub.java` and the one in `SessionLogServiceTest.java`, throw `UnsupportedOperationException` for it
- [X] T005 [US1] Test in `BT/sessionlog/infrastructure/output/SessionLogRepositoryImplIT.java`: two session logs of a client on different dates, with set logs, and one of another client: `findAllByClient` answers the client's two, newest first, each with its entries and set logs
- [X] T006 [US1] Domain input in `B/sessionlog/domain/input/`: `ClientSession`, a class with Lombok `@Getter` of `date`, `name`, `state`, `locked`, `setsLogged`, `setsPlanned`, `averageRpe` and `sessionLogId`, where `state`, `averageRpe` and `sessionLogId` may be absent: nullable fields without a Lombok getter, read through `Optional` getters, as `ClientSummary.getGoal()` does (`backend/CLAUDE.md`: `Optional` "not for fields or parameters", and a record cannot answer `Optional` for a component); `ClientOverview`, a class of the same kind with `today`, `week` (a nested record `Week(int number, LocalDate startsOn, List<ClientSession> sessions)`, absent when the plan is not published or has finished), `recentSessions` and `quizAnswers` (`QuizSubmission`, absent before the quiz), with `Optional` getters and copied lists; `ViewClientTrainingUseCase` with `ClientOverview overview(AccountId trainer, AccountId client)`, whose Javadoc says another trainer's client and an unknown one are both `ClientNotFoundException`
- [X] T007 [US1] `B/sessionlog/domain/service/ClientTrainingViewService.java` (`@RequiredArgsConstructor`, no Spring) with `ViewClientsUseCase`, `ViewQuizAnswersUseCase`, `ViewOwnPlanUseCase`, `SessionLogRepository` and a `Clock`. `overview`: `getClient(trainer, client)` first (research R2); today is the clock's instant in the summary's time zone; the session logs come from `findAllByClient`. Each log becomes a `ClientSession` with its own date and name, `SessionState.of` on its header, `locked` when its 24 hours have ended (`SessionLogHeader.open`), its set logs' count, `setsPlanned()` and `averageRpe()`. Only when the summary's plan state is `PUBLISHED` is `view(client, null)` read (research R8): the week's planned sessions take the log of their date, or, without one, a `ClientSession` from the plan session (`SessionState.of` with the publish day, `locked` false, 0 sets logged, the entries' sets planned); and each planned date of weeks 1 to the lower of the current week and the plan's weeks, on or after the publish day, before today and without a log, adds a Missed `ClientSession` (research R5). Logs and Missed sessions are sorted newest first and the first four (a constant) are the recent sessions. The quiz answers come from `answers(client)`
- [X] T008 [US1] [US3] Tests in `BT/sessionlog/domain/service/ClientTrainingViewServiceTest.java`, with hand-written fakes and a fixed clock on a Wednesday in `Europe/Warsaw`, plan sessions on Monday, Wednesday and Friday published on the Monday of week 1: in the week, a Monday session finished within its 24 hours is `DONE` and not locked, one finished more than 24 hours earlier is `DONE` and locked, a session started and not finished within its 24 hours is `IN_PROGRESS`, one past its 24 hours is `MISSED`, locked and keeps its sets, today's session never started is `TODAY`, Friday is `PLANNED`, and a day before the publish day has no state; the recent sessions are the four newest of the logs and the Missed sessions never started, newest first, a never-started one with 0 sets logged, its planned sets, no average RPE and no session log id, and a log on a day the plan no longer has keeps its own name; a plan not `PUBLISHED` reads no plan and has no week; a finished plan has no week and keeps its recent sessions; no quiz answers leaves them absent; 23:30 UTC on Tuesday is already Wednesday for the client; another trainer's client is `ClientNotFoundException` and reads no log, plan or answers
- [X] T009 [US1] Wiring: a `viewClientTrainingUseCase` bean in `B/sessionlog/infrastructure/config/SessionLogConfig.java` with `Clock.systemUTC()`
- [X] T010 [US1] [US3] Web: `B/sessionlog/infrastructure/input/ClientTrainingController.java` with `GET /api/v1/clients/{id}/overview`, the principal's name as the trainer's id (as `ClientQuizAnswersController`), a one-line comment that `/api/v1/clients/**` needs the role TRAINER and the use case checks the client is the trainer's own, and Javadoc naming `404 client.not-found`; `B/sessionlog/infrastructure/input/ClientOverviewResponse.java` with nested `OverviewWeekResponse`, `ClientSessionResponse` (with `day` from the date) and `OverviewQuizAnswersResponse` (the fields of the client feature's `QuizAnswersResponse`, likes in declared order), names that no other schema uses (research R3); `@NotNull` on every field that is always present and `@JsonInclude(NON_NULL)` where one may be absent, as `SessionStatesResponse`; the mapping in `SessionLogWebMapper.java`
- [X] T011 [US1] [US3] Tests in `BT/sessionlog/infrastructure/input/ClientTrainingControllerTest.java` (`@WebMvcTest`, the use case mocked, as `SessionStateControllerTest`): the answer's fields for a full overview, absent fields left out (`week`, `quizAnswers`, `state`, `averageRpe`, `sessionLogId`); `ClientNotFoundException` answers `404 client.not-found`; a client is refused with `403 request.not-allowed` and a visitor with `401 request.unauthenticated`, and the use case is not called
- [X] T012 `./gradlew check` passes (Docker running), including `HexagonalArchitectureTest`; `npm run gen:api` with the backend running adds the endpoint to `F/lib/api/schema.d.ts`, and `npm run check` passes; follow `quickstart.md` section 1; commit `feat(backend): add client overview api`

## Phase 3: task #106, client overview tab (US1)

**Blocked by**: #105 and #38 (closed).

**Goal**: the Overview tab of the client page shows this week, the recent sessions and the quiz answers as on the board "Trainer · client overview" at 1280 px (spec FR-006, FR-016 to FR-018; research R9).

- [X] T013 [US1] Before code: open the board "Trainer · client overview" and the design system's Card, Badge and week strip, list each control of "This week", "Recent sessions" and "Quiz answers" with its look, and record in `research.md` every difference from what T014 to T018 build; update those tasks first if one is needed
- [X] T014 [US1] Move the quiz answers list (`Answers`) from `F/features/plan/components/draft-panel.tsx` to `F/components/quiz-answers.tsx` as `QuizAnswersList`, typed by `components['schemas']['QuizAnswersResponse']`; the draft panel imports it and its texts keep their keys; `draft-panel.test.tsx` stays green
- [X] T015 [US1] Move `F/features/plan/components/session-state-badge.tsx` to `F/components/session-state-badge.tsx`, typed by the schema's session state, with a `locked` prop: a `DONE` session that is `locked` shows "Locked" with a lock icon (FR-004), never by colour alone; `F/features/plan/components/session-card.tsx` and `F/features/plan/pages/today-page.tsx` import it from there
- [X] T016 [US1] `useClientOverview(clientId)` in `F/features/sessionlog/api/client-training.ts`, reading `GET /api/v1/clients/{id}/overview` through `api`
- [X] T017 [US1] `F/features/sessionlog/components/week-days.tsx`: the seven days of `week.startsOn`, each with its short name and, as the board shows, the session's letter in week order (A, B, C, as the plan editor letters them), a mark for Done and Missed, today marked, and a dash for a rest day, each with the look of research R12; each day's session and state, or its rest day, also in words for assistive technology, and today's day with `aria-current="date"`
- [X] T018 [US1] `F/features/sessionlog/pages/client-overview-tab.tsx`: the client from `useOutletContext`, typed by `components['schemas']['ClientResponse']` (as `plan-page.tsx`); cards in a two-column grid, as research R12 lays them out: "This week" (the day strip and one row per session: "{name} · {weekday}", "RPE {value}" once a set is logged, the badge), "Recent sessions" (name, date, "{logged}/{planned} sets", "RPE {value}" when there is one, the badge; "All logs", a small outline button in the card's header, links to `logs`) and "Quiz answers" (`QuizAnswersList`); "No published plan yet." in "This week" and "Recent sessions" when the client's plan state is not `PUBLISHED`, "The plan has finished." when it is and there is no week, "No sessions yet." and "No quiz answers yet."; a skeleton while loading and, on failure, the error's message (`errorMessageKey`) with "Try again"; measured values in `num`; Kinetiq tokens only
- [X] T019 [US1] `F/app/router.ts`: the client page's index route loads the overview tab from `features/sessionlog`; `logs` and `messages` keep `ClientTabEmpty`
- [X] T020 [US1] Strings under `sessionlog.overview` and `plan.state.LOCKED` in `F/locales/en.json` and `pl.json`, as in the spec's Texts: the three card titles, "All logs", "{name} · {weekday}", the figures of the Overview's rows, the four empty states, the rest day and each day's session and state of the day strip, and a loading status ("Try again" is the feature's `sessionlog.retry`)
- [X] T021 [US1] Tests in `F/features/sessionlog/pages/client-overview-tab.test.tsx`: the week's sessions and day strip, the recent sessions with Done, Locked and Missed, the figures and the quiz answers; each empty state; "All logs" goes to the Logs tab; a failure offers "Try again"; Polish texts in Polish. `F/features/client/pages/client-page.test.tsx` answers the overview and finds it on the Overview tab, which is no longer empty; it and the Plan and Today tests stay green
- [X] T022 `npm run check` passes; follow `quickstart.md` section 2 at 1280 px, light and dark, with the keyboard, and compare each control with the board; commit `feat(frontend): add client overview tab`

## Phase 4: task #107, client logs API and tab (US2, US3)

**Blocked by**: #99 (closed), #105 (the list rule, research R5) and #106 (the moved badge).

**Goal**: the Logs tab lists every session newest first and shows a chosen one's sets against their targets, with locked and missed sessions labelled (spec FR-008 to FR-011, FR-014; research R10).

- [X] T023 [US2] Domain: `ViewClientTrainingUseCase` gains `List<ClientSession> sessions(AccountId trainer, AccountId client)`, the full list of research R5 that `overview` already builds (the private list of T007 becomes this method), and `ClientSessionLog sessionLog(AccountId trainer, AccountId client, SessionLogId id)`, a record in `B/sessionlog/domain/input/` of the session log, its `ClientSession` and the trainer's library as a `Map<ExerciseId, Exercise>` (`ListExercisesUseCase.library(trainer)`, a new dependency of the service and its bean in `SessionLogConfig.java`); an unknown or foreign log is `SessionLogNotFoundException` (`findByIdAndClient`, research R2). `B/sessionlog/domain/model/SessionLog.java` gains `minutes()`, an `Optional<Integer>` of the whole minutes from the start to the finish, at least 1, empty until finished, which `SessionSummary.of` uses instead of its own lines (research R10); `SessionSummaryTest` stays green
- [X] T024 [US2] [US3] Tests in `ClientTrainingViewServiceTest.java`, its tests grouped by method with `@Nested` (`Overview`, `Sessions`, `SessionLogOf`), as `backend/CLAUDE.md` asks once a class tests more than one, with a hand-written library fake: `sessions` lists every log and Missed session newest first; `sessionLog` answers the client's own log with its client session; another client's log under an own client's id and an unknown id are `SessionLogNotFoundException`; another trainer's client is `ClientNotFoundException`
- [X] T025 [US2] Web: `GET /api/v1/clients/{id}/session-logs` and `GET /api/v1/clients/{id}/session-logs/{sessionLogId}` in `ClientTrainingController.java`, with the response records of `contracts/client-training-api.md`: `ClientSessionsResponse` (the list, of `ClientOverviewResponse.ClientSessionResponse`) and `ClientSessionLogResponse` (the client session's fields, `startedAt`, `finishedAt` and `minutes` left out until finished, and `entries` as the logger's `SessionLogResponse.LoggedEntryResponse`, each planned set with its target and its log), with exercise names in the request's language, as `SessionLogWebMapper` does for the logger (research R10); `SessionLogTest` proves `minutes()`; tests in `ClientTrainingControllerTest.java`, grouped by endpoint with `@Nested` like the service test, for both shapes, `404 client.not-found`, `404 session-log.not-found`, `403` and `401`
- [X] T026 Regenerate `F/lib/api/schema.d.ts` (`npm run gen:api`, backend running) and run `npm run check`
- [X] T027 [US2] Before code: open the board "Trainer · client logs", list each control with its look, and record every difference in `research.md`; update T028 to T030 first if one is needed
- [X] T028 [US2] Hooks `useClientSessions(clientId)` and `useClientSessionLog(clientId, sessionLogId)` in `F/features/sessionlog/api/client-training.ts`
- [X] T029 [US2] `F/features/sessionlog/pages/client-logs-tab.tsx`, laid out as research R13 says: the list card (one button per session: name, "{date} · RPE {value}" or "{date} · No sets", the badge with `locked`; the chosen one marked with `aria-current`) beside the chosen session's card, the newest chosen at first and the choice kept in local state; the chosen session's title "{name} · {date}", the line "{logged} of {planned} sets", with " · {minutes} min" once finished and " · average RPE {value}" once a set is logged, as whole translated sentences; the table in `F/features/sessionlog/components/session-log-table.tsx`: one `Table` at `data-density="compact"` with the columns Exercise, Set, Target, the trainer's unit, Reps (or "Reps / s" when an exercise is measured in seconds, whose amounts read "45 s") and RPE (the number with a dot in `rpe-easy`, `rpe-target` or `rpe-hard` by the set's target RPE), loads in the trainer's unit (`useWeightUnit`, `fromKg`), "Not logged" for a planned set without a log; "Not started" for a Missed session without a log; and "{client} can still change these logs until {time}." while not locked, the time from `startedAt` by `locksAt` of `F/lib/edit-window.ts`, as the client's locked logger reads it (research R10); skeleton, error with "Try again"; route `logs` in `F/app/router.ts`; strings under `sessionlog.logs` in both locales
- [X] T030 [US2] Tests in `F/features/sessionlog/pages/client-logs-tab.test.tsx`: the list newest first with Done, Locked, In progress and Missed; choosing another session shows it; a chosen session's sets against targets, seconds as "45 s", "Not logged" and "Not started"; loads in pounds for a trainer in pounds; the line until when the client can change a log, and none when locked; Polish texts
- [X] T031 `./gradlew check` and `npm run check` pass; follow `quickstart.md` section 3, steps 1 to 4, at 1280 px, light and dark, with the keyboard; commit `feat: add client logs api and tab`

## Phase 5: task #261, client's note in the Logs tab (US2)

**Blocked by**: #107.

**Goal**: the chosen session shows the note its client wrote for the trainer (spec FR-012; spec 98, FR-013a).

- [X] T032 [US2] Backend: `note` in `ClientSessionLogResponse` (T025), from `SessionLog.getNote()`, left out when there is none, and its mapping in `SessionLogWebMapper.java`; a controller test for both
- [X] T033 Regenerate `F/lib/api/schema.d.ts` and run `npm run check`
- [X] T034 [US2] `F/features/sessionlog/pages/client-logs-tab.tsx`: "Note from {client}" (the client's first name, as the line until when logs can change) with the note as plain text (`wrap-anywhere whitespace-pre-wrap`, as the quiz answers' notes, never as markup), in a muted block between the chosen session's figures and its table, for a session that has one, nothing otherwise; the board has no note, so the look follows the design system; strings `sessionlog.logs.noteFrom` in both locales; tests in `client-logs-tab.test.tsx` for both cases
- [X] T035 `./gradlew check` and `npm run check` pass; follow `quickstart.md` section 3, step 5; commit `feat: show the client's note in the logs tab`

## Phase 6: task #108, progress section in the client overview (US4)

**Blocked by**: #106, #158 and #159 (story #156). Iteration 4.

**Goal**: the Overview shows the client's progress for 4, 8 or 12 weeks with the same numbers the client sees (spec FR-013).

- [ ] T036 [US4] Before code: read story #156's spec and the statistics API for trainers of #158, and record in `research.md` which figures, periods and charts the section shows and how it reads them; add the tasks that build it here, before any code of this phase
- [ ] T037 `npm run check` passes; follow `quickstart.md` section 4; commit `feat(frontend): add progress to the client overview`

## Phase 7: Close the story

- [ ] T038 On the branch of the story's last task, before its pull request is opened, run `/speckit-converge` and turn its findings into new tasks under #104; that pull request ends with `Closes #<task>` and `Closes #104`

## Dependencies

- T001 goes with #105. #106 needs #105's API. #107 needs #105's list and #106's badge. #261 needs #107's answer and tab. #108 needs #106 and story #156's #158 and #159.
- Inside #105: T002 before T003 and T007; T004 before T005 and T007; T006 before T007; T007 before T008 and T009; T009 before T010; T010 before T011; T012 last.
- Inside #106: T013 first; T014 to T017 in any order; T018 after them; T019 and T020 beside T018; T021 after T018; T022 last.
- Inside #107: T023 before T024 and T025; T025 before T026; T027 before T028 to T030; T031 last.
- Inside #261: T032 before T033; T033 before T034.

## Parallel opportunities

- T003 and T005 can be written together; T002, T004 and T006 touch different files.
- T014, T015, T016 and T017 touch different files.
- T024 and T025 can be written together once T023 exists.

## Implementation strategy

Build in task order: the overview API first, since the tab and the logs reuse its rules; then the Overview tab; then the logs and the note. The progress section waits for story #156. Each task is one pull request, opened when its checks pass and the user agrees.
