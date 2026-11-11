---
description: "Tasks for story #122, Trainer sees a client's adjustments (tasks #123, #124, #290)"
---

# Tasks: Trainer sees a client's adjustments

**Input**: `specs/122-trainer-adjustments/` (spec.md, plan.md, research.md, data-model.md, contracts/overview-api.md, quickstart.md)

**Tests**: Required by the Definition of Done and principle V. Each test sits with the code it covers.

**Organization**: Phases follow the GitHub tasks in commit order: #123 (readiness and adjustments in the client overview API), #124 (readiness strip and adjustment entries), #290 (lighter targets in the trainer's session log). One pull request covers the story, with one commit per task, each ending in `(#<task>)`.

**Paths**:

| Alias | Path |
|---|---|
| `B` | `backend/src/main/java/com/kinetiq/readiness` |
| `BT` | `backend/src/test/java/com/kinetiq/readiness` |
| `S` | `backend/src/main/java/com/kinetiq/sessionlog` |
| `ST` | `backend/src/test/java/com/kinetiq/sessionlog` |
| `BR` | `backend/src/main/resources` |
| `F` | `frontend/src` |
| `FS` | `frontend/src/features/sessionlog` |

**User stories** (spec.md):

| Label | Story |
|---|---|
| US1 | readiness for the last 14 days |
| US2 | every recovery day and whether the client reverted |
| US3 | lighter targets in the session's log |
| US4 | only the client's own trainer sees it |

## Phase 1: Setup

- [X] T001 Commit `specs/122-trainer-adjustments/` with the code of task #123, so the spec is reviewed in the story's pull request. `CONTEXT.md` needs no new term: "Readiness band" already names Recovery day, and "Adjustment" is defined. No migration, so no migration number to check.

## Phase 2: task #123, readiness and adjustments in the client overview API (US1, US2, US4)

**Goal**: `GET /api/v1/clients/{id}/overview` returns `readiness` (14 days) and `recoveryDays`, as in contracts/overview-api.md.

**Independent test**: `ClientTrainingViewServiceTest` and `ClientTrainingControllerTest` prove the 14 days, empty days, every outcome and the `404`.

- [X] T002 [P] [US2] `B/domain/model/Cause.java`: add `public static List<Cause> of(Answers answers)`, returning `SLEEP` for sleep ≤ 2, `SORENESS` for soreness ≥ 4 and `STRESS` for stress ≥ 4, in that order, with Javadoc. `B/domain/model/Adjustment.java`: delete the private `causes(Answers)` and call `Cause.of`. The existing `AdjustmentTest` cause cases cover the moved rule unchanged, so add no new test.
- [X] T003 [P] [US1] `B/domain/output/CheckInRepository.java`: add `List<CheckIn> findAllByClientAndDateBetween(AccountId client, LocalDate from, LocalDate to)` ("both ends included, oldest first"). `B/infrastructure/output/JpaCheckInRepository.java`: add `List<CheckInEntity> findAllByClientIdAndCheckInDateBetweenOrderByCheckInDate(UUID clientId, LocalDate from, LocalDate to)`. `B/infrastructure/output/CheckInRepositoryImpl.java`: map it with the existing `CheckInMapper`.
- [X] T004 [US1] `BT/infrastructure/output/CheckInRepositoryImplIT.java`: a `@Nested` group for `findAllByClientAndDateBetween`. Check-ins on `from − 1`, `from`, a day between, `to` and `to + 1` return exactly the middle three, oldest first. Another client's check-in in range is not returned.
- [X] T005 [US1] `B/domain/input/CheckInUseCase.java`: add `List<CheckIn> between(AccountId client, LocalDate from, LocalDate to)`, with Javadoc saying both ends are included, oldest first, and "the caller has checked that it may read this client". `B/domain/service/CheckInService.java`: delegate to the repository. Add the method to every hand-written fake: the `CheckInUseCase` fakes in `ST/domain/service/SessionLogServiceTest.java` and `ST/domain/service/SessionStateServiceTest.java` throw `UnsupportedOperationException`, and the readiness repository fake used by `BT/domain/service/CheckInServiceTest.java` filters its map.
- [X] T006 [US1] [US2] `S/domain/input/ClientOverview.java`: add `List<ReadinessDay> readiness` and `List<RecoveryDay> recoveryDays`, both required and copied defensively, and extend the class Javadoc.
  - Nested class `ReadinessDay(LocalDate date, Readiness readiness)`: `date` is required, and `readiness` may be `null` ("absent without a check-in; never a score of 0 in its place"), read through `Optional<Readiness> getReadiness()`. Use the pattern `ClientOverview` uses for `week` (`@Getter`, `@Getter(AccessLevel.NONE)` on the field, an `Optional` getter); a record cannot have an accessor of another type.
  - Nested class `RecoveryDay(LocalDate date, int score, List<Cause> causes, TargetChoice targets, ClientSession session)`, with the same pattern: `causes` is copied and non-empty; `targets` and `session` may be `null` and are read as `Optional`. Javadoc: "`targets` is `LIGHTER` when the day's adjustment is in use, `ORIGINAL` when not, absent without an adjustment; `session` is the started session of that date, else the plan's session that weekday within the plan's dates, else absent".
