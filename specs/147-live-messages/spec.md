# Feature Specification: Messages arrive without reloading

**Feature Branch**: `feat/147-live-messages`

**Created**: 2026-11-11

**Status**: Draft

**Input**: Story #147 "Messages arrive without reloading" and its tasks #148 (event stream per signed-in person: one server-sent event stream for the signed-in person, new messages published to the recipient's stream; works through the development proxy and with the sign-in cookie; an integration test receives an event) and #149 (frontend subscription: subscribe once per sign-in and update conversations and counts; reconnects after the backend restarts; the comment on #149 asks that the waiting state's message sheet at `/app/quiz/waiting`, which sits outside the client area, is live too). Screens: Kinetiq Screens, boards "Client · Messages" and "Trainer · Messages". The boards add nothing new for this story: it changes when things appear, not how they look. Builds on spec `58-client-trainer-messaging` (conversations, messages, unread counts, mark read, both conversation screens), spec `172-ask-about-set` (About reference) and spec `247-waiting-trainer-draft` (the message sheet on the waiting state). Every place this spec departs from the story, the boards or an earlier spec is listed under "Departures".

## Clarifications

### Session 2026-11-11

- Q: Should the server limit how many streams one sign-in holds open at once? → A: Yes, at most 10 per sign-in; opening an 11th closes the oldest.
- Q: While the stream is down, do screens fall back to the fixed refreshes of spec 58? → A: No. The catch-up after every reconnect is the only recovery.
- Q: Does a hidden tab keep its stream open? → A: Yes. It stays open while hidden; a browser that freezes or drops it is covered by the reconnect and catch-up.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A message appears on the other person's screen at once (Priority: P1)

A client asks their trainer a question while the trainer has Messages open on the desktop. The trainer's list moves that client to the top with a count, and, if the conversation is open, the message appears in it, within a second or two and without anyone reloading. The same holds the other way round, on the client's phone, and on the waiting state where a new client talks to their trainer before their plan is published.

**Why this priority**: This is the story. Spec 58 refreshes every 5 to 10 seconds, so a conversation feels like email, and every open screen asks the server again and again even when nothing happened.

**Independent Test**: Sign in as a trainer in one browser and as one of their clients in another. With the conversation open on both, send a message from each side: it appears on the other side in under 2 seconds. Watch the network: while nothing is sent, neither screen asks the server for messages again.

**Acceptance Scenarios**:

1. **Given** the trainer has a client's conversation open and visible, **When** the client sends a message, **Then** it appears at the bottom of the trainer's conversation within 2 seconds, with the client's name and time as any other message, and it is marked read as spec 58 FR-026 says (not counted, no unread separator).
2. **Given** the trainer has Messages open with another conversation selected, **When** a client sends a message, **Then** that client's row moves to the top with the beginning of the message, its time and its unread count, within 2 seconds.
3. **Given** the client has Messages open, **When** the trainer sends a message, **Then** it appears in the client's conversation within 2 seconds, the same way.
4. **Given** a client on the waiting state with the message sheet open, **When** the trainer writes, **Then** the message appears in the sheet within 2 seconds. The waiting state shows no unread count (spec 247), so with the sheet closed nothing changes on screen until it is opened, and then it shows the message.
5. **Given** a message carries an About reference, **When** it arrives live, **Then** it shows its About chip, as when the conversation is loaded.
6. **Given** a conversation is open and nothing is sent, **When** a minute passes, **Then** the screen has not asked the server for its messages, the list or the count again.
7. **Given** a tab is hidden, **When** a message arrives, **Then** the conversation, list and count are up to date when the tab is shown again, and the message stays unread until then (spec 58 FR-026).

---

### User Story 2 - Unread counts follow the conversation everywhere (Priority: P1)

The Messages count in the navigation changes as soon as the other person writes, wherever the person is in the app: on Today, in a session, in the Plan, in a client's profile. It clears as soon as the conversation is opened, on every device of that person, not only the one they opened it on.

