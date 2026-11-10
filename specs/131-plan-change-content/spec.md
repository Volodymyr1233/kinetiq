# Feature Specification: Plan change content is drafted and checked

**Feature Branch**: `feat/131-plan-change-content`

**Created**: 2026-11-10

**Status**: Draft

**Input**: Story #131 "Plan change content is drafted and checked" with tasks #132 (plan change model and migration), #133 (plan-change drafter port and LLM adapter), #134 (limits per rule), #135 (fixed transformation per rule) and #136 (reason line and title). One pull request for the story, one commit per task. Screen: Trainer · plan change detail (Kinetiq Screens, `trainer-plan-change`). Decision: `docs/adr/0002-plan-changes-rules-decide-llm-drafts.md`. Builds on spec `125-plan-change-rules` (story #125, done: a rule fires and hands over a due plan change) and on specs `68-plan-editor`, `75-client-plan` and `79-plan-drafting` (plans, and the LLM drafting precedent). Two later stories complete the flow: #137 (trainer and client accept, undo or keep the current plan; Approvals queue; client screen) and #143 (an accepted plan change takes effect, an undecided one expires, the plan shows the upcoming change).

## Clarifications

### Session 2026-11-10

- Q: Should this story include the read-only Trainer · plan change detail screen, or stay backend only? → A: Backend plus a read-only trainer detail screen. Accept, keep current plan, the Approvals queue and the client screen belong to story #137.
- Q: How does a trainer reach the detail before the Approvals queue exists? → A: The trainer's client page shows the client's open plan change with a link to its detail.
- Q: Must the fixed fallback obey the rule's percentage limits (e.g. reps × 0.9 with at least −1 can exceed −15%)? → A: No. The fallback follows its own formula and the plan rules only.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A due plan change becomes a stored plan change (Priority: P1)

When a rule fires for a client, the system creates one plan change for that client: the rule, the numbers that made it fire, the targets that change with their old and new values, and the effective week, which is the Monday after the plan change was created. It is stored with the content already drafted and checked, so a trainer only has to decide.

**Why this priority**: Without a stored plan change the rules of story #125 produce nothing a person can see; every other story of this feature builds on it.

**Independent Test**: With a rule firing for a client, run the check and read the stored plan change: it holds the rule, its numbers, each changed target with old and new value, and an effective week that is the Monday after the day it was created in the client's time zone.

**Acceptance Scenarios**:

1. **Given** a rule fires for a client, **When** the weekly check runs, **Then** exactly one plan change is stored for that client, holding the rule, its numbers, the changed targets (old and new) and the effective week.
2. **Given** a plan change is created on any day of a week, **Then** its effective week starts on the following Monday in the client's time zone.
3. **Given** the same weekly check runs twice, **Then** only one plan change exists for the client.
4. **Given** a client already has a plan change that has not ended, **Then** no second one is stored.
5. **Given** the content cannot be completed for any reason, **When** the check ends, **Then** no partial plan change exists and the client is checked again on the next run.
6. **Given** the effective week would begin after the last week of the client's plan, **Then** no plan change is created, because there is nothing left to change.
7. **Given** a plan change is stored, **Then** it belongs to one client and is read only by that client and their trainer; anyone else gets "not found".

---

### User Story 2 - The LLM drafts the content, without identifying the client (Priority: P1)

The LLM receives the rule, an anonymised copy of the plan and the logged numbers, and answers with the proposed changes. Nothing that identifies a person is sent: no name, email or id.

**Why this priority**: Choosing which targets move is the reason the team wants an LLM (ADR 0002); sending personal data would break the constitution (principle III).

**Independent Test**: Capture the prompt for a client whose name, email and ids are known: none of them appears in it, and the exercises are referred to by keys that mean something only inside that prompt.

**Acceptance Scenarios**:

1. **Given** a due plan change, **When** the content is requested, **Then** the prompt holds the rule and its numbers, the plan's sessions with exercises and targets, and weekly aggregates of the logs, and no name, email, id or free text a person wrote.
2. **Given** the LLM answers within the limits of the rule, **Then** its answer becomes the content of the plan change.
3. **Given** the LLM is unavailable, times out, answers with something unreadable, or answers outside the limits, **Then** that counts as a failed attempt (see User Story 4).
4. **Given** the LLM names an exercise, session or target that is not in the plan, **Then** the proposed content is rejected as outside the plan rules.

