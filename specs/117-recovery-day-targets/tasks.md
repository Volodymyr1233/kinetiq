---
description: "Tasks for story #117, Today's targets are lightened on a recovery day (tasks #118 to #121)"
---

# Tasks: Today's targets are lightened on a recovery day

**Input**: `specs/117-recovery-day-targets/` (spec.md, plan.md, research.md, data-model.md, contracts/targets-api.md, quickstart.md)

**Tests**: Required by the Definition of Done and principle V. Each test sits with the code it covers.

**Organization**: Phases follow the GitHub tasks in commit order: #118 (adjustment rules), #119 (apply, revert and undo API), #120 (adjusted session view), #121 (use original targets with Undo). One pull request covers the story, with one commit per task, each ending in `(#<task>)`.

**Paths**:

| Alias | Path |
|---|---|
| `B` | `backend/src/main/java/com/kinetiq/readiness` |
| `BT` | `backend/src/test/java/com/kinetiq/readiness` |
| `S` | `backend/src/main/java/com/kinetiq/sessionlog` |
| `ST` | `backend/src/test/java/com/kinetiq/sessionlog` |
| `BR` | `backend/src/main/resources` |
| `F` | `frontend/src` |
| `FR` | `frontend/src/features/readiness` |

**User stories** (spec.md):

| Label | Story |
|---|---|
| US1 | lighter targets on a recovery day |
| US2 | log against them |
| US3 | use the original targets, with Undo |
| US4 | changing the check-in recomputes |
| US5 | privacy |

## Phase 1: Setup

- [X] T001 Commit `specs/117-recovery-day-targets/` with the code of task #118, so the spec is reviewed in the story's pull request. `CONTEXT.md` needs no new term ("Adjustment" is there). Before writing the migration, run `git fetch` and `gh pr list --json number,files`, and use the next free `V<n>` (V22 at the time of writing).

## Phase 2: task #118, adjustment rules (US1)

**Goal**: the pure rule of data-model.md "Adjustment" and research R3 to R5, unit-tested without Spring.

- [X] T002 [US1] `B/domain/model/Target.java`: record `(BigDecimal loadKg, int amount, int rpe)`. `loadKg` is `null` for no load, otherwise kept at scale 1. `B/domain/model/Planned.java`: record `(Target target, Measure measure)`, with `Measure` from `exercise.domain.model`, both required.
- [X] T003 [US1] `B/domain/model/Cause.java`: enum `SLEEP, SORENESS, STRESS`, declared in that order. Javadoc gives the thresholds: sleep 1–2, soreness 4–5, stress 4–5.
- [X] T004 [US1] `B/domain/model/Adjustment.java`, record `(List<Target> lighter, List<Cause> causes, boolean inUse)` with defensive copies.
  - `static Target lighter(Target planned, Measure measure, WeightUnit unit)`, with `WeightUnit` from `account.domain.model`:
    - With a load, kg: `load × 0.9`, rounded to a multiple of 0.5 with an exact half going lighter (`BigDecimal`, `RoundingMode.HALF_DOWN` on `× 2`, then `/ 2`).
    - With a load, lb: convert kg to lb (`/ 0.45359237`), `× 0.9`, round to a whole pound `HALF_DOWN`, convert back and round to 0.1 kg `HALF_UP`.
    - Either unit: keep the planned load when the result is not lighter, or is below 0.5 kg (1 lb).
    - The amount stays when there is a load.
    - Without a load: `REPS` → `max(1, amount − 1)`; `SECONDS` → `max(1, floor(amount × 0.9))`.
    - RPE: `max(5, rpe − 1)`.
  - `static Optional<Adjustment> of(Readiness readiness, Answers answers, TargetChoice choice, List<Planned> entries, WeightUnit unit)`: empty unless the band is `RECOVERY_DAY` and at least one lighter target differs from its planned one. `causes` comes from the answers in enum order. `inUse` is `choice == LIGHTER`. (It takes `boolean inUse`, which the service derives from the stored choice (T009).)
  - `Target inUse(int position, Target planned)`: `lighter.get(position)` when `inUse`, otherwise `planned`.
  - Javadoc names the rule and the half-down rounding.
