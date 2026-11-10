# Feature Specification: Client sees their progress

**Feature Branch**: `feat/157-statistics-queries`

**Created**: 2026-11-10

**Status**: Draft

**Input**: Story #156 "Client sees their progress" and its tasks #157 (statistics queries over logs), #158 (statistics API, the client's own and, for trainers, their clients'), #159 (shadcn chart with the Kinetiq series tokens), #160 (Progress screen: tiles and sessions chart) and #161 (progress by exercise). Screen: Kinetiq Screens, board "Client · Progress". Design: the Kinetiq design system, section 3.7 (data visualisation) and its rewards rule (a PR badge and a week streak, on client screens only). Builds on spec `75-client-plan` (the client's plan, its goal and plan weeks), spec `92-session-logging` (session log, set log, the exercises copied when a session starts), spec `98-finish-session` (finished sessions, session states, average RPE), spec `101-log-edit-window` (Locked), spec `125-plan-change-rules` (the same weekly figures for the rules) and spec `42-account-settings` (weight unit). Spec `104-client-training` shows these statistics to the trainer in task #108. The story, the tasks and the board are starting points; every place this spec departs from them is listed under "Departures from the story, the board and earlier specs".

## Clarifications

### Session 2026-11-10

- Q: "Each compared with the previous week": which two weeks are compared? → A: The last completed week against the week before it; the current week is still running and shows in the sessions chart instead.
- Q: Which exercise does the best-set tile show? → A: The exercise logged in the most sessions of the period (then the most recently logged), the same one "By exercise" opens on.
- Q: Do the chart titles that state the finding count the current, still running week? → A: The sessions chart's title counts completed weeks only; the exercise chart's title also counts the current week.
- Q: An exercise has sets with and without a load in the period: which set is the top set, and in which measure is its chart drawn? → A: A set with a load always beats a set without; the chart is in load, and a week with only sets without a load has no point there, while its sessions still show in the recent sets in repetitions or seconds.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Client sees how their weeks went (Priority: P1)

The client opens the Progress tab. For the last 4, 8 or 12 weeks they see how many of their planned sessions they did, how hard their sets felt against the usual RPE 7–8 target, how many weeks in a row they trained, and a chart of the sessions they did each week.

**Why this priority**: It answers the story's question, "how am I doing over time", with the figures every client has as soon as they log (story acceptance criteria 1 and 2).

**Independent Test**: With a fixed clock on a Wednesday of plan week 4 and a plan of 3 sessions a week published on the Monday of week 1: weeks 1 and 2 with 3 finished sessions, week 3 with 2 finished and 1 missed, week 4 with 1 finished. Sets with RPE 7, 8 and 9 in known numbers. The 4-week view shows 9 of 12 sessions done, the mean RPE worked out by hand to one decimal with its place against 7–8, 4 weeks in a row, and one bar per week with week 4 marked as still running.

**Acceptance Scenarios**:

1. **Given** a client with a published plan, **When** they open the Progress tab, **Then** it shows the plan's goal and current plan week, the period switch on 8 weeks, and the tiles "Sessions done", "Average RPE", the best set of one exercise and "In a row", as on the board.
2. **Given** the Progress tab, **When** the client chooses 4, 8 or 12 weeks, **Then** every figure and chart covers that many plan weeks, ending with the current one (FR-001).
3. **Given** a period, **When** "Sessions done" is shown, **Then** it gives the sessions done out of the sessions planned in the period, with a bar of done against planned, and the comparison of FR-007.
4. **Given** a period with logged sets, **When** "Average RPE" is shown, **Then** it gives the mean RPE of the period to one decimal, says whether it is within, above or below the 7–8 target in words, and the comparison of FR-007.
5. **Given** a client who finished at least 2 sessions in each of the last 4 weeks, **When** "In a row" is shown, **Then** it says 4 weeks, with the rule "At least 2 sessions each week".
6. **Given** a period, **When** the sessions chart is shown, **Then** it has one bar per week, the sessions done against the sessions planned, a 70% line, and the current week marked as still running with the number of sessions left; its title states what the chart shows (FR-021).
7. **Given** a period longer than the plan has run, **When** it is shown, **Then** only the weeks from the plan's first week appear, and nothing before it is counted as missed.

