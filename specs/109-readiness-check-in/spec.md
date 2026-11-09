# Feature Specification: Client checks in and sees readiness

**Feature Branch**: `feat/109-readiness-check-in`

**Created**: 2026-11-08

**Status**: Draft

**Input**: Story #109 "Client checks in and sees readiness" and its tasks #110 (check-in model and API), #111 (readiness score and band), #112 (check-in on the Today screen) and #113 (readiness card). Screens: Kinetiq Screens, boards "Today · check-in" and "Today · ready". Builds on specs `89-todays-session` (the Today tab and its states) and `92-session-logging` (session logs and set logs, which close the check-in). Later stories use it: #117 (today's targets are lightened on a recovery day, which recomputes when the check-in changes) and #122 (the trainer sees a client's readiness). The story, the tasks and the boards are starting points; every place this spec departs from them is listed under "Departures from the story and the boards".

## Clarifications

### Session 2026-11-08

- Q: Do the arrow keys pick a value, or only move, with Space/Enter picking it? → A: Arrows move only; Space, Enter, a click or a tap picks; the check-in saves at once on the third pick (FR-012).
- Q: When the client chooses "Change answers", are their saved answers already picked? → A: Yes; the saved answers are picked, and changes save only with "Done" ("Cancel" keeps them). The first check-in still saves on its third answer, and the arrow keys move only in both cases (FR-018).
- Q: How long does a skipped check-in stay skipped? → A: On that device until the client's day ends; nothing is stored on the server (FR-013).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Client checks in with three taps (Priority: P1)

On a training day, before training, Today asks the client how they slept, how sore they are and how stressed they feel, each on a 1–5 scale. Their third answer saves the check-in; there is no submit button.

**Why this priority**: This is the story's promise and the source of every readiness number, here and in #117 and #122.

**Independent Test**: As a client with a session today and nothing logged, open Today: the check-in shows three questions of five choices; choose sleep 4, soreness 2, stress 1: it saves without another tap, and reloading shows it saved.

**Acceptance Scenarios**:

1. **Given** a client whose plan has a session today, with no check-in and no logged set today, **When** they open Today, **Then** they see the check-in above today's session: "How do you feel today?", then Sleep (1 bad · 5 great), Soreness (1 none · 5 very sore) and Stress (1 calm · 5 high), each with the choices 1 to 5, and a line saying it saves after the third answer.
2. **Given** the check-in, **When** the client chooses answers in any order, **Then** nothing is saved until all three questions have an answer; the moment the third one is chosen, the check-in is saved for today.
3. **Given** the third answer was chosen, **When** the check-in is being saved, **Then** the choices stay as chosen and the card says it is saving; once saved, the readiness card takes its place (User Story 2).
4. **Given** the save fails, **When** the client sees the card, **Then** their three answers are still chosen, a translated message says it was not saved, and "Try again" saves the same answers.
5. **Given** a day without a session, a finished plan, or a client without a published plan, **When** they open Today, **Then** no check-in is shown (a rest day has nothing to be ready for).
6. **Given** today's session is shown below the check-in, **When** the client has not checked in, **Then** they can still start the session; the check-in never blocks training.

---

### User Story 2 - Client sees their readiness (Priority: P1)

After the check-in, Today shows a readiness card: the score from 0 to 100, its band and one line saying what it means for today, with the answers it came from.

**Why this priority**: The number is what makes the check-in worth giving.

**Independent Test**: Check in with 4/2/1: the card shows 83, "Ready" and "Train as planned."; change to 2/4/3: it shows 33 and "Recovery day"; nothing on the card is red.

**Acceptance Scenarios**:

1. **Given** answers sleep *s*, soreness *x* and stress *y*, **When** readiness is computed, **Then** the score is the mean of (*s* − 1) / 4, (5 − *x*) / 4 and (5 − *y*) / 4, times 100, rounded to a whole number: 2/4/3 gives 33, 4/2/1 gives 83, 5/1/1 gives 100 and 1/5/5 gives 0.
2. **Given** a score, **When** its band is chosen, **Then** 70 and above is Ready, 45 to 69 is Take it easier, and below 45 is Recovery day.
3. **Given** a saved check-in, **When** the readiness card is shown, **Then** it shows the label "Readiness", the score, the band's name, the band's line (Ready: "Train as planned."; Take it easier: "Same plan. Stop a rep earlier if you need to."; Recovery day: "Same plan. Stop two reps earlier if you need to."), and the answers: "Sleep 4 · soreness 2 · stress 1".
4. **Given** any band, **When** it is shown, **Then** it uses the readiness colour scale (high, mid, low) and never red; the band is also told by its name, not by colour alone.
5. **Given** a saved check-in, **When** the client reloads Today or comes back to the tab later that day, **Then** they see the same readiness card, not the check-in.
6. **Given** the readiness card, **When** it is shown, **Then** today's session is shown below it as planned, in every band.

