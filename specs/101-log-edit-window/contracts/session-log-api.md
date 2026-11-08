# Contract: session log API, changes for story #101

The API of spec `92-session-logging` (`contracts/session-log-api.md` there) with the changes below. For a signed-in **client**, taken from the sign-in. Errors are RFC 9457 problem details with a stable `code`. The OpenAPI document generated from code is the source; this file says what it must contain.

## PUT /api/v1/session-logs/{id}/set-logs/{setLogId} (#102)

Body and `200` answer unchanged. One more error:

| Status | `code` | When |
|---|---|---|
| 409 | `session-log.locked` | The request arrives at or after 24 hours from the session's `startedAt`, and it would log a new set or change a logged one. Nothing changes. |

A repeat of a logged set with the same place and values still answers `200` as the first time, locked or not. A session that is not the client's still answers `404 session-log.not-found`, locked or not.

## POST /api/v1/session-logs (#102)

Unchanged. `startedAt` is what the logger counts the 24 hours from.

## GET /api/v1/session-logs/{id} (#256)

The signed-in client's own session log, without starting anything.

`200` answer: `SessionLogResponse`, as from `POST /api/v1/session-logs`.

Errors:

| Status | `code` | When |
|---|---|---|
| 404 | `session-log.not-found` | The session log is another client's or does not exist; the two look the same |

## GET /api/v1/session-states (#256)

As spec `98-finish-session` describes it, with one more field on each session:

| Field | Type | Notes |
|---|---|---|
| `startedAt` | instant | when its session log started; left out when the session was never started, like `sessionLogId` |

## Access

| Caller | Answer |
|---|---|
| Client | own session logs |
| Trainer | 403 `request.not-allowed` |
| Not signed in | 401 `request.unauthenticated` |
