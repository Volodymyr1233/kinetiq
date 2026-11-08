# Data model: Client finishes a session

Changes on top of specs `92-session-logging` and `101-log-edit-window`.

## Plan (published day)

| Field | Type | Rule |
|---|---|---|
| `publishedOn` | `Optional<LocalDate>` | Set by `publish(today)` (the client's local date); empty for a draft. Kept by every method that returns an edited plan. |

Stored as `plans.published_on date` (V19). Constraints: `published_on` is null exactly when `first_week` is null; when set it is within `first_week` .. `first_week + 6`.

## Session log

| Field | Type | Rule |
|---|---|---|
| `finishedAt` | `Optional<Instant>` | Already there (spec 92). Set once, by `finish`. |
| `note` | `Optional<String>` | #100. Trimmed, 1 to 500 characters; empty input clears it. Stored as `session_logs.note varchar(500)` (V20), null when absent. |

### Behaviour

| Method | Rule |
|---|---|
| `finish(now)` | See research R6. Never changes `finishedAt` of a finished log. |
| `writeNote(text, now)` | Finished and open, else `NotFinishedException` / `SessionLogLockedException`; over 500 characters is `NoteInvalidException`. |
| `summary(measures)` | A `SessionSummary` of the current sets (research R8); only for a finished log. |

## Session log header (read model)

`SessionLogHeader(id, date, startedAt, finishedAt)`: what the states need, read for a range of dates without entries and sets. Not an aggregate; never saved.

## Session state

`SessionState`: `DONE`, `IN_PROGRESS`, `MISSED`, `TODAY`, `PLANNED`. "None" is an empty `Optional`. Derived, never stored. The rule is in research R4.

### Transitions of one planned session over time

```text
(no log, before the day published)           none
(no log, future day)                         PLANNED
(no log, today)                              TODAY
        open the logger                      -> IN_PROGRESS
        finish (needs 1 logged set)          -> DONE
(no log, day passed, on/after day published) MISSED
IN_PROGRESS, 24 hours pass, never finished   -> MISSED
MISSED without a log cannot become DONE: nothing to finish
```

A finished log is `DONE` for good, also after its lock.

## Session summary

| Field | Type | Rule |
|---|---|---|
| `setsLogged` | int | Logged sets of the log. |
| `setsPlanned` | int | Sum of `sets` over the log's entries. |
| `volumeKg` | `Optional<BigDecimal>` | Sum of load × amount over logged sets with a load whose exercise is measured in `REPS`; empty when none counts. One decimal. |
| `averageRpe` | `BigDecimal` | Mean RPE of the logged sets, half up, one decimal. |
| `minutes` | int | Whole minutes from `startedAt` to `finishedAt`, at least 1. |

Derived on read. `name`, `date` and `note` of the log travel with it in the answer.

## Migrations

| Version | Task | Content |
|---|---|---|
| V19 `add_plan_published_on` | #99 | `alter table plans add column published_on date`; set it to `first_week` for published plans; named constraints for the two rules above. |
| V20 `add_session_log_note` | #100 | `alter table session_logs add column note varchar(500)` and `ck_session_logs_note_not_blank`. |

Before opening the pull request, check `master` and the open pull requests for migrations that took V19 or V20, and renumber if needed (`backend/CLAUDE.md`).

## Demo data

- `R__demo_plans.sql`: every inserted published plan gets `published_on`; `Demo Client Daily`'s is the Wednesday of its first week.
- `R__demo_session_logs.sql` (#100): finished logs, with sets, for some past days of that plan; other past days have no log. All are older than 24 hours, so they are locked and read-only.
