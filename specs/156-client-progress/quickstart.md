# Quickstart: Client sees their progress

How to prove the story works end to end. Every demo account uses the password `Kinetiq-demo-1`.

## Prerequisites

- Docker running. Backend: `./gradlew bootRun` in `backend/`. Frontend: `npm run dev` in `frontend/`; open http://localhost:5173.
- `demo.trainer@example.com` trains `demo.client.daily@example.com` (id `00000000-0000-4000-8000-000000000012`), whose demo session logs (`R__demo_session_logs.sql`) are finished in plan weeks 1 and 2, with Missed days between them. `demo.client.other@example.com` (id `00000000-0000-4000-8000-000000000020`) belongs to `demo.trainer2@example.com`. `demo.client.draft@example.com` has only a draft.
- The demo sets copy their targets, so each exercise's top set is flat. For a rising line, sign in as `demo.client.daily` on a day with a session, open http://localhost:5173/app/session and log a set heavier than any before, then finish the session. No demo client has a session on a Sunday.

## 1. Statistics queries (#157)

No screen yet. `./gradlew check` in `backend/`: `JdbcTrainingLogIT` proves the top set of each exercise in each session (a load beats no load, ties by repetitions and then by place), the RPE sums, the finished days and that another client's logs are never read; `TrainingWeekTest` proves the weekly sessions planned and done around the publish day, the mean RPE and the weekly top sets.

## 2. Statistics API (#158)

1. Sign in as `demo.client.daily` and open http://localhost:5173/api/v1/progress?weeks=4. The answer has `goal`, `week`, `weeks` with `planned`, `done` and `averageRpe` per week, the period's `sessionsDone`, `sessionsPlanned` and `averageRpe`, `weeksInARow`, and `exercises` with `best`, `weeks` and `recent` (contracts/progress-api.md).
2. Open it with `weeks=5`: `400` with `"code": "progress.period-invalid"`.
3. Sign in as `demo.trainer` and open http://localhost:5173/api/v1/clients/00000000-0000-4000-8000-000000000012/progress?weeks=4: the same answer as in step 1.
4. Open the same address with `00000000-0000-4000-8000-000000000020`: `404` with `"code": "client.not-found"`, the same as for a made-up id.
5. As `demo.client.daily`, open the trainer's address: `403`. As `demo.client.draft`, open the client's address: `404` with `"code": "plan.not-found"`.

## 3. Chart (#159)

1. Render a sample chart with two series from `lib/chart-series.ts` on any page, without committing it.
2. In light and dark: the series use their fixed colours and line styles, the grid lines and axis text are muted, the tooltip has the popover background. Remove the sample.

## 4. Progress tab (#160)

1. Sign in as `demo.client.daily` and open Progress. It shows the goal and plan week, the switch on 8 weeks, the tiles "Sessions done", "Average RPE", the best set of one exercise and "In a row", and the sessions chart, as on the board "Client · Progress".
2. Switch to 4 and 12 weeks: every figure follows; weeks before week 1 never appear.
3. The sessions chart marks the current week as running with the sessions it has left, and its title counts the completed weeks at 70% or more. "Show as table" shows the same numbers as a table.
4. Average RPE says within, above or below the 7–8 target in words; the comparison with the week before shows an arrow and its value.
5. Sign in as `demo.client.draft`: the waiting state stands in for every tab until a plan is published (spec 50), Progress included. The tab's own text for a client without a published plan is proven by `progress-page.test.tsx`.
6. Switch the client's units to lb in Settings: loads are in pounds.
7. Check light and dark at 375 px, keyboard focus and names, English and Polish, and an empty console.

## 5. By exercise (#161)

1. On Progress, "By exercise" lists the exercises of the period, the one in the most sessions first and chosen, the same as the best-set tile.
2. The chart shows the top set and the average RPE per week, the RPE 7–8 band, and a title with the change of the top set; "Show as table" shows the same numbers.
3. The 3 most recent sessions with the exercise are listed with date, top set and RPE.
4. After the heavier set of the prerequisites, the tile shows "PR today".
5. Check light and dark at 375 px, keyboard focus, the chart's description for assistive technology, English and Polish, and an empty console.

## Automated

- `backend/`: `./gradlew check`; `frontend/`: `npm run check`.
- `ProgressViewServiceTest` proves the period, the comparisons, the weeks in a row, "PR today", the exercise order and that another trainer's client is not found; `ProgressControllerTest` proves both endpoints' shape, `400 progress.period-invalid`, `404` and the refused roles.
