# API contract: exercise library

Base path `/api/v1`. Authentication: session cookie (existing). This is the only operation of the story.

## `GET /api/v1/exercises`

Lists the exercises in the library (starter exercises for now), filtered and ordered, in the language of the request.

### Request

| Part | Name | Rule |
|---|---|---|
| Header | `Accept-Language` | `en` or `pl`; anything else is treated as `en`. The SPA sends the UI language |
| Query | `category` | optional; one of `CHEST`, `BACK`, `LEGS`, `SHOULDERS`, `ARMS`, `CORE`, `FULL_BODY`, `CARDIO`, `MOBILITY` |
| Query | `name` | optional; at most 100 characters; trimmed; blank = no filter. Matches any part of the English or the Polish name, ignoring letter case |

No body, no CSRF token (safe method).

### Responses

**200** `application/json`

```json
{
  "total": 40,
  "exercises": [
    {
      "id": "00000000-0000-4000-9100-000000000001",
      "name": "Back squat",
      "category": "LEGS",
      "equipment": "Barbell",
      "measure": "REPS",
      "sets": 3,
      "amount": 5,
      "restSeconds": 120,
      "cues": "Bar on upper back. Hips and knees break together. Knees follow toes.",
      "plansUsing": 0
    }
  ]
}
```

- `total`: all exercises in the library, whatever the filters.
- `exercises`: matches ordered by `name` in the response language (case ignored, that language's alphabet), then by `id`.
- `name`, `equipment`, `cues`: the response language, falling back to English per field.
- `sets`, `amount`: the default; `amount` counts repetitions for `REPS` and seconds for `SECONDS`.
- `plansUsing`: plans using the exercise; 0 until plans exist.

**400** `application/problem+json`, `code: "validation.failed"`, `errors: [{ "field": "category", "code": "type-mismatch" }]` for an unknown category, `{ "field": "name", "code": "size" }` for a name over 100 characters.

**401** `code: "request.unauthenticated"` without a session. No exercise data.

**500** `code: "internal.error"` with a correlation id; no internals.

### Rules

- Nothing owned by a trainer is returned in this story: the use case reads only starter exercises (R3).
- The operation is idempotent and has no side effects.
