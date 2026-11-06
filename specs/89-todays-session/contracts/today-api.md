# Contract: today API

For a signed-in **client**. The client is taken from the sign-in; no id is sent. A trainer is refused with `403 request.not-allowed`; no sign-in gets `401 request.unauthenticated`. Errors are RFC 9457 problem details with a stable `code`. The generated OpenAPI document is the source; this file says what it must contain.

## GET /api/v1/today

What the client trains today in their time zone. Exercise names are in the language of `Accept-Language`.

`200` response (`TodayResponse`):

| Field | Type | Notes |
|---|---|---|
| `date` | date | required; today where the client is |
| `state` | `SESSION` \| `REST` \| `FINISHED` | required |
| `session` | object | only for `SESSION`: `{ name, day, entries }` as in the trainer's plan response (`PlanResponse.SessionResponse`) |

An entry is `{ exerciseId, exerciseName, measure, sets, amount, loadKg?, rpe, restSeconds }`; `loadKg` is left out when there is no load.

Errors:

| Status | `code` | When |
|---|---|---|
| 404 | `plan.not-found` | The client has no plan, or it is a draft. The two are indistinguishable. |

## Access

| Caller | Result |
|---|---|
| Client | own session |
| Trainer | 403 `request.not-allowed` |
| Not signed in | 401 `request.unauthenticated` |
