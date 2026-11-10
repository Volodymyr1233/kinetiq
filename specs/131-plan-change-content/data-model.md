# Data Model: Plan change content is drafted and checked

## Tables (V24 `create_plan_changes`)

Named constraints, plural snake_case, UUID ids created in the application. Demo rows in `R__demo_plan_changes.sql` (local profile only).

### plan_changes

| Column | Type | Notes |
|---|---|---|
| id | uuid | PK |
| client_id | uuid | FK `accounts (id)` |
| rule | varchar | `LOW_COMPLETION`, `HIGH_RPE`, `STALLED_WEIGHT` (check) |
| state | varchar | `PENDING`, `ACCEPTED_BY_ONE`, `APPLIED`, `DECLINED`, `EXPIRED` (check); this story writes `PENDING` |
| plan_version | bigint | `PlanSnapshot.version` of the plan the content was made from, set when the plan change is created; later stories compare it |
| window_start | date | first Monday of the weeks the rule looked at |
| window_end | date | last Sunday of those weeks |
| effective_week | date | the Monday after creation in the client's zone (check `isodow = 1`) |
| removed_day | varchar null | weekday of the removed session (low completion) |
| removed_name | varchar null | its name at that time; null together with `removed_day` |
| created_at | timestamptz | |
| ended_at | timestamptz null | set by #137/#143 when the plan change ends |

Unique partial index `uq_plan_changes_one_open_per_client` on `(client_id)` where `state in ('PENDING','ACCEPTED_BY_ONE')`: at most one open plan change per client, enforced by the database (closes SC-003 of story #125). Index on `(client_id, ended_at)` for `lastEndedAt`.

### plan_change_targets

One row per changed target value of one entry.

| Column | Type | Notes |
|---|---|---|
| plan_change_id | uuid | FK, part of PK |
| session_day | varchar | weekday, part of PK |
| position | int | entry position in the session, part of PK |
| target | varchar | `SETS`, `AMOUNT`, `LOAD_KG`, `RPE`, `REST_SECONDS`, part of PK |
| exercise_id | uuid | FK `exercises (id)`; copied for display (an entry may later be edited) |
| session_name | varchar | name of the session then; copied for the same reason (documented denormalisation) |
| old_value | numeric(6,1) | |
| new_value | numeric(6,1) | check `new_value <> old_value` |

`exercise_id` and `session_name` depend on `(plan_change, session_day, position)`, not on `target`: they are repeated per target row (at most 2 per entry in practice). Accepted as a measured-free denormalisation to keep one table the screen reads.

### plan_change_moves

| Column | Type | Notes |
|---|---|---|
| plan_change_id | uuid | FK, part of PK |
| from_position | int | position in the removed session, part of PK |
| exercise_id | uuid | FK `exercises (id)`; copied for display, as on `plan_change_targets` |
| to_day | varchar | weekday of the receiving session |

Rule of the domain: moves exist only with a removed session; `from_*` is the removed session; at most 2 per plan change.

### plan_change_figures

The numbers behind the rule, one value per row.

| Column | Type | Notes |
|---|---|---|
| id | uuid | PK |
| plan_change_id | uuid | FK |
| kind | varchar | `MEAN_RPE`, `BEST_LOAD_KG`, `SESSIONS_PLANNED`, `SESSIONS_FINISHED` |
| exercise_id | uuid null | set only for `BEST_LOAD_KG` |
| week_start | date | Monday (check `isodow = 1`) |
| value | numeric(7,3) | |

Unique `(plan_change_id, kind, exercise_id, week_start)` (nulls not distinct).

### plan_change_exercises

The exercises the rule concerns (above RPE 8, or stalled), one row each: `(plan_change_id, exercise_id)`, PK both, used by the screen note ("only these change") and the drafting service.

## Domain model (no framework types)

- `PlanChange(id, client, rule, state, planVersion, window, effectiveWeek, content, figures, concernedExercises, createdAt, endedAt)`; created only through a factory that takes a checked `PlanChangeContent`. No setters.
- `PlanChangeContent(List<TargetChange> changes, Optional<RemovedSession> removed, List<SessionMove> moves)`; invariants: not empty; moves only with a removed session.
- `TargetChange(EntryRef entry, ExerciseId exercise, String sessionName, Target target, BigDecimal oldValue, BigDecimal newValue)`.
- `EntryRef(DayOfWeek day, int position)`, `RemovedSession(DayOfWeek day, String name)`, `SessionMove(EntryRef from, ExerciseId exercise, DayOfWeek toDay)`.
- `Figure(FigureKind kind, Optional<ExerciseId> exercise, LocalDate weekStart, BigDecimal value)`.
- `Finding(Rule rule, Set<ExerciseId> exercises, List<Figure> figures)`; `DuePlanChange` gains `figures` and `List<WeekTraining> weeks` (the rule's window, oldest first; the low completion fallback counts finished sessions per weekday in it); `WeekTraining` gains `List<DayOfWeek> finishedDays`.
- `PlanSnapshot(long version, LocalDate firstWeek, int weeks, List<SessionSnapshot> sessions)`; `SessionSnapshot(DayOfWeek day, String name, List<EntrySnapshot> entries)`; `EntrySnapshot(ExerciseId exercise, Category category, Measure measure, int sets, int amount, BigDecimal loadKg, int rpe, int restSeconds)`.
- `PlanChangeText(String title, String reason)`.

## State transitions

`PENDING` is the only state written here. Allowed later: `PENDING → ACCEPTED_BY_ONE → APPLIED`, `PENDING|ACCEPTED_BY_ONE → DECLINED`, `PENDING|ACCEPTED_BY_ONE → EXPIRED` (#137, #143). The database already forbids two open plan changes per client.

## Ownership

A plan change belongs to one client; the trainer owning that client may read it. Everyone else gets `plan-change.not-found`.
