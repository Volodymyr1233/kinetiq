# Feature Specification: Today's targets are lightened on a recovery day

**Feature Branch**: `feat/117-recovery-day-targets`

**Created**: 2026-11-09

**Status**: Draft

**Input**: Story #117 "Today's targets are lightened on a recovery day" and its tasks #118 (adjustment rules), #119 (apply, revert and undo API), #120 (adjusted session view) and #121 (use original targets with Undo). Screen: Kinetiq Screens, board "Client · Today recovery day". Builds on specs `109-readiness-check-in` (the check-in, readiness and its bands, fixed once a set is logged), `89-todays-session` (the Today tab), `92-session-logging` (the session log, its copy of the planned targets and the copy of the targets on each logged set) and `42-account-settings` (the client's unit). Later stories use it: #122 (the trainer sees a client's readiness). The story, the tasks and the board are starting points; every place this spec departs from them is listed under "Departures from the story and the board".

## Clarifications

### Session 2026-11-09

- Q: On a recovery day, what changes for an exercise without a load? → A: Target RPE one lower (not below 5) and the amount drops: one repetition fewer, or 10% fewer seconds rounded down; never below 1 (FR-004).
- Q: Until when may the client switch between lighter and original targets? → A: Until the first set of today's session is logged; the readiness card's existing "fixed" line then names the targets too, rather than a second line on the session card (FR-012, FR-024).
- Q: The client chose the original targets, then saves different answers still in Recovery day: which targets apply? → A: The lighter ones; different answers reset the choice, the same answers keep it (FR-011).
- Q: Where is the lighter/original switch offered? → A: On Today's session card only; the logger shows old → new targets without a switch (FR-017, FR-020).
- Q: For a client using pounds, may a lighter load read 179.9 lb instead of 180 lb? → A: Yes; round to the nearest whole pound and keep the nearest 0.1 kg like every load, so it may read up to 0.1 lb off (FR-002).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Client trains with lighter targets on a recovery day (Priority: P1)

A client checks in and their readiness falls in Recovery day. Today's session then shows lighter targets: less load, a target RPE one lower and, for an exercise without load, a little less to do. Each changed target shows its planned value and its new one, and one sentence says which answers caused it. The plan does not change.

**Why this priority**: It is the story's promise: on a bad day the numbers the client trains with are safer, without waiting for the trainer.

**Independent Test**: As a client with a session today (back squat 3 × 5, 100 kg, RPE 8; push-up 3 × 12, no load, RPE 8; plank 3 × 45 s, no load, RPE 7), check in with sleep 2, soreness 4, stress 3: the session shows back squat 100 → 90 kg and RPE 8 → 7 with 3 × 5 unchanged, push-up 12 → 11 reps and RPE 8 → 7, plank 45 → 40 s and RPE 7 → 6, and the sentence "Poor sleep and high soreness, so today's targets are lighter."; the Plan tab still shows 100 kg.

**Acceptance Scenarios**:

1. **Given** a saved check-in in the Recovery day band, **When** the client opens Today, **Then** today's session is marked "Adjusted for today" and its targets are adjusted by the rules of FR-001 to FR-006.
2. **Given** an exercise entry with a load, **When** it is adjusted, **Then** its load is 10% lower, rounded in the client's unit (FR-002), its target RPE is one lower but not below 5, and its sets, repetitions or seconds and rest are unchanged.
3. **Given** an exercise entry without a load, **When** it is adjusted, **Then** its target RPE is one lower but not below 5, and its amount is lower: one repetition fewer, or 10% fewer seconds rounded down; never below 1. Sets and rest are unchanged.
4. **Given** an adjusted target, **When** it is shown, **Then** the planned value is shown struck through, then an arrow, then the new value; a target that did not change is shown once, as planned.
5. **Given** the adjusted session, **When** it is shown, **Then** one sentence under its name names the answers that caused the adjustment (FR-008) and says today's targets are lighter, and the readiness card's line says "Today's targets are lighter." in place of the band's usual line.
6. **Given** an adjusted day, **When** the client opens the Plan tab, the plan editor is opened by the trainer, or any later day comes, **Then** they show the plan's own targets; the adjustment exists for that client and that date only. The client's Plan week marks that day's session "Adjusted" (FR-028).
7. **Given** a check-in in the Ready or the Take it easier band, a skipped check-in, or no check-in, **When** Today is shown, **Then** the session shows its planned targets and nothing is marked as adjusted.

