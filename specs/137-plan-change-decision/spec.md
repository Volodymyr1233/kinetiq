# Feature Specification: Trainer and client decide on a plan change

**Feature Branch**: `feat/137-plan-change-decision`

**Created**: 2026-11-11

**Status**: Draft

**Input**: Story #137 "Trainer and client decide on a plan change" with tasks #138 (plan change states and decision API), #139 (decline reason posted into the conversation), #140 (client plan change screen), #141 (trainer plan change list and detail) and #142 (why-this-change chart). One pull request for the story, one commit per task. Screens (Kinetiq Screens): `client-plan-change`, `trainer-approvals`, `trainer-plan-change`. Builds on specs `125-plan-change-rules` (a rule fires), `131-plan-change-content` (a plan change is stored as pending, with checked content; the trainer reads a read-only detail, reached from the client page) and `58-client-trainer-messaging` (conversation). Story #143 completes the flow (an accepted plan change takes effect on its Monday, an undecided one expires, the plan shows the upcoming change); anything that depends on it is noted below and left to it.

## Clarifications

### Session 2026-11-11

- Q: How long do ended plan changes stay in the trainer's "Decided" tab? → A: Those that ended in the last 30 days; older ones stay reachable from the client's page.
- Q: Does the trainer's client page list ended plan changes? → A: Yes: the open one plus the last 5 ended, each linking to its detail.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Either person accepts, and can take the acceptance back (Priority: P1)

