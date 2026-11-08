---
description: "Tasks for story #109, Client checks in and sees readiness (tasks #110 to #113)"
---

# Tasks: Client checks in and sees readiness

**Input**: `specs/109-readiness-check-in/` (spec.md, plan.md, research.md, data-model.md, contracts/check-in-api.md, quickstart.md)

**Tests**: Required by the Definition of Done and principle V; each test sits with the code it covers.

**Organization**: Phases follow the GitHub tasks in commit order: #111 (score and band), #110 (model and API), #112 (check-in on Today), #113 (readiness card). One pull request for the story, one commit per task, each ending in `(#<task>)`.

**Paths**: `B` = `backend/src/main/java/com/kinetiq/readiness`, `BT` = `backend/src/test/java/com/kinetiq/readiness`, `BR` = `backend/src/main/resources`, `F` = `frontend/src`, `FR` = `frontend/src/features/readiness`.

**User stories** (spec.md): US1 check in with three taps, US2 see readiness, US3 change answers until the first set, US4 skip, US5 privacy.

## Phase 1: Setup

- [X] T001 Commit `specs/109-readiness-check-in/` with the code of task #111, so the spec is reviewed in the story's pull request. `CONTEXT.md` needs no new term (check-in, readiness, readiness band are there). Before writing the migration run `git fetch` and `gh pr list --json number,files`, and use the next free `V<n>` (V19 at the time of writing)

## Phase 2: task #111, readiness score and band (US2)

**Goal**: the pure rule of data-model.md, "Readiness" and its table of examples.

- [X] T002 [US2] `B/domain/model/Answers.java`: record `(int sleep, int soreness, int stress)`; the compact constructor refuses any value outside "a whole number from 1 to 5" with `InvalidValueException` (`common.domain.exception`), naming the answer in the message only
- [X] T003 [US2] `B/domain/model/Readiness.java`: record `(int score, Band band)` with nested `enum Band { READY, TAKE_IT_EASIER, RECOVERY_DAY }` and `static Readiness of(Answers answers)`: `sum = (sleep − 1) + (5 − soreness) + (5 − stress)`, `score = Math.round(sum * 100 / 12.0)`, band `READY` when score ≥ 70, `TAKE_IT_EASIER` when 45–69, `RECOVERY_DAY` below 45. Javadoc names the mean of the three quarters it equals
- [X] T004 [US2] `BT/domain/model/ReadinessTest.java`: every one of the 125 combinations against the formula written as the mean of `(s − 1) / 4`, `(5 − x) / 4`, `(5 − y) / 4` times 100 (a parameterised test or one loop); named cases 2/4/3 → 33 `RECOVERY_DAY`, 4/2/1 → 83 `READY`, 5/1/1 → 100, 1/5/5 → 0; band edges sum 5 → 42 `RECOVERY_DAY`, sum 6 → 50 `TAKE_IT_EASIER`, sum 8 → 67 `TAKE_IT_EASIER`, sum 9 → 75 `READY`. `BT/domain/model/AnswersTest.java`: 0 and 6 refused for each answer
- [X] T005 `./gradlew check` passes (Docker running), including `HexagonalArchitectureTest`; commit `feat(backend): add readiness score and band (#111)`

## Phase 3: task #110, check-in model and API (US1, US3, US5)

**Goal**: `GET` and `PUT /api/v1/check-ins/today` (contract: `contracts/check-in-api.md`), one check-in per client per date, fixed once a set of today's session is logged.

