# Quickstart: Client reviews and submits the quiz

How to see the story working and prove it. The rules are in spec.md; the answers' rules are spec 50's.

## Prerequisites

- Node 24 and the frontend's dependencies (`npm install` in `frontend/`).
- Docker running and the backend started (`./gradlew bootRun` in `backend/`): the quiz is open only to a signed-in client. The review itself reads the draft in the tab.
- The demo client `demo.client@example.com` with the demo password from the README.

## Automated checks

- `npm run check` in `frontend/` (all tasks).
- `./gradlew check` in `backend/` (task #55).

## Browser check, task #56

Start the backend and the frontend (`npm run dev` in `frontend/`), sign in as the demo client at `http://localhost:5173/sign-in`, open `http://localhost:5173/app/quiz` at 375 px and answer all seven steps.

1. Continue on step 7 shows "Review", seven done parts, "Check your answers", seven rows in step order and a disabled "Build my plan".
2. "About you" reads "29 years · 168 cm · 62.5 kg" for those answers; likes read as a list ("Weights, Yoga and Pilates"); empty notes read "No notes".
3. "Change" on Level opens step 3 with its answer. Choose another level, Continue: the review shows the new level.
4. "Change" on About you, then the browser's back: the review shows again.
5. Back on the review opens step 7.
6. Reload on the review: it shows again with every answer.
7. Clear the age on step 4, then open `/app/quiz/review` by address: step 4 is shown.
8. Keyboard only: every "Change" link is reachable with a visible focus ring and is named for its row; focus is on the heading after opening the review.
9. Open `/app/today`, choose Polski in the settings sheet from the header, and go back to the review with the browser's Back button: every text changes, numbers use a decimal comma, likes are separated by commas. Choose English in the sheet again afterwards.
10. Repeat 1–3 in dark mode. The console shows no errors.

## Browser check, task #57

Task #55 has no screen; its tests cover it (`./gradlew check`). Start the backend and the frontend and work at 375 px. A submit of the demo client stays in the local database until the database is recreated.

1. Sign in as the demo client, who has not submitted, and open `/app/quiz/waiting` by address: step 1 shows.
2. Answer all seven steps. On the review, duplicate the tab; the copy keeps the answers.
3. Stop the backend and choose "Build my plan" in the first tab: the review stays with a message and "Try again". Start the backend again.
4. Choose "Try again": "Building your plan from 7 answers…" shows while the answers are sent, then "Your trainer is reviewing your plan" with its text, and focus is on its heading.
5. Return to the copy: the status is read again on return (spec FR-016), so the copy shows the waiting state before a second submit can be sent, and the database holds one submission. A second submit that still arrives is refused as `quiz.already-submitted` and also ends in the waiting state; the tests cover it.
6. Reload: the reviewing state shows again. Open `/app/quiz/1` and `/app/quiz/review` by address: both show the waiting state.
7. Sign in as `demo.client.waiting@example.com` and open `/app/quiz`: the waiting state shows at once. Check it in Polish and in dark mode.
8. Keyboard only: "Build my plan" and "Try again" are reachable with a visible focus ring. The console shows no errors.

The building state shows only while the answers are sent, so its Polish text and plural forms are covered by the tests.
