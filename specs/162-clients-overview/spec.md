# Feature Specification: Trainer sees the overview of all clients

**Feature Branch**: `feat/162-clients-overview`

**Created**: 2026-11-12

**Status**: Draft

**Input**: Story #162 "Trainer sees the overview of all clients" and its tasks #163 (client status), #164 (overview API: KPIs and Needs attention), #165 (overview screen) and #166 (all-clients table: status columns and filters). Screens: Kinetiq Screens, boards "Trainer · clients overview" and "Trainer · all clients". Design: the Kinetiq design system, sections 5 (Badge, Feedback), 6.10 (trainer dashboard) and 8.3 (trainer alert copy). Builds on spec `35-trainer-clients` (the trainer's own clients, plan state), spec `98-finish-session` (finished sessions, missed sessions), spec `109-readiness-check-in` and `117-recovery-day-targets` (readiness, recovery days, the lighter or original targets), spec `85-draft-review` (drafts waiting for the trainer), spec `137-plan-change-decision` (open plan changes and who they wait for) and spec `156-client-progress` (mean RPE to one decimal, the RPE 7–8 target). The story, the tasks and the boards are starting points; every place this spec departs from them is listed under "Departures from the story, the boards and earlier specs".

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Trainer sees each client's status (Priority: P1)

Each of the trainer's clients has a client status, the trainer's at-a-glance verdict: At risk, Watch, On track, Plan to review or No plan, decided by fixed rules over the client's plan, sessions and RPE. The status says why when it is At risk or Watch.

