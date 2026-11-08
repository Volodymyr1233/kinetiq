# Feature Specification: Client edits a log within 24 hours

**Feature Branch**: `feat/102-24-hour-rule`

**Created**: 2026-11-07

**Status**: Draft

**Input**: Story #101 "Client edits a log within 24 hours" and its tasks #102 (24-hour rule), #103 (locked logger state) and #256 (open a past session from Plan). Screens: Kinetiq Screens, boards "Client · Session logger" and "Client · Plan". Design: the Kinetiq design system, set logger (6.4, row state "locked: dashed, read-only"), offline and sync (6.5, "Logs lock 24 hours after the session (read-only state plus a reason)") and the read-only field state ("muted fill, dashed border, a lock reason under the field"). Builds on spec `92-session-logging` (session log, set log, logging and changing a set, the logger) and spec `75-client-plan` (the Plan tab and its session cards). Related: story #98 (finishing a session), story #114 (logging without a connection) and task #107 (the trainer's logs tab labels locked sessions). The story, the tasks and the board are starting points; every place this spec departs from them, or from an earlier spec, is listed under "Departures from the story and earlier specs".

## Clarifications

### Session 2026-11-08

- Q: Does the session's answer say when it locks, or does the logger work it out? → A: The logger works it out: 24 hours after the start time the answer already holds. The server still decides, so task #102 changes no answer (FR-008).
- Q: Is a set sent exactly 24 hours after the session started still accepted? → A: No. The session is locked from that moment (FR-001).
- Q: After the lock, is a repeat of a stored set with the same values refused? → A: No. It answers as the first time and changes nothing (FR-004).
- Q: Can a client open an earlier session that is still within its 24 hours to fix it? → A: Yes, from the Plan tab: the opened card of a past session with a session log offers "Edit log" while its 24 hours last and "View log" after. A button rather than a tap on the day, because a tap already opens the card (spec 75, FR-010). New task #256 (FR-013 to FR-015).
- Q: Does finishing a session end its 24 hours? → A: No. Finishing (task #99) leaves logging open until the 24 hours end (FR-006).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A late change is refused (Priority: P1)

The client can log and change the sets of a session for 24 hours after it started. After that, every attempt to log or change a set is refused with a stable reason, and nothing changes.

**Why this priority**: This is the rule the story adds. Without it a log can be rewritten at any time, and the trainer cannot rely on what they read.

**Independent Test**: With a fixed clock, start a session on Monday at 18:00 and change a set on Tuesday at 17:59:59: it is stored. Send another change on Tuesday at 18:00:00: it is refused with the lock code and the set keeps its values.

**Acceptance Scenarios**:

1. **Given** a session started on Monday at 18:00, **When** the client logs a new set or changes a logged one before Tuesday 18:00, **Then** it is stored as in spec 92.
2. **Given** the same session, **When** the client logs or changes a set on Tuesday at 18:00 or later, **Then** it is refused with a stable code meaning "locked", and nothing is stored or changed.
3. **Given** a set logged before the 24 hours ended, **When** the same set is sent again with the same values after they ended, **Then** the answer is the same as the first time and nothing changes (spec 92, FR-007).
4. **Given** a session that is not the client's, **When** they send a set after its 24 hours, **Then** the answer is "not found", as for any other client's session; the lock does not reveal that the session exists.
5. **Given** a finished session (story #98) whose 24 hours have not ended, **When** the client changes a set or logs a new one, **Then** it is stored; finishing does not end the 24 hours.

---

### User Story 2 - Client sees a locked session read-only and knows why (Priority: P1)

When the session shown in the logger has passed its 24 hours, the logger shows every set as it is, read-only, and says why it can no longer be changed.

**Why this priority**: Without it the client taps, fails and does not know why (story acceptance criterion 2).

**Independent Test**: Open the logger on a session past its 24 hours: every row shows its logged values or that it was not logged, nothing can be selected, typed or ticked, no control of the rows takes keyboard focus, and a message says why.

**Acceptance Scenarios**:

1. **Given** a session past its 24 hours, **When** the logger shows it, **Then** every row shows its logged values, or its targets marked as not logged, in the locked style, and a message says the session is locked because sets can be logged and changed for 24 hours after a session starts.
2. **Given** the locked logger, **When** the client uses the keyboard or taps a row, **Then** no row, field, chip or tick takes focus or opens the entry panel; the back link still works.
3. **Given** the logger is open, **When** the session's 24 hours end, **Then** the screen turns read-only at that moment, without a reload: an open entry panel closes, the rest timer goes away, and the change is announced once to assistive technology.
4. **Given** the logger shows a session as open, **When** a log is refused with the lock code, **Then** the screen turns read-only with the same message, and the refused row keeps what it showed before.

---

### User Story 3 - Client opens a past session from the Plan tab (Priority: P1)

A client who notices a wrong log the next morning opens the Plan tab, opens yesterday's session and chooses "Edit log". The logger shows that session, and they fix the set while its 24 hours last. After that the same card offers "View log", which shows the session read-only.

**Why this priority**: The logger opens only today's session, so without this the client cannot reach yesterday's session to fix it, which is what the story is for.

**Independent Test**: Log a set in a session, then open the Plan tab the next day before the session's 24 hours end: the session's card offers "Edit log", the logger opens on that session and a set can be changed. After the 24 hours the card offers "View log", and the logger shows the session locked.

**Acceptance Scenarios**:

1. **Given** a session started yesterday at 19:00 with sets logged, **When** the client opens its card on the Plan tab today at 08:00, **Then** the card offers "Edit log", which opens the logger on that session, where sets can be logged and changed.
2. **Given** a past session whose 24 hours have passed, **When** its card opens, **Then** it offers "View log", which opens the logger on that session, read-only with the reason (User Story 2).
3. **Given** a past session that was started but never finished, **When** its card opens, **Then** it offers the same action as a finished one, by the same 24 hours.
4. **Given** a past day without a session log, or a day still to come, **When** its card opens, **Then** it offers neither action.
5. **Given** the logger opened from the Plan tab, **When** the client goes back, **Then** they return to the Plan tab on the week they came from.
6. **Given** a session log of another client, **When** a client asks for it, **Then** the answer is "not found", the same as for an unknown session.

---

### Edge Cases

- The 24 hours count from the moment the session started (when the client opened the logger), not from midnight or from the plan's day, and last exactly 24 hours whatever the time zone or a clock change.
- On a day when clocks go back, a day lasts 25 hours: a session started just after midnight locks before the day ends, and the logger then shows today's session read-only.
- A set sent without a connection and delivered after the 24 hours (story #114) is refused like any late change; the time that counts is when the server receives it.
- A logger left open overnight turns read-only when the session's 24 hours end.
- Starting today's session is not affected: a session is never locked when it starts, and opening the logger again returns the same session, locked or not.
- A refused late change leaves the set and its "logged at" time as they were.
- The device's clock may differ from the server's. The server decides; the screen turns read-only on the server's refusal at the latest.
- No error response contains server internals.

## Requirements *(mandatory)*

### Functional Requirements

**24-hour rule** (task #102)

- **FR-001**: The sets of a session log MUST be loggable and changeable from the moment the session started until exactly 24 hours later. From that moment on the session is locked.
- **FR-002**: The time that counts MUST be the server's time when the request arrives.
- **FR-003**: A request to log or change a set of a locked session MUST be refused with a stable code meaning "locked", and nothing is stored or changed. Tests with a fixed clock prove the last moment before the end and the moment of the end.
- **FR-004**: A repeat of a set already stored, at the same place and with the same values, MUST answer as the first time even when the session is locked, because nothing changes (spec 92, FR-007).
- **FR-005**: A session that is not the client's MUST answer "not found" whether it is locked or not (spec 92, FR-010).
- **FR-006**: Finishing a session (story #98) MUST NOT end or shorten the 24 hours; the 24 hours are the only limit on changes that this story adds.
- **FR-007**: `CONTEXT.md` MUST gain the term "Locked".

**Locked logger** (task #103)

- **FR-008**: The logger MUST know when the session it shows locks: 24 hours after the session started, a time the session's answer already holds. The server still decides (FR-002, FR-012).
- **FR-009**: When the session shown has passed its 24 hours, the logger MUST show every set row read-only in the design system's locked style: the logged values, or the targets marked as not logged. Nothing can be selected, typed or ticked, and no control of the rows is focusable.
- **FR-010**: The locked logger MUST say why in text: the session is locked, because sets can be logged and changed for 24 hours after a session starts. The state is not shown by colour or line style alone.
- **FR-011**: When the 24 hours end while the logger is open, the screen MUST turn read-only without a reload, close an open entry panel, remove the rest timer, and announce the change once.
- **FR-012**: When a log is refused with the lock code, the logger MUST turn read-only with the same message; the refused row keeps what it showed before.
**Past session from the Plan tab** (task #256)

- **FR-013**: On the Plan tab, the opened card of a past session that has a session log, finished or not, MUST offer "Edit log" while its 24 hours last and "View log" after. A past day without a session log and a day still to come MUST offer neither.
- **FR-014**: A signed-in client MUST be able to read one of their own session logs from an earlier day, without starting today's session. Another client's session log and an unknown one MUST answer "not found" in the same way, and a test proves it.
- **FR-015**: The logger MUST open a chosen earlier session at an address of its own, changeable within its 24 hours and locked after (FR-009 to FR-012); going back returns to the Plan tab on the week the client came from.

**Quality**

- **FR-016**: Every new text MUST exist in English and Polish.
- **FR-017**: The locked logger and the Plan tab's new actions MUST be built from Kinetiq tokens and shadcn components, at 375 px, in light and dark, with visible focus on what stays focusable; at 375 px and at 320 px (200% zoom) the page MUST NOT scroll sideways.

### Public endpoints

None. Every operation needs a signed-in client.

### Key Entities

- **Session log**: Unchanged from spec `92-session-logging`, except that it locks 24 hours after it started. Nothing new is stored: the moment it locks follows from when it started.
- **Locked**: A session log whose 24 hours have passed. Its sets can be read but no longer logged or changed.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In tests with a fixed clock, a set sent one moment before the 24 hours end is stored, and a set sent at the end is refused, 100% of the time.
- **SC-002**: A refused late change leaves the session log exactly as it was in 100% of tests.
- **SC-003**: A client looking at a locked session can tell that it is locked and why within 5 seconds, without trying to change a set.
- **SC-004**: On a locked session, no control of the set rows can be reached with the keyboard.
- **SC-005**: The locked logger passes WCAG 2.2 AA checks for contrast, labels, focus order and visible focus, in light and dark, at 375 px.
- **SC-006**: Every new text exists in English and Polish.
- **SC-007**: No error response of this story contains server-internal text.
- **SC-008**: From the Plan tab, a client reaches yesterday's session in the logger with two taps: open its card, then "Edit log".

## Departures from the story and earlier specs

| Source | It says | This spec | Why |
|---|---|---|---|
| Spec 92, FR-008 and Assumptions | A logged set can be changed at any time, until this story adds a limit | A set can be logged or changed for 24 hours after the session started | This is the limit spec 92 left to this story. |
| Spec 92, FR-006 and FR-012 | The logger opens today's session | It also opens an earlier session chosen on the Plan tab | Otherwise yesterday's session cannot be reached to fix it. |
| Board "Client · Plan" | A past session's card has a status badge and no action | It also offers "Edit log" or "View log" | The board has no way back to a past session; the button follows the design system's Button. |

## Texts

The main English and Polish texts. Polish uses informal "ty" and should be confirmed by a copywriter, as the design system says (8.5).

| Where | English | Polish |
|---|---|---|
| Locked message, title | Session locked | Sesja zablokowana |
| Locked message, text | Sets can be logged and changed for 24 hours after a session starts. | Serie można zapisywać i zmieniać przez 24 godziny od rozpoczęcia sesji. |
| Row not logged | Not logged | Nie zapisano |
| Plan card action, within 24 hours | Edit log | Popraw log |
| Plan card action, after 24 hours | View log | Zobacz log |

## Assumptions

- "After the session started" means after the session log was started, the moment the client opened the logger (spec 92, FR-001), as the story says.
- Finishing a session (story #98) does not lock it; the story limits changes by time only, and a client most often notices a wrong log after finishing.
- The trainer's logs tab labels a locked session by the same rule (task #107); this story provides the rule and does not build that label.
- The Plan tab's session states (Done, Missed and the others) come with task #100 (story #98); task #256 adds its action to the cards those states are shown on.
- A log replayed after the 24 hours by the offline queue (story #114) is refused like any late change; the design system shows such a log as "Locked" (6.5).
- The device's clock is roughly right. A screen that turns read-only a little early or late is corrected by the server, which decides.
- Nothing is stored, so this story needs no migration and no new demo entity. Seeing a locked session in the app needs a session log that started more than 24 hours ago, which `quickstart.md` explains.
