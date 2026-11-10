# Contract: messaging API and addresses, changes for story #172

The API of spec `58-client-trainer-messaging` (`contracts/api.md` there) with the changes below. The OpenAPI document generated from code is the source; this file says what it must contain. All paths are under `/api/v1`, every endpoint needs a sign-in, and errors are problem details with a stable `code`.

## `POST /conversations/{clientId}/messages` (#173)

```json
{ "id": "uuid chosen by the sender", "text": "…", "aboutSetLogId": "uuid, optional" }
```

**201** the stored message, with `about` when it has a reference. Sending the same `id` with the same text and the same `aboutSetLogId` again returns the same body and status.

One more error, and one widened:

| Status | `code` | When |
|---|---|---|
| 404 | `message.about-not-found` | `aboutSetLogId` is not a set log of a session log of the signed-in person: another client's set, an unknown id, and every reference sent by a trainer. The three look the same. No message and no conversation is stored. |
| 409 | `message.id-in-use` | as before, and also when the `id` was sent before with another `aboutSetLogId`, or with none |

Order of the checks: the conversation (`404 conversation.not-found`), the text (`400 message.empty`, `message.too-long`), then the reference.

## `GET /conversations/{clientId}/messages` (#173)

Each message may carry `about`:

```json
{
  "id": "uuid",
  "text": "…",
  "mine": true,
  "sentAt": "2026-11-10T09:12:00Z",
  "about": {
    "setLogId": "uuid",
    "sessionLogId": "uuid",
    "sessionName": "Lower body",
    "sessionDate": "2026-11-09",
    "exerciseName": "Back squat",
    "entryPosition": 0,
    "setNumber": 3,
    "rpe": 9
  }
}
```

`about` is left out of a message without a reference. Every field of `about` is required. `exerciseName` is in the language of the request (`Accept-Language`), Polish when the exercise has a Polish name. `rpe` is the set's RPE now. `sessionDate` is the plain date of the session log.

Both people of the conversation get the same `about`. Nothing else changes: the page size, the order, `hasMore`, `GET /conversations` (its `lastMessage.preview` is text only) and the unread counts.

## Access

| Caller | Sending with `aboutSetLogId` | Reading `about` |
|---|---|---|
| The client of the conversation | own set logs only; others `404 message.about-not-found` | yes |
| The trainer of the conversation | always `404 message.about-not-found` | yes |
| Anyone else | `404 conversation.not-found` | `404 conversation.not-found` |
| Not signed in | `401 request.unauthenticated` | `401 request.unauthenticated` |

## Addresses (#175)

| Address | Shows |
|---|---|
| `/app/session/{sessionLogId}?set={entryPosition}-{setNumber}` | The client's logger on that session (spec 101), with that set's row marked, in view and focused |
| `/trainer/clients/{clientId}/logs?session={sessionLogId}&set={entryPosition}-{setNumber}` | The client's Logs tab with that session chosen and that set's row marked, in view and focused |

`session` also takes the date of a session that was never started, the key the list already uses. A `set` or `session` that names nothing is ignored: the logger shows the session unmarked, the Logs tab its newest session. Choosing another session in the Logs tab sets `session` and drops `set`.