---

### User Story 3 - Each rule limits what the content may change (Priority: P1)

Proposed content is accepted only if it stays inside the limits of its rule and inside the plan rules. Anything else it touches is a violation.

**Why this priority**: Training load affects injury risk (constitution IV): the content has to pass domain bounds before anyone sees it.

**Independent Test**: For each rule, feed content exactly on and just beyond each limit: those on the limit pass, those beyond fail.

**Acceptance Scenarios**:

1. **High RPE** — **Given** a high RPE plan change, **Then** only the sets or the repetitions (or seconds) of exercises whose mean RPE over the last 2 weeks is above 8 may change; the volume (sets × repetitions or seconds) of each entry of those exercises drops by 5% to 15%; load and RPE of every entry stay as they are. Content that changes another exercise, another target, or lowers an entry's volume by less than 5% or more than 15%, fails.
2. **Stalled weight** — **Given** a stalled weight plan change, **Then** only the load of the stalled exercises may rise, each by 2.5% to 15%, rounded to 0.5 kg; their repetitions may drop by up to 2 (never below 1); everything else stays. Content that raises an unlisted exercise, lowers a load, raises a load outside 2.5%–15%, uses a load that is not a multiple of 0.5 kg, or drops repetitions by more than 2, fails.
3. **Low completion** — **Given** a low completion plan change, **Then** exactly one session is removed and at least 2 sessions remain; up to 2 exercises of the removed session may move to other sessions; no session ends with more than 8 exercises. Content that removes none or more than one session, leaves fewer than 2, moves more than 2 exercises, moves exercises that were not in the removed session, or leaves a session with more than 8 exercises, fails.
4. **Plan rules** — **Given** any proposed content, **Then** the resulting plan still obeys the plan rules (sets, repetitions or seconds, load, RPE and rest within the limits of an exercise entry; 2 to 6 sessions; one session per day).
5. **Given** any proposed content, **Then** a target the content leaves unchanged is recorded as unchanged, and the changed ones keep their old values for display.
6. **Given** a rounding edge (a rise that lands on exactly 2.5%, or one that rounds below 2.5% or above 15% after rounding to 0.5 kg), **Then** the limit is judged on the rounded value.

---

### User Story 4 - A failing LLM answer is replaced by the rule's fixed transformation (Priority: P1)

The system asks the LLM up to twice. Content that breaks its limits or the plan rules twice (an unavailable or unreadable answer counts as a break) is replaced by a fixed transformation of the rule, so a rule that fires always produces a plan change, even when the model fails.

**Why this priority**: The trainer must always get a plan change when a rule fired (ADR 0002); the fallback is what makes that true.

**Independent Test**: Make the drafter fail twice for each rule and compare the stored plan change with the fixed transformation computed by hand, including rounding edges.

**Acceptance Scenarios**:

1. **High RPE fallback** — **Given** the fallback of high RPE, **Then** for every exercise with a mean RPE above 8 the repetitions (or seconds) become the old value × 0.9, rounded to a whole number, lowered by at least 1 and never below 1; sets, load and RPE stay.
2. **Stalled weight fallback** — **Given** the fallback of stalled weight, **Then** the load of each stalled exercise becomes the old load + 5%, rounded up to the next 0.5 kg; nothing else changes.
3. **Low completion fallback** — **Given** the fallback of low completion, **Then** the session finished least often over the window is removed, and when several tie, the one on the latest weekday; no exercise moves.
4. **Given** the first answer is invalid and the second is valid, **Then** the second is used and the fallback is not.
5. **Given** the first answer is valid, **Then** the model is asked once.
6. **Given** the fallback result, **Then** it follows only its own fixed formula and the plan rules (at least 1 repetition, load a multiple of 0.5 kg within the entry limits); the percentage limits of the rule do not apply to it (for example 5 repetitions → 4 is −20%, and a 1 kg load rounds up to 1.5 kg). A plan change in which the formula changes nothing is not created.
7. **Given** two fallbacks computed twice from the same input, **Then** both give the same plan change (deterministic).

---

### User Story 5 - The reason line and title say what changed in plain words (Priority: P2)

