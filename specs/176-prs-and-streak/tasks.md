---
description: "Tasks for story #176, Client sees PRs and their week streak (tasks #177, #304, #302, #178, #303)"
---

# Tasks: Client sees PRs and their week streak

**Input**: `specs/176-prs-and-streak/` (spec.md, plan.md, research.md, data-model.md, contracts/session-summary-api.md, quickstart.md)

**Tests**: Required by the Definition of Done and principle V; each test sits with the code it covers.

**Organization**: Phases follow the GitHub tasks, in order: #177 (backend), #304 (backend), #302 (backend and types), #178 (frontend), #303 (frontend). One pull request per task, squash merged.

**Paths**: `B` = `backend/src/main/java/com/kinetiq`, `BT` = `backend/src/test/java/com/kinetiq`, `F` = `frontend/src`.

## Phase 1: Setup

- [X] T001 Commit `specs/176-prs-and-streak/` with the code of task #177, so the spec is reviewed in that pull request. No migration and no demo data: nothing new is stored (data-model.md)

## Phase 2: task #177, PRs and streak (US1, US2)

**Blocked by**: #157 (closed).

**Goal**: for one client and one date, the PRs of that day's session log and the week streak, from one rule that every screen uses (spec FR-001 to FR-005; research R1, R4).

- [X] T002 [US1] `B/progress/domain/model/Pr.java`: a record of `exercise` (`ExerciseId`, required) and `top` (`TopSet`, required), checked with `Objects.requireNonNull` in the compact constructor, and `static List<Pr> on(LocalDate date, List<LoggedExercise> logged)`: for each row dated `date`, a PR when the rows of the same exercise dated before `date` are at least one and the row's top set `isBetterThan` each of them; rows after `date` are ignored. Javadoc: the PR rule of `CONTEXT.md`, and that rows after `date` are not compared; the order of `logged` does not matter
- [X] T003 [US1] Tests `BT/progress/domain/model/PrTest.java` (plain unit test, rows built as in `ProgressViewServiceTest.logged`): a set heavier than every earlier one is a PR; a set equal to the earlier best is not; an earlier best reached in two sessions and only equalled is not; the same load with more repetitions is; a set with a load after sets without one is; among sets without a load, more repetitions are, and more seconds alike; the first log of an exercise is not; a heavier row after `date` does not take the PR from `date`; two exercises of one session are judged apart
- [X] T004 [US1] [US2] `B/progress/domain/input/Rewards.java`: a record of `prs` (`List<Pr>`, copied with `List.copyOf`) and `weeksInARow` (`int`); `B/progress/domain/input/ViewProgressUseCase.java` gains `Rewards rewards(AccountId client, LocalDate date)`, whose Javadoc says: the client's own PRs of their session log of `date` and their week streak, the same as `own` gives; without a published plan `PlanNotFoundException`
- [X] T005 [US1] [US2] `B/progress/domain/service/ProgressViewService.java`: `rewards` reads the plan view, the logged exercises and the finished days as `own` does and answers `new Rewards(Pr.on(date, logged), weeksInARow(all))`. The reads and `TrainingWeek.of` that both need move into one private method; what `own` answers does not change
- [X] T006 [US1] [US2] Tests in `BT/progress/domain/service/ProgressViewServiceTest.java`, a new `@Nested class Rewards`: a heavier set on a date is a PR of that date and an equal one is not; a first log is not; a later heavier row is ignored for an earlier date; the streak equals `own(FOUR_WEEKS).getWeeksInARow()` with a week of 1 of 3 done between counted weeks, and with the running week before and after its second session is done
- [X] T007 `CONTEXT.md`, section Training, the entry **PR** becomes "A set that beats every earlier set the client logged of an exercise, by the top set order; the first log of an exercise is not one." with `_Avoid_: Record, achievement` kept (spec FR-005)
- [X] T008 `./gradlew check` passes (Docker running), including `HexagonalArchitectureTest`; follow `quickstart.md` section 1; commit `feat(backend): work out the prs of a session and the week streak`

## Phase 3: task #304, no "PR today" for an exercise's first log (US1)

**Blocked by**: #177.

**Goal**: "PR today" on Progress follows the PR rule, so it agrees with the PRs of today's session (spec FR-006; research R5).

- [X] T009 [US1] `B/progress/domain/service/ProgressViewService.java`: `own` works out the exercises of `Pr.on(today, logged)` once and `exercises` marks `prToday` by membership in them, in place of the best row over all rows dated today; `best` stays for the period's best set. The Javadoc of `Progress.ExerciseProgress` in `B/progress/domain/input/Progress.java` says "whether it has a PR today" instead of "whether its best set over all logs was logged today"
- [X] T010 [US1] Tests in the `@Nested class PrToday` of `BT/progress/domain/service/ProgressViewServiceTest.java`: an exercise logged for the first time today is not marked; with three exercises logged today (a PR, an equal set, a first log), the exercises marked "PR today" are exactly those of `rewards(CLIENT, today).prs()`. The four existing tests stay as they are
- [X] T011 `./gradlew check` passes; follow `quickstart.md` section 2; commit `feat(backend): leave the first log of an exercise out of pr today`

