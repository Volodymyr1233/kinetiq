# Feature Specification: Plan change takes effect or expires

**Feature Branch**: `feat/143-plan-change-effect-expiry`

**Created**: 2026-11-13

**Status**: Draft

**Input**: Story #143 "Plan change takes effect or expires" with tasks #144 (apply or expire at the effective Monday), #145 (upcoming and applied change on the plan screens) and #146 (expired state on both screens). One pull request for the story, one commit per task. Screens (Kinetiq Screens): `client-plan`, `trainer-plan-editor`, `client-plan-change`, `trainer-plan-change`. Builds on specs `125-plan-change-rules`, `131-plan-change-content` (a plan change is stored as pending with checked content) and `137-plan-change-decision` (accept, undo, keep the current plan; decisions are refused once the effective week has begun). This story completes the flow: an accepted plan change changes the plan on time, an undecided one ends clearly, and the plan shows the change before it applies.

## Clarifications

### Session 2026-11-13

- Q: When the plan was edited after the change was suggested, what makes an accepted change still valid on its Monday? → A: Every entry it touches still exists with the old values shown on the plan change, and the result meets the rule's limits and the plan rules; unrelated edits don't matter.
- Q: Before its Monday, what do screens call a change both people accepted? → A: "Applied" (as in `CONTEXT.md`), shown as "Applied · takes effect Mon 12 Oct" before the Monday and "Applied · in effect" after; "In effect" is added to `CONTEXT.md`.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - An accepted change reaches the plan on its Monday (Priority: P1)

A plan change that trainer and client both accepted before its effective week does not alter the plan right away. When the effective Monday begins, the plan starts using the new targets, removed session and moves of the plan change, from that week onward. Earlier weeks keep what they had.

**Why this priority**: This is the point of the whole flow: an agreed change has to reach the client's training, on the day promised, without anyone doing anything.

**Independent Test**: With a fixed clock, create a plan change accepted by both with an effective Monday; before that Monday the plan is unchanged; once the Monday begins, the plan for that week and later weeks has the new values and earlier weeks have the old ones.

**Acceptance Scenarios**:

1. **Given** a plan change accepted by both, **When** its effective Monday begins in the client's time zone, **Then** within 5 minutes the plan uses the new targets from that week onward, and the plan change shows as in effect.
2. **Given** the plan has weeks before the effective week, **Then** those weeks keep their old targets, and a past week never shows new values.
3. **Given** a plan change whose content removes a session or moves exercises, **Then** the removal and moves take effect from the effective week in the same way as target changes.
4. **Given** the run takes place late (the app was down on Monday) or twice for the same week, **Then** the plan ends up with the change applied exactly once, with no doubled effect.
5. **Given** the client lives in another time zone than the trainer, **Then** "Monday" is the Monday of the client's week.
6. **Given** a client's logs of the effective week, **Then** a session already started before the change was applied is not rewritten: its logged sets stay as they were, and the change reaches only what is still to be done.
7. **Given** a plan change that applied, **Then** it records when it took effect, shows the old value, the new value and the reason, and can be reversed by the trainer editing the plan (constitution IV); nothing about it is deleted.

---

### User Story 2 - An undecided change expires, and both people can tell (Priority: P1)

A plan change that is not accepted by both before its effective Monday is expired when that Monday begins. Both people see it with the sentence "The week started before you both decided. The plan didn't change." The plan stays exactly as it was.

**Why this priority**: A plan change that never ends would stay in everyone's lists forever, and a client could assume a change happened that did not.

**Independent Test**: With a fixed clock, leave a plan change pending, and another accepted by one person only; at the effective Monday both become expired, the plan is unchanged, and both screens show the sentence.

**Acceptance Scenarios**:

1. **Given** a pending plan change, **When** its effective Monday begins, **Then** it is expired and the plan did not change.
2. **Given** a plan change accepted by one person only, **When** its effective Monday begins, **Then** it is expired as well; the one acceptance changes nothing.
3. **Given** an expired plan change, **Then** the client's screen and the trainer's detail show "The week started before you both decided. The plan didn't change." in the viewer's language, with no action to take; there is no way to revive it.
4. **Given** an expired plan change, **Then** it leaves the trainer's "Waiting for you" and "Waiting for client" tabs, appears under "Decided" for 30 days (counted from the moment it ended, which for an applied one is the second acceptance), and stops counting in the Approvals count and in the badges on Plan and Today.
5. **Given** a plan change that was declined or already applied, **When** the run happens, **Then** it is left as it is: only open plan changes expire.
6. **Given** the run takes place late or twice, **Then** a plan change expires once, and an acceptance made in the meantime is refused as expired.
7. **Given** a client for whom a plan change expired, **Then** the rules may detect the situation again and create a new plan change later, as story #125 decides; this story does not suppress or create one.

---

### User Story 3 - The plan shows the upcoming change for its week (Priority: P1)

