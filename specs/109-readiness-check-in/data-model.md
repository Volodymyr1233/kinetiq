# Data model: Client checks in and sees readiness

## Domain (`com.kinetiq.readiness.domain.model`)

### CheckInId

Record wrapping a `UUID`; `CheckInId.random()`.

### Answers

Record `(int sleep, int soreness, int stress)`. Each a whole number from 1 to 5, checked in the compact constructor (`InvalidValueException`).

### Readiness

Record `(int score, Band band)`, built only by `Readiness.of(Answers)`:

- `sum = (sleep − 1) + (5 − soreness) + (5 − stress)`, 0–12
- `score = round(100 × sum / 12)`, one of 0, 8, 17, 25, 33, 42, 50, 58, 67, 75, 83, 92, 100
- `band`: `READY` when score ≥ 70, `TAKE_IT_EASIER` when 45–69, `RECOVERY_DAY` below 45

| Answers (sleep/soreness/stress) | Score | Band |
|---|---|---|
| 2/4/3 | 33 | RECOVERY_DAY |
| 4/2/1 | 83 | READY |
| 5/1/1 | 100 | READY |
| 1/5/5 | 0 | RECOVERY_DAY |
| sum 5 (e.g. 3/3/4) | 42 | RECOVERY_DAY |
| sum 6 (3/3/3) | 50 | TAKE_IT_EASIER |
| sum 8 (e.g. 4/2/3) | 67 | TAKE_IT_EASIER |
| sum 9 (e.g. 4/2/2) | 75 | READY |

### CheckIn (aggregate)

| Field | Type | Rule |
|---|---|---|
| `id` | `CheckInId` | made by the application |
| `client` | `AccountId` | the signed-in client |
| `date` | `LocalDate` | the client's local date |
| `answers` | `Answers` | |
| `savedAt` | `Instant` | when it was first saved or last changed |

- `CheckIn.give(client, date, answers, now)` creates one.
- `change(answers, now)` replaces the answers and `savedAt`.
- `readiness()` returns `Readiness.of(answers)`.

Whether it may be saved is decided by the service, not the aggregate: it needs today's plan state and the set logs (see the state below).

### State of today's check-in

```text
                session day, no logged set
 (none) ───────────── save ─────────────▶ saved ──── save (change) ───┐
   │                                        │ ▲                        │
   │                                        │ └────────────────────────┘
   │ first set logged                       │ first set logged
   ▼                                        ▼
 not open, none                          not open, fixed
```

`open` = today's plan state is `SESSION` and `TrainingProgress.hasLoggedSet(client, date)` is false. A rest day and a finished plan are never open.

## Ports

| Port | Package | Operations |
|---|---|---|
| `CheckInUseCase` | `readiness.domain.input` | `TodayCheckIn today(AccountId client)`; `TodayCheckIn save(SaveCheckInCommand command)` |
| `SaveCheckInCommand` | `readiness.domain.input` | record `(AccountId client, Answers answers)` |
| `TodayCheckIn` | `readiness.domain.input` | record `(LocalDate date, boolean open, CheckIn checkIn)`, `checkIn` null until the client checks in (like `TodayView.session`; `Optional` is not used for fields) |
| `CheckInRepository` | `readiness.domain.output` | `Optional<CheckIn> findByClientAndDate(AccountId, LocalDate)`; `CheckIn save(CheckIn)` |
| `TrainingProgress` | `readiness.domain.output` | `boolean hasLoggedSet(AccountId client, LocalDate date)`; implemented by `sessionlog` |
| `ViewOwnPlanUseCase` | `plan.domain.input` (existing) | `today(client)` gives the date and the state |

Exceptions (`readiness.domain.exception`), each a `DomainException` with `Kind.CONFLICT`:

| Exception | Code |
|---|---|
| `NoSessionTodayException` | `check-in.no-session-today` |
| `TrainingStartedException` | `check-in.training-started` |

## Table (`V21__create_check_ins.sql`)

```text
check_ins
  id             uuid         not null  pk_check_ins
  client_id      uuid         not null  fk_check_ins_client → accounts(id)
  check_in_date  date         not null
  sleep          smallint     not null  ck_check_ins_sleep    between 1 and 5
  soreness       smallint     not null  ck_check_ins_soreness between 1 and 5
  stress         smallint     not null  ck_check_ins_stress   between 1 and 5
  saved_at       timestamptz  not null
  created_at     timestamptz  not null
  updated_at     timestamptz  not null
  version        bigint       not null
  uq_check_ins_client_date unique (client_id, check_in_date)
```

The unique constraint's index serves `findByClientAndDate` and #122's "last 14 days" by client. No score or band column (research R5).

## Demo data (`db/dev-data/R__demo_check_ins.sql`)

Demo Client Daily (`…8000-000000000012`): check-ins on `current_date − 1`, `− 2` and `− 3` days of the first run, one per band; none today.
