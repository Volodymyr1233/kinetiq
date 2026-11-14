# Data model: Client sees PRs and their week streak

Nothing is stored and no migration is added. Everything below is derived when read (FR-003).

## `progress` (backend)

### `Pr` (`progress.domain.model`, #177)

One exercise whose top set in one session log beats every earlier set of it (FR-001).

| Field | Type | Rule |
|---|---|---|
| `exercise` | `ExerciseId` | required |
| `top` | `TopSet` | the session's top set of that exercise |

`Pr.on(LocalDate date, List<LoggedExercise> logged)` returns the PRs of the session log dated `date`: for each row of that date, the rows of the same exercise dated before it must be at least one, and the row's top set must be better than each of them (`TopSet.isBetterThan`, so an equal set is not). Rows after `date` are ignored, and the order of `logged` does not matter.

### `Rewards` (`progress.domain.input`, #177)

What the session done screen shows besides the session summary.

| Field | Type | Rule |
|---|---|---|
| `prs` | `List<Pr>` | the PRs of the session log of the date; empty when none |
| `weeksInARow` | `int` | the week streak of spec 156 (FR-006), the same number `own` gives |

### `ViewProgressUseCase.rewards(AccountId client, LocalDate date)` (#177)

The client's own rewards for the session log of `date`. Reads the plan, the logged exercises and the finished days, as `own` does. Without a published plan `PlanNotFoundException`, which does not happen for a finished session (research R4).

### "PR today" (#304)

`Progress.ExerciseProgress.prToday` is true when the exercise is among `Pr.on(today, logged)`, `today` in the client's time zone. Nothing else in `Progress` changes.

## `sessionlog` (backend, #302)

### `SessionSummaryView` (`sessionlog.domain.input`)

| Field | Type | Change |
|---|---|---|
| `log` | `SessionLog` | unchanged |
| `summary` | `SessionSummary` | unchanged |
| `prs` | `List<SessionSummaryView.ExercisePr>` | new: in the order of the first place of their exercise in the session |
| `weeksInARow` | `int` | new |

`SessionSummaryView.ExercisePr`: `exercise` (`Exercise`, from the trainer's library, for its name and measure) and `top` (`TopSet`); named apart from `progress`'s `Pr`, which `SessionLogService` reads too.

`SessionSummary` (the sets, volume, average RPE and minutes of spec 98) does not change, so `CONTEXT.md`'s "Session summary" keeps its meaning.

## API (#302)

`SessionSummaryResponse` gains `prs` and `weeksInARow`; see [contracts/session-summary-api.md](contracts/session-summary-api.md).

## Frontend (#178, #303)

No new state. The session done screen reads `prs` and `weeksInARow` from the summary it already loads; the summary's query key gains the UI language (research R6).

## `CONTEXT.md` (#177)

**PR**: "A set that beats every earlier set the client logged of an exercise, by the top set order; the first log of an exercise is not one." `_Avoid_: Record, achievement` stays (FR-005).
