# Data Model: Client checks in and sees readiness

## Readiness (value object, task #111)

| Part | Meaning |
|---|---|
| `score` | Whole number 0 to 100. Refused outside it |

- Built from a check-in's answers with `Readiness.fromCheckIn(sleep, soreness, stress)`; each answer is 1 to 5, otherwise refused.
- `band()` gives the readiness band: Ready for 70 and above, Take it easier for 45 to 69, Recovery day below 45.
- Not stored.

## Readiness band (enum, task #111)

`READY`, `TAKE_IT_EASIER`, `RECOVERY_DAY`.

## Check-in (aggregate, task #110, planned)

| Field | Meaning |
|---|---|
| id | typed id, created by the application |
| client | the account of the client, role CLIENT |
| day | the calendar day in the client's time zone |
| sleep | 1 to 5, more is better |
| soreness | 1 to 5, more is worse |
| stress | 1 to 5, more is worse |
| created and updated times, version | as every entity |

- At most one check-in per client and day: replacing it updates the same row.
- It is changeable until the client's first set of that day is logged.
- Readiness is calculated from the three answers when a check-in is read.

### Planned table `check_ins` (migration `V<next free>`)

| Column | Type | Constraint |
|---|---|---|
| id | uuid | primary key |
| client_id | uuid | not null, foreign key to `accounts` |
| day | date | not null |
| sleep, soreness, stress | smallint | not null, each between 1 and 5 (named check constraints) |
| created_at, updated_at, version | as `BaseEntity` | |

Unique on (`client_id`, `day`). Demo data for the table goes into the local-only Flyway location (a check-in for one demo client).