Each plan change has a title and a reason line: the rule in plain words with its number, in English and Polish. Examples of the wording: title "Cut leg volume by 10%", reason "Average RPE was above 8 for 2 weeks"; title "Raise back squat load by 10%", reason "Back squat rose less than 5% in 3 weeks"; title "Cut to 2 sessions a week", reason "50% of sessions were finished in 2 weeks".

**Why this priority**: Every change made by the system must show the reason (constitution IV), but the content works without polished wording.

**Independent Test**: For each rule, build the title and reason from known numbers and compare with the expected English and Polish text.

**Acceptance Scenarios**:

1. **Given** any plan change, **Then** its reason line states the rule's condition and the number measured (mean RPE, rise in percent, share of sessions finished).
2. **Given** any plan change, **Then** its title says what changes in the plan and by how much (the change of the total volume or load of the changed exercises, in whole percent; the number of sessions a week after the change).
3. **Given** a plan change affects one exercise, **Then** the title names it; **Given** it affects several, **Then** the title counts them.
4. **Given** the person reading uses Polish, **Then** title and reason are in Polish; otherwise in English. Wording never assumes a client's gender.
5. **Given** any title or reason, **Then** it contains no name or email of a person.

---

### User Story 6 - The trainer reads the plan change detail (Priority: P2)

The trainer opens a plan change and sees, as in the Kinetiq Screens: the client, the title, the reason line, a table of every changed target (exercise, session, now, new, change), the note of what stays the same, the rule's numbers and the weeks behind it ("Why this change", "Same weeks"). In this story it is read only: accepting, keeping the current plan and the Approvals queue belong to story #137.

**Why this priority**: The story's screen is "Trainer · plan change detail"; the content has to be visible to be checked end to end.

**Independent Test**: Open a stored plan change of each rule as its trainer: the detail shows the old and new value of every changed target, the reason and the rule's numbers.

**Acceptance Scenarios**:

1. **Given** a plan change of a trainer's client, **When** the trainer opens it, **Then** they see title, reason, each changed target with old and new value and change, the effective week and the rule's numbers.
2. **Given** a plan change of someone else's client, **Then** the trainer gets "not found".
3. **Given** a client with an open plan change, **When** the trainer opens that client's page, **Then** it shows the plan change (title, reason, effective week) with a link to its detail.
4. **Given** a client-role caller, **Then** the trainer's detail is refused (their own view belongs to story #137).
5. **Given** the screen, **Then** it is complete in English and Polish, light and dark, at 1280px, and operable by keyboard with labelled controls.

---

### Edge Cases