**Why this priority**: Acceptance criterion of the story. A count that lags by 10 seconds, or stays on the laptop after the phone read the message, says something that is no longer true.

**Independent Test**: Sign in as a client on two browsers, one on Today and one on Plan. The trainer sends two messages: both show 2 on the Messages tab within 2 seconds. Open the conversation in one browser: the other shows no count within 2 seconds.

**Acceptance Scenarios**:

1. **Given** a person anywhere in their area, **When** the other person sends a message, **Then** the Messages count in their navigation goes up by one within 2 seconds.
2. **Given** a person signed in on two devices, **When** they open a conversation with unread messages on one, **Then** the count of that conversation and the total in the navigation clear on the other within 2 seconds.
3. **Given** a person signed in on two devices, **When** they send a message from one, **Then** it appears in the conversation on the other within 2 seconds, as Sent, with no Sending or Not sent state there.
4. **Given** a person opens a conversation, **When** their own last-read time moves, **Then** the other person is told nothing: their screen does not change, and nothing on the stream says the message was read (spec 58 FR-008, no read receipts).
5. **Given** a person's own message, **When** it arrives on their other device, **Then** it does not count as unread there.

---

### User Story 3 - The live connection repairs itself (Priority: P1)

Connections drop: the phone goes through a tunnel, the laptop sleeps, the server restarts after a release. The person does nothing. When the connection comes back, the app catches up on everything that happened while it was gone and is live again.

**Why this priority**: Acceptance criterion of the story ("reconnecting by itself") and of task #149 ("reconnects after the backend restarts"). A live screen that silently stops being live is worse than a screen that refreshes slowly, because the person trusts it.

**Independent Test**: With the trainer's Messages open, stop the backend, send nothing, start it again. Within 30 seconds of the backend being back, a message from the client appears live. Then cut the trainer's network, have the client send a message, and restore the network: the message is on the trainer's screen within 30 seconds of the network returning, with its unread count.

**Acceptance Scenarios**:

1. **Given** the connection is lost (server restart, network loss, laptop sleep), **When** the server can be reached again, **Then** the app connects again by itself within 30 seconds, without a reload and without anything the person has to press.
2. **Given** messages were sent or conversations read while the connection was down, **When** it connects again, **Then** the conversations, the open conversation and the counts show them, as after a reload.
3. **Given** the server stays unreachable, **When** the app keeps trying, **Then** it waits longer between attempts, up to 30 seconds, and never more than one attempt at a time from one tab.
4. **Given** the connection is down, **When** the person sends a message, **Then** sending works and behaves as spec 58 describes (Sending, Sent, Not sent with Retry). Sending never depends on the live connection.
5. **Given** the connection is down, **When** the person looks at the screen, **Then** nothing on screen says so. The screens look as they do today; the delivery state of their own messages is the only state shown.
6. **Given** the sign-in has ended (signed out elsewhere by a password reset, or 14 days without use), **When** the app tries to connect, **Then** it is refused as signed out, the app stops trying and behaves as it does for any signed-out answer.

---

### User Story 4 - Only the two people of a conversation hear about it (Priority: P1)

The live connection carries news about the person's own conversations only. Nobody can listen to another person's conversations, and nobody hears more than the conversation screens already show them.

**Why this priority**: Messages can hold health details. A connection that stays open for hours is a new way in, so it has to be held to the same rules as every other part of messaging.

**Independent Test**: With two trainers and their clients connected, every message and every mark read reaches only the streams of the sign-ins it is about. A signed-out request for a stream is refused. A stream whose sign-in is ended stops receiving within a minute.

**Acceptance Scenarios**:

1. **Given** a message between a client and their trainer, **When** it is sent, **Then** only the sign-ins of those two people are told. Another trainer, another client and a signed-out visitor receive nothing.
2. **Given** a signed-out visitor, **When** they ask for a stream, **Then** they are refused as signed out.
3. **Given** a person's stream, **When** they are told about a message, **Then** what they are told holds nothing that the conversation screens would not show them: no email, nothing about another conversation, no other person's last-read time.
4. **Given** a sign-in is ended by a sign-out, a password reset or its 14 days without use, **When** a message is sent after that, **Then** no stream of that sign-in receives it later than 1 minute after the sign-in ended.
5. **Given** a stream that is open for hours, **When** nothing is sent, **Then** it carries nothing but what keeps it open, and message text never appears in server logs.

---

### Edge Cases

- **Several tabs**: each open tab of the app holds its own connection. All tabs of all sign-ins of one person are told, so every tab stays current. Closing a tab ends its connection. A sign-in with more than 10 tabs open loses the stream of its oldest one (FR-001). The closed tab is told to wait 30 seconds before it reconnects, so tabs pushing each other out do so at most once per 30 seconds each, not in a tight loop; with that many tabs, this is accepted.
- **Event and refresh crossing**: a live message for a conversation that a page load is fetching at the same moment appears once, not twice, and in the order the server received it (spec 58).
- **Own message on this device**: the live notice of a message this tab just sent does not show it a second time and does not undo its Sending → Sent change.
- **Many messages in a burst**: ten messages sent in quick succession all appear, in order, and the screen fetches no more than it needs to show them (not ten full reloads of every page of the conversation).
- **Conversation not loaded**: a message for a conversation the person has not opened in this tab updates only the list and counts; the conversation is loaded when it is opened, as today.
- **A trainer with a new client**: a client who joins through an invite is not a message; the trainer's list shows them when the list is next loaded, as today. The stream is not used for it.
- **Signed out in this tab**: signing out closes the connection at once, and a new sign-in in the same tab, as a different person, never receives events of the previous person.
- **Server restart during a conversation**: covered by Story 3. Nothing sent during the outage is lost: it was stored, and the catch-up on reconnect shows it.
- **Phone in the background**: a phone browser may freeze a background tab and drop its connection. When the person returns, Story 3 catches up; the person sees current messages and counts.
- **Proxies and buffering**: the stream reaches the browser through the development proxy and through the production setup without being held back, so a notice arrives as soon as it is sent, not when a buffer fills.
- **Idle connections**: a connection with nothing to say for a long time is not closed by the browser, the proxy or the server for being idle, and a connection that is silently dead is noticed and replaced within a minute.

## Requirements *(mandatory)*

### Functional Requirements