---

### User Story 2 - Client sees their progress in each exercise (Priority: P1)

Below the weekly figures the client picks an exercise and sees its best set and average RPE week by week, and the best set of their last sessions with it. A new personal best on the day is marked "PR today".

**Why this priority**: Getting stronger is the progress clients care about most (story acceptance criterion 2, "best set per exercise").

**Independent Test**: A back squat logged once a week for 4 weeks with best sets of 100, 100, 102.5 and 105 kg at RPE 8, the last one today. Choosing back squat shows a top-set line rising from 100 to 105 kg, an RPE line at 8 inside the 7–8 band, a title saying the top set rose 5 kg in 4 weeks, the last 3 sessions with their top sets, and the best-set tile marked "PR today".

**Acceptance Scenarios**:

1. **Given** exercises logged in the period, **When** "By exercise" is shown, **Then** it lists them as choices, the one logged in the most sessions first, and the first one is chosen; it is the exercise of the best-set tile.
2. **Given** a chosen exercise, **When** its chart is shown, **Then** it has the top set of each week the exercise was logged and its average RPE that week, the RPE 7–8 target band, and a title that states the change in top set over the period.
3. **Given** a chosen exercise, **When** its recent sets are shown, **Then** the 3 most recent sessions with that exercise are listed, newest first, each with its date, its top set and that set's RPE.
4. **Given** the best set the client has ever logged for an exercise was logged today and beats every earlier set of it, **When** that exercise's best set is shown, **Then** it is marked "PR today".
5. **Given** an exercise measured in seconds or logged without a load, **When** it is shown, **Then** its top set is the most repetitions or seconds, shown in that measure, not in kilograms.
6. **Given** a client whose weight unit is pounds, **When** loads are shown, **Then** they are in pounds, converted from the kilograms stored.

---

### User Story 3 - The trainer reads the same numbers, nobody else does (Priority: P1)

