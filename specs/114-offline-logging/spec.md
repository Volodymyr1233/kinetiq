# Feature Specification: Client logs without a connection

**Feature Branch**: `feat/114-offline-logging`

**Created**: 2026-11-09

**Status**: Draft

**Input**: Story #114 "Client logs without a connection" and its tasks #115 (pending set logs kept and replayed) and #116 (offline banner, per-log status and retry). Screens: Kinetiq Screens, board "Client · Session logger" in its offline mode. Design: the Kinetiq design system, offline and sync (6.5): an offline banner, and per-log states "Saved on this phone", "Synced" and "Not synced" with a retry. Decision: `docs/adr/0003-offline-logging-without-service-worker.md`. Builds on spec `92-session-logging` (set log, logging and changing a set, the logger, FR-007 repeat of a stored set) and spec `101-log-edit-window` (the 24-hour rule, Locked). The story, the tasks and the board are starting points; every place this spec departs from them is listed under "Departures from the story and the board".

## Clarifications

### Session 2026-11-09

- Q: When does a set turn from Saved on this phone to Not synced after a failed send that is not a refusal? → A: After 3 automatic tries with growing waits; the reason is "cannot reach the server" and Retry, which starts a new round of 3, is offered (FR-012a).
- Q: What happens to kept sets when the sign-in has expired by the time the connection returns? → A: They stay Saved on this phone, the client is sent to sign in, and they are replayed after a sign-in as the same account; another account never sends them and they are discarded (FR-005a).
- Q: What does Finish session do while offline or with sets not yet Synced? → A: It is disabled with a short reason until every set is Synced; nothing about finishing is queued (FR-006a).
- Q: With the logger open in two tabs of one account, who sends a kept set? → A: Either tab may; the server stores it once, and each tab shows a row as Synced once the server has it, refetching when it regains focus (FR-003a).
- Q: Can the client discard a Not synced set? → A: Yes: each Not synced row offers Retry and Discard; Discard removes the kept copy and the row shows what the server holds, or its targets as not logged (FR-013a).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Logs made offline are kept and sent later (Priority: P1)

A client in a gym basement loses signal while a session is open. They keep logging sets as usual. Each set is kept on the phone, survives closing the tab or the browser, and is sent by itself, once, when the connection returns.

**Why this priority**: This is the point of the story: a lost connection must not cost the client their log.

**Independent Test**: Open the logger online, switch the browser to offline, log two sets, close the tab, reopen the logger, switch back online: both sets reach the server exactly once and show as synced.

**Acceptance Scenarios**:

1. **Given** the logger is open and the connection drops, **When** the client logs or changes a set, **Then** the row shows the logged values at once and the set is kept on the phone.
2. **Given** sets kept on the phone, **When** the client closes the tab and opens the logger again while online, **Then** each kept set is sent once and the rows show them as synced.
3. **Given** the same kept set is replayed twice (a second tab, a repeated reconnect, a lost reply), **When** the server receives it again, **Then** it is stored once (spec 92, FR-007).
4. **Given** several changes to the same set made offline, **When** the connection returns, **Then** the server ends with the last values the client entered.
5. **Given** sets kept on the phone, **When** the client is signed out or signs in as another account, **Then** the kept sets of the first account are never sent under the second and are removed from the phone on sign-out.
6. **Given** a kept set whose session locked meanwhile (spec 101), **When** it is replayed, **Then** the server refuses it as locked and the row shows it as not synced with the reason (User Story 3).

---

### User Story 2 - Client sees that they are offline (Priority: P1)

While the connection is down, the logger says so and says that logging continues. When the connection returns, the message goes away.

**Why this priority**: Without it the client does not know whether their sets are safe, and may stop logging.

**Independent Test**: With the logger open, switch the browser to offline: a banner appears and rows can still be logged. Switch back online: the banner disappears.

**Acceptance Scenarios**:

1. **Given** the logger is open, **When** the connection drops, **Then** a banner says the client is offline and that logs are saved on the phone and sent later, and rows stay editable.
2. **Given** the banner shows, **When** the connection returns, **Then** the banner goes away and kept sets are sent.
3. **Given** the banner appears or goes away, **Then** assistive technology is told once, without moving focus.
4. **Given** the app is not open when the connection is down, **When** the client opens it, **Then** nothing is promised: opening the app offline is not supported (story criterion 4).

