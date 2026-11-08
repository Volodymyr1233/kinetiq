# Research: Trainer sees a client's training

Decisions taken while planning. Each: what, why, what else was considered.

## R1. Where the trainer's view lives

**Decision**: In the `sessionlog` feature: a use case `ViewClientTrainingUseCase` in `sessionlog.domain.input`, its service `ClientTrainingViewService`, and a controller `ClientTrainingController` under `/api/v1/clients/{id}/…`.

**Why**: The view reads session logs, which `sessionlog` owns. `sessionlog` already depends on `plan`, and `plan` depends on `client` (`PlanManagementService` uses `ViewClientsUseCase`), so `sessionlog` may use `client` and `quiz` without a cycle. Constitution I: "Features MUST NOT form dependency cycles."

**Rejected**: The `client` feature, which owns the client page's other endpoints (`client` → `sessionlog` → `plan` → `client` is a cycle); a new feature for the trainer's view (a second home for the same rules, for one use case).

## R2. Only the client's trainer

**Decision**: Every method first calls `ViewClientsUseCase.getClient(trainer, client)`, which throws `ClientNotFoundException` (`404 client.not-found`) for another trainer's client and for an unknown id alike. A session log is then read with `findByIdAndClient(id, client)`, so a log of anyone else under an own client's address is `404 session-log.not-found`. `/api/v1/clients/**` already needs role TRAINER (`SecurityConfig`).

**Why**: The same check and the same answer as the plan (`ManagePlanUseCase`) and the quiz answers (`ClientQuizAnswersController`) of the same client page. `CLAUDE.md`: "Answer `404`, not `403`, for another person's resource."

## R3. One overview answer, with the quiz answers

**Decision**: `GET /api/v1/clients/{id}/overview` answers this week, the recent sessions and the quiz answers together, as task #105 says ("Endpoint with this week's sessions, recent sessions and quiz answers for one client"). The quiz answers come from `ViewQuizAnswersUseCase.answers(client)` and are left out when there are none. The response repeats the fields of the client feature's `QuizAnswersResponse` in a record of its own, with another name.

**Why**: It is what the task asks for, and the tab gets everything with one request and no "not found" to turn into "none yet". A feature's infrastructure is private to it (constitution I), so the response record cannot be shared; the OpenAPI document names schemas by simple class name, so the record gets a distinct name.

**Rejected**: Reading the answers from the existing `GET /api/v1/clients/{id}/quiz-answers` (spec 85) beside a sessions-only overview: one record less, but it departs from the task's text and makes the tab handle two requests and `quiz.not-found` as an empty state.

## R4. State and lock, not a new label

**Decision**: Each session in an answer carries its session state (`SessionState`, spec 98) and `locked` (its 24 hours have ended, spec 101). The tab shows "Locked" for a session that is `DONE` and `locked`, and the state otherwise (spec FR-004).

**Why**: Both are terms of `CONTEXT.md` that the domain already computes: `SessionState.of` and `EditWindow`. The Logs tab needs the lock anyway, to say until when a session can change (FR-009). Nothing new enters the domain's vocabulary.

