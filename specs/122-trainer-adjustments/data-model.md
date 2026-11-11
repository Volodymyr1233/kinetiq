# Data model: Trainer sees a client's adjustments

## Stored

Nothing new. No migration. Read from `check_ins` (answers, `target_choice`), `session_logs` with their entries, and the published plan (FR-007).

Demo data: `db/dev-data/R__demo_check_ins.sql` sets `target_choice = 'ORIGINAL'` on the recovery check-in three days before today (research R9).

## Domain (backend)

### `readiness.domain.model.Cause` (changed)

- New `static List<Cause> of(Answers answers)`: `SLEEP` for sleep ≤ 2, `SORENESS` for soreness ≥ 4, `STRESS` for stress ≥ 4, in that order. Moved from the private `Adjustment.causes`, which now calls it.

### `readiness.domain.input.CheckInUseCase` (changed)

- New `List<CheckIn> between(AccountId client, LocalDate from, LocalDate to)`: the client's check-ins with `from ≤ date ≤ to`, oldest first. No ownership check: callers are other features' use cases that already checked it (as for `adjustmentOn`).

### `readiness.domain.output.CheckInRepository` (changed)

- New `List<CheckIn> findAllByClientAndDateBetween(AccountId client, LocalDate from, LocalDate to)`, oldest first.

### `sessionlog.domain.input.ClientOverview` (changed)

New fields, both never `null`:

| Field | Type | Rule |
|---|---|---|
| `readiness` | `List<ReadinessDay>` | Exactly 14: `today − 13` … `today`, oldest first (FR-001) |
| `recoveryDays` | `List<RecoveryDay>` | One per day of `readiness` whose readiness band is `RECOVERY_DAY`, newest first (FR-003) |

#### `ClientOverview.ReadinessDay` (nested class, `Optional` getters)

| Field | Type | Rule |
|---|---|---|
| `date` | `LocalDate` | Required |
| `readiness` | `Readiness`, nullable, read as `Optional` | Absent without a check-in that date; never a score of 0 in its place (FR-002) |

#### `ClientOverview.RecoveryDay` (nested class, `Optional` getters)

| Field | Type | Rule |
|---|---|---|
| `date` | `LocalDate` | Required |
| `score` | `int` | 0–44 |
| `causes` | `List<Cause>` | `Cause.of(answers)`; never empty for a recovery day |
| `targets` | `Optional<TargetChoice>` | `LIGHTER` when the day's adjustment is in use, `ORIGINAL` when not, empty without an adjustment (FR-004, research R3) |
| `session` | `Optional<ClientSession>` | The started session of that date, else the plan's session that weekday within the plan's dates, else empty (FR-005, research R4) |

### `sessionlog.domain.input.ClientSessionLog` (changed, task #290)

- New nullable `Adjustment adjustment` component (as `StartedSession` has): the adjustment of the log's date over the log's own copy of its entries; empty without one (FR-015).

### Derivation in `ClientTrainingViewService.overview`

1. `today` = now in the client's time zone (as today). `from = today − 13`.
2. `checkIns.between(client, from, today)` → by date.
3. For each of the 14 dates: a `ReadinessDay` with the check-in's `readiness()` or none.
4. For each check-in in the `RECOVERY_DAY` band, newest first:
   - planned entries: the session log of that date (its entries and the library's measures), else the published plan's session on that weekday when `publishedOn ≤ date < firstWeek + weeks` (its entries, `OwnPlanView.library` measures), else none;
   - `targets` = `checkIns.adjustmentOn(client, date, planned).map(a -> a.inUse() ? LIGHTER : ORIGINAL)`; with no planned entries, empty;
   - `session` = `started(log, now)`, or `planned(session, date, SessionState.of(date, today, publishedOn, empty, now))`, or empty.

## API (backend → frontend), see [contracts/overview-api.md](contracts/overview-api.md)

- `ClientOverviewResponse` gains `readiness` and `recoveryDays`.
- `ClientSessionLogResponse` gains `adjustment`; its entries' `planned` and unlogged sets' `target` follow the adjustment.

## Frontend

- `lib/readiness.ts`: `BAND_STYLE` (band → `surface`, `dot` classes), moved from `features/readiness/components/readiness-card.tsx`; used by the readiness card and the strip.
- `features/sessionlog/components/readiness-strip.tsx`: the card of FR-008 to FR-014, fed `overview.readiness`, `overview.recoveryDays` and `overview.today`.
- `features/sessionlog/pages/client-logs-tab.tsx`: chosen session in `?session=<key>` (research R5); reason line from `log.adjustment`.
- `features/sessionlog/components/session-log-table.tsx`: `TargetChange` for entries with `planned`.
