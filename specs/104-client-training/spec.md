# Feature Specification: Trainer sees a client's training

**Feature Branch**: `feat/105-client-overview-api`

**Created**: 2026-11-08

**Status**: Draft

**Input**: Story #104 "Trainer sees a client's training" and its tasks #105 (client overview API), #106 (client overview tab), #107 (client logs API and tab), #261 (client's note in the Logs tab) and #108 (progress section in the client overview). Screens: Kinetiq Screens, boards "Trainer · client overview" and "Trainer · client logs". Design: the Kinetiq design system (badges, cards, tables at trainer density, RPE colours). Builds on spec `35-trainer-clients` (the trainer's clients and the client page), spec `92-session-logging` (session log, set log, targets copied when a session starts), spec `98-finish-session` (session states, session summary, the client's note), spec `101-log-edit-window` (Locked) and spec `85-draft-review` (the trainer reads a client's quiz answers beside a draft). The progress section uses the statistics of story #156 (tasks #158 and #159). The story, the tasks and the boards are starting points; every place this spec departs from them, or from an earlier spec, is listed under "Departures from the story, the boards and earlier specs".

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Trainer sees how the client's week is going (Priority: P1)

The trainer opens a client's page. The Overview tab shows this week's planned sessions, each with its state, the client's most recent sessions with how many sets they logged and how hard they felt, and the client's quiz answers.

**Why this priority**: It is the first thing a trainer needs before coaching: did the client train, and what did they ask for (story acceptance criterion 1).

**Independent Test**: With a fixed clock on a Wednesday and a plan with sessions on Monday, Wednesday and Friday: a finished Monday session, a Wednesday session started and not finished, and an earlier week with one finished and one missed session. The Overview shows Monday as Done with its average RPE, Wednesday as In progress, Friday as Planned, the four most recent sessions newest first, and the quiz answers.

**Acceptance Scenarios**:

1. **Given** a client of the trainer with a published plan, **When** the trainer opens the Overview tab, **Then** "This week" shows each planned session of the client's current plan week with its name, weekday and label (FR-004), and the seven days of the week marked as on the client's Plan tab (spec 98, FR-016).
2. **Given** the same client, **When** the Overview is shown, **Then** "Recent sessions" lists their four most recent sessions that were started or have passed, newest first, each with name, date, sets logged out of sets planned, average RPE when a set is logged, and label.
3. **Given** a client who submitted the quiz, **When** the Overview is shown, **Then** "Quiz answers" shows goal, training experience, level, age, height, weight, likes and notes, as the trainer reads them beside a draft (spec 85).
4. **Given** a client with no published plan (none, or only a draft), **When** the Overview is shown, **Then** "This week" and "Recent sessions" say there is no published plan yet, and the quiz answers still show.
5. **Given** a client who has not submitted the quiz, **When** the Overview is shown, **Then** "Quiz answers" says there are none yet, and the sessions still show.
6. **Given** a client whose plan has finished, **When** the Overview is shown, **Then** "This week" says the plan has finished, and "Recent sessions" still lists their last sessions.
7. **Given** "Recent sessions", **When** the trainer chooses "All logs", **Then** the Logs tab opens.

---

### User Story 2 - Trainer reads every set the client logged (Priority: P1)

On the Logs tab the trainer sees the client's sessions, newest first. Choosing one shows every set the client logged next to the targets of that session: load, repetitions or seconds, and RPE.

**Why this priority**: Coaching needs the numbers, not only "done" (story acceptance criterion 2).

**Independent Test**: A client logs 3 sets of a bench press planned as 3 × 8 at 50 kg, RPE 8, logging 50 kg × 8 at RPE 8, 50 kg × 8 at RPE 8.5 and 50 kg × 7 at RPE 9, and finishes. The trainer opens the Logs tab: the session is first, labelled by FR-004, and shows "3 of 3 sets", the minutes and an average RPE of 8.5; each row shows set number, target 8 × 50 kg, and the logged 50 kg, 8, 8 and so on.

**Acceptance Scenarios**:

