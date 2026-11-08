# Contract: today API (change)

`GET /api/v1/today` as in spec `89-todays-session`, with one optional field added to `TodayResponse`.

| Field | Type | Notes |
|---|---|---|
| `next` | object | only for `state` = `REST` and only when the plan has a session after today: `{ date, session }` |
| `next.date` | date | required inside `next`; the date the session falls on, in the client's time zone |
| `next.session` | object | required inside `next`; `{ name, day, entries }` as in `PlanResponse.SessionResponse` |

Absent for `SESSION` and `FINISHED`. Errors and access are unchanged: `404 plan.not-found` for no plan and for a draft; a trainer gets `403 request.not-allowed`; no sign-in gets `401 request.unauthenticated`.
