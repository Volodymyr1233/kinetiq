# Feature Specification: Client and trainer exchange messages

**Feature Branch**: `feat/58-client-trainer-messaging`

**Created**: 2026-11-02

**Status**: Draft

**Input**: Story #58 "Client and trainer exchange messages" and its tasks #59 (conversation and message model), #60 (send, list and mark-read API), #61 (client conversation screen) and #62 (trainer messages screen). Screens: Kinetiq Screens, boards Client → "Messages" and Trainer → "Messages". Design: the Kinetiq design system. The boards guide the design; where a better result for the person using it is found while building, the build wins and the difference is listed under Assumptions.

## Clarifications

### Session 2026-11-02

- Q: Does the trainer's list show every linked client, or only clients with at least one message? → A: Every linked client. Clients with no messages come last, ordered by name, and show "No messages yet". The trainer can write first.
- Q: Does the trainer's list have a search? → A: Yes. It filters the list by the client's first name and surname only, never by message text.
- Q: Is the number of messages a person can send limited? → A: No. The 2000-character limit and signed-in access are the only limits in this story; a rate limit can be added later without changing the screens.
- Q: What does the trainer's Messages page show when it opens? → A: The list, and a prompt to choose a conversation. No conversation opens by itself, so nothing is marked read until the trainer opens one.
- Q: Does a message that arrives in a conversation open in a hidden browser tab count as read? → A: No. It is marked read only while the tab is visible; in a hidden tab it stays unread until the tab becomes visible again.
- Q: Is the count in the navigation the number of unread messages or of conversations with unread messages? → A: The number of unread messages, summed over all conversations.
- Q: What do Enter and Shift+Enter do in the composers? → A: On both screens Enter sends and Shift+Enter adds a line break.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Client writes to their trainer and sees the answer (Priority: P1)

A client opens Messages on their phone and lands directly in the conversation with their trainer. They read what the trainer wrote, type a message and send it. The message shows whether it is being sent, was sent, or could not be sent, and a failed message can be sent again without typing it again.

**Why this priority**: This is the reason for the story: a question about a set, a missed day or a sore knee reaches the trainer without another app.

**Independent Test**: Sign in as a client with a trainer, open Messages, send a message, and see it as Sent. Repeat with the network cut: the message shows Not sent with its text intact; restore the network, choose Retry, and it shows Sent exactly once.

**Acceptance Scenarios**:

1. **Given** a client whose trainer has written, **When** the client opens Messages, **Then** they see the conversation with the trainer's name above it, messages in time order with the newest at the bottom, and the view is scrolled to the newest message.
2. **Given** a conversation with no messages, **When** the client opens Messages, **Then** they see a short invitation to write to their trainer, by name, and the composer.
3. **Given** the composer is empty or has only spaces, **When** the client looks at Send, **Then** it is disabled.
4. **Given** the client types a message and sends it, **When** the message has not been confirmed yet, **Then** it appears at once at the bottom, marked "Sending…", and the composer is empty and ready for the next message. **When** it is confirmed, **Then** the mark changes to "Sent" with the time.
5. **Given** a message could not be delivered, **When** the failure is known, **Then** the message stays in the conversation with its full text, marked "Not sent" with a Retry button. **When** the client chooses Retry, **Then** it shows "Sending…" and then "Sent", and the trainer receives it once, even if the first attempt had in fact arrived.
6. **Given** the client sent several messages in a row, **When** they are all Sent, **Then** only the last one of the run shows its time, as on the board.
7. **Given** the trainer wrote a new message while the client has Messages open, **When** a short time passes, **Then** it appears without a reload.
8. **Given** a conversation with more messages than fit one screen, **When** the client scrolls to the top, **Then** they can load earlier messages, and the view stays on the message they were reading.
9. **Given** any message, **When** it is shown, **Then** nothing indicates whether the other person has read it. There are no read ticks.

---

### User Story 2 - Trainer sees every conversation and answers (Priority: P1)

A trainer opens Messages on a desktop and sees all their conversations in a list, the ones with something new marked with a count. They choose a client and read and answer in the same screen.

**Why this priority**: A trainer works with many clients. Without one list showing who wrote, they would open fourteen conversations to find two.