**Why this priority**: Every other part of the story sorts, counts or filters by it (story acceptance criterion 1, task #163).

**Independent Test**: With a fixed clock and a plan of 3 sessions a week published 5 weeks ago, 12 sessions due in the last 28 days: 7 done gives At risk, 8 done Watch, 9 done On track; with 9 done and a mean RPE of 8.1 over the last 14 days Watch, with 8.0 On track. A client with a draft is Plan to review; one whose plan finished, or who has not answered the quiz, No plan.

**Acceptance Scenarios**:

1. **Given** a client with a running plan, **When** fewer than 60% of their sessions due in the last 28 days were done, **Then** the status is At risk with the reason "did {done} of {due} sessions in 4 weeks".
2. **Given** a client with a running plan and 60% to 69% of due sessions done, **When** the status is read, **Then** it is Watch with that reason.
3. **Given** a client with a running plan and at least 60% done, **When** the mean RPE of their sets in the last 14 days is above 8.0, **Then** the status is Watch with the reason "average RPE {rpe} over 2 weeks"; a client who is At risk keeps At risk and gets both reasons.
4. **Given** a client with a running plan and none of the above, **When** the status is read, **Then** it is On track.
5. **Given** a client whose plan waits for the trainer (a draft built, failed or saved, or quiz answers with no plan yet), **When** the status is read, **Then** it is Plan to review.
6. **Given** a client without a quiz, one whose first plan is still being built, or one whose plan has finished, **When** the status is read, **Then** it is No plan.

---

### User Story 2 - Trainer sees who needs them first (Priority: P1)

The trainer opens Clients and sees four KPI tiles across all clients, then "Needs attention", one alert per thing that asks for them, most severe first, each with one action, then the top of the client table, most urgent first.

**Why this priority**: It is the story's goal, "spend time where it matters" (acceptance criteria 2 and 3, tasks #164 and #165).

**Independent Test**: As the demo trainer, open Clients: the tiles show the active clients, this week's sessions done of planned, this week's average RPE against last week and the approvals waiting; "Needs attention" lists the At risk and Watch clients with their reasons, the drafts to review, the open plan changes, today's recovery days, clients with no log for 6 or more days and plans ending; every action opens the right screen; nothing of the second trainer's clients appears.

**Acceptance Scenarios**:

1. **Given** a trainer with clients, **When** they open Clients, **Then** the tiles show: Active clients (clients with a running plan) with the number of all clients; Sessions done this week, done of planned over the active clients' current weeks, with a bar; Average RPE of this week's sets with its change against last week; Waiting for you, the drafts and plan changes waiting for the trainer, which opens Approvals.
2. **Given** an At risk or Watch client, **When** "Needs attention" is shown, **Then** it has an alert naming the client and each reason; when the client has an open plan change it also says what the plan change is and whom it waits for, and its action opens the plan change; otherwise the action opens the client.
3. **Given** an open plan change of a client who is not At risk or Watch, **When** "Needs attention" is shown, **Then** it has an alert with the plan change's title and whom it waits for, opening the plan change.
4. **Given** a client whose draft waits for the trainer, **When** "Needs attention" is shown, **Then** it says the plan is ready to review, or could not be built automatically, and its action opens the client's plan.
5. **Given** a client in the Recovery day band today, **When** "Needs attention" is shown, **Then** it says so with the readiness and whether the client kept the lighter targets or chose the original ones, and opens the client.
6. **Given** a client with a running plan and no set logged for 6 or more days, **When** "Needs attention" is shown, **Then** it says for how many days, with the last session and its date, or that nothing was logged since the plan was published, and its action opens the conversation with the client.
7. **Given** a client in the last week of their plan, or whose plan has finished, **When** "Needs attention" is shown, **Then** it says the day the plan ends or ended, and opens the client's plan.
8. **Given** nothing needs the trainer, **When** "Needs attention" is shown, **Then** it says so in one line.
9. **Given** the overview, **When** the table is shown, **Then** it has the 8 most urgent clients (status order, then name) and "View all {n}", which opens All clients.
10. **Given** a trainer with no clients, **When** they open Clients, **Then** they see the empty state with Invite client instead of tiles and lists.

---

### User Story 3 - Trainer filters and sorts all clients (Priority: P2)

All clients lists every client with plan week, last log, sessions over 4 weeks, average RPE over 2 weeks, readiness today and status. The trainer searches by name, filters by status with a count on each filter, and sorts by column.

**Why this priority**: The full list behind the overview (acceptance criterion 4, task #166).

**Independent Test**: With 14 clients, choose Watch: only Watch clients remain and the count shown on Watch equals the rows. Type part of a name: every count follows the search. Sort by average RPE: highest first, clients without an RPE last; again: lowest first, still last.

**Acceptance Scenarios**:

1. **Given** All clients, **When** it is shown, **Then** each row has name, goal, plan (week {n} of {m}, finished, or the plan state), last log, sessions done of due over 4 weeks with a bar, average RPE over 2 weeks with its RPE place, readiness today with its band and "Lighter targets" when kept, and status.
2. **Given** the status filters All, At risk, Watch, On track, Plan to review and No plan, **When** one is chosen, **Then** only rows with that status remain and each filter's count is the number of rows it would show with the current search.
3. **Given** a sortable column (name, last log, sessions, average RPE, status), **When** its header is chosen, **Then** the rows sort by it, choosing it again reverses the order, rows without a value stay last, ties fall back to name, and the header announces the order.
4. **Given** a search or a filter that matches no client, **When** it is shown, **Then** it says so and offers to clear the search and filter.

---

### User Story 4 - Only the trainer's own clients (Priority: P1)

Everything on both screens comes from the trainer's own clients. Another trainer's clients, plan changes and drafts never appear and never count.

**Why this priority**: Training logs and readiness are personal data (constitution III).

**Independent Test**: Give the second demo trainer's client an At risk record, an open plan change and a recovery day today: the first trainer's overview and table show none of it and no count includes it. A client asking for the overview is refused by role; a visitor is asked to sign in.

**Acceptance Scenarios**:

1. **Given** two trainers, **When** one reads the overview, **Then** no client, plan change, draft or set of the other's clients is part of any row, count or alert.
2. **Given** a signed-in client, **When** they ask for the overview, **Then** they are refused by role.
3. **Given** a visitor, **When** they ask for the overview, **Then** they get the standard unauthenticated answer.

### Edge Cases

- Days and weeks are the client's own, in their time zone: "today", the last 28 and 14 days and this week (Monday to Sunday) differ between two clients in different zones.
- A planned session counts as due on its day once the day has passed, or on the day itself once it is finished; one still to come today is not due, so a client is not At risk at breakfast for today's session (spec 98: a missed session is one whose day has passed).
- The plan was published less than 28 days ago: only days from the publish day count. Its first days without any due session give no completion, so the status is decided by RPE only and is On track without high RPE.
- A plan was changed in the window (a session moved or removed): planned days come from the plan as it is when read, as in spec 125.
- A session started and not finished: its sets count in the RPE; it is not done.
- A finished session on a day the current plan has no session: its sets count; it is not a due session done.
- A client without any set in the last 14 days has no average RPE; RPE never makes them Watch.
- The mean RPE is decided on the value shown, to one decimal half up: 8.0 is not above 8, 8.05 rounds to 8.1 and is.
- A client in the last week of a plan with a running week: the plan ends on its last Sunday. A plan whose last week has passed is finished: No plan, with "Plan ended".
- A client with both an open plan change and another reason (a recovery day, no log): each gets its own alert; only the status alert absorbs the plan change.
- A plan change waiting for the client only (the trainer accepted): it is in "Needs attention", but not in "Waiting for you".
- A client with a draft and no published plan has no plan week, sessions or last log yet; those cells say so instead of 0.
- No log ever and the plan published 6 or more days ago: the alert says nothing was logged since the plan was published.
- Readiness today without a check-in: "No check-in", never a score of 0.
- No error response contains server internals.

## Requirements *(mandatory)*

### Functional Requirements

**Client status** (task #163)

- **FR-001**: Every client of the trainer MUST have exactly one client status: At risk, Watch, On track, Plan to review or No plan.
- **FR-002**: A client whose plan state is a draft waiting for the trainer (built, failed or saved), or quiz answers without a plan, MUST be Plan to review. A client without a quiz, with a first plan still being built, or with a finished plan MUST be No plan.
- **FR-003**: For a client with a running plan (published, not finished), the sessions due in the last 28 days MUST be the plan's sessions on days from the later of the publish day and today minus 27 days, up to today, that are before today, or today when finished; the sessions done are the due ones with a finished session log that day.
- **FR-004**: The average RPE over 2 weeks MUST be the mean RPE of every set logged in session logs dated today or in the 13 days before, finished or not, to one decimal, half up; absent without a set.
- **FR-005**: A client with a running plan MUST be At risk when fewer than 60% of their due sessions were done; otherwise Watch when fewer than 70% were done or the average RPE over 2 weeks is above 8.0; otherwise On track. Without a due session the completion decides nothing.
- **FR-006**: An At risk or Watch status MUST carry each reason that holds: low completion (done and due) and high RPE (the average).
- **FR-007**: Statuses MUST sort in the order At risk, Watch, Plan to review, No plan, On track.

**Overview** (task #164)

- **FR-008**: A signed-in trainer MUST be able to read the overview of their own clients: the tiles of FR-009, the alerts of FR-010 and a row per client (FR-013), in one answer.
- **FR-009**: The tiles MUST give: the active clients (with a running plan) and all clients; the sessions planned and done in the active clients' current plan weeks, planned meaning the plan's sessions of the week on or after the publish day; the mean RPE of the sets the active clients logged this week and its change against the week before, absent when either week has no set; the drafts waiting for the trainer and the open plan changes the trainer has not accepted.
- **FR-010**: The alerts MUST be, in this order and by client name within each kind: At risk; Watch; no log for 6 or more days (a running plan and 6 or more days since the date of the last set's session, or since the publish day without any set); open plan changes of clients not At risk or Watch; drafts waiting for the trainer; plans ending (in their last week) or ended (finished); recovery days today (a check-in today in the Recovery day band).
- **FR-011**: An At risk or Watch alert of a client with an open plan change MUST name the plan change's title and whom it waits for (the trainer, the client, both); a plan change alert likewise.
- **FR-012**: Each alert MUST have one action: a plan change opens it; a status alert without a plan change and a recovery day open the client; a draft and a plan ending open the client's plan; no log opens the conversation.

**Client rows** (tasks #164 and #166)

- **FR-013**: Each row MUST give: id, name, goal, plan state, the plan week and the plan's weeks (or that it finished), status and its reasons, the sessions done and due over 4 weeks (running plan only), the average RPE over 2 weeks, when the last set was logged, and today's readiness score and band with whether the lighter targets are in use.

**Overview screen** (task #165)

- **FR-014**: Clients MUST show, as on the board "Trainer · clients overview": a date line and the title, the four tiles, "Needs attention" and the top of the client table with "View all {n}".
- **FR-015**: While it loads it MUST show skeletons in the final layout of tiles, alerts and table; on failure a translated message and "Try again".
- **FR-016**: The alerts MUST use the design system's tones (At risk destructive; Watch and no log warning; plan changes, drafts and plans ending info; recovery days the readiness scale, never red), each with an icon and text, so the tone is never the only signal, and follow the trainer alert formula: name + fact + what is waiting.

**All clients** (task #166)

- **FR-017**: All clients MUST have the columns of FR-013, the name search of spec 35, the status filters with counts that equal the rows they show under the current search, sorting by name, last log, sessions (by share done), average RPE and status, the sticky name column, Invite client and Open invites.
- **FR-018**: Each status MUST show as a badge with its name: At risk destructive, Watch warning, Plan to review info, No plan outline, On track neutral. The RPE and readiness carry their band in words or numbers as well as by colour.

**Access and quality**

- **FR-019**: The overview MUST hold only the trainer's own clients, their plan changes and drafts. A client MUST be refused by role and a visitor get the standard unauthenticated answer. Tests prove each.
- **FR-020**: Reading the overview MUST NOT change any data.
- **FR-021**: Every new text MUST exist in English and Polish with Polish plural forms; numbers, dates and relative times follow the reader's language.
- **FR-022**: Both screens MUST be built from Kinetiq tokens and shadcn components, designed at 1280 px, in light and dark, with visible focus, keyboard use and a name for every control.

### Public endpoints

None. The overview needs a signed-in trainer.

### Key Entities

- **Client status**: As in CONTEXT.md; with the reasons of FR-006. Derived when read, never stored.
- **Active client**: A client whose plan is published and has not finished.
- **Overview**: For one trainer: the tiles, the alerts and the client rows. Derived when read, never stored.
- **Alert** (Needs attention): One thing that asks for the trainer: its kind, its client and what the kind needs to be told (plan change, draft, days without a log, plan end).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A trainer sees which clients need them, and why, by opening Clients, with no further click.
- **SC-002**: In tests with a fixed clock, the status is right on both sides of each threshold (60%, 70%, RPE 8.0 and 8.1) and each window edge (the 28th and 29th day back, the 14th and 15th, today before and after a finished session, the publish day, the last day of the plan), 100% of the time.
- **SC-003**: In tests against the real database, every tile and row figure equals the figure worked out by hand from known logs, and another trainer's data is part of none of them.
- **SC-004**: On All clients, every status filter's count equals the rows it shows, with and without a search, in 100% of cases.
- **SC-005**: Both screens pass WCAG 2.2 AA checks for contrast, labels, focus order and visible focus, in light and dark, at 1280 px.
- **SC-006**: Every new text exists in English and Polish, and no error response contains server-internal text.

## Departures from the story, the boards and earlier specs

| Source | It says | This spec | Why |
|---|---|---|---|
| Story, criterion 1 | "4-week completion" and "2-week mean RPE" | The last 28 and 14 days, counting only sessions whose day has passed or which are done (FR-003, FR-004) | Counting sessions still to come as missed would make every client At risk on Monday morning; rolling days avoid a jump at each week start. |
| Board "clients overview" | A period switch 4 / 8 / 12 weeks; design system 6.10: period chips | No period switch | The status rule is fixed at 4 and 2 weeks; a switch that changed columns but not the status beside them would contradict itself. |
| Board "clients overview" | A search box and a notification bell in the top bar, a notifications sheet | Not part of this story | Search is on All clients (spec 35); notifications are a story of their own (design system 6.11). |
| Board "clients overview" | Sortable headers on the overview table | The overview table is fixed in status order; sorting is on All clients | The overview shows the 8 most urgent clients; sorting 8 rows by name would show a different, arbitrary 8. |
| Board "clients overview" | "1 new this week" under Active clients | "of {n} clients" | Active is a running plan; joining is not activity, and the count of all clients tells what the active share is. |
| Board "clients overview" | Some At risk and Watch clients have no alert; Jakub (At risk) shows only "hasn't logged" | Every At risk and Watch client has an alert; no log is an alert of its own beside it | Story criterion 3; each alert has a different action (plan change or client, conversation). |
| Board "all clients" | Filter counts over all clients while a search is typed | Counts follow the search | Task #166, "filter counts match the rows". |
| Board "all clients" | "Plan week" column | "Plan" column: week {n} of {m}, Finished, or the plan state (No quiz yet, Building plan, Draft, …) | The plan state of spec 35 stays visible; the status alone does not say whether a draft was built or failed. |
| Board "all clients" | "Avg RPE" without a period | "Avg RPE, 2 wk" | It is the figure that makes a client Watch. |
| Spec 35 | All clients at `/trainer/clients` | The overview at `/trainer/clients`, All clients at `/trainer/clients/all` | The Clients item of the sidebar opens the overview, as on the board. |

## Texts

Polish uses informal "ty"; to be confirmed by a copywriter (design system 8.5).

| Where | English | Polish |
|---|---|---|
| Date line | {weekday, day month} · week of {range} | {dzień tygodnia, dzień miesiąc} · tydzień {zakres} |
| Tiles | Active clients · of {n} clients | Aktywni klienci · z {n} klientów |
| | Sessions done this week · {done} of {planned} | Sesje zrobione w tym tygodniu · {done} z {planned} |
| | Average RPE · ▲ {change} vs last week · ▼ … · Same as last week · No sets logged this week | Średnie RPE · ▲ {change} w porównaniu z zeszłym tygodniem · ▼ … · Tak samo jak w zeszłym tygodniu · Brak serii w tym tygodniu |
| | Waiting for you · {n} plan changes, {m} new plans | Czeka na Ciebie · {n} zmiany planu, {m} nowe plany |
| Needs attention | Needs attention · Nothing needs you right now. | Wymaga uwagi · Teraz nic nie czeka na Ciebie. |
| Low completion | {name} did {done} of {due} sessions in 4 weeks | {name}: {done} z {due} sesji w 4 tygodnie |
| High RPE | {name}'s average RPE is {rpe} over 2 weeks | Średnie RPE {name}: {rpe} w 2 tygodnie |
| Plan change | {title} is waiting for you / for {name} / for both of you. | {title} czeka na Ciebie / na {name} / na Was oboje. |
| Draft | {name}'s plan is ready to review. · {name}'s plan couldn't be built automatically. · {name}'s draft is waiting to be published. | Plan {name} czeka na przegląd. · Nie udało się automatycznie zbudować planu {name}. · Szkic {name} czeka na publikację. |
| Recovery day | {name} has a recovery day · Readiness {score}. Lighter targets for today. / Kept the original targets. | {name} ma dzień regeneracji · Gotowość {score}. Lżejsze cele na dziś. / Zostały pierwotne cele. |
| No log | {name} hasn't logged for {n} days · Last session: {session}, {date}. · Nothing logged since the plan was published on {date}. | {name} nic nie zapisuje od {n} dni · Ostatnia sesja: {session}, {date}. · Nic nie zapisano od publikacji planu {date}. |
| Plan ending | {name}'s plan ends on {date}. · {name}'s plan ended on {date}. | Plan {name} kończy się {date}. · Plan {name} skończył się {date}. |
| Actions | Open · Open plan change · Review plan · Open plan · Message | Otwórz · Otwórz zmianę planu · Przejrzyj plan · Otwórz plan · Napisz |
| Table | Client · Goal · Plan · Last log · Sessions, 4 wk · Avg RPE, 2 wk · Readiness today · Status · View all {n} | Klient · Cel · Plan · Ostatni zapis · Sesje, 4 tyg. · Śr. RPE, 2 tyg. · Gotowość dziś · Status · Zobacz wszystkich ({n}) |
| Cells | Week {n} of {m} · Finished · No logs yet · No check-in · Lighter targets | Tydzień {n} z {m} · Zakończony · Brak zapisów · Brak check-inu · Lżejsze cele |
| Statuses | At risk · Watch · On track · Plan to review · No plan | Zagrożony · Do obserwacji · Na dobrej drodze · Plan do przeglądu · Bez planu |
| Filters | Status · All | Status · Wszyscy |
| No match | No clients match. · Clear search and filter | Brak pasujących klientów. · Wyczyść wyszukiwanie i filtr |

## Assumptions

- The thresholds (60%, 70%, RPE above 8) are the story's and the same for every client; nothing is configurable.
- "Active client" is a new term for CONTEXT.md: a client with a running plan.
- "Waiting for you" counts what the Approvals item of the sidebar counts, so the two always agree.
- "This week" for the session and RPE tiles is each client's current plan week, Monday to Sunday in their time zone; the date line is the trainer's own today.
- The lighter targets are in use when today's check-in is in the Recovery day band and the client did not choose the original targets (spec 117); a check-in can only be saved on a session day.
- The last log is the newest logged set; its session's date decides the days without a log.
- Plan change titles come in the trainer's language, as on Approvals (spec 137).
- A trainer has few enough clients that the whole overview is one answer with no paging, as for the client list of spec 35.
- Nothing is stored, so there is no migration; the demo data already gives every status and alert something to show, and gains rows only where one is missing.
