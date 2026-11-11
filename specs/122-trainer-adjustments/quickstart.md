# Quickstart: Trainer sees a client's adjustments

## Prerequisites

- Docker running. A fresh local database, so the demo dates are relative to today (`R__demo_check_ins.sql`, first line).
- `backend/`: `./gradlew bootRun` (local profile, demo data).
- `frontend/`: `npm run dev`; open `http://localhost:5173` in the built-in browser at 1280 px.
- Sign in as `demo.trainer@example.com` with the demo password (README) and open the client "Demo Client Daily", tab Overview.

## Automated checks

- `backend/`: `./gradlew check` passes, including `ClientTrainingViewServiceTest` (14 days, empty days, a score of 0, every outcome, session from log, plan or none), `CheckInRepositoryImplIT` (`between`), `ClientTrainingControllerTest` (new fields, `404` for another trainer's client, `403` for a client) and `HexagonalArchitectureTest`.
- `frontend/`: `npm run check` passes, including the overview tab, the logs tab and `SessionLogTable` tests.
- After the API change: `npm run gen:api` with the backend running; `schema.d.ts` is committed with it.

## Scenarios

| # | Do | Expect |
|---|---|---|
| 1 | Open the Overview tab | First card of the grid "Readiness, last 14 days": 14 columns, today on the right and marked; bars with their scores for the demo check-ins (a Ready, a Take it easier and two Recovery day bars); every other day empty; a legend with the three bands (spec US1). |
| 2 | Read the strip with a screen reader (or the accessibility tree) | 14 items, each "{date}: {score}, {band}" or "{date}: No check-in". |
| 3 | Look under the strip | Two entries, newest first: three days ago, 33, "Poor sleep and high soreness", "Original targets" with its never-started session and its state (Missed), no link; when today is a Wednesday that day is a Sunday, a rest day, so it reads "No lighter targets" with no session. Then Thursday of plan week 2, 33, the same reason, "Lighter targets", "Hinge day" Done, as a link (US2). |
| 4 | Choose the linked session | The Logs tab opens with that session chosen, URL `…/logs?session=<id>`; reload keeps it. |
| 5 | Read that log | The reason line "Recovery day: poor sleep and high soreness, so the targets were lighter."; each changed target struck → lighter, logged sets with the targets they were logged against (US3). |
| 6 | As the client in another window, check in today with 1/5/5 and choose "Use original targets"; reload the trainer's overview | Today has a Recovery day bar reading 0, visible; a new first entry "0 · Poor sleep, high soreness and high stress · Original targets" with today's session state Today. |
| 7 | `GET /api/v1/clients/{id}/overview` for a client of `demo.trainer2@example.com` while signed in as `demo.trainer` | `404 client.not-found` ([contract](contracts/overview-api.md)). |
| 8 | Switch to dark theme and to Polish | Bars use the readiness scale, nothing is red, text contrast holds, every string is translated with Polish plurals. |
| 9 | Narrow the window to 768 px | The strip fits its card, no sideways page scroll. |
| 10 | Tab through the card | Only the session links take focus, each with a visible focus ring. |
