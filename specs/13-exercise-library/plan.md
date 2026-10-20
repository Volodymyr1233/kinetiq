# Implementation Plan: Trainer browses the exercise library

**Branch**: `feat/13-exercise-library` | **Date**: 2026-10-20 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/13-exercise-library/spec.md`

## Summary

A read-only exercise library: about 40 starter exercises stored in both English and Polish, one authenticated endpoint that lists them filtered by category and name in the UI's language, and a trainer screen with category chips, search, a table and a detail panel. The backend gets its second feature (`exercise`); the frontend replaces the `/trainer/exercises` placeholder. One pull request, one commit per task #14–#17, in that order, after a first docs commit with the spec.

## Technical Context

**Language/Version**: Java 25; TypeScript 6

**Primary Dependencies**: Spring Boot 4.1 (MVC, Security 7, Data JPA, Flyway, Validation), MapStruct, Lombok; React 19, React Router 8, TanStack Query 5, react-i18next, shadcn/ui. No new dependency. New shadcn components added through the CLI: `table`, `badge`, `toggle-group` (with `toggle`), `empty`.

**Storage**: PostgreSQL 18, table `exercises` (data-model.md); starter rows inserted by a regular migration (R6)

**Testing**: JUnit 5 + AssertJ (unit with a fake repository, `@WebMvcTest`, `@DataJpaTest` + Testcontainers), ArchUnit; Vitest + Testing Library with a stubbed `fetch`

**Target Platform**: Browser SPA, same origin as the API

**Project Type**: Web application (`backend/` + `frontend/`)

**Performance Goals**: List responds in well under 100 ms (about 40 rows, filtered in memory, R2); the screen shows a settled result within 1 s of the last keystroke (SC-002)

**Constraints**: Endpoint stays authenticated, no change to `SecurityConfig` (R8); no server text in the UI; response language follows `Accept-Language` already sent by the client (R9)

**Scale/Scope**: 1 route, 1 endpoint, 1 table, 40 rows

## Constitution Check

| Principle | Status |
|---|---|
| I. Hexagonal boundaries | Pass. Domain uses only the JDK (`Locale`, `Collator`) and the `account` feature's `AccountId`; no cycle (`account` does not know `exercise`). Repository is an output port; the controller reaches data only through `ListExercisesUseCase`. |
| II. One contract | Pass. `schema.d.ts` regenerated in the commit that adds the endpoint (#16). Errors are problem details; a bad category or an over-long name returns `validation.failed` with per-field codes through the existing handler (R4). |
| III. Secure by default | Pass. No public endpoint; `SecurityConfig` untouched, so the listing is 401 without a session, proven by an integration test. The repository port exposes only starter exercises (R3), so #47 cannot leak owned ones by forgetting a filter. Name text is bound as a parameter, never concatenated into SQL (R2: no SQL at all). |
| IV. Plan changes | Not affected. |
| V. Tests verify behaviour | Pass. Test list below targets invariants, filtering, ordering, language fallback, the migration's data guarantees, error mapping and screen behaviour only. |
| VI. Simplicity | Pass. No translation table (R1), no SQL filtering, paging or caching (R2), no plan-count port (R5), no detail endpoint, no owner logic beyond a nullable column (R3). |
| VII. One design system | Pass. shadcn components with Kinetiq tokens; EN and PL; browser check at 1280 px, light and dark. |

Re-checked after design: unchanged.

### Definition of Done deviations (`CLAUDE.md`)

Two items of the Definition of Done do not apply as written, by design; the pull request description repeats this.

- *New entities get demo data in the local-only Flyway location*: the starter exercises are real data every environment needs, so they are a regular migration (R6). Owned exercises cannot exist before story #47, and `findStarter()` would not show them anyway, so local demo rows of that kind would be invisible.
- *Another person's resource answers 404, and a test proves it*: no operation addresses an exercise by id, and starter exercises belong to no one. The ownership test arrives with story #47.

## Project Structure

### Documentation (this feature)

```text
specs/13-exercise-library/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/api.md
├── checklists/requirements.md
└── tasks.md             (/speckit-tasks)
```

### Source Code

```text
backend/src/
├── main/java/com/kinetiq/exercise/
│   ├── domain/
│   │   ├── model/Exercise, ExerciseId, Category, Measure, LocalizedText
│   │   ├── input/ListExercisesUseCase, ListExercisesQuery, ExerciseListing
│   │   ├── output/ExerciseRepository
│   │   └── service/ExerciseListingService
│   └── infrastructure/
│       ├── config/ExerciseConfig
│       ├── input/ExerciseController, ListExercisesRequest, ExerciseResponse, ExerciseListResponse, ExerciseWebMapper
│       └── output/ExerciseEntity, JpaExerciseRepository, ExerciseRepositoryImpl, ExerciseMapper
├── main/resources/db/migration/V3__create_exercises.sql, V4__add_starter_exercises.sql
└── test/java/com/kinetiq/exercise/...             tests listed below
   test/java/com/kinetiq/KinetiqBackendApplicationIT.java   extended: library is 401 anonymously

