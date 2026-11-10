---
description: "Tasks for story #156, Client sees their progress (tasks #157, #158, #159, #160, #161)"
---

# Tasks: Client sees their progress

**Input**: `specs/156-client-progress/` (spec.md, plan.md, research.md, data-model.md, contracts/progress-api.md, quickstart.md)

**Tests**: Required by the Definition of Done and principle V; each test sits with the code it covers.

**Organization**: Phases follow the GitHub tasks, in order: #157 (backend), #158 (backend), #159 (frontend), #160 (frontend), #161 (frontend). One pull request per task, squash merged.

**Paths**: `B` = `backend/src/main/java/com/kinetiq`, `BT` = `backend/src/test/java/com/kinetiq`, `F` = `frontend/src`.

## Phase 1: Setup

- [X] T001 Commit `specs/156-client-progress/` with the code of task #157, so the spec is reviewed in that pull request. No migration and no demo data: nothing new is stored (data-model.md)

## Phase 2: task #157, statistics queries over logs (US1, US2)

**Blocked by**: #99 (closed).

**Goal**: for one client, the sessions planned and done, the mean RPE and each exercise's top set and mean RPE, week by week, read from the logs and proven against PostgreSQL with known logs (spec FR-002 to FR-004; research R1 to R4).

- [X] T002 [US1] `B/progress/domain/model/TopSet.java`: a record of `loadKg` (`BigDecimal`, null for a set without a load, as `planchange`'s `LoggedSet` documents), `amount` (at least 1) and `rpe` (5 to 10), validated in the compact constructor, and `isBetterThan(TopSet other)`: a load beats no load, then the higher load, then the higher amount; an equal set is not better (research R3)
- [X] T003 [US1] `B/progress/domain/model/LoggedExercise.java`: a record of `date` (`LocalDate`), `exercise` (`ExerciseId`), `top` (`TopSet`), `rpeSum` and `sets` (at least 1), as data-model.md describes
- [X] T004 [US1] `B/progress/domain/output/TrainingLog.java`: `List<LoggedExercise> loggedExercises(AccountId client)`, "one per exercise in each session log of the client that has a logged set of it, oldest date first", and `Set<LocalDate> finishedDays(AccountId client)`, "the dates of the client's finished session logs"
- [X] T005 [US1] `B/progress/infrastructure/output/JdbcTrainingLog.java` (`@Repository`, `JdbcTemplate`, as `JdbcTrainingHistory`), with a one-line comment that it is a read model over the log tables of another feature: `loggedExercises` joins `set_logs`, `session_logs` and `session_log_entries` (the exercise is on the session's copy of the entry) for the one client, keeps one row per session log and exercise with `distinct on (l.id, e.exercise_id)` ordered by `s.load_kg desc nulls last, s.amount desc, s.entry_position, s.set_number`, and takes `sum(s.rpe)` and `count(*)` over the same partition as window functions; `finishedDays` reads `log_date` of the client's session logs with `finished_at is not null`
- [X] T006 [US1] Test `BT/progress/infrastructure/output/JdbcTrainingLogIT.java` (`@DataJpaTest`, `@Import({TestcontainersConfiguration.class, JdbcTrainingLog.class})`, accounts, plan, session logs, entries and set logs inserted with SQL as `JdbcTrainingHistoryIT` does): a set with a load is the top set over a set with more repetitions and no load; of two loads the higher; of two equal loads the one with more repetitions; of two equal sets the one earlier in the session; an exercise in two entries of one session gives one row whose RPE sum and count cover both; a seconds-based exercise's amount is its seconds; a session started without a logged set gives no row; `finishedDays` holds finished session logs only; another client's logs are never read
- [X] T007 [US1] `B/progress/domain/model/TrainingWeek.java`: a record of `number`, `monday`, `planned`, `done`, `running`, `rpeSum`, `sets` and `exercises` (a `Map<ExerciseId, ExerciseWeek>`, copied; `ExerciseWeek` a nested record of `top`, `rpeSum` and `sets`), with `averageRpe()`, an `Optional<BigDecimal>` of `rpeSum / sets` to one decimal, half up, as `SessionLog.averageRpe()`, empty without sets. `static List<TrainingWeek> of(Plan plan, int lastWeek, boolean lastRunning, List<LoggedExercise> logged, Set<LocalDate> finishedDays)` builds weeks 1 to `lastWeek` from the plan's first week: planned are the plan's sessions on days of the week on or after `getPublishedOn()`; done are those days in `finishedDays`; a row counts in the week of its date; a week's top set per exercise is the best of its rows' (`isBetterThan`, the earlier date kept on a tie); only the last week can be `running` (research R4)
- [X] T008 [US1] Tests `BT/progress/domain/model/TrainingWeekTest.java`: a plan with sessions on Monday, Wednesday and Friday published on the Wednesday of week 1 plans 2 sessions in week 1 and 3 later; a finished session log on a planned day is done, an unfinished one is not, and a finished one on a day the plan has no session adds nothing to done; the mean RPE counts every row of the week, finished or not, to one decimal; a week without rows has no mean; a week's top set is the best of its sessions', and of two equal ones the earlier; a row on a Sunday belongs to that week and one on the next Monday to the next; only the last week is `running`, and only when asked
- [X] T009 [US1] `CONTEXT.md`, section Training: "**Top set**: The best set of an exercise in a session or a week: a set with a load before one without, then the highest load, then the most repetitions or seconds." with `_Avoid_: Best set, max`
- [X] T010 `./gradlew check` passes (Docker running), including `HexagonalArchitectureTest`; follow `quickstart.md` section 1; commit `feat(backend): add statistics queries over logs`

## Phase 3: task #158, statistics API (US1, US2, US3)

**Blocked by**: #157.

**Goal**: a client reads their own progress for 4, 8 or 12 weeks and a trainer reads the same for their own client; another trainer's client answers `404 client.not-found` (spec FR-001, FR-005 to FR-010, FR-022, FR-023; contract: `contracts/progress-api.md`; research R4 to R9).

- [ ] T011 [US1] `B/progress/domain/model/Period.java`, a record of `weeks` that accepts 4, 8 and 12 and otherwise throws `B/progress/domain/exception/PeriodInvalidException.java` (`DomainException`, code `progress.period-invalid`, kind `INVALID`, as `PlanWeekOutOfRangeException`)
- [ ] T012 [US1] Domain input in `B/progress/domain/input/`: `Progress`, a class with Lombok `@Getter` of `goal`, `week`, `finished`, `weeks` (the period's `TrainingWeek`s, oldest first, copied), `sessionsDone`, `sessionsPlanned`, `weeksInARow` and `exercises`, and nullable `averageRpe`, `sessionsDoneChange` and `averageRpeChange` read through `Optional` getters, as `ClientOverview` does; its nested `ExerciseProgress` of the same kind: `exercise` (the library's `Exercise`, for name and measure), `best` (`TopSet`), `prToday`, `weeks` (number, top set and mean RPE of each period week it was logged in), `recent` (date and top set of its 3 newest rows) and a nullable `topSetChange` (a record of a nullable `loadKg` and a nullable `amount`, exactly one present); `ViewProgressUseCase` with `Progress own(AccountId client, Period period)` and `Progress ofClient(AccountId trainer, AccountId client, Period period)`, whose Javadoc names `PlanNotFoundException` and, for `ofClient`, `ClientNotFoundException` for another trainer's client and an unknown one alike
- [ ] T013 [US1] [US2] [US3] `B/progress/domain/service/ProgressViewService.java` (`@RequiredArgsConstructor`, no Spring) with `ViewClientsUseCase`, `ViewOwnPlanUseCase`, `CurrentAccountUseCase`, `TrainingLog` and a `Clock`. `ofClient` calls `getClient(trainer, client)` first and then does what `own` does. `own`: `view(client, null)` (no published plan is `PlanNotFoundException`); the last week is `currentWeek`, or `plan.getWeeks()` when `finished`, and it is running unless finished; `TrainingWeek.of` over the rows of `loggedExercises` and `finishedDays`; the period is its last `period.weeks()` weeks. Sums of planned and done; the period's mean RPE from the summed `rpeSum` and `sets`. Comparisons by research R5: the last week of the period that is not running against the week before it, both in the period; sessions done by count, mean RPE by the difference of the one-decimal means, an exercise's top set in kilograms when both have a load, in amount when neither has, none otherwise or when either week lacks a value. Weeks in a row by research R6 over all weeks. Exercises logged in the period, ordered by the number of sessions (rows) in the period, then by the newest date; each with its best top set in the period, `prToday` when its best row over all rows (the earlier date kept on a tie) is dated the client's today (the clock in `accounts.find(client).getTimeZone()`), its period weeks and its 3 newest rows (research R7)
- [ ] T014 [US1] [US2] [US3] Tests `BT/progress/domain/service/ProgressViewServiceTest.java`, with hand-written fakes and a fixed clock on a Wednesday in `Europe/Warsaw`, grouped with `@Nested` by subject: the period (8 weeks asked in plan week 3 gives weeks 1 to 3; 4 weeks in week 10 gives weeks 7 to 10, the last running; a finished 6-week plan gives its last 4 weeks, none running); the sums and mean over the period; comparisons (week 9 against week 8 while week 10 runs; a finished plan compares its last two weeks; none in plan week 1; none for RPE when one week has no set; an exercise's change in kilograms, in repetitions, and none for one loaded and one unloaded week); weeks in a row (3 full weeks; a first week shortened by the publish day with its 1 session done counts; a week with 1 of 3 done ends it; the running week adds one only once it has 2 done); "PR today" (a heavier set today; an equal set today is not one; a heavier set yesterday is not "today"; 23:30 UTC on Tuesday is already Wednesday for the client); the exercise order and its tie; the 3 newest rows include ones before the period; `ofClient` for another trainer's client is `ClientNotFoundException` and reads no plan and no log; no published plan is `PlanNotFoundException`
- [ ] T015 [US1] Wiring: `B/progress/infrastructure/config/ProgressConfig.java` (`@Configuration`) with a `viewProgressUseCase` bean and `Clock.systemUTC()`, as `SessionLogConfig`
- [ ] T016 [US1] [US3] Web: `B/progress/infrastructure/input/ProgressController.java` with `GET /api/v1/progress` (the principal's name as the client's id) and `GET /api/v1/clients/{id}/progress` (the principal's name as the trainer's id), both with a required `int weeks` turned into `Period`, the request's `Locale`, a one-line comment on which role each path needs and that the use case checks ownership, and Javadoc naming their errors; `B/progress/infrastructure/input/ProgressResponse.java` with the fields of `contracts/progress-api.md` and nested records whose names no other schema uses (`ProgressWeekResponse`, `ExerciseProgressResponse`, `ExerciseWeekResponse`, `RecentSetResponse`, `TopSetResponse`, `TopSetChangeResponse`), `@NotNull` on every field that is always present and `@JsonInclude(NON_NULL)` where one may be absent, as `ClientOverviewResponse`; `B/progress/infrastructure/input/ProgressWebMapper.java` (`@Component`), exercise names by `getName().in(locale)` (research R9)
- [ ] T017 [US1] `B/common/infrastructure/config/SecurityConfig.java`: `/api/v1/progress/**` added to the paths that need role CLIENT
- [ ] T018 [US1] [US3] Tests `BT/progress/infrastructure/input/ProgressControllerTest.java` (`@WebMvcTest`, the use case mocked, as `ClientTrainingControllerTest`), grouped by endpoint with `@Nested`: the answer's fields, and absent ones left out (`averageRpe`, both changes, a week's `averageRpe`, `loadKg`); `weeks=5` answers `400 progress.period-invalid`; `PlanNotFoundException` answers `404 plan.not-found` and `ClientNotFoundException` `404 client.not-found`; a trainer on the client's path and a client on the trainer's are refused with `403 request.not-allowed`, a visitor with `401 request.unauthenticated`, and the use case is not called
- [ ] T019 Regenerate `F/lib/api/schema.d.ts` (`npm run gen:api`, backend running); `errors.progress.period-invalid` in `F/locales/en.json` and `pl.json` ("Choose 4, 8 or 12 weeks." / "Wybierz 4, 8 lub 12 tygodni."); `CONTEXT.md`, section Training: "**Week streak**: The weeks in a row in which a client did at least 2 sessions, or every session of a week that had fewer." with `_Avoid_: Chain, combo`
- [ ] T020 `./gradlew check` and `npm run check` pass; follow `quickstart.md` section 2; commit `feat: add progress statistics api`

## Phase 4: task #159, shadcn chart with the Kinetiq series tokens (US1)

**Blocked by**: nothing.

**Goal**: every chart has the shadcn chart component and the five series of the design system's section 3.7, right in light and dark (spec FR-011; research R10).

- [ ] T021 [US1] Before code: `npx shadcn@latest docs chart`; then `npx shadcn@latest add chart`, which adds `F/components/ui/chart.tsx` and Recharts to `frontend/package.json` and `package-lock.json`; read the files it adds
- [ ] T022 [US1] Fit `F/components/ui/chart.tsx` to section 3.7 where its defaults differ: the tooltip on `bg-popover` with `text-popover-foreground`, grid lines on `stroke-border`, axis text `text-xs` in `fill-muted-foreground`; no hex values
- [ ] T023 [US1] `F/lib/chart-series.ts`: the five series by meaning, each with its colour `var(--chart-n)`, line width and dash: `completion` `--chart-1` solid 2.5, `rpe` `--chart-2` solid 2.5, `load` `--chart-3` solid 2, `wearables` `--chart-4` dotted, `previous` `--chart-5` dashed `4 4`, with a doc comment naming section 3.7
- [ ] T024 `npm run check` passes; follow `quickstart.md` section 3 on a sample chart that is not committed, in light and dark; commit `feat(frontend): add chart with the kinetiq series`

## Phase 5: task #160, Progress screen: tiles and sessions chart (US1)

**Blocked by**: #158 and #159.

**Goal**: the Progress tab shows the period switch, the four tiles and the sessions chart as on the board "Client · Progress" at 375 px (spec FR-012 to FR-017, FR-021, FR-024 to FR-026; research R11, R12).

- [ ] T025 [US1] Before code: open the board "Client · Progress" and the design system's Card (`metric`), Badge, ToggleGroup and section 3.7, list each control of the header, the switch, the tiles and the sessions chart with its look, and record in `research.md` every difference from what T026 to T032 build; update those tasks first if one is needed
- [ ] T026 [US1] `useProgress(weeks)` in `F/features/progress/api/progress.ts`, reading `GET /api/v1/progress` through `api`, its key `['progress', weeks]`
- [ ] T027 [US1] `F/features/progress/findings.ts` with tests in `findings.test.ts`: `rpePlace(value)` is `within` from 7.0 to 8.0, `above` over 8.0 and `below` under 7.0, on the one-decimal value; `weeksAtTarget(weeks)` counts the weeks that are not running, have planned sessions and done at least 70% of them, out of those weeks (3 of 3 and 7 of 10 count, 2 of 3 does not)
- [ ] T028 [US1] `npx shadcn@latest docs progress`, then `npx shadcn@latest add progress` for the sessions bar (`F/components/ui/progress.tsx`); read the file it adds
- [ ] T029 [US1] `F/features/progress/components/chart-card.tsx`: a `Card` with the chart's title (the finding), its subtitle and a "Show as table" / "Show as chart" button (`aria-pressed`), showing either its chart or its table, the choice in local state
- [ ] T030 [US1] `F/features/progress/components/progress-tiles.tsx`: four `metric` cards in two columns. "Sessions done": "{done} of {planned}", the `Progress` bar with an accessible name, and the change. "Average RPE": the mean, its place against 7–8 in words with an arrow (`rpePlace`, coloured `text-rpe-easy`, `text-rpe-target` or `text-rpe-hard`, never red or warning, as `frontend/CLAUDE.md` says of high RPE), and the change. The best set: the first exercise's name, its best in the reader's unit (`useWeightUnit`, `fromKg`) or in repetitions or "45 s", the change in the same measure, and a "PR today" `Badge` when `prToday`. "In a row": the weeks with the rule "At least 2 sessions each week". A change shows "▲ {change} week on week", "▼ …" or "Same as the week before", the arrow hidden from assistive technology and the direction in words; a tile without a value shows a dash and its empty line; every measured value in `num`
- [ ] T031 [US1] `F/features/progress/components/sessions-chart.tsx` in a `ChartCard`: one bar per period week of done as a share of planned, `completion` from `chart-series.ts`, 4 px top radius, the running week lighter and labelled as running, a 70% reference line labelled "70%", the week numbers below; the title from `weeksAtTarget` (spec, Texts), the subtitle "Sessions done by week", "Week {n} has {k} sessions left." under it while a week runs; the table: Week · Done · Planned; an `aria-label` that states the title and the running week
- [ ] T032 [US1] `F/features/progress/pages/progress-page.tsx`: "{goal} · Week {n}" (the goal's label `quiz.goal.options.<goal>.title`) or "{goal} · Plan finished", the title "Progress", the period `ToggleGroup` (4, 8, 12 weeks; 8 at first; local state; 48 px touch targets, `h-12` items as the client's settings toggles in `features/account/components/setting-toggle-group.tsx` have), the tiles and the sessions chart; while loading a skeleton of the layout; `404 plan.not-found` shows "Your progress shows here once your trainer publishes your plan."; nothing logged in the period shows "Log a session to see your progress here." in place of the charts; any other failure the error's message (`errorMessageKey`) with "Try again". `F/app/router.ts`: `progress` loads this page lazily; `clientPlaceholder` and `ClientPlaceholder` in `F/app/client-area.tsx`, used only by it, are removed with any string only they use
- [ ] T033 [US1] Strings under `progress` in `F/locales/en.json` and `pl.json`, as in the spec's Texts, with Polish plural forms for weeks and sessions: the header, the switch, the four tiles, the change lines, the RPE places, "PR today", the sessions chart's title, subtitle, running line, table headers and description, "Show as table" / "Show as chart", the two empty states, a loading status and "Try again"
- [ ] T034 [US1] Tests `F/features/progress/pages/progress-page.test.tsx`: the header, the tiles and the bars for an answer; switching to 4 weeks asks for 4; the running week's line; the comparison's three forms; "PR today"; loads in pounds for a client in pounds; the table toggle; no plan, nothing logged and a failure with "Try again"; Polish texts. `F/app/client-area.test.tsx` answers the progress request and still finds the Progress tab's heading
- [ ] T035 `npm run check` passes; follow `quickstart.md` section 4 at 375 px, light and dark, with the keyboard, and compare each control with the board; commit `feat(frontend): add the progress tab`

## Phase 6: task #161, progress by exercise (US2)

**Blocked by**: #160.

**Goal**: "By exercise" shows the chosen exercise's top set and RPE week by week and its recent sets, with a chart description for assistive technology (spec FR-018 to FR-021; research R12).

- [ ] T036 [US2] Before code: open the board's "By exercise" and the design system's section 3.7 (target band, direct labels), list each control with its look, and record in `research.md` every difference from what T037 and T038 build; update them first if one is needed
- [ ] T037 [US2] `topSetTrend(exercise)` in `F/features/progress/findings.ts`: the first and the last period week with a top set, in load when any of them has a load (weeks without one left out), otherwise in amount: `rose` or `fell` by the difference, or `held`; tests in `findings.test.ts` for each and for a single week
- [ ] T038 [US2] `F/features/progress/components/exercise-progress.tsx`: a single-choice `ToggleGroup` of the exercises in the answer's order, the first chosen (local state), with 48 px touch targets and the chosen one marked by more than colour; a `ChartCard` with a line chart of the top set per week (`load` series, in the reader's unit or in amount) and the mean RPE per week (`rpe` series, its own axis from 5 to 10, the 7–8 target band as a 10% tint labelled at the right edge), each series labelled at its line's end, weeks without the exercise left without a point; the title from `topSetTrend` (spec, Texts), the subtitle naming both axes; the table: Week · Top set · RPE; an `aria-label` that states the trend; under it the recent sessions as a table: Date · Top set · RPE, seconds as "45 s". `progress-page.tsx` shows it below the sessions chart when the period has exercises
- [ ] T039 [US2] Strings under `progress.exercise` in both locales: the section title, the chart's titles, subtitle, table headers and description, and the recent sets' headers
- [ ] T040 [US2] Tests `F/features/progress/components/exercise-progress.test.tsx`: the first exercise chosen and another chosen on a click; the rose, fell and held titles; the recent sessions with a load and in seconds; the table toggle; Polish texts
- [ ] T041 `npm run check` passes; follow `quickstart.md` section 5 at 375 px, light and dark, with the keyboard and a screen reader's view of the chart's description; commit `feat(frontend): add progress by exercise`

## Phase 7: Close the story

- [ ] T042 On the branch of task #161, before its pull request is opened, run `/speckit-converge` and turn its findings into new tasks under #156; that pull request ends with `Closes #161` and `Closes #156`

## Dependencies

- T001 goes with #157. #158 needs #157's port and weeks. #159 needs nothing. #160 needs #158's API and #159's chart. #161 needs #160's page and chart card. Task #108 of story #104 needs #158 and #159.
- Inside #157: T002 before T003; T003 before T004; T004 before T005 and T007; T005 before T006; T007 before T008; T010 last.
- Inside #158: T011 and T012 before T013; T013 before T014 and T015; T015 before T016; T016 and T017 before T018; T018 before T019; T020 last.
- Inside #159: T021, T022, T023 in order; T024 last.
- Inside #160: T025 first; T026 to T029 in any order; T030 and T031 after T027 to T029; T032 after them; T033 beside T030 to T032; T034 after T032; T035 last.
- Inside #161: T036 first; T037 before T038; T039 beside T038; T040 after T038; T041 last.

## Parallel opportunities

- T002, T004 and T009 touch different files; T006 and T008 can be written together once T005 and T007 exist.
- T011 and T012 touch different files; T014 and T016 can be written together once T013 exists.
- #159 can be built at any time before #160.
- T026, T027, T028 and T029 touch different files.

## Implementation strategy

Build in task order: the queries first, since every figure comes from them; then the API, which fixes the numbers for the client and the trainer; then the chart, the tab and "By exercise". Each task is one pull request, opened when its checks pass and the user agrees.
