# Feature Specification: Client asks about a logged set

**Feature Branch**: `feat/172-ask-about-set`

**Created**: 2026-11-10

**Status**: Draft

**Input**: Story #172 "Client asks about a logged set" and its tasks #173 (About reference on a message), #174 (Ask-your-trainer action on a set) and #175 (About chip in both conversation views). Screens: Kinetiq Screens, boards "Client · Session logger" and "Client · Messages", with "Trainer · Messages" and "Trainer · Client logs" for the trainer's side. Design: the Kinetiq design system, messaging (6.7, "A message started from a log carries an **About** context chip (session, exercise, set, RPE) that links back") and the set logger (6.4). Builds on spec `58-client-trainer-messaging` (conversation, message, sending with retry, both conversation screens), spec `92-session-logging` (session log, set log, the logger), spec `101-log-edit-window` (locked sessions, an earlier session at its own address) and spec `104-client-training` (the trainer's Logs tab). The story, the tasks and the boards are starting points; every place this spec departs from them, or from an earlier spec, is listed under "Departures from the story, the boards and earlier specs".

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Client asks their trainer about one logged set (Priority: P1)

A client has logged a set that felt wrong: too heavy, a sore knee, an odd RPE. On that set's row in the logger they choose "Ask your trainer". Their conversation with the trainer opens with the set already attached to the message they are about to write, so they type only the question. They send it and go back to the same session, where they left it.

**Why this priority**: This is the story. Today the client has to describe the set in words ("the third squat set on Monday"), and the trainer has to find it.

**Independent Test**: Sign in as a client with a session that has a logged set. Choose "Ask your trainer" on that set, type a question, send it. The message is Sent, and it carries the set. Close the conversation: the logger shows the same session, and a running rest timer is still running.

**Acceptance Scenarios**:

1. **Given** a set the server has stored, **When** the client looks at its row in the logger, **Then** the row offers "Ask your trainer", named for assistive technology with the exercise and the set number.
2. **Given** a planned set that is not logged, or a log that is Saved on this phone or Not synced, **When** the client looks at its row, **Then** the row does not offer the action.
3. **Given** the client chooses the action on set 3 of Back squat, **When** the conversation opens, **Then** the composer shows an About chip for that set (session, its date, exercise, set number and RPE), the message field has focus, and nothing has been sent.
4. **Given** the chip in the composer, **When** the client sends a message, **Then** that message carries the reference to the set, the chip leaves the composer, and the next message carries none.
5. **Given** the chip in the composer, **When** the client removes it, **Then** the message they send is an ordinary message.
6. **Given** the chip in the composer and an empty message field, **When** the client looks at Send, **Then** it is disabled: a reference alone is not a message.
7. **Given** a message with a reference could not be delivered, **When** the client chooses Retry, **Then** it is sent with the same text and the same reference, and the trainer receives it once.
8. **Given** the conversation was opened from the logger, **When** the client closes it, with or without sending, **Then** they are back in the same session at the same place, with its entry panels and rest timer as they were. A chip that was not sent is gone.
9. **Given** a session that is locked, **When** the client opens it, **Then** every logged set still offers "Ask your trainer" and the action works as above. Nothing else on a locked row can be used.

---

### User Story 2 - Both people see what the message is about and reach the set (Priority: P1)

A message that carries a reference shows an About chip above its text in the client's and in the trainer's view of the conversation. The chip names the set. Choosing it opens that set: for the client, the logger on that session; for the trainer, that session in the client's Logs tab.

**Why this priority**: Acceptance criterion of the story. The chip is what tells the trainer "exactly what I mean"; without the link the trainer still has to look the set up.

**Independent Test**: After User Story 1, the client sees the chip above their message, and it opens the logger on that session with the set in view. The trainer opens the conversation, sees the same chip above the client's message, and one click opens the client's Logs tab with that session chosen and the set marked.

**Acceptance Scenarios**:

1. **Given** a message with a reference, **When** either person reads the conversation, **Then** an About chip sits directly above that message's text, on the sender's side, reading the label "About" and then the session's name, the session's date, the exercise, the set number and the RPE.
2. **Given** a message without a reference, **When** it is shown, **Then** it looks as it did before this story.
3. **Given** the chip on the client's screen, **When** the client chooses it, **Then** the logger opens on that session, read-only if it is locked, with that set's row brought into view and marked.
4. **Given** the chip on the trainer's screen, **When** the trainer chooses it, **Then** the client's Logs tab opens with that session chosen and that set's row brought into view and marked. The address holds the session and the set, so a reload shows the same.
5. **Given** the client changed the set after asking, while its session was not locked, **When** either person reads the conversation, **Then** the chip shows the set as it is now, the same values the link leads to.
6. **Given** a message with a reference is still Sending or is Not sent, **When** the client looks at it, **Then** its chip is already shown above it.
7. **Given** a message with a reference arrives while the trainer has the conversation open, **When** a short time passes, **Then** it appears with its chip, without a reload.
8. **Given** a screen reader, **When** it reaches a message with a reference, **Then** the chip is read as a link whose name holds "About" and everything the chip shows.

