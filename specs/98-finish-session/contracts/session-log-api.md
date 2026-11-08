# Contract: session log API, changes for story #98

The API of specs `92-session-logging` and `101-log-edit-window` with the changes below. For a signed-in **client**, taken from the sign-in. Errors are RFC 9457 problem details with a stable `code`. The OpenAPI document generated from code is the source; this file says what it must contain.

## POST /api/v1/session-logs (changed, #99)

`SessionLogResponse` gains `finishedAt` (`Instant`, left out until the session is finished), so the logger knows to offer "View summary".

## POST /api/v1/session-logs/{id}/finish (#99)

Finishes the client's own session. No body. A repeat answers as the first time and changes nothing.

`200` answer: `SessionSummaryResponse`.

| Status | `code` | When |
|---|---|---|
| 404 | `session-log.not-found` | The session is not the client's, or does not exist |
| 409 | `session-log.locked` | Not finished, and 24 hours or more since `startedAt` |
| 409 | `session-log.nothing-logged` | Not finished, and no set is logged |
| 409 | `session-log.changed` | Another request changed the session at the same moment; repeat the request |

## GET /api/v1/session-logs/{id}/summary (#99)

The summary of the client's own finished session, as the figures are now.

`200` answer: `SessionSummaryResponse`.

| Status | `code` | When |
|---|---|---|
| 404 | `session-log.not-found` | Not the client's, or unknown |
| 409 | `session-log.not-finished` | The session is not finished |

### SessionSummaryResponse

| Field | Type | Notes |
|---|---|---|
| `sessionLogId` | uuid | required |
| `name` | string | the session's name, as copied at the start; required |
| `date` | date | required |
| `setsLogged` | int | required |
| `setsPlanned` | int | required |
| `volumeKg` | number | left out when no logged set counts (time-measured or without a load); one decimal |
| `averageRpe` | number | one decimal; required |
| `minutes` | int | at least 1; required |
| `note` | string | left out when none (#100) |

## GET /api/v1/session-states?week={n} (#99)

The state of each planned session of plan week `n` (the current week without `week`). Works out nothing for a finished plan: without `week` it answers `sessions: []`.

`200` answer: `SessionStatesResponse`:

| Field | Type | Notes |
|---|---|---|
| `today` | date | the client's local date; required |
| `sessions[]` | list | one per planned session of the week, in week order; required |
| `sessions[].day` | `DayOfWeek` | required |
| `sessions[].date` | date | the date of that day in the shown week; required |
| `sessions[].state` | `DONE` `IN_PROGRESS` `MISSED` `TODAY` `PLANNED` | left out when the session has no state (before the plan was published) |
| `sessions[].sessionLogId` | uuid | left out when no session log exists for that date |

| Status | `code` | When |
|---|---|---|
| 404 | `plan.not-found` | No plan, and a draft alike |
| 400 | `plan.week-out-of-range` | A week the plan does not have |

## PUT /api/v1/session-logs/{id}/note (#100)

Body `SaveNoteRequest`: `{ "text": string }`, at most 500 characters after trimming (`@Size` and the domain). Empty or blank text removes the note. `204` without a body.

| Status | `code` | When |
|---|---|---|
| 400 | `session-log.note-invalid` | Over 500 characters (also `request.invalid` for a body that cannot be read) |
| 404 | `session-log.not-found` | Not the client's, or unknown |
| 409 | `session-log.not-finished` | The session is not finished |
| 409 | `session-log.locked` | 24 hours or more since `startedAt` |
| 409 | `session-log.changed` | Another request changed the session at the same moment |

## Access

| Caller | Answer |
|---|---|
| Client | own data only |
| Trainer | 403 `request.not-allowed` |
| Not signed in | 401 `request.unauthenticated` |

`/api/v1/session-states/**` is added to the client-only matchers in `SecurityConfig`; a test proves the trainer and the visitor answers. All state-changing requests need the CSRF header, as before.

## Frontend texts for the new codes

`errors.session-log.nothing-logged`, `errors.session-log.not-finished`, `errors.session-log.changed`, `errors.session-log.note-invalid`, in English and Polish. `session-log.locked` exists (spec 101).
