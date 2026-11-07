# Research: Client logs sets

## R1. A feature of its own

**Decision**: A backend feature `sessionlog` (aggregates `SessionLog` with its `LoggedEntry` copies and `SetLog`s) and a frontend feature `sessionlog`.

**Why**: Its lifecycle differs from the plan's: started by the client, finished in #98, read by the trainer in #104. Putting it in `plan` would grow that feature past its job. It depends on `plan` one way, so there is no cycle.

**Rejected**: Extending `plan` (mixes the trainer's prescription with the client's record).

## R2. Where today's session comes from

**Decision**: The start use case calls `ViewOwnPlanUseCase.today(client)` and copies the `SESSION` state's session; `REST` and `FINISHED` become `NoSessionTodayException` (`session-log.no-session-today`), and no plan stays `plan.not-found`.

**Why**: The rules for today (time zone, weekday, finished) are in one place already (spec 89); the log must not decide them again.

## R3. A copy, not a link

**Decision**: `session_logs` keeps the session's name; `session_log_entries` keeps each entry (exercise id, sets, amount, load, RPE, rest) by position. `set_logs` keeps the targets of its set (load, amount, RPE) as well.

**Why**: A plan save replaces its sessions and entries, so they have no stable id; a started session must keep its targets for the day (spec 68 assumption; story 92, "keeps a copy of its targets"). The entry copy lets the logger list sets not logged yet; the set copy is what the trainer and the rules read later.

**Rejected**: A foreign key to plan rows (they are replaced); only per-set copies (the logger could not list unlogged sets).

## R4. Endpoints

**Decision**: `POST /api/v1/session-logs` starts today's session or returns it (`201` when created, `200` when it existed). `PUT /api/v1/session-logs/{id}/set-logs/{setLogId}` logs or changes a set (`200`, the logged set). No `GET`.

**Why**: Starting is idempotent, so opening the logger is one call that also serves a reload; a `GET` would repeat it. `PUT` by a client id is idempotent by nature: the first call creates, a repeat with the same values changes nothing, with new values it replaces.

**Rejected**: `POST` for a set (needs its own idempotency key handling); a separate `GET today`.

## R5. Idempotency and ownership

**Decision**: `set_logs.id` is the primary key. A `PUT` whose session is not the client's is `404 session-log.not-found`. An id that already belongs to another session answers the same `404`. An id that exists in this session at another place is `409 set-log.id-in-use`. A second set log at an already logged place with a different id is `409 set-log.place-taken`.

**Why**: The place is unique per session; the id says which set it is. Another client's id must not be revealed (FR-010, US5), so it answers like an unknown session.

## R6. Validation

**Decision**: In the domain (`SetValues`): amount 1–3600 whole; RPE 5–10 whole; load empty or above 0, at most 999.9 with one decimal; the place must be a planned set (entry position within the entries, set number from 1 to its sets). A breach is `set-values.invalid` (400) with a `field`. Bean Validation checks types at the boundary.

**Why**: The limits are the plan's (spec 68, FR-005); the invariant belongs in the domain.

## R7. The logger as a route outside the tab bar

**Decision**: `/app/session`, a sibling of `/app/quiz/...` behind `requireRole('CLIENT')`, no tab bar; it calls start on load and navigates to `/app` when the answer is `plan.not-found` or `no-session-today`.

**Why**: Logging takes the whole screen on a phone. The Today button will link to it in the follow-up task of #89.

## R8. Entering values

**Decision**: Draft values per row in component state (load as text in the client's unit, amount, RPE). The tick logs the draft; an untouched row's draft is its targets. Load steps are 2.5 kg or 5 lb. Validation lives in `set-values.ts` and is shared by the panel and the tick.

**Why**: One source of truth for what can be logged; the server checks again.

## R9. The client's id

**Decision**: `crypto.randomUUID()` when the row first needs one, kept in the row's draft until the server answers; a retry sends the same id.

**Why**: That is what makes a repeated send store once.

## R10. Rest timer

**Decision**: A component with `endsAt` and a one-second tick that computes the seconds left from `Date.now()`; started by a successful log, restarted by the next, dismissible, not persisted, no sound.

**Why**: Computing from `endsAt` survives a throttled background tab; nothing else is needed.

## R11. Weight steps

**Decision**: ±2.5 kg for kilograms, ±5 lb for pounds, on the value in the client's unit; the value sent is converted with the shared `toKg`.

**Why**: Gym plates, and the unit follows the account (spec 42).