- [X] T005 [US1] `BT/domain/model/AdjustmentTest.java`, `@Nested` per method:
  - kg: 100 → 90, 80 → 72, 102.5 → 92 (92.25 is a half), 20 → 18, 2.5 → 2, 0.5 → 0.5 (would fall below the step).
  - lb: a load whose 90% lands on a half pound goes lighter; a load stays when the whole pound rounds back to it.
  - RPE: 8 → 7, 5 → 5.
  - Reps without a load: 12 → 11, 5 → 4, 1 → 1. Seconds: 45 → 40, 30 → 27, 60 → 54, 5 → 4, 1 → 1.
  - With a load, reps and seconds stay.
  - `of`: empty for `READY` and `TAKE_IT_EASIER`; empty for `RECOVERY_DAY` when every entry is at its floor; causes for 2/4/3 → `[SLEEP, SORENESS]`, 1/5/5 → all three, 3/4/3 → `[SORENESS]`; `inUse` follows the choice; `inUse(position, planned)` returns the planned target when not in use.
- [X] T006 Run `./gradlew check` (Docker running), which must pass, including `HexagonalArchitectureTest`: readiness may now depend on `account` and `exercise` domain models, with no cycle. Commit `feat(backend): add recovery day adjustment rules (#118)`.

## Phase 3: task #119, apply, revert and undo API (US1, US2, US3, US4, US5)

**Goal**: the choice stored on the check-in, today's adjustment in `GET /api/v1/check-ins/today`, `PUT /api/v1/check-ins/today/targets`, and logged sets copying the targets in use. Contract: `contracts/targets-api.md`.

- [ ] T007 [US3] Migration `BR/db/migration/V22__add_check_in_target_choice.sql`:

  ```sql
  alter table check_ins
      add column target_choice varchar(8) not null default 'LIGHTER',
      add constraint ck_check_ins_target_choice check (target_choice in ('LIGHTER', 'ORIGINAL'));
  ```

  A one-line comment says the adjusted targets are derived, so only the choice is stored.
- [ ] T008 [US3] `B/infrastructure/output/CheckInEntity.java`: `@Enumerated(EnumType.STRING) TargetChoice targetChoice`. `CheckInMapper`: map it both ways.
- [ ] T009 [US3] `B/domain/model/TargetChoice.java`: enum `LIGHTER, ORIGINAL`. `CheckIn`:
  - New field `targetChoice`, set to `LIGHTER` by `give`, and a rebuild constructor that takes it.
  - `change(answers, now)` also sets `LIGHTER` when the answers differ; the same answers change nothing.
  - `choose(TargetChoice)`: the same choice changes nothing.
  - Update `Adjustment.of` to take `TargetChoice`.
  - `BT/domain/model/CheckInTest.java`: different answers reset `ORIGINAL` to `LIGHTER`; the same answers keep `ORIGINAL`.
- [ ] T010 [US3] `B/domain/exception/NoAdjustmentException.java`: code `check-in.no-adjustment`, kind `CONFLICT`, following `TrainingStartedException`. The common handler maps `DomainException` by kind, so no handler change is needed; confirm it.
- [ ] T011 [US1] `B/domain/input/`:
  - `TodayCheckIn` gains `Adjustment adjustment`, `null` without one, documented like `checkIn`.
  - `ChooseTargetsCommand` record `(AccountId client, TargetChoice choice)`.
  - `CheckInUseCase` gains `TodayCheckIn choose(ChooseTargetsCommand)` and `Optional<Adjustment> adjustmentOn(AccountId client, LocalDate date, List<Planned> entries)`.
  - Javadoc lists the refusals: `PlanNotFoundException`, `NoSessionTodayException`, `TrainingStartedException`, `NoAdjustmentException`.
- [ ] T012 [US1] `B/domain/service/CheckInService.java`: add `CurrentAccountUseCase accounts`; the unit comes from the account's settings.
  - `today` and `save`: when there is a check-in and the state is `SESSION`, build `List<Planned>` from `today.session().entries()`, with measures from `today.library()`, and set `adjustment = Adjustment.of(…)`. Both use one private helper, so that a saved check-in answers with its adjustment at once (SC-001).
  - `choose`, refusing in this order:
    1. No plan (passes through).
    2. `REST` or `FINISHED` → `NoSessionTodayException`.
    3. A logged set → `TrainingStartedException`.
    4. No check-in, or no adjustment → `NoAdjustmentException`.

    Then `checkIn.choose(choice)`, save, and answer as `today`.
  - `adjustmentOn`: the stored check-in of that date and `Adjustment.of` over the given entries, empty without a check-in.
  - Add a `// ponytail:` comment on a switch racing the first logged set (research R7).
  - Wire it in `B/infrastructure/config/ReadinessConfig.java`.
