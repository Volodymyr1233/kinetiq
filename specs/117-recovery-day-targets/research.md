# Research: Today's targets are lightened on a recovery day

All unknowns of the plan's Technical Context are resolved here; nothing is left as NEEDS CLARIFICATION.

## R1. Which feature owns the adjustment

- **Decision**: `readiness`. The rule (`Adjustment`), the client's choice (on `CheckIn`) and the switch endpoint live there. `GET /api/v1/check-ins/today` carries today's adjustment; `sessionlog` asks `CheckInUseCase` for the adjustment of a session's date.
- **Rationale**: Readiness already depends on `plan` (`ViewOwnPlanUseCase.today`) and `sessionlog` already depends on readiness (`TrainingProgress`). Putting the adjustment in `plan`'s Today answer would make `plan → readiness`, a cycle that `HexagonalArchitectureTest` refuses; a port in `plan` implemented by readiness would loop at run time (`plan.today → readiness → plan.today`). Readiness also owns the inputs: band, answers and the "fixed once a set is logged" rule.
- **Alternatives**: a new feature `adjustment` (one more feature for one rule and one column; rejected by constitution VI); computing in the frontend (refused by FR-006 and constitution IV).

## R2. Store the adjusted targets, or derive them

- **Decision**: Store only the choice, as `check_ins.target_choice` (`LIGHTER` default, `ORIGINAL`). Derive the lighter targets on every read from the planned targets (the plan's session for Today, the session log's copy for the logger), the check-in and the client's unit. A set log copies the targets in use when it is logged.
- **Rationale**: Until the first set the answers, the plan and the unit can all change, and each would leave stored targets stale. After the first set the check-in and the choice are fixed (spec 109), so the derived targets no longer move; the set logs carry the record. No new table, no sync code.
- **Alternatives**: a `today_adjustments` table with one row per entry (needs rewriting on every check-in save, plan save and unit change); storing lighter values on `LoggedEntry` at session start (the session can start before the check-in changes).

## R3. Where the rule sits and what it takes

- **Decision**: `readiness.domain.model.Adjustment.of(CheckIn, List<Planned>, WeightUnit)` returns `Optional<Adjustment>`: empty unless the band is `RECOVERY_DAY` and at least one target changes. `Planned` pairs a readiness-owned `Target(loadKg, amount, rpe)` with the exercise's `Measure`. Readiness reads `WeightUnit` from `account.domain.model` and `Measure` from `exercise.domain.model`; neither depends on readiness, so no cycle.
- **Rationale**: One pure function, unit-testable without Spring (FR-006), fed the same way from Today (plan session + library) and from the logger (session log copy + library).

## R4. Rounding

- **Decision**:
  - Kilograms: `0.9 × load`, rounded to a multiple of 0.5 with `RoundingMode.HALF_DOWN` on `load × 0.9 × 2`, then `/ 2`.
  - Pounds: convert the planned kg to lb (`kg / 0.45359237`), `× 0.9`, round to a whole pound `HALF_DOWN`, convert back to kg and round to 0.1 kg `HALF_UP`, the scale every load has.
  - Then: if the result is not lighter than planned or is below the smallest step (0.5 kg; 1 lb as kg), keep the planned load.
  - Seconds: `floor(0.9 × amount)`, at least 1. Repetitions: `amount − 1`, at least 1. RPE: `max(5, rpe − 1)`. Amount changes only when there is no load.
- **Rationale**: Clarifications Q1 and Q5. `HALF_DOWN` gives the lighter value on an exact half (102.5 → 92). `BigDecimal` keeps the arithmetic exact; the planned load is already scale 1.
- **Alternatives**: `Math.round` as the board does (rounds a half up, heavier); rounding in kg for everyone (rejected in Q5).

## R5. The reason

- **Decision**: `Adjustment.causes()` returns `Cause` values in the fixed order `SLEEP` (sleep ≤ 2), `SORENESS` (soreness ≥ 4) and `STRESS` (stress ≥ 4). The API returns the codes. The frontend joins their translated noun phrases with `Intl.ListFormat(language, { type: 'conjunction' })`, capitalises the first letter for the locale, and puts the list into `readiness.adjustment.reason`.
- **Rationale**: Text stays in the frontend (constitution II). Noun phrases avoid the Polish gendered past tense (spec departures). A score under 45 always has at least one cause (spec Edge Cases), so the list is never empty.

## R6. Switch endpoint

