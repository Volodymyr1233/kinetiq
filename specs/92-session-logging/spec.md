# Feature Specification: Client logs sets

**Feature Branch**: `feat/92-session-logging`

**Created**: 2026-11-07

**Status**: Draft

**Input**: Story #92 "Client logs sets" and its tasks #93 (session log and set log model), #94 (start session and log set API), #95 (logger screen with set rows), #96 (set entry and tick-to-log) and #97 (rest timer). Screen: Kinetiq Screens, board "Client · Session logger". Builds on specs `75-client-plan` (the client's published plan) and `89-todays-session` (today's session and the disabled "Start session"). Later stories use it: #98 (finishing a session), #101 (editing a log within 24 hours), #104 (the trainer sees a client's training) and a follow-up task under #89 (the "Start session" button opens this logger). The story, the tasks and the board are starting points; every place this spec departs from them is listed under "Departures from the story and the board".

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Client starts today's session and sees it as a list of sets (Priority: P1)

The client opens the logger for today. Their session starts, and the screen lists each exercise of today's session with one row per planned set. Each row shows the targets in grey: load, repetitions or seconds, RPE.

**Why this priority**: Nothing can be logged until there is a started session with sets to fill.

**Independent Test**: As a client with a session today, open the logger: the session is started, every exercise shows as many rows as it has sets, each with its grey targets; open it again: the same session, nothing doubled.

**Acceptance Scenarios**:

1. **Given** a client whose plan has a session today, **When** they open the logger, **Then** today's session is started (once) and the screen shows its name, then each exercise in plan order with one row per planned set, numbered, with the targets in grey.
2. **Given** an exercise measured in seconds, **When** its rows are shown, **Then** the amount is in seconds, never repetitions.
3. **Given** a load in kilograms, **When** it is shown, **Then** it is in the client's unit; an exercise without a load shows its row without a load.
4. **Given** the session was already started today, **When** the client opens the logger again, **Then** it is the same session with the sets already logged filled in.
5. **Given** the trainer saves a change to the plan after the session started, **When** the client keeps logging, **Then** the targets stay as they were when the session started; the change applies from the next session.
6. **Given** a day without a session, or a finished plan, or a client with no published plan, **When** they open the logger, **Then** no session starts and they are told why and sent back to Today.

---

### User Story 2 - Client logs a set as planned with one tick (Priority: P1)

A tick on a row that the client has not touched logs the set with the target values. The row then shows what was logged.

**Why this priority**: This is the promise of the story: a set in one tap.

**Independent Test**: Tick an untouched row: it is logged with the target load, amount and RPE, and shows as done.

**Acceptance Scenarios**:

1. **Given** an untouched set row, **When** the client ticks it, **Then** the set is logged with the target load, repetitions or seconds, and RPE, and the row shows as logged.
2. **Given** a logged set, **When** the screen is reloaded, **Then** the row is still logged with the same values.
3. **Given** the tick is pressed twice, or the request is sent twice, **When** the sets are counted, **Then** the set is stored once.
4. **Given** the connection fails while logging, **When** the client taps again, **Then** the set is stored once, with the same result as a first success.

---

### User Story 3 - Client logs a set with different values (Priority: P1)

The client selects a row and sets the load (in steps or typed), the repetitions or seconds, and the RPE (by chips), then ticks it. They can correct a logged set the same way.

**Why this priority**: Real sets differ from the plan.

**Independent Test**: Select a row, change the load, the amount and the RPE, tick: the logged values are the ones entered; select it again, change one, tick: the set holds the new value.

**Acceptance Scenarios**:

1. **Given** the entry panel of a row, **When** the client changes the load by a step, **Then** it moves by 2.5 kg (5 lb for a client in pounds) and can also be typed.
2. **Given** the entry panel, **When** the client enters repetitions or seconds, **Then** it must be a whole number from 1; 0 or empty cannot be logged and the field says how to fix it.
3. **Given** the RPE chips, **When** the client picks one, **Then** it is a whole number from 5 to 10; nothing else can be chosen.
4. **Given** a load typed by the client, **When** it is logged, **Then** it is stored in kilograms to one decimal; an empty load means no added weight, and 0 is not a valid load.
5. **Given** a logged set, **When** the client changes it and ticks, **Then** the set is updated, not added; the logged values replace the old ones and the set count does not change.
6. **Given** each control of the entry panel, **When** it is used on a phone, **Then** it is at least 48 px high and wide.

---

### User Story 4 - Client sees a rest timer after each set (Priority: P2)

After a set is logged, a countdown of the target rest of that exercise starts and shows the time left. It never stops the client from entering or logging the next set.

**Why this priority**: It helps training but is not needed to log.

**Independent Test**: Log a set with a 90 s rest: a countdown starts from 90; log the next set while it runs: it works and the countdown restarts from that set's rest.

**Acceptance Scenarios**:

