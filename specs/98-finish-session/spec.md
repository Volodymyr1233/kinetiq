# Feature Specification: Client finishes a session

**Feature Branch**: `feat/98-finish-session`

**Created**: 2026-11-08

**Status**: Draft

**Input**: Story #98 "Client finishes a session" and its tasks #99 (finish API and session states) and #100 (session done screen and states on Plan and Today). Screens: Kinetiq Screens, boards "Client · Session done" (`client-session-done`), "Client · Session logger" (the "Finish session" footer), "Client · Plan" (state badges and week strip) and "Client · Today". Design: the Kinetiq design system (badges, metric cards, alert dialog, footer actions). Builds on spec `92-session-logging` (the session log, which already holds a finish time that nothing sets), spec `101-log-edit-window` (the 24 hours, the locked logger, "Edit log" and "View log" on the Plan tab), spec `89-todays-session` (Today) and spec `75-client-plan` (the Plan tab). The story, the tasks and the boards are starting points; every place this spec departs from them or from an earlier spec is listed under "Departures from the story, the boards and earlier specs".

## Clarifications

### Session 2026-11-08

- Q: A session was started but not finished and its day has passed (started 23:50, now 00:10): Missed at once, or In progress while its 24 hours last? → A: In progress until its 24 hours end, then Missed if never finished. A session never started is Missed as soon as its day has passed (FR-006).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Client finishes a session and sees what they did (Priority: P1)

At the end of training the client chooses "Finish session" in the logger. The session is marked finished, and a summary shows how many sets they logged, the total volume and the average RPE. They can go back to Today or message their trainer from there.

**Why this priority**: This is the promise of the story: "I know it counted". Everything else (states, access to the summary) hangs on the session being finished.

**Independent Test**: Log 3 sets of a session (100 kg × 5 at RPE 7, 100 kg × 5 at RPE 8, 102.5 kg × 5 at RPE 9) and finish it: the summary says 3 of the planned sets, a volume of 1,512.5 kg, and an average RPE of 8.0. Finish it again: the same summary, and the finish time does not move.

**Acceptance Scenarios**:

1. **Given** a started session with at least one logged set, **When** the client chooses "Finish session", **Then** the session is finished and the summary screen shows the sets logged out of the sets planned, the total volume and the average RPE.
2. **Given** a session in which every planned set is logged, **When** the client chooses "Finish session", **Then** the session is finished at once, with no question.
3. **Given** a session with planned sets not logged, **When** the client chooses "Finish session", **Then** they are asked to confirm, with the number of sets not logged, and can finish anyway or keep logging; nothing changes until they confirm.
4. **Given** a started session with no set logged, **When** the logger is shown, **Then** "Finish session" cannot be used and says in words that at least one set must be logged first.
5. **Given** a finished session, **When** the client finishes it again, **Then** the answer is the same as the first time, the finish time does not change and nothing else changes.
6. **Given** the connection fails while finishing, **When** the client tries again, **Then** the session is finished once and the summary shows; their entries are kept.
7. **Given** a session that is not the client's, **When** they try to finish it, **Then** the answer is "not found", the same as for an unknown session, and nothing changes.
8. **Given** a session past its 24 hours (locked) that was not finished, **When** the client tries to finish it, **Then** it is refused with the lock code of spec 101 and nothing changes.

---

### User Story 2 - The summary matches the sets, now and later (Priority: P1)

The numbers on the summary come from the sets as they are logged. Because the client can still change a set for 24 hours after the start (spec 101), even after finishing, the summary always shows the current numbers, never a figure frozen at the moment of finishing.

