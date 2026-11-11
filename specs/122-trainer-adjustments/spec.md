# Feature Specification: Trainer sees a client's adjustments

**Feature Branch**: `feat/122-trainer-adjustments`

**Created**: 2026-11-10

**Status**: Draft

**Input**: Story #122 "Trainer sees a client's adjustments" and its tasks #123 (readiness and adjustments in the client overview API) and #124 (readiness strip and adjustment entries), plus task #290 (lighter targets in the trainer's session log), added during clarification. Screen: Kinetiq Screens, board "Trainer · client overview". Design: the Kinetiq design system, readiness section (the readiness colour scale, never red). Builds on spec `109-readiness-check-in` (the check-in, readiness and its bands), spec `117-recovery-day-targets` (the adjustment, its reason, and the client's choice of lighter or original targets), spec `104-client-training` (the client page's Overview and Logs tabs, the session log a trainer reads) and spec `156-client-progress` (the progress shown above the overview cards). The story, the tasks and the board are starting points; every place this spec departs from them is listed under "Departures from the story and the board".

## Clarifications

### Session 2026-11-10

- Q: Should planned → lighter targets in the trainer's session log (User Story 3) be part of this story or a later task? → A: Part of this story, as its own task #290 under #122, built in this pull request with its own commit.
- Q: How far back does the list of recovery days go? → A: Only the 14 days of the strip; older recovery days show in each session's log (FR-003).
- Q: Where on the Overview tab does the readiness card go? → A: First card of the two-column grid, under the progress and before "This week", as on the board (FR-008).
- Q: Does a training day without a check-in look different from a rest day without one? → A: No; every day without a check-in is empty and looks the same (FR-010).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Trainer sees the client's readiness for the last 14 days (Priority: P1)

The trainer opens a client's Overview tab. A card "Readiness, last 14 days" shows one column per day, oldest on the left and today on the right. A day with a check-in has a bar as tall as its readiness, coloured by its readiness band, with its score written on it. A day without a check-in has no bar. A legend names the three bands.

**Why this priority**: It is half of the story's acceptance criterion, and it gives the trainer the context for every number in the logs and the progress above: a week of low readiness explains a week of lower loads.

**Independent Test**: With a fixed clock on Wed 7 Oct in the client's time zone, check-ins on 24 Sep (score 75), 26 Sep (50), 3 Oct (33) and 7 Oct (0), and none on the other days. The card shows 14 columns from 24 Sep to 7 Oct; 24 Sep a Ready bar reading 75, 26 Sep a Take it easier bar reading 50, 3 Oct a Recovery day bar reading 33, 7 Oct a visible Recovery day bar reading 0, and ten columns with no bar. A screen reader reads each day's date with its score and band, or "No check-in".

**Acceptance Scenarios**:

1. **Given** a client of the signed-in trainer, **When** the trainer opens the Overview tab, **Then** the card "Readiness, last 14 days" shows today and the 13 days before it, by the client's local date, each labelled with its day of the month, today marked as today (FR-001, FR-008).
2. **Given** a day with a check-in, **When** the card is shown, **Then** that day has a bar whose height follows the score from 0 to 100, in the colour of its band, with the score written as a number (FR-009).
3. **Given** a day without a check-in, **When** the card is shown, **Then** that day has no bar and no number; it never reads as 0 (FR-002, FR-010).
4. **Given** a check-in with score 0, **When** the card is shown, **Then** that day still has a visible bar reading 0, distinct from a day without a check-in (FR-010).
5. **Given** the card, **When** it is read with a screen reader, **Then** each day is announced with its date and either its score and band or "No check-in" (FR-011).
6. **Given** a client with no check-in in the 14 days, **When** the card is shown, **Then** it says "No check-ins in the last 14 days." in place of the columns (FR-012).
7. **Given** the client changed today's answers, **When** the trainer opens or reloads the Overview tab, **Then** today shows the score of the stored answers.

---

### User Story 2 - Trainer sees every recovery day and whether the client reverted (Priority: P1)

Under the strip, the same card lists each recovery day of the 14 days, newest first: the date, the score, the reason (the answers that made it a recovery day), and which targets the client had: "Lighter targets" or "Original targets", the second meaning the client switched back. Each entry names the session of that day with its session state, and opens that session's log when one exists.

