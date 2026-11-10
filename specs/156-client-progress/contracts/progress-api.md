# Contract: Progress API

Both endpoints answer the same `ProgressResponse`. Loads are in kilograms with one decimal; dates are the client's local dates (`YYYY-MM-DD`). Fields marked optional are left out when they have no value.

## `GET /api/v1/progress?weeks={4|8|12}` (#158)

The signed-in client's own progress. Role CLIENT.

| Status | When |
|---|---|
| `200` | `ProgressResponse` |
| `400 progress.period-invalid` | `weeks` is not 4, 8 or 12 |
| `404 plan.not-found` | no plan, or only a draft |
| `401` / `403` | not signed in / signed in as a trainer |

## `GET /api/v1/clients/{id}/progress?weeks={4|8|12}` (#158)

The same for one of the signed-in trainer's clients. Role TRAINER.

| Status | When |
|---|---|
| `200` | `ProgressResponse`, equal to the client's own for the same period |
| `400 progress.period-invalid` | `weeks` is not 4, 8 or 12 |
| `404 client.not-found` | the client is another trainer's, or unknown; checked before anything else |
| `404 plan.not-found` | the client has no published plan |
| `401` / `403` | not signed in / signed in as a client |

## `ProgressResponse`

```text
goal                 BUILD_MUSCLE | GET_STRONGER | LOSE_FAT | MOVE_BETTER | GENERAL_FITNESS
week                 int        current plan week; the last week once the plan has finished
finished             boolean
sessionsDone         int        over the period
sessionsPlanned      int        over the period
sessionsDoneChange   int?       last completed week minus the week before
averageRpe           decimal?   over every set of the period, one decimal
averageRpeChange     decimal?   one decimal
weeksInARow          int
weeks[]                         the period's plan weeks, oldest first
  number             int
  startsOn           date       its Monday
  planned            int
  done               int
  running            boolean    the current week of a plan that has not finished
  averageRpe         decimal?
exercises[]                     logged in the period; the one in the most sessions first, then the most recently logged
  exerciseId         uuid
  name               string     in the request's language
  measure            REPS | SECONDS
  best               Set        its top set in the period
  topSetChange       Change?    last completed week against the week before
  prToday            boolean
  weeks[]                       one per period week in which it was logged
    number           int
    top              Set
    averageRpe       decimal
  recent[]                      its 3 newest sessions over all logs, newest first
    date             date
    top              Set

Set     { loadKg: decimal?, amount: int, rpe: int }
Change  { loadKg: decimal? | amount: int? }   exactly one is present
```

Example (shortened), `weeks=4`, Wednesday of plan week 4, 3 sessions a week:

```json
{
  "goal": "BUILD_MUSCLE", "week": 4, "finished": false,
  "sessionsDone": 9, "sessionsPlanned": 12, "sessionsDoneChange": -1,
  "averageRpe": 7.8, "averageRpeChange": 0.3, "weeksInARow": 4,
  "weeks": [
    { "number": 1, "startsOn": "2026-10-19", "planned": 3, "done": 3, "running": false, "averageRpe": 7.5 },
    { "number": 4, "startsOn": "2026-11-09", "planned": 3, "done": 1, "running": true, "averageRpe": 8.0 }
  ],
  "exercises": [
    {
      "exerciseId": "…", "name": "Back squat", "measure": "REPS",
      "best": { "loadKg": 105.0, "amount": 5, "rpe": 8 },
      "topSetChange": { "loadKg": 2.5 }, "prToday": true,
      "weeks": [ { "number": 1, "top": { "loadKg": 100.0, "amount": 5, "rpe": 8 }, "averageRpe": 7.7 } ],
      "recent": [ { "date": "2026-11-11", "top": { "loadKg": 105.0, "amount": 5, "rpe": 8 } } ]
    }
  ]
}
```
