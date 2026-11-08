# Quickstart: Client finishes a session

How to prove the story works end to end. Every demo account uses the password `Kinetiq-demo-1`.

## Prerequisites

- Docker running. Backend: `./gradlew bootRun` in `backend/`. Frontend: `npm run dev` in `frontend/`; open http://localhost:5173.
- `demo.client.daily@example.com` has a plan with a session on Monday to Saturday, in week 3 of 8, published on the Wednesday of week 1 (Monday and Tuesday of week 1 have no state). Run the steps on a day from Monday to Saturday.
- "Start session" on Today is disabled until another task enables it. Open the logger by its address, http://localhost:5173/app/session, which starts today's session.
- Past days of the plan carry demo session logs (Done) and days without one (Missed), all older than 24 hours.
- To see a late finish, make today's session log old (it sets the start of the client's latest log to 25 hours ago):

```bash
docker compose exec postgres psql -U kinetiq -d kinetiq -c "update session_logs set started_at = now() - interval '25 hours' where id = (select l.id from session_logs l join accounts a on a.id = l.client_id where a.email = 'demo.client.daily@example.com' order by l.started_at desc limit 1)"
```

## 1. Finish and see the summary (#99, #100)

1. Sign in as `demo.client.daily`, open the Plan tab: today's session is **Today**, later days of the week **Planned**, past days **Done** or **Missed**, the first two days of week 1 (use the previous-week control) show no badge.
2. Open http://localhost:5173/app/session. In the footer "Finish session" is disabled and says a set must be logged first. Log one set with the tick: it becomes available and the footer says "1 of N sets logged".
3. Open the Plan tab again: today's session is **In progress**.
4. Back in the logger, choose "Finish session": a confirmation names the sets not logged. "Keep logging" closes it with nothing finished. Choose "Finish session" again: the done screen shows sets "1 of N", volume, average RPE and time in session.
5. Tick the other sets, finish again: no question this time, since nothing is missing.
6. Reload the done screen: the same figures. Open the Plan tab: today's session is **Done**; the week line counts it among the sessions done this week. Open Today: the session is shown as done with "View summary" and no start action.

## 2. Summary matches the sets (#100)

1. In the logger (still within 24 hours) change one logged set's load and RPE. The footer now reads "View summary"; open it: volume and average RPE reflect the change.
2. In Settings switch the unit to lb: the volume is shown in lb.
3. Use an exercise measured in seconds (Plank in the Daily plan's Saturday session): its sets count in "sets" and average RPE, not in volume. A session of only such sets shows volume "—".

## 3. The note (#100)

1. On the done screen type a note, choose "Save note": a confirmation is announced, and after a reload the note is still there. Save it empty: it is removed.
2. A note of 501 characters is refused with a message under the field; the counter stops at 500.
3. After the late-finish statement above, the note field is read-only with the lock message.

## 4. Late and private (#99)

1. Start a new session, log one set and run the statement above, then try "Finish session": it is refused with "Session locked", the logger turns read-only (spec 101). The Plan tab shows the session as **Missed**.
2. As another demo client (for example `demo.client.published`), call `POST /api/v1/session-logs/<an id of the first client>/finish`, `GET .../summary` and `PUT .../note` in the developer tools: each answers `404 session-log.not-found`. As a trainer: `403`. Not signed in: `401`.
3. Open http://localhost:5173/app/summary/<id of a session that is not finished>: you are sent to the logger (`/app/session`, which shows today's session, or Today when there is none, until #256 opens earlier sessions); a made-up id shows the not-found page.

## 5. Looks and checks

- Light and dark; 375 px and 320 px (200% zoom); Polish and English; Tab order and visible focus on the footer, the dialog (focus moves in and returns), the note field and the "View summary" links; empty console.
- Backend gate in `backend/`: `./gradlew check`. Frontend gate in `frontend/`: `npm run check`. `HexagonalArchitectureTest` is in the first.