A client or trainer looking at the plan can see what is about to change and for which week. When trainer and client both accepted, the week the change takes effect shows the new values as "takes effect", marked so nobody mistakes them for what is being trained now. While the plan change is still undecided, the plan only says it is waiting, as it already does. After it applied, the plan shows the new values like any other values, with a note that they come from a plan change. Past weeks show the old targets.

**Why this priority**: People plan their week from the plan screen; a change that is hidden until Monday surprises the client and the trainer.

**Independent Test**: With a plan change accepted by both and the effective week still ahead: the client's plan on that week shows the new values marked as upcoming and the previous weeks show the old ones; after the Monday the same week shows them as the plan; past weeks show old targets throughout.

**Acceptance Scenarios**:

1. **Given** a plan change accepted by both and not yet in effect, **When** the client or the trainer opens the plan on the effective week, **Then** each changed target shows its new value, marked as coming from a plan change that takes effect on that Monday, with the old value still readable.
2. **Given** weeks before the effective week, **Then** they show the old targets and no mark.
3. **Given** weeks after the effective week, **Then** they show the new values as upcoming (before it applied) or as the plan (after).
4. **Given** a plan change that is still undecided, **Then** the plan screens show it as waiting for a decision with a link to it, and keep showing the old targets everywhere.
5. **Given** a plan change that removes a session, **Then** the plan marks that session as removed from the effective week, and the days that become free are rest days from that week.
6. **Given** the plan change applied, **Then** the plan shows who changed it and why: a line such as "Changed on Mon 12 Oct by a plan change" linking to the plan change; the existing "changed" date of the plan stays truthful.
7. **Given** the trainer's plan editor, which edits the live plan and has no week selector, **Then** it shows a banner for the upcoming change (old and new values, the Monday) and, once it is in effect, a note "Changed on … by a plan change" linking to it; what the trainer edits for a week in effect stays an ordinary edit of the plan (story #68), never an edit of the plan change.
8. **Given** the screens, **Then** they are complete in English and Polish, light and dark, at 375px (client) and 1280px (trainer), with labels and keyboard focus for every control; the mark is understood without colour alone.

---

### User Story 4 - Applied and expired are visible in the lists the people already use (Priority: P2)

