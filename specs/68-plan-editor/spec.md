# Feature Specification: Trainer builds, edits and publishes a plan

**Feature Branch**: `feat/68-plan-editor`

**Created**: 2026-11-01

**Status**: Draft

**Input**: Story #68 "Trainer builds, edits and publishes a plan" and its tasks #69 (plan model), #70 (plan rules), #71 (get and save API), #72 (publish API), #73 (editor: sessions and exercises) and #74 (editor: targets, save and publish). Screens: Kinetiq Screens, board "Trainer · plan editor". Design: the Kinetiq design system. The story, the tasks and the board are starting points, not the source of truth; every place this spec departs from them is listed under "Departures from the story and the board".

## Clarifications

### Session 2026-11-01

- Q: Task #70 says "distinct weekdays". Does that mean Monday to Friday, which would make 6 sessions impossible? → A: No. Sessions may fall on any day of the week, Monday to Sunday, so 2–6 sessions on distinct days stays as written.
- Q: Can a trainer build a plan for a client who has not submitted the quiz yet? → A: No. The quiz comes first: the Plan tab says the client has not answered it, offers no "Build plan", and a save is refused with a stable code.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Trainer builds a client's plan (Priority: P1)

On the Plan tab of a client's page the trainer sets the goal and the number of weeks, adds named sessions on days of the week, and fills each session with exercises from the exercise library. Saving keeps it as a draft the client cannot see.

**Why this priority**: Nothing else in the planning and training stories can happen until a plan exists.

**Independent Test**: Open a client without a plan, build a plan with two sessions and one exercise in each, save and reload: the draft is there as saved, and the client's plan is still not visible to the client.

**Acceptance Scenarios**:

1. **Given** the trainer's client has submitted the quiz and has no plan, **When** the trainer opens the Plan tab, **Then** they see one line saying no plan exists yet and the action "Build plan".
2. **Given** that empty state, **When** the trainer chooses "Build plan", **Then** the editor opens with the goal from the client's quiz, 12 weeks and no sessions; nothing is stored until Save.
3. **Given** the editor, **When** the trainer adds a session, **Then** it gets an empty name field, which takes focus, and the first free day of the week, and it becomes the selected session.
4. **Given** a session, **When** the trainer chooses "Add exercise", **Then** a picker lists the exercise library, searchable by name and filterable by category, and only exercises the picker lists can be added.
5. **Given** the picker, **When** the trainer picks an exercise, **Then** a row is added at the end of the session with the exercise's default sets, repetitions or seconds and rest, target RPE 8 and no load; the picker stays open for the next pick, and the addition is announced.
6. **Given** a draft with valid values, **When** the trainer chooses "Save plan", **Then** it is stored as a draft, "Plan saved" is confirmed, and the unsaved-changes note is gone.
7. **Given** the trainer's client has not submitted the quiz, **When** the trainer opens the Plan tab, **Then** they see one line saying the client has not answered the quiz yet and no "Build plan"; a save sent anyway is refused with a stable code.
8. **Given** a new plan the trainer has not changed, **When** they leave the tab, **Then** nothing asks and nothing is stored, and the tab shows "Build plan" again.

---

### User Story 2 - Trainer sets the targets of each exercise (Priority: P1)

Each exercise row is edited in place: sets, repetitions or seconds, optional load, target RPE and rest. The trainer can reorder rows and remove them.

**Why this priority**: The targets are what the client will train with; a plan without them says nothing.

**Independent Test**: Change every target of a row, move it up, delete another row after confirming, save and reload: the values, the order and the deletion are all kept.

**Acceptance Scenarios**:

1. **Given** a row of an exercise measured in repetitions, **When** it is shown, **Then** its amount is in repetitions; **given** an exercise measured in seconds, **then** its amount shows a seconds unit, so a plank reads 45 s and not 45 reps.
2. **Given** a row with an empty load, **When** it is shown or saved, **Then** the load stays empty and means no added weight.
3. **Given** a trainer whose unit is pounds, **When** they see or type a load, **Then** it is in pounds; **and** a load they did not touch is stored unchanged by a save.
4. **Given** a row, **When** the trainer chooses "Move up" or "Move down" by pointer or keyboard, **Then** the row moves one place, focus stays on the moved row's control, and the move is announced to assistive technology. "Move up" is unavailable on the first row and "Move down" on the last; when a move makes the focused control unavailable, focus goes to the other arrow of that row.
5. **Given** a row, **When** the trainer chooses delete, **Then** a confirmation asks whether to delete that exercise from that session, focus starts on the safe option, and the row goes only after confirming.
6. **Given** a target outside its limits, or empty where a value is required, **When** the trainer leaves the field or chooses Save, **Then** the field is marked invalid with a message saying how to fix it; the message goes as soon as the value is valid.

