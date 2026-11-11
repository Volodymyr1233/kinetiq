# Research: Trainer sees a client's adjustments

All unknowns of the plan's Technical Context are resolved here; nothing is left as NEEDS CLARIFICATION.

## R1. Which feature builds the 14 days and the recovery days

- **Decision**: `sessionlog`, in `ClientTrainingViewService.overview`, which already answers `GET /api/v1/clients/{id}/overview` and checks that the client is the trainer's own. It reads check-ins through `readiness.domain.input.CheckInUseCase`.
- **Rationale**: `sessionlog → readiness` already exists (`SessionLogService`, `SessionStateService` call `CheckInUseCase.adjustmentOn`). An entry needs the day's session (log or planned day) and its state, which only `sessionlog` knows. Readiness depends on `plan`, and `sessionlog` on both, so no cycle. One request, as before (spec Assumptions).
- **Alternatives**: a trainer endpoint in `readiness` (`GET /api/v1/clients/{id}/readiness`): readiness would need sessions and states from `sessionlog`, a cycle; a second request from the overview tab for data shown in the same card.

## R2. How readiness gives the check-ins of 14 days

- **Decision**: One new method on `CheckInUseCase`: `List<CheckIn> between(AccountId client, LocalDate from, LocalDate to)`, both ends included, by date. Backed by `CheckInRepository.findAllByClientAndDateBetween` and the Spring Data derived query `findAllByClientIdAndCheckInDateBetween`, on the existing unique `(client_id, check_in_date)` index. `Adjustment.causes(Answers)` moves to a public `Cause.of(Answers)`, so a recovery day has its reason even without an adjustment.
- **Rationale**: The aggregate already carries date, `readiness()` and the stored answers; a separate read record would copy it field for field. One query for 14 rows. A score under 45 always has at least one cause (spec 117 Edge Cases), so `Cause.of` is never empty for a recovery day.
- **Alternatives**: a new `ViewReadinessUseCase` port (a second interface and bean for one method; the two test fakes of `CheckInUseCase` gain one method instead); 14 calls of `findByClientAndDate`.

## R3. Whether a recovery day had lighter targets, and which

- **Decision**: For each recovery check-in, the day's planned entries are:
  - the session log's own copy when the session was started (as `SessionLogService.adjustment` does);
  - otherwise the published plan's session on that weekday, when the date is on or after the publish day and before the plan's end (`firstWeek + weeks`), with measures from `OwnPlanView.library`;
  - otherwise none.
  `CheckInUseCase.adjustmentOn(client, date, planned)` then gives the adjustment; the outcome is `TargetChoice.LIGHTER` when it is in use, `ORIGINAL` when not, and absent when there is no adjustment ("No lighter targets").
- **Rationale**: Reuses the rule and its unit handling as they are (FR-004, constitution IV); the client's own Plan week already uses the plan's targets for days not started (`SessionStateService`, its `ponytail:` note). No new enum: `Optional<TargetChoice>` carries the three outcomes.
- **Alternatives**: the stored `target_choice` alone (shows "Lighter targets" on a day nothing could be lighter, FR-004 scenario 4); a new `RecoveryTargets` enum (three values where two exist plus absence).

## R4. The session of a recovery day

- **Decision**: The existing `ClientSession`: `started(log, now)` when a log exists, else `planned(session, date, SessionState.of(...))` for the plan's session that day, else absent. The frontend shows its name, `SessionStateBadge` and, with a `sessionLogId`, a link to the Logs tab.
- **Rationale**: The same shape and state rules as "This week" and "Recent sessions", so a day reads the same in every card.

## R5. Opening a session log from the overview

- **Decision**: The Logs tab keeps its chosen session in the URL, `logs?session=<key>` (the key it already uses: the log id, or the date of a session never started), with `useSearchParams` and `replace`, instead of `useState`. The entry links to `logs?session=<sessionLogId>`. An unknown key falls back to the newest session, as today.
- **Rationale**: Frontend rule "local state first, then URL state": a link from another tab needs URL state; it also makes a chosen log reloadable and shareable. No new route.
- **Alternatives**: router `state` on the link (lost on reload); a nested route `logs/:id` (more routing for the same result).

## R6. The trainer's session log with lighter targets (task #290)

- **Decision**: `ClientSessionLog` gains a nullable `Adjustment` for its date over the log's copy, computed in `ClientTrainingViewService.sessionLog` through `CheckInUseCase.adjustmentOn`. `SessionLogWebMapper` passes it to `entries(...)` instead of `null`, so `planned` and the unlogged sets' lighter targets come out as for the client's logger. `ClientSessionLogResponse` gains `adjustment: { causes, inUse }`, left out without an adjustment. The frontend's `SessionLogTable` shows each changed target with the shared `TargetChange` (load and amount, and the RPE target when it changed); the Logs tab's detail shows the reason line under the title.
- **Rationale**: The mapping for the client's logger already does this; the trainer's view passed `null` only because the story was not built. Logged sets keep their copied targets (FR-015).
- **Alternatives**: a separate adjustment endpoint per log (a second request for one log).

## R7. The strip

- **Decision**: Plain flex columns, not Recharts: a fixed-height track per day with a bar of `max(score, 3)%` height in `bg-ready-high|mid|low` by band, the score above it in `num`, the day of the month below, today marked with the existing "Today" copy. The list is an `<ol>`; each `<li>` has its visual parts `aria-hidden` and one `sr-only` text: "{date}: {score}, {band}" or "{date}: No check-in". The band → class map moves from `features/readiness/components/readiness-card.tsx` to `lib/readiness.ts`, used by both.
- **Rationale**: 14 bars of one value need no chart library, axis or tooltip; the board draws the same divs. A list is read item by item and needs no `role="img"`. A minimum height keeps a 0 visible (FR-010). The map has two users now, so it moves up (frontend rule on shared code).
- **Alternatives**: shadcn `chart` bar chart (tooltip-only values, an SVG harder to label per day); `role="img"` with one long label (cannot be read day by day).

## R8. The reason text for the trainer

- **Decision**: Reuse the cause nouns `readiness.adjustment.cause.*` joined with `Intl.ListFormat(language, { type: 'conjunction' })` and capitalised, as the client's reason does. New keys: the entry outcomes ("Lighter targets", "Original targets", "No lighter targets"), the log's lines "Recovery day: {causes}, so the targets were lighter." and "Recovery day: the client used the original targets.", the card title, empty messages, and the per-day text alternatives. In Polish the causes are nouns, so no gender (spec 117 departures).
- **Rationale**: One wording of the causes for both people; text stays in the frontend (constitution II).

## R9. Demo data

- **Decision**: `R__demo_check_ins.sql` already has, within the last 14 days of Demo Client Daily, two recovery days (answers 2/4/3, score 33): Thursday of plan week 2 with its session logged on lighter targets, and three days before today without a session log. Set `target_choice = 'ORIGINAL'` on the second, so the overview shows both outcomes, a logged and a never-started session, and the Ready and Take it easier bars of the other two days. Inserts stay `on conflict do nothing`; a database made before needs recreating, as the file's first line says.
- **Rationale**: Definition of Done asks demo data for new entities; there is none, but the card must be seen with real outcomes in the browser.
