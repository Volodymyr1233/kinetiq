# Data Model: Trainer sees a client's training

Nothing is stored and no table changes, so there is no migration and no new demo data. The tables are those of specs `92-session-logging` and `98-finish-session`; this story only reads them.

## Client session (derived)

One session of the client as the trainer reads it in a list (spec, Key Entities).

| Value | From | Notes |
|---|---|---|
| Date | the session log's date, or the planned day of a plan week | the client's local date |
| Name | the session log's copied name, or the plan session's name | a log keeps its own name after the plan changes |
| State | `SessionState.of` (spec 98, FR-006) | Done, In progress, Missed, Today, Planned; absent before the publish day |
| Locked | the session log's start + 24 hours is past (spec 101) | false for a session never started |
| Sets logged | the session log's set logs | 0 for a session never started |
| Sets planned | the session log's entries, or the plan session's entries | sum of each entry's sets |
| Average RPE | mean of the logged sets' RPE, one decimal, half up | absent while no set is logged |
| Session log id | the session log | absent for a session never started |

Label shown (spec FR-004): Locked when the state is Done and Locked is true; otherwise the state.

## Which sessions are listed

| List | Content | Order and size |
|---|---|---|
| This week | each planned session of the current plan week | week order; absent when the plan is not published or has finished |
| Recent sessions | every session log, plus each planned session dated on or after the publish day and before today without a session log | newest first, the first four |
| Logs tab | the same as Recent sessions | newest first, all (at most 96) |

## Client overview (derived)

| Value | Notes |
|---|---|
| Today | the client's local date |
| Week | number, Monday it starts on, its client sessions; absent as above |
| Recent sessions | as above |
| Quiz answers | the submitted answers of spec 50 and 54; absent before the quiz |

## Session log detail (derived, #107 and #261)

| Value | Notes |
|---|---|
| Client session values | as above |
| Started, finished | instants; minutes as spec 98 counts them, only when finished |
| Locks at | start + 24 hours |
| Entries | in order: exercise name and measure from the trainer's library, targets copied at the start (sets, repetitions or seconds, load, RPE) |
| Set logs | entry position, set number, logged load, repetitions or seconds, RPE |
| Note | the client's note, when there is one (#261) |