**Rejected**: A `LOCKED` value in `SessionState` (it would change the client's states of #99, and `CONTEXT.md` keeps Locked apart from the states); a separate label enum (a third name for what the two values already say).

## R5. The sessions behind "Recent sessions" and the Logs tab

**Decision**: One list, newest first: every session log of the client, plus each planned session of the published plan whose date is on or after the publish day, before today, and has no session log (a Missed session never started). A session log's state is `SessionState.of` with its header; a never-started one is `MISSED`. The overview takes the first four (FR-003); the Logs tab (#107) takes all (FR-008).

**Why**: One rule for both places, so a session has the same label everywhere (spec, Assumptions). Session logs come first because a log on a day the trainer later moved keeps its own date and name (spec, Edge Cases); the plan gives the sessions that never started. A plan has at most 16 weeks of 6 sessions (spec 68), so the list holds at most 96 sessions.

**Rejected**: Calling the session states of #99 week by week (a plan read per week, and moved logs are dropped because states match logs by the planned day).

## R6. Reading every session log of a client

**Decision**: `SessionLogRepository.findAllByClient(client)` returns the client's session logs with their entries and set logs. The entity's two collections get `@Fetch(FetchMode.SUBSELECT)`, so loading N logs takes three queries, not 1 + 2N.

**Why**: The overview needs the sets logged and the average RPE of each listed session, which the `SessionLog` aggregate already holds; at most 96 logs per client. Hibernate's documentation names subselect fetching as the way to initialise a collection for every owner loaded by one query.

**Rejected**: An aggregate query with counts and averages (a second definition of "sets logged" and "average RPE" in SQL, beside the aggregate's); join fetching both collections (two list collections cannot be join fetched together).

## R7. Sets logged and average RPE of any started session

**Decision**: `SessionLog` gains `setsPlanned()` and `averageRpe()` (empty while no set is logged, one decimal, half up). `SessionSummary.of` uses them instead of its own lines. A never-started Missed session counts the planned sets of the plan's session and has no average RPE.

**Why**: The summary of spec 98 needs a finished session, but the trainer also reads In progress and Missed sessions that have sets (FR-003). One computation keeps the trainer's figures equal to the client's summary (SC-004).

## R8. This week and today

**Decision**: The client's local date comes from the clock and the client's time zone, both from `ClientSummary`, which the ownership check already returns. This week is read only when the client's plan state is `PUBLISHED`, through `ViewOwnPlanUseCase.view(client, null)`; its planned sessions get their state as #99 computes it (`SessionState.of`, a log matched by date). The answer leaves `week` out when the plan is not published or has finished; the tab tells the two apart by the client's plan state, which the client page already has.

**Why**: Week and today are the client's (`CONTEXT.md`, Week; spec 98). Checking the plan state first avoids turning `PlanNotFoundException` into an empty week. A published plan cannot become unpublished, so a published plan without a current week has finished.

**Rejected**: A plan status field in the answer (it repeats what the client page has already read).

## R9. The two tabs in the frontend (#106, #107, #261)

**Decision**: The Overview and Logs tabs live in `features/sessionlog/pages/`, mirroring the backend feature, and the router puts them at the client page's index and `logs`. The quiz answers list of the draft panel (`features/plan`) moves up to `components/quiz-answers.tsx` and serves both places; the session state badge of the Plan tab moves up to `components/session-state-badge.tsx` and gains a `locked` flag that shows "Locked" for a Done session. Loads use the trainer's weight unit (`useWeightUnit`, spec 42).

**Why**: `frontend/CLAUDE.md`: "A feature imports from `components/`, `lib/` and its own folder. It does not import from another feature; shared code moves up." The trainer reads the quiz answers in the same rows wherever they appear: the draft panel's rows (age, height and weight apart, plus days a week) stand in for the board's single "About" row, a difference named in the pull request of #106.

**Rejected**: Copies of the list and the badge in `features/sessionlog` (two places to keep in step for the same look and texts).

## R10. Logs API (#107) and the note (#261)

**Decision**: `GET /api/v1/clients/{id}/session-logs` answers the list of R5 in full; `GET /api/v1/clients/{id}/session-logs/{sessionLogId}` answers one session log: its client session (state, `locked`, sets logged and planned, average RPE), `startedAt`, `finishedAt` and `minutes` once finished, and its entries in the logger's shape (`SessionLogResponse.LoggedEntryResponse`): each entry's exercise name and measure from the trainer's library (`ListExercisesUseCase.library(trainer)`), and each planned set with its target and, once logged, its log. #261 adds the note to that answer. A Missed session never started has no session log and is shown from its list item alone. The minutes come from `SessionLog.minutes()`, which the session summary of #99 uses as well. The time the log locks is not in the answer: the tab works it out from `startedAt` with `locksAt` of `lib/edit-window.ts`, as the client's locked logger does (spec 101), while the server decides `locked`.

**Why**: The list answers every item at once (at most 96); one log's sets are read when the trainer opens it. The logger already takes names and measures from the library for the same entries (`StartedSession`), and its entry records, private to this feature, already pair each planned set with its log, which the tab's table shows row by row with "Not logged" for a set without one. One `minutes()` keeps the trainer's minutes equal to the client's summary, as R7 does for the sets and the RPE.

**Rejected**: A flat list of set logs beside the entries (the tab would match them to planned sets itself); `locksAt` in the answer (spec 101 kept the lock time out of the API, and the tab already has the same rule).

## R11. Seeing the labels locally

**Decision**: `quickstart.md` uses the demo session logs of #100 (`R__demo_session_logs.sql`, Demo Client Daily) and one SQL statement to make a session started less than 24 hours ago. No demo data is added.

**Why**: Nothing new is stored, so no demo entity is due (`CLAUDE.md`, Definition of Done). A repeatable demo migration cannot hold a time relative to "now" (spec 101, R8).

## R12. The Overview tab against the board (#106)

**Decision**: The tab follows the board "Trainer · client overview" card by card, with these differences:

- **Layout**: the board's plan change alert, metric tiles and "Readiness, last 14 days" belong to other stories (spec, Departures). The three cards of this task keep the board's order in its two-column grid: "This week" beside "Recent sessions", "Quiz answers" below. On the board "This week" sits beside the readiness chart and "Recent sessions" beside "Quiz answers".
- **Day strip**: as on the board, a Done day is a filled ink circle with a check, today's cell is tinted lime with a ringed circle, a planned day is an outlined circle with the session's letter, and a rest day is a muted circle with a dash. The board shows no Missed day: it is a circle in the muted destructive colour with a cross, as the Missed badge is red. A session In progress, and one before the publish day, keeps its letter like a planned day. The strip is not interactive; each day says its session and state in words for assistive technology, and today's day is marked as the current date.
- **Rows of "This week"**: "Upper body · Mon", "RPE 8.2" once a set is logged, and the badge, as on the board. The board's "in progress" beside a "Today" badge is the In progress badge (spec, Departures).
- **Figures of "Recent sessions"**: "15/15 sets · RPE 8.2", as on the board, instead of the longer "15 of 15 sets" and "Average RPE 8.2" of the Logs tab (spec, Texts).
- **Badges**: Locked is the board's neutral badge, with a lock icon; Done, Missed, Today and Planned keep the look they have on the client's Plan tab.
- **"All logs"**: the board's small outline button at the top right of "Recent sessions".
- **Quiz answers**: the draft panel's rows (R9).
- **Failure**: the error's own translated message and "Try again", as the client page and its Plan tab show a failed load (`frontend/CLAUDE.md`: "Show `t('errors.<code>')`").

**Why**: Task #106 is done when the tab "Matches the mockup at 1280px"; the cards of other stories are left out, so their places close up. A Missed day needs a look the board does not give, and red is what the Missed badge already uses.

**Rejected**: Leaving the readiness chart's place empty until story #122 (an empty half of the first row); the longer figures of the Logs tab in the overview's rows (they do not fit the board's one-line rows).

## R13. The Logs tab against the board (#107)

**Decision**: The tab follows the board "Trainer · client logs": the list of sessions in a card on the left and the chosen session in a card on the right, with these choices and differences:

- **List**: each row is a button with the name, "Mon 5 Oct · RPE 8.2" (or "No sets" when nothing was logged) and the badge (Done, Locked, In progress, Missed); the chosen row has the muted background and a line on its left edge, as on the board, and is marked as the current one for assistive technology. The newest session is chosen when the tab opens (spec, Assumptions).
- **Chosen session**: its card stays in view while a long list scrolls; the title "Upper body · Mon 5 Oct" and the line "15 of 15 sets · 48 min · average RPE 8.2", the minutes only once finished and the RPE only once a set is logged.
- **Table**: one table at trainer density, as on the board, with the columns Exercise, Set, Target, the trainer's unit ("kg" or "lb", the board's "KG"), Reps and RPE. The exercise's name is on its first set's row only. The target reads "8 × 50" (repetitions or seconds × load), or the amount alone without a load. The board shows only repetitions: an exercise measured in seconds shows its amounts as "45 s", and in a session that has one the column is "Reps / s". A planned set without a log shows "Not logged" across the logged columns.
- **RPE**: the logged RPE's number with a dot in the design system's RPE colour, by the set's target: below it easy, at it on target, above it hard. The number carries the value, so the colour is never the only cue.
- **Still changeable**: "{client} can still change these logs until {time}." under the table while the log is not locked, as on the board.
- **Missed, never started**: the title, and "Not started" instead of the table.
- **Not built**: the "Synced" badge (#114), the message icon on each set (#172) and "Adjusted" (#117, #122) (spec, Departures).

**Why**: The board is the screen the tab answers to (spec FR-011); a table per exercise, as first planned, would repeat the header for every exercise where the board has one. A session mixing repetitions and seconds must not show seconds under "Reps" (spec, Edge Cases).

**Rejected**: A table per exercise (unlike the board); the column "Load" (the board names the unit).
