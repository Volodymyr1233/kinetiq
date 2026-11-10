# Quickstart: Trainer sees a client's training

How to prove the story works end to end. Every demo account uses the password `Kinetiq-demo-1`.

## Prerequisites

- Docker running. Backend: `./gradlew bootRun` in `backend/`. Frontend: `npm run dev` in `frontend/`; open http://localhost:5173.
- `demo.trainer@example.com` trains `demo.client.daily@example.com` (id `00000000-0000-4000-8000-000000000012`), whose demo session logs (`R__demo_session_logs.sql`) are Done and locked, with Missed days between them. `demo.client.other@example.com` (id `00000000-0000-4000-8000-000000000020`) belongs to `demo.trainer2@example.com`.
- A session started less than 24 hours ago: sign in as `demo.client.daily`, open http://localhost:5173/app/session and log a set (In progress), then finish it (Done). No demo client has a session on a Sunday, so do this on another day.
- To lock that session, run this from the repository root. It sets the start of the client's latest session log to 24 hours ago:

```bash
docker compose exec postgres psql -U kinetiq -d kinetiq -c "update session_logs set started_at = now() - interval '24 hours' where id = (select l.id from session_logs l join accounts a on a.id = l.client_id where a.email = 'demo.client.daily@example.com' order by l.started_at desc limit 1)"
```

## 1. Overview API (#105)

1. Sign in as `demo.trainer` and open http://localhost:5173/api/v1/clients/00000000-0000-4000-8000-000000000012/overview. The answer has `today`, `week` with the current week's sessions and their `state`, `recentSessions` with four sessions newest first, each with `setsLogged`, `setsPlanned` and `averageRpe`, and `quizAnswers`.
2. A Done session older than 24 hours has `"locked": true`; the session finished in the prerequisites has `"locked": false` until the statement above is run.
3. A missed day without a session log is in `recentSessions` with `"state": "MISSED"`, `"setsLogged": 0` and no `averageRpe`.
4. Open the same address with `00000000-0000-4000-8000-000000000020`: `404` with `"code": "client.not-found"`, the same as for a made-up id.
5. Sign in as `demo.client.daily` and open the first address: `403`.
6. As `demo.trainer`, open the overview of `demo.client.draft@example.com` (a draft, no published plan) and of `demo.client.finished@example.com` (a finished plan): no `week`; the quiz answers still show when the client submitted the quiz.

## 2. Overview tab (#106)

1. As `demo.trainer`, open Clients, then Demo Client Daily. The Overview tab shows "This week" with its day strip and sessions, "Recent sessions" with labels Done, Locked or Missed, and "Quiz answers", as on the board "Trainer · client overview".
2. "All logs" opens the Logs tab.
3. Demo Client Draft shows "No published plan yet."; Demo Client Finished shows "The plan has finished."; a client before the quiz shows "No quiz answers yet."
4. Switch the trainer's units to lb in Settings: the quiz weight is in pounds.
5. Check light and dark at 1280 px, keyboard focus and names, English and Polish, and an empty console.

## 3. Logs tab (#107, #261)

1. Open the Logs tab of Demo Client Daily: every session since the plan was published, newest first, each labelled; the newest is shown.
2. Choose a Done session: each exercise lists its planned sets with target and logged values, and the line with sets, minutes and average RPE.
3. Choose a Missed day without a log: it says the session was not started.
4. The session finished in the prerequisites says until when the client can still change it; after the statement above it is Locked.
5. After the client saves a note on a finished session (spec 98), the trainer sees it with that session (#261).

## 4. Progress (#108)

1. Sign in as `demo.client.daily`, open Progress and note every figure for 4, 8 and 12 weeks.
2. Sign in as `demo.trainer` and open Demo Client Daily. The Overview starts with the period switch on 8 weeks, the tiles Sessions done, Average RPE and the best set, then the sessions chart beside "By exercise". For each period the figures, the chart titles and the tables of "Show as table" equal the client's; there is no "In a row" and no "PR today".
3. Demo Client Draft shows no progress section; its cards say "No published plan yet."
4. Check light and dark at 1280 px, keyboard focus and names, English and Polish, and an empty console.

## Automated

- `backend/`: `./gradlew check`; `frontend/`: `npm run check`.
- `ClientTrainingViewServiceTest` proves each label with a fixed clock, the four recent sessions, a Missed session never started, the plan states and that another trainer's client is not found; `ClientTrainingControllerTest` proves the answer's shape, `404 client.not-found` and the refused client; `SessionLogRepositoryImplIT` proves that every session log of a client is read with its sets.