**Why this priority**: It is the other half of the acceptance criterion and the story's promise: the trainer understands why a session's numbers were lower, or sees that the client chose to train as planned anyway.

**Independent Test**: On the fixture of User Story 1, with 3 Oct's check-in at sleep 1, soreness 5 and stress 1 and the client's "Lower body A" started that day with lighter targets in use, and 7 Oct's check-in at sleep 1, soreness 5, stress 5 with "Use original targets" chosen and no set logged: the list shows 7 Oct first, "0 · Poor sleep, high soreness and high stress · Original targets", its session "Upper body" with state Today and no link; then 3 Oct, "33 · Poor sleep and high soreness · Lighter targets", its session "Lower body A" with its state and a link that opens its log in the Logs tab.

**Acceptance Scenarios**:

1. **Given** check-ins in the Recovery day band within the 14 days, **When** the Overview tab is shown, **Then** each has one entry under the strip, newest first, with its date, its score and its reason (FR-003, FR-013).
2. **Given** a recovery day whose lighter targets were in use, **When** its entry is shown, **Then** it reads "Lighter targets" (FR-004).
3. **Given** a recovery day on which the client chose "Use original targets", **When** its entry is shown, **Then** it reads "Original targets", so the trainer sees the client reverted (FR-004).
4. **Given** a recovery day with no target that could be lighter (no session that day any more, or every target already at its floor), **When** its entry is shown, **Then** it reads "No lighter targets" (FR-004).
5. **Given** a recovery day whose session was started, **When** the trainer chooses the entry's session, **Then** that session's log opens in the Logs tab (FR-014).
6. **Given** a recovery day whose session was never started, **When** its entry is shown, **Then** it names the session with its state (for example Missed or Today) and offers no link (FR-014).
7. **Given** no recovery day in the 14 days and at least one check-in, **When** the card is shown, **Then** it says "No recovery days in the last 14 days." under the strip (FR-012).
8. **Given** today is a recovery day and the client switches targets, **When** the trainer reloads the Overview tab, **Then** today's entry shows the client's current choice.

---

### User Story 3 - Trainer sees the lighter targets in the session's log (Priority: P2)

When the trainer opens the log of a session trained on a recovery day with the lighter targets, each target that was lighter shows the planned value struck through, an arrow and the lighter value, and a line under the session's title gives the reason. When the client chose the original targets, the line says so and the targets show once.

**Why this priority**: The entry of User Story 2 links here. Without it the trainer would see the lighter targets as if they were the plan, which hides the old value that every change the system makes must show (constitution IV).

**Independent Test**: Open the log of 3 Oct from User Story 2: the back squat reads 100 → 90 kg and RPE 8 → 7, the line reads "Recovery day: poor sleep and high soreness, so the targets were lighter.", and logged sets show the targets they were logged against. Open a log of a recovery day with the original targets: the line reads "Recovery day: the client used the original targets." and no arrows show.

**Acceptance Scenarios**:

1. **Given** a session log on a recovery day with the lighter targets in use, **When** the trainer opens it, **Then** each lighter target shows planned → lighter, unchanged targets show once, and the reason line names the answers (FR-015, FR-016).
2. **Given** a session log on a recovery day with the original targets chosen, **When** the trainer opens it, **Then** no target shows an arrow and the line says the client used the original targets (FR-016).
3. **Given** a session log on a day without a check-in, or not in the Recovery day band, **When** the trainer opens it, **Then** it reads as before, with no reason line.
4. **Given** a logged set, **When** its log is shown, **Then** it keeps the targets it was logged against, whatever the plan or the check-in says now.

---

### User Story 4 - Only the client's own trainer sees it (Priority: P1)

**Why this priority**: Readiness answers are health data about a person.

**Independent Test**: Trainer A reads their client's overview and session log: readiness is there. Trainer B reads the same client's: `404` with the client-not-found code. A client reads either: refused by role. No sign-in: the standard unauthenticated answer.

**Acceptance Scenarios**:

1. **Given** another trainer's client or an unknown client, **When** a trainer reads its overview or a session log, **Then** the answer is `404` with the existing client-not-found code, the same for both (FR-006).
2. **Given** a client or a visitor who is not signed in, **When** they read a client's overview, **Then** the client is refused by role and the visitor gets the standard unauthenticated answer (FR-006).

