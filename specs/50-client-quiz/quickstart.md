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
9. Set the browser's language to Polish and reload (the app follows the browser's language): every text changes, answers stay, the weight example reads "np. 62,5".
10. Repeat 1–5 in dark mode. The console shows no errors.
