# Quickstart: Client reviews and submits the quiz

How to see the story working and prove it. The rules are in spec.md; the answers' rules are spec 50's.

## Prerequisites

- Node 24 and the frontend's dependencies (`npm install` in `frontend/`).
- Task #56: no backend; the review reads the draft in the tab.
- Tasks #55 and #57: the backend and Docker running.

## Automated checks

- `npm run check` in `frontend/` (all tasks).
- `./gradlew check` in `backend/` (task #55).

## Browser check, task #56

Start the frontend (`npm run dev` in `frontend/`), open `http://localhost:5173/app/quiz` at 375 px and answer all seven steps.

1. Continue on step 7 shows "Review", seven done parts, "Check your answers", seven rows in step order and a disabled "Build my plan".
2. "About you" reads "29 years · 168 cm · 62.5 kg" for those answers; likes read as a list ("Weights, Yoga and Pilates"); empty notes read "No notes".
3. "Change" on Level opens step 3 with its answer. Choose another level, Continue: the review shows the new level.
4. "Change" on About you, then the browser's back: the review shows again.
5. Back on the review opens step 7.
6. Reload on the review: it shows again with every answer.
7. Clear the age on step 4, then open `/app/quiz/review` by address: step 4 is shown.
8. Keyboard only: every "Change" link is reachable with a visible focus ring and is named for its row; focus is on the heading after opening the review.
9. Set the browser's language to Polish and reload: every text changes, numbers use a decimal comma, likes are separated by commas.
10. Repeat 1–3 in dark mode. The console shows no errors.

## Browser check, tasks #55 and #57

Refined when those tasks start: sign in as the demo client, submit from the review, see the building state then the reviewing state, reload, open `/app/quiz/1` and `/app/quiz/review` (both show the waiting state), and submit from a second tab (the waiting state, nothing stored twice).
