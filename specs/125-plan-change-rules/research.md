# Research: Rules detect that a plan change is due

No `NEEDS CLARIFICATION` remained after the spec session; these are the design decisions.

## Which clients are due, and how often the job runs

- **Decision**: a job every 10 minutes (`fixedDelay`). For each published-plan client it computes the local check week: take the client's local date-time, subtract 3 hours, take the Monday on or before that date. A client is due when that Monday is after the last week it was checked for.
- **Rationale**: subtracting 3 hours makes "Monday 03:00 local" a plain date boundary, so it works in any time zone, including half-hour offsets, and a client is picked up at most 10 minutes late. Using "last checked week" instead of "is it exactly 03:00" gives the catch-up after downtime for free (FR-002).
- **Alternatives**: a cron at exactly 03:00 per zone (one trigger per zone, misses downtime); an hourly cron (misses nothing but fixes a coarser delay for no gain).

## Once per week, and safe when runs overlap

- **Decision**: a `weekly_checks` row (client, week Monday) is inserted at the start of the client's own transaction with `on conflict do nothing`; no row inserted means skip. The row is committed with the plan change or rolled back with it.
- **Rationale**: the primary key makes "once per week" true even for two instances or overlapping ticks; a failure rolls back the claim so the next tick retries (FR-011). The week key is a date, so a time zone change cannot cause a second check for the same week.
- **Alternatives**: an in-memory set (lost on restart); a lock around the whole run (one bad client holds up all).

## One open plan change at all times

- **Decision**: the service asks the plan change port `hasOpen` before evaluating, and skips the client when it is true. The guarantee in the database (a partial unique index on open plan changes) comes with the table in task #132.
- **Rationale**: the table belongs to #132; building a second schema here would clash with it. In this story the rule is proven by unit tests with a fake port, and the weekly claim already stops overlapping runs from evaluating the same client twice.

## Reading logs and plan facts

- **Decision**: a driven port `TrainingHistory` with a JDBC adapter in `planchange.infrastructure.output` that reads `plans`, `plan_sessions`, `accounts`, `session_logs`, `set_logs` and `session_log_entries` directly.
- **Rationale**: the existing use cases return per-client views for the signed-in client and would need one call per client per week; a read-only query keeps `planchange` free of dependencies on other features. Precedent: `JdbcDraftedPlanDirectory`.
- **Alternatives**: calling `ViewSessionStatesUseCase` per client and week (N×weeks queries, and it is built around "the signed-in client"); a port implemented in `sessionlog` (makes `sessionlog` depend on `planchange` for no gain).

## Counting planned and finished sessions

- **Decision**: planned sessions of a week = sessions of the plan as it is now whose date in that week is on or after the day the plan was published; finished = those with a session log of that date that has `finished_at`. Same date matching as `SessionStateService`.
- **Rationale**: matches the Missed/Done semantics of spec 98 and the answered clarification (current plan for every week). A started session that locked unfinished is therefore not finished.

## Exact arithmetic for thresholds

- **Decision**: no rounded averages. High RPE: `sum(rpe) > 8 * count`. Stalled weight: `best3 < best1 * 1.05` with `BigDecimal` loads (scale 1) and `sum(rpe) <= 8 * count`. Low completion: `finished * 10 < planned * 7`.
- **Rationale**: `SessionLog.averageRpe()` rounds half up to one decimal, so 8.04 would read as 8.0 and wrongly not fire. Integer comparisons are exact at "8.0 and just above", "exactly 5%" and "exactly 70%".

## Cool-down and plan end

- **Decision**: the earliest week a rule may look at is `max(first plan week, first Monday that begins at or after the last end of a plan change, in the client's zone)`. A rule fires only when its whole window (2 or 3 weeks) lies on or after that Monday, and its last week is not after the plan's last week. Because every rule's own window length must fit, each rule gets its own cool-down (2 and 3 weeks) from one earliest-week limit.
- **Rationale**: implements FR-003 and FR-010 with one clip; an accepted plan change ends at the start of its effective week (clarification), a declined one when it was declined, an expired one at the start of its effective week.

## Concerned exercises

- **Decision**: high RPE records exercises whose mean RPE over the 2-week window is above 8 (`sum > 8 * count` per exercise); stalled weight records the stalled exercises. They travel in the due plan change; #132 stores them.
- **Rationale**: ADR 0002 limits the later draft to "the exercises whose RPE was above 8" and to stalled lifts. If the overall mean exceeds 8 in every week, at least one exercise exceeds 8 over the window, so the list is never empty.

## Plan change storage stays out of this story

- **Decision**: a port `PlanChangeRepository` (`hasOpen`, `lastEndedAt`, `open`) with a placeholder adapter `LoggingPlanChangeRepository` that logs the rule name and stores nothing, following `UnavailablePlanGenerator`. Task #132 (model and migration) supplies the real adapter.
- **Rationale**: scope. #132 already owns the plan change model with per-target old and new values, rule numbers, effective week and state.
- **Consequence**: until #132 the placeholder reports no open plan change and no earlier end, so a rule that still holds fires again each week and is only logged. The open-plan-change check and cool-down are proven by unit tests with a fake, not on a database.

## Ordering with later stories

An open plan change that nobody ends would block its client's checks. Ending one is built in #138 (decline), #139 and #144 (apply or expire at the effective Monday). Those, with #132, must land before this is used in production.
