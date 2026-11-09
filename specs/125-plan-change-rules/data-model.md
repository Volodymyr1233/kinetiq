# Data Model: Rules detect that a plan change is due

Scope: the story stores only which weeks a client was checked for. The plan change itself (its table, aggregate, states, old and new values, effective week) belongs to task #132 of story #131 and is not built here; this story hands a due plan change to a port.

## Table

### weekly_checks (V22)

One row per client and week the client was checked for.

| Column | Type | Notes |
|---|---|---|
| client_id | uuid | FK `accounts (id)` |
| week_start | date | the Monday of the check week, in the client's time zone at that time |
| checked_at | timestamptz | |

Primary key `(client_id, week_start)`; check that `week_start` is a Monday (`isodow = 1`). Demo data is not needed: the table is bookkeeping and fills itself when the job runs.

## Domain model (no framework types)

- `Rule` enum in priority order: `LOW_COMPLETION(2)`, `HIGH_RPE(2)`, `STALLED_WEIGHT(3)`; the number is the window in weeks.
- `LoggedSet(ExerciseId exercise, BigDecimal loadKg /* nullable */, int rpe)`.
- `WeekTraining(LocalDate monday, int sessionsInPlan, int plannedSessions, int finishedSessions, List<LoggedSet> sets)`: `sessionsInPlan` is how many sessions the plan has per week; `plannedSessions` leaves out days before the plan was published.
- `ScheduledClient(AccountId client, ZoneId zone, LocalDate firstWeek, int weeks, Optional<LocalDate> lastCheckedWeek)` for a client with a published plan.
- `Finding(Rule rule, Set<ExerciseId> exercises)`; each rule is a pure function from `List<WeekTraining>` (its window, oldest first) to `Optional<Finding>`.
- `DuePlanChange(AccountId client, Rule rule, LocalDate windowStart, LocalDate windowEnd, Set<ExerciseId> exercises)`: what the run hands over when a rule fires. Task #132 turns it into a stored plan change and adds the effective week.

## What other stories own

| Concern | Story or task |
|---|---|
| Plan change table, aggregate, states, old and new values | #131, #132 |
| Draft content, limits, fixed transformation | #133 to #136 |
| Accept, undo, decline | #137, #138 |
| Apply or expire at the effective Monday (ends a plan change) | #143, #144 |
