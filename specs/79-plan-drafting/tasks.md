# Tasks: First plan is drafted from the quiz

**Input**: Design documents from `specs/79-plan-drafting/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api.md, contracts/prompt.md, quickstart.md

**Tests**: Included. The Definition of Done and constitution V ask for them; each is a behaviour test, not a coverage test. No automated test calls the real service (FR-034).

**Organization**: One phase per GitHub task, because each phase ends in one commit of the story's pull request (`CLAUDE.md`, one pull request per story). Tasks that serve one user story carry its label. Every commit must compile and pass the gate of each side it touches.

## Format: `- [ ] [ID] [P?] [Story?] Description`

- **[P]**: can run in parallel (different files, no dependency on unfinished tasks)
- **[US1]…[US5]**: the user story in spec.md

Paths below use `plan/`, `quiz/` and `client/` for those features under `backend/src/main/java/com/kinetiq/`, or under `backend/src/test/java/com/kinetiq/` for tests.

---

## Phase 1: Add Spring AI for OpenRouter (#80)

**Purpose**: The client exists, and the application starts without a key. Commit: `feat(backend): add spring ai client for openrouter (#80)`, with the Spec Kit files.

- [X] T001 Add `mavenBom("org.springframework.ai:spring-ai-bom:2.0.2")` under `dependencyManagement { imports { … } }` and `implementation("org.springframework.ai:spring-ai-openai")` (the module, not the starter, research R2) in `backend/build.gradle.kts`
- [X] T002 [P] Add under `kinetiq:` in `backend/src/main/resources/application.yaml`: `plan-drafting.api-key: ${OPENROUTER_API_KEY:}` and `plan-drafting.model: ${OPENROUTER_MODEL:openrouter/free}`, with a comment that an empty key means no drafting
- [X] T003 [P] Create `.env.example` at the repository root listing `OPENROUTER_API_KEY=` and `OPENROUTER_MODEL=openrouter/free`, and add one line on them to the Prerequisites list of `README.md`. Never read or print `.env`
- [X] T004 In `plan/infrastructure/config/PlanConfig.java` add a `ChatModel` bean behind `@ConditionalOnExpression("!'${kinetiq.plan-drafting.api-key:}'.isBlank()")`, built with `OpenAiChatModel.builder().options(OpenAiChatOptions.builder()…)`: `baseUrl("https://openrouter.ai/api/v1")`, the key and model from properties, `timeout(Duration.ofSeconds(150))`, `maxRetries(0)`, `maxTokens(16000)`, `temperature(0.2)`, `extraBody` with `provider.require_parameters: true` and `reasoning: { effort: low, exclude: true }`, and `outputSchema` with the schema of contracts/prompt.md (contracts/prompt.md, "Request settings"; options on a prompt replace these defaults, so all of them stay here)
- [X] T005 Set `kinetiq.plan-drafting.api-key=` in the properties of `backend/src/test/java/com/kinetiq/KinetiqBackendApplicationIT.java`, so the smoke test proves the application starts without a key even where the environment has one (FR-006)
- [X] T006 Run `./gradlew check` in `backend/`; commit with `specs/79-plan-drafting/`, the constitution amendment (1.2.0) and its lines in `CLAUDE.md` and `backend/CLAUDE.md`

---

## Phase 2: Plan-generator port and adapters (#81)

**Purpose**: One gateway to the model that turns its answer into domain objects, or a clear failure. Commit: `feat(backend): add plan generator port and adapter (#81)`.

- [X] T007 Add record `GeneratedPlan` with nested `GeneratedSession(name, entries)` and `GeneratedEntry(exerciseKey, sets, amount, loadKg, rpe, restSeconds)` in `plan/domain/model/GeneratedPlan.java`. Numbers are boxed and `loadKg` is a `BigDecimal`, so a missing value stays visible; it validates nothing
- [X] T008 Add `PlanPromptMessages` record (`system`, `user`, `rejections`, a list of sentences for a second attempt) in `plan/domain/model/`, and port `PlanGenerator` with `GeneratedPlan generate(PlanPromptMessages messages)` in `plan/domain/output/PlanGenerator.java`, documented to throw the two exceptions of T009
- [X] T009 Add `PlanGenerationFailedException` (carries a reason that holds no text from the service) and `PlanGeneratorUnavailableException` in `plan/domain/exception/`. Neither reaches an API response
- [X] T010 [US2] Add `OpenRouterPlanGenerator` in `plan/infrastructure/output/`, behind the same condition as T004, taking `ChatModel`. It sends the system message, the user message and, when `rejections` is not empty, one more user message as in contracts/prompt.md "Second attempt". It maps to `GeneratedPlan` with Jackson, removing a code fence first. Empty content, finish reason `length`, a shape that does not parse and any exception from the client become `PlanGenerationFailedException`. It logs model and duration, the exception's class name and never its message, the prompt or the answer
- [X] T011 [US2] Add `UnavailablePlanGenerator` in `plan/infrastructure/output/`, behind the opposite condition; it logs one warning at start and throws `PlanGeneratorUnavailableException` from `generate` (FR-006)
- [X] T012 [US2] Write `plan/infrastructure/output/OpenRouterPlanGeneratorTest.java` with a hand-written `ChatModel` fake: a valid answer maps to domain objects, including a null load; a fenced answer is accepted; empty content, finish reason `length`, text that is not JSON, valid JSON of another shape and a thrown exception each give `PlanGenerationFailedException`; `rejections` arrive as a final user message; with Boot's `OutputCaptureExtension`, no test string from the prompt or the answer, and no message of a thrown exception, reaches the log (FR-018, User Story 5 scenario 4)
- [X] T013 [US2] Extract the options of T004 into a package-private method taking base URL, key, model and timeout, in `plan/infrastructure/config/PlanConfig.java`. Write `plan/infrastructure/config/PlanChatModelTest.java` against a JDK `HttpServer` stub: a slow response fails at about a 1 second timeout (FR-019); a 429 makes exactly 1 request; the request body holds `provider`, `reasoning` and the JSON schema
- [X] T014 Run `./gradlew check`; commit

---

## Phase 3: Prompt from quiz answers and library (#82)

**Purpose**: What leaves the system, proven free of personal data. Commit: `feat(backend): build plan prompt from quiz answers and library (#82)`.

- [X] T015 Add `Optional<QuizSubmission> findByClient(AccountId)` to `quiz/domain/output/QuizSubmissionRepository.java`; implement it in `quiz/infrastructure/output/QuizSubmissionRepositoryImpl.java` with `toDomain` in `QuizSubmissionMapper.java` (written by hand, as `toEntity` is; fix its comment that nothing reads a submission back), and in the fake `quiz/domain/service/InMemoryQuizSubmissionRepository.java` under test sources
- [X] T016 Add port `ViewQuizAnswersUseCase` (`Optional<QuizSubmission> answers(AccountId client)`) in `quiz/domain/input/`, implemented by `QuizSubmissionService`, wired in `quiz/infrastructure/config/QuizConfig.java`; extend `quiz/infrastructure/output/QuizSubmissionRepositoryImplIT.java` with a round trip keeping likes, weight scale and empty notes
- [X] T017 Add record `DraftInput` in `plan/domain/input/`: `QuizSubmission answers`, `AccountId trainer`, `String clientName`, `String trainerName`, `Language language`, `List<Exercise> library`
- [X] T018 [US5] Add `PersonalDataCleaner` in `plan/domain/service/`: each word of the given names as a whole word, ignoring case and Polish diacritics, with its Polish inflected forms when its stem has at least 3 letters (the stem drops a final vowel and a fleeting `e` in the last syllable, then allows up to 3 more letters, so `Anna` catches `Anny` and `Marek` catches `Marka`), and anything shaped like an email address or a phone number (7 or more digits with optional `+`, spaces, dashes or brackets), becomes `[removed]`. Ordinary numbers such as `2019`, `120 kg` and `3x10` stay. Mark third-person names and irregular forms as a known limit with a `ponytail:` comment
- [X] T019 [US1] Add the days table of FR-013 as `SessionDays` in `plan/domain/service/` and a test that each of 2–6 sessions gives distinct days, Monday first
- [X] T020 [US1] Add `PlanPrompt` in `plan/domain/service/`, built from `DraftInput`, the rejections and the session days of FR-013 (see T019, `SessionDays`). It produces `PlanPromptMessages` holding the system and user messages of contracts/prompt.md word for word, and a map from key to `ExerciseId`. The catalogue is sorted by category, then name, keyed `E1`, `E2`, …, with English names; cues and ids stay out. Own exercises' names and equipment go through the cleaner, starter exercises do not. Notes go through the cleaner, with `<<<` and `>>>` removed; empty notes read `none`
- [X] T021 [US5] Write `plan/domain/service/PersonalDataCleanerTest.java`: names in any case and without diacritics, inflected forms (`Anny`, `Annę`, `Marka`, `Pawła`), a name inside a longer word stays, a short name matches only exactly, emails, phone formats, numbers that stay
- [X] T022 [US5] Write `plan/domain/service/PlanPromptTest.java`: with distinctive sentinel strings for client name, email, trainer name, phone and a person's id, also written into the notes and into an own exercise's name and equipment, none reaches either message, and neither do cues or any UUID (the "Done when" of #82, SC-004). Also: a fence in the notes cannot close, 240 exercises get keys and all keys map back, session days and language appear, both languages
- [X] T023 Run `./gradlew check`; commit

---

## Phase 4: Draft validation, retry and failure state (#83)

**Purpose**: Nothing unchecked is stored, and failure is a normal end. Commit: `feat: validate drafted plans, retry and store a failed draft (#83)`.

### Storage and model

- [ ] T024 Write `backend/src/main/resources/db/migration/V16__add_plan_drafting.sql`: `alter table plans add column automatic_draft varchar(16)` with `ck_plans_automatic_draft` (`BUILT`, `FAILED` or null) and `ck_plans_automatic_draft_unpublished` (`first_week is null or automatic_draft is null`); `create table plan_draftings (client_id uuid not null, started_at timestamptz not null)` with `pk_plan_draftings` and `fk_plan_draftings_client` to `accounts`. Before opening the pull request, check `gh pr list --json number,files` for the number
- [ ] T025 Add enum `AutomaticDraft` (`BUILT`, `FAILED`) in `plan/domain/model/`. In `Plan.java` add the optional field, `Plan.built(client, goal, weeks, sessions)` (calls `requireComplete()`) and `Plan.failed(client, goal, weeks)`; `edit` and `publish` return a plan without a mark. Update `plan/infrastructure/output/PlanEntity.java`, `PlanMapper.java` and `copyValues` in `PlanRepositoryImpl.java`, which copies each value by hand, so without it a save would not clear the mark. Leave `clientSeenAt` alone (spec 75). Update every `new Plan(` in tests
- [ ] T026 [US4] Extend `plan/domain/model/PlanTest.java` (marks are set and cleared by `edit` and `publish`, an incomplete `built` is refused) , `plan/infrastructure/output/PlanRepositoryImplIT.java` (the mark round-trips, and saving an edited plan clears it in storage) and `plan/domain/service/PlanViewServiceTest.java` (a `BUILT` and a `FAILED` draft answer `plan.not-found` to their client, FR-029)

### Checks and attempts

- [ ] T027 [US2] Add `GeneratedPlanRejectedException` carrying its codes in `plan/domain/exception/`, and `GeneratedPlanChecks` in `plan/domain/service/`: from a `GeneratedPlan`, the key map, `DraftInput` and the session days it returns `List<Session>` or throws with every applicable code of data-model.md "Rejection codes". Limits, quoted from FR-017: "exactly as many sessions as the client's days per week"; "4–10 entries per session, with no exercise twice in one session"; "session names unique within the plan, at most 40 characters after trimming, holding no link or email address"; "1–8 sets"; "1–50 repetitions, or 5–3600 seconds, as the exercise measures"; "RPE of 5–9, or 5–8 for a client aged 16–17 or 60 and over"; "rest of 0–300 seconds"; "a load that is empty, or above 0 and at most twice the client's body weight". It builds `ExerciseEntry` and `Session`, so plan rules apply too; a `PlanRuleViolationException` becomes `draft.plan-rule`
- [ ] T028 [US2] Write `plan/domain/service/GeneratedPlanChecksTest.java`: one test per code at its boundaries, `draft.rpe` at ages 17, 18, 59 and 60, a valid plan with a loaded exercise measured in seconds and null loads, sessions on the days of FR-013 in the answer's order, `{"sessions": []}` and a session of one fragment are rejected
- [ ] T029 [US2] Add the sentence per rejection code to `PlanPrompt` (contracts/prompt.md, "Second attempt"), holding no client text
- [ ] T030 Add port `DraftPlanUseCase` in `plan/domain/input/` with `Optional<DraftInput> input(AccountId client)`, `void storeBuilt(AccountId client, AccountId trainer, List<Session> sessions)` and `void storeFailed(AccountId client)`; implement in `PlanDraftingService` in `plan/domain/service/`, using `ViewQuizAnswersUseCase`, `CurrentAccountUseCase`, `ListExercisesUseCase` and `PlanRepository`. `input` is empty when the answers or the client's trainer are gone. Both stores use the quiz's goal and 12 weeks and store nothing when the client already has a plan (FR-022), or when the answers or trainer are gone. `storeBuilt` stores nothing when the client's trainer is no longer `trainer`, checks that trainer's library again in its transaction, and throws `GeneratedPlanRejectedException` with `draft.unknown-exercise`
- [ ] T031 Add port `PlanDrafting` (`void run(AccountId client)`) in `plan/domain/input/`, documented as not a `*UseCase` so it holds no transaction (research R4), and `PlanDraftingJob` in `plan/domain/service/`, constructed with `DraftPlanUseCase`, `PlanGenerator` and a pause `Duration`: an empty input goes straight to `storeFailed`, which stores nothing in that case; at most 2 attempts; a rejection feeds its codes to the second attempt; any other failure repeats the same prompt; `PlanGeneratorUnavailableException` goes straight to `storeFailed` without a pause; after 2 failed attempts `storeFailed`; a final catch of `RuntimeException` so a job never dies silently. Logs outcome, duration and codes, and never who it was for
- [ ] T032 [US2] Write `plan/domain/service/PlanDraftingJobTest.java` with hand-written fakes and a zero pause: valid first; rejected then valid, with the second prompt naming the reasons; two rejections give an empty failed draft; timeout then valid; two failures; no generator stores at once with no attempt; a plan already stored is left untouched and nothing is added; never more than 2 calls; with `OutputCaptureExtension`, no client text or id reaches the log (FR-018)
- [ ] T033 [US2] Write `plan/domain/service/PlanDraftingServiceTest.java`: goal, 12 weeks and sessions of a stored draft; the mark on each; an exercise gone from the library is refused; no answers or trainer gives an empty input, and `storeFailed` then stores nothing and does not throw; a changed trainer stores nothing. Add a fake `plan/domain/service/StubPlanGenerator.java` in test sources
- [ ] T034 Wire `DraftPlanUseCase` and `PlanDrafting` in `plan/infrastructure/config/PlanConfig.java` with a 5 second pause; nothing calls them until #84

### Marks on the screen

- [ ] T035 [US4] In `plan/infrastructure/input/PlanResponse.java` add optional `automaticDraft`, mapped in `PlanWebMapper.java`; extend `plan/infrastructure/input/PlanControllerTest.java`. Update `plan/domain/service/PlanManagementServiceTest.java` so a save clears the mark
- [ ] T036 [US4] In `client/domain/model/PlanState.java` add `DRAFT_FAILED`. In `client/infrastructure/output/JdbcClientDirectory.java` select `p.automatic_draft` and map by data-model.md: `FAILED` gives `DRAFT_FAILED`, `BUILT` gives `PLAN_TO_REVIEW`, otherwise `DRAFT`; a client with answers and no plan stays `PLAN_TO_REVIEW`. Extend `client/infrastructure/output/JdbcClientDirectoryIT.java`
- [ ] T037 Start the backend and run `npm run gen:api` in `frontend/`, committing `frontend/src/lib/api/schema.d.ts`
- [ ] T038 [US4] Add `DRAFT_FAILED: 'warning'` to `frontend/src/features/client/components/plan-state-badge.tsx`, and `clients.planStates.DRAFT_FAILED` to `frontend/src/locales/en.json` and `pl.json` (spec, Texts)
- [ ] T039 [US4] Add `frontend/src/features/plan/components/automatic-draft-note.tsx`, an `Alert` with `variant="info"`, an icon and the text of spec Texts for `BUILT` and `FAILED`, naming the client. Show it above the sessions in `frontend/src/features/plan/components/plan-editor.tsx` while `saved.plan?.automaticDraft` is set. Add `plan.note.built` and `plan.note.failed` to both locales
- [ ] T040 [US4] Extend `frontend/src/features/plan/components/plan-editor.test.tsx`: each note shows; a successful save removes it; a failed draft shows "Add session" and publishing it lists what is missing. Extend `frontend/src/features/client/pages/clients-page.test.tsx` for the new badge
- [ ] T041 Add `demo.client.built@example.com` with a `BUILT` three-session draft and `demo.client.failed@example.com` with an empty `FAILED` draft, both with answers, in `backend/src/main/resources/db/dev-data/R__demo_accounts.sql`, `R__demo_quiz.sql` and `R__demo_plans.sql`, taking the next free ids in each (story #75 used accounts up to `…8000-000000000009`, answers up to `…000000000305`, plans up to `…9300-000000000004`, sessions up to `…9400-000000000007`). Add `automatic_draft` to the `plans` insert's column list, which already holds `client_seen_at`. Add both accounts to the demo table in `README.md` (FR-033)
- [ ] T042 [P] Add "Failed draft" to `CONTEXT.md`, under Planning: "A draft the system could not build automatically: empty, and marked so for the trainer."
- [ ] T043 Run `./gradlew check` and `npm run check`; commit

---

## Phase 5: Draft in the background on quiz submit (#84)

**Purpose**: A submit starts drafting, both people see it happen, and nothing is left hanging. Commit: `feat: draft the first plan in the background on quiz submit (#84)`.

### Starting and ending

- [ ] T044 Add record `QuizSubmitted(AccountId client)` in `quiz/domain/model/`, port `QuizEventPublisher` in `quiz/domain/output/`, and `SpringQuizEventPublisher` in `quiz/infrastructure/output/` using `ApplicationEventPublisher`. `QuizSubmissionService.submit` publishes after saving; wire it in `QuizConfig.java`, with a recording fake in `quiz/domain/service/QuizSubmissionServiceTest.java`, proving a refused second submit publishes nothing
- [ ] T045 Add port `PlanDraftingRepository` in `plan/domain/output/` (`start(client, at)`, `boolean remove(client)` reporting whether a row was there, `List<AccountId> startedBefore(instant)`), `PlanDraftingRepositoryImpl` with `JdbcTemplate` in `plan/infrastructure/output/`, an `IT` next to it, and a fake in test sources
- [ ] T046 [US1] Extend `DraftPlanUseCase` and `PlanDraftingService`: `request(client)` writes the row; `storeBuilt` and `storeFailed` begin with `remove` and store nothing when no row was there, so the job and the sweep cannot both store and a late result is dropped (FR-005). Add `List<AccountId> overdue()`: rows older than 5 minutes 30 seconds by the `Clock`. Extend `PlanDraftingServiceTest.java`, with a fixed clock at both sides of the limit, and with a plan already there the row goes and nothing is stored
- [ ] T047 [US1] Add `QuizSubmittedListener` in `plan/infrastructure/input/`: `@EventListener` calls `DraftPlanUseCase.request` inside the submit's transaction; `@TransactionalEventListener(phase = AFTER_COMMIT)` hands `PlanDrafting.run` to a `newVirtualThreadPerTaskExecutor`, closed on shutdown (FR-001, FR-002)
- [ ] T048 [US2] Add `PlanDraftingSweep` in `plan/infrastructure/input/` with `@Scheduled(fixedDelay = 30, timeUnit = SECONDS)` calling `overdue` and then `storeFailed` for each row in its own transaction, logging a failure by class name and carrying on with the next, and `@EnableScheduling` on `PlanConfig` (FR-005). Test in `plan/infrastructure/input/PlanDraftingSweepTest.java` that one failing row does not stop the others
- [ ] T049 [US1] Extend `backend/src/test/java/com/kinetiq/KinetiqBackendApplicationIT.java` with `@MockitoBean PlanGenerator`: a real submit answers `DRAFTING_PLAN` while the generator blocks, then the plan is stored and the status is `WAITING_FOR_PLAN`; a failing submit starts nothing. Use a latch and Awaitility or a bounded loop, not a sleep

### Statuses

- [ ] T050 [US3] Add `DRAFTING_PLAN` to `quiz/domain/model/QuizStatus.java`, and `isPlanDrafting` to `QuizSubmissionRepository` with a native exists query on `plan_draftings` in `JpaQuizSubmissionRepository.java`, as `hasPublishedPlan` does. `status` follows data-model.md's order, and `submit` returns `DRAFTING_PLAN`. Update `QuizSubmissionServiceTest.java`, `QuizSubmissionRepositoryImplIT.java` and `quiz/infrastructure/input/QuizControllerTest.java`
- [ ] T051 [US4] Add `PLAN_DRAFTING` to `PlanState.java`; left-join `plan_draftings` in `JdbcClientDirectory.java`, the row winning over every other state. Extend `JdbcClientDirectoryIT.java`
- [ ] T052 [US4] Add `PlanDraftingInProgressException` (code `plan.drafting`, kind `CONFLICT`) in `plan/domain/exception/`; `PlanManagementService.firstDraft` throws it when the state is `PLAN_DRAFTING`. Extend `PlanManagementServiceTest.java` and `PlanControllerTest.java` for `409` `plan.drafting` with nothing stored, and for a foreign client still answering `404` during drafting (FR-029)
- [ ] T053 Start the backend and run `npm run gen:api`, committing `schema.d.ts`

### Client screens

- [ ] T054 [US3] In `frontend/src/features/quiz/components/quiz-status-check.tsx` accept several statuses for one page, and add `DRAFTING_PLAN: '/app/quiz/waiting'` to `PAGE_FOR`
- [ ] T055 [US3] Rework `frontend/src/features/quiz/pages/waiting-page.tsx`: both statuses render one page. While `DRAFTING_PLAN` it polls with `refetchInterval` of 5 seconds, stopping on any other status, and shows the title and text of spec Texts, a `Spinner` and an `aria-hidden` `Skeleton` card as on the building board. The heading takes focus only on first mount; the change of state is announced through a `role="status"` region and moves no focus. `settings` and `message` stay in both states and are not unmounted. Motion respects reduced motion
- [ ] T056 [US3] Replace `quiz.waiting.building.*` in `frontend/src/locales/en.json` and `pl.json` with spec Texts, dropping the count of answers and days
- [ ] T057 [US3] Extend `frontend/src/features/quiz/pages/waiting-page.test.tsx`: building state shows with a skeleton; it moves to reviewing by itself, with focus unchanged and the change announced; a failed check keeps the state and checking goes on; Polish; a published status leaves
- [ ] T058 [US5] Show the notice of spec Texts above the submit button in `frontend/src/features/quiz/pages/review-page.tsx`, and test it in `review-page.test.tsx`
- [ ] T059 [US5] Change `account.join.consent` in both locales to the sentence of spec Texts; update `frontend/src/features/account/pages/join-page.test.tsx` if it quotes the old text (FR-028)

### Trainer screens

- [ ] T060 [US4] In `frontend/src/features/plan/pages/plan-page.tsx` treat `client.planState === 'PLAN_DRAFTING'` before everything else: `usePlan` stays off, and a new `DraftingState` shows the line of spec Texts with `role="status"` and `PlanSkeleton`, with no "Build plan". While mounted it invalidates `['clients', client.id]` every 5 seconds unless `document.hidden`, so the tab and its badge move on by themselves (research R8)
- [ ] T061 [US4] Add `PLAN_DRAFTING: 'outline'` to `plan-state-badge.tsx`, `clients.planStates.PLAN_DRAFTING` and `errors.plan.drafting` to both locales
- [ ] T062 [US4] Extend `frontend/src/features/plan/pages/plan-page.test.tsx`: drafting shows the line and no "Build plan"; it moves to the editor when the state changes; `plan.drafting` from a save shows its message and keeps the edits
- [ ] T063 Run `./gradlew check` and `npm run check`; commit

---

## Phase 6: Polish

**Purpose**: Everything `CLAUDE.md` asks for before the pull request. Fixes join the commit of the task they belong to, or a commit of their own when they cross tasks.

- [ ] T064 [US1] Run quickstart scenarios 1–5 end to end in the running app, with the real service where the scenario says so, and search the backend log for the client's name and notes; fix what they find
- [ ] T065 [US1] Run the evaluation matrix of contracts/prompt.md against the real service, all 8 cases with cases 2 and 3 repeated, and report each draft's split and exercises against SC-005; refine the prompt and the contract together if fewer than 8 of the 10 runs pass
- [ ] T066 Check every changed screen in the built-in browser: waiting (building and reviewing), review notice, Plan tab states, both notes and badges, each at 375 px and 1280 px; light and dark; English and Polish; keyboard focus; console errors. Take screenshots where they help
- [ ] T067 Review the changed screens with the ui-ux-pro-max skill for WCAG 2.2 AA and UX issues, keeping the design system authoritative; fix what it finds
- [ ] T068 Run the `security-review` skill over the branch, checking personal data in prompts and logs, service text in responses, and access to drafts
- [ ] T069 Check `gh pr list --json number,files` for migration numbers and renumber V16 if needed; run `./gradlew check` and `npm run check` a last time and report the real result
- [ ] T070 Open one pull request with `Closes #79`, `Closes #80`, `Closes #81`, `Closes #82`, `Closes #83` and `Closes #84`, stating that it should be merged with rebase or a merge commit, and listing the spec's departures from #79 and #81: no fixed-template generator, and "Try again" left to #88

---

## Dependencies & Execution Order

Phases run in order, because each ends in a commit that must pass its gates: #80, #81, #82, #83, #84, Polish.

- #81 needs the client of #80.
- #82 needs `PlanPromptMessages` of #81.
- #83 needs the prompt of #82 and adds the migration.
- #84 needs the job of #83.

Within a phase, tasks marked [P] touch different files. Within #84, backend tasks T044–T052 come before T053, and T053 before any frontend task. In #83, T036 comes before T037, and T037 before T038–T040.

Parallel examples: T002 and T003; T042 with any other task of #83; once T053 is done, T054–T059 (client screens) and T060–T062 (trainer screens) touch different files, apart from the locales.

## Implementation Strategy

Phases 1 to 5 are one story and ship together: a generated draft is only useful once a submit starts it. Within them, #83 proves that bad answers are stopped before anything can start a job, and #84 is what users see. Each commit must pass the gate of every side it touches.

## Independent tests by user story

| Story | Proven by |
|---|---|
| US1 | T049: a submit answers at once with a blocked generator, then a draft exists and the client still waits |
| US2 | T032: a stand-in giving bad answers leads to two attempts, the reasons named, and an empty failed draft |
| US3 | T057 and T066: building moves to reviewing by itself |
| US4 | T040, T062 and T066: three messages and marks that go on saving |
| US5 | T022: sentinel strings never reach the prompt |

## Requirement coverage

| Requirement | Tasks |
|---|---|
| FR-001–004 | T044, T046, T047, T049, T050 |
| FR-005 | T046, T048 |
| FR-006 | T004, T005, T011, T032 |
| FR-007 | T002, T003 |
| FR-008, FR-008a, FR-009–012 | T018–T022 |
| FR-013, FR-014 | T020, T027, T019 |
| FR-015–018 | T027, T028, T030 |
| FR-019–022 | T004, T013, T029, T030–T033 |
| FR-023 | T025, T026, T035 |
| FR-024 | T036, T038, T051, T061 |
| FR-025 | T052, T060, T062 |
| FR-026 | T039, T040 |
| FR-027 | T054–T057 |
| FR-028 | T058, T059 |
| FR-029 | T026, T052, T068 |
| FR-030 | T010, T039, T068 |
| FR-031, FR-032 | T038, T039, T056–T061, T066, T067 |
| FR-033 | T041 |
| FR-034 | T012, T049, T065 |