## Phase 4: task #302, PRs and week streak in the session summary API (US1, US2, US3)

**Blocked by**: #177.

**Goal**: the summary of a finished session, after a finish and when read later, answers its PRs in the session's order and the client's week streak (spec FR-007, FR-011; contract: `contracts/session-summary-api.md`; research R2, R3, R6).

- [X] T012 [US1] [US2] `B/sessionlog/domain/input/SessionSummaryView.java`: gains `prs` (`List<ExercisePr>`, copied with `List.copyOf`) and `weeksInARow` (`int`); the nested record `ExercisePr` of `exercise` (`Exercise`) and `top` (`TopSet`), both required; the Javadoc says the PRs are in the order of the session's exercises
- [X] T013 [US1] [US2] `B/sessionlog/domain/service/SessionLogService.java`: the constructor gains `ViewProgressUseCase progress`, after `checkIns`; `view(log, client)` reads the library once, works out the summary first (so an unfinished session is refused before progress is read), asks `progress.rewards(client, log.getDate())`, orders the PRs by the first position of their exercise in `log.getEntries()`, pairs each with its `Exercise` from the library, and returns them with `weeksInARow` (research R2). `B/sessionlog/infrastructure/config/SessionLogConfig.java` passes the `ViewProgressUseCase` bean
- [X] T014 [US1] [US2] Tests in `BT/sessionlog/domain/service/SessionLogServiceTest.java`: a hand-written fake `ViewProgressUseCase` whose `own` and `ofClient` throw `UnsupportedOperationException` and whose `rewards` answers given rewards and records the date asked; the service built with it where the class builds it now. In `@Nested class Finish`: the finish answers the PRs in the session's order when the fake gives them in the other order, each with the library's exercise, and the streak; the rewards are asked for the session log's date; the summary answers the same as the finish
- [X] T015 [US1] [US2] Web, as `contracts/session-summary-api.md`: `B/sessionlog/infrastructure/input/SessionSummaryResponse.java` gains `@NotNull List<SummaryPrResponse> prs` and `@NotNull int weeksInARow`, and the nested record `SummaryPrResponse` of `@NotNull UUID exerciseId`, `@NotNull String name`, `@NotNull Measure measure`, `BigDecimal loadKg` (left out without a load, by its own `@JsonInclude(NON_NULL)`, as every nested response record has) and `@NotNull int amount`, a name no other schema uses; `finish` and `summary` in `B/sessionlog/infrastructure/input/SessionLogController.java` take the request's `Locale`, as `ProgressController` does; `toResponse(SessionSummaryView, Locale)` in `B/sessionlog/infrastructure/input/SessionLogWebMapper.java` names each exercise by `getName().in(locale)`
- [X] T016 [US1] [US3] Tests in `BT/sessionlog/infrastructure/input/SessionLogControllerTest.java`: `summaryView` gives two PRs, one with a load and one in seconds without, and a streak; `shouldAnswerTheSummary_whenTheSessionIsFinished` checks `$.prs[0]` (`exerciseId`, `name`, `measure`, `loadKg`, `amount`), that `$.prs[1]` has no `loadKg`, and `$.weeksInARow`; the summary read with `Accept-Language: pl` names the exercise in Polish; the summary of `shouldAnswerTheNoteInTheSummary` gains no PR and a streak of 0. The existing tests of `404` for another client's session and of the refused trainer and visitor stay as they are
- [X] T017 Regenerate `F/lib/api/schema.d.ts` (`npm run gen:api`, backend running)
- [X] T018 `./gradlew check` and `npm run check` pass; follow `quickstart.md` section 3; commit `feat: add prs and the week streak to the session summary`

## Phase 5: task #178, PR badge on the session done screen (US1, US3)

**Blocked by**: #302 (and #160, #100, closed).

**Goal**: the session done screen shows a badge for each PR under its title, the first lime and the others outlined, in the client's unit and language (spec FR-008, FR-009, FR-011 to FR-013; research R6, R7).

