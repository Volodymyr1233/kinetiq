# Contract: quiz API and addresses

The API of task #55 is a plan, refined when the task starts; its generated types come into `schema.d.ts` in that task's pull request. The addresses are final for task #56.

## `GET /api/v1/quiz`

The signed-in client's quiz status.

- `200`: `{ "status": "NOT_SUBMITTED" | "WAITING_FOR_PLAN" | "PLAN_PUBLISHED" }`
- `401` `request.unauthenticated`: no session
- `403` `request.not-allowed`: an account that is not a client

## `POST /api/v1/quiz`

Submits the signed-in client's answers once.

Request:

```json
{
  "goal": "BUILD_MUSCLE",
  "experience": "YEARS_1_TO_2",
  "level": "INTERMEDIATE",
  "age": 29,
  "heightCm": 168,
  "weightKg": 62.5,
  "likes": ["WEIGHTS", "YOGA_PILATES"],
  "daysPerWeek": 3,
  "notes": "Left knee gets stiff on deep squats"
}
```

`notes` may be absent or `null`; it is trimmed, and empty text is stored as none. `likes` is a set: a code sent twice counts once.

- `201`: `{ "status": "WAITING_FOR_PLAN" }`
- `400` `validation.failed` with `errors: [{ field, code }]`: a rule of spec 50 is broken; nothing is stored
- `400` `request.invalid`: a body that cannot be read, such as an unknown code or a number sent as text, as in the exercise API; nothing is stored
- `409` `quiz.already-submitted`: the client has submitted before, also when two submits race (research R8, R9)
- `401` `request.unauthenticated`, `403` `request.not-allowed`: as above

## Addresses (frontend)

| Address | Shows | Task |
|---|---|---|
| `/app/quiz/review` | The review, when the draft is complete; otherwise the step that may open (research R2) | #56 |
| `/app/quiz/<n>` with `state.fromReview` | Step `n`; Continue and Back return to the review (research R3) | #56 |
| `/app/quiz/waiting` | The waiting states (spec User Story 3) | #57 |

Spec 50's contract said `/app/quiz/<anything else>` falls back "until task #56 adds `/app/quiz/review`"; task #56 updates that line and spec 50's research R12.
