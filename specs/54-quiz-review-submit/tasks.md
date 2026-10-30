---
description: "Tasks for story #54, Client reviews and submits the quiz"
---

# Tasks: Client reviews and submits the quiz

**Input**: `specs/54-quiz-review-submit/` (spec.md, plan.md, research.md, data-model.md, contracts/api.md, quickstart.md), building on `specs/50-client-quiz/`

**Tests**: Required by the Definition of Done (`CLAUDE.md`) and principle V. Tests sit in the same phase as the code they cover.

**Organization**: Phases follow the GitHub tasks, not the spec's user stories, because each task is its own pull request, squash merged (`CLAUDE.md`, Workflow). #56 builds the review (US1), #55 the stored answers and status (US2), #57 the submit action and the waiting states (US2, US3). Story labels show which spec story a task serves.

**Paths**: `F` = `frontend/src`, `Q` = `frontend/src/features/quiz`, `B` = `backend/src/main/java/com/kinetiq`, `BT` = `backend/src/test/java/com/kinetiq`, `R` = `backend/src/main/resources`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: spec user story (US1 review the answers, US2 submit once, US3 wait for the plan)

---

## Phase 1: Setup

- [X] T001 Commit `specs/54-quiz-review-submit/` (spec, checklist, plan, research, data model, contract, quickstart, tasks) with the code of task #56, so the spec is reviewed in that pull request. `CONTEXT.md` needs no new term: the spec uses "Quiz" and the words of spec 50

---

## Phase 2: task #56, quiz review step (US1)

**Goal**: `/app/quiz/review` lists the seven answers from the draft, each with a Change link that returns to the review, as on the board "Quiz · review"; "Build my plan" is shown disabled until task #57.

