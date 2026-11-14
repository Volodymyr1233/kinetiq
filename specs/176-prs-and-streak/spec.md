# Feature Specification: Client sees PRs and their week streak

**Feature Branch**: `feat/177-prs-and-streak`

**Created**: 2026-11-14

**Status**: Draft

**Input**: Story #176 "Client sees PRs and their week streak" and its tasks #177 (PRs and streak, the domain calculation), #304 ("PR today" not for the first log of an exercise), #302 (PRs and week streak in the session summary API), #178 (PR badge on the session done screen and streak tile on Progress) and #303 (week streak on the session done screen). Screens: Kinetiq Screens, boards "Client · Session done" (a PR badge and "weeks in a row" under the title) and "Client · Progress" (the "In a row" tile). Design: the Kinetiq design system, section 6.8 (the PR badge, lime, and the week streak, outline, are the only reward elements, on client screens only) and its brand rule (at most one lime element per view). Builds on spec `156-client-progress`, which already delivered the week streak (FR-006, the "In a row" tile of FR-016) and "PR today" on the best-set tile (FR-005, FR-015), on client screens only; spec `98-finish-session` (the session done screen, which left the PR badge and the streak to a later story); spec `101-log-edit-window` (the 24 hours of changes); spec `104-client-training` (the trainer's views of a client) and spec `42-account-settings` (weight unit). The story, the tasks and the boards are starting points; every place this spec departs from them or from an earlier spec is listed under "Departures from the story, the boards and earlier specs".

## Clarifications

### Session 2026-11-14

- Q: Story acceptance criterion 1 says a PR "beats the client's best load", while the top set, and with it "PR today" of spec 156, also ranks more repetitions or seconds at the same load, and sets without a load. Does a PR follow the top set order, or only a higher load? → A: The top set order (CONTEXT.md, Top set), as "PR today" counts it: a set with a load beats one without, then the higher load, then at the same load the most repetitions or seconds.
- Q: Is the first time an exercise is logged a PR? → A: No. A PR needs an earlier set of that exercise to beat; "PR today" on Progress follows the same rule (FR-006).
- Q: The board shows one lime PR badge, and the design system allows at most one lime element per view. With more than one PR in a session, is every PR badge lime? → A: Only the first, in the order of the session's exercises; the others are outlined, with the same text.

## Delivered before this story

Spec 156 was built before this story and already covers part of it. This spec builds only the rest.

| Story acceptance criterion | Already delivered (spec 156) | Added by this spec |
|---|---|---|
| 1. A PR badge when a set beats the client's best load for that exercise | "PR today" on the Progress best-set tile, for the exercise that tile shows (FR-005, FR-015) | The PRs of a session, shown as badges on the session done screen (FR-001, FR-008); "PR today" no longer marks the first log of an exercise (FR-006) |
| 2. A streak of consecutive weeks with at least 2 finished sessions | The week streak (FR-006) and the "In a row" tile on Progress (FR-016) | The same streak on the session done screen (FR-010) |
| 3. Reward elements appear on client screens only | The trainer's progress section of a client shows no streak and no "PR today" (spec 156, Assumptions) | The trainer's other views of a client stay without rewards, and tests prove it (FR-011) |

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Client sees the PRs of a session (Priority: P1)

The client finishes a session. Under the title of the session done screen, a PR badge names each exercise whose best set in this session beats everything they logged of that exercise before, with that set.

**Why this priority**: It is the story's first acceptance criterion and the moment a client feels progress most: right after the set that beat their best.

**Independent Test**: Back squat logged on Monday with a top set of 100 kg × 5 and on Wednesday with 102.5 kg × 5. Finish Wednesday's session: the session done screen shows "PR · Back squat 102.5 kg × 5". On Friday log back squat at 102.5 kg × 5 again and finish: no PR badge, since an equal set is not a PR.

**Acceptance Scenarios**:

1. **Given** a finished session in which an exercise's top set beats every earlier set of that exercise, **When** the session done screen shows, **Then** it shows a PR badge with the exercise's name and that set, in the client's weight unit.
2. **Given** an exercise whose top set only equals the best earlier set, **When** the session done screen shows, **Then** it shows no PR badge for that exercise.
3. **Given** an exercise whose top set has the same load as the best earlier set and more repetitions, **When** the session done screen shows, **Then** it shows a PR badge for that exercise.
4. **Given** an exercise logged for the first time, **When** the session done screen shows, **Then** it shows no PR badge for that exercise, and Progress does not mark it "PR today".
5. **Given** a session with a PR today, **When** the client opens Progress and that exercise is on the best-set tile, **Then** it is marked "PR today"; an exercise without a PR on the session done screen is not marked (FR-006).
6. **Given** a finished session, **When** the client changes a set within its 24 hours so that it now beats, or no longer beats, the earlier best, **Then** the next time the session done screen shows, the badge follows the sets as they are now.
7. **Given** a session finished on an earlier day, **When** the client opens its summary later from the Plan tab, **Then** it shows the PRs that session set against the sessions before it, even when a later session has beaten them since.
8. **Given** a session with more than one PR, **When** the session done screen shows, **Then** every PR has its badge, in the order of the session's exercises; only the first is lime, the others are outlined with the same text.

---

### User Story 2 - Client sees their week streak after a session (Priority: P1)

Next to the PR badges, the session done screen tells the client how many weeks in a row they have trained, the same number as the "In a row" tile on Progress.

**Why this priority**: The story's second acceptance criterion; the session done screen is where finishing a session can extend the streak.

**Independent Test**: A plan of 3 sessions a week, 2 sessions done in each of plan weeks 1 to 3, and in week 4 the second session just finished: the session done screen shows "4 weeks in a row", and the "In a row" tile on Progress shows 4. With week 2 at 1 of 3 sessions done instead, both show 2.

**Acceptance Scenarios**:

1. **Given** a client whose week streak is at least 1, **When** the session done screen shows, **Then** it shows "{n} weeks in a row" with the same number as the "In a row" tile on Progress.
2. **Given** a client whose week streak is 0, **When** the session done screen shows, **Then** it shows no streak.
3. **Given** the session just finished is the one that makes the current week meet the rule, **When** the session done screen shows, **Then** the streak already counts the current week.
4. **Given** a completed week with fewer sessions done than the rule needs, **When** the streak is counted, **Then** only the weeks after it count.

---

### User Story 3 - Trainers never see rewards (Priority: P1)

A trainer reads a client's progress and sessions, but no PR badge, "PR today" or week streak appears on any trainer screen.

**Why this priority**: The story's third acceptance criterion and the design system's rule: rewards are for the client; the trainer judges the training by its numbers.

**Independent Test**: As a trainer, open the overview of a client who has a PR today and a week streak of 4, then one of that client's sessions with a PR: neither screen shows a PR badge, "PR today" or a streak.

**Acceptance Scenarios**:

1. **Given** a client with a PR today and a week streak, **When** their trainer opens the client's overview, **Then** its progress section shows neither (spec 104, task #108).
2. **Given** a client's session with a PR, **When** their trainer opens that session, **Then** it shows no PR badge and no streak.
3. **Given** a session's summary with its PRs and streak, **When** anyone other than its client asks for it, **Then** the answer is as before: "not found" for another client's or an unknown session, and a refusal by role for a trainer (spec 98, FR-004 and FR-008).

---

### Edge Cases

- Earlier sets are the sets of the client's session logs dated before the session's date, finished or not, as spec 156 counts sets of unfinished sessions in best sets.
- The same exercise twice in one session: both entries count as that exercise, and it has at most one PR badge.
- An exercise measured in seconds, or logged without a load: its top set and its badge are in repetitions or seconds, never in kilograms.
- An exercise logged with and without a load (a pull-up, weighted on some days): a set with a load is the top set over one without (CONTEXT.md, Top set).
- A client who uses pounds: the badge's load is in pounds, converted from the kilograms stored.
- A later session that beats a PR does not take the badge from the earlier session's summary; the badge says what that session did against the sessions before it.
- A set of an earlier session changed within its 24 hours can change whether a later session's set is a PR, since both are worked out when read.
- The week streak runs up to the current week as spec 156 counts it: a running week adds one once it meets the rule and never ends the streak before it ends; a first week shortened by the publish day counts when every session it had was done; once the plan has finished, it runs up to the plan's last week.
- Dates and weeks are the client's, in their time zone (spec 156).
- A finished session always has a published plan behind it: a session starts only from one, and at the time of this story nothing removes a published plan while its client has an account. So the streak can always be worked out for a summary.
- No error response contains server internals.