---

### Edge Cases

- A day before the client joined, before the plan was published, a rest day or a skipped check-in: no check-in, so no bar; nothing tells these apart, and nothing counts as 0.
- The client's time zone changed in the 14 days: days are the client's local dates as stored with each check-in; today is the client's local date now. A date never shows twice.
- Today without a check-in yet: today's column is empty; it fills once the client checks in.
- The trainer changed the plan after a recovery day: the entry's targets outcome is worked out from the session's own copy when it was started, otherwise from the plan as it is now; a lighter target may then appear or disappear for a day never trained, which only changes "No lighter targets" against "Lighter targets" for a session nobody logged.
- A recovery day whose session the trainer since removed from the plan and that was never started: "No lighter targets" and no session named.
- A plan that finished, or no published plan: the strip and entries still show any check-ins of the 14 days; with none, the empty message.
- A check-in's answers changed after it made a recovery day into another band: only the stored answers count; the day is not a recovery day any more and has no entry.
- The overview fails to load: the existing error with "Retry" covers the readiness card too; nothing shows partly.

## Requirements *(mandatory)*

### Functional Requirements

**Readiness and adjustments in the overview** (task #123)

- **FR-001**: The client overview MUST give 14 days of readiness: the client's local date today and the 13 days before it, oldest first, each with its date.
- **FR-002**: A day with a check-in MUST carry its score (0–100) and band; a day without one MUST carry neither. A day without a check-in MUST NOT be given a score of 0.
- **FR-003**: The client overview MUST give one recovery day for each check-in of those 14 days in the Recovery day band, and none from earlier days, newest first, with its date, score, the causes of FR-007 of spec `117-recovery-day-targets` in their fixed order, its targets outcome and its session.
- **FR-004**: The targets outcome MUST be "lighter" when the day's adjustment exists and is in use, "original" when it exists and the client chose the original targets, and "none" when the day has no adjustment. Whether an adjustment exists MUST follow the rules of spec `117-recovery-day-targets` over the session's own copy of its targets when the session was started, otherwise over the plan's targets for that day.
- **FR-005**: A recovery day's session MUST give the name and session state of the planned session on that date and, when it was started, its session log; it MUST be absent when no session is planned on that date and none was started.
- **FR-006**: Only the client's own trainer MUST be given any of it, through the existing overview: another trainer's client and an unknown client MUST both answer `404` with the client-not-found code; a client MUST be refused by role; a visitor MUST get the standard unauthenticated answer. Tests prove each.
- **FR-007**: Nothing of this MUST be stored: readiness, recovery days and outcomes are derived from stored check-ins, the client's choice, session logs and the plan whenever the overview is read.

**Readiness strip and recovery-day entries** (task #124)

- **FR-008**: The Overview tab MUST show a card "Readiness, last 14 days" as the first card of the overview grid, under the progress and before "This week", with a legend naming Ready, Take it easier and Recovery day, each with its colour.
- **FR-009**: Each day with a check-in MUST show a bar whose height is proportional to its score, coloured with the readiness scale of its band (never red), and its score as a number in the measured-value style.
- **FR-010**: A day without a check-in MUST show no bar and no number, only its date label, the same whether a session was planned that day or not; a score of 0 MUST still show a bar of a small visible height.
- **FR-011**: The strip MUST have a text alternative: each day read as its date and either "{score}, {band}" or "No check-in". The band MUST never be told by colour alone: the legend, the written score and the text alternative carry it.
- **FR-012**: With no check-in in the 14 days, the card MUST say "No check-ins in the last 14 days." and show neither strip nor list. With check-ins but no recovery day, it MUST say "No recovery days in the last 14 days." under the strip.
- **FR-013**: Each recovery-day entry MUST show the date (weekday, day and month in the UI language), the score, the reason, and the outcome as "Lighter targets", "Original targets" or "No lighter targets". The reason MUST name the causes as spec `117-recovery-day-targets` FR-007 does, joined as a list in the UI language, with no grammatical gender.
- **FR-014**: Each entry with a session MUST name it with its session state badge; a started session's name MUST be a link that opens its log in the client's Logs tab; a session never started MUST have no link.

**Lighter targets in the trainer's session log** (task #290)

- **FR-015**: A trainer's session log of a day whose lighter targets are in use MUST give, for each entry whose target is lighter, the planned target as well as the lighter one, worked out over the session's own copy (spec `117-recovery-day-targets` FR-006); the targets of planned sets not yet logged MUST be the lighter ones, and logged sets MUST keep the targets they were logged against.
- **FR-016**: The trainer's session log view MUST show each lighter target as the planned value struck through, an arrow and the lighter value, and on a recovery day a line under the title: "Recovery day: {reason}, so the targets were lighter." while the lighter targets are in use, "Recovery day: the client used the original targets." when they are not. A day with no adjustment shows no line.

**Both**

- **FR-017**: Every new string MUST exist in English and Polish, with Polish plural forms where a number is counted and no gendered forms for the client.
- **FR-018**: The card and the session log MUST be checked in a real browser at 1280px, light and dark, in English and Polish, with keyboard focus and labels (WCAG 2.2 AA). At narrower widths the strip MUST fit its card without scrolling the page sideways.

### Key Entities

- **Readiness day**: One of the 14 days of a client's overview: its date and, if the client checked in, its readiness score and band. Derived.
- **Recovery day**: A day of the 14 whose check-in is in the Recovery day band: its date, score, causes, targets outcome (lighter, original or none) and its session (name, session state, session log if started). Derived.
- **Check-in**, **Readiness**, **Adjustment** and the client's target choice: as in specs `109-readiness-check-in` and `117-recovery-day-targets`; unchanged.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: From the Overview tab alone, a trainer can name every recovery day of the last 14 days and whether the client reverted, in under 10 seconds, without opening another tab.
- **SC-002**: In every test fixture, 100% of days without a check-in show as empty and none as 0; a score of 0 is always visible.
- **SC-003**: From a recovery-day entry, the trainer reaches that session's log in one click, and sees both the planned and the lighter value of every target that changed.
- **SC-004**: Another trainer, a client and a visitor get no readiness data of the client in any test: `404`, refused by role, unauthenticated.
- **SC-005**: The card passes WCAG 2.2 AA checks for contrast, keyboard focus and text alternatives in light and dark, English and Polish.

## Departures from the story and the board

| Source | It says | This spec | Why |
|---|---|---|---|
| #122 | "Every recovery day" | Every recovery day of the same 14 days | The card is about the last 14 days; older recovery days still show in each session's log (User Story 3). |
| Board | "1 recovery day: targets were lighter on Sat 3 Oct." | One entry per recovery day with score, reason, outcome and session | The story asks whether the client reverted, and the trainer needs the reason and the way to the session to understand the numbers. |
| Board | Score only in a mouse `title` on each bar | Score written on each bar, and a text alternative per day | A hover-only value is unreadable by keyboard, touch and screen readers (WCAG 1.1.1, 1.4.13); the band is not told by colour alone. |
| Board | Bar labels are day numbers only | Day numbers, with today marked | Today is the column a trainer looks for first; the full date is in the text alternative. |
| Board | KPI tile "Readiness today" | Not added | The strip's last column is today; the KPI row above is the client's progress (spec `156-client-progress`). |
| Board, #122 | Nothing about the session log | The trainer's session log shows planned → lighter targets and the reason (User Story 3, task #290) | The entry links there, and the trainer's log showed lighter targets as if they were the plan; constitution IV asks that a change made by the system shows the old value, the new value and the reason. |

## Assumptions

- The 14 days are counted in the client's time zone, as everything else of the client (`CONTEXT.md`, Week).
- A recovery day is a check-in in the Recovery day band, whether or not an adjustment came of it; the outcome "No lighter targets" covers the rest.
- The reason for the trainer is the same list of causes the client sees, in the past: "the targets were lighter", not "today's targets are lighter".
- Showing the adjusted days in the overview's "This week" card is not part of this story; the entries and the session log cover them.
- The overview already loads once per visit; readiness comes with it and adds no separate request.
- No new data is stored, so no migration is added. The demo check-ins already hold a recovery day with a session logged on lighter targets; one recovery day with the original targets chosen is added so every outcome can be seen locally.
