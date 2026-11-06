# Quickstart: Client sees today's session

Every demo account uses the password `Kinetiq-demo-1`. Start the backend (`./gradlew bootRun` in `backend/`, Docker running) and the frontend (`npm run dev` in `frontend/`), then open http://localhost:5173.

## 1. A session today (#91)

Sign in as `demo.client.daily@example.com` (a session Monday to Saturday; on a Sunday it shows a rest day). On the Today tab: the date, the session's name and every exercise with sets, repetitions or seconds, load (in your unit) or "No load", RPE and rest. "Start session" is shown, disabled, with a line saying starting arrives with logging.

## 2. A rest day

Sign in as `demo.client.seen@example.com` (sessions Tuesday and Friday). On any other day: "Rest day" and the date, no session and no "Start session".

## 3. A finished plan

Sign in as `demo.client.finished@example.com`: "Your trainer is preparing your next plan." and no session.

## 4. Look and keyboard

At 375 px and 320 px there is no sideways scroll; light and dark; Polish and pounds in settings; Tab reaches the page without traps and the focus ring is visible.

## 5. Privacy (#90)

As the draft client (`demo.client.draft@example.com`) `GET /api/v1/today` is `404 plan.not-found`, the same as for `demo.client`. As the trainer it is `403`.

## Automated

`./gradlew check` in `backend/` and `npm run check` in `frontend/`. The service tests use a fixed clock across a time-zone boundary and the last Sunday of the plan.
