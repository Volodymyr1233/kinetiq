# Contract: check-in API (planned, task #110)

Planned shape, to be confirmed when #110 starts. Nothing here exists yet; #111 adds no endpoint. All endpoints are under `/api/v1`, need a signed-in client (role CLIENT) and answer in the problem-detail format with a stable `code`. They address only the signed-in client; there is no id of a person in the path.

| Method and path | Purpose | Success | Refusals |
|---|---|---|---|
| `PUT /check-ins/today` | Stores the client's check-in for today, or replaces today's while it is still changeable | `200` with the check-in | `400` `validation.failed` with `errors: [{field, code}]` for an answer outside 1 to 5 or missing; `409` `check-in.locked` once the first set of the day is logged; `401` without a session; `403` `request.not-allowed` for a trainer |
| `GET /check-ins/today` | Reads today's check-in | `200` with the check-in, or `204` when the client has none | `401`, `403` as above |

## Request of `PUT`

```json
{ "sleep": 4, "soreness": 2, "stress": 1 }
```

## Check-in response

```json
{ "sleep": 4, "soreness": 2, "stress": 1, "readiness": 83, "band": "READY", "locked": false }
```

- `readiness` is the 0 to 100 score; `band` is `READY`, `TAKE_IT_EASIER` or `RECOVERY_DAY`; `locked` is true once the first set of the day is logged.
- "Today" is the day in the client's time zone.
- No personal identifier is in the response or in any log line about the check-in.