- [ ] T013 [US3] `BT/domain/service/CheckInServiceTest.java`, with the existing fakes plus a fake `CurrentAccountUseCase`:
  - `today`: carries the adjustment on 2/4/3 and none on 4/2/1; the lb client's lighter load is rounded in pounds.
  - `save`: saving 2/4/3 answers with the adjustment; saving 4/2/1 after it answers with none.
  - `choose`: `ORIGINAL` then `LIGHTER` is stored; a repeat gives the same result.
  - `choose` refusals: `check-in.no-adjustment` without a check-in and on 4/2/1; `check-in.training-started` after a logged set (choice unchanged); `check-in.no-session-today` on a rest day.
  - Changed answers reset the choice; the same answers keep it.
  - Client B's choice never changes A's.
- [ ] T014 [US1] Web, `B/infrastructure/input/`:
  - `ChooseTargetsRequest`: `@NotNull TargetChoice choice`.
  - `CheckInController`: `@PutMapping("/targets")`, answering `TodayCheckInResponse`, with Javadoc listing the codes of the contract.
  - `TodayCheckInResponse.CheckInResponse` gains `AdjustmentResponse adjustment`, left out when `null`, with `@NotNull boolean inUse`, `@NotNull List<Cause> causes` and `@NotNull List<AdjustedTargetResponse> entries`. Each entry has `@NotNull int position`, `BigDecimal loadKg` (left out when `null`), `@NotNull int amount`, `@NotNull int rpe`.
  - `CheckInWebMapper` maps it.