- [X] T007 [US1] [US2] [US4] `S/domain/service/ClientTrainingViewService.java`: take `CheckInUseCase checkIns` in the constructor, and `S/infrastructure/config/SessionLogConfig.java` passes it. In `overview`, after `clients.getClient(trainer, client)`:
  - `from = today.minusDays(13)`; `checkIns.between(client, from, today)`, keyed by date.
  - `readiness`: for each of the 14 dates, oldest first, a `ReadinessDay` with `checkIn.readiness()` or `null`.
  - `recoveryDays`: for each check-in whose `readiness().band() == RECOVERY_DAY`, newest first:
    - `score` from its readiness and `causes` from `Cause.of(checkIn.getAnswers())`.
    - Planned entries:
      - If `byDate` has the session log, use its entries with measures from the trainer's library, as `SessionLogService.adjustment` does.
      - Otherwise, if the published `OwnPlanView` has a session with `session.day() == date.getDayOfWeek()` and `!date.isBefore(publishedOn) && date.isBefore(firstWeek.plusWeeks(weeks))`, use its entries with measures from `view.library()`.
      - Otherwise there are none.
    - `targets`: `checkIns.adjustmentOn(client, date, planned).map(a -> a.inUse() ? LIGHTER : ORIGINAL)` when there are planned entries, else `null`.
    - `session`: `started(log, now)`, or `planned(session, date, SessionState.of(date, today, publishedOn, Optional.empty(), now).orElse(null))`, or `null`.
  - Extract a private helper for "planned entries of a session log", reused by T022.
- [X] T008 [US1] [US2] [US4] `ST/domain/service/ClientTrainingViewServiceTest.java`: add a `CheckInUseCase` fake whose `between` and `adjustmentOn` read a map of check-ins, using the real `Adjustment.of` with kg. Use a fixed clock with the client in a time zone where UTC midnight is another local date. `@Nested` for the overview's readiness:
  - Exactly 14 days, `today − 13` to `today`, oldest first, by the client's local date.
  - A day without a check-in has no readiness; a 1/5/5 check-in gives score 0 and `RECOVERY_DAY`, present.
  - A check-in on `today − 14` is not included.
  - Recovery days come newest first, with `Ready` and `Take it easier` days excluded; causes for 2/4/3 are `[SLEEP, SORENESS]`.
  - Targets outcomes:
    - Started session with the lighter targets in use: `LIGHTER`, from the log's copy, which differs from the plan after a plan edit.
    - Original targets chosen: `ORIGINAL`.
    - A day with no plan session and no log: absent, and the session is absent.
    - Every target at its floor: absent, with the session present.
    - A never-started past plan day: `MISSED`, with no `sessionLogId`.
    - Today not started: `TODAY`.
    - A recovery check-in before the publish day: no session and no targets.
  - Another trainer's client: `ClientNotFoundException`, with `between` never called.
- [X] T009 [US1] [US2] `S/infrastructure/input/ClientOverviewResponse.java`: add `@NotNull List<ReadinessDayResponse> readiness` and `@NotNull List<RecoveryDayResponse> recoveryDays`.
  - `record ReadinessDayResponse(@NotNull LocalDate date, Integer score, Readiness.Band band)` with `@JsonInclude(NON_NULL)`, and Javadoc "score and band are both present or both left out; left out means no check-in, never 0".
  - `record RecoveryDayResponse(@NotNull LocalDate date, @NotNull int score, @NotNull List<Cause> causes, TargetChoice targets, ClientSessionResponse session)` with `@JsonInclude(NON_NULL)`.
  - `S/infrastructure/input/SessionLogWebMapper.java`: map both, reusing the existing `ClientSessionResponse` mapping for `session`.
