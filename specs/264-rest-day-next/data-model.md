# Data Model: Client sees what comes next on a rest day

Nothing is stored and there is no migration.

## TodayView (domain, `plan.domain.input`)

| Field | Notes |
|---|---|
| `date` | today in the client's time zone (unchanged) |
| `state` | `SESSION`, `REST` or `FINISHED` (unchanged) |
| `session` | the session of today, only for `SESSION` (unchanged) |
| `next` | new, only for `REST`: the next session and the date it falls on; absent when the plan has no session after today |
| `library` | the trainer's exercises naming the entries of `session` or `next` (unchanged) |

## Rules

- `next` is the first session strictly after `date`, by date; its date is within the plan's last week (up to its Sunday).
- `next` is set only for `REST`; for `SESSION` and `FINISHED` it is absent.
