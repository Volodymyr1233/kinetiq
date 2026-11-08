# Contract: client training API for the trainer

For a signed-in **trainer**, taken from the sign-in, about one of their own clients. Errors are RFC 9457 problem details with a stable `code`. The OpenAPI document generated from code is the source; this file says what it must contain. Dates are the client's local dates (`yyyy-mm-dd`); instants are ISO 8601 in UTC.

## Client session

The item every list below holds.

| Field | Type | Notes |
|---|---|---|
| `date` | date | |
| `day` | weekday | `MONDAY` … `SUNDAY` |
| `name` | string | |
| `state` | `DONE`, `IN_PROGRESS`, `MISSED`, `TODAY`, `PLANNED` | left out before the plan's publish day |
| `locked` | boolean | |
| `setsLogged` | integer | |
| `setsPlanned` | integer | |
| `averageRpe` | decimal, one place | left out while no set is logged |
| `sessionLogId` | uuid | left out for a session never started |

## GET /api/v1/clients/{id}/overview (#105)

`200` answer:

| Field | Type | Notes |
|---|---|---|
| `today` | date | the client's local date |
| `week` | object | left out when the plan is not published or has finished |
| `week.number` | integer | the plan week |
| `week.startsOn` | date | its Monday |
| `week.sessions` | client session[] | the week's planned sessions, in week order |
| `recentSessions` | client session[] | at most 4, newest first (research R5) |
| `quizAnswers` | object | `goal`, `experience`, `level`, `age`, `heightCm`, `weightKg`, `likes`, `daysPerWeek`, `notes` (left out when empty), `submittedAt`, as `GET /api/v1/clients/{id}/quiz-answers`; left out before the quiz |

## GET /api/v1/clients/{id}/session-logs (#107)

`200` answer: `{ "sessions": client session[] }`, every listed session newest first (research R5).

## GET /api/v1/clients/{id}/session-logs/{sessionLogId} (#107, #261)

`200` answer: the client session's fields, and:

| Field | Type | Notes |
|---|---|---|
| `startedAt` | instant | |
| `finishedAt` | instant | left out until finished |
| `minutes` | integer | left out until finished; as spec 98 counts them |
| `locksAt` | instant | `startedAt` + 24 hours |
| `entries` | object[] | in order: `exerciseName`, `measure` (`REPS`, `SECONDS`), `sets`, `amount`, `loadKg` (left out without a load), `rpe` |
| `setLogs` | object[] | `entryPosition`, `setNumber`, `loadKg` (left out without a load), `amount`, `rpe` |
| `note` | string | #261; left out when there is none |

The exact shape of #107 is fixed when #107 is planned in detail; this table records what the spec needs.

## Errors

| Status | `code` | When |
|---|---|---|
| 404 | `client.not-found` | The client is another trainer's or does not exist; the two look the same |
| 404 | `session-log.not-found` | The session log is not this client's or does not exist (#107) |

## Access

| Caller | Answer |
|---|---|
| Trainer | own clients only |
| Client | 403 `request.not-allowed` |
| Not signed in | 401 `request.unauthenticated` |