**Independent Test**: Sign in as a trainer with three clients, two of whom have written. The list shows those two with counts, newest first. Open one, answer it, and see the answer as Sent.

**Acceptance Scenarios**:

1. **Given** a trainer with clients, **When** they open Messages, **Then** the list shows one row per client with their name, the beginning of the last message ("You: " in front when the trainer wrote it), when it was sent and, if there are unread messages, their count.
2. **Given** the list, **When** it is shown, **Then** conversations are ordered by their last message, newest first. Clients with no messages come last, ordered by name, and show "No messages yet" instead of a message.
3. **Given** the list, **When** the trainer chooses a conversation, **Then** it opens beside the list with the client's name, their goal and plan week, a link to their profile, the messages and the composer. The conversation has its own address, so a reload or a shared link opens the same one.
4. **Given** the trainer has opened Messages, **When** no conversation is chosen yet, **Then** the conversation area asks them to choose one and no conversation is marked as read.
5. **Given** the trainer types in the composer, **When** they press Enter, **Then** the message is sent. **When** they press Shift+Enter, **Then** a new line is added. A note under the composer says so. The client's composer behaves the same (FR-020).
6. **Given** the trainer has no clients, **When** they open Messages, **Then** they see that there are no conversations yet and a link to invite a client.
7. **Given** a message from a client arrives while the trainer has Messages open, **When** a short time passes, **Then** the list and the open conversation update without a reload.
8. **Given** a message the trainer sent fails, **When** it is shown, **Then** it behaves as in Story 1: Not sent, text kept, Retry.
9. **Given** the list has a search field, **When** the trainer types part of a client's first name or surname, **Then** the list shows only the matching clients, still in the same order and with their counts. **When** nothing matches, **Then** the list says no client matches. **When** the field is cleared, **Then** every client is back. The search never looks at message text.
10. **Given** a conversation is open and the search hides its row, **When** the trainer keeps writing, **Then** the conversation stays open.

---

### User Story 3 - Unread counts say what is new (Priority: P1)

Both people see how many messages from the other person they have not seen. The count sits on the Messages entry of the navigation, so they know before opening it. Opening a conversation clears it.

**Why this priority**: Acceptance criterion of the story. Without it, nobody knows a message is waiting, and the conversation only works when both people happen to look.

**Independent Test**: The trainer sends a client two messages. The client's Messages tab shows 2. The client opens the conversation and the count disappears. The trainer's own view of the conversation never counts their own messages.

**Acceptance Scenarios**:

1. **Given** the other person sent messages after I last opened the conversation, **When** I look at the navigation, **Then** Messages shows how many.
2. **Given** I wrote the most recent messages, **When** I look at the navigation, **Then** they do not count: only messages from the other person do.
3. **Given** the client has unread messages, **When** they open the conversation, **Then** the count clears and the tab shows no count.
4. **Given** a trainer with unread messages from two clients, **When** they look at the navigation, **Then** Messages shows the total of both. **When** they open one client's conversation, **Then** that client's row shows no count and the total drops by that count.
5. **Given** I have a conversation open and visible and the other person writes, **When** the message appears, **Then** it does not stay counted as unread, because I see it. **When** the tab is hidden, **Then** it stays unread until the tab becomes visible again.
6. **Given** a count above 99, **When** it is shown, **Then** it reads "99+".
7. **Given** unread messages, **When** a screen reader reaches the navigation entry or a list row, **Then** the count is part of its announced name ("Messages, 2 unread").
8. **Given** the unread count changes because the other person wrote, **When** a short time passes, **Then** the navigation shows the new count without a reload.

---

### User Story 4 - Only the two people see the conversation (Priority: P1)

A conversation belongs to one client and the trainer they train under. Nobody else can read it, write to it or find out that it exists.

**Why this priority**: Acceptance criterion of the story. Messages can hold health details (an injury, a sleepless night), so a leak is worse than a missing feature.

**Independent Test**: With two trainers and their clients, every attempt to read, write, mark read or count messages of a conversation that is not yours fails the same way as for one that does not exist.

**Acceptance Scenarios**:

1. **Given** a client, **When** they use Messages, **Then** they can reach only the conversation with their own trainer.
2. **Given** a trainer, **When** they use Messages, **Then** they can reach only conversations with clients linked to them.
3. **Given** a person who is not one of the two, **When** they try to read, send to or mark read a conversation, **Then** the answer is "not found", the same as for a conversation that does not exist.
4. **Given** a signed-out visitor, **When** they try any messaging operation, **Then** they are refused as signed out.
5. **Given** an empty message or one over the limit, **When** it is sent, **Then** it is refused with a stable code the screen turns into a translated message. No server text is shown.
6. **Given** a trainer who stops being linked to a client, **When** the trainer opens Messages, **Then** that client's conversation is gone from their list and cannot be opened.

---

### Edge Cases

- Message text is shown as plain text. Links are not made clickable, and markup typed by a person is shown as typed, never run.
- Line breaks inside a message are kept and shown. Spaces and line breaks around the whole message are not kept. A message of only spaces or line breaks counts as empty.
- The limit is 2000 characters. The composer accepts no more, typed or pasted, and the server counts characters the same way, so text the screen accepts is never refused for length.
- Sending twice with a double tap or Enter held down sends one message per distinct press of Send, but a Retry never duplicates a message that already arrived.
- A message that is Sending when the page is closed or reloaded is not kept in the browser. If it had reached the server it is in the conversation after the reload; if not, it is gone. A Not sent message is kept only while the page stays open.
- Messages sent from two devices of the same account appear on both after a short time, in the order the server received them.
- The messages of one conversation are shown in the order the server received them, not by the clocks of the devices.
- A very long unbroken word or address wraps inside the bubble and never makes the page scroll sideways.
- Messages show times in the viewer's own time zone. A new day starts a day separator ("Today", "Yesterday", then the date).
- If loading the conversation or the list fails, the screen shows a translated message and "Try again". A failure in the unread count never blocks the rest of the page: the count is hidden.
- A client whose quiz is not submitted or whose plan is not published is routed by the entry rules of story #50 and does not reach Messages before that; this story adds no rule for it.

## Requirements *(mandatory)*

### Functional Requirements