- [X] T006 [US1] Migration `BR/db/migration/V21__create_check_ins.sql` as in data-model.md: `check_ins` (`id uuid` `pk_check_ins`, `client_id uuid not null` `fk_check_ins_client → accounts (id)`, `check_in_date date not null`, `sleep`, `soreness`, `stress smallint not null` each with `ck_check_ins_<answer> check (<answer> between 1 and 5)`, `saved_at timestamptz not null`, `created_at`, `updated_at timestamptz not null`, `version bigint not null`, `uq_check_ins_client_date unique (client_id, check_in_date)`); no score or band column
- [X] T007 [US1] Domain model in `B/domain/model/`: `CheckInId` (record over `UUID`, `random()`), `CheckIn` aggregate (`id`, `client` `AccountId`, `date` `LocalDate`, `answers`, `savedAt` `Instant`; `static give(client, date, answers, now)`, `change(answers, now)`, `readiness()` = `Readiness.of(answers)`; `@Getter`, no setters)
- [X] T008 [US1] Ports: `B/domain/output/CheckInRepository.java` (`Optional<CheckIn> findByClientAndDate(AccountId, LocalDate)`, `CheckIn save(CheckIn)`), `B/domain/output/TrainingProgress.java` (`boolean hasLoggedSet(AccountId client, LocalDate date)`, Javadoc: implemented by the feature that logs sets, so `readiness` does not depend on it); `B/domain/input/CheckInUseCase.java` (`TodayCheckIn today(AccountId client)`, `TodayCheckIn save(SaveCheckInCommand command)`, Javadoc with the refusals), `SaveCheckInCommand` record `(AccountId client, Answers answers)`, `TodayCheckIn` record `(LocalDate date, boolean open, CheckIn checkIn)`, `checkIn` null until the client checks in (like `TodayView.session`; `Optional` is not used for fields)
- [X] T009 [US3] Exceptions in `B/domain/exception/`: `NoSessionTodayException` (`check-in.no-session-today`, kind `CONFLICT`), `TrainingStartedException` (`check-in.training-started`, kind `CONFLICT`)
- [X] T010 [US1] `B/domain/service/CheckInService.java`: `today` calls `ViewOwnPlanUseCase.today(client)` (plan; `PlanNotFoundException` passes through), `open` = state `SESSION` and not `trainingProgress.hasLoggedSet(client, date)`, and the stored check-in of that date; `save` refuses in this order: no plan (passes through), `REST` or `FINISHED` → `NoSessionTodayException`, a logged set → `TrainingStartedException`; then `change`s the stored one or `give`s a new one with the clock's `now`, saves, and answers like `today`. A `// ponytail:` comment on the race of a set logged during a save (research R4). Wiring in `B/infrastructure/config/ReadinessConfig.java` with the clock bean the other features use
- [X] T011 [US1] JPA adapter in `B/infrastructure/output/`: `CheckInEntity` (table `check_ins`, extends `BaseEntity`, answers as `short`/`int` columns), `JpaCheckInRepository` (`findByClientIdAndCheckInDate`), `CheckInMapper` (hand-written like `SessionLogMapper`), `CheckInRepositoryImpl` (`@Repository`, `save` inserts or updates by id). A `// ponytail:` comment on two first saves racing into the unique constraint (research R7)
- [X] T012 [US3] `backend/src/main/java/com/kinetiq/sessionlog/infrastructure/output/SessionLogTrainingProgress.java`: `@Component` implementing `readiness.domain.output.TrainingProgress`; true when the client's session log of that date has at least one set log (an `exists` query on `JpaSessionLogRepository` joined to `set_logs`, not a full load)
- [X] T013 [US1] Web in `B/infrastructure/input/`: `CheckInController` at `/api/v1/check-ins/today` (`GET`, `PUT` with `@Valid`, both `200`; the client from `authentication.getName()` as in `TodayController`; Javadoc lists the codes), `SaveCheckInRequest` (`@NotNull @Min(1) @Max(5) Integer sleep, soreness, stress`), `TodayCheckInResponse` (`date`, `open`, `checkIn` nullable with `sleep`, `soreness`, `stress`, `score`, `band` enum `READY`/`TAKE_IT_EASIER`/`RECOVERY_DAY`, `savedAt`; required fields marked for the OpenAPI schema like the other responses), `CheckInWebMapper`
- [X] T014 [US5] `backend/src/main/java/com/kinetiq/common/infrastructure/config/SecurityConfig.java`: add `/api/v1/check-ins/**` to the role `CLIENT` rule beside `/api/v1/today/**` and `/api/v1/session-logs/**`
- [X] T015 [US1] Service test `BT/domain/service/CheckInServiceTest.java` (fake repository, fake `TrainingProgress`, fake `ViewOwnPlanUseCase`, fixed clock): no check-in reads `open` with none; saving stores one for the client's date and answers its readiness; saving again replaces (still one) and updates `savedAt`; the same answers twice give the same result; a rest day and a finished plan read `open: false` and refuse a save with `check-in.no-session-today`; no plan is `plan.not-found` for both; a logged set reads `open: false` and refuses a new and a changed check-in with `check-in.training-started`, leaving the stored answers unchanged; client B never reads client A's check-in of the same date; the date is `TodayView.date()` (the client's day, not the clock's UTC date)
- [X] T016 [US1] Adapter test `BT/infrastructure/output/CheckInRepositoryImplIT.java` (`@DataJpaTest` with Testcontainers, accounts inserted with SQL as in `SessionLogRepositoryImplIT`): stored and read back by client and date; a change keeps one row; the database refuses a second row for the same client and date, and an answer of 0 or 6. `backend/src/test/java/com/kinetiq/sessionlog/infrastructure/output/SessionLogTrainingProgressIT.java`: false without a session log, false for a started session with no set, true after one set, false for another client or another date
- [X] T017 [US5] Controller test `BT/infrastructure/input/CheckInControllerTest.java` (`@WebMvcTest`, use case mocked): `GET` and `PUT` `200` with the response shape (no `checkIn` when none); `400 validation.failed` with `errors` `{field: sleep, code: min}`, `max`, `not-null`; `400 request.invalid` for `2.5` and for text; `404 plan.not-found`; `409 check-in.no-session-today` and `check-in.training-started`; `PUT` without CSRF refused; `403` for a trainer and `401` for a visitor on both
- [X] T018 Demo data `BR/db/dev-data/R__demo_check_ins.sql`: Demo Client Daily (`00000000-0000-4000-8000-000000000012`) check-ins on `current_date - 1` (4/2/1), `- 2` (3/3/3) and `- 3` (2/4/3), fixed ids in the `…-4000-9800-…` range (check no other demo file uses it), `on conflict do nothing`; none for today; a header comment like the other demo files
- [X] T019 Regenerate `frontend/src/lib/api/schema.d.ts` (`npm run gen:api` with the backend running) and run `npm run check` in `frontend/`
- [X] T020 `./gradlew check` passes, including `HexagonalArchitectureTest` (no cycle: `sessionlog → readiness → plan`); follow `quickstart.md` step 5's request by hand; commit `feat(backend): add check-in model and api (#110)`

