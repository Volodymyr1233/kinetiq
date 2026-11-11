# Quickstart: check that messages arrive without reloading

## Prerequisites

- Docker running; JDK 25 on `JAVA_HOME`.
- Backend: `./gradlew bootRun` in `backend/` (local profile, demo data with a demo trainer and demo clients and their conversations).
- Frontend: `npm run dev` in `frontend/` (proxies `/api` to `:8080`).
- Two browser contexts with separate cookies (the built-in browser plus a second tab in a private window, or two tabs signed in as different demo accounts in separate profiles).

## Automated

| Side | Command | Proves |
|---|---|---|
| backend | `./gradlew check` | who is told (`MessagingServiceTest`), the registry rules (`ConversationStreamsTest`), and a real stream over the session cookie receiving `event:message` after commit, a signed-out stream refused and a stream closed after its session is deleted (`ConversationStreamIT`) |
| frontend | `npm run check` | one stream per account, cache updates per event, catch-up on open, reconnect backoff and stop on 401 (`use-conversation-events.test.tsx`) |

## Manual

1. **Stream through the proxy**: signed in, open `http://localhost:5173/api/v1/conversations/events` in a tab: `event: ping` appears at once and again every 25 s. Signed out (private window), the same address answers `401`.
2. **Live message (Story 1)**: trainer at `/trainer/messages/<client>` (1280 px), client at `/app/messages` (375 px). Send from each side: the other side shows it in under 2 s, the trainer's row moves to the top. Network panel: no repeated requests to `/conversations` or `/messages` while idle.
3. **Counts everywhere (Story 2)**: client on `/app/today` in two tabs; trainer sends two messages: both tabs show 2 on Messages. Open Messages in one tab: the other's count clears in under 2 s. The trainer's screen does not change when the client reads.
4. **Waiting state**: a demo client with a submitted quiz and no published plan at `/app/quiz/waiting`, sheet open; the trainer writes: the message appears in the sheet.
5. **Reconnect (Story 3)**: with both screens open, stop the backend, start it again: within 30 s of it being up, a new message arrives live. Cut the network of one tab (devtools offline), send from the other, restore: the message and count appear within 30 s.
6. **Sign-in ended (Story 4)**: with the client's stream open, reset the client's password from another browser (or sign out in another tab of the same browser): within 1 minute the stream closes, and the app sends the client to sign-in.
7. **Cap**: open 11 tabs of one sign-in: the first tab's stream closes and it reconnects; the server never holds more than 10 for that sign-in (backend log at debug, or the registry test).
8. **Look**: nothing on screen changed; light and dark, 375 px and 1280 px, EN and PL show the same screens as before. A live message is announced by the screen reader region of spec 58 and does not move the focus or the scroll position of someone reading earlier messages.