---

### User Story 3 - Client changes their answers until the first set (Priority: P1)

The client can change today's answers from the readiness card until they log the first set of today's session. After that the check-in is fixed for the day.

**Why this priority**: A mis-tap must be fixable, and #117 builds today's targets from these answers, so they must stop moving once training starts.

**Independent Test**: Check in with 4/2/1, choose "Change answers": 4/2/1 are picked; pick sleep 2, soreness 4, stress 3 and choose "Done": the card shows 33; log a set in the logger, return to Today: the card has no "Change answers", and a change sent anyway is refused with nothing changed.

**Acceptance Scenarios**:

1. **Given** a saved check-in and no logged set today, **When** the client chooses "Change answers", **Then** the check-in opens again with the saved answers picked, with "Done" and "Cancel" in place of "Skip" and the line about saving; picking other values saves nothing yet.
2. **Given** the check-in being changed, **When** the client chooses "Done", **Then** today's check-in is replaced by the picked answers (still one check-in for the day) and the readiness card shows the new score, band and answers; "Done" with nothing changed returns the same card.
3. **Given** the check-in being changed, **When** the client chooses "Cancel", **Then** the readiness card returns with the saved answers, unchanged.
4. **Given** at least one set of today's session is logged, **When** the client sees the readiness card, **Then** it has no "Change answers"; it says that answers are fixed once training has started.
5. **Given** at least one set of today's session is logged, **When** a check-in for today is sent (new or changed), **Then** it is refused with a stable code and nothing changes; Today then shows what is stored.
6. **Given** no check-in and at least one logged set today, **When** the client opens Today, **Then** no check-in is offered: the day's first session has begun.

---

### User Story 4 - Client skips the check-in (Priority: P2)

A client who does not want to answer skips it. Nothing about their day changes.

**Why this priority**: The check-in is optional; skipping must be easy and harmless, but it is not the main flow.

**Independent Test**: Choose "Skip": the check-in folds into one line "Check-in skipped. Your session is as planned." with "Check in"; nothing is stored; choose "Check in": the check-in is back.

**Acceptance Scenarios**:

1. **Given** the check-in, **When** the client chooses "Skip", **Then** it is replaced by the line "Check-in skipped. Your session is as planned." and a "Check in" action; no check-in is stored and today's session is unchanged.
2. **Given** the check-in was skipped, **When** the client chooses "Check in", **Then** the check-in shows again with no answers chosen.
3. **Given** the check-in was skipped, **When** the client returns to Today later that day on the same device, **Then** it is still skipped; on the next day the check-in is offered again.
4. **Given** the client answered one or two questions, **When** they choose "Skip", **Then** those answers are dropped and nothing is stored.

---

### User Story 5 - A check-in is private to the client (Priority: P1)

A client reads and saves only their own check-in for their own today.

**Why this priority**: Sleep, soreness and stress are health data the client consented to give for their own plan (spec `30-client-join`), and the project's rule is that another person's data is never reachable.

**Independent Test**: Client A checks in; client B reads their own today: no check-in. As a trainer, save a check-in: refused by role. With no sign-in: the standard refusal.

**Acceptance Scenarios**:

1. **Given** client A has checked in today, **When** client B reads their check-in for today, **Then** B sees none; nothing of A's check-in is returned.
2. **Given** any request, **When** a check-in is read or saved, **Then** the client is taken from the sign-in, and the day is that client's today; no request can name another client or another day.
3. **Given** a trainer or a visitor who is not signed in, **When** they read or save a check-in, **Then** a trainer is refused by role and a visitor is asked to sign in.

---

### Edge Cases

- The score is 100 × (sum of the three parts) / 12, where each part is 0 to 4, so it is never exactly halfway between two whole numbers; the possible scores are 0, 8, 17, 25, 33, 42, 50, 58, 67, 75, 83, 92 and 100. The band edges therefore fall between 42 and 50 and between 67 and 75; tests cover 42 (Recovery day), 50 (Take it easier), 67 (Take it easier) and 75 (Ready).
- An answer outside 1–5, not a whole number, or missing is refused as a whole with a stable code for that answer; nothing is stored. A body that cannot be read is refused with the standard invalid-request code.
- Saving the same answers twice stores one check-in and answers the same both times.
- "Today" is the client's date in their time zone (spec `89-todays-session`); a check-in saved at 23:55 belongs to that date, and after midnight the next day asks again. The screen is not live: a day that ends while it is open shows the old day until reload.
- The trainer changes the plan so that today becomes a rest day after the client checked in: no check-in and no readiness card are shown that day; the stored check-in stays.
- A session started in the logger with no set logged yet still allows checking in and changing.
- A set logged in another tab while the check-in is open: the next save is refused (scenario 3.5), the card says the answers are fixed, and Today shows what is stored.
- A check-in is refused on a rest day and on a finished plan, each with a stable code, and with the standard "no plan" answer for a client without a published plan.
- Choices are moved through with the arrow keys and chosen with Space, Enter or a tap; moving through the last question's choices does not save a value the client passed over.
- No error response contains server internals.

