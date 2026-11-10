# Research: Client sees their progress

## R1. Where the statistics live

**Decision**: A new feature `progress` on both sides. Its read model, `JdbcTrainingLog` in `progress.infrastructure.output`, reads `session_logs`, `session_log_entries` and `set_logs` with SQL, as `planchange`'s `JdbcTrainingHistory` and `plan`'s `JdbcDraftedPlanDirectory` read other features' tables. The service uses the input ports `ViewOwnPlanUseCase` (the published plan, its current week and the trainer's exercise library), `ViewClientsUseCase.getClient` (the trainer's ownership check, `404 client.not-found`) and `CurrentAccountUseCase` (the client's time zone, for today).

**Why**: The figures are about plans and logs together, and neither `sessionlog` (logging) nor `plan` (prescribing) should grow a second purpose. `progress` depends on `account`, `client`, `plan` and `exercise`, none of which depends on it, so there is no cycle. `planchange` already shows that a read model may read the log tables directly instead of going through `sessionlog`.

**Alternatives considered**: Computing in `sessionlog` from `SessionLogRepository.findAllByClient`: no new SQL, but it loads every set into aggregates for a read that needs sums and maxima, and task #157 asks for queries tested against PostgreSQL. Reusing `planchange`'s `TrainingHistory`: it is another feature's output port, while `backend/CLAUDE.md` has other features call a feature's `domain.input` ports, and it lacks repetitions, sessions and dates per set.

## R2. One query per client, figures in the domain

**Decision**: The port `TrainingLog` has two methods. `loggedExercises(client)` returns one `LoggedExercise` per exercise in each session log of the client: the session's date, the exercise, the top set of that exercise in that session, and the sum and count of its RPE. `finishedDays(client)` returns the dates of finished session logs. `TrainingWeek` builds the plan weeks from these and the plan (sessions planned and done, mean RPE, each exercise's top set and mean RPE); the service takes the period, the comparisons, the streak, PR and recent sessions from the same weeks and rows.

**Why**: One row per session and exercise is the finest grain any figure needs: a week's top set is the best of its sessions' top sets, its mean RPE is the sum over the count, and the recent sessions and PR read the rows directly. Grouping by week in SQL would need a second query for sessions, and planned sessions need the plan anyway, which the domain has. A client has at most 96 sessions per plan, so reading all their rows each time is a few hundred rows.

**Alternatives considered**: One SQL query per figure (weekly completion, weekly RPE, weekly top sets, recent sessions, PR): five queries with the same joins and the same top-set order written several times. Filtering by the period in SQL: the streak and PR need every week anyway.

## R3. The top set in SQL

**Decision**: The query keeps the first row of each session and exercise with `distinct on (l.id, e.exercise_id)`, ordered by `s.load_kg desc nulls last, s.amount desc, s.entry_position, s.set_number`; the RPE sum and count come from window functions over the same partition, which PostgreSQL evaluates before `distinct on`. `TopSet` holds the same order in Java (`isBetterThan`) for comparing sessions and weeks: a load beats no load, then the higher load, then more repetitions or seconds; an equal set does not beat an earlier one.

**Why**: `desc` puts nulls first in PostgreSQL by default, so `nulls last` makes a set with a load beat one without (spec, Clarifications). Ordering by place in the session makes "the earliest" stable even when a set is changed later, which would change `logged_at`. The exercise of a set is on the session's copy of the planned entry (`session_log_entries`), as `JdbcTrainingHistory` reads it, so a plan edited later does not move old sets.

**Alternatives considered**: `max(load_kg)` with a join back to find the set: two passes and still needs the tie order. Ordering by `logged_at`: changes when a set is edited.

## R4. Plan weeks and the period

**Decision**: Weeks are the plan's weeks, numbered from its first week (CONTEXT.md, Week). The last week is the current plan week, or the plan's last week once it has finished; the period is the last 4, 8 or 12 of the weeks from 1 to that one. For each week, planned sessions are the plan's sessions on days on or after the publish day, as the plan is when read; a planned session is done when a finished session log has its date. The current week is `running` until the plan has finished; its days still to come count as planned. Mean RPE counts every logged set of the week, finished or not.

**Why**: These are the definitions of spec 125 (planned sessions from the plan as it is when read, matched by date, nothing before the publish day), so the client's figures agree with the rules that call for plan changes. `OwnPlanView` already gives the current week and whether the plan has finished, by the client's time zone.

**Alternatives considered**: Counting only days up to today as planned: the board's "Week 14 has 2 sessions left" shows the whole week planned (spec, Assumptions).

## R5. Comparisons with the week before

**Decision**: The compared weeks are the last week of the period that is not running and the week before it (spec, Clarifications). Sessions done compare by count; mean RPE by the difference of the two one-decimal means; an exercise's top set by load in kilograms when both have a load, or by repetitions or seconds when neither has, and not at all when one has and the other has not. A week with no value for the figure (nothing planned, no set, the exercise not logged) gives no comparison. The response carries the differences, not the two weeks.

**Why**: The difference of the shown values never contradicts the numbers on screen. The trainer's section of task #108 then shows the same comparison without computing it again.

**Alternatives considered**: Sending both weeks and letting each screen subtract: the rule of which weeks are compared would live twice.