## Phase 4: task #112, check-in on the Today screen (US1, US4)

**Goal**: three radio groups of 1–5 that save on the third answer, and Skip (spec FR-010 to FR-014).

- [ ] T021 Before building: fetch current docs through context7 and `npx shadcn@latest docs toggle-group`; confirm in `F/components/ui/toggle-group.tsx` and Radix that `type="single"` items render `role="radio"` with `aria-checked`, the root accepts a `role` override, arrows move focus without choosing, and Space and Enter choose (research R9). If any of it fails, stop and report before building on another primitive
- [ ] T022 [US1] `FR/api/check-in.ts`: `useCheckIn()` (query `['check-in', 'today']` on `GET /api/v1/check-ins/today`, throws the problem detail like `useToday`) and `useSaveCheckIn()` (`PUT`, writes the answer into the query on success; on `check-in.training-started` invalidates it); types from `components['schemas']['TodayCheckInResponse']`
- [ ] T023 [US1] `FR/components/answer-group.tsx`: one question as `ToggleGroup type="single"` with `role="radiogroup"`, `aria-labelledby` its visible question and `aria-describedby` its scale line ("1 bad · 5 great", "1 none · 5 very sore", "1 calm · 5 high"); five items named `1` to `5`, each at least 48 × 48 px (`size-12` or the touch control height); the chosen one has `aria-checked` and a visible cue that is not hue alone: the ink fill (`bg-primary text-primary-foreground`) with at least 3:1 contrast against the unchosen items, as the board's chips do; an empty `onValueChange` is ignored so a second tap does not clear the answer; while saving, items stay focusable, get `aria-disabled="true"` and ignore choices (never `disabled`, which would drop focus to the page)
- [ ] T024 [US1] `FR/components/check-in-card.tsx`: shadcn `Card` with title "How do you feel today?" (a heading with `tabIndex={-1}` for focus), the three groups, footer "Skip" and "Saves after the third answer". Choosing a value from the click or key handler saves when all three have one (never from an effect); pending: "Saving…" in a `role="status"` line, answers kept; failure: answers kept, `Alert` with `t(errorMessageKey(error))` and "Try again" sending the same answers. Props for the change mode of #113 (`initial` answers, `onDone`, `onCancel`) are added in #113, not here
- [ ] T025 [US4] `FR/skip.ts`: `isSkipped(date)`, `skip(date)`, `unskip()` on `localStorage['kinetiq.checkIn.skippedOn']`, every access in `try/catch` (no storage: the skip lives in component state only)
- [ ] T026 [US1] `FR/components/readiness-section.tsx`: uses `useCheckIn()`; skeleton while loading; a failed read shows a small `Alert` with "Try again" and nothing else hidden; with `open` and no check-in: the check-in card, or when skipped for that `date` the line "Check-in skipped. Your session is as planned." with "Check in"; with a check-in: a placeholder showing the score, band and answers as text until #113 brings the card; otherwise nothing
- [ ] T027 [US1] `F/features/plan/pages/today-page.tsx`: optional prop `beforeSession?: ReactNode` rendered above the session card only when `state === 'SESSION'` (Javadoc says what goes there); `F/app/today-route.tsx`: `TodayRoute` renders `<TodayPage beforeSession={<ReadinessSection />} />`; `F/app/router.tsx`: `/app/today` lazily loads `app/today-route`
- [ ] T028 [US1] Strings `readiness.checkIn.*` (title, the three questions and scale lines, "Skip", "Saves after the third answer", "Saving…", "Try again", skipped line, "Check in") and `errors.check-in.training-started`, `errors.check-in.no-session-today` in `F/locales/en.json` and `F/locales/pl.json`; Polish written for this, sentence case, no exclamation marks
- [ ] T029 [US1] Tests `FR/components/check-in-card.test.tsx` and `FR/components/readiness-section.test.tsx` (`userEvent`, queries by role): two answers send nothing, the third sends one `PUT` with the three values; arrow keys move focus within a group without choosing and without sending, Space chooses; a second tap on the chosen value keeps it; pending shows "Saving…" and the focused choice keeps focus; a failed save keeps the answers and "Try again" resends them; Skip shows the skipped line, sends nothing, survives a remount for the same date and not for another date; "Check in" brings the card back with nothing chosen; `open: false` and no check-in renders nothing. `F/features/plan/pages/today-page.test.tsx`: `beforeSession` shows on a session day and not on a rest day or a finished plan
- [ ] T030 [US1] `npm run check` passes; in the built-in browser as Demo Client Daily on a training day: check in by tap and by keyboard, skip and undo, a failed save (backend stopped); the chosen value is clear in light and dark without relying on hue (contrast of chosen against unchosen at least 3:1), and focus stays on the choice while saving; light and dark, 375 px and 320 px, EN and PL; console clean; commit `feat(frontend): add check-in to the today screen (#112)`

