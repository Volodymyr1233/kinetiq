# API contract: Trainer reviews and publishes a drafted plan

All paths under `/api/v1`, same origin, session cookie, CSRF header on `POST`. Errors are problem details with a `code`. The OpenAPI document generated from the controllers is the source of truth; this file states the intent. `schema.d.ts` is regenerated in the commit of each task that changes it.

## `GET /approvals/drafted-plans` (task #86)

Role `TRAINER`. The signed-in trainer's clients whose plan is a draft, oldest first (`createdAt`, then plan id). Includes a plan being rebuilt; excludes a client whose first plan is still being drafted.

`200`:

```json
{
  "items": [
    {
      "clientId": "uuid",
      "clientName": "string",
      "kind": "BUILT | FAILED | SAVED",
      "createdAt": "2026-11-07T09:30:00Z",
      "weeks": 12,
      "sessionsPerWeek": 3
    }
  ]
}
```

Errors: `401` unauthenticated, `403` for a client's sign-in.

## `GET /clients/{id}/quiz-answers` (task #86)

Role `TRAINER`, own clients only.

`200`: `goal`, `experience`, `level`, `age`, `heightCm`, `weightKg`, `likes` (list), `daysPerWeek`, `notes` (omitted when none), `submittedAt`, using the enums of the quiz.

Errors: `404 client.not-found` (unknown and another trainer's client alike), `404 quiz.not-found` (own client, no answers).

## `POST /clients/{id}/plan/drafting` (task #88)

Role `TRAINER`, own clients only. No body. Starts a new automatic draft of the client's draft plan.

`202` with no body: drafting started. The client's `planState` becomes `PLAN_DRAFTING`; when it ends, `GET /clients/{id}/plan` answers with the new draft (higher `version`) or with the same one.

| Status | Code | When |
|---|---|---|
| 404 | `client.not-found` | unknown or another trainer's client |
| 404 | `plan.not-found` | own client with no plan (a first drafting is the quiz's) |
| 409 | `plan.already-published` | the plan is published |
| 409 | `plan.drafting` | a drafting is already running for this client |
| 429 | `plan.rebuild-limit-reached` | 10 accepted builds in the last 24 hours; carries `availableAt` (ISO instant) |

## Changed behaviour of existing operations (task #88)

| Operation | Change |
|---|---|
| `PUT /clients/{id}/plan` | Also refused with `409 plan.drafting` for an existing plan while a drafting runs (was only a first save). |
| `POST /clients/{id}/plan/publication` | Refused with `409 plan.drafting` while a drafting runs. |
| `GET /clients/{id}/plan` | Unchanged. |

## Security notes

- `/approvals/**` is added to the trainer-only rules in `SecurityConfig`; the default stays deny.
- Ownership is in the SQL (list) and in the use case (the rest). Foreign and unknown clients are indistinguishable.
- No response carries text from the drafting service or an exception message.