## R6. Weeks in a row

**Decision**: Counted backwards from the last completed week over every plan week from week 1: a week counts when the client did at least 2 sessions, or every planned session when it had fewer than 2; the first week that does not count ends the streak. A week without a planned session, which only a first week published after its last session day can be, does not count. The running week adds one once it already counts. Independent of the period.

**Why**: The board's rule "At least 2 sessions each week" and the spec's edge case for a first week shortened by the publish day. A running week cannot break the streak before it ends.

## R7. PR today and recent sessions

**Decision**: For each exercise logged in the period, the best `LoggedExercise` over all rows (by `TopSet`, the earlier date winning a tie) is a PR today when its date is the client's today. The recent sessions are the exercise's 3 newest rows, each with its top set.

**Why**: Because an equal later set does not beat an earlier one, "the best of all rows is dated today" means today's set beats every earlier set, the definition of FR-005. Both come from the rows of R2, without another query.

## R8. The API

**Decision**: `GET /api/v1/progress?weeks=8` for the signed-in client and `GET /api/v1/clients/{id}/progress?weeks=8` for a trainer, both answering `ProgressResponse` (contracts/progress-api.md). `weeks` is required; `Period` accepts 4, 8 and 12 and refuses anything else with `400 progress.period-invalid` (`DomainException`, kind INVALID), as `plan.week-out-of-range` does for a week. No published plan answers `404 plan.not-found`, as the Plan tab's endpoints do; another trainer's client answers `404 client.not-found` before anything else is read.

**Why**: One response shape for both readers keeps their numbers equal by construction (spec SC-004). The trainer's path sits under `/api/v1/clients/**`, which already needs role TRAINER, as `ClientTrainingController`'s overview does. The client's path is added to the CLIENT paths of `SecurityConfig`.

**Alternatives considered**: A `period` enum parameter (`EIGHT`): the number reads better in the address and in tests. Leaving out the weeks of no interest: the period is at most 12 weeks.

## R9. Exercise names and measures

**Decision**: Exercises are named from the client's trainer's library in `OwnPlanView`, in the request's language (`exercise.getName().in(locale)`), with their measure, as the logger's response does.

**Why**: Exercises are never deleted (`ExerciseController`), and a session log only refers to exercises of the trainer's library, so every logged exercise has a name.

## R10. The chart component

**Decision**: `npx shadcn@latest add chart` adds `components/ui/chart.tsx` and Recharts. It is then fitted to the design system's section 3.7: tooltips on `popover`, grid lines on `border`, axis text `text-xs` in `muted-foreground`. `lib/chart-series.ts` maps the five meanings to their token, line width and dash: completion `--chart-1` solid 2.5 px, RPE `--chart-2` solid 2.5 px, load `--chart-3` solid 2 px, wearables `--chart-4` dotted, previous period `--chart-5` dashed 4 4. The tokens already exist in `index.css` in light and dark. #159 checks a sample chart in light and dark on a page that is not committed, since no screen uses the chart before #160.

Two details came out of that check. Recharts is 3.10, not the 3.8.0 the CLI installs: with 3.8.0 a `Line` loses its dash pattern when the reader prefers reduced motion, so the dotted and dashed series would differ by colour alone. The component's axis text selector is changed to `.recharts-cartesian-axis-tick-value`, the class Recharts 3 gives tick text; the shadcn selector matches nothing, which left the text in Recharts' grey. The tooltip formats its numbers in the active language instead of the browser's, so an English screen in a Polish browser shows `7.8`, not `7,8`.

**Why**: Task #159 asks for the component and the five tokens; one map keeps every chart's meaning fixed (story acceptance criterion 3).

**Alternatives considered**: Another chart library: `frontend/CLAUDE.md` names shadcn `chart` (Recharts).

## R11. Frontend structure

**Decision**: `features/progress` holds the API hook, the page, the tiles, the two charts, a chart card (title, subtitle, "Show as table") and the pure helpers in `findings.ts` (chart titles, the RPE target place, the exercise trend). When task #108 shows the same charts to the trainer, they move up to `components/`, as #106 moved the session state badge.

**Why**: `frontend/CLAUDE.md`: a feature does not import another feature, and shared code moves up when it is shared.

## R12. Findings, target place and units on the frontend

**Decision**: The chart titles (spec, Texts), the description for assistive technology and "within / above / below 7–8 target" are worked out on the frontend from the numbers the API gives: the sessions chart's title counts the weeks that are not running with done ≥ 70% of planned; the exercise chart's title compares the first and last week the exercise has a top set in the period; the RPE place is decided on the one-decimal value (7.0 to 8.0 is within). Loads are converted with `fromKg` and the reader's `useWeightUnit()`.

**Why**: They are wording of numbers the domain already decided, in the reader's language; the rules that define the numbers stay in the backend.

## R13. Task scope against the issues

**Decision**: #157 ("Completion, mean RPE and best set per exercise per week") builds the queries and the weekly figures; #158 ("Endpoint for the client's own statistics and, for trainers, their clients' statistics") builds the statistics the spec defines from those weeks and serves them; #159, #160 and #161 are the chart, the tab and "By exercise" as their issues name them, "recent sets" included. Nothing in this plan lies outside the five issues.