## Requirements *(mandatory)*

### Functional Requirements

**Check-in model and API** (task #110)

- **FR-001**: A check-in MUST record, for one client and one date (the client's local date): sleep, soreness and stress, each a whole number from 1 to 5, and when it was saved.
- **FR-002**: A client MUST have at most one check-in per date.
- **FR-003**: A signed-in client MUST be able to read today's check-in: its three answers, its score and band (FR-007, FR-008), and whether it can still be changed; or that there is none yet, and whether one can still be given.
- **FR-004**: A signed-in client MUST be able to save today's check-in by sending all three answers; if one exists for today, it is replaced. Saving the same answers again MUST give the same result.
- **FR-005**: Saving MUST be refused, with a stable code each, and nothing changes: when a set of today's session is logged (the check-in is fixed); when today has no session in the client's plan; when the plan has finished; and with the standard "no plan" answer when there is no published plan. Values outside FR-001 MUST be refused with a stable code per answer.
- **FR-006**: The client and the day MUST be taken from the sign-in and the client's time zone, never from the request. A trainer MUST be refused by role and a visitor MUST get the standard unauthenticated answer. One client's check-in MUST NOT be returned to another client. Tests prove each.

**Readiness score and band** (task #111)

- **FR-007**: The readiness score MUST be the mean of (sleep − 1) / 4, (5 − soreness) / 4 and (5 − stress) / 4, times 100, rounded to the nearest whole number. Unit tests MUST include 2/4/3 → 33 and 4/2/1 → 83, and the band edges in "Edge Cases".
- **FR-008**: The readiness band MUST be Ready for 70 and above, Take it easier for 45 to 69, and Recovery day below 45.
- **FR-009**: The score and band MUST be computed by one rule in the system's domain, from the answers alone, deterministically; the screen shows what the system computed and does not compute it again.

**Check-in on the Today screen** (task #112)

- **FR-010**: On a day with a session, when there is no check-in and no logged set today, the Today tab MUST show the check-in card above today's session: its title, three questions as three labelled radio groups of the values 1 to 5, each with the meaning of its ends (Sleep 1 bad · 5 great; Soreness 1 none · 5 very sore; Stress 1 calm · 5 high), "Skip", and a line saying it saves after the third answer.
- **FR-011**: The check-in MUST be saved the moment all three questions have an answer, with no submit button; while it saves the answers stay chosen and the card says so; on failure the answers stay, a translated message is shown and "Try again" sends them again.
- **FR-012**: Within a group, the arrow keys MUST move between the values and Space, Enter, a click or a tap MUST choose one, so that moving through values never saves one the client did not choose. Each value's accessible name MUST include its number, and each group's accessible name its question; the chosen value MUST be shown by more than colour.
- **FR-013**: "Skip" MUST replace the check-in with "Check-in skipped. Your session is as planned." and a "Check in" action that brings it back; nothing is stored, and the skip is remembered on that device until the client's day ends.
- **FR-014**: Every choice and action of the check-in MUST be at least 48 px by 48 px.

**Readiness card** (task #113)

- **FR-015**: With a check-in saved for today, on a day with a session, the Today tab MUST show the readiness card above today's session instead of the check-in: "Readiness", the score as a measured value, the band's name, the band's line (FR-016), and the answers ("Sleep 4 · soreness 2 · stress 1").
- **FR-016**: Each band MUST have one line: Ready "Train as planned."; Take it easier "Same plan. Stop a rep earlier if you need to."; Recovery day "Same plan. Stop two reps earlier if you need to." The advice is optional because the targets themselves do not change (bug #274).
- **FR-017**: The band MUST be shown with the readiness colour scale (Ready high, Take it easier mid, Recovery day low), with its name in text, and never with red or an error style.
- **FR-018**: While the check-in can be changed, the card MUST offer "Change answers", which opens the check-in with the saved answers picked and "Done" and "Cancel" in place of "Skip" and the line about saving. Picking values there MUST NOT save; "Done" saves the picked answers, replacing the check-in, with the saving and failure behaviour of FR-011; "Cancel" returns to the card unchanged. Once a set is logged, the card MUST NOT offer it and MUST say the answers are fixed once training has started.
- **FR-019**: A newly shown score MUST be announced to assistive technology, and focus MUST NOT be lost when the check-in turns into the card or back, or while it saves: after a switch the client caused, focus moves to the new card's heading; on first load it is not moved.
- **FR-020**: While today's check-in loads, its place above the session MUST show a skeleton; a failed read of the check-in shows a translated message and "Try again" without hiding today's session.

**Quality**

- **FR-021**: Every user-facing text MUST exist in English and Polish; numbers follow the UI language.
- **FR-022**: The screen MUST be built from Kinetiq tokens and shadcn components, designed at 375 px, in light and dark, with visible focus and keyboard use; at 375 px and at 320 px (200% zoom) the page MUST NOT scroll sideways.
- **FR-023**: Demo data MUST let the check-in be tried: `demo.client.daily` has no check-in for today, and has check-ins on earlier days in each band for later stories to show.

### Public endpoints

None. Every operation needs a signed-in client.

### Key Entities

- **Check-in**: One client's answers for one date: sleep, soreness and stress (1–5 each) and when it was saved. One per client per date; replaced when changed; fixed once a set of that day's session is logged.
- **Readiness**: The 0–100 score and its band (Ready, Take it easier, Recovery day), derived from a check-in by FR-007 and FR-008; not entered by anyone.
- **Session log, Set log**: Unchanged (spec `92-session-logging`); a set log on today's session log is what fixes the check-in.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A client checks in with exactly three taps and sees their readiness within 2 seconds of the third tap on an ordinary connection.
- **SC-002**: In tests, every one of the 125 possible answer combinations gives the score and band of FR-007 and FR-008, including 2/4/3 → 33 and 4/2/1 → 83.
- **SC-003**: In tests, a check-in sent after a set is logged is refused 100% of the time and nothing changes; another client's check-in is never returned.
- **SC-004**: A keyboard-only client can choose any value in each question, and moving through values never saves an answer they did not choose.
- **SC-005**: The check-in and the readiness card pass WCAG 2.2 AA checks for contrast, labels, focus order, visible focus and touch size, in light and dark, at 375 px; no band is shown in red.
- **SC-006**: Every text of the check-in and the card exists in English and Polish.
- **SC-007**: No error response of this story contains server-internal text.

## Departures from the story and the boards

| Source | It says | This spec | Why |
|---|---|---|---|
| #109 | Check-in "before the day's first session"; changes allowed "until the first set is logged" | Both a new check-in and a change are refused once a set of today's session is logged; no check-in on a rest day or a finished plan | A plan has at most one session a day, so the first logged set is when that session begins. The rest-day board says "No check-in either"; readiness is about training today. |
| #109 | Bands Ready and Recovery day have no advice line | Every band has one line | The boards give every band a line, and a score alone does not tell the client what to do. |
| Boards | Recovery day: "Today's targets are lighter.", "See today's targets", session hidden | "Same plan. Stop two reps earlier."; the session stays as planned and shown | Lighter targets are story #117, which replaces this line and the session view. Promising lighter targets now would be false. |
| Board | "Change answers" clears the answers with no way back | The saved answers stay picked; changes save with "Done", and "Cancel" keeps the saved ones | A client fixing one answer should not answer all three again, and opening it by mistake must lose nothing. Saving on every pick would close the card after one change. |
| #112 | "Three radio groups" that save on the third answer | Arrow keys move, Space, Enter or a tap chooses, in the first check-in and when changing | In a usual radio group the arrow keys choose, so a keyboard user moving to stress 4 would save stress 1 on the way; one keyboard model for both cases. |
| #109 | "Skipping changes nothing" | Not stored; remembered on the device until the day ends, and can be undone with "Check in" | A skip that reappears on every visit to the tab would nag; a stored skip would be data the story says it does not create. |
| #110 | "Endpoints to save and change it" | One save that creates or replaces today's check-in | The client always answers all three; creating and changing are the same operation, and repeated sends stay safe. |
| Board | "Your trainer sees this", notifications, settings link, week line on Today | Not here | The trainer's view is story #122; the others belong to other stories and are unchanged. |

## Assumptions

- The client's time zone comes from their account settings (spec `42-account-settings`); "today" is computed as in spec `89-todays-session`.
- The consent to process health data given at joining (spec `30-client-join`) names sleep, soreness and stress, so no new consent is asked.
- Nothing of a check-in goes to an LLM; readiness is a fixed rule (constitution IV).
- The score and band are derived from the answers when read, so a later change of the rule would apply to past check-ins too; nothing in this story or #122 needs the rule to change.
- The check-in is optional and never blocks "Start session" or the logger.
- Lighter targets on a recovery day, and recomputing them when answers change, are story #117; the trainer's view of readiness is story #122. This story stores what they need.
- Offline check-in is not in scope; story #114 covers logging without a connection.
- The migration takes the next free Flyway number at the time of writing (V21 now), and is renumbered if another pull request takes it first.
