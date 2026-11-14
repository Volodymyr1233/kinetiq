# Feature Specification: Trainer and client get in-app notifications

**Feature Branch**: `feat/150-notifications`

**Created**: 2026-11-14

**Status**: Draft

**Input**: Story #150 "Trainer and client get in-app notifications" with tasks #151 (notification model and API), #152 (notifications from feature events), #153 (notifications on the event stream), #154 (bell, side sheet and toasts), #155 (approvals count badge) and #296 (notify the trainer when a client deletes their account). Screen: Kinetiq Screens, "Client · Today recovery day" (the notifications sheet). Builds on spec `147-live-messages` (the per-sign-in event stream), spec `85-draft-review` (Approvals), spec `137-plan-change-decision`, spec `117-recovery-day-targets`, spec `58-client-trainer-messaging` and spec `167-data-protection`.

## Clarifications

### Session 2026-11-14

- Q: Which notifications does each role get? → A: Client: new plan, plan changed by the trainer, plan change waiting, the trainer's decision on a plan change, new message. Trainer: new draft to review, plan change waiting, the client's decision on a plan change, a client's recovery day, new message, a client deleted their account.
- Q: Does a person get notified about their own action? → A: No (spec `137-plan-change-decision`, scenario 6).
- Q: Is the notification list kept for ever? → A: The side sheet shows the latest 30; older ones stay stored and are erased with the account.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Told when something needs me (Priority: P1)

A trainer or client is told, inside the app, when something needs them: each event of the lists above raises one notification for the person it concerns.

**Why this priority**: This is the point of the story; every other part shows or delivers these notifications.

**Independent Test**: Cause each of the eleven events once and read the recipient's notifications: exactly one of the right type, none for the person who acted.

**Acceptance Scenarios**:

1. **Given** a trainer publishes a client's plan, **Then** the client gets "new plan".
2. **Given** a trainer saves a change to an already published plan, **Then** the client gets "plan changed by the trainer".
3. **Given** a client's draft is built, **Then** the trainer gets "new draft to review".
4. **Given** a plan change is created, **Then** both the trainer and the client get "plan change waiting".
5. **Given** the trainer decides on a plan change, **Then** the client gets "trainer's decision"; **given** the client decides, **Then** the trainer gets "client's decision"; the person who decided gets nothing.
6. **Given** a client's check-in leads to a recovery day, **Then** the trainer gets "client's recovery day".
7. **Given** a message is sent, **Then** its recipient gets "new message" and the sender gets none.
8. **Given** a client deletes their account, **Then** their trainer gets "a client deleted their account", once, with no name, no email and no link to the client.
9. **Given** the event's own change is rolled back, **Then** no notification remains.

---

### User Story 2 - Own notifications only (Priority: P1)

A person sees and changes only their own notifications; another person's answer "not found".

**Independent Test**: As person A list notifications: only A's. As person B mark one of A's read: 404 and A's is still unread.

**Acceptance Scenarios**:

1. **Given** notifications of two people, **When** one lists, **Then** they get only theirs, newest first, at most 30, with the unread count of all theirs.
2. **Given** another person's notification, **When** it is marked read, **Then** the answer is 404 and nothing changes.
3. **Given** no sign-in, **Then** the standard unauthenticated answer.
4. **Given** a notification that is already read, **When** it is marked read again, **Then** nothing changes (harmless).

---

### User Story 3 - See them: bell, sheet and toast (Priority: P1)

A bell in the top bar of both areas shows the unread count. It opens a side sheet with the list. A toast appears when a notification arrives while the app is open. Opening an item marks it read and goes to the screen it is about.

**Independent Test**: With two unread notifications the bell says 2; open the sheet, choose one: it is read, the count says 1 and the matching screen opens.

**Acceptance Scenarios**:

1. **Given** unread notifications, **Then** the bell shows their count (99+ above 99) and its accessible name says it.
2. **Given** the sheet is open, **Then** it lists the latest 30, unread ones marked, each with its text and when it arrived; with none, an empty message.
3. **Given** an item is chosen, **Then** it is marked read and the app goes to its screen: plan, messages, Approvals or the client's page; "a client deleted their account" goes nowhere.
4. **Given** a notification arrives while the app is open, **Then** a toast shows its text and the bell count rises without a reload.
5. **Given** the UI is Polish, **Then** every text and date is Polish.

---

### User Story 4 - Arrives live (Priority: P1)

A new notification reaches the recipient's open tabs on the signed-in event stream of spec 147, without a reload.

**Independent Test**: With a stream open as the recipient, raise a notification: the stream receives a `notification` event; another person's stream does not.

**Acceptance Scenarios**:

1. **Given** an open stream of the recipient, **When** a notification is stored and its transaction commits, **Then** that stream receives event `notification`; streams of other people receive nothing.
2. **Given** a stream that cannot be written, **Then** it is closed and the request that raised the notification is unaffected.

---

### User Story 5 - Approvals count updates live (Priority: P2)

The count badge on Approvals (drafts and plan changes waiting) updates when a notification arrives.

**Independent Test**: With the Approvals item showing 1, a "new draft to review" arrives: it shows 2 without a reload.

**Acceptance Scenarios**:

1. **Given** the trainer's area is open, **When** a notification arrives, **Then** the Approvals count is read again.

---

### User Story 6 - A deleted client leaves no trace (Priority: P1)

Deleting a client erases every notification that points to them and the notification of their deletion holds nothing of them.

**Independent Test**: A client with notifications for them and about them deletes the account: no notification row refers to the client; the trainer's deletion notice has no client reference.

**Acceptance Scenarios**:

1. **Given** a client, **When** they delete their account, **Then** every notification addressed to them or about them is erased with the account.
2. **Given** the deletion, **Then** the trainer gets the notice once, holding the trainer, the type and the time only.

---

### Edge Cases

- A trainer with no clients and a client with no trainer yet get no events of the other role.
- Two events of the same type for the same subject are two notifications; nothing is merged.
- A sign-out ends the stream as in spec 147; the list is read again on the next load.
- A notification about a client never shows the client's data once the client is gone.
- A failed request shows the usual translated message and "Try again".

## Requirements *(mandatory)*

### Functional Requirements

**Notifications** (#151, #152, #296)

- **FR-001**: A notification MUST hold its recipient, its type, the client it is about when there is one, when it was created and whether it is read.
- **FR-002**: The types MUST be: new plan, plan changed, plan change waiting, plan change decision, new message (client); new draft, plan change waiting, plan change decision, recovery day, new message, client deleted (trainer).
- **FR-003**: Each event of User Story 1 MUST create one notification for its recipient, in the transaction of the change that caused it, and none for the person who acted.
- **FR-004**: A person MUST be able to list their latest 30 notifications with the unread count and mark one read; another person's answers 404, and no request names the person.
- **FR-005**: A notification about a client MUST be erased with the client's account; the "client deleted" notification MUST hold no client reference.
- **FR-006**: The test of data erasure of story #167 MUST cover notifications.

**Live and screens** (#153, #154, #155)

- **FR-007**: After the transaction that stored a notification commits, the recipient's open streams MUST receive event `notification`; no other person's stream does.
- **FR-008**: Both areas MUST show a bell with the unread count, a side sheet listing the latest 30 and a toast for a notification that arrives while open.
- **FR-009**: Choosing an item MUST mark it read and go to the screen of its type.
- **FR-010**: The Approvals count MUST be read again when a notification arrives.
- **FR-011**: The screens MUST be built from Kinetiq tokens and shadcn components, designed at 375 px (client) and 1280 px (trainer), in light and dark, with visible focus, labels and keyboard use; no sideways scroll at 375 px and 320 px.
- **FR-012**: Every user-facing text MUST exist in English and Polish, with Polish plural forms.
- **FR-013**: Demo data MUST let notifications be seen in both areas.

### Public endpoints

None. Every operation needs a signed-in person.

### Key Entities

- **Notification**: A notice to one person: recipient, type, optional client, created time, read flag. Erased with the recipient and with the client it is about.

## Success Criteria *(mandatory)*

- **SC-001**: A recipient sees a new notification within 2 seconds of the event, without a reload, while the app is open.
- **SC-002**: In tests, each of the eleven notification types is raised once and only for the right person, and none for the person who acted.
- **SC-003**: No request returns or changes another person's notification, proven for list and mark-read.
- **SC-004**: After a client's deletion no notification refers to them, proven in the erasure test.
- **SC-005**: The bell and sheet pass WCAG 2.2 AA checks in both themes at 375 px and 1280 px.
- **SC-006**: Every text exists in English and Polish.

## Departures from the story and the board

| Source | It says | This spec | Why |
|---|---|---|---|
| Story #150 | "Notifications arrive on the event stream" | They ride the stream of spec 147 as event `notification` | One stream per sign-in already exists; a second would double the connections. |
| Board | Notification text names the client | The list shows the client's current name, read when listed, never stored | A stored name would outlive the client's deletion. |

## Assumptions

- The recipient of "plan change waiting" are both the trainer and the client of the plan change, as both can decide (spec `137-plan-change-decision`).
- "New message" notifies the recipient only; opening the conversation does not erase it, the person marks it read by choosing it.
- The stream already carries no data about notifications; the screen asks the list again when told.
- The side sheet and bell are built once in `features/notifications` and mounted in the client and trainer areas.
