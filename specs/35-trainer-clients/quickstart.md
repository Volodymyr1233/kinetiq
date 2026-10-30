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
