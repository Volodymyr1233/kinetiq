# Data Model: Client edits a log within 24 hours

Nothing is stored and no table changes, so there is no migration and no new demo data. The tables are those of spec `92-session-logging`.

## Session log

| Value | From | Notes |
|---|---|---|
| Lock moment | `started_at` + 24 hours | Derived on each use, never stored; the same for every time zone |

## What the 24 hours allow

| Request | Before the lock moment | At or after it |
|---|---|---|
| Log a new set | stored | `409 session-log.locked`, nothing stored |
| Change a logged set | changed | `409 session-log.locked`, the set keeps its values and its "logged at" |
| The same set again, same place and values | answered as the first time | answered as the first time, nothing changes |
| Any of these on another client's or an unknown session | `404 session-log.not-found` | `404 session-log.not-found` |
| Start today's session | as spec 92 | as spec 92; a session is never locked when it starts |

## State

```
started --(24 hours after the start)--> locked
finished (story #98) does not change the lock moment
```