- **Decision**: `PUT /api/v1/check-ins/today/targets` with `{ "choice": "LIGHTER" | "ORIGINAL" }`. It answers with the whole `TodayCheckInResponse`. Refusals: `409 check-in.training-started`, `409 check-in.no-adjustment` (no check-in, another band, or nothing changes), `409 check-in.no-session-today`, `404 plan.not-found`, `400 validation.failed`. The choice is idempotent.
- **Rationale**: One resource state set by `PUT`, the way the check-in itself is saved; the answer is what Today shows, so no second read. A boolean body would read badly in the contract.
- **Alternatives**: `POST …/revert` and `POST …/restore` (two endpoints for one state); `DELETE` for revert (not idempotent in meaning, no body).

## R7. Logged sets and the logger answer

- **Decision**:
  - `LogSessionUseCase.logSet` asks readiness for the adjustment of the log's date and passes the targets in use for that place to `SessionLog.logSet`, which copies them into the `SetLog`.
  - `SessionLogWebMapper` shows a logged set's target from its `SetLog` copy, and an unlogged set's from the targets in use.
  - `LoggedEntryResponse` gains `planned`, a `TargetResponse` that is present only when the targets in use for that entry differ from the planned ones.
  - The trainer's logs answer goes through the same mapper with no adjustment. Logged sets then show their true copy, so the RPE colour compares against what was asked; unlogged sets show the planned targets until #122.
- **Rationale**: FR-014. Today the mapper prints the entry's planned targets even for logged sets, which would be wrong on an adjusted day. A `planned` field only where needed keeps the Ready and Take it easier answers unchanged.
- **Race**: a switch and the first set logged at the same moment can both pass. This is the same accepted ceiling as spec 109 (one person, milliseconds apart), so it gets a `ponytail:` comment rather than a lock.

## R8. Frontend composition

- **Decision**: The `plan` feature's `TodayPage` gains one optional prop, `adjusted: { targets, mark, reason }`:
  - `targets`: the lighter targets in use, by position, typed from `schema.d.ts`.
  - `mark` and `reason`: nodes drawn by `readiness`, placed in the session card's header and under its name.

  `app/today-route.tsx` reads the check-in and passes them in, so neither feature imports the other.

  A shared `components/target-change.tsx` draws "old struck → new" with an accessible text ("Load 100 kg, today 90 kg"). Both `plan`'s `EntryRow` and `sessionlog`'s logger use it.
- **Rationale**: frontend/CLAUDE.md forbids cross-feature imports; `beforeSession` already set this pattern for #112.
- **Alternatives**: moving the session card into `app/` (moves code #89 owns), or a React context (global state for one screen).

## R9. Switch UX and the Undo message

- **Decision**: `useChooseTargets` updates the cached check-in at once (`onMutate`), rolls it back on error (FR-023), and stores the server's answer on success. Both mutations also invalidate the session log query, so the logger refetches. "Use original targets" calls `toast()` from the existing `sonner` `Toaster` with `action: { label: Undo }`. The switch is a `Button variant="link" size="touch"`; focus moves to the button that replaces it, and an `sr-only` status announces the targets in use.
- **Rationale**: One tap with no wait (SC-001). `sonner` is already installed and placed in `app/app.tsx`. Its default time is fine because "Use lighter targets" stays on the card (WCAG 2.2.1).
- **To verify in the browser**: the toast sits above the tab bar at 375 px; dark and light contrast of the `adjusted` badge.

## R10. Look

- **Decision**:
  - Badge: a new design-system variant `adjusted` (`bg-adjusted-muted text-adjusted`).
  - Session card border: `color-mix(in oklch, var(--adjusted) 45%, var(--border))`, as the board's `.card-adjusted`. The tokens `--adjusted` and `--adjusted-muted` already exist in `index.css`.
  - Old value: `line-through` in `text-muted-foreground`; new value in `text-foreground font-medium`; numbers in `num`.
- **Rationale**: The design system defines these; nothing is red (FR-026).

## R11. Migration and demo data

- **Decision**: `V22__add_check_in_target_choice.sql` adds `target_choice varchar(8) not null default 'LIGHTER'` with check constraint `ck_check_ins_target_choice`. No open pull request takes V22 (checked with `gh pr list`). Demo data: `R__demo_check_ins.sql` keeps an earlier recovery-day check-in for `demo.client.daily`; `R__demo_session_logs.sql` gives that day's set logs lighter target copies.
- **Rationale**: One column of the check-in's own key (3NF); the default makes every existing check-in "lighter", which matches what an adjusted day starts as.