- [ ] T015 [US5] `BT/infrastructure/input/CheckInControllerTest.java`: `PUT /targets` returns `200` with the body; `400 validation.failed` for a missing and for an unknown `choice`; `404 plan.not-found`; `409 check-in.no-session-today`, `check-in.no-adjustment` and `check-in.training-started`; refused without CSRF; `403` for a trainer, `401` for a visitor; `GET` shows `adjustment` when present and leaves it out otherwise.
- [ ] T016 [US1] `BT/infrastructure/output/CheckInRepositoryImplIT.java`: `target_choice` is stored and read back; an existing row reads `LIGHTER`; the database refuses an unknown value.
- [ ] T017 [US2] `S/domain/model/SessionLog.java`: `logSet` takes the targets in use for the place (`BigDecimal targetLoadKg, int targetAmount, int targetRpe`, or one small record of the sessionlog's own) and copies them into the new `SetLog`. A change of a logged set keeps its copy. Update the Javadoc. Update `ST/domain/model/SessionLogTest.java`: a new set copies the given targets; a change keeps the first copy.
- [ ] T018 [US2] `S/domain/input/StartedSession.java`: add `Adjustment adjustment` (`null` without one, documented). `S/domain/service/SessionLogService.java`: add `CheckInUseCase checkIns`.
  - `start` and `find` set `adjustment = checkIns.adjustmentOn(client, log.getDate(), planned(log, library))`, where `planned` maps each `LoggedEntry` to `Planned(new Target(loadKg, amount, rpe), measure)`.
  - `logSet` computes the same adjustment and passes `adjustment.inUse(position, plannedTarget)` (or the planned target) to `SessionLog.logSet`.
  - Wire it in `S/infrastructure/config/SessionLogConfig.java`.
- [ ] T019 [US2] `ST/domain/service/SessionLogServiceTest.java`, with a fake `CheckInUseCase`: on an adjusted date a set logged as planned stores the lighter targets as its copy; with `ORIGINAL`, the planned ones; `start` carries the adjustment; without a check-in nothing changes from today. FR-013: after an adjusted set is logged, the session log's `LoggedEntry` copy still holds the planned targets (the adjustment never writes the planned copy). The rest of FR-013 is structural: readiness has no port that writes a plan, and `HexagonalArchitectureTest` keeps it so.
- [ ] T020 [US2] `S/infrastructure/input/SessionLogResponse.java`: `LoggedEntryResponse` gains `TargetResponse planned`, left out when `null`. `SessionLogWebMapper`:
  - A logged set's `target` comes from its `SetLog` copy (`targetLoadKg`, `targetAmount`, `targetRpe`).
  - An unlogged set's `target` is the target in use (`StartedSession.adjustment`).
  - `planned` is set only when the target in use differs from the entry's planned one.
  - `ClientSessionLog` (trainer) has no adjustment: unlogged sets show planned targets and `planned` is never set.

  `ST/infrastructure/input/SessionLogControllerTest.java`: `planned` is present on an adjusted entry and absent otherwise. `ClientTrainingControllerTest`: a logged set shows its copy.
- [ ] T021 [US5] Ownership test in `ST/infrastructure/input/SessionLogControllerTest.java` or `SessionLogServiceTest`: client B asking for A's adjusted session log by id gets `404 session-log.not-found`.
- [ ] T022 Demo data:
  - `BR/db/dev-data/R__demo_check_ins.sql`: Demo Client Daily keeps 2/4/3 on `current_date - 3`; add `target_choice` only if the insert lists columns that need it (the default is `LIGHTER`).
  - `BR/db/dev-data/R__demo_session_logs.sql`: give Demo Client Daily a finished session log on `current_date - 3`, the date of the 2/4/3 check-in, whatever its weekday. Copy its entries from one of the client's plan sessions, and log every set at the lighter values of the rule, with `set_logs.target_load_kg`, `target_amount` and `target_rpe` holding those lighter values. Use fixed ids in an unused range (check the other demo files), with `on conflict do nothing`. Today stays without a check-in.
- [ ] T023 Start the backend and regenerate `frontend/src/lib/api/schema.d.ts` (`npm run gen:api`). Add `errors.check-in.no-adjustment` to `F/locales/en.json` ("Today's targets are as planned, so there is nothing to switch.") and `pl.json` ("Dzisiejsze cele są zgodne z planem, więc nie ma czego przełączać."). Run `npm run check` in `frontend/`.
- [ ] T024 Run `./gradlew check`, which must pass (`HexagonalArchitectureTest`: `sessionlog → readiness → plan`, no cycle). Try quickstart scenario 6 by hand. Commit `feat(backend): add today's target adjustment api (#119)`.

## Phase 4: task #120, adjusted session view (US1, US2, US4)

**Goal**: Today and the logger show old → new and the reason (spec FR-016 to FR-019). Read-only; the switch is #121.

- [ ] T025 Before building: fetch current docs through context7 for `sonner` (the `toast` `action` option) and for `Intl.ListFormat`, and run `npx shadcn@latest docs badge`. Open the board "Client · Today recovery day" and the design system (adjusted tokens, `.strike`, `.card-adjusted`).
- [ ] T026 [P] [US1] `F/components/ui/badge.tsx`: add the design-system variant `adjusted` (`bg-adjusted-muted text-adjusted`), with a one-line comment as the other variants have.
- [ ] T027 [P] [US1] `F/components/target-change.tsx`: `TargetChange({ planned, today, label })` takes formatted strings and draws `<s className="text-muted-foreground">{planned}</s>`, an `ArrowRightIcon` with `aria-hidden`, and `<span className="text-foreground font-medium">{today}</span>`, all in `num`. The visible part is `aria-hidden`, and an `sr-only` text carries `t('targets.change', { label, planned, today })` ("Load 100 kg, today 90 kg" / "Obciążenie 100 kg, dziś 90 kg"). When `planned === today`, it renders `today` only. `F/components/target-change.test.tsx` covers both.
- [ ] T028 [US1] `F/features/plan/components/entry-row.tsx`: an optional `today?: Target` (`components['schemas']['AdjustedTargetResponse']`). For load, amount and RPE, render `TargetChange` when `today` differs from the entry, and as now otherwise. "No load" stays words. Units come from `useWeightUnit()` and `fromKg`.
- [ ] T029 [US1] `F/features/plan/pages/today-page.tsx`: an optional prop `adjusted?: { targets?: AdjustedTarget[]; mark: ReactNode; reason?: ReactNode; note?: ReactNode }`. When it is set:
  - `mark` sits on a row above the name, the way the board does it.
  - `reason` sits under the name.
  - `note` sits at the bottom of the card, under "Start session", as the board does.
  - Only while `targets` is set: the session section gets the adjusted border (`border-[color-mix(in_oklch,var(--adjusted)_45%,var(--border))]`), and each `EntryRow` gets `today={targets[position]}`.

  Javadoc says `app/` fills it from readiness. `today-page.test.tsx`: with `targets`, the struck planned value and the new one are shown; with `adjusted` but no `targets`, only the planned values; without `adjusted`, the page is as now.
- [ ] T030 [US1] `FR/components/adjustment-reason.tsx`:
  - The sentence `t('readiness.adjustment.reason', { causes })`, where `causes` is the translated `readiness.adjustment.cause.<CAUSE>` phrases joined with `new Intl.ListFormat(i18n.language, { type: 'conjunction' })`, first letter upper-cased with `toLocaleUpperCase(i18n.language)`.
  - `FR/components/adjustment-note.tsx`: a muted, centred line `t('readiness.adjustment.planUnchanged')`, passed as `note`.
  - EN: `cause` `{SLEEP: "poor sleep", SORENESS: "high soreness", STRESS: "high stress"}`, `reason` "{{causes}}, so today's targets are lighter.", `planUnchanged` "Only today's targets change. Your plan stays the same.".
  - PL: `{SLEEP: "słaby sen", SORENESS: "duża bolesność mięśni", STRESS: "wysoki stres"}`, "{{causes}}, więc dzisiejsze cele są lżejsze.", "Zmieniają się tylko dzisiejsze cele. Twój plan pozostaje bez zmian.".
  - `FR/components/adjustment-mark.tsx` (badge only in this task): `<Badge variant="adjusted">` `t('readiness.adjustment.badge')` ("Adjusted for today" / "Dostosowane na dziś").
- [ ] T031 [US1] `F/app/today-route.tsx`: read `useCheckIn()`. When `checkIn.adjustment?.inUse`, pass `adjusted={{ targets, mark: <AdjustmentMark …/>, reason: <AdjustmentReason causes=… />, note: <AdjustmentNote /> }}`, otherwise nothing. `ReadinessSection` keeps its place.
- [ ] T032 [US1] `FR/components/readiness-card.tsx`:
  - With `checkIn.adjustment?.inUse`, the band line reads `t('readiness.card.lighter')` ("Today's targets are lighter." / "Dzisiejsze cele są lżejsze.").
  - With `checkIn.adjustment` present and `!open`, the fixed line reads `t('readiness.card.fixedWithTargets')` ("Answers and targets are fixed once training has started." / "Po rozpoczęciu treningu odpowiedzi i cele są już stałe.").
  - Otherwise, as now. `readiness-card.test.tsx` covers the three lines.
- [ ] T033 [US2] `F/features/sessionlog/pages/logger-page.tsx`: under each exercise name with `entry.planned`, one muted line of `TargetChange` for every target that differs (load, amount with its measure, RPE). The set rows already show the target in use. `logger-page.test.tsx`: an adjusted entry shows planned → today; ticking a set as planned sends the lighter values.
- [ ] T034 [US4] Move the session log query key out of the sessionlog feature: `export const SESSION_LOGS_KEY = ['session-log']` in `F/lib/query-keys.ts`, used by `F/features/sessionlog/api/session-log.ts` in place of its local `SESSION_LOGS`. `FR/api/check-in.ts`: `useSaveCheckIn` also invalidates `SESSION_LOGS_KEY` on success, so the logger shows recomputed targets. No feature imports another.
- [ ] T035 Run `npm run check`, which must pass. Commit `feat(frontend): show adjusted targets on today and in the logger (#120)`.

## Phase 5: task #121, use original targets with Undo (US3)

**Goal**: one-tap switch both ways, an Undo message, no dialog, and the fixed state (spec FR-020 to FR-024).

- [ ] T036 [US3] `FR/api/check-in.ts`: `useChooseTargets()`, a `PUT /api/v1/check-ins/today/targets` with `{ choice }`.
  - `onMutate`: cancel the check-in query, keep the previous value, and set `checkIn.adjustment.inUse` to `choice === 'LIGHTER'`.
  - `onError`: restore the previous value; on `check-in.training-started`, invalidate.
  - `onSuccess`: `setQueryData` with the answer, and invalidate `SESSION_LOGS_KEY` (T034).
- [ ] T037 [US3] `F/app/today-route.tsx`: pass `adjusted` whenever `checkIn.adjustment` is present, not only when it is in use. When `inUse` is false, leave out `targets`, `reason` and `note`, so `today-page.tsx` shows the planned targets with the plain border (T029 already allows this). `today-page.test.tsx`: with the original targets in use, only planned values are shown, which covers the visible half of FR-013.
- [ ] T038 [US3] `FR/components/adjustment-mark.tsx`, props `{ adjustment, open }`:
  - In use: the badge, plus, while `open`, `Button variant="link" size="touch" className="px-0"` labelled `t('readiness.adjustment.useOriginal')` ("Use original targets" / "Użyj celów z planu").
  - Not in use: `t('readiness.adjustment.useLighter')` ("Use lighter targets" / "Użyj lżejszych celów") while `open`, and no badge.
  - Use original: `mutate('ORIGINAL')` and `toast(t('readiness.adjustment.restored'), { action: { label: t('readiness.adjustment.undo'), onClick: () => mutate('LIGHTER') } })` ("Original targets restored" / "Przywrócono cele z planu"; "Undo" / "Cofnij").
  - Use lighter: `mutate('LIGHTER')`, with no message.
  - On error: `toast.error(t(errorMessageKey(error)))`, unless the code is `check-in.training-started` (the refetched card says why).
  - Focus moves to the button that replaces the tapped one (ref plus effect after a switch the client caused).
  - An `sr-only` `role="status"` announces `t('readiness.adjustment.announceLighter' | 'announceOriginal')` ("Lighter targets in use." / "Używasz lżejszych celów."; "Original targets in use." / "Używasz celów z planu.").
  - No dialog.
- [ ] T039 [US3] `FR/components/adjustment-mark.test.tsx`, with `userEvent` and a mocked `api`:
  - "Use original targets" shows planned targets at once and the "Undo" message; "Undo" brings the lighter targets back.
  - "Use lighter targets" switches back with no message.
  - A failed `PUT` returns to the stored targets with a translated message.
  - Neither switch is shown when `open` is false.
  - Focus is on the new switch after each tap.
- [ ] T040 [US3] Check the `Toaster` in `F/app/app.tsx` at 375 px: the message must not cover the tab bar or "Start session". If it does, set the client area's `offset` or `mobileOffset` per the sonner docs fetched in T025, without restyling.
- [ ] T041 Run `npm run check`, which must pass. Commit `feat(frontend): switch between lighter and original targets with undo (#121)`.

## Phase 6: Polish and verification

- [ ] T042 Run quickstart.md scenarios 1 to 10 in the built-in browser at 375 px (and 1280 px for scenario 8's trainer logs), in light and dark, EN and PL: no console errors, no sideways scroll at 375 and 320 px, every action 48 px, visible focus, struck values read with their new value by the accessibility tree, nothing red. Check that the Undo message is reachable and operable by keyboard alone (FR-022), and that focus stays on the new switch. Take screenshots for the pull request.
- [ ] T043 Review with the `ui-ux-pro-max` skill for WCAG 2.2 AA and UX: the contrast of `text-adjusted` on `bg-adjusted-muted` in both themes, toast timing with the persistent switch, and focus order. Fix findings without restyling the design system.
- [ ] T044 Run `/speckit-converge`, then the backend `./gradlew check` and the frontend `npm run check` one last time. Open the story's pull request: `feat: lighten today's targets on a recovery day`, with `Closes #117`, `Closes #118`, `Closes #119`, `Closes #120`, `Closes #121`. Recheck the migration number against open pull requests before pushing.

## Dependencies

- Phases run in order: #118 → #119 → #120 → #121. Each later phase builds on the types and API of the one before it.
- Within #119: T007 to T010 come before T011 and T012; T017 comes before T018; T018 comes before T020; T023 needs the backend running with T014 done.
- Within #120: T026 and T027 can run in parallel ([P]); T028 to T031 build on them.
- #121 changes `today-route.tsx` and `today-page.tsx`, which #120 created, so it comes after #120.

## Parallel examples

- #118: T002 and T003 touch different files.
- #120: T026 (badge) and T027 (TargetChange) are independent.
- Tests and implementation of one file go together; nothing else is safely parallel, because the phases share files.

## Implementation strategy

- MVP: #118 and #119. Lighter targets are computed, stored, logged and switchable through the API (US1, US2, US3 and US5 on the backend).
- Then #120 makes them visible (US1, US2, US4), and #121 adds the one-tap switch with Undo (US3).
- Each phase ends green (`./gradlew check`, `npm run check`) and with its own commit.
