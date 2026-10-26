# Quickstart: validating "Trainer adds and edits their own exercise"

Prerequisites: Docker running; `backend/` and `frontend/` dependencies installed.

## Automated

```bash
cd backend && ./gradlew check
cd frontend && npm run check
```

Both must pass. After changing the API: start the backend, then in `frontend/` run `npm run gen:api` and commit `schema.d.ts` with the backend commit.

## By hand

Start `./gradlew bootRun` in `backend/` and `npm run dev` in `frontend/`. Demo accounts and password: see the README. Use the built-in browser at 1280px.

1. Sign in as `demo.trainer@example.com`, open "Exercise library". The demo trainer's own exercises are listed with the starter ones; a starter exercise shows the "Starter" badge and no "Edit".
2. Choose "Add exercise", save with an empty name: "Enter a name.", focus on Name.
3. Add "Goblet squat" (Legs): the dialog closes, filters reset, the exercise is selected, a toast says it was added, the count grew by one.
4. Add "plank" (starter name, any case) and "deska": both refused with "An exercise with this name is already in the library." Switch the UI to Polish and repeat with "Plank".
5. Add a timed exercise (Seconds): the amount is labelled "Seconds" and the list shows "3 × 45 s".
6. Select the new exercise, "Edit", change rest and add cues, save: the panel shows both. Save an edit without changing the name: no clash.
7. Press Escape in the dialog: focus returns to the button that opened it. Tab through the dialog: focus stays inside.
8. Sign out, sign in as `demo.trainer2@example.com`: none of the first trainer's own exercises appear in the list, search or count; adding "Goblet squat" works.
9. In the developer tools (or `curl` with the session and `X-XSRF-TOKEN`): `PUT /api/v1/exercises/<a starter id>` and `PUT` on the first trainer's exercise id as trainer two → `404 exercise.not-found`; `DELETE /api/v1/exercises/<id>` → `405`.
10. With the session and `X-XSRF-TOKEN`, create exercises in a loop until the demo trainer owns 200: the next `POST` answers `409 exercise.limit-reached`, and the dialog shows "You've reached the limit of 200 own exercises." as an alert. Edits still work.
11. Narrow the browser to 375px: the dialog fits the screen and scrolls inside.
12. Check light and dark, English and Polish, the console for errors.
