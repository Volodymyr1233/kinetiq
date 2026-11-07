# Quickstart: Client logs sets

Every demo account uses the password `Kinetiq-demo-1`. Start the backend (`./gradlew bootRun` in `backend/`, Docker running) and the frontend (`npm run dev` in `frontend/`).

## 1. Open the logger (#95)

Sign in as `demo.client.daily@example.com` (a session Monday to Saturday; on a Sunday the logger sends you back to Today). Open http://localhost:5173/app/session. The session starts: its name, then each exercise with one numbered row per planned set and the targets in grey. Reload: the same session.

## 2. A set with one tick (#96)

Tick an untouched row: it is logged with the target values and shows as done. Reload: still logged.

## 3. A set with changed values (#96)

Select a row: the entry panel shows the targets. Change the load by a step (2.5 kg, or 5 lb in pounds), the repetitions, and the RPE chip; tick. 0 repetitions cannot be logged and the field says why. Select the logged row, change a value, tick: updated, not added.

## 4. The rest timer (#97)

After a tick a countdown of the exercise's rest starts. Log the next set while it runs: it works and the countdown restarts. Dismiss it: it goes. Reload: no countdown.

## 5. Look and keyboard

375 px and 320 px, no sideways scroll; light and dark; Polish and pounds; Tab through rows, panel and chips; controls at least 48 px.

## 6. Privacy and idempotency (#94)

With DevTools, send the same `PUT` twice: one set. As another demo client, `PUT` to the first client's session: `404 session-log.not-found`. As the trainer: `403`.

## Automated

`./gradlew check` in `backend/` and `npm run check` in `frontend/`.