- The same exercise appears twice in a session or in two sessions: entries are told apart by their place in the plan, and a rule that names an exercise concerns all its entries.
- A target the plan leaves empty (a bodyweight exercise with no load) is never given a load by an LLM answer or a fallback; such an answer is a limit violation.
- Plan edited by the trainer after the plan change was created: the stored old values stay as they were; what happens then is decided by stories #137 and #143.
- The plan has fewer entries than the rule's limit needs (for example no entry above RPE 8 any more): the proposed content is rejected and the fallback leaves nothing to change, so no plan change is created.
- A trainer opens a plan change of a client they no longer have: "not found".
- A failure in the LLM call never reaches the client or trainer as text; the system records only the class of failure, never a name, email or prompt.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST store a plan change per due plan change with the client, the rule, the numbers that made the rule fire, for each changed target its old and new value, the effective week, and the state.
- **FR-002**: The effective week of a plan change MUST be the Monday after the day it was created in the client's time zone.
- **FR-003**: The system MUST NOT store a plan change for a client who already has one that has not ended, and MUST NOT store two for one check.
- **FR-004**: The system MUST NOT store a plan change whose effective week is after the last week of the client's plan, or one that changes nothing.
- **FR-005**: The prompt sent to the LLM MUST contain the rule, its numbers, the plan in anonymised form and weekly aggregates of the logs, and MUST NOT contain a name, email, id or other identifier of a person held as data, nor any free text a person wrote.
- **FR-006**: The system MUST check every proposed content against the limits of its rule and against the plan rules before it is stored or shown to anyone.
- **FR-007**: High RPE MUST change only sets or repetitions (or seconds) of exercises with a 2-week mean RPE above 8, and the volume of each of their entries MUST drop by 5% to 15%; load and RPE MUST stay.
- **FR-008**: Stalled weight MUST change only the load of stalled exercises, each by +2.5% to +15%, rounded to 0.5 kg, and MAY lower their repetitions by up to 2, never below 1.
- **FR-009**: Low completion MUST remove exactly one session, leaving at least 2, MAY move up to 2 exercises of that session to other sessions, and MUST leave at most 8 exercises in every session.
- **FR-010**: The system MUST ask the LLM at most twice; content that breaks its limits or the plan rules twice, or an unavailable or unreadable answer counted the same way, MUST be replaced by the fixed transformation of the rule.
- **FR-011**: The fixed transformations MUST be: high RPE, repetitions × 0.9 rounded (at least −1, minimum 1); stalled weight, load +5% rounded up to 0.5 kg; low completion, remove the session finished least often, ties broken by the latest weekday.
- **FR-012**: A fixed transformation MUST be deterministic and MUST obey the plan rules, but is exempt from the percentage limits of its rule (which bound LLM drafts only).
- **FR-013**: Each plan change MUST carry a title and a reason line, built from the rule and its numbers, in English and in Polish, containing no personal identifier.
- **FR-014**: A trainer MUST be able to read the plan change of their own client in full (title, reason, every old and new value, effective week, the rule's numbers); anyone else MUST get "not found".
- **FR-015**: No endpoint of this story MUST be open without authentication, and any new endpoint MUST respond with RFC 9457 problem details and stable codes.
- **FR-016**: Every user-facing string of the screen MUST exist in English and Polish.
- **FR-017**: Only the system creates a plan change; this story offers no way to edit its content or decide on it (stories #137 and #143).

### Key Entities *(include if feature involves data)*

- **Plan change**: a suggested lasting modification of one client's plan: client, rule, the rule's numbers, changed targets, effective week, state, title and reason.
- **Changed target**: one value of one exercise entry (or one session) with its old and new value.
- **Plan change content**: the proposed changes (targets, removed session, moves), from the LLM or from the fixed transformation, before it is stored. Not a *draft* in the sense of `CONTEXT.md` (an unpublished plan).
- **Limits**: what a rule allows proposed content to change, together with the plan rules.
- **Rule numbers**: the measured values behind a fired rule (mean RPE per week, load rise in percent, share of sessions finished) with the window they cover.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For every rule, content exactly on each limit is accepted and content just beyond it are rejected, proven by tests.
- **SC-002**: A rule that fires always ends in a stored plan change (or a clear "nothing to change") even when the LLM fails every time; the fixed transformations match hand-computed values for each rule, including every rounding edge and the cases that exceed the limits for LLM answers.
- **SC-003**: No name, email or id ever appears in a prompt, proven by a test over every identifying value of the client.
- **SC-004**: A trainer sees the old value, the new value and the reason for every changed target of a plan change, in English and Polish, within 1 second of opening it.
- **SC-005**: A trainer who does not own the client of a plan change, and a request for a plan change that does not exist, always get "not found"; a client-role caller is refused by the role rule of the trainer area.

## Assumptions

- Story #125 is complete: detection hands over a due plan change (rule, window, exercises). It does not yet carry the rule's numbers or the per-week values; this story extends what it hands over so the numbers can be stored and shown.
- The plan change replaces the logging placeholder adapter of story #125 and gives its repository port a real implementation (the open state and the time it ended).
- States are those of story #137: pending, accepted by one, applied, declined, expired. This story only creates a plan change as pending; the other transitions belong to #137 and #143. Notifying people and posting into the conversation also belong to #137.
- The effective week is judged in the client's time zone; the plan version is kept with the plan change so later stories can tell whether the plan changed since.
- In this spec "draft" is only the verb for what the LLM does (`CONTEXT.md` reserves the noun for an unpublished plan); the proposed result is "plan change content".
- The screen is built from the Kinetiq Screens design and may be improved where that serves the trainer better; its accept and keep buttons and the Approvals queue are left to #137; here it is reached from the trainer's client page.
- Volume of an exercise is sets × repetitions (or seconds); a limit on percent is judged on the exact value before display rounding, except the load rule, judged after rounding to 0.5 kg.
- "Twice" counts failed attempts: an unreadable answer and an unavailable service each count as one.
- The title's percentage and the reason's numbers are derived from stored numbers, so they are available in both languages without storing text.
- Demo data is added for every new entity in the local-only Flyway location.