1. **Given** a set is logged, **When** the exercise has a rest above 0, **Then** a countdown of that rest starts and shows the seconds left.
2. **Given** the countdown is running, **When** the client selects another row, edits it or logs it, **Then** nothing is blocked.
3. **Given** another set is logged while it runs, **When** it is logged, **Then** the countdown restarts with that exercise's rest.
4. **Given** the countdown reaches zero, **When** it ends, **Then** it shows that rest is over, with no sound or vibration, and does not log anything.
5. **Given** the client wants to skip the rest, **When** they dismiss it, **Then** the countdown goes away.
6. **Given** the screen is reloaded, **When** it opens, **Then** no countdown is shown; the timer is not kept.

---

### User Story 5 - A session and its sets are private to the client (Priority: P1)

A client reaches only their own sessions and sets.

**Why this priority**: Logs are personal training data, and the project's rule is that another person's resource answers 404.

**Independent Test**: As another client, log a set to a session that is not yours: 404 and nothing stored. As a trainer, start a session: refused by role.

**Acceptance Scenarios**:

1. **Given** a session or a set id that belongs to another client, **When** a client logs or changes a set in it, **Then** the answer is "not found", the same as for an unknown session, and nothing is stored.
2. **Given** a trainer or a visitor who is not signed in, **When** they start a session or log a set, **Then** a trainer is refused by role and a visitor is asked to sign in.
3. **Given** a request that names a session, **When** the session is the client's own, **Then** it works; the client is always taken from the sign-in, never from the body.
4. **Given** a set id that was already used for another client's set, **When** a client sends it, **Then** it does not touch the other client's set and does not reveal that it exists.

---

### Edge Cases

- A client starts a session on the last day of the plan's last week: it works as on any day.
- Starting a session never happens for another day than the client's today in their time zone.
- A set number outside the exercise's planned sets is refused; the client logs the planned number of sets (extra sets are not in this story).
- A session started at 23:50 and logged after midnight stays today's session; the date is the one it started on.
- The same exercise twice in a session gives two groups of rows; each set is identified by its place, not by the exercise.
- A request that cannot be read (a number sent as text) is refused as a whole with the standard invalid-request code.
- A load above 999.9 kg, or a repetitions or seconds value above 3600, is refused; the entry panel prevents it.
- No error response contains server internals.

## Requirements *(mandatory)*

### Functional Requirements

