# Contract: API changes

No new endpoint and no security rule change. Common refusals are those of `specs/68-plan-editor/contracts/api.md` and `specs/54-quiz-review-submit`.

## `POST /api/v1/quiz` and `GET /api/v1/quiz`

`QuizStatusResponse.status` gains `DRAFTING_PLAN`.

- `POST` answers `DRAFTING_PLAN` after a first submit, without waiting for the model.
- `GET` answers by the table in [data-model.md](../data-model.md). Nothing about the draft's content is exposed.

## `GET /api/v1/clients` and `GET /api/v1/clients/{id}`

`planState` gains `PLAN_DRAFTING` and `DRAFT_FAILED`. `PLAN_TO_REVIEW` now also means an untouched generated draft.

## `GET /api/v1/clients/{id}/plan`

During drafting the answer is the same as for no plan: `404` `plan.not-found`. The frontend knows drafting from `planState` and does not ask.

`PlanResponse` gains:

| Field | Type | Meaning |
|---|---|---|
| `automaticDraft` | `BUILT` or `FAILED`, left out when none | The mark on this draft |

The same field comes back from `PUT` and `POST .../publish`, where the mark is always gone.

## `PUT /api/v1/clients/{id}/plan`

New refusal:

| Status | Code | When |
|---|---|---|
| `409` | `plan.drafting` | A first save while a `plan_draftings` row exists for this client |

Saving a plan marked `BUILT` or `FAILED` clears the mark. The version rules are unchanged.

## `GET /api/v1/plan` and `POST /api/v1/plan/acknowledgement`

The client's own plan (spec 75), unchanged. A draft of any kind, including a built or failed one, answers `404` `plan.not-found`, as no plan does. `ClientPlanResponse` does not gain `automaticDraft`, because a published plan never has one.

## Frontend addresses

No new route. `/app/quiz/waiting` serves `DRAFTING_PLAN` and `WAITING_FOR_PLAN`. `/trainer/clients/{id}/plan` shows the drafting state.

## Translated code

`errors.plan.drafting`, in English and Polish ([spec](../spec.md), Texts).
