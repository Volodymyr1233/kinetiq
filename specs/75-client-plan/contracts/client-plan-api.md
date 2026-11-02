# Contract: client plan API

Both operations are for a signed-in **client**. The client is taken from the sign-in; no id is sent. A trainer is refused with `403 request.not-allowed`; no sign-in gets `401 request.unauthenticated`. Errors are RFC 9457 problem details with a stable `code`. The OpenAPI document generated from code is the source; this file says what it must contain.

## GET /api/v1/plan

The client's published plan for one plan week. Exercise names are in the language of `Accept-Language`.

Query:

| Name | Type | Notes |
|---|---|---|
| `week` | integer, optional | A plan week number; default is the current plan week in the client's time zone. |

`200` response (`ClientPlanResponse`):

| Field | Type | Notes |
|---|---|---|
| `goal` | `Goal` | required |
| `weeks` | integer | required, 4-16 |
| `currentWeek` | integer | required, at least 1; above `weeks` once the plan has finished |
| `finished` | boolean | required; `currentWeek > weeks` |
| `seen` | boolean | required; the client has opened the plan before |
| `week` | object | omitted when `finished`. `{ number: integer, startsOn: date }`, `startsOn` is the Monday of that plan week |
| `sessions` | list | required, in week order, Monday first. Each: `{ name, day, entries }` as in the trainer's plan response (`PlanResponse.SessionResponse`) |

An entry is `{ exerciseId, exerciseName, measure, sets, amount, loadKg?, rpe, restSeconds }` as in the trainer's response; `loadKg` is left out when there is no load.

Errors:

| Status | `code` | When |
|---|---|---|
| 404 | `plan.not-found` | The client has no plan, or the plan is a draft. The two are indistinguishable. |
| 400 | `plan.week-out-of-range` | `week` is below 1, above `weeks`, or the plan has finished. Nothing is returned. |
| 400 | `request.invalid` | `week` is not a number. |

## POST /api/v1/plan/acknowledgement

Marks the client's published plan as seen. No body. Safe to repeat: only the first call changes anything.

`204` on success, also when the plan was already seen.

Errors:

| Status | `code` | When |
|---|---|---|
| 404 | `plan.not-found` | No plan, or a draft. |

Does not change the plan's version, so the trainer's editor stays valid.

## Access

| Caller | `GET /api/v1/plan` | `POST /api/v1/plan/acknowledgement` |
|---|---|---|
| Client | own plan | own plan |
| Trainer | 403 `request.not-allowed` | 403 `request.not-allowed` |
| Not signed in | 401 `request.unauthenticated` | 401 `request.unauthenticated` |

CSRF applies to the `POST` as to every state-changing request.