---

### User Story 3 - Only the sender's own set can be referenced (Priority: P1)

A reference can point only to a set the sender logged themselves. A client cannot attach another client's set, and a trainer cannot attach any set. A refused reference tells nothing about whether the set exists.

**Why this priority**: Acceptance criterion of task #173. A reference makes the server look up a set and show its values to another person; without the rule it would be a way to read someone else's logs.

**Independent Test**: With two clients, client A sends a message with the id of one of client B's set logs: it is answered "not found", exactly as for an id that does not exist, and no message is stored. A trainer sending any set log id gets the same answer.

**Acceptance Scenarios**:

1. **Given** a client, **When** they send a message that references one of their own stored set logs, **Then** it is stored with the reference.
2. **Given** a client, **When** they send a message that references another client's set log, **Then** the answer is "not found" with a stable code, the same as for a set log that does not exist, and no message is stored.
3. **Given** a trainer, **When** they send a message that references a set log, even one of their own client, **Then** the answer is the same "not found" and no message is stored.
4. **Given** a message stored with a reference, **When** the same attempt is sent again, **Then** the answer is the same as the first time and the message is stored once.
5. **Given** a conversation, **When** a person who is not one of its two people asks for its messages, **Then** the answer is "not found" as before, and nothing about any referenced set is returned.

---

### Edge Cases

- A set log is never deleted, so a stored reference always has a set to show. A plan saved again later does not change it: a session log keeps its own copy of the session's name and exercises.
- The same exercise can appear twice in a session. The reference points to one logged set, so the chip and both links always mean the right exercise entry.
- A set can be asked about more than once; each message carries its own reference to the same set.
- A message carries at most one reference.
- The client asks about a set while its session can still change, then changes the RPE: every chip for that set shows the new RPE from then on. Once the session locks, the chip no longer changes.
- An exercise measured in seconds reads the same on the chip: set number and RPE. Load, repetitions and seconds are not on the chip; the link shows them.
- A long session or exercise name wraps inside the chip and never makes the page scroll sideways, at 375 px and at 320 px.
- With no connection, the action is still offered on sets the server has stored; the conversation then behaves as spec 58 describes for a failed load or a message that is Not sent.
- A set logged with no connection becomes askable when it turns Synced, without a reload.
- The chip in the composer is not kept by the browser: leaving the conversation or reloading the page drops it. A Not sent message keeps its reference as long as it keeps its text (spec 58).
- The trainer's conversation list and the unread counts are unchanged: the list shows the beginning of the message text, never the chip.
- A reference never changes what a message's text may be: 1 to 2000 characters, plain text.
- Two tabs or devices of the same account show the same chip after a short time, as any message.
- No error response contains server internals.

## Requirements *(mandatory)*

### Functional Requirements

