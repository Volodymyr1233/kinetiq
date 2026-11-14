# Contract: Session summary API

Task #302 adds two fields to the answer of both endpoints of spec 98; nothing else changes. Loads are in kilograms with one decimal. Fields marked optional are left out when they have no value.

## `POST /api/v1/session-logs/{id}/finish` and `GET /api/v1/session-logs/{id}/summary`

Role CLIENT. Statuses as in spec 98: `200` with `SessionSummaryResponse`; `404 session-log.not-found` for another client's or an unknown session; `409 session-log.not-finished` (summary of an unfinished session), `409 session-log.locked`, `409 session-log.nothing-logged`, `409 session-log.changed` (finish); `401` / `403` for a visitor / a trainer.

Both read `Accept-Language` for the exercise names, as the logger's endpoints do.

## `SessionSummaryResponse`

Fields of spec 98 (`sessionLogId`, `name`, `date`, `startedAt`, `setsLogged`, `setsPlanned`, `volumeKg` optional, `averageRpe`, `minutes`, `note` optional), plus:

| Field | Type | Meaning |
|---|---|---|
| `prs` | array of `PrResponse`, required | the session's PRs, in the order of the session's exercises; empty when none |
| `weeksInARow` | integer, required | the client's week streak, the same number as `weeksInARow` of `GET /api/v1/progress` |

### `PrResponse`

| Field | Type | Meaning |
|---|---|---|
| `exerciseId` | UUID, required | the exercise |
| `name` | string, required | the exercise's name in the request language |
| `measure` | `REPS` or `SECONDS`, required | what `amount` counts |
| `loadKg` | number, optional | the top set's load; left out for a set without a load |
| `amount` | integer, required | the top set's repetitions or seconds |

## Example

```json
{
  "sessionLogId": "8f0c…",
  "name": "Lower body A",
  "date": "2026-11-11",
  "startedAt": "2026-11-11T16:02:11Z",
  "setsLogged": 15,
  "setsPlanned": 15,
  "volumeKg": 6840.0,
  "averageRpe": 7.8,
  "minutes": 52,
  "prs": [
    { "exerciseId": "3b1e…", "name": "Back squat", "measure": "REPS", "loadKg": 102.5, "amount": 5 },
    { "exerciseId": "9d4a…", "name": "Plank", "measure": "SECONDS", "amount": 75 }
  ],
  "weeksInARow": 4
}
```