1. **Given** a client with session logs and missed sessions, **When** the trainer opens the Logs tab, **Then** it lists every session that was started or has passed since the plan was published, newest first, each with name, date and label, and the newest is shown.
2. **Given** a chosen session with logged sets, **When** it is shown, **Then** each exercise lists every planned set with its number, its target (repetitions or seconds, load, RPE) and what was logged (load, repetitions or seconds, RPE); a planned set that was not logged is marked as not logged.
3. **Given** a chosen session, **When** it is shown, **Then** a line gives the sets logged out of the sets planned, the average RPE, and, for a finished session, the minutes it took (spec 98, FR-013a).
4. **Given** a session whose 24 hours have not ended, **When** it is shown, **Then** it says until when the client can still change it; a locked session is labelled Locked (FR-004).
5. **Given** a missed session that was never started, **When** it is chosen, **Then** it says the session was not started, with no sets.
6. **Given** a trainer whose weight unit is pounds, **When** loads are shown, **Then** targets and logged loads are in pounds, converted from the kilograms stored.
7. **Given** a session started before the trainer changed the plan, **When** it is shown, **Then** it shows the name, exercises and targets the session had when it started (spec 92), not the changed plan.

---

### User Story 3 - Only the client's own trainer sees it (Priority: P1)

A trainer sees the overview and logs of their own clients only. Anyone else asking for them learns nothing, not even that the client exists.

**Why this priority**: Training logs are personal data (story acceptance criterion 4, constitution III).

**Independent Test**: As a second trainer, ask for the overview and the logs of the first trainer's client: "not found", the same as for an unknown id. As a client, ask for them: refused by role. Not signed in: asked to sign in.

**Acceptance Scenarios**:

1. **Given** a client of another trainer, **When** a trainer asks for that client's overview, logs or one of their session logs, **Then** the answer is "not found", the same as for an unknown client, and tests prove it.
2. **Given** a session log id of a client who is not the trainer's, asked under one of the trainer's own clients, **When** the trainer asks for it, **Then** the answer is "not found".
3. **Given** a signed-in client or a visitor, **When** they ask for any of it, **Then** a client is refused by role and a visitor gets the standard unauthenticated answer.

---

### User Story 4 - Trainer sees the client's progress (Priority: P2)

The Overview also shows the client's progress, as the client sees it on their own Progress screen, for the last 4, 8 or 12 weeks.

**Why this priority**: It completes the overview (story acceptance criterion 3), but it needs the statistics of story #156, planned for a later iteration (task #108).

**Independent Test**: For the same client and the same period, the trainer's progress section and the client's Progress screen show the same numbers.

**Acceptance Scenarios**:

1. **Given** a client with logged sessions, **When** the trainer chooses 4, 8 or 12 weeks, **Then** the progress section shows the same figures the client's Progress screen shows for that period (story #156).
2. **Given** a client with nothing logged in the period, **When** the section is shown, **Then** it says so instead of empty charts.

---

### Edge Cases

- "This week" and today are the client's week and the client's local date (CONTEXT.md, Week), not the trainer's.
- The trainer moved a session to another day after the client started it: the session log keeps its own date and name. It appears in "Recent sessions" and on the Logs tab; "This week" shows the planned sessions as the client's Plan tab does (spec 98).
- The first plan week starts on a Monday that can be before the publish day: sessions on earlier days have no state, are not missed and are not listed (spec 98, FR-006).
- A session started and not finished whose 24 hours have ended is Missed, and shows the sets that were logged (spec 98, FR-006).
- A finished session still within its 24 hours can change while the trainer reads it; the trainer sees the sets as they are when the page loads.
- A session of seconds-based exercises shows seconds, not repetitions, and no load when none was planned or logged.
- The longest plan (16 weeks of at most 6 sessions, spec 68) gives at most 96 sessions: the Logs tab lists all of them on one page, without paging.
- No error response contains server internals.

## Requirements *(mandatory)*

### Functional Requirements

