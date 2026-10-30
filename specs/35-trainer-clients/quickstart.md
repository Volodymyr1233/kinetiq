# Quickstart: Client list API (#36)

Docker must be running. Commands in `backend/`.

## 1. Automatic checks

```
./gradlew test --tests '*ClientControllerTest' --tests '*JdbcClientDirectoryIT' --tests '*HexagonalArchitectureTest'
./gradlew check
```

Expected: `BUILD SUCCESSFUL`.

## 2. By hand

`./gradlew bootRun` (`$env:SERVER_PORT="8081"` if 8080 is taken). In the Swagger UI (`/swagger-ui.html`) or with the browser signed in as `demo.trainer@example.com` (password in the README):

- `GET /api/v1/clients` returns the demo clients of the demo trainer, ordered by name; the one with a quiz has a `goal` and `PLAN_TO_REVIEW`, the one without has no `goal` and `NO_QUIZ`.
- Signed in as `demo.trainer2@example.com`: an empty `clients` list.
- Signed in as `demo.client@example.com`: `403`. Signed out: `401`.

## 3. Contract

With the backend running, in `frontend/`: `npm run gen:api`. `schema.d.ts` gains `ClientListResponse` and `ClientResponse` and nothing else changes; then `npm run check`.

## 4. The screen (#37)

`npm run dev` in `frontend/` with the backend running; sign in as a trainer who has clients (the demo trainer, password in the README).

- `/trainer/clients` shows the clients in a table: client, goal, plan, last log. A client who answered the quiz shows a goal and "Plan to review"; one who did not shows "No quiz yet"; last log reads "None yet".
- Type part of a name in the search box: the table narrows, whatever the letter case; the line under the title counts "2 of 3 clients". Text that matches nobody shows "No client matches that name" with "Clear search".
- A trainer with no clients (the second demo trainer) sees "No clients yet" and "Invite client", which opens the invites page. "Open invites" in the header does the same.
- Check it at 1280 px in light and dark, switch the language to Polish, and use Tab: the search box, the buttons and the links all show a focus ring.


## 5. The client page (#38)

- In the table, choose a client's name: `/trainer/clients/<id>` shows the name, "Goal: ..." and the plan state, with the tabs Overview, Plan, Logs and Messages. Each tab has its own address and says it fills in later; a reload keeps the tab; the arrow keys move between tabs.
- Open `/trainer/clients/<any other id>`, and the id of a client of the second demo trainer: both show "Client not found" with a way back to all clients.
- Check it at 1280 px in light and dark, and in Polish.
