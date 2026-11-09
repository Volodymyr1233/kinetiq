# Data model: Today's targets are lightened on a recovery day

## Stored

### `check_ins` (changed, migration `V22__add_check_in_target_choice.sql`)

| Column | Type | Rule |
|---|---|---|
| `target_choice` | `varchar(8) not null default 'LIGHTER'` | `ck_check_ins_target_choice`: `LIGHTER` or `ORIGINAL` |

All other columns are unchanged (spec 109). The plan tables are never written by this story.

### `set_logs` (unchanged schema)

`target_load_kg`, `target_amount`, `target_rpe` now hold the targets in use when the set was logged, which are the lighter ones on an adjusted day.

## Domain (backend, `readiness.domain.model`)

### `TargetChoice` (enum)

`LIGHTER`, `ORIGINAL`.

### `CheckIn` (changed)

- New field `targetChoice`, `LIGHTER` when given.
- `change(answers, now)`: different answers also reset `targetChoice` to `LIGHTER`; the same answers change nothing (Clarification Q3).
- `choose(TargetChoice)`: sets it; the same choice changes nothing. Whether a switch is allowed (no set logged, an adjustment exists) is the service's decision, as for saving answers.

### `Target` (record)

`BigDecimal loadKg` (nullable: no load), `int amount`, `int rpe`. It is the value the rule reads and returns.

### `Planned` (record)

`Target target`, `Measure measure`: one entry as planned, in session order.

### `Cause` (enum)

`SLEEP` (sleep 1–2), `SORENESS` (soreness 4–5), `STRESS` (stress 4–5), in that order.

### `Adjustment` (record)

- Fields: `List<Target> lighter` (one per planned entry, by position), `List<Cause> causes`, `boolean inUse` (`targetChoice == LIGHTER`).
- `static Optional<Adjustment> of(Answers, boolean inUse, List<Planned>, WeightUnit)`: the service passes the check-in's answers and `targetChoice == LIGHTER`; empty unless the band is `RECOVERY_DAY` and at least one target changes.
- `static Target lighter(Target, Measure, WeightUnit)`: the rule of FR-002 to FR-005 and research R4.
- `Target inUse(int position, Target planned)`: the lighter target when `inUse`, else `planned`.

## Use case (backend, `readiness.domain.input`)

### `CheckInUseCase` (changed)

- `today(client)` and `save(command)`: `TodayCheckIn` also carries `Adjustment adjustment`, which is `null` without one. It is computed from today's plan session, the library's measures and the client's unit.
- `choose(ChooseTargetsCommand(client, TargetChoice))`: `TodayCheckIn`. Refusals: `PlanNotFoundException`, `NoSessionTodayException`, `TrainingStartedException`, `NoAdjustmentException` (`check-in.no-adjustment`).
- `adjustmentOn(client, LocalDate date, List<Planned>)`: `Optional<Adjustment>`, used by `sessionlog` for a session log of that date.

## Session log (backend, `sessionlog`)

- `StartedSession` carries the `Adjustment` of the log's date, or `null`.
- `SessionLog.logSet(…)` takes the targets in use for the place and copies them into the new `SetLog`; a change of a logged set keeps its copy.
- `LoggedEntry` is unchanged: it is the planned copy taken at the start.

## State

```text
no check-in ──save (Recovery day, something changes)──▶ adjusted, LIGHTER
adjusted, LIGHTER ──choose ORIGINAL──▶ adjusted, ORIGINAL ──choose LIGHTER / Undo──▶ adjusted, LIGHTER
any ──save different answers──▶ recomputed; LIGHTER if still adjusted
any ──first set logged──▶ fixed (no save, no switch)
```

## Frontend types

These come from `schema.d.ts` (regenerated): `TodayCheckInResponse.adjustment`, `AdjustmentResponse`, `AdjustedTargetResponse`, `ChooseTargetsRequest`, and `LoggedEntryResponse.planned`.