frontend/src/
├── app/router.ts                                  /trainer/exercises loads the page (placeholder removed)
├── components/ui/                                 table, badge, toggle, toggle-group, empty (shadcn CLI)
├── features/exercise/
│   ├── api/exercises.ts                           useExercises
│   ├── components/exercise-table.tsx, exercise-detail.tsx
│   ├── pages/exercise-library-page.tsx (+ .test.tsx)
│   └── format.ts                                  default and plans labels helpers, only if two components share them
├── hooks/use-debounced-value.ts
├── lib/api/schema.d.ts                            regenerated (#16)
└── locales/en.json, pl.json                       exercises.*
```

**Structure Decision**: Feature `exercise` on both sides, matching `CONTEXT.md`'s Exercise and Exercise library terms. Everything about the exercise stays in the feature; `common` is not touched.

## Work per task (one commit each)

| Task | Commit | Contents |
|---|---|---|
| — | `docs: add spec for exercise library` | Spec Kit files (spec, plan, research, data model, contract, quickstart, tasks, checklist) |
| #14 | `feat(backend): add exercise model and migration (#14)` | `Exercise` with invariants, `ExerciseId`, `Category`, `Measure`, `LocalizedText`, `ExerciseRepository.findStarter`, entity, mapper, impl, `V3`, persistence IT |
| #15 | `feat(backend): add starter exercise set (#15)` | `V4` with 40 bilingual exercises; IT proving the guarantees of FR-006/FR-007 |
| #16 | `feat(backend): add exercise list and search api (#16)` | use case, query, listing, service, config, controller, request/response, web mapper, anonymous-401 check, `schema.d.ts` |
| #17 | `feat(frontend): add exercise library screen (#17)` | shadcn components, hook, API hook, page with table, chips, search, detail, states, locales, router, browser verification |

## Tests

| Test | Covers |
|---|---|
| `ExerciseTest` | blank English name, name over 100 characters in either language, sets 0, amount 0, negative rest refused; rest 0 accepted; owner optional |
| `LocalizedTextTest` | Polish when asked for Polish and present; English when Polish is absent, blank, or the language is anything else |
| `ExerciseListingServiceTest` (fake repository) | no filter returns all and the total; category filter; name filter ignores case and surrounding spaces and finds the English or the Polish name; both filters together; blank name is no filter; wildcard characters match literally; Polish order puts "łydki" after "lina" (collation); order is stable for equal names; total ignores filters; every entry reports 0 plans |
| `ExerciseRepositoryImplIT` | round trip of an exercise with and without Polish text and owner; `findStarter` skips an owned row; check constraints refuse sets 0 and an unknown category |
| `StarterExerciseSetIT` (`@DataJpaTest` on the migrated schema) | 40 starter rows; each category at least 3; at least 3 timed, Plank among them; every row has Polish name, equipment and cues; sets and amount at least 1 |
| `ExerciseControllerTest` (`@WebMvcTest`, use case mocked) | filter and search parameters reach the use case; `Accept-Language: pl` becomes the locale; response carries the text in that language and `plansUsing`; unknown category and 101-character name → 400 `validation.failed` with the field; anonymous → 401 `request.unauthenticated` |
| `KinetiqBackendApplicationIT` | extended: `GET /api/v1/exercises` anonymously → 401, with a session → 200 and 40 exercises |
| `exercise-library-page.test.tsx` | rows render with default and plans labels ("Not used", "1 plan"); first row selected and detail shown; choosing a row updates the panel; a chip filters and announces the count; typing waits for a pause then asks once; filtered-out selection moves to the first remaining one; no match → empty state, "Clear filters" resets; error → message by code (generic when unknown), "Try again" refetches; Polish plural labels; chosen chip, count region and chosen row exposed to assistive technology; Polish UI sends `Accept-Language: pl` and shows Polish category names |

## Follow-ups outside this story

- Story #47 adds per-trainer scoping: a `findVisibleTo(owner)` port method replaces `findStarter` in the listing, the controller reads the signed-in trainer, and the test that another trainer's exercise is absent lands there (comment on #16 records this).
- Story #68 replaces the constant `plansUsing = 0` with a real count.
- Story #18 makes the screen reachable in a browser without a local change (R8).

## Complexity Tracking

None.
