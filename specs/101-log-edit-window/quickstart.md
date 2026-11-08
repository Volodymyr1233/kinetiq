# Quickstart: Client edits a log within 24 hours

How to prove the story works end to end. Every demo account uses the password `Kinetiq-demo-1`.

## Prerequisites

- Docker running. Backend: `./gradlew bootRun` in `backend/`. Frontend: `npm run dev` in `frontend/`; open http://localhost:5173.
- `demo.client.daily@example.com` has a session on Monday to Saturday (spec 89). No demo client has one on a Sunday, so run the steps on another day.
- To make a session 24 hours old, run this from the repository root. It sets the start of the client's latest session log to 24 hours ago:

```bash
docker compose exec postgres psql -U kinetiq -d kinetiq -c "update session_logs set started_at = now() - interval '24 hours' where id = (select l.id from session_logs l join accounts a on a.id = l.client_id where a.email = 'demo.client.daily@example.com' order by l.started_at desc limit 1)"
```

## 1. The rule (#102)

1. Sign in as `demo.client.daily` and open http://localhost:5173/app/session. Log a set with the tick, then change it: both are stored.
2. Run the statement above.
3. Until #103 is merged the logger still offers the entry panel. Change the same set again: in the browser's developer tools the request answers `409` with `"code": "session-log.locked"`, and after a reload the set still shows its earlier values.
4. Open the same set and choose "Update set" without changing anything: it answers `200`.

## 2. The locked logger (#103)

1. After step 1.2, reload http://localhost:5173/app/session. The message "Session locked" says why; every row shows its values or "Not logged" in the locked style; nothing can be selected, typed or ticked, and Tab reaches only the back link.
2. Switch the UI to Polish: the message and "Nie zapisano" are in Polish.
3. Run the statement above with `interval '23 hours 59 minutes'` instead, reload the logger, log a set and open an entry panel. Within a minute the panel closes, the rest timer goes away and the message appears, without a reload.
4. Check light and dark, 375 px and 320 px, and an empty console.

## 3. A past session from the Plan tab (#256)

1. The day after logging a set, before its 24 hours end, open the Plan tab and open that session's card: "Edit log" opens the logger on that session, and a set can be changed.
2. Run the statement above, then open the card again: "View log" opens the logger locked, as in step 2.1.
3. A past day without a session log and a day still to come offer neither action.
4. Go back from the logger: the Plan tab shows the week you came from.

## Automated

- `backend/`: `./gradlew check`; `frontend/`: `npm run check`.
- `SessionLogTest` proves each case of the rule; `SessionLogServiceTest` proves the boundary with a fixed clock and that another client's session answers 404, locked or not; `SessionLogControllerTest` proves `409 session-log.locked`.