**Session log** (task #93)

- **FR-001**: A started session MUST be recorded for the client: its date (the client's local date), when it started, and a copy of today's session as planned at that moment: the session's name and, in order, each exercise entry with its sets, amount (repetitions or seconds), optional load, target RPE and rest. The copy does not follow later changes of the plan.
- **FR-002**: A client MUST have at most one session log per date.
- **FR-003**: A set log MUST record, for one planned set of one exercise entry of the session log: its place (the entry's place and the set's number), the load (optional) in kilograms, the repetitions or seconds, the RPE, and when it was logged. Each set log MUST also keep a copy of the targets of its set: load, amount and RPE.
- **FR-004**: A set log MUST carry an id generated by the client; the same id MUST NOT exist twice.
- **FR-005**: The record of a session MUST also be able to hold when it finished, so story #98 can set it; nothing in this story sets it.

**Start and log API** (task #94)

- **FR-006**: A signed-in client MUST be able to start today's session; if it was started already today, the same session MUST be returned, with the sets logged so far. Starting MUST be refused, with a stable code, when the client's plan has no session today, when the plan has finished, and with the standard "no plan" answer when there is no published plan.
- **FR-007**: A client MUST be able to log a set into their own session by sending its client-generated id, its place, the load, the repetitions or seconds, and the RPE; sending the same id again with the same values MUST store it once and answer as the first time.
- **FR-008**: A client MUST be able to change a logged set by sending its id with new values; the set is updated, not added. (The rule that limits changes to 24 hours is story #101.)
- **FR-009**: The values MUST be checked: repetitions or seconds a whole number from 1 to 3600; RPE a whole number from 5 to 10; a load empty, or above 0 up to 999.9 kg with at most one decimal; the place must be a planned set of the session. A breach MUST be refused as a whole with a stable code per field, and nothing is stored.
- **FR-010**: A session or a set that belongs to another client, or does not exist, MUST answer 404 in the same way, and nothing changes. A trainer MUST be refused by role and a visitor MUST get the standard unauthenticated answer. Tests prove each.
- **FR-011**: The client MUST be taken from the sign-in, never from the request.

**Logger screen** (tasks #95, #96, #97)

- **FR-012**: The logger MUST live in the client area at its own address, without the tab bar. Opening it starts today's session (FR-006) and shows its name, then each exercise in order with one row per planned set, numbered. A row shows its targets in grey until it is logged, and its logged values afterwards.
- **FR-013**: A row's measure MUST follow its exercise: repetitions or seconds. A load MUST show in the client's own unit.
- **FR-014**: A tick on an untouched row MUST log the target values. Selecting a row MUST open its entry panel with the target values filled in: load stepper (2.5 kg or 5 lb a step, also typed), repetitions or seconds, RPE chips from 5 to 10, and the tick. A tick in the panel logs what the panel shows.
- **FR-015**: Repetitions or seconds of 0, empty, or above 3600, and a typed load of 0 or above 999.9 kg (after conversion), MUST NOT be loggable; the field is marked invalid with a message saying how to fix it, and nothing is sent.
- **FR-016**: Every control of the entry panel MUST be at least 48 px by 48 px. Each row, chip and button has an accessible name that includes its exercise and set number, because the same exercise can appear twice.
- **FR-017**: A logged set MUST show as logged by text or an icon, not by colour alone. Changing it follows FR-014 and FR-008.
- **FR-018**: A failed log MUST keep what the client entered, show a translated message and let them try again; a repeated send MUST NOT store the set twice (FR-007).
- **FR-019**: After a set is logged, a countdown of that exercise's rest MUST start, showing the seconds left, when the rest is above 0; it MUST NOT block any input; logging another set restarts it with that set's rest; it can be dismissed; it is not kept across a reload. The countdown has no sound.
- **FR-020**: While the logger loads it MUST show a skeleton of its layout; on failure a translated message and "Try again".

**Quality**

- **FR-021**: Every user-facing text MUST exist in English and Polish, with Polish plural forms; numbers and units follow the UI language and the client's settings.
- **FR-022**: The screen MUST be built from Kinetiq tokens and shadcn components, designed at 375 px, in light and dark, with visible focus and keyboard use; at 375 px and at 320 px (200% zoom) the page MUST NOT scroll sideways.
- **FR-023**: Demo data MUST let the logger be tried: `demo.client.daily` has a session on Monday to Saturday (spec `89-todays-session`) and needs no change; no demo logs are added, so a fresh session is always possible.

### Public endpoints

None. Every operation needs a signed-in client.

### Key Entities

- **Session log**: One started session of one client on one date: the date, when it started, when it finished (empty here), a copy of the planned session (its name and its exercise entries with their targets). One per client per date.
- **Set log**: What a client did in one planned set: its place in the session log, the load, the repetitions or seconds, the RPE, when it was logged, and a copy of the targets of that set. It has an id from the client.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A client logs a set exactly as planned with one tap, and a set with changes in at most four taps (select, change one value, tick), without typing for load steps and RPE.
- **SC-002**: In tests, the same set id sent twice stores one set 100% of the time, and another client's session or set answers 404 100% of the time.
- **SC-003**: A set logged while the countdown runs is accepted 100% of the time; the countdown never prevents an input.
- **SC-004**: The session started in the morning keeps its targets for the whole day, however many times the trainer saves the plan, proven in tests.
- **SC-005**: The logger passes WCAG 2.2 AA checks for contrast, labels, focus order, visible focus and touch size, in light and dark, at 375 px.
- **SC-006**: Every text of the screen exists in English and Polish.
- **SC-007**: No error response of this story contains server-internal text.

## Departures from the story and the board

| Source | It says | This spec | Why |
|---|---|---|---|
| #93 | Session log has a "planned session" | A copy of the session's name and entries, not a link | A plan save replaces its sessions and entries, so nothing of the plan can be referred to by id; the copy also keeps a started session's targets (spec `68-plan-editor`, assumption on logs). |
| #92 | Each set log "keeps a copy of its targets" | Kept, and the session log keeps the whole copy as well | The logger lists every planned set, including those not logged yet, so the planned targets must be on the session log too. |
| #89 | "Start session opens the logger" | Opening the logger starts the session; the Today button is enabled by a follow-up task under #89 | That task is blocked by #94 and #95, so it follows this story. The logger is reachable by its address until then. |
| Board | Add a set to an exercise, skip or finish | Not here | Finishing a session is #98; extra sets are not in the five tasks. |
| Board | Timer sound and vibration | None | Not in the story; a sound would need a permission and a setting. |

## Assumptions

- The client's weight unit and time zone come from their account settings (spec `42-account-settings`); loads are stored in kilograms and shown in the client's unit.
- A plan has at most one session a day (spec `68-plan-editor`), so there is one session log a day.
- "Today" is the client's local date at the moment they start (spec `89-todays-session`).
- Whether a session is open for changes is not limited here; the 24-hour rule and the locked logger are story #101, and finishing is story #98. Until then a logged set can be changed at any time.
- The rest timer lives only in the browser tab and is not stored.
- The logger is a route of its own (no tab bar), as the quiz is, reachable by its address; the follow-up task under #89 links it from Today.
- Logging without a connection (retry queue, offline banner) is story #114, not this one; this story only makes a repeated send safe.
- The trainer's view of logs is story #104; this story stores what it needs.
- The migration takes the next free Flyway number at the time of writing (V17 now), and is renumbered if another pull request takes it first.