---

### User Story 3 - Each log shows where it stands, and a failure can be retried (Priority: P1)

Every logged set shows one of three states: Saved on this phone (kept, not yet sent), Synced (stored by the server), or Not synced (the server refused it or sending failed for good). A Not synced log offers Retry.

**Why this priority**: The client must be able to trust what they see, and must not lose a log silently.

**Independent Test**: Log a set offline: it shows Saved on this phone. Reconnect: it shows Synced. Make the server refuse a set: it shows Not synced with Retry, and Retry sends it again.

**Acceptance Scenarios**:

1. **Given** a set logged offline, **Then** its row shows "Saved on this phone".
2. **Given** a set stored by the server, **Then** its row shows "Synced".
3. **Given** a set the server refused (locked session, invalid values) or could not store after the connection returned, **Then** its row shows "Not synced" with the reason and a Retry action, and what the client entered is kept.
4. **Given** a Not synced row, **When** the client chooses Retry, **Then** the same set is sent again and the row shows the new state; a repeat never stores a set twice.
5. **Given** a state shown, **Then** it is given by text or an icon, not by colour alone, and a change of state is announced politely to assistive technology.
6. **Given** a set refused as locked, **When** the client chooses Retry, **Then** it is refused again with the same reason and no set is stored.

---

### Edge Cases

- A set logged offline and the connection returns during sending: it is sent once, not twice.
- The browser has no usable storage (private mode, quota full): logging offline still works for the open tab, the client is told that logs are not kept on the phone, and nothing crashes.
- Kept sets that were corrupted or written by an older version are discarded, not sent, and never crash the logger.
- A session that is not the client's, or an unknown one, answers "not found" on replay; the row shows Not synced.
- A kept set whose session was meanwhile removed from the plan is refused by the server and shown Not synced.
- The device's clock may differ from the server's: the server's time decides the 24 hours (spec 101, FR-002).
- Two tabs open on the same account: each kept set is sent once, whichever tab sends it.
- A send fails without a definite refusal (no route, timeout, temporary server error): it is tried again automatically up to 3 times with growing waits, staying Saved on this phone; after the third failure it shows Not synced, "cannot reach the server", with Retry.
- A client who logs offline across the end of the 24 hours: the replay is refused (spec 101) and shown Not synced; the client is told why.
- No error response contains server internals, and no kept text is shown from the server.

## Requirements *(mandatory)*

### Functional Requirements

