# Contract: session log API

For a signed-in **client**; the client is taken from the sign-in. A trainer is refused with `403 request.not-allowed`; no sign-in gets `401 request.unauthenticated`. Errors are RFC 9457 problem details with a stable `code`. The generated OpenAPI document is the source; this file says what it must contain. Exercise names are in the language of `Accept-Language`.

## POST /api/v1/session-logs

Starts today's session in the client's time zone, or returns it when it was started already. No body.

`201` when created, `200` when it existed. Response (`SessionLogResponse`):

| Field | Type | Notes |
|---|---|---|
| `id` | uuid | required |
| `date` | date | required |
| `name` | string | required; the planned session's name |
| `startedAt` | timestamp | required |
| `entries` | list | required, in plan order |

An entry: `{ position, exerciseId, exerciseName, measure, restSeconds, sets }`, where `sets` is a list of `{ number, target, log? }`. `target` is `{ loadKg?, amount, rpe }`; `log` is present once the set is logged: `{ id, loadKg?, amount, rpe, loggedAt }`. `loadKg` is left out when empty.

Errors:

| Status | `code` | When |
|---|---|---|
| 404 | `plan.not-found` | No plan, or a draft |
| 409 | `session-log.no-session-today` | A rest day, or the plan has finished |

## PUT /api/v1/session-logs/{id}/set-logs/{setLogId}

Logs a set, or changes it. `{setLogId}` is a uuid made by the client. The first call creates, a repeat with the same values changes nothing, a call with new values replaces them.

Body (`LogSetRequest`): `{ entryPosition, setNumber, loadKg?, amount, rpe }`, all required except `loadKg`.

`200` response: the set as in the entry's `log` plus its place (`entryPosition`, `setNumber`).

Errors:

| Status | `code` | When |
|---|---|---|
| 404 | `session-log.not-found` | The session is not the client's, does not exist, or `{setLogId}` belongs to another session |
| 400 | `set-values.invalid` | `amount` outside 1–3600, `rpe` outside 5–10 or not whole, `loadKg` not above 0 or above 999.9 or with more than one decimal, or a place that is not a planned set. The problem carries the `field` |
| 400 | `request.invalid` | The body cannot be read |
| 409 | `set-log.id-in-use` | `{setLogId}` already logs another place of this session |
| 409 | `set-log.place-taken` | The place is already logged under another id |

## Access

| Caller | Result |
|---|---|
| Client | own sessions and sets |
| Trainer | 403 `request.not-allowed` |
| Not signed in | 401 `request.unauthenticated` |

CSRF applies to both operations.