**Conversation and messages** (task #59)

- **FR-001**: There MUST be exactly one conversation for each client and the trainer they train under. It exists from the moment either of them needs it; nobody creates or deletes it.
- **FR-002**: A message MUST belong to one conversation and carry its sender (the client or the trainer), its text and the time the server received it. A message is text only: no images, files, emoji pickers, reactions, edits or deletion.
- **FR-003**: Message text MUST be 1 to 2000 characters after surrounding spaces and line breaks are removed.
- **FR-004**: Each person in a conversation MUST have a last-read time: the moment they last opened it. A new conversation starts with none, so every message from the other person is unread.
- **FR-005**: The unread count of a conversation for one person MUST be the number of messages from the other person received after that person's last-read time. A person's own messages never count.
- **FR-006**: The total unread count of a person MUST be the sum of the unread counts of all their conversations. For a client it is the count of their one conversation.
- **FR-007**: Marking a conversation as read MUST set the person's last-read time to now. It MUST NOT move the time backwards, and it MUST NOT touch the other person's last-read time.
- **FR-008**: Whether the other person has read a message MUST NOT be exposed to the sender anywhere.

**Operations** (task #60)

- **FR-009**: A signed-in person MUST be able to send a message to their conversation, read a page of its messages, mark it read, and read their unread counts.
- **FR-010**: Reading a conversation MUST return the newest messages first page by page, so earlier messages can be loaded on demand. A page holds at most 50 messages. Within the view they appear oldest to newest.
- **FR-011**: Sending MUST be safe to repeat: every attempt of one message carries the same identity, and the server stores it once, whichever attempt arrives first. Repeating it answers as the first attempt did.
- **FR-012**: An empty or too long message MUST be refused with a stable machine-readable code for each case. The screens translate by that code and never show server text.
- **FR-013**: Every operation MUST require a signed-in person. A request that changes anything MUST be protected against forged requests, as every other change in the app.
- **FR-014**: Every operation MUST act on the conversation of the signed-in person only: a client's own, or a trainer's with one of their clients. Any other conversation, including one that does not exist, answers "not found" and a test proves it. The id in the address is checked against the signed-in person on every call.
- **FR-015**: A response MUST carry the other person's name and, for each message, whether the signed-in person wrote it, and nothing else about either: no email, and nothing about anyone else.
- **FR-016**: Message text MUST NOT be written to logs and MUST NOT be sent to any LLM, in line with the rule that nothing identifying a person leaves the system.

**Client screen** (task #61)

- **FR-017**: The client's Messages tab MUST open straight into the conversation with their trainer, with the trainer's name and the words "Your trainer" above it. The board's one-row inbox is not shown, because a client has no other conversation.
- **FR-018**: The composer MUST sit at the bottom, within reach of the thumb, and stay visible when the on-screen keyboard is open. It has a labelled field that grows with its text up to a few lines, and a Send button with an accessible name.
- **FR-019**: A message the client sends MUST appear immediately as "Sending…", become "Sent · HH:mm" when confirmed, or become "Not sent · Retry" when not. The text of a Not sent message is never lost, and Retry sends exactly that text.
- **FR-020**: In the composer Enter MUST send the message and Shift+Enter MUST add a line break, the same as on the trainer screen. The Send button sends too. Enter that only confirms text being composed by an input method does not send.
- **FR-021**: The Messages tab MUST show the client's unread count, and opening the conversation MUST mark it read.

**Trainer screen** (task #62)

- **FR-022**: The trainer's Messages page MUST show a list of conversations beside the selected conversation, as on the board, at a width of 1280 px. On a narrow window it shows one at a time, with a way back to the list.
- **FR-023**: The list rows MUST show the client's initials and name, the beginning of the last message, the time of the last message and the unread count as defined in User Story 2.
- **FR-024**: The list MUST have a labelled search field that filters the rows by the client's first name and surname, and by nothing else. Matching ignores letter case and Polish diacritics, so "zielinski" finds "Zieliński". The filter applies to the list the trainer already has, as they type, and does not change which conversation is open or any unread count.
- **FR-025**: The selected conversation MUST show the same message states, retry and day separators as the client screen. Enter sends; Shift+Enter adds a line (FR-020). A note under the composer says so.
- **FR-026**: Opening a conversation MUST mark it read, and so must a message that arrives in the conversation while it is open and its tab is visible. A message that arrives while the tab is hidden stays unread until the tab becomes visible.
- **FR-027**: The trainer's Messages entry in the sidebar MUST show their total unread count.

**Quality** (both screens)

- **FR-028**: The messages area MUST announce new messages to a screen reader without moving the keyboard focus, and the whole feature MUST be usable with the keyboard alone, with visible focus. Touch targets on the client screen are at least 44 px.
- **FR-029**: Bubbles, states and counts MUST NOT rely on colour alone: Sending, Sent and Not sent are words, and my messages and theirs also differ by side.
- **FR-030**: New messages MUST reach the other person's open screen and navigation count within 15 seconds without a reload.
- **FR-031**: Every text MUST exist in English and Polish, the failure messages included. Times and dates follow the UI language.
- **FR-032**: The screens MUST be built from Kinetiq tokens and shadcn components, and work in light and dark.
- **FR-033**: New entities MUST come with demo data in the local-only location: conversations between the demo trainer and demo clients, some with unread messages.

### Public endpoints

None. Every endpoint of this story requires a signed-in client or trainer.

### Key Entities

- **Conversation**: All messages between one client and their trainer. One per pair. It holds each of the two people's last-read time.
- **Message**: A piece of text sent by one of the two people in a conversation. It has its text, its sender, the time the server received it and an identity that makes repeated attempts safe.
- **Last-read time**: When one person last opened a conversation. With the message times it defines Unread.
- **Delivery state**: What the sender sees on their own message: Sending, Sent or Not sent. Only the sender's own view has one, and it is not stored: a message the server holds is Sent.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A client goes from the tab bar to a sent message in under 15 seconds.
- **SC-002**: A trainer with 14 clients finds which of them have written, and opens the first one, in under 10 seconds.
- **SC-003**: A message sent by one person appears on the other's open screen, and in their navigation count, within 15 seconds.
- **SC-004**: A message that failed to send is never lost and never duplicated: after Retry it is in the conversation exactly once, in every test of the failure cases.
- **SC-005**: In tests, 100% of attempts by a person who is not part of a conversation to read, write, mark read or count it are answered "not found", and 100% by a signed-out visitor are refused as signed out.
- **SC-006**: The unread count always equals the number of the other person's messages after my last-read time, and falls to 0 after I open the conversation.
- **SC-007**: Every text exists in English and Polish. Switching language changes all of them, failure messages and times included.
- **SC-008**: Both screens pass WCAG 2.2 AA checks for contrast, labels, focus and focus visibility, in light and dark, at 375 px (client) and 1280 px (trainer).

## Assumptions

- The story fixes: one conversation per pair, text only, three delivery states with Retry and no read ticks, the trainer's list with unread counts, a total on Messages, and access for the two people only. Everything else below is a product default chosen here.
- The maximum length is 2000 characters: long enough for a detailed question about a session, short enough that nobody writes an essay in a coaching tool. It is a single value in one place.
- A page is 50 messages, loaded newest first, with "Load earlier messages" at the top of the conversation. The last page being reached is shown by the button disappearing.
- New messages arrive by the open screen asking again every few seconds, and the navigation count the same way, not by a push connection. This meets the 15-second target and needs no new moving part. Push can replace it later without changing what the screens show.
- Every client has a trainer (the account rules require it), so a conversation can exist for every client. A conversation without messages is not stored until someone sends the first message; for a trainer, every linked client still appears in the list.
- A Not sent message lives only in the browser while the page is open, in its place at the bottom of the conversation. It is not stored on the server, because it never arrived. Closing the page loses it; the board's behaviour is the same.
- A message is "Sent" the moment the server confirms it, as the board does with its short delay. Marking it as read by the other person is out of scope by the story: there are no read ticks.
- Messages are never edited or deleted, by anyone. The product has no account deletion yet, so what happens to messages then is not decided here.
- Unlinking a client from a trainer does not exist yet. If it does, the old conversation stays stored and neither person reaches it (User Story 4, scenario 6). Nothing in this story creates that situation.
- Differences from the board, chosen for the person using it, all small and visible in the review:
  - The client sees the conversation directly, without the one-row inbox, and keeps the tab bar available. The board's back arrow therefore has nothing to go back to and is dropped.
  - The "About …" cards that link a message to a set and the grey system lines such as "Plan change declined · Schedule" belong to the logging and plan-change stories. They are not part of this story, and the conversation leaves room for them without them being built.
  - The bell with a red dot in the trainer's top bar belongs to a notification story. The count on the Messages entry and on the list rows is what this story provides.
  - On opening Messages the trainer's page asks them to choose a conversation instead of opening the top one, so a trainer glancing at the page does not clear counts they have not seen. Narrow windows show only the list at first, for the same reason.
  - Both composers allow several lines: Enter sends, Shift+Enter adds a line. The board's client field is a single line, which cannot hold a longer question.
  - The trainer's header shows the client's name, goal and plan week as on the board (plans landed on `master` during this story; the week is hidden for a draft and once the plan has finished), and links to the client's profile page (story #35).
- Times use the viewer's own time zone. The "week" rules of the plan do not apply to messages.
- Not in this story: push notifications and email alerts for new messages, attachments, searching inside message text, limiting how fast messages can be sent, typing indicators, online status, message deletion or editing, blocking or reporting, group conversations, conversations between two trainers or two clients, a conversation with the system itself, and any use of an LLM on messages.
- Dependencies: the trainer–client link and the client list exist (stories #26, #35). The navigation shells of both areas, with placeholder pages for Messages, exist and are replaced by this story. The client's entry rules (story #50) decide when a client can reach Messages at all. The client's profile page of the trainer (story #35) is the target of the "Open profile" link.
- Task order and commits: #59 model, #60 API (with the regenerated API types), #61 client screen, #62 trainer screen. Each is one commit in one pull request, as the workflow describes. Terms **Unread** and **Delivery state** are added to `CONTEXT.md` in the first commit.
