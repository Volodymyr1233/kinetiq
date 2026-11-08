# Contract: check-in API

For a signed-in **client**; the client and the day are taken from the sign-in and the client's time zone, never from the request. A trainer is refused with `403 request.not-allowed`; no sign-in gets `401 request.unauthenticated`. Errors are RFC 9457 problem details with a stable `code`. The generated OpenAPI document is the source; this file says what it must contain. CSRF applies to `PUT`.

## Response: `TodayCheckInResponse`

Both operations answer it.

| Field | Type | Notes |
|---|---|---|
| `date` | date | required; the client's today |
| `open` | boolean | required; a check-in can still be saved today (a session day and no logged set) |
| `checkIn` | object | present when the client has checked in today |

`checkIn`:

| Field | Type | Notes |
|---|---|---|
| `sleep` | integer 1–5 | required |
| `soreness` | integer 1–5 | required |
| `stress` | integer 1–5 | required |
| `score` | integer 0–100 | required |
| `band` | enum `READY`, `TAKE_IT_EASIER`, `RECOVERY_DAY` | required |
| `savedAt` | timestamp | required |

## GET /api/v1/check-ins/today

Today's check-in, if any, and whether one can still be saved.

`200` always for a client with a published plan; a rest day or a finished plan reads `open: false`.

| Status | `code` | When |
|---|---|---|
| 404 | `plan.not-found` | No plan, or a draft |

## PUT /api/v1/check-ins/today

Saves today's check-in: creates it, or replaces the answers of the one saved today. A repeat with the same answers gives the same result.

Body (`SaveCheckInRequest`): `{ sleep, soreness, stress }`, each a required whole number from 1 to 5.

`200`: `TodayCheckInResponse` with `checkIn` set and `open: true`.

| Status | `code` | When |
|---|---|---|
| 400 | `validation.failed` | An answer is missing or outside 1–5; `errors: [{ field, code }]` with `field` one of `sleep`, `soreness`, `stress` and `code` `not-null`, `min` or `max` |
| 400 | `request.invalid` | The body cannot be read (e.g. `2.5` or text for an answer) |
| 404 | `plan.not-found` | No plan, or a draft |
| 409 | `check-in.no-session-today` | Today is a rest day, or the plan has finished |
| 409 | `check-in.training-started` | A set of today's session is logged; nothing changes |

## Access

| Caller | Result |
|---|---|
| Client | their own check-in for their own today |
| Trainer | 403 `request.not-allowed` |
| Not signed in | 401 `request.unauthenticated` |

`/api/v1/check-ins/**` joins the role CLIENT rule in `SecurityConfig`.