- [X] T010 [US1] [US2] [US4] `ST/infrastructure/input/ClientTrainingControllerTest.java` (`@WebMvcTest`, use case mocked):
  - The JSON has 14 `readiness` items, one without `score` and `band`, and one with `score: 0`.
  - A `recoveryDays` item has `causes`, `targets`, and `session.sessionLogId`; another item leaves out `targets` and `session`.
  - `ClientNotFoundException` gives `404 client.not-found`; the `CLIENT` role gives `403`; no sign-in gives `401`.
- [X] T011 [US2] `BR/db/dev-data/R__demo_check_ins.sql`: the check-in on `current_date - 3` (2/4/3, a recovery day) gets `target_choice = 'ORIGINAL'`. Add the column to that insert, keep `on conflict do nothing`, and update the comment: "one recovery day with the original targets chosen, so the trainer's overview shows both outcomes".
- [X] T012 Run `./gradlew check` (Docker running), which must pass, including `HexagonalArchitectureTest`: `sessionlog → readiness` already exists, and readiness gains no dependency. Start the backend and run `npm run gen:api` in `frontend/`. Add `readiness: []` and `recoveryDays: []` to the overview fixtures in `FS/pages/client-overview-tab.test.tsx` (and any other test that builds a `ClientOverviewResponse`) so `npm run check` passes. Commit `feat(backend): add readiness and recovery days to the client overview (#123)`, with `schema.d.ts`.

## Phase 3: task #124, readiness strip and adjustment entries (US1, US2)

**Goal**: the card "Readiness, last 14 days" as the first card of the overview grid (FR-008 to FR-014), and the Logs tab opened on a chosen session.

**Independent test**: `readiness-strip.test.tsx` renders the strip and entries from a fixture, and `client-logs-tab.test.tsx` opens `?session=<id>`.

