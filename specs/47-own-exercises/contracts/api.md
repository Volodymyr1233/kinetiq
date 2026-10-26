# API contract: Trainer adds and edits their own exercise

All under `/api/v1`, JSON. Every endpoint requires a signed-in trainer: anonymous → `401 request.unauthenticated`, client → `403 request.not-allowed`. Errors are problem details with `code`. State-changing requests carry `X-XSRF-TOKEN`. `Accept-Language` decides the language of listed texts (own exercises read the same in both).

`ExerciseResponse` (existing, one field added):

```json
{
  "id": "6f1d3c1e-2a4b-4c55-9a77-0e8f5b9d2c10",
  "name": "Goblet squat",
  "category": "LEGS",
  "equipment": "Dumbbell",
  "measure": "REPS",
  "sets": 3,
  "amount": 10,
  "restSeconds": 90,
  "cues": "Elbows inside the knees.",
  "plansUsing": 0,
  "starter": false
}
```

## GET /exercises (changed)

Same query parameters and response as spec #13. The list is now the starter exercises plus the signed-in trainer's own, never another trainer's; `total` counts both. Every item carries `starter`.

## POST /exercises

Request:

```json
{
  "name": "Goblet squat",
  "category": "LEGS",
  "measure": "REPS",
  "equipment": "Dumbbell",
  "sets": 3,
  "amount": 10,
  "restSeconds": 90,
  "cues": ""
}
```

- `name`: `@NotBlank @Size(max = Exercise.NAME_MAX_LENGTH)`, trimmed by the service.
- `category`, `measure`: `@NotNull`.
- `equipment`: `@NotNull @Size(max = Exercise.EQUIPMENT_MAX_LENGTH)`; empty allowed.
- `cues`: `@NotNull @Size(max = Exercise.CUES_MAX_LENGTH)`; empty allowed.
- `sets`: `@Min(1) @Max(20)`; `amount`: `@Min(1) @Max(3600)`; `restSeconds`: `@Min(0) @Max(600)`.
- The owner is the session's account; the body has no owner field.

| Status | Body |
|---|---|
| 201 | `ExerciseResponse` with `starter: false`, `plansUsing: 0` |
| 400 | `validation.failed`, `errors: [{field, code}]` with `code` one of `not-blank`, `not-null`, `size`, `min`, `max` |
| 400 | also `request.invalid` for an unknown `category` or `measure`, or text where a number belongs (no `errors`) |
| 409 | `exercise.name-taken`: a starter exercise (English or Polish name) or one of the trainer's own has this name |
| 409 | `exercise.limit-reached`: the trainer already owns 200 exercises |

## PUT /exercises/{id}

Request: as `POST` without `measure` (an unknown property is ignored, the measure never changes).

| Status | Body |
|---|---|
| 200 | `ExerciseResponse` with the new values |
| 400 | as `POST` |
| 404 | `exercise.not-found`: unknown id, another trainer's exercise or a starter exercise; identical in every case |
| 409 | `exercise.name-taken`; saving the exercise's own unchanged name is not a clash |

## DELETE

Not offered. `DELETE /exercises/{id}` and `DELETE /exercises` answer `405 request.method-not-allowed` and remove nothing.

## Frontend error copy

| Code | EN | PL |
|---|---|---|
| `exercise.name-taken` | An exercise with this name is already in the library. | Ćwiczenie o tej nazwie jest już w bibliotece. |
| `exercise.not-found` | This exercise can't be edited. | Tego ćwiczenia nie można edytować. |
| `exercise.limit-reached` | You've reached the limit of 200 own exercises. | Osiągnięto limit 200 własnych ćwiczeń. |
