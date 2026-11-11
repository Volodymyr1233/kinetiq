# Contract: HTTP API

All endpoints require a signed-in account; none is public. State-changing calls (`POST`, `DELETE`) carry the CSRF header. The role rule of `SecurityConfig` applies first (wrong role: `403 request.not-allowed`), then ownership (not the caller's: `404 plan-change.not-found`). No `SecurityConfig` change is needed: `/api/v1/plan-changes/**`, `/api/v1/approvals/**` and `/api/v1/clients/**` are trainer-only, `/api/v1/plan/**` is client-only. Types are generated into `frontend/src/lib/api/schema.d.ts` in the same pull request.

## Shared shapes

**Decision fields**, added to `PlanChangeResponse` (trainer, existing in #131) and to `ClientPlanChangeDetailResponse` (client): `acceptedByTrainer: boolean`, `acceptedByClient: boolean`, `declinedBy: TRAINER|CLIENT|null`, `declineReason: TOO_EASY|TOO_HARD|SCHEDULE|INJURY|OTHER|null`, `declineNote: string|null`, `endedAt: instant|null`.

**KeepCurrentPlanRequest**: `{ reason: DeclineReason (required), note: string|null (≤ 500) }`. Violations give `400 request.invalid` with `errors: [{ field, code }]` (this is the spec's "reason required").

## Trainer

| Method and path | Does | 200 body |
|---|---|---|
| `GET /api/v1/plan-changes/{id}` (exists) | reads the detail; gains the decision fields | `PlanChangeResponse` |
| `POST /api/v1/plan-changes/{id}/acceptance` | the trainer accepts | `PlanChangeResponse` |
| `DELETE /api/v1/plan-changes/{id}/acceptance` | the trainer undoes their acceptance | `PlanChangeResponse` |
| `POST /api/v1/plan-changes/{id}/declination` | keeps the current plan; posts the message | `PlanChangeResponse` |
| `GET /api/v1/approvals/plan-changes` | the trainer's open plan changes and those ended in the last 30 days, newest first | `{ items: PlanChangeApprovalResponse[] }` |
| `GET /api/v1/clients/{id}/plan-changes/ended` | the last 5 ended plan changes of the client | `{ items: PlanChangeApprovalResponse[] }` |
| `GET /api/v1/clients/{id}/plan-change` (exists) | the open one | `ClientPlanChangeResponse` |

`PlanChangeApprovalResponse`: `{ id, clientId, clientName, title, reason, state, acceptedByTrainer, acceptedByClient, declinedBy, declineReason, effectiveWeek, createdAt, endedAt }`. `title` and `reason` are in the trainer's language.

## Client

| Method and path | Does | 200 body |
|---|---|---|
| `GET /api/v1/plan/changes/current` | the client's open plan change | `ClientPlanChangeDetailResponse` |
| `GET /api/v1/plan/changes/{id}` | one of the client's own plan changes, ended ones too | `ClientPlanChangeDetailResponse` |
| `POST /api/v1/plan/changes/{id}/acceptance` | the client accepts | `ClientPlanChangeDetailResponse` |
| `DELETE /api/v1/plan/changes/{id}/acceptance` | the client undoes | `ClientPlanChangeDetailResponse` |
| `POST /api/v1/plan/changes/{id}/declination` | keeps the current plan; posts the message | `ClientPlanChangeDetailResponse` |

`ClientPlanChangeDetailResponse` = the fields of `PlanChangeResponse` (rule, state, title, reason, effectiveWeek, windowStart/End, targets, removedSession, moves, concernedExercises, figures) with `counterpartName` (the trainer's name) in place of `clientName`; title and reason are in the client's language.

## Errors (problem details, stable `code`)

| Status | code | When |
|---|---|---|
| 401 | `request.unauthenticated` | not signed in |
| 403 | `request.not-allowed` | wrong role for the area |
| 404 | `plan-change.not-found` | unknown id, or not the caller's; `client.not-found` for a client that is not the trainer's; `plan-change.not-found` also for "no open one" |
| 409 | `plan-change.ended` | the plan change is applied, declined or expired |
| 409 | `plan-change.nothing-to-undo` | undo by a person who has not accepted |
| 409 | `plan-change.week-begun` | the effective Monday has begun |
| 400 | `request.invalid` | missing reason or note over 500 characters (`errors` names the field); an unknown reason is rejected before validation, without `errors` |

Idempotence: `POST …/acceptance` by someone who already accepted returns `200` with the unchanged state; `DELETE` by someone who has not accepted is `409 plan-change.nothing-to-undo`; a repeated `…/declination` after it ended is `409 plan-change.ended` and posts nothing.

## Side effect

`…/declination` posts one message into the conversation of the client and the trainer, from the decliner, in the decliner's language, in the same transaction: title, reason and note as plain text. Visible through the existing messaging endpoints.

## Frontend translation

The frontend shows `t('errors.<code>')` for every code above; it never shows backend text except the server-built `title` and `reason` of a plan change, which the story requires in the reader's language.
