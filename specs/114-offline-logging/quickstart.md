# Quickstart: validating offline logging

Prerequisites: `backend/` running (`./gradlew bootRun`), `frontend/` running (`npm run dev`), a demo client with a plan and today's session.

## Automated

- `cd backend && ./gradlew check`
- `cd frontend && npm run check`

## In the browser (375 px, light and dark)

1. Sign in as the demo client, open today's logger.
2. DevTools → Network → Offline. The offline banner appears; log two sets. Rows show "Saved on this phone".
3. Close the tab, reopen the logger while still offline: not supported (nothing promised).
4. Go online, reopen the logger: both sets reach the server once and show "Synced"; the banner is gone. Check the network panel: one PUT per set.
5. Offline again, log a set, then (as the server) let the session lock or make the PUT fail with 4xx: the row shows "Not synced", a reason, Retry and Discard. Retry sends the same id; Discard restores the row.
6. Sign out with a pending set, sign in as another account: the first account's set is not sent and its storage key is gone.
7. Finish session is disabled while offline or while a set is not Synced.
8. Tab through the logger: focus is visible, states are announced, nothing scrolls sideways at 320 px.

## Backend

Replay cases: same id twice stores once; another client's session answers 404; a locked session refuses (`SessionLogServiceTest`, `SessionLogControllerTest`).