---

### User Story 2 - Client logs against the adjusted targets (Priority: P1)

The logger shows the same old → new targets and suggests the adjusted values for each set. Each logged set keeps a copy of the targets it was logged against, so the trainer later sees what was asked that day.

**Why this priority**: Lighter targets that the logger ignores would be shown but never trained with; the copy keeps the record true (task #120).

**Independent Test**: On the adjusted day of User Story 1, choose "Start session": the back squat reads 100 → 90 kg and RPE 8 → 7; a set ticked as planned is logged with 90 kg, 5 reps and RPE 7, and its stored targets are 90 kg, 5 and 7.

**Acceptance Scenarios**:

1. **Given** an adjusted day, **When** the logger shows an exercise, **Then** its targets show planned → new values as on Today, and each unlogged set suggests the adjusted load, amount and RPE.
2. **Given** an adjusted day, **When** a set is logged, **Then** its copy of the targets is the adjusted one; with the original targets in use (User Story 3), it is the planned one.
3. **Given** a set was logged, **When** the trainer later changes the plan or the readiness rule is changed, **Then** the logged set keeps the targets it was logged against.
4. **Given** a session started on an adjusted day, **When** it is opened later from the Plan tab, **Then** it shows the targets of that day, old → new, as they were when its first set was logged.

---

### User Story 3 - Client uses the original targets, with Undo (Priority: P1)

A client who feels fine despite the answers taps "Use original targets" on the session. The planned targets return at once, with no dialog, and a message offers Undo. The session card then offers "Use lighter targets" to switch back.

**Why this priority**: The story requires it, and the constitution requires every change the system makes to be reversible (IV).

**Independent Test**: On the adjusted day, tap "Use original targets": the session shows 100 kg and RPE 8 with no arrows, the message "Original targets restored" with "Undo" appears; tap "Undo": the lighter targets return. Reload: the last choice is kept.

**Acceptance Scenarios**:

1. **Given** an adjusted session and no set logged today, **When** the client taps "Use original targets", **Then** without a dialog the session shows its planned targets, the mark reads "Original targets" instead of "Adjusted for today", the reason sentence is gone, the readiness card shows the band's usual line, and a message "Original targets restored" with "Undo" appears.
2. **Given** that message, **When** the client taps "Undo", **Then** the lighter targets return as before and the message closes.
3. **Given** the original targets in use, **When** the session card is shown, **Then** it offers "Use lighter targets", which brings the lighter targets back in one tap, without a message.
4. **Given** either choice, **When** the client reloads Today or opens the logger, **Then** the choice is kept: it is stored for that day, not only on the screen.
5. **Given** a set of today's session is logged, **When** Today is shown, **Then** neither "Use original targets" nor "Use lighter targets" is offered, and the readiness card says "Answers and targets are fixed once training has started."; a switch sent anyway is refused with a stable code and nothing changes.
6. **Given** the switch fails, **When** the request ends, **Then** the session returns to the stored targets and a translated message says it was not saved; nothing stays shown as switched that was not.

---

### User Story 4 - Changing the check-in recomputes the adjustment (Priority: P2)

Until the first set is logged the client can change their answers (spec `109-readiness-check-in`); today's targets follow the new answers.

**Why this priority**: The story requires it; without it a mis-tap would leave the wrong targets, but the main flow is a single check-in.

**Independent Test**: Check in with 2/4/3 (33, Recovery day): adjusted. Change to 4/2/1 (83, Ready): the planned targets, nothing marked. Change back to 2/4/3: adjusted again. Change to 1/4/3: the sentence names poor sleep and high soreness.

**Acceptance Scenarios**:

1. **Given** an adjusted day, **When** the client changes their answers to ones outside the Recovery day band, **Then** the session shows the planned targets and nothing is marked as adjusted.
2. **Given** a day outside the Recovery day band, **When** the client changes their answers into it, **Then** the session is adjusted as in User Story 1.
3. **Given** a recovery day, **When** the client changes their answers, **Then** the reason sentence names the new answers.
4. **Given** the client chose the original targets, **When** they save different answers that are still in the Recovery day band, **Then** the lighter targets apply again: a new reading starts from the lighter targets. Saving the same answers keeps the choice.
5. **Given** a session started and no set logged, **When** the answers change, **Then** the logger shows the recomputed targets the next time it is shown.

---

### User Story 5 - The adjustment is private to the client (Priority: P1)

Only the client reads and switches their own targets for their own today.

**Why this priority**: The adjustment reveals the client's readiness; the project's rule is that another person's data is never reachable.

**Independent Test**: Client A chooses the original targets; client B's Today stays adjusted on their own recovery day. As a trainer, send the switch: refused by role. With no sign-in: the standard refusal.

**Acceptance Scenarios**:

1. **Given** any request, **When** today's targets are read or the choice is switched, **Then** the client and the day are taken from the sign-in and the client's time zone; no request names another client, another day or a session.
2. **Given** client A switched their targets, **When** client B reads theirs, **Then** B's are unaffected and nothing of A's is returned.
3. **Given** a trainer or a visitor who is not signed in, **When** they read or switch a client's targets, **Then** a trainer is refused by role and a visitor is asked to sign in.
4. **Given** client B asks for client A's session log by its id, **When** it would show A's adjusted targets, **Then** the answer is the standard not-found one (spec `92-session-logging`), and a test proves it.

---

### Edge Cases

- Load rounding, kilograms: 10% off rounded to the nearest 0.5 kg, a half step going to the lighter value: 100 → 90, 80 → 72, 102.5 → 92 (92.25 is halfway), 20 → 18, 2.5 → 2 (2.25 is halfway). Tests cover each.
- Load rounding, pounds: the planned load is converted to pounds, 10% taken off and rounded to the nearest whole pound, a half going to the lighter value, and kept as the nearest 0.1 kg like every load; so it may read 0.1 lb off a whole pound (180 lb reads 179.9 lb). Tests cover a load whose 90% lands on a half pound.
- A load whose lighter value rounds to its planned value or below the smallest step (0.5 kg, 1 lb) keeps its planned load; only its RPE changes. A load never becomes "no load".
- Target RPE 5 stays 5. An entry whose every target is already at its floor (RPE 5, and 1 repetition or a load that cannot get lighter) is shown as planned, without an arrow.
- Seconds: 45 → 40 (40.5 rounded down), 30 → 27, 60 → 54, 5 → 4, 1 → 1. Repetitions: 12 → 11, 5 → 4, 1 → 1.
- When no target of the session changes at all, the session is not marked as adjusted, no sentence is shown, the readiness card keeps the band's usual line, and no switch is offered.
- Readiness below 45 always has at least one named answer: the score is 100 × (sum of three parts) / 12, so a sum of 5 or less has a part of 0 or 1, which is sleep 1–2, soreness 4–5 or stress 4–5.
- The trainer saves a plan change today after the check-in and before the session starts: Today adjusts the session as saved. After the session started, the session log's copy is what is adjusted (spec `92-session-logging`).
- The client changes their unit during an adjusted day: unlogged targets are rounded in the new unit on the next load; logged sets keep their copy.
- A switch sent twice gives the same result as once. A switch on a day that is not a recovery day, or with no adjustment, is refused with a stable code.
- A set logged in another tab while Today is open: the next switch is refused (User Story 3, scenario 5), and Today then shows what is stored.
- "Today" is the client's date in their time zone; at midnight the next day has no adjustment until a new check-in. The screen is not live.
- The "Original targets restored" message closes on its own after 5 seconds and stays while the client moves to another tab; the session card's "Use lighter targets" stays, so nothing depends on catching it in time. Only one such message shows at a time.
- No error response contains server internals.

## Requirements *(mandatory)*

### Functional Requirements

**Adjustment rules** (task #118)

- **FR-001**: An adjustment MUST apply to a client's targets of one date exactly when that date's check-in is in the Recovery day band (spec `109-readiness-check-in`, FR-008) and the client has not chosen the original targets for that check-in.
- **FR-002**: For an exercise entry with a load, the adjusted load MUST be the planned load minus 10%, rounded to the nearest 0.5 kg for a client using kilograms or the nearest whole pound for a client using pounds, with an exact half rounded to the lighter value, and kept as the nearest 0.1 kg like every load (so in pounds it may read up to 0.1 lb off a whole pound); it MUST NOT be heavier than planned, and when it would equal the planned load or fall below 0.5 kg (1 lb) the planned load MUST stay.
- **FR-003**: For every entry, the adjusted target RPE MUST be the planned one minus 1, and never below 5.
- **FR-004**: For an entry without a load, the adjusted amount MUST be one repetition fewer for an exercise measured in repetitions, and 10% fewer seconds rounded down to a whole second for one measured in seconds; never below 1.
- **FR-005**: Sets and rest MUST NOT change; for an entry with a load, the repetitions or seconds MUST NOT change.
- **FR-006**: The rules MUST be one deterministic calculation in the system's domain, from the planned targets, the check-in's answers and band, and the client's unit alone; the screen shows what the system computed and does not compute it again. Unit tests MUST cover the rounding in both units with halves, the RPE floor, entries without a load in both measures, the floors of FR-002 and FR-004, and an entry that does not change.
- **FR-007**: The reason MUST name the answers that caused the adjustment: poor sleep for sleep 1–2, high soreness for soreness 4–5 and high stress for stress 4–5, in that order, joined as a list in the UI language.
- **FR-008**: The reason sentence MUST read "{answers}, so today's targets are lighter." (e.g. "Poor sleep and high soreness, so today's targets are lighter."), in English and Polish, with no grammatical gender that the system cannot know.

**Apply, revert and undo** (task #119)

- **FR-009**: A signed-in client MUST be able to read today's targets: for each entry of today's session, its planned targets and, when adjusted, its adjusted ones; whether an adjustment exists for today, whether it is in use, which answers caused it, and whether the choice can still be switched.
- **FR-010**: A signed-in client MUST be able to switch between the lighter and the original targets for today; sending the choice already in use MUST give the same result.
- **FR-011**: The choice MUST be stored with today's check-in, and only the choice: the adjusted targets are derived from the plan or the session log's copy, the check-in and the client's unit whenever they are read (FR-006). A saved check-in with different answers MUST reset the choice to the lighter targets; the same answers keep it.
- **FR-012**: A switch MUST be refused, each with a stable code, and nothing changes: when a set of today's session is logged (the targets are fixed); when today has no adjustment (no check-in, another band, or nothing that changes); and with the codes of spec `109-readiness-check-in` for a rest day, a finished plan and no published plan.
- **FR-013**: The plan MUST NOT be changed by an adjustment or a switch: the trainer's plan, the client's Plan tab and every other date show the planned targets. A test proves the plan is unchanged after adjusting and switching.
- **FR-014**: The targets the logger suggests and every set log's copy of its targets MUST be the targets in use when the set is logged: adjusted on an adjusted day, planned otherwise. Once a set of the session is logged, the check-in and the choice are fixed (spec `109-readiness-check-in`), so the session's targets no longer change.
- **FR-015**: The client and the day MUST be taken from the sign-in and the client's time zone, never from the request. A trainer MUST be refused by role and a visitor MUST get the standard unauthenticated answer. One client's targets or choice MUST NOT be returned to or changed by another client. Tests prove each.

**Adjusted session view** (task #120)

- **FR-016**: On an adjusted day Today's session card MUST show the mark "Adjusted for today", the reason sentence under the session's name, and for each changed target the planned value struck through, an arrow and the new value; unchanged targets show once. The readiness card's line for the band MUST read "Today's targets are lighter." while the lighter targets are in use.
- **FR-017**: The logger MUST mark the session "Adjusted" once, beside its date, and show each exercise's changed targets the same way, old → new, without repeating the mark; it MUST suggest the adjusted values for unlogged sets, including the RPE marked as the target. The logger MUST NOT offer either switch; the choice is made on Today.
- **FR-018**: A struck-through value MUST NOT rely on the strike alone: its accessible text MUST say it was the planned value and give the new one (e.g. "Load 100 kg, today 90 kg").
- **FR-019**: At the bottom of an adjusted session card, under "Start session", the card MUST say "Only today's targets change. Your plan stays the same."

**Use original targets with Undo** (task #121)

- **FR-020**: While the choice can be switched, an adjusted session card MUST offer "Use original targets"; one tap MUST switch to the planned targets with no dialog and show the message "Original targets restored" with "Undo", which switches back. The message MUST last 5 seconds, MUST stay while the client moves between tabs, and a new one MUST replace the one still showing.
- **FR-021**: With the original targets in use and the choice switchable, the session card MUST be marked "Original targets" in the place of "Adjusted for today" and offer "Use lighter targets", which switches back in one tap with no message and closes the Undo message, which then has nothing to undo.
- **FR-022**: Focus MUST stay on the switch, which takes the place of the one tapped, and the change MUST be announced to assistive technology; the message with "Undo" MUST be reachable by keyboard, and nothing depends on it, as FR-021 offers the same.
- **FR-023**: A switch MUST show the requested targets at once; if the system refuses it or cannot be reached, the screen MUST return to the stored targets and show a translated message, so that once the request ends it never shows a choice that was not stored.
- **FR-024**: Once a set is logged, the session card MUST offer neither switch. On a day with an adjustment, the readiness card's line about fixed answers (spec `109-readiness-check-in`, FR-018) MUST read "Answers and targets are fixed once training has started." instead, so the rule is said once, in one place; on other days it is unchanged.

**Plan week mark** (task #275)

- **FR-028**: The client's Plan week MUST mark a session "Adjusted", next to its state, when the lighter targets of that date's check-in are in use for it; a session with the original targets chosen, without an adjustment, or on a day still to come is not marked. The opened card still lists the plan's own targets (FR-013). `GET /api/v1/session-states` gives the mark per session.

**Quality**

- **FR-025**: Every user-facing text MUST exist in English and Polish; numbers and units follow the UI language and the client's unit.
- **FR-026**: The screens MUST be built from Kinetiq tokens and shadcn components, designed at 375 px, in light and dark, with visible focus and keyboard use, every action at least 48 px by 48 px; nothing about the adjustment is shown in red or as an error; at 375 px and at 320 px (200% zoom) the page MUST NOT scroll sideways.
- **FR-027**: Demo data MUST let the adjustment be tried and seen: `demo.client.daily` can check in with 2/4/3 today, and has an earlier recovery day whose logged sets carry adjusted targets.

### Public endpoints

None. Every operation needs a signed-in client.

### Key Entities

- **Adjustment**: Today's lighter targets of one client's session on a recovery day (`CONTEXT.md`): for each exercise entry, its planned and its adjusted targets, and the answers that caused it. Derived, not stored.
- **Check-in**: Gains the client's choice for that date: the lighter targets (the default) or the original ones. Reset to the lighter targets when different answers are saved; fixed with the check-in once a set is logged.
- **Set log**: Unchanged; its copy of the targets is the adjusted one when it was logged on an adjusted day.
- **Plan, Session, Exercise entry, Target**: Unchanged; never written by this story.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A client on a recovery day sees the lighter targets with no tap beyond the check-in, and switches between lighter and original targets with one tap, without a dialog.
- **SC-002**: In tests, every rule of FR-002 to FR-005 gives the expected value for each example under "Edge Cases", in kilograms and in pounds.
- **SC-003**: In tests, after any sequence of check-ins and switches the plan is unchanged 100% of the time, and the sets logged carry the targets in use when they were logged.
- **SC-004**: In tests, a switch after a set is logged is refused 100% of the time and nothing changes; another client's targets and choice are never returned or changed.
- **SC-005**: A screen-reader user hears both the planned and the new value of each changed target, and hears the switch's result.
- **SC-006**: The Today tab and the logger pass WCAG 2.2 AA checks for contrast, labels, focus order, visible focus and touch size, in light and dark, at 375 px; nothing about the adjustment is red.
- **SC-007**: Every text of this story exists in English and Polish, and no error response contains server-internal text.

## Departures from the story and the board

| Source | It says | This spec | Why |
|---|---|---|---|
| #117 | "Bodyweight exercises change RPE only"; "sets, reps and seconds stay" | An entry without a load drops one repetition, or 10% of its seconds, along with one point of RPE; entries with a load keep their repetitions and seconds | RPE here measures reps left in reserve (spec `92-session-logging`: "8 · 2 reps left"). With a load, 10% less weight at the same repetitions is what makes the effort one point lower. With no load and the same amount the set is the same set, so a lower RPE target could not be met; rest length barely moves the RPE of a set. One repetition fewer is one more in reserve, which is RPE − 1. |
| #117 | "Rounded to 0.5 kg or 1 lb" | Nearest step, an exact half to the lighter value; kept as the nearest 0.1 kg, so pounds may read 0.1 lb off | The direction of a half was not given and the goal is lighter. Loads are kept to 0.1 kg everywhere (spec `68-plan-editor`), which a whole pound is not always. |
| #117 | "Changing the check-in recomputes the adjustment" | Also resets "Use original targets" when the answers differ | A new reading starts from what it calls for; keeping a choice made for other answers would apply it to a day the client did not judge. |
| #117, #121 | "Reverts in one tap with Undo" | Also "Use lighter targets" stays on the card | A message that closes on its own cannot be the only way back (WCAG 2.2.1 timing); the board draws the same control. |
| #119 | "Store today's adjustment" | Only the client's choice is stored; the adjusted targets are derived | Stored targets would go stale when the trainer saves the plan or the client changes the answers or their unit before training; the set logs keep what was actually asked. |
| Review | Adjusted days visible only by opening each day | The Plan week marks them (FR-028, task #275) | The client sees at a glance which days were lighter, without opening each one. |
| — | Switching at any time | Switching only until the first set is logged | The answers are fixed from then on (spec `109-readiness-check-in`), and logged sets keep their targets; changing targets mid-session would make sets of one exercise disagree. |
| Board | "You slept badly and you're sore" | "Poor sleep and high soreness, so today's targets are lighter." | Polish second-person past tense has a gender the system does not know; nouns read the same for everyone, and the per-target arrows already say how much. |
| Board | "−10% · −1 RPE" beside each exercise | Not shown | It repeats the arrows, and is wrong when rounding, a floor or no load changes the result. |
| Board | "Your trainer sees this. Your plan doesn't change." | "Only today's targets change. Your plan stays the same." | The trainer's view of readiness is story #122; promising it now would be false. |
| Board | Notifications, settings, week line on Today | Not here | Other stories; unchanged. |

## Assumptions

- Only the Recovery day band adjusts targets; Take it easier keeps its line "Same plan. Stop a rep earlier if you need to." and the planned targets. With the original targets chosen, Recovery day shows its band line "Same plan. Stop two reps earlier if you need to." (bug #274).
- The client's unit comes from their account settings (spec `42-account-settings`); "today" is computed as in spec `89-todays-session`.
- The adjustment is a fixed rule; nothing goes to an LLM (constitution IV).
- The adjusted targets are derived when read, so a later change of the rule would change how an earlier day's unlogged targets read; logged sets keep their copy, and nothing in this story needs the rule to change.
- The trainer's views of the adjustment are story #122; the trainer's logs tab already shows each set's copy of its targets.
- Offline logging is not in scope (story #114).
- The check-in migration takes the next free Flyway number at the time of writing, and is renumbered if another pull request takes it first.
