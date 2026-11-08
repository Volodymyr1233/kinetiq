# Research: Client edits a log within 24 hours

Decisions taken while planning. Each: what, why, what else was considered.

## R1. Where the rule lives

**Decision**: In the `SessionLog` aggregate. `logSet` already receives the time; it refuses a set when that time is at or after `startedAt` + 24 hours. The service passes `clock.instant()`, the server's time when the request arrives (FR-002).

**Why**: Logging and changing a set both go through `logSet`, so one check covers both, and no other path can change a set late. `backend/CLAUDE.md`: "Aggregates expose behaviour, not setters: `@Getter` plus methods that keep invariants."

**Rejected**: A check in the service (another use case could change a set without it); a stored `locks_at` column (it depends on `started_at`, not on the key alone, and could disagree with it).

## R2. Exactly 24 hours

**Decision**: The session is open while the time is before `startedAt` + `Duration.ofHours(24)`, and locked from that instant on.

**Why**: Adding a duration to an instant counts elapsed time, so a time zone or a clock change cannot make the window 23 or 25 hours long. The story says "until 24 hours after the session started", so the end itself is already locked (Clarifications).

**Rejected**: The same local time on the next day (23 or 25 hours on a clock change, and it needs the client's time zone).

## R3. Order of the checks

**Decision**: The service keeps its order (another client's or an unknown session, then a set id of another session: `404 session-log.not-found`), and the values are checked as before (`400 set-values.invalid`). Inside `logSet`: a place the session does not have (400), an id logged at another place (`409 set-log.id-in-use`), the same values again (answered as the first time), then the lock (`409 session-log.locked`), then a place taken under another id (`409 set-log.place-taken`).

**Why**: A lock must not tell that another client's session exists, so ownership comes first (FR-005). A repeat that changes nothing stays safe after the lock, which a retry and the offline queue of story #114 rely on (spec 92, FR-007; FR-004 here).

## R4. The code

**Decision**: `SessionLogLockedException` with code `session-log.locked` and kind `CONFLICT` (409).

**Why**: The request is valid, but the session's state refuses it, as `session-log.no-session-today` and the set-log conflicts already answer with 409. The frontend reads the code, never a status or a text (constitution II).

**Rejected**: 403 (the API uses it for a refused role, `request.not-allowed`); 423 Locked (used nowhere in the API, and the code already says what happened).

## R5. No lock time in the answers

**Decision**: The answers keep their shape. The logger works out the lock moment as `startedAt` + 24 hours, and the server still decides (Clarifications).

**Why**: Task #102 covers the rule and its enforcement, not new fields. The frontend already mirrors the backend's set limits in `set-values.ts`, and the server checks again (spec 92, R8); the lock follows the same pattern.

**Rejected**: A `locksAt` field in the session's answer (outside task #102's text, and the screen would still compare it with the device's clock).

## R6. The locked logger (#103)

**Decision**: The page is locked when the device's time is at or after the lock moment, or after a refusal with `session-log.locked`. A timer set to the lock moment switches it while the page is open. A locked row shows its values as text, with no button: the logged values, or the targets with "Not logged", in the design system's locked style ("locked: dashed, read-only", 6.4). An `Alert` above the exercises says "Session locked" and why. The open entry panel and the rest timer are dropped, and the change is announced once through a polite status.

**Why**: Rows without controls leave nothing to focus (task #103, "No inputs are focusable when locked"). The timer covers a logger left open overnight (FR-011), and the refusal covers a device whose clock is behind (FR-012). The lock is never more than 24 hours away, well under the longest timer a browser keeps.

**Rejected**: Disabled controls (they still read as actions and some stay focusable); asking the server whether the session is locked (one more request for what `startedAt` already tells).

## R7. Opening an earlier session (#256)

**Decision**: `GET /api/v1/session-logs?date=YYYY-MM-DD` answers the signed-in client's session log of that date, in the same shape as starting one, or `404 session-log.not-found`. The logger opens it at `/app/session/:date`, and `/app/session` keeps starting today's session. The Plan card links to that address.

**Why**: The Plan card knows its day's date and nothing about session log ids. A date in the address survives a reload and needs no state. The answer's shape is the one the logger already shows, so the locked state of #103 applies unchanged.

**Rejected**: An address by session log id (the Plan does not know ids); a list of the week's session logs for the logger (more than it needs; whether the Plan needs one is the plan's open item).

## R8. Seeing a locked session locally

**Decision**: `quickstart.md` moves the start of a demo client's session log back 24 hours with one SQL statement. No demo data is added.

**Why**: A repeatable demo migration runs when its file changes, not every day, so a start time relative to "now" would be wrong the next day. Nothing new is stored, so no demo entity is due.
