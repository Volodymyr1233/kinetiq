---
description: "Tasks for story #13, Trainer browses the exercise library"
---

# Tasks: Trainer browses the exercise library

**Input**: `specs/13-exercise-library/` (spec.md, plan.md, research.md, data-model.md, contracts/api.md, quickstart.md)

**Tests**: Required by the Definition of Done (`CLAUDE.md`) and principle V. Tests sit in the same phase as the code they cover.

**Organization**: Phases follow the GitHub tasks, not the spec's user stories, because the pull request carries one commit per task (`CLAUDE.md`, Workflow). #14 is the model, #15 the starter data (US5), #16 the backend of US1–US4, #17 the screen of US1–US4. Story labels show which spec story a task serves.

**Paths**: `B` = `backend/src/main/java/com/kinetiq`, `BT` = `backend/src/test/java/com/kinetiq`, `R` = `backend/src/main/resources`, `F` = `frontend/src`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: spec user story (US1 see the library, US2 filter, US3 plan usage, US4 needs sign-in, US5 starter set)

---

## Phase 1: Setup — spec commit

- [X] T001 Add the terms **Category** ("The group an exercise belongs to: Chest, Back, Legs, Shoulders, Arms, Core, Full body, Cardio or Mobility."), **Measure** ("What an exercise counts: repetitions or seconds.") and **Cues** ("Short coaching instructions for doing an exercise well.") under Planning in `CONTEXT.md`, after **Starter exercise**, with `_Avoid_` lines only where a synonym is a trap; commit them with `specs/13-exercise-library/` as `docs: add spec for exercise library`

---

## Phase 2: task #14, exercise model and migration

**Goal**: The `exercises` table exists, Hibernate validates it on start, and an exercise can be saved and read back, with its invariants enforced.

