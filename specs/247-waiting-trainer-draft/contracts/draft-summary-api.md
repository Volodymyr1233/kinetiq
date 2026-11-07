# Contract: draft summary API

For a signed-in **client**. The client is taken from the sign-in; no id is sent. Errors are RFC 9457 problem details with a stable `code`. The OpenAPI document generated from code is the source; this file says what it must contain.

## GET /api/v1/plan/draft-summary

The summary of the client's own draft.

`200` response (`DraftSummaryResponse`):

| Field | Type | Notes |
|---|---|---|
| `trainingDays` | list of `DayOfWeek` | required, at least one, in week order, Monday first |
| `weeks` | integer | required, 4-16 |
| `goal` | `Goal` | required |

The answer holds nothing else of the draft: no session names, exercises, targets or loads.

Errors:

| Status | `code` | When |
|---|---|---|
| 404 | `plan.not-found` | A failed draft, a draft without sessions, no plan or a published plan. The four are indistinguishable. |

## Access

| Caller | Answer |
|---|---|
| Client | own draft |
| Trainer | 403 `request.not-allowed` |
| Not signed in | 401 `request.unauthenticated` |

## Unchanged

- `GET /api/v1/plan` still answers `404 plan.not-found` for a draft (spec 75, FR-003).
- `GET /api/v1/quiz` still says nothing about the draft's content (spec 79, FR-003).
- `GET /api/v1/conversations` gives the waiting client their trainer's name (spec 58); #248 changes nothing in it.
