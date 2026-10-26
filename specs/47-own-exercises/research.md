# Research: Trainer adds and edits their own exercise

Context7 was disconnected during planning, so library behaviour that matters is stated as a check in the task that depends on it, not assumed: the shadcn `select` docs come from `npx shadcn@latest docs select`; Spring and Hibernate behaviour is proven by the tests named in plan.md.

## R1: Where the owner comes from

- **Decision**: The owner is the signed-in account (`Authentication#getName()`, the account id, as in `InviteController`). No request carries an owner. The update request has no field for the measure either.
- **Rationale**: Mass assignment of `owner` would let a trainer create or take over another's exercise. The record types simply have no such field.
- **Alternatives**: A shared `signedIn(...)` helper in `common` — rejected; it is a one-liner and `common` must not depend on features. The one in `InviteController` is private to `account`.

## R2: Ownership and the `404`

- **Decision**: `ExerciseManagementService.update` loads the exercise with `findById`, keeps it only if `isOwnedBy(trainer)`, else throws `ExerciseNotFoundException` (`exercise.not-found`, kind `NOT_FOUND`). A starter exercise has no owner, so `isOwnedBy` is false; unknown and foreign ids behave the same.
- **Rationale**: Identical answers for all three cases (spec FR-007) and the project rule "404, not 403". Same pattern as `InviteManagementService.own`.
- **Alternatives**: Query by `(id, owner)` in SQL — rejected; the domain rule stays testable without a database.

## R3: Ranges and where they live

- **Decision**: Constants in `Exercise`: sets 1–20, amount 1–3600, rest 0–600; existing name 100, equipment 50, cues 500. The constructor enforces them (`InvalidValueException`), and the request records reference the same constants in Bean Validation annotations so a bad value gets a per-field code before reaching the domain.
- **Rationale**: Invariants in the aggregate, field codes at the boundary (CLAUDE.md). One amount range for both measures keeps a cross-field rule out of the request; 3600 because the starter exercise bike is 1 × 1200 s and the aggregate also loads starter rows (`StarterExerciseSetIT` fails if one exceeded a limit). Largest starter values: 6 sets, 1200 amount, 90 s rest.
- **Alternatives**: 999 for amount (spec's first draft) — would reject the bike row on load. Separate ranges per measure — a cross-field annotation for little gain.

## R4: Name clash, in memory, plus an index for the race

- **Decision**: The service loads `findVisibleTo(trainer)` (starter and own) and refuses the name when any other exercise there has an equal trimmed, case-folded (`Locale.ROOT`) English or Polish name. A partial unique index `(owner_id, lower(name)) where owner_id is not null` makes two simultaneous creates leave one row; the adapter saves with `saveAndFlush` and turns a violation of that index into `ExerciseNameTakenException`.
- **Rationale**: ~40 rows, the listing already filters in memory, and Polish case folding is easier in Java. The index covers only the race (spec FR-010). Flushing inside the adapter makes the violation surface there, where it can be translated, not at commit.
- **Alternatives**: Index on starter names too — rejected, starter rows have an owner of null and a Polish column. Locking per trainer — more code than the race deserves. `ponytail:` comment on the service: Postgres `lower()` and Java case folding can differ for rare characters; the index then misses a clash that the in-memory check catches first.

## R5: Saving an update

- **Decision**: `ExerciseRepositoryImpl.save` loads the entity by id (or creates one), copies the aggregate's fields onto it with a hand-written `update` in `ExerciseMapper` (as `InviteMapper.update`), and calls `saveAndFlush`.
- **Rationale**: Keeps `createdAt` and the `@Version`; a detached entity with a null version would be inserted again. Own-exercise texts go into `name`, `equipment` and `cues`; the Polish columns stay `null` (agreed in the conversation about languages).
- **Alternatives**: `jpa.save(newEntity)` — breaks optimistic locking and resets `createdAt`.

## R6: Update is `PUT`, delete is not mapped

- **Decision**: `POST /api/v1/exercises` creates (`201`), `PUT /api/v1/exercises/{id}` replaces the editable fields (`200`). No `DELETE` is mapped; Spring answers `405`, which `GlobalExceptionHandler` already gives the code `request.method-not-allowed`.
- **Rationale**: The form always sends every field, so a full replacement is the simplest contract; "cannot be deleted" is then true by construction and a test proves the refusal.
- **Alternatives**: `PATCH` — partial-update rules nobody needs.

## R7: UI building blocks

- **Decision**: shadcn `select` for Category (the only new component); `radio-group` (present) for Measure; `field`, `input`, `textarea`, `dialog`, `alert`, `spinner`, `sonner` (present). Form with `react-hook-form` + `zod`, as `InviteDialog`.
- **Rationale**: The design system's field and dialog patterns, already used by the invite dialog; a native `<select>` would need hand-styling.
- **Alternatives**: `ToggleGroup` for Measure — the chips are already used for categories; two radio choices read better in a form.

## R8: Behaviour after a save

- **Decision**: On success the page resets the category chip to "All" and clears the search, sets the saved exercise as chosen and shows a Sonner toast ("{name} added to the library." / "{name} saved."). Mutations invalidate every `['exercises', …]` query.
- **Rationale**: Spec FR-018: the saved exercise must be visible whatever filter was set (an edit may change its category). `chosen` already falls back to the first row until the refetch arrives.
- **Alternatives**: Keep filters and hope the row matches — a saved exercise could vanish from the list.

## R9: Field errors from the server

- **Decision**: On `validation.failed` the dialog reads `errors[].field` and sets that field's single message (the same key the dialog's own check uses). `exercise.name-taken` sets the name error; `exercise.not-found` closes the dialog, refreshes the list and shows a toast; any other code shows `errors.<code>` (generic when unknown) as an alert inside the dialog.
- **Rationale**: One message per rule and none of the server's text (spec FR-017, FR-018, constitution II).
- **Alternatives**: Show the server's `detail` — forbidden.

## R10: Demo data

- **Decision**: `R__demo_accounts.sql` adds a confirmed `demo.trainer2@example.com`; new `R__demo_exercises.sql` gives the demo trainer three own exercises (one timed, one with no equipment and no cues) and trainer two one. README lists the new account.
- **Rationale**: Spec FR-023; the isolation check needs a second signed-in trainer, and the existing second trainer is unconfirmed and cannot sign in.

## R11: Cap on own exercises

- **Decision**: A trainer owns at most 200 exercises. `ExerciseManagementService.create` counts the exercises of `findVisibleTo(trainer)` that `isOwnedBy(trainer)` and throws `ExerciseLimitReachedException` (`exercise.limit-reached`, kind `CONFLICT`, `409`) at 200. Updates are never limited.
- **Rationale**: The listing returns the whole library in one response with no paging (spec #13 assumption); without a cap one trainer can grow that response, and every list, without bound. A state conflict (`409`), not a rate limit (`429`): waiting does not help, deleting is impossible, so the message says the limit is reached.
- **Alternatives**: A rate limit like invites — does not bound the total. Paging — more work than the story needs. No cap — accepted risk, rejected. `ponytail:` comment: simultaneous creates can overshoot by a few; count in SQL under a lock if it ever matters.

## R12: Unreadable values

- **Decision**: An unknown category or measure, or text where a number belongs, is rejected by Spring before validation with the generic `400 request.invalid`. A missing number reads as 0 and fails the range with `min`. The dialog never sends these, so no field message exists for them.
- **Rationale**: Only a request made outside the UI can produce them; the generic code leaks nothing.