## Phase 5: task #113, readiness card (US2, US3)

**Goal**: the card with score, band, line and answers, with "Change answers" until the first set (spec FR-015 to FR-020).

- [ ] T031 [US2] `FR/components/readiness-card.tsx`: `Card` with label "Readiness", a band block on `bg-ready-high-muted` / `bg-ready-mid-muted` / `bg-ready-low-muted` with a dot in `bg-ready-high` / `-mid` / `-low` (aria-hidden), the score in `num` at the metric size, the band name as a heading (`tabIndex={-1}`), the band's line (Ready "Train as planned."; Take it easier "Same plan. Stop a rep earlier."; Recovery day "Same plan. Stop two reps earlier."), and "Sleep {{sleep}} · soreness {{soreness}} · stress {{stress}}"; never `destructive` or a red token; the score and band inside an `aria-live="polite"` region. Follow the "Today · ready" board's layout with Kinetiq tokens, without restyling the design system
- [ ] T032 [US3] "Change answers" (link-style `Button`) only while `open`; when not open, the line that answers are fixed once training has started. Change mode: `CheckInCard` gains `initial`, `onDone` and `onCancel`: the saved answers are chosen, the footer shows "Cancel" and "Done" instead of "Skip" and the saving line, choosing saves nothing, "Done" saves the chosen answers with the pending and failure behaviour of T024, "Cancel" returns to the card unchanged
- [ ] T033 [US2] `FR/components/readiness-section.tsx`: replace the placeholder with `ReadinessCard`; switch between card and check-in for "Change answers", "Done" and "Cancel"; move focus to the heading of whichever card appears (check-in title, band heading) only when the view switches because of the client (third answer saved, "Done", "Cancel", "Change answers", "Check in"), never when Today first loads; a `check-in.training-started` refusal shows its translated message and the refetched card without "Change answers"
- [ ] T034 [US2] Strings `readiness.card.*` (label, three band names and lines, answers line, "Change answers", fixed line, "Done", "Cancel") in `en.json` and `pl.json`
- [ ] T035 [US2] Tests `FR/components/readiness-card.test.tsx` and additions to `readiness-section.test.tsx`: 83 / Ready / "Train as planned." and the answers line for 4/2/1; each band's name and line; no "Change answers" once training has started, with the fixed line; "Change answers" opens the check-in with the saved answers chosen, choosing sends nothing, "Done" sends one `PUT` with the new answers and shows the new card; "Cancel" sends nothing and shows the old card; focus moves to the new card's heading after a switch and stays where it is on first load; a `check-in.training-started` answer shows the card without "Change answers"
- [ ] T036 [US2] `npm run check` passes; in the built-in browser: each band (4/2/1, 3/3/3, 2/4/3) in light and dark, no red, contrast of the band text on each `-muted` surface checked; change and cancel; log a set at `/app/session` and return: no "Change answers"; 375 px and 320 px, EN and PL; review with the ui-ux-pro-max skill for WCAG 2.2 AA and UX; screenshots; commit `feat(frontend): add readiness card (#113)`

