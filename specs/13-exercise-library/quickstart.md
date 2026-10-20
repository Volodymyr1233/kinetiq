# Quickstart: validate the exercise library

How to prove the feature end to end after the four tasks are built. Contract: [contracts/api.md](contracts/api.md). Data: [data-model.md](data-model.md).

## Prerequisites

- Docker running (PostgreSQL via `compose.yaml`).
- Backend gate and frontend gate green:

```bash
cd backend && ./gradlew check
```

```bash
cd frontend && npm run check
```

## 1. Backend by tests

`./gradlew check` runs the exercise tests listed in [plan.md](plan.md). Expected: invariants, filtering, Polish ordering, language fallback, the 40-row guarantee, error mapping and anonymous 401 all pass.

## 2. Backend by hand

Start it with `./gradlew bootRun` in `backend/` (local profile).

- Without a session, `GET http://localhost:8080/api/v1/exercises` → `401` with `code: request.unauthenticated` and no exercise data.
- With the demo trainer's session (sign-in is not built yet; use the temporary local change in step 3), the same request → `200`, `total: 40`.
- `?category=LEGS&name=squat` → only legs exercises with "squat" in a name.
- `Accept-Language: pl` → Polish names, equipment and cues, ordered by the Polish names.
- `?category=NOPE` → `400` with a `category` field error; `name` of 101 characters → `400` with a `name` field error.

## 3. Screen (browser)

Sign-in does not exist yet, so the browser gets `401`. To see the screen:

1. In `SecurityConfig`, add `.requestMatchers(HttpMethod.GET, "/api/v1/exercises").permitAll()` locally. This is a throwaway edit.
2. Start the backend (`./gradlew bootRun`) and the frontend (`npm run dev`), open `http://localhost:5173/trainer/exercises` at 1280 px.
3. Check, in light and dark and in English and Polish:
   - heading "40 of 40 exercises", 10 chips, table and detail panel as on the board;
   - the first exercise selected; choosing another row updates the panel and underlines the row;
   - chip "Legs" then typing "squat" narrows the list; the count line changes; clearing restores it;
   - typing "zzz" shows the empty state; "Clear filters" restores the list;
   - "Not used" in the list and "Not used yet" in the panel for every exercise;
   - keyboard: Tab reaches chips, search and row buttons with a visible focus ring; Space/Enter chooses;
   - console has no errors.
4. Stop the servers and **revert the `SecurityConfig` edit**; `git diff` must show nothing outside the task's files.

## 4. Security sanity

- `git diff master -- backend/src/main/java/com/kinetiq/common` shows no change to `SecurityConfig`.
- Responses and errors contain no stack traces, SQL or class names.
