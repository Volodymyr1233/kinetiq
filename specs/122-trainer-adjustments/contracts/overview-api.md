# Contract: readiness in the client overview and the trainer's session log

No new endpoint. Both stay under `/api/v1/clients/**` (role `TRAINER`), and both check in the use case that the client is the signed-in trainer's own. The OpenAPI document is generated from code; `frontend/src/lib/api/schema.d.ts` is regenerated in the same pull request.

## `GET /api/v1/clients/{id}/overview` (changed, task #123)

New required fields on `ClientOverviewResponse`:

```json
{
  "today": "2026-10-07",
  "readiness": [
    { "date": "2026-09-24", "score": 75, "band": "READY" },
    { "date": "2026-09-25" },
    "… 14 items, oldest first, the last one is today …",
    { "date": "2026-10-07", "score": 0, "band": "RECOVERY_DAY" }
  ],
  "recoveryDays": [
    {
      "date": "2026-10-07",
      "score": 0,
      "causes": ["SLEEP", "SORENESS", "STRESS"],
      "targets": "ORIGINAL",
      "session": { "date": "2026-10-07", "day": "WEDNESDAY", "name": "Upper body", "state": "TODAY", "locked": false, "setsLogged": 0, "setsPlanned": 15 }
    },
    {
      "date": "2026-10-03",
      "score": 33,
      "causes": ["SLEEP", "SORENESS"],
      "targets": "LIGHTER",
      "session": { "date": "2026-10-03", "day": "SATURDAY", "name": "Lower body A", "state": "DONE", "locked": true, "setsLogged": 15, "setsPlanned": 15, "averageRpe": 7.1, "sessionLogId": "…" }
    }
  ],
  "week": "… unchanged …",
  "recentSessions": "… unchanged …",
  "quizAnswers": "… unchanged …"
}
```

| Field | Rule |
|---|---|
| `readiness` | Required, exactly 14 items, `today − 13` to `today` in the client's time zone, oldest first. `score` (0–100) and `band` (`READY`, `TAKE_IT_EASIER`, `RECOVERY_DAY`) are both present or both left out; left out means no check-in, never 0. |
| `recoveryDays` | Required, possibly empty, newest first; one per `readiness` item with `band = RECOVERY_DAY`, none from earlier days. |
| `recoveryDays[].causes` | Required, 1–3 of `SLEEP`, `SORENESS`, `STRESS`, in that order. |
| `recoveryDays[].targets` | `LIGHTER` or `ORIGINAL`; left out when the day had no lighter targets. |
| `recoveryDays[].session` | The existing `ClientSessionResponse`; left out when no session was planned or started that day. `sessionLogId` only when started. |

Errors (unchanged): `401` without a sign-in; `403` for a client; `404 client.not-found` for another trainer's client and an unknown id alike.

## `GET /api/v1/clients/{id}/session-logs/{sessionLogId}` (changed, task #290)

New optional field on `ClientSessionLogResponse`, left out when the day has no adjustment:

```json
{
  "adjustment": { "causes": ["SLEEP", "SORENESS"], "inUse": true },
  "entries": [
    {
      "position": 0,
      "exerciseName": "Back squat",
      "planned": { "loadKg": 100.0, "amount": 5, "rpe": 8 },
      "sets": [
        { "number": 1, "target": { "loadKg": 90.0, "amount": 5, "rpe": 7 }, "log": { "loadKg": 90.0, "amount": 5, "rpe": 7 } },
        { "number": 2, "target": { "loadKg": 90.0, "amount": 5, "rpe": 7 } }
      ]
    }
  ]
}
```

| Field | Rule |
|---|---|
| `adjustment.causes` | As in the overview. |
| `adjustment.inUse` | `true`: lighter targets in use; `false`: the client chose the original targets. |
| `entries[].planned` | Present only when that entry's target in use differs from the session's copy, as in the client's `SessionLogResponse` (was always left out for the trainer). |
| `entries[].sets[].target` | A logged set: the targets it was logged against. A set not logged: the target in use. |

Errors (unchanged): `404 client.not-found` for another trainer's client, `404 session-log.not-found` for a log that is not this client's.
