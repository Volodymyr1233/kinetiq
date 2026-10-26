# Implementation Plan: Trainer adds and edits their own exercise

**Branch**: `feat/47-own-exercises` | **Date**: 2026-10-26 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/47-own-exercises/spec.md`

## Summary

A trainer adds an exercise of their own and edits it later; the library shows each trainer the starter exercises plus only their own. The backend extends the `exercise` feature: a `ManageExercisesUseCase` (create, update), `findVisibleTo` / `findById` / `save` on the repository, ownership in the use case with `404` for anything not the trainer's, a name-clash rule over the exercises the trainer sees, ranges in the aggregate, a unique index for simultaneous creates, a cap of 200 own exercises per trainer, and `POST` / `PUT` endpoints. The listing is scoped to the signed-in trainer and says which exercises are starter ones. The frontend adds one Add/Edit dialog, an "Add exercise" button, an "Edit" button and a "Starter" badge in the panel. One pull request, one commit per task #48 and #49, after a first docs commit with the Spec Kit files.

## Technical Context

**Language/Version**: Java 25; TypeScript 6

**Primary Dependencies**: Spring Boot 4.1 (MVC, Security 7, Data JPA, Flyway, Validation), MapStruct, Lombok; React 19, TanStack Query 5, react-i18next, react-hook-form, zod, shadcn/ui. New: shadcn `select` (R7). No new npm or Gradle dependency.

**Storage**: PostgreSQL 18; existing table `exercises` gets one partial unique index (migration `V9`, data-model.md).

**Testing**: JUnit 5 + AssertJ (unit with an in-memory fake repository, `@WebMvcTest`, `@DataJpaTest` + Testcontainers, the smoke `@SpringBootTest`), ArchUnit; Vitest + Testing Library with a stubbed `fetch`

**Target Platform**: Browser SPA, same origin as the API

**Project Type**: Web application (`backend/` + `frontend/`)

**Performance Goals**: Normal web expectations; the library is about 40 starter rows plus a trainer's own (spec #13 assumption, no paging)

**Constraints**: Ownership decided in the use case; a starter, foreign or missing exercise answers `404 exercise.not-found`; no server text in the UI; the aggregate rejects values the starter data does not exceed (R3)

**Scale/Scope**: 2 new endpoints, 1 changed endpoint, 1 index, 1 dialog, 2 small panel changes

## Constitution Check

| Principle | Status |
|---|---|
| I. Hexagonal boundaries | Pass. `Exercise` and the new service use only the JDK; the unique-index translation lives in the persistence adapter (R4). `exercise` already depends on `account.domain.model.AccountId`; no cycle. |
| II. One contract | Pass. `schema.d.ts` regenerated in #48; two new error codes translated by `code` in EN and PL; field errors use the existing `validation.failed` + `errors[]`. |
| III. Secure by default | Pass. No public endpoint; `/api/v1/exercises/**` already requires `TRAINER`. The owner comes from the session, never from the body (R1). Ownership checked in the use case with `404` (R2). Ranges and lengths bound every input (R3). Error bodies carry codes only. |
| IV. Plan changes | Not affected. |
| V. Tests verify behaviour | Pass. Tests below target ownership, the name clash, ranges, isolation between trainers, the race and the dialog states. |
| VI. Simplicity | Pass. No new port beyond three repository methods; the clash check runs in memory over the visible library like the listing does; no Polish fields, no delete, no archive. `ponytail:` ceilings marked (race on case folding, last save wins). |
| VII. One design system | Pass. shadcn `dialog`, `select`, `radio-group`, `textarea`, `field`, `sonner` only; board copy kept; EN and PL; browser check at 1280px, light and dark, keyboard. |

Re-checked after design: unchanged.

## Project Structure

### Documentation (this feature)

```text
specs/47-own-exercises/
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
│   │   ├── model/Exercise                       ranges, isStarter, isOwnedBy, edit
│   │   ├── input/ManageExercisesUseCase, CreateExerciseCommand, UpdateExerciseCommand,
│   │   │         ListExercisesQuery (+ trainer)
│   │   ├── output/ExerciseRepository            findVisibleTo, findById, save
│   │   ├── service/ExerciseManagementService, ExerciseListingService (visible library)
│   │   └── exception/ExerciseNotFoundException, ExerciseNameTakenException,
│   │                 ExerciseLimitReachedException
│   └── infrastructure/
│       ├── config/ExerciseConfig                management use case
│       ├── input/ExerciseController (+ POST, PUT), CreateExerciseRequest,
│       │         UpdateExerciseRequest, ExerciseResponse (+ starter), ExerciseWebMapper
│       └── output/ExerciseRepositoryImpl, JpaExerciseRepository, ExerciseMapper (update)
├── main/resources/db/migration/V9__add_exercise_name_index.sql
└── main/resources/db/dev-data/R__demo_accounts.sql (second trainer), R__demo_exercises.sql

frontend/src/
├── components/ui/select.tsx                     shadcn CLI
├── features/exercise/
│   ├── api/exercises.ts                         useCreateExercise, useUpdateExercise
│   ├── exercise-schema.ts (+ .test.ts)          zod rules, message keys
│   ├── components/exercise-dialog.tsx           Add / Edit
│   ├── components/exercise-detail.tsx           Edit, Starter badge, "Not set"
│   ├── components/exercise-table.tsx            "Not set"
│   └── pages/exercise-library-page.tsx (+ test) Add button, empty state, after-save reset
├── lib/api/schema.d.ts                          npm run gen:api
└── locales/en.json, pl.json
README.md                                        second demo trainer
```

**Structure Decision**: Everything stays in the `exercise` feature on both sides. `common` is not touched: `CONFLICT` and `NOT_FOUND` kinds and the `/api/v1/exercises/**` trainer rule already exist.

## Work per task (one commit each)

| Task | Commit | Contents |
|---|---|---|
| spec | `docs: add spec for trainer own exercises` | Spec Kit files of this story |
| #48 | `feat(backend): add create and update exercise api (#48)` | Aggregate ranges and `edit`; use cases, service, exceptions; `findVisibleTo` replaces `findStarter`; adapter `save`; `V9`; controller, requests, `starter` in the response; listing scoped to the trainer; demo data and README; `schema.d.ts`; tests |
| #49 | `feat(frontend): add and edit exercise form (#49)` | shadcn `select`; mutations; schema; dialog; Add / Edit / Starter / Not set in the page; EN and PL copy and error codes; tests; browser verification |

## Tests

| Test | Covers |
|---|---|
| `ExerciseTest` (extended) | each range at its edges and one past (sets 0/1/20/21, amount 0/1/3600/3601, rest -1/0/600/601); blank name; `edit` keeps id, owner and measure and revalidates; `isOwnedBy` false for a starter exercise |
| `ExerciseManagementServiceTest` (in-memory repository) | create: saved with the trainer as owner, text trimmed; a starter name in English or in Polish, another own name, any letter case and surrounding spaces → `exercise.name-taken`, nothing saved; another trainer's same name → saved; the 200th own exercise is saved, the 201st → `exercise.limit-reached`, starter and other trainers' exercises do not count. update: own → replaced, measure unchanged; own name unchanged → no clash with itself; name of another visible exercise → name-taken; starter, another trainer's and unknown id → `exercise.not-found` with nothing changed |
| `ExerciseListingServiceTest` (extended) | own exercises listed with starter ones and counted in the total; another trainer's absent from list, search and total |
| `ExerciseControllerTest` (`@WebMvcTest`) | `POST` `201` with the response shape and `starter: false`; `PUT` `200`; field errors per rule (`not-blank`, `size`, `min`, `max`, `not-null`); `409`, `404` codes; the owner is the session's account, not a body field; client role → `403 request.not-allowed`; anonymous → `401`; missing CSRF → `403 request.forbidden`; `DELETE` on an exercise and on the collection → `405 request.method-not-allowed`; `409 exercise.limit-reached`; an unknown category or a text where a number belongs → `400 request.invalid` without server text; a use case throwing an unexpected exception → `500 internal.error` without exception text; an own exercise with no Polish text reads the same under `Accept-Language: pl`; listing passes the trainer to the use case |
| `ExerciseRepositoryImplIT` (extended) | `findVisibleTo` returns starter and own, never another trainer's; `save` inserts, then updates the same row keeping `createdAt` and bumping `version`; two own exercises with names differing only in letter case → `ExerciseNameTakenException`; the same name for two trainers is allowed; a starter name is not blocked by the index |
| `StarterExerciseSetIT` | reads through `findVisibleTo` instead of the removed `findStarter()`; proves the aggregate ranges accept every starter exercise |
| `KinetiqBackendApplicationIT` (extended) | as two trainers against the real database: create → list → update; each sees only their own; the second trainer's `PUT` on the first's exercise → `404`; `PUT` on a starter exercise → `404`; `DELETE` → `405` |
| `exercise-schema.test.ts` | each rule's message key at its edge; spaces-only name; decimal or empty number |
| `exercise-library-page.test.tsx` (extended) | Add opens with the board defaults and Name focused; empty name → "Enter a name." with focus on the field; success posts, resets filters, selects the new exercise and announces it; name-taken from the server shows on the name field; seconds measure relabels the amount; switching the measure keeps the typed numbers; `409 exercise.limit-reached` shows as an alert and keeps the dialog; starter shows "Starter" and no Edit; own shows Edit, the dialog is filled, the measure is read-only, save sends `PUT`; busy button blocks a second request; a failed save keeps the dialog and entries; `404` closes the dialog and refreshes; "Not set" for empty equipment; empty state offers both buttons |

## Follow-ups outside this story

- #68 (plans) supplies the real "In plans" count and decides what editing an exercise used in plans means.
- #179 gives trainers a phone layout; the dialog then becomes a drawer below 768px (design system).
- If another migration takes `V9` first, renumber (backend/CLAUDE.md): `V8` is taken by the open pull request #204.

## Complexity Tracking

None.