A trainer can read the same statistics for each of their own clients, so the progress section of the client overview (task #108, spec 104) shows what the client sees. Anyone else asking learns nothing, not even that the client exists.

**Why this priority**: Training logs are personal data (constitution III), and a trainer and a client who disagree about the numbers cannot coach together (spec 104, FR-013).

**Independent Test**: For one client and one period, the client's statistics and their trainer's statistics for that client are equal. As a second trainer, ask for the first trainer's client: "not found", the same as for an unknown id. Not signed in: asked to sign in.

**Acceptance Scenarios**:

1. **Given** a client and their trainer, **When** both ask for the same period, **Then** every figure is the same.
2. **Given** a client of another trainer, **When** a trainer asks for that client's statistics, **Then** the answer is "not found", the same as for an unknown client, and tests prove it.
3. **Given** a signed-in client, **When** they ask for the trainer's statistics of any client, **Then** they are refused by role; they can read only their own statistics.
4. **Given** a visitor, **When** they ask for any statistics, **Then** they get the standard unauthenticated answer.

---

### Edge Cases

- Weeks are Monday to Sunday in the client's time zone, and a set counts in the week of its session's date, also when it was logged or changed after midnight within the session's 24 hours (spec 101).
- The plan was published in the middle of its first week: sessions on days before the publish day are not planned and not missed (spec 98, FR-006), so that week can have fewer than 2 planned sessions; for "In a row" it counts when every session it had was done.
- The plan has finished: the period ends with the plan's last week, and the header says the plan has finished.
- The trainer changed the plan during the period: planned sessions come from the plan as it is when read, for every week of the period, as for the rules of spec 125; sets logged keep the exercises their session had when it started (spec 92).
- A session was started and not finished: its sets count in the mean RPE and the best sets, and it is not a session done.
- A session log on a day the current plan has no session (the trainer moved the session after it was logged): its sets count, and it is not a session done, since sessions done are matched to planned days by date, as in spec 125.
- The same exercise twice in one session: both entries count as that exercise.
- Two sets with the same highest load: the one with more repetitions or seconds is the top set; the same again: the earlier one.
- An exercise logged with and without a load (a pull-up, weighted on some days): a set with a load is always the top set over one without; the chart is in load (FR-019).
- A week with no logged set has no mean RPE and no top set; it is left out of comparisons, never counted as 0.
- A client with a published plan and nothing logged: tiles show 0 sessions done and no RPE, and the charts say there is nothing logged yet instead of showing empty axes.
- The current week is still running: its sessions still to come count as planned, and the chart marks the week as running.
- No error response contains server internals.

## Requirements *(mandatory)*

### Functional Requirements

**Statistics** (task #157, queries over logs; task #158, API)

- **FR-001**: The statistics MUST cover a period of 4, 8 or 12 plan weeks: the current plan week and the weeks before it, or, once the plan has finished, its last weeks. Weeks before the plan's first week MUST NOT be part of a period, so a period can have fewer weeks than chosen.
- **FR-002**: For each week the statistics MUST give the sessions planned and the sessions done. Planned sessions are the current plan's sessions on days of that week on or after the day the plan was published, including days still to come in the current week. A planned session is done when a session log of that day is finished (spec 98).
- **FR-003**: For each week and for the whole period the statistics MUST give the mean RPE of every set logged in sessions dated in it, finished or not; with no set there is no mean.
- **FR-004**: For each week and each exercise logged in it the statistics MUST give the top set: the set with the highest load, then the most repetitions or seconds, then the earliest; a set with a load always beats a set without one, and among sets without a load the most repetitions or seconds wins. They MUST also give that exercise's mean RPE in the week.
- **FR-005**: For each exercise the statistics MUST tell whether its top set ever was logged today and beats every earlier set of that exercise (a PR, CONTEXT.md), over all of the client's session logs, not only the period.
- **FR-006**: The statistics MUST give the weeks in a row, up to the last completed week, in which the client did at least 2 sessions, or every session of a week that had fewer than 2 planned; the current week adds one once it meets the rule. The count runs over the whole plan, not only the period.
- **FR-007**: Sessions done, mean RPE and each exercise's top set MUST each be compared week on week: the last completed week of the plan against the week before it. A comparison with a week that has no value (no planned session, no set, the exercise not logged) MUST NOT be shown.
- **FR-008**: For each exercise logged in the period, the statistics MUST give its 3 most recent sessions with that exercise: date, top set and that set's RPE.
- **FR-009**: A signed-in client MUST be able to read their own statistics for a period; a signed-in trainer MUST be able to read the same statistics for one of their own clients. For the same client and period both MUST be equal.
- **FR-010**: Loads MUST be read in kilograms and shown in the reader's weight unit (spec 42); dates and weeks are the client's.

**Charts** (task #159)

- **FR-011**: Charts MUST use the series colours of the design system with their fixed meanings, in light and dark: completion `chart-1`, RPE `chart-2`, load `chart-3`, wearables `chart-4`, previous period `chart-5`; this story uses the first three.

**Progress screen** (task #160)

- **FR-012**: The Progress tab MUST show, as on the board "Client · Progress": the plan's goal and current plan week, the period switch (4, 8, 12 weeks; 8 when it opens), the four tiles, the sessions chart and "By exercise".
- **FR-013**: "Sessions done" MUST show the sessions done out of the sessions planned in the period, a bar of done against planned, and the comparison of FR-007.
- **FR-014**: "Average RPE" MUST show the period's mean RPE to one decimal, whether it is within, above or below the 7–8 target, decided on the value shown (7.0 to 8.0 is within), said in words, never by colour alone, and the comparison of FR-007 with its arrow.
- **FR-015**: The best-set tile MUST show the first exercise in the order of FR-018: its name, its top set in the period, the comparison of FR-007, and "PR today" when FR-005 holds.
- **FR-016**: "In a row" MUST show the weeks of FR-006 with the rule "At least 2 sessions each week".
- **FR-017**: The sessions chart MUST show one bar per week of the period, sessions done against sessions planned, a line at 70%, the plan week numbers below, and the current week marked as running with a line saying how many sessions it has left.

**By exercise** (task #161)

- **FR-018**: "By exercise" MUST list the exercises logged in the period as choices, the one logged in the most sessions first, then the most recently logged; the first is chosen when the tab opens, so it matches the best-set tile.
- **FR-019**: The chosen exercise's chart MUST show its top set per week (`chart-3`) and its mean RPE per week (`chart-2`), with the RPE 7–8 target band; a week without the exercise has no point. When any set of the exercise in the period has a load, the top-set line is in load and a week whose sets all lack a load has no point on it; otherwise it is in repetitions or seconds.
- **FR-020**: Below the chart, the 3 most recent sessions with the exercise MUST be listed (FR-008): date, top set, RPE.

**All charts** (tasks #160 and #161)

- **FR-021**: Every chart MUST follow the design system's section 3.7: its title states the finding (for example "Top set rose 5 kg in 4 weeks") and the axis name goes in the subtitle; at most 3 series; a "Show as table" toggle; a description for assistive technology that summarises the trend. The sessions chart's title counts the completed weeks of the period only, since a running week has not missed 70% yet; the exercise chart's title compares the first and the last week the exercise was logged in the period, the current week included.

**Access**

- **FR-022**: A trainer's request for a client who is not their own MUST answer "not found", the same as for an unknown client. A client MUST be refused by role on the trainer's statistics and can read only their own. A visitor MUST get the standard unauthenticated answer. Tests prove each.
- **FR-023**: Reading statistics MUST NOT change any data.

**Quality**

- **FR-024**: Every new text MUST exist in English and Polish, with Polish plural forms; numbers, dates and units follow the reader's language and settings.
- **FR-025**: The Progress tab MUST be built from Kinetiq tokens and shadcn components, designed at 375 px, in light and dark, with visible focus, keyboard use and a name for every control.
- **FR-026**: While the statistics load the tab MUST show a skeleton of its layout; on failure a translated message and "Try again". A client with no published plan sees that their progress shows once their trainer publishes their plan.

### Public endpoints

None. Every operation needs a signed-in client (their own statistics) or trainer (their clients').

### Key Entities

- **Week statistics**: For one client and one plan week: sessions planned, sessions done, mean RPE, and for each exercise logged its top set and mean RPE. Derived when read, not stored.
- **Progress**: For one client and one period: the weeks' statistics, the period's sessions done of planned and mean RPE, the comparisons of FR-007, the weeks in a row, each exercise's PR flag and recent sessions. Derived when read, not stored.
- **Top set**: An exercise's best set in a week or a session (FR-004).
- **PR**: As in CONTEXT.md, a client's best logged result for an exercise.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A client sees how their last 4, 8 or 12 weeks went by opening the Progress tab and, for another period, one more tap.
- **SC-002**: In tests against the real database with known logs, every weekly figure (sessions planned and done, mean RPE, top set per exercise) equals the figure worked out by hand in 100% of cases, including a mid-week publish day, an unfinished session, a session in seconds and a set without a load.
- **SC-003**: In tests with a fixed clock, the period, the weeks in a row and "PR today" are right on both sides of each boundary (the start of a week, the publish day, midnight, the end of the plan), 100% of the time.
- **SC-004**: For the same client and period, the trainer's statistics equal the client's in 100% of compared cases.
- **SC-005**: Another trainer's client answers "not found" in 100% of tests.
- **SC-006**: The Progress tab passes WCAG 2.2 AA checks for contrast, labels, focus order and visible focus, in light and dark, at 375 px; every chart can be read as a table.
- **SC-007**: Every new text exists in English and Polish, and no error response of this story contains server-internal text.

## Departures from the story, the board and earlier specs

| Source | It says | This spec | Why |
|---|---|---|---|
| Board "Client · Progress" | Chart titles "Sessions done by week" and, for the exercise chart, only its legend | Titles state the finding (FR-021); the board's titles become subtitles | Story acceptance criterion 3 and the design system's section 3.7. |
| Board "Client · Progress" | No "Show as table" toggle, no RPE target band on the exercise chart | Both added (FR-019, FR-021) | Design system section 3.7: every chart has the toggle, and a target band sits behind RPE lines. |
| Board "Client · Progress" | A message count on the Messages tab | Not part of this story | Unread messages belong to the messaging stories. |

## Texts

The main English and Polish texts. Polish uses informal "ty" and should be confirmed by a copywriter, as the design system says (8.5).

| Where | English | Polish |
|---|---|---|
| Header | {Goal} · Week {n} · Plan finished | {Cel} · Tydzień {n} · Plan zakończony |
| Period switch | 4 weeks · 8 weeks · 12 weeks | 4 tygodnie · 8 tygodni · 12 tygodni |
| Sessions done | Sessions done · {done} of {planned} | Zrobione sesje · {done} z {planned} |
| Average RPE | Average RPE · within / above / below 7–8 target | Średnie RPE · w celu / powyżej / poniżej celu 7–8 |
| Best set | {exercise} best · PR today | {exercise} – najlepsza seria · PR dziś |
| In a row | In a row · {n} weeks · At least 2 sessions each week | Z rzędu · {n} tygodnie · Co najmniej 2 sesje w tygodniu |
| Comparison (FR-007) | ▲ {change} week on week · ▼ {change} week on week · Same as the week before | ▲ {change} tydzień do tygodnia · ▼ {change} tydzień do tygodnia · Tak samo jak tydzień wcześniej |
| Sessions chart | Subtitle "Sessions done by week" · Week {n} has {k} sessions left. | Podtytuł „Zrobione sesje w tygodniu” · W tygodniu {n} zostały {k} sesje. |
| Sessions chart title (finding) | {n} of {weeks} weeks at 70% or more | {n} z {weeks} tygodni na 70% lub więcej |
| By exercise | By exercise · Top set, kg · Average RPE | Według ćwiczeń · Najlepsza seria, kg · Średnie RPE |
| Exercise chart title (finding) | Top set rose / fell {change} in {weeks} weeks · Top set held at {value} | Najlepsza seria wzrosła / spadła o {change} w {weeks} tygodnie / tygodni · Najlepsza seria bez zmian: {value} |
| Recent sets | Date · Top set · RPE | Data · Najlepsza seria · RPE |
| Show as table | Show as table · Show as chart | Pokaż jako tabelę · Pokaż jako wykres |
| Nothing logged | Log a session to see your progress here. | Zapisz sesję, a tu zobaczysz swoje postępy. |
| No plan | Your progress shows here once your trainer publishes your plan. | Postępy pojawią się tu, gdy trener opublikuje Twój plan. |
| Load failed | The error's message and "Try again", as on the other tabs | Komunikat błędu i „Spróbuj ponownie”, jak na innych zakładkach |

## Assumptions

- The period ends with the current plan week, which is still running; its sessions still to come count as planned, as the board's "Week 14 has 2 sessions left" shows.
- The period switch opens on 8 weeks, as on the board; the choice is not kept in the address.
- Weekly figures follow spec 125: planned sessions from the plan as it is when read, matched to session logs by date, weeks before the publish day not counted.
- A top set is the highest load, as in spec 125; there is no estimated one-repetition maximum.
- "PR today" shows only on the day the PR was logged, as on the board; the design system allows a PR badge and a week streak on client screens only, so the trainer's section of task #108 shows neither.
- The 70% line of the sessions chart is the completion below which the low completion rule of spec 125 calls for a plan change.
- "In a row" counts at least 2 sessions a week, the board's rule and the smallest plan (2 sessions a week).
- The RPE 7–8 target is the usual target of CONTEXT.md (RPE), the same for every client.
- Exercise names come from the trainer's exercise library, in the reader's language, as in the logger.
- Nothing is stored, so this story needs no migration and no new demo entity; the demo session logs already give every figure something to show.