## Requirements *(mandatory)*

### Functional Requirements

**PRs and week streak** (task #177)

- **FR-001**: For a session log, the system MUST tell, for each exercise logged in it, whether its top set (CONTEXT.md, Top set; spec 156, FR-004) is a PR: the client's session logs dated before it hold at least one set of that exercise, finished or not, and the top set beats every one of them. A set that only equals the earlier best is not a PR, and the first log of an exercise is not a PR.
- **FR-002**: The week streak MUST be the one of spec 156 (FR-006; CONTEXT.md, Week streak), unchanged, so that every screen that shows it shows the same number.
- **FR-003**: PRs and the week streak MUST be worked out when read, from the sets as they are, and never stored, so a change within the 24 hours of spec 101 shows at once.
- **FR-004**: Tests MUST cover ties (a set equal to the earlier best; an earlier best reached in two sessions; the same load with more repetitions) and broken streaks (a completed week below the rule between counted weeks; the running week before and after it meets the rule; a first week shortened by the publish day).
- **FR-005**: CONTEXT.md's entry for **PR** MUST say that a PR beats every earlier result of the client for that exercise, by the top set order, so the first log of an exercise is not one.

**"PR today" on Progress** (task #304)

- **FR-006**: The PRs of FR-001 MUST agree with "PR today" on Progress (spec 156, FR-005): for the session log dated today, an exercise has a PR exactly when Progress marks it "PR today". "PR today" therefore no longer marks an exercise logged for the first time.

**Session summary** (task #302)

- **FR-007**: A signed-in client MUST be able to read, with the summary of their own finished session, the session's PRs (FR-001), each with its exercise and top set in the order of the session's exercises, and their week streak (FR-002). The access rules of spec 98 stay: another client's or an unknown session answers "not found", a session not finished is refused with its stable code, a trainer is refused by role and a visitor gets the standard unauthenticated answer.

**PR badges on the session done screen** (task #178)

- **FR-008**: The session done screen MUST show under its title, as on the board, a PR badge for each PR, in the order of the session's exercises: "PR · {exercise} {set}", the set as load × repetitions, or as repetitions or seconds when it has no load, in the client's weight unit. Only the first PR badge is lime; every further one is outlined, with the same text, so the view keeps one lime element.
- **FR-009**: The session done screen's loading, failure and not-found states MUST stay as in spec 98 (FR-015); the skeleton keeps a place for the badges.

**Week streak on the session done screen** (task #303)

- **FR-010**: The session done screen MUST show the week streak, next to the PR badges, as "{n} weeks in a row" in an outline badge when it is at least 1, and nothing when it is 0.

**Client screens only** (every task)

- **FR-011**: No trainer screen MUST show a PR badge, "PR today" or a week streak: neither the progress section of a client's overview (spec 104, task #108), whose test already proves it, nor the trainer's view of a client's session. The PRs and the streak of a session MUST reach only its client: a trainer asking for a session's summary is refused by role (spec 98, FR-004), as its tests already prove.

**Quality**

- **FR-012**: Every new text MUST exist in English and Polish, with Polish plural forms; numbers and units follow the reader's language and settings.
- **FR-013**: The badges MUST say what they are in words ("PR", "weeks in a row"), never by colour alone, and MUST be built from Kinetiq tokens and shadcn components (the first PR badge in the brand variant with ink text, further PR badges and the streak in the outline variant), designed at 375 px, in light and dark; at 375 px and at 320 px (200% zoom) the screen MUST NOT scroll sideways.

### Public endpoints

None. Every operation needs a signed-in client.

### Key Entities

- **PR**: A logged result that beats every earlier result of the client for an exercise, by the top set order (FR-001, FR-005). For a session: an exercise whose top set in that session beats every earlier set of it. Derived when read, not stored.
- **Week streak**: As in CONTEXT.md and spec 156 (FR-006). Derived when read, not stored.
- **Session summary**: As in spec 98, unchanged; the session done screen shows the PRs and the week streak next to it.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In tests with known logs, a session's PRs equal those worked out by hand in 100% of cases, including a tie, the same load with more repetitions, a set without a load, an exercise in seconds and an exercise logged for the first time.
- **SC-002**: In tests with a fixed clock, the PRs of today's session and "PR today" on Progress agree in 100% of cases, and the streak on the session done screen equals the "In a row" tile in 100% of cases.
- **SC-003**: In tests with a fixed clock, the week streak is right in 100% of cases on both sides of each boundary: a broken week, the running week meeting the rule, the publish day and the end of the plan.
- **SC-004**: A client sees the PRs and the week streak of a session with no tap beyond finishing it.
- **SC-005**: No trainer screen shows a reward element: the trainer's progress section shows none for a client with a PR today and a streak, and a trainer is refused the session summary that carries them, in 100% of tests.
- **SC-006**: The badges pass WCAG 2.2 AA checks for contrast and text alternatives, in light and dark, at 375 px; every new text exists in English and Polish.

## Departures from the story, the boards and earlier specs

| Source | It says | This spec | Why |
|---|---|---|---|
| Story #176, criterion 1 | A PR "beats the client's best load" | The top set order: a set with a load beats one without, then the higher load, then at the same load the most repetitions or seconds | Clarifications: one definition with "PR today" and CONTEXT.md (Top set), so exercises without a load have PRs too. |
| Spec 156, FR-005 | "PR today" also marks the first log of an exercise | The first log of an exercise is not a PR, on Progress too (FR-006) | Clarifications: a PR needs an earlier set to beat; a new client's first session would otherwise mark every exercise. |
| Story #176, criterion 2 | "At least 2 finished sessions" | The week streak of spec 156: a week also counts when it had fewer than 2 planned sessions and all were done, and sessions done are matched to planned days | CONTEXT.md, Week streak. Only a first week shortened by the publish day has fewer than 2 planned sessions. |
| Task #178 | "Streak tile on Progress" | The tile was delivered by spec 156 (task #160); the streak on the session done screen is task #303 | The board "Client · Session done" shows "weeks in a row" under the title. |
| Board "Client · Session done" | One lime PR badge | With more than one PR, only the first badge is lime, the others outlined | The design system's brand rule: at most one lime element per view. |
| Board "Client · Session done" | "4 weeks in a row" | Hidden when the streak is 0 | "0 weeks in a row" right after finishing a session would read as a reproach. |
| Board "Client · Session done" | "Synced" badge, "Target 7–8" under the average RPE | Not part of this story | Spec 98, Departures: the sync badge belongs to offline logging (story #114), and a session has no single RPE target. |

## Texts

The main English and Polish texts. Polish uses informal "ty" and should be confirmed by a copywriter, as the design system says (8.5).

| Where | English | Polish |
|---|---|---|
| PR badge | PR · {exercise} {set} | PR · {exercise} {set} |
| Set with a load | {load} {unit} × {repetitions} | {load} {unit} × {repetitions} |
| Set without a load | {n} rep · {n} reps · {n} s | {n} powtórzenie · {n} powtórzenia · {n} powtórzeń · {n} s |
| Week streak | {n} week in a row · {n} weeks in a row | {n} tydzień z rzędu · {n} tygodnie z rzędu · {n} tygodni z rzędu |

## Assumptions

- The "In a row" tile on Progress stays as spec 156 delivered it; "PR today" changes only for the first log of an exercise (FR-006).
- A plan has at most one session a day (spec 68), so a session log's date orders it among the client's sessions.
- The streak on the session done screen is the client's streak when the screen is read, also for a session opened later from the Plan tab, so it always matches Progress.
- The trainer has no session done screen; the trainer's views of a client (spec 104) are the only trainer screens that read the same logs.
- Exercise names come from the trainer's exercise library, in the reader's language, as on Progress.
- Nothing is stored, so this story needs no migration and no new demo entity. The demo session logs copy their planned targets, so they already give a week streak to show; a PR shows once a set heavier than the demo logs is logged.
