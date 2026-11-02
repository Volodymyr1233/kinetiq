# Quickstart: validating messaging

Run after tasks #59 to #62, and again as the whole-story review. Demo accounts and password are in the README (`Kinetiq-demo-1`): Demo Trainer, Demo Client, Demo Client Waiting, Demo Trainer Two. See [data-model.md](data-model.md) for the demo conversations and [contracts/api.md](contracts/api.md) for the API.

## Prerequisites

- Docker running. In `backend/`: `./gradlew bootRun` (local profile starts PostgreSQL and loads the demo data).
- In `frontend/`: `npm run dev`, then open `http://localhost:5173`.
- Two browser sessions at once: the built-in browser for one person and a private window for the other (a sign-in is per browser).

## 1. Automated gates

| What | Where | Command | Expected |
|---|---|---|---|
| Backend | `backend/` | `./gradlew check` | passes, including `HexagonalArchitectureTest` and the `*IT` tests |
| Frontend | `frontend/` | `npm run check` | passes |
| API types | `frontend/` (backend running) | `npm run gen:api` | no diff after task #60 |

## 2. Client at 375 px, light and dark

Sign in as Demo Client, open Messages.

1. The conversation with the trainer's name opens directly; the newest message is in view; the tab bar and the composer are both visible. Send is disabled while the field is empty.
2. Type a message, press Enter: it appears at once as "Sending…" and turns to "Sent · HH:mm". Shift+Enter makes a second line. No read ticks anywhere.
3. Stop the backend (or block the request in the browser), send a message: "Not sent · Retry" with the text intact. Restore the backend and choose Retry: "Sent", and the trainer receives it **once**.
4. Paste 2100 characters: the field keeps 2000.
5. Type `<b>x</b> https://example.com` and send: shown as typed, not bold, not a link.
6. Tab through the screen: visible focus on every control; a screen reader name on Send; new messages are announced without moving focus.
7. Repeat 1 and 2 in dark. Check the console is clean.
8. WCAG 2.2 AA: text and count contrast, every control labelled, focus visible, in light and dark (SC-008).
9. Sign in as Demo Client Waiting (60 or more messages): the newest 50 show; "Load earlier messages" adds the rest and the view stays on the message that was at the top.
10. The on-screen keyboard cannot be opened in the browser pane: the composer staying visible with it open (FR-018) is checked on a real phone, or stated as not verified in the pull request.

## 3. Trainer at 1280 px, light and dark

Sign in as Demo Trainer in the other session, open Messages.

1. The list shows every client; those with messages first with their counts; the rest "No messages yet". The sidebar shows the total. The conversation area asks to choose a conversation, and no count changed.
2. Search for part of a client's surname, with and without Polish letters: the list narrows; a name that matches nothing shows the no-match line; message text is not searched.
3. Open a conversation with unread messages: its count disappears and the total drops by it. Reload: the same conversation opens (its address).
4. Send from the client session: within 15 seconds the message appears in the open conversation and the list. Hide the trainer's tab (switch tab), send another: the count stays; switch back and it is read.
5. Enter sends, Shift+Enter adds a line; the note under the composer says so.
6. Narrow the window below about 800 px: only the list or only the conversation shows, with a way back. WCAG 2.2 AA as in section 2 step 8, at 1280 px.
7. Open `/trainer/messages/<id of a Demo Trainer Two client>` and `/trainer/messages/not-an-id`: both show the not-found state, nothing leaks.

## 4. Security checks on the backend

With a signed-in session and the CSRF header (the browser's network tab shows the cookie), call the API:

| Call | Expected |
|---|---|
| Demo Trainer Two: `GET`, `POST`, `PUT /conversations/<Demo Client id>/...` | 404 `conversation.not-found` on all three |
| Demo Client: the same paths with the other demo client's id | 404 |
| Signed out: any endpoint | 401 |
| `POST` without `X-XSRF-TOKEN` | 403 |
| `POST` with an empty or all-space text; with 2001 characters | 400 `message.empty`; 400 `message.too-long` |
| `POST` the same `id` twice with the same text | the same 201 body, one row in `messages` |
| `POST` the same `id` with other text | 409 `message.id-in-use` |
| Any response body | no email, no sender id, no stack trace, no SQL |
| `GET .../messages?before=<id of a message in another conversation>` | 404 `message.not-found` |

Then read the backend log for the whole session: no message text appears in it.

## 5. Languages

Switch the UI to Polish: every text of both screens, the failure messages, times and the day separators change; a Polish message with diacritics is stored and shown intact.

## Expected outcome

Sections 1 to 5 pass. Anything that does not is fixed in a further commit of the same pull request and the section is run again. Whatever could not be checked (for example a screen reader) is named in the pull request, not claimed.