**Event stream** (task #148)

- **FR-001**: A signed-in client or trainer MUST be able to open a stream of events about their own conversations. Each open tab holds one stream; one person may hold several (several tabs, several devices). One sign-in MUST hold at most 10 open streams: opening an 11th closes the oldest of that sign-in, telling it to wait 30 seconds before it reconnects; its tab, if it still exists, then reconnects by FR-015.
- **FR-002**: When a message is stored, both people of its conversation MUST be told on every stream of every one of their sign-ins: the recipient so the message and count appear, the sender so their other tabs and devices show it.
- **FR-003**: When a person marks a conversation read, every stream of that person MUST be told, so their counts clear everywhere. The other person of the conversation MUST NOT be told (no read receipts, spec 58 FR-008).
- **FR-004**: An event MUST identify which conversation changed and what happened (a new message, or read by me). It MUST NOT carry anything the conversation operations would not show the same person, and MUST NOT carry any other person's last-read time. Sending a message again with the same id (spec 58 FR-011) MUST NOT tell anyone a second time.
- **FR-005**: The stream MUST require a signed-in person and MUST be refused as signed out otherwise. Opening it changes nothing, so it needs no forgery protection; it is the only new endpoint and it is not public.
- **FR-006**: A stream MUST receive events only for the sign-in that opened it. When that sign-in ends (sign-out, password reset, 14 days without use), the stream MUST stop receiving within 1 minute and be closed.
- **FR-007**: The server MUST send something on an idle stream at least every 30 seconds, so idle connections stay open through the browser and proxies and a dead one is noticed.
- **FR-008**: Events MUST reach the browser as soon as they are sent, through the development proxy and in production (one image serving the app and the API), never held back by buffering or compression.
- **FR-009**: Sending a message and marking read MUST succeed whether or not anyone is listening, and a failure to tell a stream MUST NOT fail or slow the send or the mark read.
- **FR-010**: Message text MUST NOT be written to logs by the stream, in line with spec 58 FR-016.

**Subscription** (task #149)

- **FR-011**: The app MUST open one stream per tab while a client or trainer is signed in, from the moment either area or the waiting state is shown, and keep it while the person moves between screens. It MUST NOT open one per screen, and MUST NOT open one for a signed-out visitor.
- **FR-012**: On a new-message event the app MUST bring up to date what the person can see of that conversation: the conversation list, the unread count and, if the conversation is loaded in this tab, its newest messages. It MUST NOT reload earlier pages for it.
- **FR-013**: On a read event the app MUST bring the list and the unread count up to date.
- **FR-014**: A message this tab is sending MUST keep its delivery state as spec 58 describes: the event of its own message does not duplicate it or turn it back to Sending.
- **FR-015**: When the stream drops, the app MUST connect again by itself, first within a few seconds, then waiting longer between attempts up to 30 seconds, with at most one attempt at a time per tab. After every reconnect it MUST bring the list, the count and any loaded conversation up to date, as after a reload.
- **FR-016**: When connecting is refused as signed out, the app MUST stop trying and react as to any signed-out answer. On sign-out the app MUST close the stream at once.
- **FR-017**: The fixed refreshes of spec 58 (every 5 seconds for an open conversation and the list, every 10 seconds for the count) MUST be removed: the stream replaces them, with no fallback refresh while it is down. A screen still loads its data when it opens. While the stream has been heard from in the last 35 seconds (it pings every 25), nothing MUST ask for the list, the count or the messages again by itself: not coming back to the tab, not the network coming back, not another screen showing them, not a send or a mark read, whose own event refreshes them. Each is asked once when it is first shown, and again only after an event or a reconnect says it changed (FR-015, FR-022). When the stream has gone quiet, as after a phone froze the hidden tab, an open conversation is asked for again on coming back, so the unread separator follows what is unread now (spec 58 FR-034).
- **FR-022**: A tab MUST keep its stream open while it is hidden, so its counts stay current; it does not close the stream on hiding.
- **FR-018**: A message arriving while the conversation is open and its tab is visible MUST be marked read as spec 58 FR-026 says; in a hidden tab it stays unread until the tab is shown. The unread separator rules of spec 58 FR-034 are unchanged.
- **FR-019**: The waiting state's message sheet (spec 247) MUST be live in the same way as the client's Messages tab.

**Quality**

- **FR-020**: Live messages MUST be announced to a screen reader as new messages are today (spec 58 FR-028), without moving the keyboard focus or the scroll position of someone reading earlier messages.
- **FR-021**: The story adds no user-facing text. If one is added while building, it exists in English and Polish.

### Public endpoints

None. The stream requires a signed-in client or trainer.

### Key Entities

- **Event stream**: One open connection from one tab of one sign-in, over which the server tells that tab what changed in the person's conversations. Held only in the server's memory while open; nothing about it is stored.
- **Event**: A notice on a stream that one conversation changed: a new message in it, or it was read by this person. It says which conversation; the app asks for what it then shows.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A message sent by one person appears on the other's open conversation and in their navigation count in under 2 seconds, in 95% of attempts on a working connection (spec 58 SC-003 required 15 seconds).
- **SC-002**: Opening a conversation on one device clears its count on the person's other devices in under 2 seconds.
- **SC-003**: After the server restarts, or a network returns, every open tab is live again within 30 seconds, and shows every message sent in the meantime, with no action by the person.
- **SC-004**: With a conversation open and nothing sent for 10 minutes, the app makes no requests for messages, conversations or counts (only the open stream stays).
- **SC-005**: In tests, 100% of events reach only the sign-ins of the two people of the conversation (and, for a read event, only the reader's), and 100% of signed-out stream requests are refused.
- **SC-006**: No stream receives an event later than 1 minute after its sign-in ended.
- **SC-007**: A message appears exactly once in the conversation, in the order the server received it, in every test where an event and a page load cross.
- **SC-008**: No sign-in ever holds more than 10 open streams; in tests, opening an 11th closes the oldest and the other 10 keep receiving.

## Assumptions

- **One server**: the app runs as one server process (one image, docs/adr/0001). Open streams are held in that process's memory, and a message sent through it reaches every stream it holds. Running several server processes would need the events shared between them; that is not part of this story.
- **Events are notices, not copies**: an event says which conversation changed; the app then asks for that conversation's newest page, the list and the count through the operations that already exist. Every rule of spec 58 (access, names, About chips in the person's language, "mine") therefore applies unchanged, and nothing about messages is written twice.
- **Missed events are not replayed**: an event sent while a tab was disconnected is lost; the catch-up after every reconnect (FR-015) shows the result instead. This makes a stored event log unnecessary.
- **An open app keeps the sign-in alive as before**: spec 58's refreshes counted as use every 5 to 10 seconds. With the stream, each stream ends after at most 30 minutes and the tab reconnects by itself, and that reconnect counts as use, so an open app keeps its sign-in alive. A sign-in still ends after 14 days without an open app, and then Story 3 scenario 6 applies.
- **Hidden tabs**: the stream stays open in a hidden tab, so a returning person sees current data at once; a browser that freezes the tab drops it, and the reconnect catches up.
- **Nothing on screen shows the connection**: the boards show no connection indicator, and spec 58 shows none for its refreshes. Adding one would put a technical worry in front of people who cannot act on it; sending works regardless (Story 3 scenario 4).
- **Tab count**: each tab holds one long-lived connection, and the server holds at most 10 per sign-in (FR-001), so one runaway tab cannot fill the server's memory. A person with very many tabs open in one browser may also reach the browser's own limit on connections to one site; this is accepted.
- **No fallback refresh**: while the stream is down, nothing refreshes on a timer. If the server cannot be reached, a refresh would fail as well; when it can, the reconnect and catch-up (FR-015) bring everything up to date within 30 seconds.
- **Departures**:
  - From the story: "one stream per signed-in person" is read as one stream per tab of each sign-in, all of them told. One stream for every tab of a person would need tabs to share a connection, which the browser does not give for free.
  - From the story: the sender's own other sign-ins are told too, and a person's mark read is sent to their own sign-ins. The story names only new messages to the recipient; without these, a second device would still show messages and counts that are out of date until it reloads, which the story's "counts update without a reload" excludes.
  - From spec 58: its Assumption "New messages arrive by the open screen asking again every few seconds ... Push can replace it later" is replaced here, and its refresh intervals are removed (FR-017). SC-003 of spec 58 (15 seconds) is tightened to SC-001 above.
  - From task #149: the subscription covers the waiting state as well as both areas, as its comment asks.
- **Not in this story**: a connection indicator, push notifications outside the open app, email alerts, typing indicators, online status, read receipts, live updates for anything other than conversations (plan changes, approvals, client statuses), and live updates of the trainer's list when a new client joins.
- **Dependencies**: story #58 (messages, unread counts, mark read, both screens; #60, #61 and #62 are closed), story #172 (About reference), story #247 (the waiting state's message sheet), sign-in and sign-out (story #18), and the password reset that ends every sign-in of an account.
- **Task order and commits**: #148 stream and publishing, with the regenerated API types; #149 subscription in the app, removing the fixed refreshes. Each is one commit in one pull request for the story, as the workflow describes. Terms **Event stream** and **Event** are added to `CONTEXT.md` only if they end up in user-facing copy; they are technical and stay out of the domain vocabulary otherwise.
