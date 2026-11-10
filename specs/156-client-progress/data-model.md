# Data Model: Client sees their progress

Nothing is stored. Every type below is derived when read, from the tables of specs 68 and 92 (`plans`, `plan_sessions`, `session_logs`, `session_log_entries`, `set_logs`), and lives in the `progress` feature.

## TopSet (#157)

A set's figures: `loadKg` (optional, kilograms with one decimal), `amount` (repetitions or seconds), `rpe`.

- `isBetterThan(other)`: a load beats no load; then the higher load; then the higher amount. An equal set is not better, so the earlier one stays the top set.

## LoggedExercise (#157)

One exercise in one session log, as `TrainingLog.loggedExercises` returns it.

| Field | Meaning |
|---|---|
| `date` | the session log's date, the client's local date |
| `exercise` | `ExerciseId`, from the session's copy of the planned entry |
| `top` | `TopSet`: the best set of this exercise in this session (research R3) |
| `rpeSum`, `sets` | the sum of the RPE of its logged sets and how many there are (at least 1) |

An exercise in two entries of one session gives one row. Rows exist only for exercises with at least one logged set.

## TrainingWeek (#157)

One plan week of one client.

| Field | Meaning |
|---|---|
| `number`, `monday` | the plan week's number and first day |
| `planned` | the plan's sessions on days of the week on or after the publish day |
| `done` | how many of those days have a finished session log |
| `running` | the current plan week of a plan that has not finished |
| `rpeSum`, `sets` | over every logged set of the week; the mean is `rpeSum / sets` to one decimal, none when `sets` is 0 |
| `exercises` | per exercise logged in the week: its top set (the best of its sessions') and its RPE sum and count |

`TrainingWeek.of(plan, lastWeek, loggedExercises, finishedDays)` builds weeks 1 to `lastWeek`.

## Period (#158)

4, 8 or 12 weeks; anything else is `PeriodInvalidException` (`progress.period-invalid`).

## Progress (#158)

What a client and their trainer read for one period.

| Field | Meaning |
|---|---|
| `goal`, `week`, `finished` | the plan's goal, the current plan week (the last one once finished), whether the plan has finished |
| `weeks` | the period's `TrainingWeek`s, oldest first |
| `sessionsDone`, `sessionsPlanned` | sums over the period |
| `averageRpe` | over every set of the period, one decimal; none without sets |
| `sessionsDoneChange`, `averageRpeChange` | the last completed week against the week before (research R5); none when either has no value |
| `weeksInARow` | research R6 |
| `exercises` | the exercises logged in the period, the one in the most sessions first, then the most recently logged |

Each exercise: its id, its `best` top set in the period, its `topSetChange` (in kilograms or in repetitions or seconds, or none), `prToday`, its weeks in the period (top set and mean RPE, none for a week without it), and its 3 most recent sessions over all logs (date and top set).

## Rules that hold

- Every figure is the client's own; the trainer reads the same `Progress` for their client.
- Reading changes nothing.
