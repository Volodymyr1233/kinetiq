# Research: Trainer builds, edits and publishes a plan

No open questions were left in the technical context. These are the design decisions, with what was rejected. The Context7 documentation server was not reachable while planning, so library use follows the patterns already in this repository and the installed packages' own types and docs (`npx shadcn@latest docs`).

## R1. Drafts may be incomplete; completeness is a second level of rules

- **Decision**: `Plan`'s constructor enforces the rules of FR-005 for every plan. `requireComplete()` adds FR-006 (2–6 sessions, each with an exercise); it runs on publish and whenever a published plan is built or edited.
- **Rationale**: #79 stores an empty draft when drafting fails, and a trainer must be able to save part-way.
- **Rejected**: the same rules for all plans, which makes #79's empty draft impossible and loses part-way work.

## R2. "Published" is the first week; sessions order by day

- **Decision**: `plans.first_week` is null for a draft. `status` in the API is derived. Sessions have no order column and sort by `DayOfWeek`. Letters A, B, C are computed from that order.
- **Rationale**: #69 lists a status and a session order, but each would be a second value that can contradict the first (backend `CLAUDE.md`, 3NF).
- **Rejected**: storing `status` and `position` on sessions.

## R3. Saving replaces the sessions; ids of sessions are not part of the API

- **Decision**: the API has no session or entry ids. A save clears the plan's sessions, flushes, and inserts the new ones. Entries are an `@ElementCollection` of their session with `@OrderColumn`. The frontend keeps its own local keys.
- **Rationale**: the flush is needed because Hibernate inserts before it deletes, and `uq_plan_sessions_plan_day` would fail otherwise. Ids from the browser could name another plan's rows.
- **Rejected**: client-generated ids and merging by id. Whichever story first needs a stable session reference (#94) can match on the day of the week, which is unique per plan.

## R4. Concurrent saves: a version in the request, a row lock in the use case

- **Decision**: responses carry `version`; `PUT` and the publish `POST` send it back. The use case loads the plan with `PESSIMISTIC_WRITE`, compares versions, and answers `409 plan.changed` on a difference. The first save sends no version; `uq_plans_client` turns a lost race into the same refusal. The repository force-increments the version on every save, because a change that touches only an entry would not make `plans` dirty.
- **Rationale**: without the lock, two saves could both pass the comparison. A response needs the new version, so `PESSIMISTIC_FORCE_INCREMENT` (immediate) is used, not the optimistic mode, which increments at commit.
- **Rejected**: an `If-Match` header, because nothing else in the API uses one; last write wins, which loses a trainer's work silently.

## R5. Ownership and time zone through the `client` feature

- **Decision**: the plan service calls `ViewClientsUseCase.getClient(trainer, client)`, which already answers `client.not-found` for a foreign and an unknown client alike. `ClientSummary` gains `timeZone` from the same query. `planState == NO_QUIZ` is the quiz-first check.
- **Rationale**: one call gives ownership, quiz state and time zone. Dependencies stay one way: `plan → client → quiz`, `plan → exercise`.
- **Rejected**: a `trainer_id` on plans, which would repeat the client's link and break when a client switches trainer (#180).

## R6. Other features learn about plans by SQL, not classes

- **Decision**: `JdbcClientDirectory` left-joins `plans` for `planState` and the goal. `QuizSubmissionRepository` gains `hasPublishedPlan`, a native query. `ExerciseRepository` gains `countPlansUsing(trainer)`, a native query filtered to that trainer's clients.
- **Rationale**: `plan` depends on `client` and `exercise`, so a class dependency back would be a cycle, which `HexagonalArchitectureTest` forbids. `JdbcClientDirectory` is the precedent for a read model over other features' tables.
- **Rejected**: events, or a shared port in `common`, for three one-line reads.

## R7. Value limits appear twice, on purpose

- **Decision**: request records carry Bean Validation with the domain's constants, as `CreateExerciseRequest` does, giving `validation.failed` with `errors[].field` such as `sessions[0].entries[1].rpe`. The domain repeats them with `plan.*` codes, because #79 will build plans without a request.
- **Rejected**: domain checks only, which gives no field in the error; Bean Validation only, which leaves #79's drafts unchecked.

## R8. Exercise library for the plan service

- **Decision**: `ListExercisesUseCase` gains `library(trainer)`, returning the trainer's visible exercises. The service uses it to check membership and to return names and measures.
- **Rationale**: `list()` sorts, filters and counts plans, none of which is needed here.
- **Known limit**: when a client switches trainer (#180), a plan may name the former trainer's own exercises. #180 decides that.

## R9. No plan answers 404

- **Decision**: `GET /clients/{id}/plan` answers `404 plan.not-found` for an own client with no plan, and `404 client.not-found` for a foreign or unknown one. A foreign client never gets `plan.not-found`, so nothing leaks.
- **Trade-off**: the browser logs the 404 of the empty state in its console, as it does for the 401 on the sign-in page.
- **Rejected**: `200` with an empty wrapper, or `204`, which the generated types would describe wrongly.

## R10. The frontend's editor state

- **Decision**: `useState` over one draft object inside `PlanEditor`. Numbers are kept as typed text, so "8," stays while typing. `dirty` compares the draft with the last saved draft, so undoing an edit clears it. A load keeps its original kilograms next to its text and sends them unchanged when the text is unchanged, so pounds never drift (0.1 kg rounding).
- **Rationale**: no form library field arrays for a nested, reorderable table with rules across sessions; plain functions in `plan-draft.ts` are testable without rendering. The query's data seeds the draft once and is not read again until a save.
- **Rejected**: `react-hook-form` field arrays, which fit flat forms better; storing the draft in the query cache.

## R11. Reordering without drag

- **Decision**: Move up and Move down icon buttons per row. Focus stays on the control; at the first or last place, it moves to the other arrow. A polite live region announces the new position.
- **Rationale**: WCAG 2.2 criterion 2.5.7, no new dependency (constitution VI).
- **Rejected**: drag and drop with `dnd-kit`, a dependency for what two buttons do.

## R12. Cross-feature code on the frontend

- **Decision**: `useExercises`, `CATEGORIES` and the `Exercise` type move from `features/exercise/api` to `lib/exercise-library.ts`. The client reaches the plan tab through `<Outlet context={client}>`, typed from `schema.d.ts`.
- **Rationale**: a feature must not import from another; shared code moves up (frontend `CLAUDE.md`).

## R13. Leaving with unsaved changes

- **Decision**: React Router's `useBlocker` for in-app navigation with an `AlertDialog`; a `beforeunload` listener for reload and close. The app already uses a data router (`createBrowserRouter`).

Signing out needs one more step. `useSignOut` ends the sign-in first and navigates afterwards, so a blocker would ask too late. `lib/leave-guard.ts` holds one optional callback: the editor's guard registers a function that opens its dialog and resolves to the answer, and `SignOutButton` awaits it before signing out. The blocker lets `/sign-in` through. Rejected: moving sign-out to its own route, which changes every caller for one case.

## R14. Publishing with unsaved changes

- **Decision**: the Publish button's handler runs validation, confirms, then saves if dirty and publishes with the returned version. These are two requests. If the save succeeds and the publish fails, the plan stays a saved draft and the error shows.
- **Rejected**: a combined endpoint, which adds a second way to save for one rare path.

## R15. Estimated time

- **Decision**: the sum over entries of `sets × (rest + work)`, where work is the amount in seconds, or 4 seconds per repetition, rounded to whole minutes. Invalid numbers count as 0.
- **Rationale**: the board's `sets × 3.4 min` ignores rest. The figure is a guide for the trainer, computed on the frontend from values it already has.