**Overview** (task #105, API; task #106, tab)

- **FR-001**: A signed-in trainer MUST be able to read, for one of their own clients: this week's planned sessions with their labels, the four most recent sessions, and the quiz answers.
- **FR-002**: "This week" MUST be the client's current plan week: each planned session with its name, weekday, date and label, and, once started, the sets logged out of planned and the average RPE. A client with no published plan, or whose plan has finished, MUST have no sessions there, and the answer MUST say which.
- **FR-003**: "Recent sessions" MUST be the four most recent sessions that were started or whose day has passed on or after the day the plan was published, newest first, whatever week they are in: each with its name, date, label, sets logged out of sets planned and average RPE when a set is logged. A session that was never started counts its planned sets and has no average RPE.
- **FR-004**: Every session the trainer sees MUST carry one label, derived when read from its session log, the calendar and the client's local date, by the session states of spec 98 (FR-006) and the lock of spec 101:
  - **Done**: finished, and its 24 hours have not ended;
  - **Locked**: finished, and its 24 hours have ended;
  - **In progress**: started, not finished, and its 24 hours have not ended;
  - **Missed**: as in spec 98: its day has passed, on or after the day the plan was published, and it was never started, or it was started and its 24 hours ended before it was finished;
  - **Today** and **Planned**: as in spec 98.
- **FR-005**: The quiz answers MUST be the client's submitted answers, the same as spec 85 shows beside a draft; a client who has not submitted the quiz MUST have none, and the answer MUST say so rather than fail.
- **FR-006**: The Overview tab MUST show, as on the board "Trainer · client overview": "This week" with its seven-day strip and session rows, "Recent sessions" with "All logs", and "Quiz answers", each with its empty state (User Story 1). A label MUST be shown as text, never by colour alone.
- **FR-007**: The average RPE MUST be the mean of the logged sets' RPE, shown to one decimal, as in spec 98 (FR-008).

**Logs** (task #107)

- **FR-008**: A signed-in trainer MUST be able to read the list of one of their own clients' sessions: every session log, and every planned session that is Missed without a session log, newest first, each with name, date and label (FR-004).
- **FR-009**: A signed-in trainer MUST be able to read one session of one of their own clients: its name and date, label, sets logged out of sets planned, average RPE, the minutes it took when finished, and, while its 24 hours last, when they end. For each exercise in its order: the exercise's name and measure, and for each planned set its number, its target (repetitions or seconds, load, RPE) and the logged load, repetitions or seconds and RPE, or that it was not logged.
- **FR-010**: Targets MUST be those the session had when it started (spec 92); loads MUST be shown in the trainer's weight unit (spec 42), converted from kilograms.
- **FR-011**: The Logs tab MUST show, as on the board "Trainer · client logs", the list of sessions beside the chosen session's sets; a label MUST be shown as text, never by colour alone, and RPE values use the design system's RPE colours with the number.

**Client's note** (task #261)

- **FR-012**: The chosen session on the Logs tab MUST show the note the client wrote for their trainer, when there is one (spec 98, FR-013a: "the trainer reads it in story #104"); a session without a note shows nothing in its place.

**Progress** (task #108)

- **FR-013**: The Overview MUST show the client's progress for the last 4, 8 or 12 weeks, chosen by the trainer, with the same figures, periods and rules as the client's Progress screen of story #156, read through that story's statistics for trainers (task #158); the numbers MUST equal the client's for the same period.

**Access**

- **FR-014**: Everything in this spec MUST answer "not found" for a client who is not the trainer's own, the same as for an unknown client, and a session log that is not that client's MUST answer "not found" too. A client MUST be refused by role and a visitor MUST get the standard unauthenticated answer. Tests prove each.
- **FR-015**: Nothing in this spec changes data: reading the overview and the logs MUST NOT start, finish or change a session, and MUST NOT mark anything as seen.

**Quality**

- **FR-016**: Every new text MUST exist in English and Polish, with Polish plural forms; numbers, dates and units follow the trainer's language and settings.
- **FR-017**: The new tab contents MUST be built from Kinetiq tokens and shadcn components, designed at 1280 px with trainer table density, in light and dark, with visible focus, keyboard use and a name for every control.
- **FR-018**: While a tab's data loads it MUST show a skeleton of its layout; on failure a translated message and "Try again". An unknown or foreign client keeps the client page's own "not found" state (spec 35).

### Public endpoints

None. Every operation needs a signed-in trainer.

### Key Entities

- **Client overview**: For one client: this week's planned sessions with their labels, the four most recent sessions and the quiz answers. Derived when read, not stored.
- **Client session**: One session of the client as the trainer sees it in a list: name, date, label (FR-004), sets logged out of planned and average RPE. Derived from a session log, or from the plan for a session never started.
- **Session log**: As in spec 92 and spec 98 (its sets, the targets copied at its start, its finish time and the client's note). Read, never changed, by this story.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A trainer sees this week's sessions, the last four sessions and the quiz answers of a client by opening the client's page, without another step.
- **SC-002**: From the Overview a trainer reaches the sets of a given past session in two clicks: "All logs", then the session.
- **SC-003**: In tests with a fixed clock, every label (Done, Locked, In progress, Missed, Today, Planned) is derived correctly on both sides of each boundary (the start of the day, the publish day, the finish, the end of the 24 hours), 100% of the time.
- **SC-004**: In tests, sets logged out of planned and the average RPE equal the figures worked out by hand from the logged sets in 100% of cases, including a session never started and one with a single set.
- **SC-005**: Another trainer's client, and a session log of a client who is not the trainer's, answer "not found" in 100% of tests.
- **SC-006**: For the same client and period, the trainer's progress figures equal the client's in 100% of compared cases (task #108).
- **SC-007**: The new tab contents pass WCAG 2.2 AA checks for contrast, labels, focus order and visible focus, in light and dark, at 1280 px.
- **SC-008**: Every new text exists in English and Polish, and no error response of this story contains server-internal text.

## Departures from the story, the boards and earlier specs

| Source | It says | This spec | Why |
|---|---|---|---|
| Board "Trainer · client overview" | Tiles: sessions over 4 weeks, average RPE over 2 weeks, best load, readiness today | Built only as far as they are the client's progress figures, by task #108 (FR-013) | Progress comes from story #156; readiness today and its chart are story #122 (and the check-in of story #109). |
| Board "Trainer · client overview" | A plan change alert with "Open" | Not built | Plan changes are story #137. |
| Board "Trainer · client overview" | Header with a status badge ("Watch"), plan week, days a week, unit, "Message" and "Edit plan" | Header unchanged (spec 35) | Client status is story #162; the header is not part of this story. |
| Board "Trainer · client overview" | Today's started session shows "in progress" next to a "Today" badge | Labelled In progress | A started session is In progress whatever its day (spec 98, FR-006). |
| Boards "Trainer · client overview" and "Trainer · client logs" | Labels Done, Locked and Missed side by side | The same labels, with one rule (FR-004): Locked is a finished session past its 24 hours | Task #107 asks for locked and missed sessions to be labelled; a finished session the client can still change is told apart from one that can no longer change. |
| Board "Trainer · client logs" | "Adjusted" label, "Synced" badge, a message icon on each set | Not built | Adjusted sessions are stories #117 and #122, syncing is story #114, asking about a set is story #172. |
| Spec 98, User Story 3 | Session states for the client only | The same states are read for the trainer, with Locked added | This story is the trainer's view spec 98 left to it. |

## Texts

The main English and Polish texts. Polish uses informal "ty" and should be confirmed by a copywriter, as the design system says (8.5).

| Where | English | Polish |
|---|---|---|
| Overview sections | This week · Recent sessions · Quiz answers | Ten tydzień · Ostatnie sesje · Odpowiedzi z quizu |
| Link to Logs | All logs | Wszystkie logi |
| Labels | Done · Locked · In progress · Missed · Today · Planned | Zrobiona · Zablokowana · W trakcie · Opuszczona · Dziś · Zaplanowana |
| Sets | {logged} of {planned} sets | Serie: {logged} z {planned} |
| Average RPE | Average RPE {value} | Średnie RPE {value} |
| Figures in the Overview's rows | {logged}/{planned} sets · RPE {value} | Serie: {logged}/{planned} · RPE {value} |
| No plan | No published plan yet. | Brak opublikowanego planu. |
| Plan finished | The plan has finished. | Plan się zakończył. |
| No sessions | No sessions yet. | Brak sesji. |
| No quiz answers | No quiz answers yet. | Brak odpowiedzi z quizu. |
| Not started | Not started | Nie rozpoczęto |
| Set not logged | Not logged | Nie zapisano |
| Still changeable | {client} can still change these logs until {time}. | {client} może jeszcze zmieniać te logi do {time}. |
| Logs table | Exercise · Set · Target · Load · Reps · Seconds · RPE | Ćwiczenie · Seria · Cel · Ciężar · Powt. · Sek. · RPE |
| Client's note | Note from {client} | Notatka od: {client} |
| Load failed | The error's message, as on the client page's other tabs, and "Try again" | Komunikat błędu, jak na innych zakładkach strony klienta, i „Spróbuj ponownie” |

## Assumptions

- "Recent" means the four latest sessions, the number on the board, whatever week they are in; the current week's past sessions can therefore appear both in "This week" and in "Recent sessions", as on the board.
- The Logs tab lists the sessions of the client's current plan and every session log they have. A client has one plan at a time; earlier plans of a client who changed trainer are story #180.
- The newest session is shown when the Logs tab opens; which session is chosen is not kept in the address.
- The time a session locks is shown in the trainer's own time zone; days and dates of sessions are the client's local dates (spec 98).
- The four most recent sessions and the Logs list come from the same rule (FR-004, FR-008), so a session has the same label in both.
- Progress (task #108) waits for story #156's statistics for trainers (#158) and its chart component (#159); its figures and their rules are those that story defines.
- Nothing is stored, so this story needs no migration and no new demo entity. Seeing each label in the browser needs session logs of different ages, which `quickstart.md` explains, as specs 98 and 101 do.
