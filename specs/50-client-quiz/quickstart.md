# Quickstart: Client answers the quiz

How to see the feature working and prove it. The rules themselves are in spec.md and data-model.md.

## Prerequisites

- Node 24 and the frontend's dependencies (`npm install` in `frontend/`).
- No backend for tasks #52 and #51: the quiz needs no data. Task #53 needs the backend with task #25 and the quiz status read of story #54 merged.

## Automated checks

In `frontend/`:

```bash
npm run check
```

ESLint, Prettier, all tests (the quiz tests listed in plan.md among them), type check and build must pass.

## Browser check (task #51; task #52 uses a throwaway local page, research R11)

Start the frontend (`npm run dev` in `frontend/`) and open `http://localhost:5173/app/quiz` at 375 px.

1. Step 1 shows "Step 1 of 7", five options, no Back and a disabled Continue. Choose one; Continue is enabled.
2. Go through steps 2–3. On step 4 type age 12, leave the field: "Enter an age between 16 and 99." appears. Type 29: it disappears. Enter height 168 and weight 62,5.
3. Step 5 opens with nothing chosen. Choose two, clear both: Continue is disabled again.
4. Step 6 shows 3. Press − twice: it stops at 2, and − is disabled.
5. Step 7 reads "Skip and continue". Type text: it reads "Continue" and the counter counts. Paste 600 characters: 500 are kept.
6. Press the browser's back twice: step 5 with its answers. Reload: still step 5, answers kept.
7. Open `/app/quiz/7` by address after reaching only step 5: step 5 is shown.
8. Keyboard only: Tab reaches every option, field and button with a visible focus ring; arrow keys move within the radio group; Space toggles a checkbox. After Continue, focus is on the new heading.
9. Open `/app/today`, choose Polski in the settings sheet from the header, and go back to the step with the browser's Back button: every text changes, answers stay, the weight example reads "np. 62,5". Choose English in the sheet again afterwards.
10. Repeat 1–5 in dark mode. The console shows no errors.

## Browser check, task #216

Start the backend (`./gradlew bootRun` in `backend/`) and the frontend, sign in as the demo client and work at 375 px.

1. On step 1 the header shows the Settings button on the right; on steps 2–7 and on the review it sits after Back. It opens the settings sheet with units, language, theme and "Sign out".
2. On step 4 type weight 62,5 and choose lb in the sheet: the field shows 137.8 in lb. Choose kg again: 62.5.
3. Choose Polski and then the dark theme in the sheet: the quiz changes at once, answers stay. Choose English and the system theme again.
4. Sign in as `demo.client.waiting@example.com` and open `/app/quiz/waiting`: the Settings button sits above the heading and opens the same sheet.
5. Choose "Sign out" there: the sign-in page opens. Sign in as the demo client, whose quiz answers in this tab are gone.
6. Keyboard only: the Settings button is reachable with a visible focus ring and is named "Settings"; Escape closes the sheet and returns focus to it. The console shows no errors.

## Browser check, task #53

Start the backend (`./gradlew bootRun` in `backend/`) and the frontend, and work at 375 px.

1. Sign in as `demo.client@example.com`, who has not submitted the quiz: step 1 of the quiz opens, without the tab bar. Open `/app/today`, `/app/plan` and `/app/messages` by address: each shows the quiz.
2. Sign out from the settings sheet and sign in as `demo.client.waiting@example.com`: the waiting state opens. Open `/app/today` by address: the waiting state again.
3. Repeat 1 in Polish and in dark mode. Keyboard only: after being sent to the quiz, Tab reaches its options with a visible focus ring. The console shows no errors.

A client with a published plan cannot be shown in the browser at the time of this story: plans are published by the plan stories, so the tests cover it (plan.md, Tests).

## Browser check, task #226

Start the backend (`./gradlew bootRun` in `backend/`) and the frontend, and work at 375 px.

1. Sign in as `demo.client@example.com`, answer step 1 and continue. Reload: the answer stays.
2. Duplicate the browser tab, sign out in the first tab and sign in there as `demo.client.waiting@example.com`. Reload the second tab: it shows the waiting state, and its session storage (the browser's developer tools) no longer holds `kinetiq.quiz`. The console shows no errors.
