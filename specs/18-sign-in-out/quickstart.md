# Quickstart: Trainer and client sign in and out

## Prerequisites

- Docker running.
- `backend/`: `./gradlew bootRun` (local profile, demo data).
- `frontend/`: `npm run dev`, open `http://localhost:5173`.
- Demo accounts, all with the demo password from the README: `demo.trainer@example.com` (trainer), `demo.unconfirmed@example.com` (trainer, unconfirmed), `demo.client@example.com` (client).

## Automated

| Side | Command | Expected |
|---|---|---|
| backend | `./gradlew check` | all tests and architecture rules pass |
| frontend | `npm run check` | lint, format, tests, types, build pass |

## Scenarios in the browser

Check each at 1280px (trainer) or 375px (client), light and dark, English and Polish.

1. **Sign in, land in the area**: open `/sign-in`, sign in as the demo trainer → `/trainer/clients`. Sign out, sign in as the demo client → `/app/today`.
2. **Same answer**: sign in with `nobody@example.com` and with the demo trainer's email and a wrong password → the same "Email or password is wrong" alert; in the network panel both are `401` with identical bodies.
3. **Locked**: fail five times for one email; the sixth attempt, also with the right password, shows "Too many attempts. Try again in 15 minutes." Do the same for an email without an account → same message.
4. **Unconfirmed**: sign in as `demo.unconfirmed@example.com` with the right password → "Confirm your email" naming the address; "Resend link" → disabled, "Link sent again."; the link appears in the backend log. With a wrong password → the wrong-credentials alert instead.
5. **Return to the page**: signed out, open `/trainer/exercises?from=mail` → sign-in; sign in as the trainer → `/trainer/exercises?from=mail`. Repeat and sign in as the client → `/app/today`.
6. **Other area**: as the client open `/trainer` → `/app/today`; as the trainer open `/app/quiz` → `/trainer/clients`.
7. **Survives restart**: signed in, stop and start `bootRun`, reload → still signed in.
8. **Sign out clears the tab**: as the client answer two quiz steps, open settings, sign out, sign in as the client again → the quiz starts empty. Press "back" after signing out → sign-in, no data.
9. **Expired elsewhere**: signed in in two tabs, sign out in one, click a sidebar link in the other → sign-in, then back to that page after signing in.
10. **Already signed in**: open `/sign-in` while signed in → own area.
11. **Keyboard**: tab through the sign-in form; after a failed attempt the alert is announced and focus is on the password field; show/hide has an accessible name; the sign-out buttons are reachable and labelled.

## Checks by request

- After sign-in the `SESSION` cookie is `HttpOnly`, `SameSite=Lax` (and `Secure` outside the local profile), and its value differs from any cookie before sign-in.
- `select count(*) from spring_session` does not grow while signed out and clicking around public pages.
- Replay `GET /api/v1/accounts/me` with the cookie copied before sign-out → `401 request.unauthenticated`.
- As the client, `GET /api/v1/exercises` → `403 request.not-allowed`.