- [X] T013 [P] [US1] `F/lib/readiness.ts`: move `BAND_STYLE` (band → `{ surface, dot }`) from `F/features/readiness/components/readiness-card.tsx`, with its comment "the readiness scale, never red: a low score is not an error", and add `bar: 'bg-ready-high' | 'bg-ready-mid' | 'bg-ready-low'` if `dot` cannot serve the bar. `readiness-card.tsx` imports it, with no behaviour change.
- [X] T014 [P] [US1] [US2] `F/locales/en.json` and `F/locales/pl.json`, under `sessionlog.overview.readiness`:
  - `title`: "Readiness, last 14 days" / "Gotowość, ostatnie 14 dni"
  - `legend`: "Bands" / "Zakresy" (the legend list's accessible name)
  - `day`: "{{date}}: {{score}}, {{band}}" / same pattern
  - `noCheckIn`: "{{date}}: No check-in" / "{{date}}: brak check-inu"
  - `today`: "Today" / "Dziś"
  - `noCheckIns`: "No check-ins in the last 14 days." / "Brak check-inów w ostatnich 14 dniach."
  - `noRecoveryDays`: "No recovery days in the last 14 days." / "Brak dni regeneracji w ostatnich 14 dniach."
  - `recoveryDays`: "Recovery days" / "Dni regeneracji"
  - `reason`: "{{causes}}" (capitalised list)
  - `targets.LIGHTER`: "Lighter targets" / "Lżejsze cele"
  - `targets.ORIGINAL`: "Original targets" / "Pierwotne cele"
  - `targets.none`: "No lighter targets" / "Bez lżejszych celów"
  - `score`: "Readiness {{score}}" / "Gotowość {{score}}" (the entry's score, for screen readers)

  Reuse `readiness.card.band.*.name` and `readiness.adjustment.cause.*`. Match the Polish words "check-in", "cel" and "dzień regeneracji" to their existing uses in `pl.json`.
- [X] T015 [US1] `FS/components/readiness-strip.tsx`: `ReadinessCard({ overview })` as a `Card` with `role="region"` named by its `h2` title, the same pattern as `OverviewCard`.
  - Header: the title and a legend `<ul>` of three items, each a `size-2 rounded-full` dot in the band colour and the band name.
  - Strip: an `<ol className="grid grid-cols-14 …">` with one `<li>` per `overview.readiness` item. Each item has an `aria-hidden` column: a fixed-height track (`h-28`, `flex items-end`), a bar of `height: max(score, 3)%` in the band colour with `rounded-t-sm`, the score above it in `num text-xs`, and the day of the month below in `text-muted-foreground text-[10px]`. Today's column is the last, with its label "Today" in `font-semibold`. Each item also has one `sr-only` text: `day` or `noCheckIn`, with the date formatted weekday-day-month in `timeZone: 'UTC'` as `useFormats` does.
  - A day without a check-in has no bar and no number (FR-010).
  - With no check-in in the 14 days, the card shows `noCheckIns` only (FR-012).
  - Inline style is allowed only for the bar height; use semantic classes otherwise.
- [X] T016 [US2] Same file, under the strip: an `h3` "Recovery days" and a `<ul className="divide-y">` of `overview.recoveryDays` in API order. Each `<li>` shows:
  - the date in the weekday-day-month format and the score in `num` with `sr-only` `score`;
  - the reason, from `Intl.ListFormat(language, { type: 'conjunction' })` over the translated causes, first letter capitalised for the locale, as the client's adjustment reason does (reuse its helper if it lives in `F/lib`; otherwise move it there);
  - the outcome text (`targets.LIGHTER`, `targets.ORIGINAL` or `targets.none`) in `text-muted-foreground`;
  - the session: its name as a `Link` to `../logs?session=<sessionLogId>` (relative to the client route, matching how the "All logs" button links to `logs`) when `sessionLogId` exists, plain text otherwise, followed by `SessionStateBadge`.

  With check-ins but no recovery day, show `noRecoveryDays` (FR-012). Text wraps, with no fixed widths, so that at 768 px nothing scrolls sideways.
- [X] T017 [US1] `FS/pages/client-overview-tab.tsx`: render `<ReadinessCard overview={overview} />` as the first child of the grid, before "This week" (Clarification Q3). Add a fourth `Skeleton` first in `OverviewLoading`.
- [X] T018 [US2] `FS/pages/client-logs-tab.tsx`: replace `useState` for `chosen` with `useSearchParams`. Read `session`, and on choose call `setSearchParams({ session: keyOf(session) }, { replace: true })`. An unknown key still falls back to `sessions[0]`. Done on `master` by story #172 before this story landed, so nothing is left here.
- [X] T019 [US1] [US2] `FS/components/readiness-strip.test.tsx`:
  - 14 list items, the last marked today.
  - A day without a check-in reads "…: No check-in" and has no visible number; a score-0 day reads "…: 0, Recovery day".
  - The legend names three bands.
  - The no-check-ins message hides the strip; the no-recovery-days message shows under the strip.
  - Entries are newest first, with reason "Poor sleep and high soreness", the three outcome texts, a link to `logs?session=<id>` for a started session, and no link for a missed one.
  - In Polish, a reason with three causes is a Polish list.
- [X] T020 [US2] `FS/pages/client-overview-tab.test.tsx`: the readiness card is the first region. `FS/pages/client-logs-tab.test.tsx`: opening the route at `logs?session=<an older id>` shows that session's log; choosing another session updates the URL. Covered by the tests of story #172 on `master`.
- [X] T021 Run `npm run check`, which must pass. Commit `feat(frontend): add the readiness strip and recovery days to the overview (#124)`.

## Phase 4: task #290, lighter targets in the trainer's session log (US3, US4)

**Goal**: the trainer's session log shows planned → lighter targets and the reason line (FR-015, FR-016).

**Independent test**: `ClientTrainingViewServiceTest`, `ClientTrainingControllerTest` and `session-log-table.test.tsx` with an adjusted log, an original-targets log and a plain log.

- [ ] T022 [US3] `S/domain/input/ClientSessionLog.java`: add a nullable `Adjustment adjustment` component, as `StartedSession` holds one (no `Optional` fields, backend rule), with Javadoc "the adjustment of the log's date over the log's own copy; absent without one". `S/domain/service/ClientTrainingViewService.java` `sessionLog`: compute it with `checkIns.adjustmentOn(client, log.getDate(), plannedOf(log, library))` using T007's helper.
- [ ] T023 [US3] `S/infrastructure/input/ClientSessionLogResponse.java`: add `LogAdjustmentResponse adjustment` with `@JsonInclude(NON_NULL)` and `record LogAdjustmentResponse(@NotNull List<Cause> causes, @NotNull boolean inUse)` (not `AdjustmentResponse`: springdoc names schemas by simple name, and readiness already has one). `S/infrastructure/input/SessionLogWebMapper.java`: pass `read.adjustment()` to `entries(...)` instead of `null`, delete the "story #122" comment, and map `adjustment`.
- [ ] T024 [US3] [US4] Tests:
  - `ST/domain/service/ClientTrainingViewServiceTest.java`, `sessionLog`: a recovery day with lighter targets gives an adjustment with `inUse` true; original targets gives `inUse` false; a Ready day and a day without a check-in give none; another trainer's client gives `ClientNotFoundException`.
  - `ST/infrastructure/input/ClientTrainingControllerTest.java`: an adjusted log has `adjustment.causes` and `inUse`, an entry with `planned` 100 kg/8 and an unlogged set target of 90 kg/7, and a logged set keeps its copied target. A log without an adjustment leaves out `adjustment` and `planned`.
- [ ] T025 Run `./gradlew check`, which must pass. Start the backend and run `npm run gen:api`.
- [ ] T026 [P] [US3] `FS/components/session-log-table.tsx`: take `Entry['planned']`. In the target cell, when `entry.planned` exists, render `TargetChange` with `planned` formatted as the cell formats `set.target` and `today` as the formatted `set.target`. When `entry.planned.rpe !== set.target.rpe`, add a second line `TargetChange` "RPE {{planned}}" → "RPE {{today}}" in `text-xs`. Otherwise render the cell as today. Add the key `sessionlog.logs.rpeTarget`: "RPE {{value}}" / "RPE {{value}}".
- [ ] T027 [P] [US3] `FS/pages/client-logs-tab.tsx` `ChosenSession`: under the `CardDescription`, when `log.adjustment` exists, show a `p` in `text-muted-foreground`:
  - `sessionlog.logs.lighter`: "Recovery day: {{causes}}, so the targets were lighter." / "Dzień regeneracji: {{causes}}, więc cele były lżejsze." (causes in lower case, joined with `Intl.ListFormat`).
  - or `sessionlog.logs.original`: "Recovery day: the client used the original targets." / "Dzień regeneracji: wybrano pierwotne cele." (impersonal, so no gendered past tense).
- [ ] T028 [US3] Tests: `FS/components/session-log-table.test.tsx` (new, or in `client-logs-tab.test.tsx`): an adjusted entry is read as "planned 100 kg, today 90 kg"-style text via `TargetChange`'s `sr-only`, with no arrow when `planned` is absent. `client-logs-tab.test.tsx`: the lighter line with causes, the original line, and no line without `adjustment`. Run `npm run check`, which must pass. Commit `feat: show lighter targets in the trainer's session log (#290)`, with `schema.d.ts`.

## Phase 5: Polish and verification

- [ ] T029 Run quickstart.md scenarios 1 to 10 in the built-in browser at 1280 px (and 768 px for scenario 9), in light and dark, EN and PL. Check:
  - no console errors;
  - the readiness scale with nothing red;
  - text alternatives in the accessibility tree;
  - visible focus on the session links;
  - the link opening the log;
  - planned → lighter in the log.

  Recreate the local database first so the demo dates are current. Take screenshots for the pull request.
- [ ] T030 Review with the `ui-ux-pro-max` skill for WCAG 2.2 AA and UX:
  - contrast of the score labels and day labels on the card in both themes;
  - bar colours against the card (non-text contrast, 3:1);
  - the legend;
  - the reading order of the strip;
  - the entry layout at 1280 and 768 px.

  Fix findings without restyling the design system.
- [ ] T031 Run `/speckit-converge`, then the backend `./gradlew check` and the frontend `npm run check` one last time. Open the story's pull request `feat: show a client's readiness and adjustments to the trainer`, with `Closes #122`, `Closes #123`, `Closes #124` and `Closes #290`, to be merged by rebase or a merge commit.

## Dependencies

- Phases run in order: #123 → #124 → #290.
  - #124 needs #123's generated types.
  - #290 reuses T007's helper and changes `client-logs-tab.tsx` after T018.
- Within #123: T002 and T003 can run in parallel ([P]). T004 needs T003. T005 needs T003. T006 needs T002. T007 needs T005 and T006. T008 needs T007. T009 needs T006. T010 needs T009. T012 comes last.
- Within #124: T013 and T014 can run in parallel ([P]). T015 and T016 need both. T017 needs T015. T018 is independent of T015 to T017. T019 and T020 follow their code.
- Within #290: T022 → T023 → T024 → T025. T026 and T027 need T025 and can run in parallel ([P]). T028 comes last.

## Parallel examples

- #123: T002 (`Cause`) and T003 (repository) touch different files.
- #124: T013 (`lib/readiness.ts`) and T014 (locales) are independent; T018 (Logs tab URL state) can be done alongside T015 and T016.
- #290: T026 (table) and T027 (detail line) touch different files.

## Implementation strategy

- MVP: #123 and #124, the story's acceptance criterion. The trainer sees 14 days of readiness and every recovery day with whether the client reverted (US1, US2, US4).
- Then #290 makes the link from an entry land on a log that shows the old and new values (US3, constitution IV).
- Each phase ends green (`./gradlew check`, `npm run check`) and with its own commit.