- [X] T002 [P] Create `Category` enum (`CHEST`, `BACK`, `LEGS`, `SHOULDERS`, `ARMS`, `CORE`, `FULL_BODY`, `CARDIO`, `MOBILITY`) and `Measure` enum (`REPS`, `SECONDS`) in `B/exercise/domain/model/`
- [X] T003 [P] Create `ExerciseId` (record wrapping `UUID`, `random()`) in `B/exercise/domain/model/ExerciseId.java`, as `AccountId`
- [X] T004 [P] Create `LocalizedText` record in `B/exercise/domain/model/LocalizedText.java`: `String en`, `String pl`; compact constructor requires non-null `en` and normalises a null `pl` to `""`; `String in(Locale)` returns `pl` when `locale.getLanguage()` is `pl` and `pl` is not blank, else `en`
- [X] T005 [P] Write `BT/exercise/domain/model/LocalizedTextTest.java`: Polish asked and present → Polish; Polish asked but empty or blank → English; English asked → English; any other language → English
- [X] T006 Create `Exercise` in `B/exercise/domain/model/Exercise.java` (`@Getter`, no setters, public constructor that validates): `ExerciseId id`, `Optional<AccountId> getOwner()` over a nullable field, `LocalizedText name`, `Category category`, `LocalizedText equipment`, `Measure measure`, `int defaultSets`, `int defaultAmount`, `int restSeconds`, `LocalizedText cues`. Invariants throw `InvalidValueException`: name "English required, not blank, at most 100 characters; Polish at most 100"; equipment "English required, at most 50; Polish at most 50"; cues "English may be empty, at most 500; Polish at most 500"; `defaultSets` "at least 1"; `defaultAmount` "at least 1"; `restSeconds` "at least 0". Method `boolean matches(String text)`: trimmed text lower-cased with `Locale.ROOT` is part of the lower-cased English or Polish name; blank text matches
- [X] T007 Write `BT/exercise/domain/model/ExerciseTest.java` (`@Nested` per method): blank English name, 101-character name in English and in Polish, sets 0, amount 0, rest -1 refused; rest 0 and a 100-character name accepted; no owner by default; `matches` finds English and Polish names ignoring case and surrounding spaces, blank text matches all, `%` and `_` match only themselves, "squat back" does not match "Back squat"
- [X] T008 [P] Write `R/db/migration/V3__create_exercises.sql` exactly as data-model.md "Table `exercises`": `pk_exercises`; `fk_exercises_owner` on `owner_id uuid null` → `accounts(id)`; index `ix_exercises_owner_id`; `name varchar(100) not null` with `ck_exercises_name_not_blank` (`trim(name) <> ''`); `name_pl varchar(100)`; `category varchar(16) not null` with `ck_exercises_category` (the nine values); `equipment varchar(50) not null`, `equipment_pl varchar(50)`; `measure varchar(16) not null` with `ck_exercises_measure` (`REPS`, `SECONDS`); `default_sets integer not null` with `ck_exercises_default_sets` (`>= 1`); `default_amount integer not null` with `ck_exercises_default_amount` (`>= 1`); `rest_seconds integer not null` with `ck_exercises_rest_seconds` (`>= 0`); `cues varchar(500) not null`, `cues_pl varchar(500)`; `created_at`, `updated_at timestamptz not null`; `version bigint not null`
- [X] T009 Create `ExerciseRepository` port in `B/exercise/domain/output/` with `List<Exercise> findStarter()` (exercises with no owner; javadoc: story #47 adds `findVisibleTo`), and in `B/exercise/infrastructure/output/`: `ExerciseEntity` (table `exercises`, extends `BaseEntity`, `@Id UUID`, enums as strings, nullable `*Pl` fields, `UUID ownerId`), `JpaExerciseRepository` (`List<ExerciseEntity> findByOwnerIdIsNull()`), `ExerciseMapper` (MapStruct; hand-written `toDomain` through the `Exercise` constructor, empty-string for a null Polish text; no entity-from-domain direction until story #47 saves exercises) and `ExerciseRepositoryImpl` (`@Repository`)
- [X] T010 Write `BT/exercise/infrastructure/output/ExerciseRepositoryImplIT.java` (`@DataJpaTest`, `TestcontainersConfiguration`): an exercise with Polish texts and one without round-trip intact; `findStarter` returns an ownerless exercise and skips one owned by an account; the database refuses `default_sets` 0, `default_amount` 0, an unknown category and a blank name. The port has no `save` (nothing creates exercises yet), so the test stores rows through `JpaExerciseRepository` and reads them through `ExerciseRepositoryImpl`
- [X] T011 Run `./gradlew check` and `./gradlew bootRun` (V3 applied, Hibernate validates); commit `feat(backend): add exercise model and migration (#14)`

---

## Phase 3: task #15, starter exercise set (US5)

**Goal**: A fresh database holds 40 starter exercises in both languages, the same for every trainer.

- [ ] T012 [US5] Write `R/db/migration/V4__add_starter_exercises.sql`: 40 inserts into `exercises`, `owner_id` null, ids `00000000-0000-4000-9100-0000000000NN` with `NN` = 01–40 in the order of the catalogue below, `created_at`/`updated_at` `now()`, `version` 0. Every row has English and Polish `name`, `equipment` and `cues` ("natural gym language a Polish trainer would use", spec FR-007), `category`, `measure`, `default_sets`, `default_amount`, `rest_seconds`. Cues are one or two short imperative sentences, as on the board. Timed rows use `SECONDS`
- [ ] T013 [US5] Write `BT/exercise/infrastructure/output/StarterExerciseSetIT.java` (`@DataJpaTest` on the migrated schema, using `findStarter`): exactly 40 starter exercises; every `Category` has at least 3; at least 3 are `SECONDS` and Plank is one of them; every exercise has Polish name, equipment and cues, none blank; sets and amount at least 1; no two ids equal; Polish names are not equal to English ones for at least 30 rows (catches a pasted English copy; a few names are the same word in both languages)
- [ ] T014 Run `./gradlew check`; commit `feat(backend): add starter exercise set (#15)`

### Starter catalogue (English / Polish names; defaults and texts are written in T012)

| # | Category | Measure | English / Polish |
|---|---|---|---|
| 01 | Chest | REPS | Bench press / Wyciskanie sztangi leżąc |
| 02 | Chest | REPS | Incline dumbbell press / Wyciskanie hantli na ławce skośnej |
| 03 | Chest | REPS | Push-up / Pompki |
| 04 | Chest | REPS | Dumbbell fly / Rozpiętki z hantlami |
| 05 | Chest | REPS | Cable crossover / Krzyżowanie linek wyciągu |
| 06 | Back | REPS | Lat pulldown / Ściąganie drążka wyciągu górnego |
| 07 | Back | REPS | Seated cable row / Wiosłowanie na wyciągu siedząc |
| 08 | Back | REPS | Bent-over row / Wiosłowanie sztangą w opadzie |
| 09 | Back | REPS | Pull-up / Podciąganie na drążku |
| 10 | Back | SECONDS | Dead hang / Zwis na drążku |
| 11 | Legs | REPS | Back squat / Przysiad ze sztangą |
| 12 | Legs | REPS | Romanian deadlift / Martwy ciąg rumuński |
| 13 | Legs | REPS | Bulgarian split squat / Przysiad bułgarski |
| 14 | Legs | REPS | Leg press / Wypychanie ciężaru na suwnicy |
| 15 | Legs | REPS | Leg curl / Uginanie nóg na maszynie |
| 16 | Legs | SECONDS | Wall sit / Krzesełko przy ścianie |
| 17 | Shoulders | REPS | Overhead press / Wyciskanie sztangi nad głowę |
| 18 | Shoulders | REPS | Lateral raise / Unoszenie hantli bokiem |
| 19 | Shoulders | REPS | Face pull / Face pull na wyciągu |
| 20 | Shoulders | REPS | Rear delt fly / Odwodzenie hantli w opadzie |
| 21 | Arms | REPS | Biceps curl / Uginanie ramion z hantlami |
| 22 | Arms | REPS | Hammer curl / Uginanie młotkowe |
| 23 | Arms | REPS | Triceps pushdown / Prostowanie ramion na wyciągu |
| 24 | Arms | REPS | Overhead triceps extension / Prostowanie ramienia z hantlem zza głowy |
| 25 | Arms | REPS | Bench dip / Pompki tylne na ławce |
| 26 | Core | SECONDS | Plank / Deska |
| 27 | Core | SECONDS | Side plank / Deska boczna |
| 28 | Core | REPS | Dead bug / Martwy robak |
| 29 | Core | REPS | Hanging knee raise / Unoszenie kolan w zwisie |
| 30 | Core | REPS | Pallof press / Wyciskanie Pallofa |
| 31 | Full body | REPS | Deadlift / Martwy ciąg |
| 32 | Full body | REPS | Kettlebell swing / Swing kettlebell |
| 33 | Full body | SECONDS | Farmer's carry / Spacer farmera |
| 34 | Full body | REPS | Burpee / Burpees |
| 35 | Cardio | SECONDS | Rowing intervals / Interwały na wioślarzu |
| 36 | Cardio | SECONDS | Easy bike ride / Spokojna jazda na rowerze |
| 37 | Cardio | SECONDS | Jump rope / Skakanka |
| 38 | Mobility | REPS | Hip 90/90 / Mobilizacja bioder 90/90 |
| 39 | Mobility | REPS | Cat-cow / Kot–krowa |
| 40 | Mobility | REPS | Thoracic rotation / Rotacje klatki piersiowej |

Counts: Chest 5, Back 5, Legs 6, Shoulders 4, Arms 5, Core 5, Full body 4, Cardio 3, Mobility 3. Timed: 10, 16, 26, 27, 33, 35, 36, 37.

---

## Phase 4: task #16, exercise list and search API (US1, US2, US3, US4)

**Goal**: `GET /api/v1/exercises` lists the starter exercises, filtered by category and name, in the request's language; anonymous requests get 401. `schema.d.ts` is regenerated.

- [ ] T015 [US1] Create `ListExercisesQuery` (record: `Category category` nullable, `String name`, `Locale locale`), `ExerciseListing` (record: `List<Entry> entries`, `int total`; nested `record Entry(Exercise exercise, int plansUsing)`) and `ListExercisesUseCase` (`ExerciseListing list(ListExercisesQuery)`) in `B/exercise/domain/input/`
- [ ] T016 [US1] [US2] [US3] Create `ExerciseListingService` in `B/exercise/domain/service/` (plain class, constructor takes `ExerciseRepository`): load `findStarter()`; `total` = its size; keep exercises matching the category (when given) and `matches(name)`; sort by `exercise.getName().in(locale)` with `Collator.getInstance(locale)` set to `Collator.PRIMARY`, then by id; every entry `plansUsing` 0 with the comment `plans arrive with story #68, which counts them`
- [ ] T017 [US1] [US2] [US3] Write `BT/exercise/domain/service/ExerciseListingServiceTest.java` with a hand-written fake repository: no filter → all and total; category filter; name filter by English and by Polish name; both filters together; filters leave `total` unchanged; Polish order puts a name starting "Ł" after one starting "L" and English order does not; equal names keep a fixed order; plans used is 0
- [ ] T018 [US1] Create `ExerciseConfig` in `B/exercise/infrastructure/config/` registering `ListExercisesUseCase` as an `ExerciseListingService` bean
- [ ] T019 [US1] [US2] Create in `B/exercise/infrastructure/input/`: `ListExercisesRequest` (record: `Category category`, `@Size(max = 100) String name`), `ExerciseResponse` (record: `id`, `name`, `category`, `equipment`, `measure`, `sets`, `amount`, `restSeconds`, `cues`, `plansUsing`), `ExerciseListResponse` (record: `int total`, `List<ExerciseResponse> exercises`), `ExerciseWebMapper` (picks `in(locale)` for name, equipment, cues) and `ExerciseController` (`@RestController`, `GET /api/v1/exercises`, takes `@Valid ListExercisesRequest` bound from the query string and `Locale`, calls one use case). Do not touch `SecurityConfig`
- [ ] T020 [US1] [US2] [US4] Write `BT/exercise/infrastructure/input/ExerciseControllerTest.java` (`@WebMvcTest`, use case mocked, `@WithMockUser`): parameters reach the use case; `Accept-Language: pl` gives a Polish locale and the response carries the text in that language; an unknown language gives English; `plansUsing` and `total` are in the body; `category=NOPE` → 400 `validation.failed` with a `category` error; a 101-character `name` → 400 with a `name` error; no user → 401 `request.unauthenticated`. If Spring reports binding failures differently from research R4, add the smallest override to `GlobalExceptionHandler` and a test for it
- [ ] T021 [US4] Extend `BT/KinetiqBackendApplicationIT.java`: `GET /api/v1/exercises` without a session → 401 with `code: request.unauthenticated` and no exercise in the body; with a mock user → 200, `total` 40
- [ ] T022 Start the backend, run `npm run gen:api` in `frontend/`, check that `F/lib/api/schema.d.ts` gained only `/api/v1/exercises` and its schemas
- [ ] T023 Run `./gradlew check`; commit `feat(backend): add exercise list and search api (#16)`

---

## Phase 5: task #17, exercise library screen (US1, US2, US3)

**Goal**: `/trainer/exercises` shows the library as on the board "Trainer · exercise library": chips, search, table, detail panel, with loading, error and empty states, in English and Polish, light and dark.

- [ ] T024 Add shadcn components with the CLI in `frontend/`: `npx shadcn@latest add table badge toggle-group empty`; read the files it adds; fetch the docs of each with `npx shadcn@latest docs <name>` first (`frontend/CLAUDE.md`)
- [ ] T025 [P] Create `F/hooks/use-debounced-value.ts` (`useDebouncedValue<T>(value: T, delayMs: number): T`, timer cleared on change and unmount) and `F/hooks/use-debounced-value.test.ts`: value changes only after the delay, and only the last of several quick changes arrives (fake timers)
- [ ] T026 [P] Add `exercises.*` to `F/locales/en.json` and `F/locales/pl.json`, same keys in both: `title`, `count` ("{{shown}} of {{total}} exercises"), `categories.{ALL,CHEST,BACK,LEGS,SHOULDERS,ARMS,CORE,FULL_BODY,CARDIO,MOBILITY}`, `categoryFilter` (group label "Category"), `search` (label and placeholder), column headings (`exercise`, `category`, `equipment`, `default`, `plans`), `default.reps` ("{{sets}} × {{amount}}"), `default.seconds` ("{{sets}} × {{amount}} s"), `plans` with plural forms (English `one`/`other`, Polish `one`/`few`/`many`/`other`) and `notUsed` ("Not used"), `detail.*` (`restSeconds` "{{seconds}} s", `plansUsing`, `notUsedYet` "Not used yet", `cues`, `equipment`, `rest`, `default`), `empty.title`, `empty.body`, `empty.clear` ("Clear filters"), `error.retry` ("Try again"). Sentence case, no exclamation marks
- [ ] T027 [US1] [US2] Create `F/features/exercise/api/exercises.ts`: `useExercises({ category, name })` → `useQuery` keyed by `['exercises', language, category, name]`, calling `api.GET('/api/v1/exercises', { params: { query } })` (omit empty values), throwing the problem detail on error as `features/account/api/account.ts` does, `placeholderData: keepPreviousData`
- [ ] T028 [P] [US1] [US3] Create `F/features/exercise/components/exercise-detail.tsx` (`Card` with `CardHeader` and `CardContent`: category `Badge`, name as `h2`, equipment, default, rest with the `num` class, "In plans" — "Not used yet" or the plural label — and cues) and `F/features/exercise/components/exercise-table.tsx` (`Table`, `data-density="compact"`, columns Exercise, Category, Equipment, Default, In plans; the name is a `button` with `aria-current="true"` and an underline on the chosen row; measured values use `num`)
- [ ] T029 [US1] [US2] [US3] Create `F/features/exercise/pages/exercise-library-page.tsx`: heading with `role="status"` count line; `ToggleGroup type="single"` of "All" + nine categories with `aria-label`, ignoring an empty value so one chip stays chosen; search `Input` with a label; the chosen exercise held as an id and resolved to the one in the list, else the first, else none; `useDebouncedValue(search, 300)` feeds the query; layout as the board at 1280px (table left, panel right, wrapping under the table when narrow); loading with `Skeleton` rows, error with `Alert` showing `t(errorMessageKey(error))` from `@/lib/api/problem` and a "Try again" button that refetches, empty with `Empty` and "Clear filters"; no "Add exercise" or "Edit" (spec FR-026); colours from tokens only
- [ ] T030 [US1] Replace `trainerPlaceholder('exercises')` in `F/app/router.ts` with a lazy route loading `ExerciseLibraryPage` (`HydrateFallback: BlankWhileLoading`); fix `F/app/router.test.tsx` and `F/app/trainer-area.test.tsx` if they assumed the placeholder there
- [ ] T031 [US1] [US2] [US3] Write `F/features/exercise/pages/exercise-library-page.test.tsx` with `stubApi` from `F/test/page.tsx`: rows with default and plans labels ("Not used", "1 plan"); the first row chosen and its detail shown; choosing a row updates the panel; a chip narrows the list and changes the count; typing sends one request after the pause; a filtered-out choice moves to the first remaining; no match → empty state and "Clear filters" resets; failure with an unknown code → generic message, with `request.unauthenticated` → "Sign in to continue.", and "Try again" refetches; Polish UI sends `Accept-Language: pl` and shows Polish category names; plural labels in Polish for 1, 2 and 5 plans ("1 plan", "2 plany", "5 planów"); accessibility: the chosen chip is exposed as checked and exactly one is, the count line is a `status` region whose text changes with the filter, the chosen row's name button has `aria-current`
- [ ] T032 [US1] Verify in the browser as `quickstart.md` step 3 says: apply the throwaway `SecurityConfig` permit, run backend and frontend, check 1280px, light and dark, English and Polish, keyboard and focus, empty and error states, console; take screenshots; then revert the edit and confirm `git diff` shows only this task's files
- [ ] T033 Run `npm run check`; commit `feat(frontend): add exercise library screen (#17)`

---

## Phase 6: Final checks

- [ ] T034 Walk through `quickstart.md` sections 1, 2 and 4 against the finished branch; confirm `git log --oneline master..` shows the docs commit and one commit per task, and that `SecurityConfig` is unchanged from `master`
- [ ] T035 Open the pull request (`Closes #13`, `Closes #14`, `Closes #15`, `Closes #16`, `Closes #17`) after the user agrees; title `feat: add exercise library` per the commit convention

---

## Dependencies

- T001 first. #14 (T002–T011) → #15 (T012–T014) → #16 (T015–T023) → #17 (T024–T033).
- Inside #14: T002–T004 and T008 are independent; T006 needs T002–T004; T009 needs T006 and T008.
- Inside #16: T015 → T016 → T018 and T019; T022 needs the backend running with T019.
- Inside #17: T024, T025, T026 are independent; T028 needs T024 and T026; T029 needs T025, T027 and T028; T032 needs everything before it.

## Implementation strategy

Build in order and commit after each task's gate (`./gradlew check`, `npm run check`). The branch is useful from #16 (a tested API) and complete after #17. Nothing in this story depends on sign-in, which arrives with story #18.
