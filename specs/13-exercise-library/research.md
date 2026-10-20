# Research: Trainer browses the exercise library

Each entry: decision, reason, alternatives.

## R1 — Bilingual content: nullable Polish columns beside the English ones

**Decision**: `exercises` keeps `name`, `equipment`, `cues` (English, required) and adds `name_pl`, `equipment_pl`, `cues_pl` (nullable). A small value `LocalizedText(en, pl)` has `in(Locale)`: Polish when the language is `pl` and a Polish text exists, English otherwise. Used by three fields, so the abstraction earns its place.

**Why**: Two languages are fixed by the product (constitution VII). Own exercises of story #47 are written in one language, so Polish must be optional; nullable columns give that and need no join. Fallback "per field" (spec edge case) falls out of `in(Locale)`.

**Alternatives**: a translation table keyed by exercise and language (a join for every list, and a model for languages that do not exist); JSON column (no constraints, harder to search); Polish names only in the frontend's locale files (breaks for the trainer's own exercises and for plans later reading names).

## R2 — Filter and sort in the domain service, not in SQL

**Decision**: The port returns the starter exercises; `ExerciseListingService` filters by category and name and sorts with `java.text.Collator` for the request's language. No query methods, no paging.

**Why**: About 40 rows (hundreds at most for one trainer). Correct Polish ordering ("ł" after "l") and case-insensitive matching on two language columns are simple in Java and fragile in SQL (depends on the database collation installed). The name text never reaches SQL, which removes the wildcard problem (`%`, `_`) of the spec's edge cases. Domain rules stay unit-testable with a fake repository.

**Alternatives**: `ILIKE` with escaped wildcards and `ORDER BY ... COLLATE "pl-PL-x-icu"` (depends on how the image is built, harder to test); Specification/Criteria queries (more code for 40 rows).

**Ceiling**: if a trainer's own list grows to thousands, move filtering to SQL with paging in a later change.

## R3 — The port exposes `findStarter()`, not `findAll()`

**Decision**: `ExerciseRepository.findStarter()` returns exercises with no owner. Story #47 adds `findVisibleTo(owner)` and switches the listing to it.

**Why**: The table already has `owner_id` (task #14), so a `findAll()` would be one forgotten filter away from showing another trainer's exercise once #47 can create them. The name of the method says what is returned.

**Alternatives**: `findAll()` plus filtering in the service (leaks if forgotten); no `owner_id` until #47 (task #14 asks for the owner and #47 would then change the model).

## R4 — Query parameters bound to a record, so errors reuse the existing handler

**Decision**: `GET /api/v1/exercises?category=&name=` binds to `ListExercisesRequest(Category category, @Size(max = 100) String name)`. A bad enum value or a long name raises `MethodArgumentNotValidException` for a bound object; `GlobalExceptionHandler` already turns it into `validation.failed` with `errors: [{field, code}]` (`type-mismatch`, `size`).

**Why**: Meets FR-018 with no change in `common`. A controller test proves it; if Spring reports binding failures differently, the test fails and the handler gets a small override then.

**Alternatives**: `@RequestParam` with `@Validated` (raises `HandlerMethodValidationException`, which the handler would have to learn); a `String` category parsed by hand (error code invented by hand).

## R5 — "In plans" is a constant 0 until plans exist

**Decision**: Each listing entry carries `plansUsing`; the service sets 0 with a comment pointing to story #68.

**Why**: No plan data exists. An output port with no adapter is speculation; story #68 decides whose plans count and adds the port and the test (spec FR-015).

**Alternatives**: leave the field out and add it with #68 (changes the contract and the screen twice); a `PlanUsageCounter` port with a zero adapter (code that exists only to be replaced).

## R6 — Starter exercises: fixed ids, regular migration

**Decision**: `V4__add_starter_exercises.sql` inserts 40 rows with fixed UUIDs, `owner_id` null. It is a normal migration, not dev-data.

**Why**: Starter exercises must exist in production for every trainer (FR-008), so they are not demo data. Fixed ids are stable across environments, which plans will rely on to reference them. Existing accounts see them without any per-trainer copy.

**Alternatives**: `gen_random_uuid()` (ids differ per environment); a repeatable migration (would rewrite rows after plans reference them); seeding from Java at startup (hides data from the schema history).

## R7 — Frontend: a few state rules, no new library

**Decisions**:
- Server data: one `useExercises({ category, name })` query keyed by both. `placeholderData: keepPreviousData` keeps the old list on screen while a new one loads; a slower older response cannot replace a newer one, because each key has its own result and only the current key is shown.
- Search: the input keeps its own value; a tiny `useDebouncedValue` (300 ms) feeds the query. Settles within 1 s of the last keystroke (SC-002). No library.
- Selection: stored as an id. The shown exercise is the one with that id in the current list, else the first, else none. This covers "filtered out → first remaining" without effects.
- Chips: shadcn `ToggleGroup type="single"` with an `aria-label`; selecting the chosen chip again gives an empty value, which is ignored so one chip is always chosen. Whether it exposes radio semantics is checked in the component test and the browser; if it does not, items get `role="radio"` by hand.
- Table: shadcn `Table` with `data-density="compact"`; the name is a `button` in the first cell, the chosen one has `aria-current="true"` and an underline, so state is not colour only.
- Count line: `role="status"` so a change in "N of M" is announced politely.
- States: `Skeleton` rows while loading, `Alert` plus "Try again" on failure, shadcn `Empty` with "Clear filters" for no match.
- Text: `exercises.*` keys; category labels by `exercises.categories.<CODE>`; default and plans labels with i18next interpolation and plural forms (Polish has `one`, `few`, `many`, `other`).

**Why**: Follows `frontend/CLAUDE.md` (server state in TanStack Query, local state first, no hand-written types).

## R8 — Seeing the screen before sign-in exists

**Decision**: The endpoint stays authenticated. For the browser check, a throwaway local edit of `SecurityConfig` permits `GET /api/v1/exercises`; it is reverted and `git diff` is checked before the commit. The backend side is proven with `@WithMockUser` tests and an integration test that a session gets 200 and none gets 401.

**Why**: Constitution III and `CLAUDE.md`: never open an endpoint to make something work. The edit exists only in the working tree during verification.

**Alternatives**: test-only stubbed API in the browser (does not exercise the backend, language header or real data); a dev-only login bypass (the same risk, kept in code).

## R9 — Response language

**Decision**: The controller takes a `Locale` argument. `WebConfig` already resolves it from `Accept-Language` (English or Polish, English default), and the API client already sends `i18n.language`.

**Why**: Reuses the mechanism built for emails in story #2. When the frontend changes language, the query key includes the language so the list is fetched again.

## R10 — Categories on the wire

**Decision**: `Category` serialises as `CHEST`, `BACK`, `LEGS`, `SHOULDERS`, `ARMS`, `CORE`, `FULL_BODY`, `CARDIO`, `MOBILITY`; `Measure` as `REPS`, `SECONDS`. The frontend translates them. Stored as strings with check constraints.

**Why**: Enums are stored as strings (`backend/CLAUDE.md`); the issue's lower-case `reps`/`seconds` is the domain word, written here in the project's enum style. Labels are UI strings, so they live in the locale files, not in the database.