---

### User Story 3 - Trainer publishes the plan (Priority: P1)

The trainer publishes a complete draft. The client can now see it, and week 1 is the week of publishing in the client's time zone.

**Why this priority**: Publishing is the point of the story: until then the client has nothing to train.

**Independent Test**: Publish a complete draft after confirming: the plan is published with week 1 at the current week in the client's time zone, a waiting client's status becomes "plan published", and a second publish is refused.

**Acceptance Scenarios**:

1. **Given** a complete draft, **When** the trainer chooses "Publish plan", **Then** a confirmation says the client sees it from this week; focus starts on the safe option.
2. **Given** that confirmation, **When** the trainer confirms, **Then** the plan is published, week 1 is the current week in the client's time zone, "Plan published" is confirmed, and the editor shows week 1 of N.
3. **Given** a draft with unsaved changes, **When** the trainer confirms publishing, **Then** the changes are saved first and published as one visible step; if the save is refused, nothing is published and the reasons show; if the save succeeds and the publish fails, the plan stays a saved draft, the changes count as saved, and the failure shows with "Publish plan" available again.
4. **Given** a draft that is not complete, **When** the trainer tries to publish, **Then** the validation summary lists what is missing and nothing is published.
5. **Given** a plan already published (a second tab, a repeated request), **When** another publish arrives, **Then** it is refused with a stable code and the plan does not change.
6. **Given** a client's Sunday evening is already Monday in UTC, or the other way round, **When** the plan is published, **Then** week 1 is computed from the client's time zone, not the server's or the trainer's.
7. **Given** a client waiting for their plan, **When** the plan is published, **Then** their status says the plan is published, so the waiting screen no longer shows.
8. **Given** the draft was saved elsewhere since this editor loaded it, **When** the trainer publishes, **Then** the publish is refused as for a stale save, nothing is published, and the trainer's edits stay on screen.

---

### User Story 4 - Trainer edits a published plan (Priority: P2)

The trainer changes a published plan and saves it. The change applies from today.

**Why this priority**: Plans get adjusted while running, but only after a first plan is out.

**Independent Test**: Change a target on a published plan and save: the plan shows the new target, stays published with the same week 1, and the current plan week is unchanged.

**Acceptance Scenarios**:

1. **Given** a published plan, **When** the trainer opens the Plan tab, **Then** the editor shows it with "week N of M"; the only save action is "Save plan", and a note says saving applies the change from today.
2. **Given** a published plan with changes, **When** the trainer saves, **Then** the plan stays published, week 1 does not move, and the plan rules for a complete plan are checked.
3. **Given** a published plan of 16 weeks in week 6, **When** the trainer shortens it below 6 weeks, **Then** the save is refused with a message saying the plan is already in week 6.
4. **Given** a plan past its last week, **When** the trainer opens it, **Then** the editor says the plan has finished after its last week; the trainer can still change its targets and save without changing its length, and can lengthen it up to 16 weeks.

---

### User Story 5 - Unsaved work is safe (Priority: P2)

Changes stay in the editor until Save. They survive switching sessions, and leaving with unsaved changes asks first.

**Why this priority**: A trainer's plan is minutes of careful work; a stray click must not lose it.

**Independent Test**: Edit a target, switch to another session and back: the edit is still there. Choose another tab of the client's page: a question asks whether to leave. Stay: the edit is still there.

**Acceptance Scenarios**:

1. **Given** unsaved changes, **When** the trainer switches sessions, **Then** all changes stay and no question is asked.
2. **Given** unsaved changes, **When** the trainer navigates anywhere in the app (another tab, the sidebar, the browser's back) or chooses to sign out, **Then** a confirmation asks whether to leave without saving, before anything happens; "Stay" keeps everything and focus starts there.
3. **Given** unsaved changes, **When** the trainer reloads or closes the browser tab, **Then** the browser's own leave warning shows.
4. **Given** no unsaved changes, **When** the trainer navigates, **Then** nothing asks; "Save plan" is disabled, with a line saying there is nothing to save.
5. **Given** the same plan was saved in another tab or by another device, **When** the trainer saves stale edits, **Then** the save is refused with a stable code, a message says the plan changed elsewhere, and the trainer's edits stay on screen.
6. **Given** a save fails for no connection or a server error, **When** the failure shows, **Then** the edits stay and the trainer can try again.

---

### User Story 6 - Only the client's trainer can reach the plan (Priority: P1)

A plan is private to the client and their trainer.

**Why this priority**: A plan is a client's personal data, and the project's rule is that another person's resource answers 404.

**Independent Test**: As another trainer, read, save and publish the plan of a client who is not theirs: all three answer 404 and change nothing.

**Acceptance Scenarios**:

1. **Given** a client of another trainer, or an unknown id, **When** a trainer reads, saves or publishes their plan, **Then** the answer is the same not-found for both, and nothing changes.
2. **Given** a signed-in client, **When** they call the trainer's plan operations, **Then** they are refused by role, even for their own plan; their own view of the plan is story #75.
3. **Given** no sign-in, **When** any plan operation is called, **Then** it gets the standard unauthenticated refusal.
4. **Given** a save naming an exercise of another trainer, or one that does not exist, **When** it arrives, **Then** it is refused with a stable code and does not reveal which of the two it was.

---

### Edge Cases

- Two sessions on the same day are refused; the day picker shows taken days as unavailable.
- Changing a session's day moves it to its place in the week; the trainer's selection stays on that session.
- Deleting a session with exercises asks first; deleting an empty session does not. The selection moves to the next session, or the previous one when it was the last.
- An empty sets, repetitions or seconds, RPE or rest field is invalid; only the load may be empty. A load of 0 is invalid: empty is how to say no added weight.
- A load is limited after conversion to kilograms, so in pounds the upper limit is 2204.4 lb, and a value that rounds to 0.0 kg (0.1 lb) is invalid. Messages name the trainer's unit.
- A client who moved to a time zone behind the one of publishing can, for a moment, be before their week 1; the current plan week is then 1, never 0.
- A seventh session cannot be added; "Add session" is disabled with a line saying why.
- The same exercise may appear twice in a session (a heavy and a light block).
- A session name of only spaces counts as empty; names are trimmed.
- Two first saves at the same moment store one plan; the other gets the "changed elsewhere" refusal.
- A trainer's own exercise that is renamed keeps its place in plans and shows the new name; its measure never changes (spec 47).
- Long Polish exercise and session names wrap; buttons are never cut.
- The exercise library's count of plans using an exercise counts only the signed-in trainer's plans, so a starter exercise never reveals another trainer's use of it.
- A request that cannot be read (an unknown day, a number sent as text) is refused as a whole with `request.invalid`, as other APIs do.
- No error response contains server internals.

## Requirements *(mandatory)*

### Functional Requirements

**Plan model** (task #69)

- **FR-001**: A client MUST have at most one current plan; storage itself MUST guarantee this, also for simultaneous saves.
- **FR-002**: A plan MUST hold its goal (one of the quiz's goals), its length in weeks, its first week once published, and its sessions. Each session holds a name, a day of the week and its ordered exercise entries. Each entry holds an exercise, sets, an amount (repetitions or seconds, as the exercise measures), an optional load, a target RPE and rest.
- **FR-003**: A plan MUST be either a draft or published. A plan with a first week is published.
- **FR-004**: Sessions MUST be ordered by their day of the week, Monday first.

**Plan rules** (task #70)

- **FR-005**: The rules that hold for every saved plan, draft or published, with a stable code each:
  - length of 4–16 weeks;
  - 0–6 sessions, on distinct days of the week (Monday to Sunday);
  - a session name of 1–60 characters after trimming;
  - 0–30 exercises per session;
  - 1–20 sets;
  - an amount of 1–3600 (repetitions or seconds);
  - a load that is empty, or above 0 up to 999.9 kg with at most one decimal;
  - target RPE of 5–10 in whole steps;
  - rest of 0–600 seconds;
  - every exercise from the trainer's exercise library (starter exercises plus their own).
- **FR-006**: The rules that hold additionally for a complete plan, checked on publishing and on saving a published plan: 2–6 sessions, each with at least 1 exercise. A draft that breaks only these can be saved.
- **FR-007**: Saving a published plan MUST NOT shorten its length to below its current plan week. An unchanged or longer length is always allowed, so a finished plan can still be saved.
- **FR-008**: Each rule MUST be enforced in the domain and proven by a unit test per rule.

**Read and save** (task #71)

- **FR-009**: The trainer MUST be able to read a client's plan: its state, goal, length, first week and current plan week, and every session with its entries, each with the exercise's name in the UI's language and its measure. The current plan week counts Monday-to-Sunday weeks from the first week in the client's time zone, starting at 1 and never below it. A current plan week above the length means the plan has finished.
- **FR-010**: Reading the plan of a client with no plan MUST tell that apart from a foreign client, without revealing anything about foreign clients.
- **FR-011**: The trainer MUST be able to save a plan as a whole: a first save creates a draft and is refused with a stable code while the client has not submitted the quiz; a later save replaces goal, length and sessions.
- **FR-012**: Saving a published plan MUST apply from today: the next read returns the plan as saved, with the same first week and current plan week. It MUST NOT touch any log.
- **FR-013**: A save based on an outdated version of the plan MUST be refused with a stable code.
- **FR-014**: Rule violations MUST give stable codes, per field where a field is at fault. Nothing is stored on a refusal.

**Publish** (task #72)

- **FR-015**: The trainer MUST be able to publish a saved draft. Its first week becomes the Monday of the current week in the client's time zone.
- **FR-016**: Publishing a published plan MUST be refused with a stable code; publishing with no plan answers not found; publishing an incomplete draft is refused with the codes of FR-006; publishing based on an outdated version is refused like a stale save (FR-013), so a trainer never publishes content they have not seen.
- **FR-017**: After publishing, the client's quiz status MUST say the plan is published (spec 54, FR-011).

**Access**

- **FR-018**: Only the client's current trainer can read, save or publish. A foreign or unknown client answers 404 the same way; a client account is refused by role; no sign-in gets the standard refusal. Tests prove each.

**Editor** (tasks #73 and #74)

- **FR-019**: The editor MUST live on the Plan tab of the client's page, at its own address. It shows the goal, the length, the plan's state and, for a published plan, "week N of M".
- **FR-020**: The editor MUST show one selectable item per session with its letter (A, B, C in week order), name and day, and for the selected session a name field, a day field, a delete action and the exercise table. The first session is selected on opening. With no sessions it shows one line and "Add session"; a session with no exercises shows one line and "Add exercise".
- **FR-021**: The table MUST allow editing sets, amount, load, RPE and rest in place, with a visible label for each column and the weight unit of the trainer on the load. Each field, "Move up", "Move down" and delete has an accessible name that includes the exercise and its row number, because the same exercise can appear twice.
- **FR-022**: Reordering MUST work by pointer and keyboard with "Move up" and "Move down" controls on each row, with the focus behaviour of User Story 2, scenario 4. Dragging is not offered.
- **FR-023**: The picker MUST search by name and filter by category over the trainer's library, in the UI's language. It shows a skeleton while loading, a translated message and "Try again" on failure, and an empty state when nothing matches. It stays open after a pick, announces each addition, and closes with "Done" or Esc.
- **FR-024**: The selected session MUST show its number of exercises, its total sets and an estimated time in whole minutes: the sum over its entries of sets × (rest + work), where work is the amount in seconds, or 4 seconds per repetition. A value that is not a valid number counts as 0.
- **FR-025**: Changes MUST stay in the editor until Save and survive switching sessions. The editor shows "Unsaved changes" while the editor's content differs from the last saved plan, so undoing an edit clears it. A new plan counts as the empty starting point: it has unsaved changes only after a first edit.
- **FR-026**: Leaving with unsaved changes MUST ask first, for every navigation in the app and for signing out, and use the browser's warning on reload or close.
- **FR-027**: Deleting an exercise MUST ask first; deleting a session that holds exercises MUST ask first.
- **FR-028**: A draft, including a new plan not yet saved, MUST offer both "Save plan" and "Publish plan"; a published plan MUST offer only "Save plan". "Save plan" is unavailable when there are no unsaved changes, with a line saying so. "Publish plan" stays available, because the summary says what is missing. Publishing asks for confirmation.
- **FR-029**: On a failed attempt to save or publish, the editor MUST show a validation summary above the sessions and move focus to it. A save lists breaches of FR-005; a publish, or a save of a published plan, also lists breaches of FR-006. Each line names its problem and, where it has them, the session and exercise. A line with a field links to it: the link selects that session and moves focus to the field. A missing session links to "Add session", and an empty session to its "Add exercise". The summary updates as problems are fixed and goes when none are left. A session holding a problem is marked with an icon and accessible text, not colour alone. Refusals from the server are translated by their code and shown the same way, with a link when the refusal names a field and without one otherwise; server text is never shown.
- **FR-030**: While a save or publish is on its way, its button MUST show a spinner and a present-tense label, keep its width and block repeat clicks.
- **FR-031**: While the plan loads the editor MUST show a skeleton of its layout; on failure a translated message and "Try again".

**Other screens**

- **FR-032**: The client list and the client page MUST show where the client stands with the plan: no quiz yet, plan to review (quiz submitted, no plan), draft or published. The goal they show is the plan's when a plan exists, otherwise the quiz's.
- **FR-033**: The exercise library MUST show the real number of the trainer's own plans using each exercise, replacing today's constant 0.

**Quality**

- **FR-034**: Every text MUST exist in English and Polish, with Polish plural forms; numbers, dates and units follow the UI language.
- **FR-035**: The screens MUST be built from Kinetiq tokens and shadcn components in compact table density, in light and dark, designed at 1280 px. At 375 px and at 320 px (200% zoom) the page MUST NOT scroll sideways: the table scrolls within its box, and the actions and sessions wrap. Focus is never covered.
- **FR-036**: Demo data MUST include a client with a draft and a client with a published plan past week 4.
- **FR-037**: Focus MUST be managed when the focused control goes away. Every confirmation starts on its safe option and returns focus to the control that opened it when cancelled. After deleting a row, focus goes to the next row's delete control, else the previous row's, else "Add exercise". After deleting a session, it goes to the selected session's item, else "Add session". After closing the picker, it returns to "Add exercise". After publishing, it goes to "Save plan".

### Public endpoints

None. Every plan operation needs a signed-in trainer (FR-018).

### Key Entities

- **Plan**: The training prescribed for one client: goal, length in weeks, first week once published. Draft or published. One per client. Its trainer is the client's trainer, not a value of its own.
- **Session**: A named training occasion on one day of the week within a plan.
- **Exercise entry**: One exercise within a session in a given place, with its targets: sets, repetitions or seconds, optional load, target RPE and rest. It refers to an exercise of the library, which gives it its name and measure.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A trainer builds and publishes a two-session plan with three exercises per session in under 5 minutes.
- **SC-002**: In tests, every rule of FR-005 to FR-007 refuses a breaking plan, and 0 refused saves change what is stored.
- **SC-003**: In tests, 100% of read, save and publish attempts on a foreign client answer as not found and change nothing.
- **SC-004**: 0 unsaved changes are lost by switching sessions; 100% of in-app navigations with unsaved changes ask first.
- **SC-005**: Week 1 matches the client's time zone at both sides of the Sunday-to-Monday boundary, proven with a fixed clock.
- **SC-006**: Every text of the editor exists in English and Polish.
- **SC-007**: The editor passes WCAG 2.2 AA checks for contrast, labels, focus order, visible focus and dragging alternatives, in light and dark.
- **SC-008**: No error response of this story contains server-internal text.

## Departures from the story and the board

| Source | It says | This spec | Why |
|---|---|---|---|
| #70 | "distinct weekdays" | Distinct days of the week, Monday to Sunday | `CONTEXT.md` defines a week as Monday to Sunday, the quiz accepts 6 days a week, and the design system's week strip has 7 cells. Mon–Fri only would make 6 sessions impossible. |
| #69 | Session has an order | Sessions are ordered by day | With distinct days, a stored order could only contradict the day. |
| #69 | Plan has a status | Published is known from the first week (FR-003) | Two values saying one thing can disagree. |
| #70 | RPE 5–10; board's field takes decimals | Whole steps | #92 logs RPE 5–10 in whole steps, and the client's picker has no half steps. |
| Story | "2–6 sessions" on every plan | Drafts may be incomplete; complete on publish | #79 stores an empty draft when drafting fails, and a trainer must be able to save part-way. |
| Story | "1 rep or second" lower bound only | Upper limits added | Same limits as the exercise library; a boundary needs them. |
| Board | Week pills (13–16) | Removed; "week N of M" shown | A plan's sessions repeat every week, so the pills would show the same content. Weeks that differ arrive with story #143. |
| Board | Draft has Publish only | Draft has Save and Publish | The story keeps changes until Save, and part-way work needs saving. |
| Board | Drag rows, grip hidden from assistive technology | Move up and Move down | WCAG 2.2 criterion 2.5.7 needs a non-dragging way; this one needs no new dependency. |
| Board | "Reps" column shows a plank as 45 | Unit follows the exercise's measure | 45 repetitions of a plank is wrong. |
| Board | Load 0 for a plank | Empty load | The story says load is optional; 0 and empty would mean the same. |
| Board | No controls for goal, length, adding, renaming or moving sessions | Added | The story needs them. |
| Board | Estimated time = sets × 3.4 min | From each set's rest plus a work time | The board's figure ignores rest. |
| Board | "gets a notice" | Not said | Notifications are story #150; the copy must not promise them. |

## Texts

The main English and Polish texts. Names are the client's as stored; Polish cannot inflect them, so its sentences put the name where the case does not change. Errors follow the design system's pattern of saying what to do. Limits in messages come from FR-005. Polish uses informal "ty" and should be confirmed by a copywriter, as the design system says (8.5).

| Where | English | Polish |
|---|---|---|
| Empty, no quiz | {name} hasn't answered the quiz yet. You can build a plan once they have. | {name}: quiz jeszcze bez odpowiedzi. Plan zbudujesz, gdy będą gotowe. |
| Empty, no plan | No plan yet. | Nie ma jeszcze planu. |
| Action | Build plan | Zbuduj plan |
| Actions | Save plan · Publish plan · Add session · Add exercise | Zapisz plan · Opublikuj plan · Dodaj sesję · Dodaj ćwiczenie |
| Loading labels | Saving · Publishing | Zapisywanie · Publikowanie |
| Toasts | Plan saved · Plan published | Plan zapisany · Plan opublikowany |
| Note | Unsaved changes | Niezapisane zmiany |
| Nothing to save | No changes to save. | Brak zmian do zapisania. |
| Published note | Saving applies the change from today. Logged sets don't change. | Zapis obowiązuje od dziś. Zalogowane serie się nie zmieniają. |
| Week | Week {n} of {m} · Finished after week {m} | Tydzień {n} z {m} · Zakończony po tygodniu {m} |
| Publish dialog | Publish this plan for {name}? / They can see it once it's published, and week 1 is the current week in their time zone. You can edit it later, but you can't take it back to a draft. | Opublikować plan? Klient: {name} / Klient zobaczy plan po publikacji, a tydzień 1 to bieżący tydzień w jego strefie czasowej. Możesz go później edytować, ale nie cofniesz go do szkicu. |
| Delete exercise | Delete "{exercise}" from {session}? / Logged sets for it stay in the history. · Delete exercise | Usunąć „{exercise}” z sesji {session}? / Zalogowane serie zostają w historii. · Usuń ćwiczenie |
| Delete session | Delete {session}? / Its {count} exercises go with it. · Delete session | Usunąć sesję {session}? / Znikną też jej ćwiczenia ({count}). · Usuń sesję |
| Leave | Leave without saving? / Your unsaved changes to this plan will be lost. · Stay · Leave | Wyjść bez zapisywania? / Niezapisane zmiany w tym planie przepadną. · Zostań · Wyjdź |
| Changed elsewhere | This plan was changed somewhere else. Reload the page to see it; copy anything you want to keep first. | Ten plan zmieniono gdzie indziej. Odśwież stronę, aby go zobaczyć; najpierw skopiuj to, co chcesz zachować. |
| Already published | This plan is already published. Reload the page to see it. | Ten plan jest już opublikowany. Odśwież stronę, aby go zobaczyć. |
| Quiz first | {name} has to answer the quiz before you build a plan. | {name}: najpierw potrzebne są odpowiedzi z quizu. |
| Week limit | This plan is already in week {n}. Enter {n} or more weeks. | Ten plan jest już w tygodniu {n}. Wpisz co najmniej {n} tyg. |
| Too few sessions | Add at least 2 sessions. | Dodaj co najmniej 2 sesje. |
| Empty session | Add at least 1 exercise to {session}. | Dodaj co najmniej 1 ćwiczenie do sesji {session}. |
| Library | Choose exercises from your exercise library. | Wybieraj ćwiczenia ze swojej biblioteki. |
| Field limits | Enter 4 to 16 weeks. · Enter a session name of up to 60 characters. · Enter 1 to 20 sets. · Enter 1 to 3600. · Leave the load empty or enter a number above 0. · Enter an RPE from 5 to 10, in whole steps. · Enter 0 to 600 seconds. | Wpisz od 4 do 16 tygodni. · Wpisz nazwę sesji do 60 znaków. · Wpisz od 1 do 20 serii. · Wpisz od 1 do 3600. · Zostaw obciążenie puste lub wpisz liczbę większą od 0. · Wpisz RPE od 5 do 10, w pełnych krokach. · Wpisz od 0 do 600 sekund. |

## Assumptions

- Defaults chosen while reviewing the requirements, and open to review: the picker stays open between picks; an untouched new plan counts as unchanged; publish failures after a successful save leave a saved draft; a finished plan stays editable; the session list starts on the first session.
- A sign-in that ended while editing follows the app's standard redirect to sign-in on the next request, so unsaved edits can be lost then; the browser's warning shows on that redirect. Sign-ins last 14 days without use, so this is rare, and keeping edits across a sign-in would need browser storage for a client's plan on a possibly shared device.
- "Publishing makes the plan visible to the client" is met in two parts: this story makes the plan available and moves the client past the waiting screen; the screen that shows it is story #75. Until #75 merges, the client sees the app's placeholder pages. Open pull request #229 sends clients past the quiz only with `PLAN_PUBLISHED`, which this story supplies.
- The UI language can only be changed on the settings page, so changing it passes the leave question, and a language change never mixes names in an editor with unsaved work.
- The board's other states belong to other stories: the built-from-quiz alert, "Draft failed", "Build again" and the client's notes and quiz answers next to a draft to #79 and #85; approvals to #85; the notice to the client to #150; the client's view of the plan to #75.
- "A session already started today keeps its targets" and "logged sets never change" have nothing to act on yet, because sessions and logs do not exist. This story guarantees that a save touches only the plan. The copy of targets is made by the logging stories: #92 copies them per set log, and #94 (start session) needs to copy them when a session starts, as a comment on #94 says.
- Saving overwrites the plan in place; earlier targets are not kept. Story #145 ("past weeks show the old targets") decides how history is stored.
- A plan exists only after the quiz was submitted, so task #53's routing does not change: a published plan is what lets a client past the waiting state. A trainer may start a plan while #84 drafts one in the background; #84 must not replace a plan the trainer saved meanwhile, as a comment on #84 says.
- Publishing is not reversible in this story; there is no "unpublish".
- New plans start at 12 weeks, in the middle of the allowed range, and target RPE starts at 8, the usual target in `CONTEXT.md`.
- Limits on sets, amount and rest are the exercise library's. The limits on session names, exercises per session and load are chosen here so that a request stays bounded.
- The client page keeps its frame with tabs (task #38), so the board's own breadcrumb and page heading are not repeated inside the tab.
- The trainer's area is designed at 1280 px; a phone layout for trainers is story #179, so at 375 px the editor must stay usable but is not redesigned.