**About reference** (task #173)

- **FR-001**: A message MAY carry one About reference: a pointer to one stored set log. A message without one behaves exactly as before this story.
- **FR-002**: A reference MUST point to a set log of a session log that belongs to the sender. Any other set log, a set log that does not exist, and any reference sent by a trainer MUST be refused with one stable code meaning "not found", the same in every case, and no message is stored. Tests prove another client's set log and a trainer's attempt.
- **FR-003**: The reference MUST be fixed when the message is stored. Nobody can add, change or remove it afterwards, as nobody can edit a message.
- **FR-004**: Sending with a reference MUST stay safe to repeat (spec 58, FR-011): a repeated attempt with the same identity, text and reference answers as the first and stores nothing new.
- **FR-005**: For each message with a reference, both people of the conversation MUST receive what the chip shows and what its link needs: the session's name and date, the exercise's name, the set number, the set's RPE as it is now, and the identity of the session and of the set's place in it. Nothing else about the set, the session or the client is added to the message.
- **FR-006**: A reference MUST NOT widen who can read anything: messages are returned only to the two people of the conversation (spec 58, FR-014), and the trainer of a conversation is already allowed to read that client's logs (spec 104).
- **FR-007**: A reference MUST NOT change anything else about messaging: the page of 50 messages, the order of messages, the unread counts, marking read and the trainer's conversation list, which shows the beginning of the message text and never the reference.
- **FR-008**: References MUST NOT be sent to any LLM and MUST NOT be written to logs together with message text (spec 58, FR-016).
- **FR-009**: `CONTEXT.md` MUST gain the term "About reference".
- **FR-010**: The local demo data MUST include at least one message with a reference, from a demo client about one of their logged sets, so the chip can be seen in both views without setting anything up.

**Ask-your-trainer action** (task #174)

- **FR-011**: In the logger, every set row whose log the server has stored MUST offer the action "Ask your trainer". A row that is not logged, Saved on this phone or Not synced MUST NOT offer it.
- **FR-012**: The action MUST be a real button with a touch target of at least 44 px and an accessible name that holds the exercise and the set number ("Ask your trainer about set 3 of Back squat", worded as the row's other controls). It MUST NOT log, change or open the set, and the row's existing controls keep working as before.
- **FR-013**: The action MUST be offered and work the same on a locked session. It is the only control of a locked row that takes focus (see Departures, spec 101).
- **FR-014**: Choosing the action MUST open the client's conversation with their trainer, as on the Messages tab, with an About chip for that set in the composer and the focus in the message field. Nothing is sent until the client sends.
- **FR-015**: The composer's chip MUST show what the sent chip will show (FR-018) and MUST have a labelled control to remove it. The message sent while the chip is there carries the reference; after a successful hand-over to sending, the chip is gone from the composer.
- **FR-016**: A message with a reference MUST still need text (spec 58, FR-003). Send stays disabled while the field is empty.
- **FR-017**: Opening the conversation this way MUST NOT lose the client's place: closing it returns to the same session with open entry panels and a running rest timer as they were, and unsent sets are neither lost nor sent twice.

**About chip** (task #175)

- **FR-018**: In the client's and in the trainer's conversation view, a message with a reference MUST show an About chip directly above its text, on the same side as the message. It reads the label "About" and then the session's name, the session's date, the exercise's name, the set number and the RPE. The label is text; the chip is not told apart by colour alone.
- **FR-019**: The chip MUST be a link. On the client's side it opens the logger on that session (spec 101, FR-015), with that set's row brought into view and marked. On the trainer's side it opens the client's Logs tab with that session chosen and that set's row brought into view and marked.
- **FR-020**: The trainer's Logs tab MUST accept an address that names a session and a set, so the chip's link, a reload and a shared link show the same session and set. Without such an address the tab behaves as before.
- **FR-021**: The mark on the set's row MUST NOT rely on colour alone, and the keyboard focus MUST land on or at that row, so the next Tab continues from it.
- **FR-022**: The chip MUST show on a message in every delivery state: Sending, Sent and Not sent.
- **FR-023**: The chip MUST be part of what assistive technology reads for the message: a link whose name holds "About" and the chip's whole content. New messages with a chip are announced as any new message, without moving focus.
- **FR-024**: A message's run with its neighbours (spec 58: one time under the last message of a run) MUST be unchanged by a chip.

**Quality**

- **FR-025**: Every new text MUST exist in English and Polish, the refusal of FR-002 included. The session's date on the chip follows the UI language.
- **FR-026**: Everything new MUST be built from Kinetiq tokens and shadcn components, in light and dark, with visible focus: the client's screens at 375 px, with no sideways scroll at 375 px and 320 px; the trainer's at 1280 px.

### Public endpoints

None. Every operation of this story requires a signed-in client or trainer.

### Key Entities

- **About reference**: The one logged set a message is about. Optional, at most one per message, chosen by the sender from their own set logs and fixed when the message is stored. It stores which set; what the chip shows is read from that set as it is now.
- **Message**: Unchanged from spec `58-client-trainer-messaging`, except that it may carry an About reference.
- **Set log**: Unchanged from spec `92-session-logging`. It is never deleted, so a reference to it stays valid.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A client goes from a logged set to a sent question about it in under 20 seconds, with one tap before typing and one after.
- **SC-002**: A trainer goes from a message with a chip to the set it is about with one click, and can tell which session, exercise and set it is without opening anything.
- **SC-003**: In tests, 100% of attempts to reference a set that is not the sender's own, including every attempt by a trainer, are answered "not found" in the same way as an unknown set, and store no message.
- **SC-004**: After a Retry, a message with a reference is in the conversation exactly once, with the same reference, in every test of the failure cases.
- **SC-005**: The chip and the set its link opens never disagree: in tests, the RPE on the chip equals the set's RPE after a change.
- **SC-006**: A client who asks during a rest returns to the logger with the rest timer still counting and no set lost, in every test.
- **SC-007**: On a locked session every logged set can be asked about; no other control of its rows can be reached with the keyboard.
- **SC-008**: Every new text exists in English and Polish, and switching language changes all of them.
- **SC-009**: The new action, the composer chip and the message chip pass WCAG 2.2 AA checks for contrast, names, focus order, focus visibility and target size, in light and dark, at 375 px (client) and 1280 px (trainer).
- **SC-010**: No error response of this story contains server-internal text.

## Departures from the story, the boards and earlier specs

| Source | It says | This spec | Why |
|---|---|---|---|
| Board "Client · Session logger" | One "Ask Marek about this" link per exercise card, in the card's footer | One action per logged set, on its row, labelled "Ask your trainer" | The story and the chip are about one set ("set 3 · RPE 9"); a link per exercise cannot say which set. The logger does not know the trainer's name, and the story's wording needs none. |
| Board "Client · Session logger" | The link leaves the logger for the Messages screen | The conversation opens without losing the client's place in the session | A client asks between sets. Leaving the logger would drop the rest timer and need several taps to come back. |
| Board "Client · Messages" and "Trainer · Messages" | The chip sits above a message written by the trainer | Only a client's message can carry a chip | Task #173: only the sender's own set can be referenced, and a trainer has no sets. |
| Board "Trainer · Client logs", spec 104 Departures | A message icon on each set row of the trainer's table, left to story #172 | Not built | The story is the client's ("As a client, I want to ask…"), and task #173 excludes it. A trainer starting a message about a client's set needs its own rule for whose set may be referenced, so it is a new task if wanted. |
| Story #172, design system 6.7 | The chip shows session, exercise, set and RPE | It also shows the session's date | "Works for locked sessions too" means a client can ask about last week's "Lower body"; without the date the chip cannot be told from this week's. |
| Spec 101, FR-009 and SC-004 | On a locked session no control of the set rows takes focus | "Ask your trainer" on a logged row does | Task #174: the action works for locked sessions too. It changes nothing in the session. |
| Spec 58, Assumptions | The "About …" cards are left to a later story | Built here | This is that story. |

## Texts

The main English and Polish texts. Polish uses informal "ty" and should be confirmed by a copywriter, as the design system says (8.5).

| Where | English | Polish |
|---|---|---|
| Action on a logged set: an icon button, this is its accessible name | Ask your trainer about set {number} of {exercise} | Zapytaj trenera o serię {number}: {exercise} |
| Chip label | About | Dotyczy |
| Chip content | {session} · {date} · {exercise}, set {number} · RPE {rpe} | {session} · {date} · {exercise}, seria {number} · RPE {rpe} |
| Chip link, accessible name | About {session}, {date}, {exercise}, set {number}, RPE {rpe}. Open this set | Dotyczy: {session}, {date}, {exercise}, seria {number}, RPE {rpe}. Otwórz tę serię |
| Remove the chip from the composer | Remove the set from this message | Usuń serię z tej wiadomości |
| Refused reference | This set can't be attached. Remove it and send the message again. | Nie można dołączyć tej serii. Usuń ją i wyślij wiadomość ponownie. |

## Assumptions

- The story is a "Could": the smallest version that meets both acceptance criteria is built, and everything under "Not in this story" stays out.
- The chip shows the set as it is now, not as it was when the client asked. A set can change only until its session locks (spec 101), the link leads to the current set anyway, and a chip that disagrees with the set it opens would confuse both people. This is the same choice as the session summary ("as its sets are now").
- Only a set the server has stored can be referenced, because the server checks that the set is the sender's. A log still on the phone becomes askable once it is Synced.
- The action lives in the logger only: today's session, and an earlier session opened from the Plan tab, locked or not. The session summary, the Plan tab and Progress do not offer it.
- The conversation the action opens is the client's one conversation (spec 58, FR-001). A client always has a trainer, so it can always open.
- "Brought into view and marked" for the linked set is a visible, non-colour mark on its row that lasts while the page is open; the plan chooses its exact form from the design system.
- The refusal of FR-002 cannot happen through the screens, because the action is offered only on the client's own stored sets. Its text exists for the case of a stale screen and to honour the rule that every code has a translation.
- Not in this story: a trainer starting a message about a client's set, a chip on a trainer's message, replying to or quoting a message, references to anything other than a set (a session, an exercise, a plan change, a check-in), more than one reference per message, a message without text, showing load or repetitions on the chip, finding all messages about a set from the set's row, and any change to unread counts, the conversation list or notifications.
- Dependencies: messaging (story #58), the logger (story #92), an earlier session at its own address and the locked logger (story #101), the offline log states (story #114) and the trainer's Logs tab (story #104) are all on `master`.
- Task order and commits: #173 the reference on a message, sending and reading it, with the regenerated API types, the demo message and the `CONTEXT.md` term; #174 the action in the logger and the chip in the composer; #175 the chip on messages in both views, both links and the address of the trainer's Logs tab. One commit each in one pull request for the story.