Approvals, the trainer's client page and the client's Plan and Today screens already list plan changes by state (story #137). This story makes the two new endings, applied "in effect" and expired, read correctly there: a row says when it took effect, or that it expired, and the badge, tab and counts follow the state.

**Why this priority**: Without it the lists would show a stale "Waiting" for a change that has already ended.

**Independent Test**: After the run, the trainer's Approvals tab counts and the client's badges drop by the plan changes that ended, and "Decided" lists them with the right label.

**Acceptance Scenarios**:

1. **Given** a plan change that applied, **Then** its row says "Applied · in effect since Mon 12 Oct"; before the Monday it says "Applied · takes effect Mon 12 Oct".
2. **Given** an expired plan change, **Then** its row says "Expired" and the Monday it ended.
3. **Given** the Approvals count and the badges on Plan and Today, **Then** they count only open plan changes, so an expiry lowers them.
4. **Given** the client page of the trainer, **Then** the open plan change and the last 5 ended ones show the new states with their detail links.

---

### Edge Cases

- The plan was edited by the trainer after the plan change was created and before it applies: on the Monday the whole content is checked again. It is still valid when every entry it touches still exists with the old values the plan change shows, and the result meets the limits of its rule and the plan rules; edits to anything else do not matter. If it is still valid it applies completely; if not, nothing is applied (never a part), the plan change ends as expired, and both people read "The plan changed after this change was suggested, so it didn't apply. The plan didn't change." instead of the standard sentence (English and Polish). The trainer's own edits are never undone.
- The plan finished (its last week passed) or was replaced before the effective Monday: nothing is applied and the plan change ends as expired with the same cause and sentence as an edited plan ("plan changed"), because the plan it was made from no longer exists.
- The client's account is deleted before the effective Monday: the plan change is deleted with it. A change of trainer needs no special handling: an accepted plan change still applies when it fits, and the client's current trainer sees it.
- A person accepts at the last moment while the run is starting: exactly one outcome happens; a plan change never ends as both applied and expired, and an acceptance accepted by the server is never lost without a result.
- The effective Monday is a day the client has no session: the change still takes effect on that Monday; the plan simply shows no session on that day.
- A client in a time zone where the Monday has not begun yet while it already has in the trainer's: the client's Monday rules.
- The plan screens are opened while the run is late: they show the state as the server has it; once the run happens they update on the next load.
- Free text of the decline note and any plan change text is shown as plain text, never as markup.
- Another person's plan change or plan: "not found" on every endpoint of this story, never "forbidden".

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST, once the effective Monday has begun in the client's time zone, change the plan of a plan change that both people accepted so that it uses the plan change's new targets, removed session and moves from the effective week onward, and MUST leave every earlier week as it was.
- **FR-002**: The system MUST, at the same moment, expire every plan change that is still open (pending or accepted by one) and leave the plan unchanged.
- **FR-003**: Both outcomes MUST happen at most once per plan change: repeating or delaying the run MUST NOT apply twice, expire twice, or turn an applied plan change into an expired one or the reverse. The run MUST catch up on every plan change whose Monday has passed.
- **FR-004**: Applying MUST NOT rewrite sets a client already logged, and a session already started MUST keep the targets it started with.
- **FR-005**: A plan change that applied MUST record when it took effect; one that expired MUST record when it ended. Both MUST stay readable, with old value, new value and reason (constitution IV), and MUST NOT be deleted.
- **FR-006**: An applied plan change MUST be reversible by the trainer editing the plan; nothing in this story prevents or hides that edit.
- **FR-007**: Applying MUST be decided only by the accepted state and the clock; no model or person is asked at that moment, and no text a person wrote is sent to an LLM (constitution III and IV).
- **FR-008**: The client's and the trainer's plan screens MUST show the plan change accepted by both, before it applies, on its effective week and the weeks after it, with the new value marked as upcoming and the old value readable; undecided plan changes MUST only be shown as waiting, and weeks before the effective week MUST show the old targets.
- **FR-009**: After applying, the plan screens MUST show the new values as the plan, with a note that links to the plan change.
- **FR-010**: Expired plan changes MUST show the sentence "The week started before you both decided. The plan didn't change." on the client's plan change screen and the trainer's detail, in English and Polish, with no action offered.
- **FR-011**: Approvals tabs and counts, and the badges on Plan and Today, MUST count only open plan changes; applied and expired ones MUST appear as ended with their date, under "Decided" for 30 days.
- **FR-012**: A decision on a plan change that ended MUST be refused with a stable machine-readable code (story #137); the frontend MUST translate by code and never show backend text.
- **FR-013**: Only the client of a plan change and that client's trainer MUST be able to read the new states and upcoming values; anyone else, and any unknown id, MUST get "not found". No endpoint is open without authentication, and the run is not reachable from the outside.
- **FR-014**: Every user-facing string, including the upcoming mark, the applied note, the row labels and the expired sentence, MUST exist in English and Polish.
- **FR-015**: The run MUST work from a clock that tests can fix, so both outcomes can be proven at an exact moment.
- **FR-016**: When the plan changed after the plan change was created, the system MUST re-check the whole content against the current plan on the Monday (every touched entry still exists with the old values shown, result within the rule's limits and the plan rules), apply it only if it is still valid, otherwise apply nothing and end the plan change as expired with the "plan was edited" sentence; it MUST NOT apply content that would break the plan rules of its limits (sets, repetitions or seconds, load, RPE and rest of an entry, 2 to 6 sessions, one session per day).

### Key Entities *(include if feature involves data)*

- **Plan change** (existing): gains the ends "in effect" (applied and past its Monday) and expired, each with the moment it happened; its states otherwise stay as in story #137.
- **Plan** (existing): receives the new targets, removed session and moves from the effective week; earlier weeks are untouched; keeps its history so the change and its reason stay traceable.
- **Upcoming change on a plan week**: what a plan screen shows for a week from a plan change accepted by both: the changed targets with old and new values and the Monday it takes effect. Derived from the plan change, never stored separately.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: With a fixed clock, a plan change accepted by both changes the plan on its Monday and not before, and an undecided one expires on that Monday and leaves the plan identical, both proven by tests.
- **SC-002**: Running the weekly step twice, or late, gives the same result as running it once, proven by tests.
- **SC-003**: Past weeks and started sessions keep their old targets after a plan change applies, proven by tests.
- **SC-004**: A client or trainer can tell, in the plan screen alone, what changes and from which Monday, without opening the plan change; the expired sentence is shown word for word in English and Polish.
- **SC-005**: A person who is not the client or the client's trainer always gets "not found" on every endpoint of this story, proven by tests.
- **SC-006**: Both screens pass a check in a real browser in light and dark, English and Polish, at 375px (client) and 1280px (trainer), with keyboard operation and labels; the backend result is checked against the real database after an applied and an expired run.

## Assumptions

- Stories #125, #131 and #137 are done: plan changes are detected and stored with checked content; both people can accept, undo, or keep the current plan; decisions are refused once the effective week begins. This story adds the end of the life of a plan change.
- "The weekly run" is a step the system performs by itself at the start of each client's Monday, checked regularly rather than at one global moment, so a late or repeated run is harmless (FR-003).
- A plan change accepted by both is already "applied" in the sense of story #137 before its Monday; this story adds the moment the plan really changes. The user-facing label stays "Applied" and always carries the timing ("takes effect Mon 12 Oct" before the Monday, "in effect" after), so nobody reads it as "already in my plan". "In effect" is added to `CONTEXT.md`.
- An expired plan change is never revived; a new one comes only from the rules.
- Nobody is sent an email or push notice about applying or expiring: the signals are the in-app indicators already decided in story #137 (Approvals count and tabs, badges on Plan and Today, state labels), which derive from the state.
- The mockups are a guide: for example the upcoming mark and the "changed by a plan change" note may be adjusted to fit the real plan layout, keeping the Kinetiq tokens and shadcn components.
- Demo data for the new moments (took effect, expired) is added in the local-only Flyway location.