**Why this priority**: A number that disagrees with the logged sets is worse than none (task #100: "Summary numbers match the logged sets").

**Independent Test**: Finish a session, open it from the Plan tab within its 24 hours, change one set's load and RPE, and open the summary again: volume and average RPE reflect the change.

**Acceptance Scenarios**:

1. **Given** a finished session, **When** the client changes a logged set within the 24 hours, **Then** the next summary shows the changed numbers.
2. **Given** a finished session, **When** the client opens its logger, **Then** the footer offers "View summary" in place of "Finish session", and it opens the current summary.
3. **Given** exercises measured in seconds, or sets without a load, **When** volume is counted, **Then** they add nothing to the volume but count as sets and in the average RPE.
4. **Given** a client who uses pounds, **When** the summary shows the volume, **Then** it is in their unit, converted from the kilograms stored.
5. **Given** a session finished with a single logged set, **When** the summary shows, **Then** it shows that set's values without error (no division by zero, no "NaN").

---

### User Story 3 - Client sees each session's state on Plan and Today (Priority: P1)

Every session on the Plan tab shows its state: **Done**, **In progress**, **Missed**, **Today** or **Planned**. Today shows the same state for today's session and offers the right action for it: "Start session", "Continue session" or the finished session's summary.

**Why this priority**: The second acceptance criterion of the story, and the way the client learns that a session counted or was missed.

**Independent Test**: With a fixed clock, a plan published on Wednesday and sessions on Monday, Wednesday and Friday: the Monday of that week shows no state, the Wednesday session is Today, the Friday session is Planned; after the Wednesday session is started it is In progress, after it is finished it is Done; on Thursday, a Wednesday session that was never started is Missed; one started on Wednesday at 18:00 and not finished is still In progress on Thursday at 17:59 and Missed from Thursday at 18:00.

**Acceptance Scenarios**:

1. **Given** a session that is finished, **When** its card is shown, **Then** it is **Done**, whatever day it is on.
2. **Given** a session that is started, not finished and not locked (within 24 hours of its start), whatever day it is on, **When** it is shown, **Then** it is **In progress**.
3. **Given** a session whose day is today and that was not started, **When** it is shown, **Then** it is **Today**.
4. **Given** a session whose day is in the future, **When** it is shown, **Then** it is **Planned**.
5. **Given** a session whose day has passed and that is not finished, either never started or started and now locked, and whose day is on or after the day the plan was published, **When** it is shown, **Then** it is **Missed**.
6. **Given** a session whose day has passed and is before the day the plan was published (the first plan week begins on a Monday that can be before the publish day), **When** it is shown, **Then** it has no state and is not counted as missed.
7. **Given** an In progress session of an earlier day, **When** the client opens its logger from the Plan tab (spec 101) and finishes it, **Then** it becomes **Done**; a Missed session cannot be finished any more, because it is locked or was never started.
8. **Given** the client's time zone, **When** a day is decided as today, past or future, **Then** it is the client's local date, as for Today (spec 89).
9. **Given** any week of the plan, past, current or future, **When** the Plan tab shows it, **Then** each session of that week, and the day marks of its week strip, show the state of that date.
10. **Given** a client who has finished sessions, **When** they open the Plan tab, **Then** the week's summary line counts the sessions done out of the sessions of the week.
11. **Given** Today shows today's session, **When** it is shown, **Then** it carries its state; for **Done** it also offers "View summary" and says the session is done. The "Start session" action itself is not changed by this story (see Assumptions).

---

### User Story 4 - Client opens a finished session's summary later (Priority: P2)

The client can see the summary of a finished session again from Today (today's session) and from the opened card of a finished session on the Plan tab.

**Why this priority**: The summary is the proof that a session counted; it must not vanish when the screen is left. Less urgent than finishing itself.

**Independent Test**: Finish a session, leave the summary, open Today: the session shows as done with "View summary", which opens the same summary. Open the same session's card on the Plan tab: "View summary" is offered there too.

**Acceptance Scenarios**:

1. **Given** a finished session, **When** the client opens its card on the Plan tab, **Then** it offers "View summary", next to the action of spec 101 ("Edit log" or "View log").
2. **Given** the summary of a session that is not finished, **When** the client opens its address, **Then** no figures are shown and they are sent to the logger (which shows today's session, or Today when there is none, until earlier sessions can be opened, task #256).
3. **Given** a session of another client, **When** a client asks for its summary, **Then** the answer is "not found", the same as for an unknown session.
4. **Given** the summary screen, **When** the client reloads it or opens its address later, **Then** it shows the same session's current summary.

---

### Edge Cases

- A session started at 23:50 and left unfinished stays In progress after midnight, until 23:50 the next day, when it locks and becomes Missed (spec 101). Finishing it before then makes it Done.
- A session finished after midnight on the day after it started is Done, and counts for the date it started on.
- The first plan week starts on a Monday that can be before the day the plan was published: sessions on days before the publish day have no state; the publish day itself counts as a normal day (Today, then Done or Missed).
- A trainer changes the plan after a session was started: the session's state follows the date of its session log, not the new plan. A started session keeps its own state, with its copied name; a changed plan only changes the sessions that were not started.
- A plan that finished: its past sessions keep their states; there is nothing Planned or Today.
- A week shown on the Plan tab in the future has only Planned sessions; a past week has Done and Missed.
- Two finish requests at the same moment finish the session once; the second is told to repeat and then answers as a repeat.
- The device's clock may differ from the server's; the server decides the finish time and the states.
- A finished session always has at least one logged set: finishing needs one (FR-001), and a logged set cannot be removed, only changed (spec 92).
- No error response contains server internals.

## Requirements *(mandatory)*

### Functional Requirements

**Finish and session states** (task #99)

- **FR-001**: A signed-in client MUST be able to finish their own started session. Finishing records the time (the server's) and is allowed only when at least one set of the session is logged; otherwise it is refused with a stable code and nothing changes.
- **FR-002**: Finishing a finished session MUST answer as the first time and change nothing, including the finish time (the same rule as a repeated set, spec 92, FR-007). Two simultaneous finishes MUST finish the session once: the second request is told to repeat, and the repeat answers as the first time.
- **FR-003**: Finishing MUST be refused with the lock code of spec 101 when the session is locked and not yet finished. A session finished within its 24 hours stays finished after the lock.
- **FR-004**: A session that is not the client's, or does not exist, MUST answer "not found" in the same way, and nothing changes. A trainer MUST be refused by role and a visitor MUST get the standard unauthenticated answer. Tests prove each. The client is taken from the sign-in, never from the request.
- **FR-005**: Finishing MUST NOT end or shorten the 24 hours of spec 101 (FR-006 there): sets of a finished session can be logged and changed until the lock.
- **FR-006**: The state of a session on a date MUST be derived when it is read, from the plan, the session logs, the client's local date and the day the plan was published. It is one of Done, In progress, Missed, Today, Planned, or none:
  - **Done**: the session log of that date is finished.
  - **In progress**: the session log is started, not finished and not locked, whatever its date.
  - **Missed**: the date has passed, it is on or after the day the plan was published, and there is no finished session log for it: either none was started, or it is started and locked.
  - **Today**: the date is today and no session log exists for it.
  - **Planned**: the date is in the future.
  - **None**: the date is before the day the plan was published.
  A Missed state MUST never apply before the plan was published (tests with a fixed clock prove each state, each boundary and the first plan week).
- **FR-007**: A signed-in client MUST be able to read, for each planned session of a plan week and for today's session, its date, its state and, when it was started, a way to open its session log. A session log is matched to the plan's session by its date.
- **FR-008**: The summary MUST be derived from the logged sets when read, never stored, so it always matches the sets: **sets logged** out of **sets planned**; **total volume**, the sum of load × repetitions over the logged sets that have a load and are measured in repetitions; **average RPE**, the mean of the RPE of the logged sets, shown to one decimal. A client MUST be able to read the summary of their own finished session. The summary of a session that is not finished is refused with a stable code. The summary of another client's session, or of an unknown one, answers "not found" in the same way.
- **FR-009**: `CONTEXT.md` MUST gain the terms **Session state** (Done, In progress, Missed, Today, Planned) and **Session summary**, and its entry for **Missed session** MUST be kept consistent with FR-006.

**Finish in the logger** (task #100)

- **FR-010**: The logger's footer MUST offer "Finish session" for a started session that is not locked, with a text of how many sets are logged out of the sets planned. It MUST be unavailable, with its reason in words, while no set is logged. For a finished session that is not locked it MUST offer "View summary" instead. A locked session shows no footer action (spec 101).
- **FR-011**: When planned sets are not logged, "Finish session" MUST first ask for confirmation naming the number of sets not logged, with "Finish session" and "Keep logging"; when all are logged it MUST finish at once. The confirmation MUST be an accessible dialog (focus moves into it, returns to the action on close, closes with Escape).
- **FR-012**: A failed finish MUST show a translated message and let the client try again, keeping the logger as it was (FR-002 makes a repeat safe). A finish refused with the lock code MUST turn the logger read-only as in spec 101 (FR-012 there).

**Session done screen** (task #100)

- **FR-013**: After finishing, the client MUST see the session done screen at an address of its own for that session, without the tab bar: the title ("{session name} done"), the date, the sets logged out of planned, the total volume in the client's unit, and the average RPE, each as a labelled figure; the time (finish minus start, in whole minutes), a note field, then "Back to today" and "Message {trainer}" (the conversation with their trainer, spec 58). The screen MUST also be reachable by its address later (FR-008).
- **FR-013a**: The time MUST be the whole minutes from when the session started (when the logger first opened it) to when it was finished, at least 1; it is told as an approximation by its label ("Time in session"). The note MUST be optional free text of up to 500 characters, saved with "Save note", kept on the session log, changeable until the session locks (spec 101) and removable by saving it empty; it is shown to the client only in this story, and the trainer reads it in story #104. Saving it for another client's session answers "not found"; for a session not finished or locked it is refused with a stable code. The note MUST NOT be sent to an LLM (constitution III).
- **FR-014**: The summary MUST show a session of seconds or bodyweight sets without error: a volume of nothing to count is shown as "—", never as 0 kg, and a session with no logged set cannot have a summary.
- **FR-015**: While the summary loads it MUST show a skeleton of its layout; on failure a translated message and "Try again"; for a session that is not found, the standard not-found page of the client area.

**States on Plan and Today** (task #100)

- **FR-016**: On the Plan tab, each session card MUST show its state as a badge with a text (never colour alone), and each day of the week strip MUST mark Done, Missed and Today by symbol and accessible text, as the board shows. The week's summary line MUST read "{done} of {sessions} sessions done" for the week shown. A session with no state shows no badge.
- **FR-017**: The opened card of a finished session MUST offer "View summary" next to the action of spec 101 ("Edit log" or "View log"); for a session with no session log it offers neither.
- **FR-018**: On Today, the session card MUST show the state. For Done it MUST replace the start action with "View summary" (48 px) and a line saying the session is done. For Today and In progress the start action stays as spec 89 and the task that enables it define it; this story does not change it.
- **FR-019**: A rest day and a finished plan MUST show as before (spec 89).

**Quality**

- **FR-020**: Every new text MUST exist in English and Polish, with Polish plural forms; numbers and units follow the UI language and the client's settings.
- **FR-021**: New screens and states MUST be built from Kinetiq tokens and shadcn components, designed at 375 px, in light and dark, with visible focus, keyboard use and a name for every control; at 375 px and at 320 px (200% zoom) the page MUST NOT scroll sideways. State is never told by colour alone.
- **FR-022**: Demo data MUST let each state be seen in the browser (Done, In progress, Missed, Today, Planned and no state) without waiting for a day to pass.

### Public endpoints

None. Every operation needs a signed-in client.

### Key Entities

- **Session log**: As in spec 92, which already holds when a session finished; this story sets it, and adds an optional note of the client's.
- **Session state**: Done, In progress, Missed, Today, Planned, or none, for one session on one date. Derived on read, not stored.
- **Session summary**: Sets logged out of planned, total volume and average RPE of one session log. Derived on read, not stored.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In tests, the summary's sets, volume and average RPE equal the figures computed by hand from the logged sets in 100% of cases, including seconds-based and bodyweight sets, a single set and a change after finishing.
- **SC-002**: In tests with a fixed clock, each of the five states and "none" is returned correctly on both sides of every boundary (the start of the day, the publish day, the finish), and Missed never appears before the publish day, 100% of the time.
- **SC-003**: Finishing the same session twice leaves one finish time and one identical answer, and two requests at the same moment leave one finish time, with the second answering as a repeat after one retry, in 100% of tests; another client's session answers "not found" 100% of the time.
- **SC-004**: A client with every planned set logged reaches the summary with one tap from the logger; with sets missing, with two (finish, then confirm).
- **SC-005**: From Today, a client reaches the summary of today's finished session in one tap.
- **SC-006**: The new screens pass WCAG 2.2 AA checks for contrast, labels, focus order, visible focus and touch size, in light and dark, at 375 px.
- **SC-007**: Every new text exists in English and Polish.
- **SC-008**: No error response of this story contains server-internal text.

## Departures from the story, the boards and earlier specs

| Source | It says | This spec | Why |
|---|---|---|---|
| Board "Session done" | Shows time, a PR badge, "weeks in a row", a "Synced" badge and "Note for Marek" | Adds time and the note to the story's three figures; PR badge, streak and sync badge are not built | Time and note were asked for by the product owner. PRs and streaks have no story; the sync badge belongs to offline (#114). Time counts from when the logger opened (see FR-013a). The trainer reading the note is story #104. |
| Board "Session done" | "Average RPE 7.8, Target 7–8" | Average RPE without a target line | Sessions have per-set targets; one target for the whole session is not defined. |
| Board "Session logger" | "Finish session" is always available | Available once a set is logged; confirms when planned sets are missing | Finishing cannot be undone from the screen; a tap by mistake would record a session without sets or with half of them. |
| Board "Session done" | Volume "6,840 kg" | Volume counts sets with a load measured in repetitions; none to count shows "—" | A time-measured set has no volume; showing 0 kg would claim the client lifted nothing. |
| Spec 89 (Departures) | A follow-up task under #89 turns "Start session" on | Not done here; owned by someone else | Until it lands, the logger is reached by its address; the Done state and its summary link do not depend on it. |
| Spec 75, board "Plan" | Sessions marked done or missed are not shown | Shown (FR-016) | This is task #100. |
| Spec 101, FR-006 | Finishing leaves logging open | Kept (FR-005) | The summary is therefore derived, not stored (FR-008). |
| CONTEXT.md, Missed session | "A session whose day has passed without the client finishing it" | Kept, plus "its day is on or after the day the plan was published", and a started session counts as missed only once locked | The story's acceptance criterion, and the 24 hours of spec 101: a session that can still be finished is not yet missed. |

## Texts

The main English and Polish texts. Polish uses informal "ty" and should be confirmed by a copywriter, as the design system says (8.5).

| Where | English | Polish |
|---|---|---|
| Finish action | Finish session | Zakończ sesję |
| Finish, nothing logged | Log at least one set to finish. | Zapisz co najmniej jedną serię, aby zakończyć. |
| Logged count | {n} of {total} sets logged | Zapisano {n} z {total} serii |
| Confirm title | Finish with sets not logged? | Zakończyć mimo niezapisanych serii? |
| Confirm text | {n} sets are not logged. You can still log them for 24 hours after the session started. | Niezapisane serie: {n}. Możesz je jeszcze zapisać przez 24 godziny od rozpoczęcia sesji. |
| Confirm buttons | Finish session / Keep logging | Zakończ sesję / Zapisuj dalej |
| View summary | View summary | Zobacz podsumowanie |
| Done screen title | {name} done | {name} zakończona |
| Done screen figures | Sets · Total volume · Average RPE | Serie · Łączna objętość · Średnie RPE |
| Done screen actions | Back to today / Message {trainer} | Wróć do dziś / Napisz do: {trainer} |
| Volume, none | — | — |
| States | Done · In progress · Missed · Today · Planned | Zrobiona · W trakcie · Opuszczona · Dziś · Zaplanowana |
| Today actions | Start session / Continue session | Zacznij sesję / Kontynuuj sesję |
| Week line | {done} of {n} sessions done | Zrobione sesje: {done} z {n} |
| Time label | Time in session | Czas sesji |
| Note label | Note for {trainer} | Notatka dla: {trainer} |
| Note help | Optional. {trainer} will see it with this session. | Opcjonalna. {trainer} zobaczy ją przy tej sesji. |
| Save note | Save note | Zapisz notatkę |
| Finish failed | Could not finish the session. Try again. | Nie udało się zakończyć sesji. Spróbuj ponownie. |

## Assumptions

- The task that enables "Start session" on Today is another person's. This story is built and checked with the logger opened by its address, and with demo session logs that show each state on Today and Plan.

- A session is finished by the client's own action only; nothing finishes a session by itself, and a session cannot be unfinished.
- Finishing is allowed for any started session of the client within its 24 hours, including an earlier day's In progress session opened from the Plan tab (spec 101).
- A plan has at most one session a day (spec 68), so a session log of a date belongs to that date's session of the plan.
- "The day the plan was published" is the client's local date of the moment of publishing; the plan weeks are numbered from the week of that day (CONTEXT.md, Week).
- Where the plan's state is computed, so that the plan and session-log features keep their boundaries and form no cycle (constitution I), is a decision for the plan.
- The trainer's view of finished sessions and the label of a locked session are stories #104 and task #107; this story provides what they read.
- Logging and finishing without a connection (retry queue) is story #114. A repeated finish is safe, which that story needs.
- The weight unit and time zone come from the client's settings (spec 42); loads are stored in kilograms.
- Plans published before this story have no recorded publish day; the Monday of their first week stands in for it, so in week 1 of such a plan the days before the real publish day can show Missed. New plans record the real day.
- "Session done screen" is the screen after finishing (the spec's summary screen); both names mean the same screen.
- Demo data: the existing demo clients (specs 89, 92) have no logs; this story adds demo session logs for one client that show each state across the current and the previous week, in the local-only Flyway location. Logs that must be inside their 24 hours, or locked, depend on the day the data is loaded; `quickstart.md` explains how to see each, as in spec 101.
