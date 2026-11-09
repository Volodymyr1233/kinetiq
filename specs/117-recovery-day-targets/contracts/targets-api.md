# Contract: today's targets

Every endpoint needs a signed-in client (`/api/v1/check-ins/**` and `/api/v1/session-logs/**` already require the role CLIENT). The client and the day come from the sign-in and the client's time zone. A trainer gets `403`; a visitor gets `401`. Errors are problem details with a `code`.

## `GET /api/v1/check-ins/today` (changed)

The `200` answer gains `checkIn.adjustment`, which is left out when today has no adjustment:

```json
{
  "date": "2026-11-09",
  "open": true,
  "checkIn": {
    "sleep": 2, "soreness": 4, "stress": 3, "score": 33, "band": "RECOVERY_DAY",
    "savedAt": "2026-11-09T06:40:00Z",
    "adjustment": {
      "inUse": true,
      "causes": ["SLEEP", "SORENESS"],
      "entries": [
        { "position": 0, "loadKg": 90.0, "amount": 5, "rpe": 7 },
        { "position": 1, "amount": 11, "rpe": 7 },
        { "position": 2, "amount": 40, "rpe": 6 }
      ]
    }
  }
}
```

- `entries` holds the lighter targets of every entry of today's session, in plan order. `loadKg` is left out for no load. The planned targets are the ones in `GET /api/v1/today`.
- `inUse` is `false` once the client chose the original targets.
- `causes` lists `SLEEP`, `SORENESS` and `STRESS`, in that order, never empty.
- The client may switch while `open` is `true` and `adjustment` is present.

## `PUT /api/v1/check-ins/today` (changed behaviour)

Saving different answers resets the choice to `LIGHTER`; saving the same answers keeps it. The answer is as for `GET`.

## `PUT /api/v1/check-ins/today/targets` (new)

Request:

```json
{ "choice": "ORIGINAL" }
```

`choice` is `LIGHTER` or `ORIGINAL` and is required.

| Status | `code` | When |
|---|---|---|
| `200` | — | Stored, or already so; the body is the `GET` answer |
| `400` | `validation.failed` | `choice` missing |
| `400` | `request.invalid` | `choice` not one of the two values, or a body that cannot be read |
| `404` | `plan.not-found` | No published plan (a draft alike) |
| `409` | `check-in.no-session-today` | A rest day or a finished plan |
| `409` | `check-in.no-adjustment` | No check-in today, a band other than Recovery day, or nothing that changes |
| `409` | `check-in.training-started` | A set of today's session is logged |

## `POST /api/v1/session-logs`, `GET /api/v1/session-logs/{id}` (changed)

- `entries[].sets[].target` is the target in use for an unlogged set (lighter on an adjusted day), and the set log's copy for a logged set.
- `entries[].planned` (new, `TargetResponse`) is present only when the targets in use for that entry differ from the planned ones. Otherwise it is left out.
- Another client's session log is `404 session-log.not-found`, unchanged.

## `GET /api/v1/clients/{id}/session-logs/{sessionLogId}` (trainer, changed)

Logged sets show their copy of the targets. Unlogged sets show the planned ones. There is no `planned` field: the trainer's view of the adjustment is story #122.

## `GET /api/v1/session-states` (changed, task #275)

Each session gains `adjusted` (boolean, always present): `true` when the lighter targets of that date's check-in are in use for it, computed with the plan's targets of that session. A day still to come is always `false`.

## Frontend error keys

New key: `errors.check-in.no-adjustment`, in EN and PL. The others already exist.