- [X] T019 [US1] Before code: open the board "Client · Session done" and the design system's Badge and section 6.8, list the PR badge's look (lime pill with ink text, "PR · {exercise} {load} kg × {repetitions}", no icon, under the title and above the figures) and record in `research.md` every difference from what T020 to T022 build; update those tasks first if one is needed. The "In a row" tile on Progress that the issue names already exists (spec 156, task #160): check it is there, build nothing for it
- [X] T020 [US1] `F/features/sessionlog/api/session-summary.ts`: the summary's query key gains the UI language from `useTranslation()`, `['session-summary', language, sessionId]`, in `useSessionSummary`, in the `setQueryData` of `useFinishSession` and in that of `useSaveNote` (research R6); the prefix `['session-summary']` that `session-log.ts` invalidates stays
- [X] T021 [US1] `F/features/sessionlog/pages/summary-page.tsx`: between the title and the figures, a wrapping row (`flex flex-wrap gap-2`) with a `Badge` per PR in the answer's order, the first `variant="brand"` and the others `variant="outline"`, each wrapping its text (`h-auto min-h-5 text-left whitespace-normal wrap-anywhere`, as `sync-badge.tsx`; research R7); its text "PR · {exercise} {set}" from `sessionlog.summary.pr` through `Trans` in one `span`, the set in the `num` class through the string's `<num>` tag, as `sign-in-page.tsx` renders its tags. The set is the load in the client's unit (`fromKg`, `plan.units.*`, one decimal at most, as the volume) and the repetitions, or the repetitions or seconds when there is no load, by `measure`. No row when there is no PR. The loading skeleton gains a line in the row's place
- [X] T022 [US1] Strings under `sessionlog.summary` in `F/locales/en.json` and `pl.json`, as the spec's Texts: `pr` ("PR · {{exercise}} <num>{{set}}</num>"), `prLoad` ("{{load}} {{unit}} × {{amount, number}}"), `prReps` with plural forms (English `_one`, `_other`; Polish `_one`, `_few`, `_many`, `_other`: "powtórzenie", "powtórzenia", "powtórzeń") and `prSeconds` ("{{count, number}} s")
- [X] T023 [US1] [US3] Tests in `F/features/sessionlog/pages/summary-page.test.tsx`: the fixture `summary()` gains `prs: []` and `weeksInARow: 0`; two PRs show their texts in the answer's order ("PR · Back squat 102.5 kg × 5", then "PR · Plank 75 s"); a load in pounds for a client in pounds; Polish texts and plural; no PR shows no badge. The trainer's screens read other answers, which carry no PR, and `client-progress-section.test.tsx` already proves the trainer's progress section shows no reward
- [X] T024 `npm run check` passes; follow `quickstart.md` section 4 at 375 px, light and dark, with the keyboard, and compare the badges with the board; commit `feat(frontend): show pr badges on the session done screen`

## Phase 6: task #303, week streak on the session done screen (US2)

**Blocked by**: #302.

**Goal**: the session done screen shows "{n} weeks in a row" next to the PR badges, the same number as the "In a row" tile, and nothing at 0 (spec FR-010, FR-012, FR-013).

- [X] T025 [US2] Before code: open the board "Client · Session done" and list the streak badge's look (outlined pill, "{n} weeks in a row", after the PR badge); record any difference from T026 in `research.md` and update T026 first if one is needed
- [X] T026 [US2] `F/features/sessionlog/pages/summary-page.tsx`: in the badge row of T021, after the PR badges, a `Badge variant="outline"` with the streak when `weeksInARow` is at least 1, its number in the `num` class through the `<num>` tag of T027, rendered by `Trans` with `count`; the row shows when there is a PR or a streak. Built after #178, which adds the row; if #178 is not merged first, this task adds the row and #178 puts its badges first in it
- [X] T027 [US2] Strings `sessionlog.summary.weeksInARow` in both locales with plural forms: English "<num>{{count, number}}</num> week in a row" / "<num>{{count, number}}</num> weeks in a row"; Polish "<num>{{count, number}}</num> tydzień z rzędu", "<num>{{count, number}}</num> tygodnie z rzędu", "<num>{{count, number}}</num> tygodni z rzędu", "<num>{{count, number}}</num> tygodnia z rzędu" (`_other`, as `progress.period.weeks_other`)
- [X] T028 [US2] Tests in `F/features/sessionlog/pages/summary-page.test.tsx`: a streak of 4 shows "4 weeks in a row" after the PR badges; 0 shows no streak; Polish "1 tydzień z rzędu", "2 tygodnie z rzędu" and "5 tygodni z rzędu"
- [X] T029 `npm run check` passes; follow `quickstart.md` section 5 at 375 px, light and dark; commit `feat(frontend): show the week streak on the session done screen`

## Phase 7: Close the story

- [X] T030 On the branch of the story's last task, before its pull request is opened, run `/speckit-converge` and turn its findings into new tasks under #176; that pull request ends with `Closes #<task>` and `Closes #176`

## Dependencies

- T001 goes with #177. #304 and #302 need #177's `Pr` and `rewards`. #178 and #303 need #302's answer and types; both change the badge row of `summary-page.tsx`, so they are built one after the other, #178 first.
- Inside #177: T002 before T003 and T004; T004 before T005; T005 before T006; T007 beside them; T008 last.
- Inside #304: T009 before T010; T011 last.
- Inside #302: T012 before T013; T013 before T014 and T015; T015 before T016; T016 before T017; T018 last.
- Inside #178: T019 first; T020 to T022 in any order; T023 after T021 and T022; T024 last.
- Inside #303: T025 first; T026 and T027 in any order; T028 after them; T029 last.

## Parallel opportunities

- T003, T004 and T007 touch different files once T002 exists.
- #304 and #302 touch different features and can be built side by side after #177.
- T020, T021 and T022 touch different files.

## Implementation strategy

Build in task order: the PR rule and the rewards first, since every screen reads them; then "PR today" and the summary API, which both rest on them; then the PR badges and the streak on the session done screen. Each task is one pull request, opened when its checks pass and the user agrees.
