# Quickstart: Today's targets are lightened on a recovery day

## Prerequisites

- Docker running.
- `backend/`: `./gradlew bootRun` (local profile, demo data).
- `frontend/`: `npm run dev`; open `http://localhost:5173` in the built-in browser at 375 px.
- Sign in as `demo.client.daily@example.com` (password in `R__demo_accounts.sql`). The client has a session today and no check-in.

## Automated checks

- `backend/`: `./gradlew check`. It must pass, including `AdjustmentTest`, `CheckInServiceTest`, `SessionLogServiceTest`, `CheckInControllerTest` and the repository `IT`.
- `frontend/`: `npm run check`. It must pass.

## Scenarios

| # | Do | Expect |
|---|---|---|
| 1 | Check in 2/4/3 | Readiness 33, Recovery day, "Today's targets are lighter."; the session reads "Adjusted for today", the reason "Poor sleep and high soreness, so today's targets are lighter.", and each changed target struck → new (spec US1). Plan tab: planned values. |
| 2 | Tap "Use original targets" | Planned targets with no arrows, the mark "Original targets", the band line "Same plan. Stop two reps earlier if you need to.", and one "Original targets restored" message with "Undo" that closes after 5 s, stays while you change tab, and is replaced, never stacked, when you switch again; focus is on "Use lighter targets". |
| 3 | Tap "Undo" | Lighter targets again. Reload: they are still in use. |
| 4 | Change answers to 4/2/1, then back to 2/4/3 | Planned targets, then lighter again (US4). |
| 5 | Start the session and tick a set as planned | The logger shows one "Adjusted" mark beside the date and old → new under the exercise name, and the set is logged with the lighter values. Back on Today: no switch, "Continue session", and the readiness card reads "Answers and targets are fixed once training has started." |
| 6 | `PUT /api/v1/check-ins/today/targets` after the set | `409 check-in.training-started`, nothing changed ([contract](contracts/targets-api.md)). |
| 7 | Settings → units lb, on a new day with 2/4/3 | Loads read in lb, rounded to a whole pound (within 0.1 lb). |
| 8 | As the trainer, open the client's logs | The set shows the lighter target copy. |
| 9 | Switch to dark theme and to Polish | Contrast holds, nothing is red, all strings are translated, and there is no sideways scroll at 375 and 320 px. |
| 10 | Go offline or block the `PUT`, then tap the switch | The targets return to what is stored, with a translated message (FR-023). |
| 11 | Open the Plan tab | Today's session shows "Adjusted" beside its state while the lighter targets are in use, and not with the original ones; in the previous week, the demo recovery day on Thursday shows it too (FR-028). |
