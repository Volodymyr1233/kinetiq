# Data model: Trainer and client sign in and out

## Account (existing, changed)

| Field | Change |
|---|---|
| `timeZone` | No longer final. `changeTimeZone(ZoneId)` replaces it; the same zone is a no-op. |

`AccountRepository` gains `findById(AccountId)`. No schema change.

## Sign-in (new, Spring Session)

Migration `V5__create_spring_session.sql`, copied from Spring Session's `schema-postgresql.sql`:

| Table | Columns | Constraints |
|---|---|---|
| `spring_session` | `primary_id char(36)`, `session_id char(36)`, `creation_time bigint`, `last_access_time bigint`, `max_inactive_interval int`, `expiry_time bigint`, `principal_name varchar(100)` | PK `spring_session_pk`; unique index `spring_session_ix1 (session_id)`; indexes `spring_session_ix2 (expiry_time)`, `spring_session_ix3 (principal_name)` |
| `spring_session_attributes` | `session_primary_id char(36)`, `attribute_name varchar(200)`, `attribute_bytes bytea` | PK `spring_session_attributes_pk`; FK `spring_session_attributes_fk` → `spring_session` on delete cascade |

- `principal_name` holds the account id, never the email.
- The only attribute written is Spring Security's context (R6).
- Lifecycle: created at sign-in → `last_access_time` and `expiry_time` move on every request → deleted at sign-out or by the clean-up job once `expiry_time` passes (14 days idle).
- Table names stay Spring Session's (they are configuration, not domain), so no `table-name` property is needed.

## FailedSignIns (new, domain, in memory)

Per `Email` (with or without an account).

| Field | Type | Rule |
|---|---|---|
| `email` | `Email` | key |
| `failures` | `List<Instant>` | failures within the last 15 minutes, at most 5 |
| `lockedUntil` | `Instant`, absent until locked | the fifth failure within 15 minutes sets it to that failure + 15 minutes |

| Method | Behaviour |
|---|---|
| `isLocked(now)` | `lockedUntil` is present and `now` is before it |
| `timeLeft(now)` | `lockedUntil − now` while locked |
| `recordFailure(now)` | ignored while locked; drops failures older than 15 minutes; adds `now`; on the fifth sets `lockedUntil` and clears `failures` |
| `isExpired(now)` | not locked and no failure within 15 minutes; the adapter may drop it |

The end of a lock needs no action: `isLocked` turns false and `failures` is already empty. A right password deletes the record.

Port `FailedSignInsRepository`: `Optional<FailedSignIns> find(Email)`, `void save(FailedSignIns)`, `void delete(Email)`. Adapter `InMemoryFailedSignInsRepository`.

## Requests per address (new, input adapter, in memory)

`SourceAddressLimiter`: key (kind `SIGN_UP` | `CONFIRMATION_LINK`, address string) → up to 10 request times. `tryAcquire(kind, address)` is true and records the time when fewer than 10 fall within the last hour. Keys whose times are all older than an hour are dropped when the map holds more than 10,000.

## Frontend state

| What | Where | Cleared |
|---|---|---|
| Current account | TanStack Query, key `['current-account']` | sign-out (`queryClient.clear()`), full page load after a 401 |
| Quiz draft (`kinetiq.quiz`) and anything else of the tab | `sessionStorage` | sign-out and sign-in (`sessionStorage.clear()`) |
| Page to return to | `next` query parameter of `/sign-in` | used once on sign-in |
