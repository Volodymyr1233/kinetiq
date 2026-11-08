# Quickstart: Client checks in and sees readiness

## Prerequisites

- Docker running.
- Backend: `cd backend && ./gradlew bootRun` (local profile, demo data).
- Frontend: `cd frontend && npm run dev`, open `http://localhost:5173`.
- Demo account: Demo Client Daily (`demo.client.daily@example.com`, password in `R__demo_accounts.sql`), whose plan has a session Monday to Saturday. Sunday is its rest day (step 8); the other steps need a training day. On a Sunday, move its Saturday session to Sunday in the plan editor (as Demo Trainer) for the run and move it back afterwards, as the #101 runs did. A session started at `/app/session` locks 24 hours after it started (story #101), which does not affect the check-in.

## Gates

- `cd backend && ./gradlew check`: `ReadinessTest` (all 125 answer combinations against the rule, 2/4/3 → 33, 4/2/1 → 83, band edges 42/50/67/75), `CheckInServiceTest` (save, change, refusals: no plan, rest day, finished, set logged; another client's check-in never returned), `CheckInRepositoryImplIT` (unique per day, constraints), `CheckInControllerTest` (validation codes, 403 trainer, 401 visitor, CSRF), `SessionLogTrainingProgressIT`, `HexagonalArchitectureTest`.
- `cd frontend && npm run check`: tests for the check-in (saves on the third answer only, arrows move without choosing, skip and undo, retry keeps answers, change with Done and Cancel) and the readiness card (score, band text, no "Change answers" once training has started).

## In the browser (375 px, light and dark, EN and PL)

1. Sign in as Demo Client Daily on a training day. Today shows "How do you feel today?" above the session.
2. Tap sleep 4, soreness 2: nothing is saved. Tap stress 1: "Saving…", then the card: 83, Ready, "Train as planned.", "Sleep 4 · soreness 2 · stress 1". Reload: the card stays.
3. "Change answers": 4/2/1 are chosen. Choose sleep 2, soreness 4, stress 3, then "Done": 33, Recovery day, "Same plan. Stop two reps earlier."; the band uses the low readiness colour, not red. "Change answers" then "Cancel": unchanged.
4. Keyboard only: Tab into Sleep, arrow to 5 (nothing chosen), Space chooses it. Arrowing through Stress never saves; Space on a value saves. Focus lands on the card's heading; a screen reader announces the score.
5. Open `/app/session`, log one set, go back to Today: the card has no "Change answers" and says answers are fixed. A `PUT /api/v1/check-ins/today` sent anyway (from the browser's console) answers `409 check-in.training-started`.
6. Next day (or a fresh client with a session today): "Skip" shows "Check-in skipped. Your session is as planned."; switch tabs and back: still skipped; "Check in" brings it back.
7. Stop the backend and choose the third answer: the answers stay, a translated message and "Try again" show.
8. A rest day or Demo Client Finished: no check-in, no card.
9. Switch the UI to Polish and repeat 2–3: every text is translated, numbers formatted for the locale.
10. At 320 px width (200% zoom), nothing scrolls sideways; every choice is at least 48 × 48 px.
