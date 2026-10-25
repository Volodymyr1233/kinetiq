# Quickstart: Trainer invites a client

## Prerequisites

- Docker running.
- `backend/`: `./gradlew bootRun` (local profile, demo data and mail logging).
- `frontend/`: `npm run dev`, open `http://localhost:5173`.
- Demo accounts with the demo password from the README: `demo.trainer@example.com` (has demo invites), `demo.client@example.com`.
- Demo invites are inserted relative to the first local start; on an older database they may have expired. Recreate the database (`docker compose down -v`) to get them fresh.

## Automated

| Side | Command | Expected |
|---|---|---|
| backend | `./gradlew check` | all tests and architecture rules pass |
| frontend | `npm run check` | lint, format, tests, types, build pass |

## Scenarios in the browser

Check each at 1280px, light and dark, English and Polish, with the keyboard only at least once.

1. **List**: sign in as the demo trainer, open Clients → "Open invites" (`/trainer/clients/invites`) → the demo open invites, newest first; the one on its last day reads "In 1 day" with the warning badge; the expired and revoked ones and the other trainer's are absent.
2. **Invite**: "Invite client" → enter ` Julia@Example.com ` → "Send invite" → "Invite sent to julia@example.com" with the link; the backend log shows `Mail not delivered, to=julia@example.com` with a different link. "Copy link" → "Copied" and "Link copied". Done → the invite tops the list with "In 7 days"; focus is back on "Invite client".
3. **Invalid email**: send `julia@` → "Enter an email like name@example.com."; focus on the field; typing clears the error.
4. **Has an account**: invite `demo.client@example.com` → "This email already has an account."; nothing in the mail log.
5. **Invite again**: invite `julia@example.com` again → one row for it, "In 7 days", a new mail in the log.
6. **Copy new link**: on a row, "Copy new link" → clipboard holds a new link, "New link copied"; focus or hover explains that the earlier link stops working.
7. **Revoke**: "Revoke" → "Revoke the invite for …?" with focus on Cancel; Escape keeps it; "Revoke invite" → row gone, "Invite revoked".
8. **No longer open**: open the page in two tabs, revoke an invite in one, revoke the same in the other → "This invite is no longer open." and the list refreshes.
9. **Empty**: revoke every open invite → "No open invites. Invites you send show up here until the client joins." with "Invite client".
10. **Limit**: send 21 invites within an hour (refused ones count) → the 21st shows "You've sent many invites. Try again later."
11. **Another trainer's invite**: with the id of the demo unconfirmed trainer's invite (see `R__demo_invites.sql`), call `DELETE /api/v1/invites/{id}` from the browser console as the demo trainer → `404 invite.not-found`.
12. **Client refused**: as the demo client, `GET /api/v1/invites` → `403 request.not-allowed`.
