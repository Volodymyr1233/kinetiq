---

description: "Tasks for Rules detect that a plan change is due (story #125)"
---

# Tasks: Rules detect that a plan change is due

**Input**: Design documents from `specs/125-plan-change-rules/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ports.md, quickstart.md

**Tests**: Required by the constitution (V) and `backend/CLAUDE.md`: plain unit tests with hand-written fakes, `*IT` with Testcontainers for the JDBC adapter. Names follow `shouldX_whenY`.

**Organization**: One phase per user story, which is also one task issue and one commit of the story pull request. Branch: `feat/125-plan-change-rules`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: US1 = #126, US2 = #127, US3 = #128, US4 = #129, US5 = #130
- Paths are under `backend/` unless they start with `specs/`
- Main code root: `src/main/java/com/kinetiq/planchange/`; tests mirror it under `src/test/java/com/kinetiq/planchange/`

## Commit plan (commit per task, rebase or merge so each stays on `master`)

| Order | Commit | Message |
|---|---|---|
| 0 | the spec folder | `docs: add spec for plan change rules (#125)` |
| 1 | Phase 1 | `feat(backend): check plans weekly per client time zone (#126)` |
| 2 | Phase 2 | `feat(backend): detect high RPE (#127)` |
| 3 | Phase 3 | `feat(backend): detect stalled weight (#128)` |
| 4 | Phase 4 | `feat(backend): detect low completion (#129)` |
| 5 | Phase 5 | `feat(backend): pick one plan change by priority with cool-down (#130)` |

No `Co-Authored-By` or other attribution footer (project `CLAUDE.md`). Each commit compiles and passes `./gradlew check` on its own.

## Phase 0: Spec commit

- [X] T001 Commit `specs/125-plan-change-rules/` (spec, plan, research, data model, contracts, quickstart, tasks, checklist) as the first commit of the pull request. Do not commit `.specify/feature.json`; it only points the local Spec Kit commands at this folder.

---

## Phase 1: User Story 1, weekly run per client time zone (#126) 🎯 MVP

**Goal**: every client with a published plan is checked exactly once per week, after Monday 03:00 in their own time zone, with no endpoint.

**Independent Test**: with a fixed clock, a Warsaw and a Los Angeles client are checked at their own Monday 03:00, once, also across a daylight-saving change and after downtime.

- [ ] T002 [US1] Create migration `src/main/resources/db/migration/V22__create_weekly_checks.sql`: table `weekly_checks (client_id uuid not null, week_start date not null, checked_at timestamptz not null)`, constraints `pk_weekly_checks (client_id, week_start)`, `fk_weekly_checks_client` to `accounts (id)`, `ck_weekly_checks_week_start_monday check (extract(isodow from week_start) = 1)`. First run `gh pr list --repo Volodymyr1233/kinetiq --json number,files` and renumber if V22 is taken.
- [ ] T003 [P] [US1] Create domain model in `planchange/domain/model/`: `Rule` enum in this order `LOW_COMPLETION(2)`, `HIGH_RPE(2)`, `STALLED_WEIGHT(3)` with `weeks()`; records `LoggedSet(ExerciseId exercise, BigDecimal loadKg /* nullable */, int rpe)`, `WeekTraining(LocalDate monday, int sessionsInPlan, int plannedSessions, int finishedSessions, List<LoggedSet> sets)` (defensive copy of `sets`), `Finding(Rule rule, Set<ExerciseId> exercises)`, `DuePlanChange(AccountId client, Rule rule, LocalDate windowStart, LocalDate windowEnd, Set<ExerciseId> exercises)`, and `ScheduledClient(AccountId client, ZoneId zone, LocalDate firstWeek, int weeks, Optional<LocalDate> lastCheckedWeek)` with `checkWeek(Instant now)` (local date-time minus 3 hours, Monday on or before; local arithmetic, not instant arithmetic) and `isDue(Instant now)` (`checkWeek` after `lastCheckedWeek`). Reuse `AccountId` and `ExerciseId`; no framework imports.
- [ ] T004 [P] [US1] Create ports: `planchange/domain/output/TrainingHistory` with `List<ScheduledClient> clientsWithPublishedPlan()`, `Optional<ScheduledClient> scheduledClient(AccountId client)` and `boolean claimWeek(AccountId client, LocalDate week, Instant now)`; `planchange/domain/input/DetectPlanChangesUseCase` with `List<AccountId> due()` and `void check(AccountId client)`.
- [ ] T005 [US1] Implement `planchange/domain/service/PlanChangeDetectionService` (`@RequiredArgsConstructor`, `Clock`, plain class): `due()` returns the clients for which `isDue(now)` holds; `check(client)` loads `scheduledClient(client)` and returns when it is empty, re-computes the check week from the clock and that client's zone, calls `claimWeek` and returns when it is `false`; logs only counts and the class name of failures (`@Slf4j`, no names, emails). The rules list is empty in this task, so `check` records the claim and does nothing else.
- [ ] T006 [US1] Implement `planchange/infrastructure/output/JdbcTrainingHistory` with `JdbcTemplate` (precedent `plan/infrastructure/output/JdbcDraftedPlanDirectory`): `clientsWithPublishedPlan` joins `plans` (`first_week is not null`), `accounts` (`time_zone`) and `max(week_start)` of `weekly_checks`, and `scheduledClient` is the same query for one client; `claimWeek` is `insert ... on conflict do nothing` and returns whether a row was inserted.
- [ ] T007 [US1] Create `planchange/infrastructure/input/PlanChangeSweep` (`@Component`, `@Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)`): `due()`, then `check` per client in its own call inside `try`/`catch (RuntimeException)`, logging only the exception class name, like `PlanDraftingSweep`. Create `planchange/infrastructure/config/PlanChangeConfig` wiring `DetectPlanChangesUseCase` with `Clock.systemUTC()` (scheduling is already enabled in `PlanConfig`).
- [ ] T008 [P] [US1] Unit tests `src/test/java/com/kinetiq/planchange/domain/model/ScheduledClientTest` and `.../service/PlanChangeDetectionServiceTest` with `Clock.fixed` and a fake `TrainingHistory`: Warsaw due at its Monday 03:00 and Los Angeles not yet; 02:59 not due, 03:00 due; once per week: a second `check` in the same week is skipped; catch-up after downtime later in the week; a week with a daylight-saving change; a client without a published plan is skipped; over a simulated month (4 Mondays, a Warsaw and a Los Angeles client) each client is checked exactly 4 times.
- [ ] T009 [P] [US1] Integration test `.../infrastructure/output/JdbcTrainingHistoryIT` (`TestcontainersConfiguration`): published plan listed with its zone and `lastCheckedWeek`, also through `scheduledClient`; a draft is not listed and `scheduledClient` is empty for it; `claimWeek` is `true` once and `false` the second time; the `weekly_checks` Monday constraint rejects a non-Monday.
- [ ] T010 [P] [US1] Unit test `.../infrastructure/input/PlanChangeSweepTest`: when `check` throws for one client the sweep still calls `check` for the others and does not rethrow (needs the sweep to take the use case and be package-private-constructible, as `PlanDraftingSweep`).
- [ ] T011 [P] [US1] Add to `JdbcTrainingHistoryIT` (or a `@SpringBootTest` slice with the real transaction wrapping) a test that when `check` fails after the claim, no `weekly_checks` row remains (FR-011).
- [ ] T012 [US1] Run `./gradlew check`, then commit `feat(backend): check plans weekly per client time zone (#126)`.

**Checkpoint**: weekly claim and schedule work end to end; no rule yet.

---

## Phase 2: User Story 2, rule: high RPE (#127)

**Goal**: high RPE fires over the last 2 completed weeks, at exact thresholds.

**Independent Test**: mean RPE exactly 8.0 in both weeks does not fire; just above 8.0 in both does.

- [ ] T013 [US2] Add `weeks(AccountId client, LocalDate firstMonday, int count)` to `TrainingHistory` and implement it in `JdbcTrainingHistory`: for each Monday-to-Sunday week, `sessionsInPlan` = the client's plan sessions; `plannedSessions` = plan sessions whose date in that week is on or after `plans.published_on`; `finishedSessions` = those planned sessions with a `session_logs` row of that `log_date` with `finished_at` not null (date matching as `SessionStateService`); `sets` = every `set_logs` row of the client's session logs dated in the week, with the exercise from `session_log_entries` (`session_log_id`, `entry_position`), the set's `load_kg` and `rpe`. Only the given client's rows are read.
- [ ] T014 [P] [US2] Create `planchange/domain/rule/PlanChangeRule` (`Rule rule()`, `Optional<Finding> evaluate(List<WeekTraining> window)`, window oldest first, exactly `rule().weeks()` weeks) and `HighRpeRule`: fires when in each of the 2 weeks the sets have `sum(rpe) > 8 * count` (integer arithmetic, never the rounded `averageRpe`); a week without sets does not fire; the finding's exercises are those with `sum(rpe) > 8 * count` over the whole window.
- [ ] T015 [US2] In `PlanChangeDetectionService.check`, after the claim: take the last completed week (`checkWeek - 7` days) and load `weeks(client, lastMonday - (maxWindow - 1) * 7, maxWindow)`; evaluate a rule only when its whole window lies on or after the plan's first week and its last week is not after plan week `weeks` (FR-003); collect the findings and log only the rule names and counts (choosing one comes in T026). Wire `HighRpeRule` in `PlanChangeConfig`.
- [ ] T016 [P] [US2] Unit tests `.../domain/rule/HighRpeRuleTest`: exactly 8.0 in both weeks no; just above in both (for example 201 over 25 sets, 8.04, which a one-decimal rounding would read as 8.0) yes; above in one week only no; empty week no; exercises listed only when their mean is above 8. Extend `PlanChangeDetectionServiceTest`: plan first week and plan end limit the window (rule not evaluated when the plan is younger than 2 weeks, or finished).
- [ ] T017 [P] [US2] Extend `JdbcTrainingHistoryIT`: `weeks` returns planned, finished and sets per week; a session started and locked unfinished is not finished; days before `published_on` are not planned; another client's logs never appear.
- [ ] T018 [US2] Run `./gradlew check`, then commit `feat(backend): detect high RPE (#127)`.

---

## Phase 3: User Story 3, rule: stalled weight (#128)

**Goal**: stalled weight fires per exercise with a load over the last 3 completed weeks.

**Independent Test**: loads 100/102/105 with mean RPE 7 do not fire (exactly 5%); 100/101/104.9 do; the same with RPE above 8 do not.

- [ ] T019 [US3] Create `planchange/domain/rule/StalledWeightRule` (window 3): per exercise, over sets with a `loadKg`: present in each of the 3 weeks; `best3 < best1 * 1.05` with `BigDecimal` (best = highest load of the week); `sum(rpe) <= 8 * count` over the 3 weeks; exercises without any load are ignored; the finding lists every stalled exercise. Wire it in `PlanChangeConfig`.
- [ ] T020 [P] [US3] Unit tests `.../domain/rule/StalledWeightRuleTest`: exactly 5% no; just under 5% yes; fall of the best load yes; RPE mean above 8 no and exactly 8 yes; logged in only 2 of 3 weeks no; bodyweight exercise ignored; only the stalled ones among several are listed.
- [ ] T021 [US3] Run `./gradlew check`, then commit `feat(backend): detect stalled weight (#128)`.

---

## Phase 4: User Story 4, rule: low completion (#129)

**Goal**: low completion fires below 70% over 2 completed weeks and never for a plan of 2 sessions a week.

**Independent Test**: 4 of 6 finished fires; 7 of 10 (exactly 70%) does not; a plan of 2 a week with none finished does not.

- [ ] T022 [US4] Create `planchange/domain/rule/LowCompletionRule` (window 2): sum `plannedSessions` and `finishedSessions` over the window; fires when planned is above 0, `sessionsInPlan` is above 2 in every week and `finished * 10 < planned * 7`; no exercises in the finding. Wire it in `PlanChangeConfig`.
- [ ] T023 [P] [US4] Unit tests `.../domain/rule/LowCompletionRuleTest`: 4 of 6 yes; 5 of 6 no; 7 of 10 no; a plan of 2 sessions a week never, also with 0 finished; a week where the plan was published mid-week counts only the planned days; zero planned no.
- [ ] T024 [US4] Run `./gradlew check`, then commit `feat(backend): detect low completion (#129)`.

---

## Phase 5: User Story 5, priority, one open plan change, cool-down (#130)

**Goal**: at most one due plan change per run, chosen by priority, never for a client with an open plan change, and never inside a rule's cool-down.

**Independent Test**: with all three rules firing only a low completion plan change is reported as due; with an open plan change nothing; after an end, each rule stays quiet until its full window has passed.

- [ ] T025 [US5] Create the port `planchange/domain/output/PlanChangeRepository` (`boolean hasOpen(AccountId client)`, `Optional<Instant> lastEndedAt(AccountId client)`, `void open(DuePlanChange due)`) and the placeholder adapter `planchange/infrastructure/output/LoggingPlanChangeRepository` (`@Repository`; `hasOpen` false, `lastEndedAt` empty, `open` logs the rule name only; Javadoc says task #132 replaces it).
- [ ] T026 [US5] Extend `PlanChangeDetectionService.check`: skip when `hasOpen`; compute the earliest usable week as `max(plan first week, first Monday on or after lastEndedAt in the client's zone)` and require each rule's whole window to start on or after it; choose the first finding in `Rule` order (completion, RPE, stalled weight); call `open` with a `DuePlanChange` carrying the rule, the window's first Monday and last Sunday and the exercises; never more than one due plan change per run. Inject `PlanChangeRepository` in `PlanChangeConfig`.
- [ ] T027 [P] [US5] Unit tests in `PlanChangeDetectionServiceTest` with an in-memory fake `PlanChangeRepository`: all three fire gives completion; RPE and stalled gives RPE; only stalled gives stalled; open plan change gives nothing due and no history read; plan change ended 1 week ago blocks completion and RPE and stalled; ended 2 weeks ago allows completion and RPE but not stalled, 3 weeks ago allows stalled; a decline mid-week counts from the next Monday; an accepted or an expired change ended at its effective Monday; the winning rule in cool-down lets the next rule fire; the week is claimed even when nothing fires.
- [ ] T028 [US5] Run `./gradlew check`, then commit `feat(backend): pick one plan change by priority with cool-down (#130)`.

---

## Phase 6: Verification and pull request

**Purpose**: prove the whole story works and is secure before the pull request.

- [ ] T029 Run `./gradlew check` in `backend/` and record the real result, including `HexagonalArchitectureTest`; fix any failure at its cause.
- [ ] T030 [P] Security review of the branch (`/security-review` or the `security-review` skill): no endpoint or `SecurityConfig` change; `/v3/api-docs` shows no new path; logs carry only rule names, counts and exception class names, no names or emails; every history query is scoped to one client's own rows; nothing is sent to an LLM; the migration has named constraints.
- [ ] T031 [P] Code review of the branch (`/code-review`) with attention to time zones (daylight saving, half-hour offsets), threshold boundaries, once-per-week under concurrency, and transaction scope (one client per transaction).
- [ ] T032 Run the manual steps of `quickstart.md` on the local profile and note what was observed; correct `quickstart.md` if reality differs.
- [ ] T033 Before opening the pull request run `gh pr list --repo Volodymyr1233/kinetiq --json number,files` to confirm V22 is free; open one pull request titled `feat(backend): detect plan changes by rule (#125)`, body closing `#125` and `#126` to `#130` (`Closes #n` each), merged with rebase or a merge commit so each task keeps its commit.

---

## Dependencies and order

- Phases run in order 0 to 6; each later phase builds on the earlier code, so there is no parallel story work (one pull request, one commit per task).
- Inside a phase, tasks marked [P] touch different files and can be done in any order; the commit comes last.
- US2 needs T013 (`weeks`), US3 and US4 reuse the window loading of T015, US5 reuses all rules.

## Out of scope (other stories)

Plan change table, model and states (#132), drafting and limits (#133 to #136), accept, undo and decline (#138, #139), apply or expire at the effective Monday (#144), all screens and notifications. Until #132 the placeholder adapter only logs.

## Implementation strategy

MVP is Phase 1 (the schedule and weekly claim), but the story is only useful with all five phases, so build in order and run `./gradlew check` before each commit. Stop and ask if a quality gate fails for a reason outside this feature.
