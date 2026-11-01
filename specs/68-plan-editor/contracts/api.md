# Contract: plan API and addresses

Every operation needs a signed-in trainer: `/api/v1/clients/**` already requires the role `TRAINER`. No security rule changes.

Common refusals:

- `401` `request.unauthenticated`: no session
- `403` `request.not-allowed`: an account that is not a trainer
- `403` `request.forbidden`: a state-changing request without the CSRF token
- `404` `client.not-found`: a foreign or unknown client, alike
- `400` `request.invalid`: a body that cannot be read, or a path id that is no UUID

## `GET /api/v1/clients/{id}/plan`

The client's plan, with exercise names in the language of `Accept-Language`.

`200`:

```json
{
  "status": "PUBLISHED",
  "goal": "BUILD_MUSCLE",
  "weeks": 16,
  "firstWeek": "2026-10-26",
  "currentWeek": 2,
  "version": 4,
  "sessions": [
    {
      "name": "Lower body",
      "day": "WEDNESDAY",
      "entries": [
        {
          "exerciseId": "…",
          "exerciseName": "Back squat",
          "measure": "REPS",
          "sets": 3,
          "amount": 5,
          "loadKg": 100.0,
          "rpe": 8,
          "restSeconds": 120
        }
      ]
    }
  ]
}
```

`firstWeek`, `currentWeek` and `loadKg` are left out when empty. Sessions are in week order, Monday first. `currentWeek` is at least 1 and may exceed `weeks`, which means the plan has finished.

- `404` `plan.not-found`: an own client with no plan

## `PUT /api/v1/clients/{id}/plan`

Saves the plan as a whole; the first save creates a draft. A published plan stays published, with the same first week.

```json
{
  "version": 4,
  "goal": "BUILD_MUSCLE",
  "weeks": 16,
  "sessions": [
    {
      "name": "Lower body",
      "day": "WEDNESDAY",
      "entries": [
        { "exerciseId": "…", "sets": 3, "amount": 5, "loadKg": 100.0, "rpe": 8, "restSeconds": 120 }
      ]
    }
  ]
}
```

`version` is left out on the first save. Field limits are in [data-model.md](../data-model.md).

- `200`: as `GET`, with the new version
- `400` `validation.failed` with `errors: [{ field, code }]`, for example `{ "field": "sessions[0].entries[1].rpe", "code": "max" }`
- `400` `plan.exercise-not-in-library`: also for an exercise that does not exist
- `400` `plan.duplicate-day`
- `400` `plan.too-few-sessions`, `plan.session-without-exercises`: only when the plan is published
- `400` `plan.weeks-before-current`, with `currentWeek`: a published plan shortened below its current week; an unchanged or longer length passes
- `409` `plan.changed`: the version is outdated, or a plan appeared since this editor opened
- `409` `plan.quiz-not-submitted`: first save for a client without a submitted quiz

## `POST /api/v1/clients/{id}/plan/publication`

Publishes the saved draft. Week 1 becomes the current week in the client's time zone.

```json
{ "version": 4 }
```

- `200`: as `GET`
- `404` `plan.not-found`
- `400` `plan.too-few-sessions`, `plan.session-without-exercises`
- `409` `plan.already-published`
- `409` `plan.changed`

## Changed responses

- `ClientResponse.planState` gains `DRAFT` and `PUBLISHED`; `goal` is the plan's when a plan exists.
- `ExerciseResponse.plansUsing` becomes the real count of the trainer's own plans.
- `GET /api/v1/quiz` can now answer `PLAN_PUBLISHED`, as its contract already says.

## Addresses (frontend)

| Address | Shows |
|---|---|
| `/trainer/clients/<id>/plan` | The Plan tab: not answered the quiz, no plan with "Build plan", or the editor |
