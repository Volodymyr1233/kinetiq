# Quickstart: Client sees PRs and their week streak

How to prove the story works end to end. Every demo account uses the password `Kinetiq-demo-1`.

## Prerequisites

- Docker running. Backend: `./gradlew bootRun` in `backend/`. Frontend: `npm run dev` in `frontend/`; open http://localhost:5173.
- `demo.trainer@example.com` trains `demo.client.daily@example.com`, whose demo session logs (`R__demo_session_logs.sql`) are finished in plan weeks 1 and 2. Their sets copy their targets, so the later logs of an exercise only equal the earlier ones: no PR, and a week streak to show. `demo.client.other@example.com` belongs to `demo.trainer2@example.com`.
- For a PR, sign in as `demo.client.daily` on a day with a session, open http://localhost:5173/app/session and log a set heavier than any before for one exercise. No demo client has a session on a Sunday.

## 1. PRs and streak, domain (#177)

No screen. `./gradlew check` in `backend/`: `PrTest` proves that a set beating every earlier one is a PR, that an equal set, the first log and a set beaten later are not, that more repetitions at the same load and a load over none are, and seconds alike; `ProgressViewServiceTest` proves that `rewards` gives the PRs of a date and the same streak as `own`, across a broken week and the running week. `CONTEXT.md` defines "PR" as in data-model.md.

## 2. "PR today" (#304)

1. After the heavier set of the prerequisites, open Progress: when that exercise is on the best-set tile, it is marked "PR today".
2. An exercise logged for the first time today is not marked: every demo exercise has earlier logs, so `ProgressViewServiceTest` (`PrToday`) proves it, together with the agreement of "PR today" with the PRs of today's session.

## 3. Session summary API (#302)

1. Finish the session of step 2. The address of the session done screen is `/app/summary/{id}`. Open http://localhost:5173/api/v1/session-logs/{id}/summary: `prs` lists the exercise with its top set (contracts/session-summary-api.md), and `weeksInARow` equals `weeksInARow` of http://localhost:5173/api/v1/progress?weeks=4.
2. When the finished session is the second of its week, `weeksInARow` already counts the current week (research R3).
3. Sign in as `demo.client.other` and open the same summary address: `404` with `"code": "session-log.not-found"`. As `demo.trainer`: `403`.

## 4. PR badges on the session done screen (#178)

1. Finish a session with a heavier set, as in the prerequisites: under the title, "PR · {exercise} {load} kg × {repetitions}", lime with ink text, as on the board "Client · Session done".
2. Log heavier sets for two exercises of one session and finish: two badges in the order of the session, only the first lime.
3. Switch the client's units to lb in Settings and open the summary again: the load is in pounds. Switch the language: the exercise name and the texts follow without a reload.
4. Sign in as `demo.trainer`, open `demo.client.daily`'s overview and its Logs tab with the same session: no PR badge, no "PR today" and no streak.
5. Check light and dark at 375 px, keyboard focus, English and Polish, no sideways scroll at 375 px and 320 px, and an empty console.

## 5. Week streak on the session done screen (#303)

1. On the session done screen of step 4, "{n} weeks in a row" stands next to the PR badges, outlined, with the same number as the "In a row" tile on Progress.
2. A streak of 0 shows nothing, and the Polish forms (1 tydzień, 2 tygodnie, 5 tygodni) are proven by `summary-page.test.tsx`.
3. Check light and dark at 375 px, English and Polish, and an empty console.

## Automated

- `backend/`: `./gradlew check`; `frontend/`: `npm run check`.
- `SessionLogServiceTest` proves the order and pairing of PRs and the streak on finish and summary; `SessionLogControllerTest` proves the response shape, `404` for another client's session and the refused trainer; `summary-page.test.tsx` proves the badges' text and order, pounds, seconds, Polish plurals and the hidden streak.