## Phase 6: Close the story

- [ ] T037 Both gates pass (`./gradlew check`, `npm run check`); `quickstart.md` followed end to end; README demo table updated only if it lists demo states per client. Push the branch and open the pull request only after the user agrees (title `feat: let the client check in and see readiness (#109)`, body with `Closes #109`, `Closes #110`, `Closes #111`, `Closes #112`, `Closes #113`); merge with rebase or a merge commit so each task keeps its commit

## Dependencies

- T001 first. #111 (T002–T005) before #110: the response carries `Readiness`.
- Inside #110: T006 and T007 before T011; T008 and T009 before T010; T010 before T013 and T015; T012 needs only T008; T013 before T014 and T017; T019 after T013.
- Backend (T002–T020) before frontend: the frontend types are generated from it.
- T021 before T023. Inside #112: T022 before T024 and T026; T023 before T024; T025 before T026; T026 before T027.
- #113 builds on #112's section and card.

## Parallel opportunities

- T002 and T003 (different files), then T004.
- T006, T007 and T009 once #111 is in; T012 beside T010 and T011.
- T015, T016 and T017 once T010, T011 and T013 exist; T018 at any point in #110.
- T022, T023 and T025 in parallel (different files) after T021.
- T031 beside T034.

## Implementation strategy

Task order, one commit per task when its checks pass: #111, #110, #112, #113. US1 with US2's placeholder (end of #112) already lets a client check in on Today; #113 completes US2 and US3. Nothing is pushed until the user agrees. Lighter targets on a recovery day (#117), the trainer's view (#122) and offline check-in (#114) are not here.
