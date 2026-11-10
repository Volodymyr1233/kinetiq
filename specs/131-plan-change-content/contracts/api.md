# Contract: HTTP API

Both endpoints are authenticated, trainer-only, read-only (`GET`, so no CSRF token) and live under `/api/v1`. Neither is public. Types are generated into `frontend/src/lib/api/schema.d.ts` in the same pull request.

## GET /api/v1/plan-changes/{id}

The plan change of one of the caller's own clients.

**200** `PlanChangeResponse`

| Field | Type | Notes |
|---|---|---|
| id | uuid | |
| clientId | uuid | |
| clientName | string | the trainer's own client; shown in the card header |
| rule | enum | `LOW_COMPLETION`, `HIGH_RPE`, `STALLED_WEIGHT`; the frontend translates the note and chart labels by it |
| state | enum | `PENDING`, `ACCEPTED_BY_ONE`, `APPLIED`, `DECLINED`, `EXPIRED` |
| title | string | built for the caller's language |
| reason | string | built for the caller's language |
| effectiveWeek | date | a Monday |
| windowStart / windowEnd | date | |
| targets | array | `{ sessionDay, sessionName, position, exerciseId, exerciseName, measure, target, oldValue, newValue }` (`measure` is `REPS` or `SECONDS`, so an amount can be labelled), ordered by weekday, position, target |
| removedSession | object / null | `{ day, name }` |
| moves | array | `{ exerciseId, exerciseName, fromDay, toDay }` |
| concernedExercises | array | `{ id, name }` |
| figures | array | `{ kind, exerciseId?, weekStart, value }` for the chart |

`exerciseName` is the library name (starter exercise or the trainer's own).

**Errors** (problem details, `application/problem+json`, stable `code`):

| Status | code | When |
|---|---|---|
| 401 | `request.unauthenticated` | no sign-in (the code the existing entry point gives) |
| 403 | `request.not-allowed` | the caller is a client: `SecurityConfig` restricts both paths to the trainer role |
| 404 | `plan-change.not-found` | no such plan change, or it belongs to another trainer's client |

## GET /api/v1/clients/{id}/plan-change

The open plan change (state `PENDING` or `ACCEPTED_BY_ONE`) of the caller's own client `{id}`.

**200** `ClientPlanChangeResponse`: `{ id, rule, state, title, reason, effectiveWeek }`.

**Errors**: `404 client.not-found` for a client that is not the caller's; `404 plan-change.not-found` when the client has no open plan change.

## Notes

- No endpoint creates, edits or decides a plan change: creation is the weekly check (internal), deciding is story #137.
- Response never carries exception text, prompt text or LLM answers.
- Existing error codes are reused as the project already names them; confirm the 401 code in `SecurityConfig` at implementation time.
- `/api/v1/plan-changes/**` is not under `/api/v1/clients/**`, so it MUST be added to the trainer `requestMatchers` in `SecurityConfig`; otherwise it falls under the generic `/api/**` rule (any signed-in role).