- [X] T002 [P] [US1] Add `isComplete(draft)` to `Q/quiz-schema.ts`: true when `reached` is 7 and `isStepValid` holds for steps 1–6 (research R2)
- [X] T003 [P] [US1] Extend `Q/quiz-schema.test.ts`: `isComplete` is true for a full valid draft with `reached` 7, false with `reached` 6, and false when one earlier step is invalid
- [X] T004 [P] [US1] Add `quiz.review.*` to `F/locales/en.json` and `F/locales/pl.json`, same keys in both, English from the board, Polish natural and informal: `label` ("Review"), `title` ("Check your answers"), `hint` ("Your first plan is built from these 7 answers. Your trainer checks it before you see it."), `rows.<goal|experience|level|about|likes|days|notes>.label` ("Goal", "Training for", "Level", "About you", "Likes", "Days a week", "Notes") and `.change` (accessible names "Change goal", "Change training for", …), `change` ("Change"), `about` ("{{count, number}} years · {{height, number}} cm · {{weight, number}} kg", `count` = age, with the plural forms of "years" in both languages: English one and other, Polish one, few, many and other, so "22 lata" and "29 lat"), `noNotes` ("No notes"), `build` ("Build my plan"), `later` ("You can change them later with your trainer.")
- [X] T005 [US1] Create `Q/pages/review-page.tsx` (research R1, R4, R5): read `loadDraft()` once; when `isComplete` is false, `<Navigate replace>` to `/app/quiz/<allowedStep(undefined, draft)>`. Layout as the step page (column up to 640 px, no tab bar): header with "Review" and a ghost Back button (48 px), a progress bar of seven parts all `bg-primary` (`aria-hidden`), `h1` with `tabIndex={-1}` focused on mount, the hint, a `dl` with one row per step in step order (`dt` label; `dd` value and a `Link` "Change" to `/app/quiz/<n>` with `state: { fromReview: true }` and the row's accessible name), and at the bottom a disabled `Button` `size="touch"` "Build my plan" with `bg-brand text-brand-foreground` and the line `later` under it. Values: option titles from `quiz.<step>.options.<CODE>.title`; "About you" through `t('quiz.review.about', …)` with age (`count`) and height from `parseWholeNumber` and weight from `parseWeight`; likes' titles in screen order joined with a comma (research R4); days as the number; "About you" and days with the `num` class; trimmed notes or `noNotes`. Back calls `navigate(-1)` when `location.state?.fromStep === 7`, else replaces with `/app/quiz/7` (research R3)
- [X] T006 [US1] Add a lazy route `/app/quiz/review` loading `ReviewPage` (`HydrateFallback: BlankWhileLoading`) in `F/app/router.ts`, beside `/app/quiz/:step?`; in `F/app/router.test.tsx` add a case: with a complete draft in `sessionStorage`, `/app/quiz/review` shows the heading "Check your answers" (without the static route the address would show step 7)
- [X] T007 [US1] In `Q/pages/quiz-page.tsx`: when `location.state?.fromReview` is true, Continue (still disabled while the step is invalid) and Back both call `navigate(-1)`, and Back is shown on step 1 too; remove the comment in `goNext` saying the review comes with task #56 (research R3)
- [X] T008 [US1] Write `Q/pages/review-page.test.tsx` (render a `createMemoryRouter` with the routes `/app/quiz/review` and `/app/quiz/:step?`, `sessionStorage` set per test): seven rows in step order with their labels and values; "About you" reads "29 years · 168 cm · 62.5 kg" in English and "29 lat · 168 cm · 62,5 kg" in Polish, and "22 lata" for age 22 in Polish; likes read "Weights, Yoga and Pilates" for those codes; empty or spaces-only notes read "No notes"; each "Change" link is named for its row and opens its step; Back opens step 7; a draft with `reached` 6 or an invalid step 4 redirects to the step that may open; "Build my plan" is disabled; the heading has focus
- [X] T009 [US1] Extend `Q/pages/quiz-page.test.tsx`: from the review, "Change" on Level opens step 3; choosing another level and Continue shows the review with the new level; Back from a step opened with "Change" returns to the review; on step 1 opened with "Change" Back is shown; Continue stays disabled while the opened step is invalid
- [X] T010 [P] [US1] Update spec 50 now that the review exists: in `specs/50-client-quiz/research.md` R12, say that task #56 added `/app/quiz/review`; in `specs/50-client-quiz/contracts/routes.md`, replace the fallback line for `/app/quiz/<anything else>` with the review address
- [X] T011 [US1] Verify in the browser per `quickstart.md`, "Browser check, task #56", steps 1–10, at 375 px, light and dark, English and Polish, keyboard only, console; take screenshots
- [X] T012 Run `npm run check` in `frontend/`; open the pull request for #56 after the user agrees (title `feat(frontend): add quiz review step`, `Closes #56`)

---

## Phase 3: task #55, quiz answers model and submit API (US2)

**Blocked by**: task #31 and the sign-in story #18 (session principal, role checks, migration `V5`, demo client account). Only #31 is linked as a blocker on GitHub; story #18 is recorded here. T013–T024 were refined to their merged code when the task started (research R8, data-model.md, contracts/api.md).

**Goal**: `GET` and `POST /api/v1/quiz` as in contracts/api.md: one stored submission per client with every rule of spec 50, refused by role for other accounts.

- [X] T013 [P] [US2] Create enums `Goal`, `Experience`, `Level`, `Like` (codes of spec 50, data-model "Option codes") and `QuizStatus` (`NOT_SUBMITTED`, `WAITING_FOR_PLAN`, `PLAN_PUBLISHED`), and `QuizSubmissionId` (record wrapping `UUID`, `random()`) in `B/quiz/domain/model/`
- [X] T014 [US2] Create `QuizSubmission` in `B/quiz/domain/model/QuizSubmission.java` (`@Getter`, no setters, a constructor that checks every rule, as `B/exercise/domain/model/Exercise.java` does): fields and rules verbatim from data-model.md "Quiz submission (domain)": age "16–99", heightCm "120–230", weightKg "30.0–250.0, at most one decimal", likes "1–5 codes; a set, so each code appears once", daysPerWeek "2–6", notes "trimmed; absent when empty after trimming; at most 500 UTF-16 units"; each broken rule throws `InvalidValueException`
- [X] T015 [US2] Write `BT/quiz/domain/model/QuizSubmissionTest.java` (`@Nested` per rule): each bound refused just outside and accepted at the bound; a weight with two decimals refused; notes trimmed, spaces-only notes absent, 500 units accepted and 501 refused; an empty likes set refused
- [X] T016 [US2] Create `QuizAlreadySubmittedException` (`DomainException`, kind `CONFLICT`, code `quiz.already-submitted`, research R8) in `B/quiz/domain/exception/`; port `SubmitQuizUseCase` with `submit(SubmitQuizCommand)` and `status(AccountId)` in `B/quiz/domain/input/` (one port, as `backend/CLAUDE.md` lets a port group closely related operations); `QuizSubmissionRepository` (`save`, `existsByClient`; nothing reads a stored submission before the trainer's view) in `B/quiz/domain/output/`; `QuizSubmissionService` in `B/quiz/domain/service/` with an injected `Clock`: refuses a client who already has a submission, stores the new one with `submittedAt`, and answers `NOT_SUBMITTED` or `WAITING_FOR_PLAN` (never `PLAN_PUBLISHED` before the plan stories)
- [X] T017 [US2] Write `BT/quiz/domain/service/QuizSubmissionServiceTest.java` with a hand-written fake repository and a fixed `Clock`: first submit stored with the time; second refused with `quiz.already-submitted` and the stored one unchanged; status for no submission and for one
- [X] T018 [US2] Write `R/db/migration/V12__create_quiz_submissions.sql` (`V11` is the highest on `master`; check again before opening the pull request and renumber if taken, plan "Open items"), exactly as data-model.md tables `quiz_submissions` and `quiz_submission_likes`, including `uq_quiz_submissions_client` and the named check constraints
- [X] T019 [US2] Create in `B/quiz/infrastructure/output/`: `QuizSubmissionEntity` (extends `BaseEntity`, likes as `@ElementCollection` on `quiz_submission_likes`, enums as strings), `JpaQuizSubmissionRepository`, `QuizSubmissionMapper` (domain to entity) and `QuizSubmissionRepositoryImpl`, which flushes on save and turns a violation of `uq_quiz_submissions_client` into `QuizAlreadySubmittedException`, as `B/exercise/infrastructure/output/ExerciseRepositoryImpl.java` does for its name index (research R9)
- [X] T020 [US2] Write `BT/quiz/infrastructure/output/QuizSubmissionRepositoryImplIT.java` (`@DataJpaTest`, Testcontainers): the stored row with and without notes; `existsByClient` for a client with and without a submission; a second submission for the same client is refused as `quiz.already-submitted`; the database refuses an out-of-range age and an unknown like
- [X] T021 [US2] Create in `B/quiz/infrastructure/input/`: `SubmitQuizRequest` (Bean Validation per contracts/api.md: enums `@NotNull`, `@Min`/`@Max`, `@NotNull` `@DecimalMin`/`@DecimalMax` and `@Digits(integer = 3, fraction = 1)` on weight, `@NotEmpty` likes as a set (a set of five codes cannot hold more than five), `@Size(max = 500)` notes), `QuizStatusResponse`, `QuizWebMapper` and `QuizController` (`GET` and `POST /api/v1/quiz`, `201` on submit, client from the session principal as `ExerciseController` does); `B/quiz/infrastructure/config/QuizConfig` (`Clock.systemUTC()`, as `AccountConfig`); in `B/common/infrastructure/config/SecurityConfig.java` require `CLIENT` for `/api/v1/quiz/**` (research R7)
- [X] T022 [US2] Write `BT/quiz/infrastructure/input/QuizControllerTest.java` (`@WebMvcTest`, use case mocked): `201` with `WAITING_FOR_PLAN`; one `validation.failed` case per field; an unknown code `request.invalid`; `409` `quiz.already-submitted`; `401` without a session; `403` `request.not-allowed` for a trainer; extend `BT/KinetiqBackendApplicationIT.java`: `/api/v1/quiz` without a session is `401`
- [X] T023 [P] [US2] Add a second demo client `demo.client.waiting@example.com` ("Demo Client Waiting", client of the demo trainer, email confirmed, with consent) to `R/db/dev-data/R__demo_accounts.sql` and to the demo accounts table in `README.md`; add `R/db/dev-data/R__demo_quiz.sql`: one submission for that client, idempotent; `demo.client@example.com` keeps no submission (data-model.md, "Demo data")
- [ ] T024 Start the backend, run `npm run gen:api` in `frontend/` and check `F/lib/api/schema.d.ts` gained only `/api/v1/quiz`; run `./gradlew check` in `backend/` and `npm run check` in `frontend/`; open the pull request for #55 after the user agrees (title `feat(backend): add quiz answers model and submit api`, `Closes #55`)

---

## Phase 4: task #57, submit and waiting states (US2, US3)

**Blocked by**: task #56, task #55 and task #217, which marks the status required in the contract (all linked on GitHub). T025–T031 were refined to their merged code when the task started; the quiz pages follow the status here, the client area in task #53 (research R12, plan "Open items").

**Goal**: "Build my plan" submits once; the building and reviewing states show and persist until a plan is published.

- [X] T025 [P] [US2] [US3] Create `Q/api/quiz.ts`: `quizStatusQuery` (`queryOptions` over `GET /api/v1/quiz`, `retry: false` as `currentAccountQuery`, since the pages wait on it; default refetch on window focus, no interval, research R10) with `useQuizStatus()`, and `useSubmitQuiz()` (mutation over `POST /api/v1/quiz`; the body from the draft as in contracts/api.md: the codes, `age` and `heightCm` with `parseWholeNumber`, `weightKg` as `toKg(parseWeight(answers.weight), unit)` with `unit` from `useWeightUnit()`, since `loadDraft(unit)` gives the weight in the person's unit, `likes`, `daysPerWeek`, `notes` as typed; on success the returned status goes into the status query with `setQueryData`, so the waiting page does not read a stale `NOT_SUBMITTED`), throwing the problem detail on error as `F/features/exercise/api/exercises.ts` does
- [X] T026 [P] [US2] Add `clearDraft()` to `Q/quiz-draft.ts` (research R11)
- [X] T027 [P] [US3] Add `quiz.waiting.*` to `F/locales/en.json` and `F/locales/pl.json`: building title ("Building your plan from 7 answers…"), building text ("Sessions, exercises and starting weights for {{count}} days a week.", plural forms in Polish), reviewing title ("Your trainer is reviewing your plan") and text ("Your trainer checks it before you see it. You get a notice here when it's ready."), and the error's "Try again"
- [X] T028 [US2] [US3] In `Q/pages/review-page.tsx`, give "Build my plan" its action: while the mutation is pending, show the building state (its title, its text with the draft's days, and a skeleton card of the plan summary, the design system's `Card` loading state; design system 6.1 names the work being done); on success `clearDraft()` and replace the address with `/app/quiz/waiting`; on `quiz.already-submitted` `clearDraft()`, invalidate the status query and go to the waiting state; on other failures keep the review with `t(errorMessageKey(error))` and "Try again" (spec FR-013)
- [X] T029 [US3] Create `Q/components/quiz-status-check.tsx` (research R12) and use it in `Q/pages/quiz-page.tsx`, `Q/pages/review-page.tsx` and a new `Q/pages/waiting-page.tsx`, with its lazy route `/app/quiz/waiting` in `F/app/router.ts` beside `/app/quiz/review`: neutral loading state while the status loads; on failure the translated message and "Try again"; the reviewing state for `WAITING_FOR_PLAN`, heading and text only (the board's trainer avatar and name, draft summary and "Message Marek" wait for an API with the trainer's name, the plan stories and messaging, spec Clarifications); the change of state announced and focus on the new heading (FR-017)
- [X] T030 [US2] [US3] Write `Q/pages/waiting-page.test.tsx` and extend `Q/pages/review-page.test.tsx` and `Q/pages/quiz-page.test.tsx` with a stubbed API: building state while sending; reviewing state after success and after a remount; `quiz.already-submitted` shows the waiting state; a failure shows the message and "Try again"; the draft is gone after success; loading and error of the status read; a step and the review send a client who submitted to the waiting state, and the waiting page sends one who did not to the quiz; in `F/app/router.test.tsx`, `/app/quiz/waiting` opens the waiting page, not a step
- [X] T031 Verify in the browser per `quickstart.md`, "Browser check, task #57", at 375 px, light and dark, English and Polish, keyboard only, console; run `npm run check` in `frontend/`; open the pull request for #57 after the user agrees (title `feat(frontend): add quiz submit and waiting states`, `Closes #57` and `Closes #54` as the story's last task); its "What changed" names what the waiting state leaves out and until when (T029)

---

## Phase 5: Convergence

- [X] T032 Run `/speckit-converge` on the branch of task #57, the story's last task, and walk through `quickstart.md` once more

---

## Dependencies

- T001 with #56. #56 (T002–T012) → #55 (T013–T024), which also waits for task #31 and story #18 → #57 (T025–T031), which waits for #55 and #56. T032 last.
- Inside #56: T002, T004 and T010 are independent; T003 follows T002; T005 needs T002 and T004; T006 needs T005; T007 is independent; T008 needs T005 and T006; T009 needs T005 and T007; T011 needs everything before it.
- Inside #55: T013 → T014 → T016; T018 and T019 need T014; T021 needs T016; tests follow their code.
- Inside #57: T025–T027 are independent; T028 needs T025–T027; T029 needs T025 and T027.

## Parallel examples

- #56: T002, T004 and T010 at once.
- #55: T013 and T023 at once.
- #57: T025, T026 and T027 at once.

## Implementation strategy

Build in order and open one pull request per task after its gate and the user's approval. #56 lets a client check the answers; #55 stores them once; #57 completes the story: submit and wait.
