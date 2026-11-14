# Research: Client sees PRs and their week streak

## R1. Where PRs and the streak are worked out

**Decision**: In the `progress` feature, which already holds the top set order (`TopSet.isBetterThan`), one row per exercise and session log (`LoggedExercise`, from `TrainingLog.loggedExercises`) and the week streak (`ProgressViewService.weeksInARow` over `TrainingWeek`). A new value `Pr` in `progress.domain.model` works out the PRs of one date from the rows (`Pr.on`), and `ViewProgressUseCase` gains `rewards(client, date)`, which answers the PRs of the client's session log of that date and the client's week streak (`Rewards`).

**Why**: FR-002 and FR-006 ask for one definition shared by every screen. Keeping PRs next to the top set order and the streak next to "In a row" means the session done screen and Progress use the same code, so they cannot disagree. The rows already hold each session's top set per exercise, with the same exercise twice in a session merged (spec 156, research R3), which is exactly what FR-001 compares.

**Alternatives considered**: Working out PRs in `sessionlog` from `SessionLogRepository`: a second copy of the top set order, which could drift from "PR today". A new endpoint in `progress` for the session done screen: task #302 puts the PRs and the streak in the session summary, and the screen would need a second request.

## R2. The summary reads the rewards through `progress`'s input port

**Decision**: `SessionLogService` (`sessionlog`) takes `ViewProgressUseCase` and calls `rewards(client, log.getDate())` when it builds a summary, for both `finish` and `summary`. It orders the PRs by the first place of their exercise in the session (`SessionLog.getEntries()`), pairs each with its `Exercise` from the library it already reads, and returns them with the streak in `SessionSummaryView`. `SessionLogConfig` passes the bean.

**Why**: `backend/CLAUDE.md`: "Other features call its `domain.input` ports." The new dependency `sessionlog → progress` makes no cycle: `progress` depends on `account`, `client`, `common`, `exercise`, `plan` and `quiz`, and none of them depends on `sessionlog` (`HexagonalArchitectureTest` checks it). `progress` reads the log tables with SQL, not through `sessionlog`'s classes, so it does not depend back. `LoggedExercise` carries no place in the session, so the order is taken where the session is known.

**Alternatives considered**: The controller calling both use cases: `backend/CLAUDE.md` has a controller "call one use case". Ordering by exercise in `progress`: it would need the place in the read model, a change to a query of spec 156 for one screen.

## R3. A finish sees itself

**Decision**: No change. `finish` saves the session log with `saveAndFlush` (`SessionLogRepositoryImpl`) before `rewards` runs `TrainingLog`'s SQL in the same transaction, so the finished day is already visible and the streak counts the session just finished (spec, User Story 2, scenario 3).

**Why**: Hibernate flushes before its own queries, not before `JdbcTemplate` ones; the explicit flush is already there. A test of `SessionLogService` with a fake port cannot prove this, so the quickstart checks it in the app (step 3.2).

**Alternatives considered**: Adding the session's own date to the finished days in the domain: a second source of truth for the same fact.

## R4. No fallback for a missing plan

**Decision**: `rewards` reads the plan with `ViewOwnPlanUseCase.view` like `own` does and lets `PlanNotFoundException` pass; `sessionlog` does not catch it. The spec states that a finished session always has a published plan (Edge Cases).

**Why**: A session starts only from a published plan, and at the time of this story nothing removes one while its client has an account. Catching the exception would not help anyway: every use case call is a transaction, and the Spring documentation says a rollback-only marker set in an inner scope makes the outer transaction end in `UnexpectedRollbackException`, which would undo the finish itself.

**Alternatives considered**: A finder in `plan` that answers empty instead of throwing: a new port method for a case that cannot happen.

## R5. "PR today" from the same rule

**Decision**: Task #304 replaces the check in `ProgressViewService.exercises` (the best row over all logs is dated today) with membership in `Pr.on(today, logged)`.

**Why**: The old check marks an exercise whose only row is today's; `Pr.on` needs an earlier row (spec, Clarifications). Using the same function is what makes FR-006 hold by construction; a test with today's session proves the two agree.

**Alternatives considered**: Adding "and an earlier row exists" to the old check: the same result with two copies of the rule.

## R6. Exercise names in the reader's language

**Decision**: `finish` and `summary` in `SessionLogController` take `Locale`, as `ProgressController` does, and the web mapper names each PR's exercise with `Exercise.getName().in(locale)`. On the frontend, the summary's query key gains `i18n.language`, as the session log's key (`features/sessionlog/api/session-log.ts`) and the plan's keys already have.

**Why**: The API client sends `Accept-Language` from the UI language (`lib/api/client.ts`), and a cached summary in the old language would show old names after a language change.

**Alternatives considered**: Sending exercise ids only and naming them on the screen: the session done screen has no exercise library of its own.

## R7. How the badges look

**Decision**: Under the title, before the figures, one row of badges that wraps: the PR badges in the order of R2, the first with `variant="brand"`, the others `variant="outline"`, then the streak with `variant="outline"`. A PR's set is its load in the client's unit (`fromKg`, `plan.units.*`) and repetitions, or its repetitions or seconds when it has no load, by the exercise's measure. No icon, as on the board.

**Why**: The board "Client · Session done" and design system section 6.8 (PR lime, streak outline); the brand rule allows one lime element per view (spec, Clarifications). The `Badge` component already has both variants (`components/ui/badge.tsx`).

**Checked at task #178** against the board and design system sections 5 and 6.8: the row under the title and the lime PR badge with ink text and no icon are as on the board. Two details the board does not show:
- The set is in the `num` class, as `frontend/CLAUDE.md` asks of every measured value. So the badge's text is one string with a `<num>` tag, rendered by `Trans`, as `sign-in-page.tsx` renders its tags.
- A badge wraps its text (`h-auto min-h-5 text-left whitespace-normal wrap-anywhere`, as `sync-badge.tsx` wraps). An exercise name can have 100 characters, and the screen must not scroll sideways at 320 px (FR-013).

**Alternatives considered**: A card per PR: the board shows badges.

## R8. Tests per task

**Decision**:
- #177: `PrTest` (plain unit test) for ties, more repetitions at the same load, a load against no load, seconds, the first log and later logs ignored; `ProgressViewServiceTest` gains `rewards` cases: the streak equal to `own`'s across a broken week and the running week, and the PRs of a date.
- #304: `ProgressViewServiceTest`'s `PrToday` gains the first log and the agreement with `rewards` for today.
- #302: `SessionLogServiceTest` with a fake `ViewProgressUseCase` for the order and pairing of PRs and the streak on `finish` and `summary`; `SessionLogControllerTest` for the response shape. The existing tests already prove `404` for another client's session and the refused trainer.
- #178 and #303: `summary-page.test.tsx` for the badges' text and order, pounds, seconds, Polish plurals and the hidden streak at 0. Which badge is lime is a style, checked in the browser, not in a test.

**Why**: `backend/CLAUDE.md` and `frontend/CLAUDE.md`, Tests: behaviour and edge cases, plain unit tests with hand-written fakes for the domain, no tests of library behaviour.