A pending plan change waits for two people. Each of them, the trainer and the client, can accept it. The first acceptance moves it to "accepted by one"; when the other accepts too it is "applied" (it takes effect from its effective week, which story #143 enforces). Someone who accepted can undo it, as long as the other person has not accepted yet; after that the plan change is settled.

**Why this priority**: Nothing in a plan changes without both agreeing (constitution IV); this is the core of the story.

**Independent Test**: Create a pending plan change; accept as the client, check "accepted by one" and that the trainer still can act; undo, check "pending"; accept as both, check "applied" and that undo is refused.

**Acceptance Scenarios**:

1. **Given** a pending plan change, **When** the client or the trainer accepts, **Then** it is "accepted by one" and records who accepted.
2. **Given** a plan change accepted by one, **When** the other person accepts, **Then** it is "applied" and both acceptances are recorded.
3. **Given** a plan change accepted by one, **When** the person who accepted undoes it, **Then** it is "pending" again and nobody's acceptance is recorded.
4. **Given** a plan change accepted by one, **When** the person who has not accepted tries to undo, **Then** it is refused: there is nothing of theirs to undo.
5. **Given** an applied plan change, **When** anyone tries to undo, **Then** it is refused: the other person already accepted.
6. **Given** a person who already accepted, **When** they accept again, **Then** nothing changes and the answer is the same state (accepting twice is harmless).
7. **Given** a plan change that is applied, declined or expired, **When** anyone tries to accept or undo, **Then** it is refused with a stable code that says it has ended.
8. **Given** the effective week has already begun and the plan change is still open, **Then** accepting is refused because it is expired (see #143 for how and when it becomes expired).
9. **Given** two people accepting at the same moment, **Then** exactly one ends in "applied" and no acceptance is lost.
10. **Given** a caller who is neither the client of the plan change nor that client's trainer, or a plan change that does not exist, **Then** the answer is "not found", never "forbidden".

---

### User Story 2 - Either person keeps the current plan, with a reason (Priority: P1)

Either person can decline the change by choosing "Keep current plan". It asks for one reason (Too easy, Too hard, Schedule, Injury, Other) and an optional note. The plan change ends as "declined", and the reason and note are posted into the conversation as a message from the person who declined, so the other person reads why.

**Why this priority**: Declining is the other half of deciding, and the conversation is how the reason reaches the other person.

**Independent Test**: As the client, keep the current plan with "Schedule" and a note: the plan change is declined, and the conversation shows one message from the client with the reason and the note, in both the client's and the trainer's view.

**Acceptance Scenarios**:

1. **Given** a pending or accepted-by-one plan change, **When** either person keeps the current plan with a reason, **Then** it is "declined" and records who declined, the reason and the note.
2. **Given** the decline, **Then** exactly one message from the decliner is posted into the conversation of that client and trainer, containing the plan change's title, the reason and the note when there is one.
3. **Given** the message is posted, **Then** it appears, unread, for the other person and, as the decliner's own message, in the decliner's view; the posting happens together with the decline: a decline without its message, or a message without its decline, never exists.
4. **Given** no reason is chosen, **Then** the decline is refused. **Given** a reason outside the five, **Then** it is refused. **Given** a note longer than the message limit, **Then** it is refused.
5. **Given** the reason is "Other", **Then** a note is allowed but not required.
6. **Given** a person accepted earlier and then keeps the current plan, **Then** the plan change is declined and their earlier acceptance no longer matters.
7. **Given** a plan change that has already ended, **When** anyone tries to keep the current plan, **Then** it is refused with a stable code and no message is posted.
8. **Given** the decliner's language is Polish, **Then** the message text is in Polish; the other person reads the same message as posted (a message is not translated afterwards).
9. **Given** the same request is sent twice (a double tap or a retry), **Then** only one message is posted.

---

### User Story 3 - The other person is told about every decision (Priority: P1)

Each decision (accept, undo, keep the current plan) tells the other person. A trainer or client who has not opened the plan change can tell from the app that something is waiting for them or that it has ended.

**Why this priority**: A decision that the other person never notices stalls the plan change until it expires.

**Independent Test**: After each of accept, undo and keep-current, the other person's indicators (see Assumptions) change; the decider's own do not.

**Acceptance Scenarios**:

1. **Given** the plan change is created, **Then** the trainer sees it as waiting for them in Approvals and the client sees it on Plan and Today.
2. **Given** one person accepts, **Then** the other is told that a plan change waits for their decision.
3. **Given** one person undoes their acceptance, **Then** the other is told it is waiting for both again (the earlier notice is withdrawn, not doubled).
4. **Given** one person keeps the current plan, **Then** the other is told the plan change ended, with the message in the conversation as the reason.
5. **Given** both have accepted, **Then** both are told it is applied and from which Monday it takes effect.
6. **Given** a person's own decision, **Then** they are not notified about it.
7. **Given** the notice, **Then** it is carried by in-app indicators derived from the plan change's state (the Approvals count and tabs, the badges on Plan and Today, the plan change's state label, and for a decline the message in the conversation); there is no separate notification list, bell or email in this story, so withdrawing or replacing a notice is automatic.

---

### User Story 4 - The client reads and decides on a plan change (Priority: P1)

On a phone, the client sees an open plan change from Plan and Today, opens it, reads what changes and decides. The "Client · Plan change" screen shows the title, the reason line, the rule's weekly numbers as a small chart, every changed target as old → new with its change, what stays the same, and who has decided what. The actions are "Accept change" (a regular button, not the lime brand one) and "Keep current plan". "Keep current plan" opens a bottom drawer to choose the reason and add a note; after accepting, the screen says "You accepted. Waiting for <trainer>" with "Undo". It also shows the ended states: applied ("You both accepted. Takes effect Mon …"), declined ("You kept the current plan: <reason>. <trainer> sees your reason in your chat.") and expired (the text of story #143).

**Why this priority**: The client is half of the decision and uses the phone.

**Independent Test**: As the client at 375px, open the plan change from Plan, accept it, undo, then keep the current plan with a reason; each state reads correctly in English and Polish, light and dark.

**Acceptance Scenarios**:

1. **Given** a client with an open plan change, **Then** Plan and Today show a card with its title, reason line and effective week that opens the screen.
2. **Given** the screen, **Then** each changed target shows the old value, the new value and the change; unchanged things are summarised in one line; both people's state is shown with words, not colour alone.
3. **Given** the plan change is pending, **Then** the client can accept or keep the current plan; **Given** they accepted and the trainer did not, **Then** they see "waiting for <trainer>" and "Undo"; **Given** it is applied, declined or expired, **Then** there is no action, only the outcome.
4. **Given** the client opens "Keep current plan", **Then** a drawer asks for a reason from the five choices (none preselected) and an optional note; "Keep current plan" stays disabled until a reason is chosen; "Back" closes it without changes.
5. **Given** a request fails (offline, server refuses), **Then** the screen shows a translated message by error code and leaves the state as the server last confirmed it; it never shows backend text.
6. **Given** another client's plan change id, or a plan change of the client's that does not exist, **Then** the screen shows "not found".
7. **Given** the screen, **Then** it is complete in English and Polish, light and dark, at 375px, with labels and keyboard focus for every control (WCAG 2.2 AA); the "Accept change" button is not the lime brand button.

---

### User Story 5 - The trainer sees plan changes in Approvals and decides on the detail (Priority: P1)

Approvals lists plan changes next to the new plans to review (Waiting for you, Waiting for client, Decided). A row shows the client, the title, the reason line, a state badge and when it takes effect or how it ended; it opens the detail. The detail (extended from story #131's read-only one) shows each target old → new, the rule's numbers and both people's state, with the actions "Accept change" and "Keep current plan" (reason dialog), "Undo" after accepting, and the ended states. The client's page shows the open plan change and its state, and under it the last 5 ended plan changes, each linking to its detail.

**Why this priority**: The trainer's side is the other half of the decision, and Approvals is the trainer's inbox.

**Independent Test**: As the trainer at 1280px, find a pending plan change in "Waiting for you", open it, accept, see it move to "Waiting for client" and show "Undo"; undo; keep the current plan with a reason and find it under "Decided".

**Acceptance Scenarios**:

1. **Given** plan changes of a trainer's clients, **Then** Approvals shows them with the tab counts, and "Waiting for you" holds those the trainer has not accepted and that are open, "Waiting for client" those the trainer accepted, "Decided" the applied, declined and expired ones that ended in the last 30 days (a plan change that ended exactly 30 days ago still counts), newest first (older ones are reached from the client's page).
2. **Given** Approvals already shows drafted plans to publish, **Then** both kinds appear in the same list and tabs; a plan to review is "Waiting for you".
3. **Given** the trainer's navigation, **Then** the Approvals count is the number of items waiting for them, and it updates after a decision.
4. **Given** the detail, **Then** it shows the client, title, reason line, state badge, each changed target (now, new, change), the note of what stays the same, both people's state and the effective week.
5. **Given** the trainer accepted and the client did not, **Then** the detail shows "You accepted. Waiting for <client>" with "Undo".
6. **Given** the trainer keeps the current plan, **Then** the dialog asks for the reason (none preselected) and an optional note, and says the client gets the reason.
7. **Given** the plan change is of someone else's client, **Then** "not found".
8. **Given** the screen, **Then** it is complete in English and Polish, light and dark, at 1280px, labelled and keyboard-operable.
9. **Given** a plan change that ended, **Then** the detail is still readable with its outcome (who declined and why; "applied" with the effective Monday; "expired" with the text of story #143).

---

### User Story 6 - "Why this change" shows the rule's numbers against its threshold (Priority: P2)

The detail shows a chart of the numbers that made the rule fire, one point per week, against the rule's threshold: average RPE against 8, the top set of a stalled exercise against the required rise, the share of sessions finished against 70%. Next to it a short "Same weeks" list gives other numbers of the same weeks (sessions done, average RPE, recovery days). The client's screen shows the compact version of the same numbers.

**Why this priority**: A change of load has to be explainable (constitution IV), but a decision can be made without the chart.

**Independent Test**: Open a plan change of each rule: the chart has one point per week of the rule's window, the threshold line, a text alternative that says every value and the threshold, and works in light and dark.

**Acceptance Scenarios**:

1. **Given** each rule, **Then** the chart shows its measured value per week of the window and the rule's threshold as a dashed line, labelled in words.
2. **Given** stalled weight with several stalled exercises, **Then** one chart per exercise.
3. **Given** the chart, **Then** it follows the chart rules of the design system (series colour tokens, threshold line style, legend, no colour-only meaning, text alternative with all values), and it is readable in light and dark.
4. **Given** the numbers, **Then** they are formatted in the viewer's language and weight unit.
5. **Given** a window week without logs, **Then** the chart shows a gap, not zero.

---

### Edge Cases

- One person accepts, the other declines: the plan change is declined; the message is posted by the decliner.
- The client's trainer changes, or the client is removed, while a plan change is open: the old trainer gets "not found", and the plan change stays as it is for story #143 to end.
- A plan change is declined while a message from the same decliner about the same plan change already exists (a retry): no second message.
- The plan was edited by the trainer after the plan change was created: deciding stays allowed; what happens to the content is decided by story #143.
- The effective Monday passes while a decision is being made: the decision is refused as expired.
- A person reads a plan change whose stored language text is missing (an older row): title and reason are built from stored numbers as in story #131, never blank.
- Free text of the note is shown as plain text, never as markup, and is never sent to an LLM by this feature.
- Accept/undo/decline sent by the wrong role endpoint (a client calling the trainer area, or the reverse) is refused by the role rule of the area, before ownership.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: A plan change MUST have exactly the states pending, accepted by one, applied, declined and expired, with these transitions and no others: pending → accepted by one (either person accepts); accepted by one → applied (the other accepts); accepted by one → pending (the one who accepted undoes it); pending or accepted by one → declined (either person keeps the current plan); pending or accepted by one → expired (story #143). Applied, declined and expired are final.
- **FR-002**: The system MUST record which of the two people accepted, so that an undo is available only to someone who accepted and only while the other has not.
- **FR-003**: Accepting twice by the same person MUST leave the plan change as it is (idempotent) and answer with its current state.
- **FR-004**: Any other transition MUST be refused with a stable machine-readable code, without exposing backend text; the code distinguishes at least "already ended", "nothing to undo" and "effective week begun"; a missing or unknown reason, or a note over the limit, is a validation error naming the field ("reason required" is that error on the reason field).
- **FR-005**: Only the client of a plan change and that client's current trainer MUST be able to read or decide it; anyone else, and any unknown id, MUST get "not found". No endpoint is open without authentication.
- **FR-006**: Concurrent decisions on one plan change MUST NOT lose an acceptance or end in an impossible state; one of two simultaneous acceptances makes it applied.
- **FR-007**: Keeping the current plan MUST require one of the reasons Too easy, Too hard, Schedule, Injury or Other, MAY carry a note, and MUST end the plan change as declined.
- **FR-008**: A decline MUST post exactly one message into the conversation of the client and the trainer, sent as the decliner, containing the reason and the note, in the decliner's language, in the same step as the decline; repeating the request MUST NOT post a second one.
- **FR-009**: Each decision MUST tell the other person (and only them) through the channel decided for story #137 (see User Story 3), and the notice MUST be withdrawn or replaced when the situation it described changes.
- **FR-010**: The client MUST see an open plan change from Plan and Today and a screen with the changed targets (old → new), the rule's numbers, both people's state and the actions allowed for the current state.
- **FR-011**: The trainer MUST see plan changes in Approvals, in the same list and tabs as drafted plans, and in the client's page, and a detail with every changed target (old → new), the rule's numbers and both people's state, with the actions allowed for the current state.
- **FR-012**: The detail MUST show a chart of the rule's weekly numbers against its threshold, following the design system's chart rules, with a text alternative.
- **FR-013**: Accept MUST NOT be styled as the lime brand button.
- **FR-014**: Every user-facing string, including the five reasons, the state labels and the posted message, MUST exist in English and Polish; the frontend translates by error code and never shows backend text.
- **FR-015**: The note MUST be stored and shown as plain text, with a length limit equal to the message limit, and MUST NOT be sent to an LLM.
- **FR-016**: Decisions MUST NOT change the plan itself; applying the content from the effective Monday, expiring and showing the upcoming change on the plan belong to story #143.

### Key Entities *(include if feature involves data)*

- **Plan change**: extended with who accepted (trainer, client, or both), who declined, the reason and note, and when it ended (the existing state and effective week stay).
- **Decision**: one person's act on a plan change: accept, undo, or keep the current plan with a reason and note.
- **Decline reason**: one of Too easy, Too hard, Schedule, Injury, Other.
- **Message** (existing): receives the decline text, sent as the decliner.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every transition of the state machine, allowed and refused, is proven by tests, including accept by each person, undo only by the one who accepted, and no change after an end.
- **SC-002**: A person who is not the client or the client's trainer, and an unknown plan change, always get "not found" on every endpoint of this story, proven by tests.
- **SC-003**: After a decline, the reason appears in both people's conversation views as one message, and a repeated request leaves it at one.
- **SC-004**: A client can accept or keep the current plan in at most 3 taps from the Plan or Today screen; a trainer in at most 2 clicks from Approvals.
- **SC-005**: After any decision, the other person's indicators show the new situation on their next load of the app.
- **SC-006**: Both screens pass a check in a real browser in light and dark, English and Polish, at 375px (client) and 1280px (trainer), with keyboard operation and labels, and the chart's text alternative reads every value.

## Assumptions

- Stories #125 and #131 are done: plan changes are detected, stored as pending with checked content, and the trainer already has a read-only detail and a link from the client's page. This story adds deciding to it. The Approvals list already shows drafted plans; plan changes are added to it.
- Story #143 owns: applying the content on the effective Monday, moving a still-open plan change to expired, the text "The week started before you both decided. The plan didn't change.", and the upcoming change on the plan. Here a plan change whose effective week has begun refuses decisions, and the screens can show the expired outcome when it exists.
- A decision's actor is taken from the signed-in account, never from the request body; client and trainer use their own areas, so the role rule applies before ownership.
- The decline reason and note are posted as an ordinary message of the existing conversation, built by the backend from the reason and the note, with the plan change's title; its first line is stable and translated at posting time in the decliner's language.
- Undo is allowed until the other person accepts (as in the issue), so it is not offered on an applied plan change.
- There is no limit on how often someone accepts and undoes before the other person decides.
- The mockups are a guide: the layout may be improved where it serves the person better (for example the bottom drawer on phone, a dialog on desktop), keeping the Kinetiq tokens and shadcn components.
- Demo data is added for the new fields in the local-only Flyway location.