**Pending logs kept and replayed** (task #115)

- **FR-001**: A set logged or changed without a connection MUST be kept on the phone, with the client-generated id the set already carries (spec 92, FR-007), until the server has stored it.
- **FR-002**: Kept sets MUST survive closing the tab and the browser, and MUST be sent without any action by the client when the app is open and the connection is back.
- **FR-003**: Replaying MUST NOT create duplicates: the same set sent any number of times is stored once (spec 92, FR-007), and a kept set is removed from the phone once the server has stored it.
- **FR-003a**: With the logger open in two tabs of one account, either tab MAY send a kept set; no lock across tabs is added. Each tab MUST show a row as Synced once the server holds the set, and MUST refresh the session when it regains focus.
- **FR-004**: Kept sets of one set MUST be sent in the order they were made, so the server ends with the last values.
- **FR-005**: Kept sets MUST belong to one account. They MUST NOT be sent under another account, and MUST be removed from the phone when the client signs out.
- **FR-005a**: When a replay is answered as unauthenticated, the kept sets MUST stay Saved on this phone, the client MUST be sent to sign in, and replay MUST resume after they sign in as the same account. After a sign-in as another account the kept sets of the first account MUST be discarded unsent. Tests prove both.
- **FR-006**: Only set logs are kept. No other request is queued while offline.
- **FR-006a**: "Finish session" MUST be disabled while the client is offline or any set of the session is Saved on this phone or Not synced, with a short reason in text; it becomes available once every set is Synced.
- **FR-007**: What is kept MUST hold no tokens, names or other identifiers of a person beyond what the set needs (constitution III); a refused set MUST NOT be sent again by itself, only by Retry.
- **FR-008**: Opening the app without a connection is not supported and MUST NOT be promised anywhere; no service worker is added (ADR 0003).
- **FR-009**: The server MUST keep its rules for a replayed set unchanged: values checked (spec 92, FR-009), ownership answering "not found" (spec 92, FR-010), the 24-hour rule (spec 101). Tests prove a replay of another client's set is "not found" and a replay after the lock is refused.

**Banner, status and retry** (task #116)

- **FR-010**: While the connection is down, the logger MUST show a banner saying that the client is offline and that logs are saved on the phone and sent later; rows stay editable. It goes away when the connection returns.
- **FR-011**: Every logged set row MUST show its state in text or an icon: Saved on this phone, Synced, or Not synced. The exercise's summary state follows its worst row.
- **FR-012**: A Not synced row MUST show the reason in words (locked, invalid values, or cannot reach the server), keep what the client entered, and offer Retry with an accessible name that includes its exercise and set number.
- **FR-012a**: A failed send that is not a definite server refusal MUST be tried again automatically, 3 tries in all with growing waits; after the third failure the row is Not synced and only Retry starts a new round of 3. A definite refusal (locked, invalid, not found) MUST NOT be tried again by itself and shows Not synced at once. Tests with a fixed clock prove the third failure and the immediate refusal.
- **FR-013**: Retry MUST send the kept set again; on success the row shows Synced and the kept copy is removed; on failure it stays Not synced.
- **FR-013a**: A Not synced row MUST also offer Discard, with an accessible name that includes its exercise and set number. Discard removes the kept copy, and the row shows what the server holds for that set, or its targets marked as not logged when it holds none. A discarded set is never sent. Discard is not offered on a row that is Saved on this phone.
- **FR-014**: A change of the banner or of a row's state MUST be announced politely to assistive technology, without moving focus.
- **FR-015**: A set refused as locked MUST also turn the logger read-only (spec 101, FR-012); its kept copy stays visible as Not synced until the client leaves.

**Quality**

- **FR-016**: Every new text MUST exist in English and Polish.
- **FR-017**: The banner, the row states and Retry MUST be built from Kinetiq tokens and shadcn components, at 375 px, in light and dark, with visible focus, controls of at least 48 px, and no sideways scroll at 375 px and at 320 px (200% zoom).
- **FR-018**: The result MUST be checked in a real browser with its offline mode (log offline, close the tab, reopen, reconnect) and against the real backend (a replay stored once, another client's replay "not found", a late replay refused).

### Key Entities

- **Pending log**: A set log made on the phone that the server has not yet confirmed: its client-generated id, its session, its place, its values, who made it, and when.
- **Log state**: Where one set stands for the client: Saved on this phone, Synced, or Not synced (with its reason).

### Public endpoints

None. Replays use the existing authenticated set-log endpoint; no endpoint is opened.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every set logged while offline reaches the server after the connection returns, with no action from the client, in 100% of test runs.
- **SC-002**: A set replayed any number of times produces exactly one stored set.
- **SC-003**: A client who closes and reopens the tab with sets pending loses none of them.
- **SC-004**: A client always sees one of three states for each logged set, and can tell within a glance whether they are offline.
- **SC-005**: A refused set is never lost silently: it stays visible with its reason and a Retry until sent or the client leaves.
- **SC-006**: The offline banner, row states and Retry pass a check at 375 px in light and dark, by keyboard and with labels, with no sideways scroll.

## Departures from the story and the board

- The board shows one badge per exercise card ("Saved on this phone" / "Synced"); this spec shows the state per logged set row too, as the story criterion 2 says "each log", keeping the card badge as the worst row's state.
- The board's offline copy ("Logs save on this phone and sync later") is kept in spirit; the exact text is part of the translations.
- Retry and Discard exist only for Not synced rows; Saved on this phone rows are sent by themselves.

## Assumptions

- Pending logs are kept in the browser's local storage, replayed by the query library's paused mutations (ADR 0003); a session is a few kilobytes.
- The set-log request already accepts a client-generated id and answers a repeat as the first time (spec 92); no server change is expected beyond tests, unless the plan finds a gap.
- "Offline" means the browser reports no connection or a send fails to reach the server; a definite server refusal is not offline.
- The logger already exists (spec 92) and shows rows, the entry panel and the locked state (spec 101); this story adds to it.
- Only the client's own logger is in scope; the trainer's view of logs is unchanged.
