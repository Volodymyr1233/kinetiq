# Tasks: Trainer reviews and publishes a drafted plan

**Input**: Design documents from `specs/85-draft-review/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api.md, quickstart.md

**Tests**: Included. The Definition of Done and constitution V ask for them; each is a behaviour test, not a coverage test. No automated test calls the real drafting service (FR-030).

**Organization**: One phase per GitHub task, because each phase ends in one commit of the story's single pull request (`CLAUDE.md`). Tasks that serve one user story carry its label. Every commit must compile and pass the gate of each side it touches (`./gradlew check` in `backend/`, `npm run check` in `frontend/`) and carry the regenerated `schema.d.ts` when it changes the API. Merge with rebase or a merge commit, not squash.

## Format: `- [ ] [ID] [P?] [Story?] Description`

- **[P]**: can run in parallel (different files, no dependency on unfinished tasks)
- **[US1]…[US3]**: the user story in spec.md (US1 list, US2 review panel, US3 build again)

Paths use `plan/`, `quiz/`, `client/` for those features under `backend/src/main/java/com/kinetiq/`, or under `backend/src/test/java/com/kinetiq/` for tests; `fe/` stands for `frontend/src/`.

---

## Phase 1: Approvals API: drafted plans (#86)

**Purpose**: The trainer's drafts and the client's answers can be read, scoped to the trainer. Commit: `feat(backend): list drafted plans for approvals (#86)`, with the Spec Kit files of this story.

- [X] T001 [US1] Add record `DraftedPlan(AccountId client, String clientName, Kind kind, int weeks, int sessionsPerWeek, Instant createdAt)` with nested enum `Kind { BUILT, FAILED, SAVED }` in `plan/domain/model/DraftedPlan.java`; no framework types (data-model.md)
- [X] T002 [US1] Add port `DraftedPlanDirectory` (`List<DraftedPlan> findOf(AccountId trainer)`) in `plan/domain/output/`, use case `ListDraftedPlansUseCase` (`List<DraftedPlan> draftedPlans(AccountId trainer)`) in `plan/domain/input/`, and `DraftedPlanListService` in `plan/domain/service/` delegating to the port; wire the bean in `plan/infrastructure/config/PlanConfig.java`
- [X] T003 [US1] Add `JdbcDraftedPlanDirectory` in `plan/infrastructure/output/` (`@Repository`, `JdbcTemplate`): one query over `plans` joined to `accounts` with `a.trainer_id = ?` in the join condition, only `p.first_week is null`, with the name, `automatic_draft` (`BUILT` → BUILT, `FAILED` → FAILED, null → SAVED), `weeks`, the count of the plan's rows in `plan_sessions` (check the column names in `V13__create_plans.sql`) and `p.created_at`, `order by p.created_at, p.id`. A comment says it reads `accounts` by SQL as `JdbcClientDirectory` does (research R6)
- [X] T004 [US1] Add `DraftedPlansController` (`GET /api/v1/approvals/drafted-plans`) and records `DraftedPlansResponse(@NotNull List<Item> items)` / `Item` in `plan/infrastructure/input/`, `@NotNull` on required fields as `PlanResponse` does; take the trainer from `Authentication` as `PlanController` does. Add `/api/v1/approvals/**` to the TRAINER matcher in `common/infrastructure/config/SecurityConfig.java`
- [X] T005 [US2] Add `QuizNotFoundException` (`quiz.not-found`, kind `NOT_FOUND`) in `quiz/domain/exception/`. In the `client` feature (which already uses `quiz`; research R7) add port `ReviewClientAnswersUseCase` (`QuizSubmission answers(AccountId trainer, AccountId client)`) in `client/domain/input/` and `ClientAnswersReviewService` in `client/domain/service/`: it finds the client through the feature's own `ClientDirectory` for that trainer (so a foreign or unknown client throws `ClientNotFoundException`), then reads `ViewQuizAnswersUseCase`; wire it in the client feature's config class
- [X] T006 [US2] Add `ClientQuizAnswersController` (`GET /api/v1/clients/{id}/quiz-answers`, already trainer-only by `/api/v1/clients/**`) and record `QuizAnswersResponse` in `client/infrastructure/input/` with `goal, experience, level, age, heightCm, weightKg, likes, daysPerWeek, notes` (omitted when none, `@JsonInclude(NON_NULL)`) and `submittedAt`, mapped with a small web mapper there
- [X] T007 [P] [US1] Demo data in `backend/src/main/resources/db/dev-data/` (`R__demo_accounts.sql`, `R__demo_quiz.sql`, `R__demo_plans.sql`, idempotent): for the demo trainer a built draft, a failed draft and a draft the trainer saved, created at different ages, plus a client with a published plan; a second trainer with one draft. Update the demo table in `README.md`
- [X] T008 [P] [US1] Write `plan/infrastructure/output/JdbcDraftedPlanDirectoryIT.java` (Testcontainers; `*IT` per `backend/CLAUDE.md`): lists only the trainer's own unpublished plans, never another trainer's, never a published one, never a client with no plan; oldest first with equal ages in a stable order; each kind (marked built, marked failed, unmarked); the session count; a client with a drafting row and no plan is absent, while a plan plus a drafting row (a rebuild) is still listed
- [X] T009 [P] [US1] Write `plan/infrastructure/input/DraftedPlansControllerTest.java` (`@WebMvcTest`): `401` without a sign-in, `403` for a client, `200` with the documented shape for a trainer, with no field outside the contract
- [X] T010 [P] [US2] Write `client/domain/service/ClientAnswersReviewServiceTest.java`: own client's answers returned; another trainer's client and an unknown client both throw `ClientNotFoundException`; an own client with no answers throws `QuizNotFoundException`. Write `client/infrastructure/input/ClientQuizAnswersControllerTest.java`: `404 client.not-found`, `404 quiz.not-found`, `403` for a client, notes omitted when none
- [X] T011 Run `./gradlew check` in `backend/` (includes `HexagonalArchitectureTest`: no cycle between `client` and `quiz`); start the backend and run `npm run gen:api` in `frontend/` to regenerate `fe/lib/api/schema.d.ts`; commit with `specs/85-draft-review/`

---

## Phase 2: Approvals screen: drafted plans (#87)

**Purpose**: The trainer sees the drafts on one page and opens any of them. Commit: `feat(frontend): add approvals screen for drafted plans (#87)`.

- [X] T012 [US1] Add `fe/features/plan/api/drafted-plans.ts`: `useDraftedPlans()` (TanStack Query, key `['approvals', 'drafted-plans']`, `api.GET('/api/v1/approvals/drafted-plans')`, throws the problem on error, refetches on window focus). In `fe/features/plan/api/plans.ts` make `useRefreshAfterSaving` also invalidate `['approvals']`, so a save or publish updates the list and the badge
- [X] T013 [US1] Add `fe/features/plan/pages/approvals-page.tsx`: heading "Approvals" with the intro text, a section "Drafted plans" holding a list (`<ul>`) of rows, each one link to `/trainer/clients/{clientId}/plan` (check the route in `fe/app/router.ts`) containing `AccountAvatar` initials, the client name, the age ("3 h ago", `Intl.RelativeTimeFormat` for the active locale), a `Badge` with the kind as text ("Plan to review", "Draft failed", "Draft" — `variant` by kind, never colour alone) and "{weeks} weeks · {n} sessions a week" with plural forms. Loading: skeleton rows with a status for assistive technology. Error: `Alert` with the translated code and a retry button. Empty: `Empty` with "Nothing to approve. Drafted plans show up here." and a "View clients" link. Semantics per FR-007
- [X] T014 [US1] Replace `trainerPlaceholder('approvals')` in `fe/app/router.ts` with a lazy route to the page. In `fe/app/trainer-area.tsx` show the count of `useDraftedPlans().data.items` after the Approvals label when above 0, as a muted pill with an `aria-label` ("{n} drafted plans waiting") — the unread count of messaging is a different feature, so build the small element here
- [X] T015 [P] [US1] Add every string of the Texts table of the spec used here to `fe/locales/en.json` and `fe/locales/pl.json` (`approvals.*`, `nav.approvalsCount`), Polish with three plural forms where a count appears
- [X] T016 [P] [US1] Write `fe/features/plan/pages/approvals-page.test.tsx`: rows in the order given with name, kind text and the link target; each kind's wording; empty state and its link; error with retry; weeks and sessions plural. Extend `fe/app/trainer-area.test.tsx`: the badge shows the count and is absent at 0
- [X] T017 Run `npm run check` in `frontend/`. Open Approvals in the built-in browser at 1280 px and at 375 px, light and dark: list, empty state (sign in as a trainer with no drafts), keyboard order and focus ring, long names truncate without sideways scroll, badge contrast. Fix what is found; commit

---

## Phase 3: Draft panel in the editor (#88)

**Purpose**: The trainer reads the answers beside a draft and can ask for a new automatic one, within a daily limit. Commit: `feat: add draft panel and build again to the plan editor (#88)`.

### Backend

- [X] T018 [US3] Add migration `backend/src/main/resources/db/migration/V18__add_plan_rebuild.sql`: `alter table plan_draftings add column rebuild boolean not null default false;` and `create table plan_build_requests (id uuid not null, trainer_id uuid not null, requested_at timestamptz not null, constraint pk_plan_build_requests primary key (id), constraint fk_plan_build_requests_trainer foreign key (trainer_id) references accounts (id))` with `create index ix_plan_build_requests_trainer_requested_at on plan_build_requests (trainer_id, requested_at)` and a comment that rows hold no client and no content. Before the pull request opens check `master` and open pull requests for a clashing number (`gh pr list --json number,files`)
- [X] T019 [US3] In `plan/domain/model/` add enum `DraftingKind { FIRST, REBUILD }` and `Plan.rebuiltWith(Goal goal, int weeks, List<Session> sessions)`: same id and client and version, no first week, mark `BUILT`, `requireComplete()`, throws `PlanAlreadyPublishedException` when published (data-model.md). Change `plan/domain/output/PlanDraftingRepository.java`: `start(client, at, kind)`, `Optional<DraftingKind> remove(client)`, `boolean exists(client)`; update `PlanDraftingRepositoryImpl` (`rebuild` column) and `QuizSubmittedListener` (`FIRST`). Change the native query of `isPlanDrafting` in `quiz/infrastructure/output/JpaQuizSubmissionRepository.java` to `select exists (select 1 from plan_draftings where client_id = :clientId and not rebuild)`, so a rebuild never changes the client's status (FR-013a, research R10)
- [X] T020 [US3] In `PlanDraftingService` branch on the kind returned by `remove`: for `FIRST` keep today's behaviour; for `REBUILD`, `storeBuilt` loads the stored plan and drops the result when it is absent or published, the answers are gone or the trainer changed, otherwise checks the library and saves `plan.rebuiltWith(quiz goal, 12, sessions)` (the save raises the version); `storeFailed` only removes the row. `PlanDraftingJob` and the sweep stay unchanged. Add `RebuildPlanUseCase` (`void rebuild(AccountId trainer, AccountId client)`) in `plan/domain/input/`
- [X] T021 [US3] Add port `PlanBuildRequests` (`lock(AccountId trainer)` transaction-scoped, `List<Instant> since(AccountId trainer, Instant from)` oldest first, `record(AccountId trainer, Instant at)` which also deletes the trainer's rows older than 24 hours) and `PlanBuildRequestsImpl` in `plan/infrastructure/output/` using `pg_advisory_xact_lock(hashtextextended(?, 0))` on the trainer id text (research R4). Add `PlanRebuildLimitReachedException` (`plan.rebuild-limit-reached`, `Kind.TOO_MANY_ATTEMPTS`, `properties()` = `availableAt` as an ISO instant string) in `plan/domain/exception/`
- [X] T022 [US3] Add `PlanRebuildService implements RebuildPlanUseCase` in `plan/domain/service/` with constants `LIMIT = 10` and `WINDOW = Duration.ofHours(24)`: `clients.getClient(trainer, client)` (404 for foreign and unknown), plan absent → `PlanNotFoundException`, published → `PlanAlreadyPublishedException`, then `lock(trainer)`, a drafting row → `PlanDraftingInProgressException`, count of requests since `now - WINDOW` at or above `LIMIT` → `PlanRebuildLimitReachedException` with `availableAt = requests.get(count - LIMIT) + WINDOW`, else `record` and `drafting.start(client, now, REBUILD)`. Only an accepted request is recorded (FR-017). Wire in `PlanConfig`
- [X] T023 [US3] In `PlanManagementService` refuse with `PlanDraftingInProgressException` when `summary.getPlanState() == PlanState.PLAN_DRAFTING` for an existing plan in `save` (before `requireSameVersion`) and in `publish`, so a stale or hand-made request cannot change a draft that a rebuild will replace (research R3)
- [X] T024 [US3] Extract the executor and `run` call of `QuizSubmittedListener` into a package-private `DraftingRunner` bean in `plan/infrastructure/input/` (`start(AccountId)`, virtual-thread executor, `@PreDestroy`), used by the listener and by the controller. Add to `PlanController` `POST /api/v1/clients/{id}/plan/drafting` → `202`, calling `rebuild(...)` and then `runner.start(client)` after it returned, so the job starts only once the transaction committed (a crash between is ended by the sweep)
- [X] T025 [P] [US3] Demo data: a trainer one build short of the limit, with nine rows in `plan_build_requests` in the last 24 hours (idempotent insert, times relative to `now()`), and a client whose draft is being rebuilt (a `plan_draftings` row with `rebuild = true`) in `R__demo_plans.sql`; update the `README.md` demo table

### Backend tests

- [X] T026 [P] [US3] `plan/domain/model/PlanTest.java`: `rebuiltWith` keeps id, client and version, sets the mark `BUILT` and replaces content; refuses an incomplete result and a published plan
- [X] T027 [P] [US3] `plan/domain/service/PlanDraftingServiceTest.java` (hand-written fakes): a rebuild result replaces a built draft, a failed draft and a trainer-saved draft, and the version is higher; a failed rebuild leaves the stored draft and its version untouched and removes the row; a result for a published plan, with gone answers, a changed trainer or an already removed row is dropped; an exercise gone from the library rejects the result and keeps the row; a first drafting still stores a new draft
- [X] T028 [P] [US3] `plan/domain/service/PlanRebuildServiceTest.java` with a fixed clock: refusals in the order of data-model.md and none of them records a request; the tenth request in 24 hours passes, the eleventh is refused with `availableAt` equal to the oldest counted request plus 24 hours; after that moment a request passes; another trainer's requests do not count; a second request while a row exists is refused with `plan.drafting`
- [X] T029 [P] [US3] `plan/domain/service/PlanManagementServiceTest.java` (extend): save and publish of an existing plan are refused with `plan.drafting` while the client's state is drafting, and work as before otherwise
- [X] T030 [P] [US3] `plan/infrastructure/output/PlanBuildRequestsImplIT.java` (Testcontainers; `*IT` per `backend/CLAUDE.md`): `since` is oldest first and per trainer; `record` deletes rows older than the window; twelve simultaneous accepted attempts through `PlanRebuildService` on a real database leave exactly ten requests and two refusals; two simultaneous requests for one client leave one `202`, one `plan.drafting` refusal and one recorded request; and (`quiz/infrastructure/output/QuizSubmissionRepositoryImplIT.java`) a `REBUILD` drafting row does not make the client's quiz status `DRAFTING_PLAN`, while a first drafting row does (SC-009)
- [X] T031 [P] [US3] Extend `plan/infrastructure/input/PlanControllerTest.java` (`@WebMvcTest`): `202` with no body; `404 client.not-found`, `404 plan.not-found`, `409 plan.already-published`, `409 plan.drafting`, `429 plan.rebuild-limit-reached` with `availableAt` in the body; `403` for a client; the job starts only after the use case returned
- [X] T032 Run `./gradlew check`; start the backend and regenerate `fe/lib/api/schema.d.ts` with `npm run gen:api`

### Frontend

- [X] T033 [US2] Add `fe/features/plan/api/quiz-answers.ts` (`useQuizAnswers(client)`, `GET /api/v1/clients/{id}/quiz-answers`, a `quiz.not-found` answer becomes `null`) and `useBuildPlanAgain(client)` in `plan.ts` (`POST /api/v1/clients/{id}/plan/drafting`; on success invalidate `['clients']` so the tab sees the drafting state; on error rethrow the problem)
- [X] T034 [US2] Add `fe/features/plan/components/draft-panel.tsx`: a `Card` with the title "{name}'s answers", a description list of goal, experience, level, age, height, weight (in the trainer's unit, as the editor does for loads), likes and days per week, then the notes as plain wrapped text or "{name} wrote no notes."; when answers are `null`, a line says they are unavailable. For a failed draft it shows the failed-draft message ("Couldn't build this plan automatically. Try again or build it yourself.") and the "Build again" button; for a built or saved draft the button sits under the answers, quieter. Absent for a published plan. Stacks under the sessions on a narrow screen
- [X] T035 [US3] Add `fe/features/plan/components/build-again-dialog.tsx` with `AlertDialog`: title "Build {name}'s plan again?", text "The current draft, including your edits, will be replaced by a new automatic one if it can be built.", focus on the safe "Cancel", accept "Replace draft". On a `429` the button is disabled and the panel shows "You've used today's automatic builds. You can build again from {time}." with `availableAt` formatted for the active locale; on `409 plan.drafting` the tab refreshes to the drafting state; other errors show `t('errors.<code>')`
- [X] T036 [US3] In `fe/features/plan/pages/plan-page.tsx` ask for the plan also while the client is drafting; when it exists render `PlanEditor` read-only with the drafting line (`role="status"`) above it, otherwise today's `DraftingState`. Add a `readOnly` prop to `PlanEditor` (`fe/features/plan/components/plan-editor.tsx` and its table, picker and session controls) that disables every editing control, Save and Publish and hides Build again. Keep polling the client every 5 seconds only while this state shows. During a rebuild do not reset the plan query (`useFollowDrafting` resets it on mount today; that stays for a first drafting only), so the draft stays on screen. Extend `DraftingEndAnnouncement` so it also announces the end of a rebuild, whether the plan changed or not. Remember the plan's version when Build again was accepted; when the state leaves drafting and the refetched plan has the same version show an `Alert` "Couldn't build a new draft. Your previous one is kept." announced to assistive technology, with Build again offered once more; a higher version shows nothing. Move the failed-draft message into the panel and keep the built note of `automatic-draft-note.tsx` (FR-010)
- [X] T037 [P] [US3] Add the strings of the Texts table (panel, dialog, limit, rebuild failed, drafting line) to `fe/locales/en.json` and `fe/locales/pl.json`, the Polish with "ty", under `plan.*` and `errors.plan.rebuild-limit-reached`, `errors.quiz.not-found`
- [X] T038 [P] [US2] Write `fe/features/plan/components/draft-panel.test.tsx`: answers and notes shown, no-notes line, unavailable-answers line, failed message and button only for a failed draft, absent for a published plan
- [X] T039 [P] [US3] Extend `fe/features/plan/pages/plan-page.test.tsx`: Build again opens the dialog with Cancel focused and does nothing until accepted; accepting posts once, shows the drafting line above the read-only draft with Save, Publish and editing disabled; a higher version afterwards shows the new draft with no message; the same version shows the "previous one is kept" message; a `429` disables the action and shows the time; a `409` returns to the drafting state; first drafting with no plan still shows only the drafting state
- [X] T040 Run `npm run check` in `frontend/` and `./gradlew check` in `backend/`; commit with `schema.d.ts`

---

## Phase 4: Verification before the pull request

**Purpose**: The result is checked end to end on the UI and the backend, as the story demands (not a commit unless it finds something, in which case the fix goes into the commit of the task it belongs to via `git commit --fixup` and an autosquash rebase).

- [X] T041 Run the quickstart scenarios 1–6 in the built-in browser against the local backend with a stand-in service (service unconfigured gives the failed rebuild; the real key gives the success path): list, review, build again, failed rebuild, refusals, stale tab. Light and dark, 1280 px for the trainer and a narrow width for the panel, keyboard only through the dialog, no console errors
- [X] T042 Security pass (scenario 7): the three new endpoints without a sign-in, with a client's sign-in, with another trainer's client id and without the CSRF header; JSON checked for text that should not be there; backend log of a rebuild searched for client names, notes and prompt text. Fix and report any finding
- [X] T043 Run a rebuild once against the real service with `OPENROUTER_API_KEY` set (never read `.env`), report the outcome (built, or kept previous draft after failure) and that the prompt rules of spec 79 still held
- [ ] T044 Re-check migration number `V18` (renumbered from `V17`, which open pull request #251 takes) against `master` and open pull requests; confirm both quality gates pass on the final tree and report their real results; push the branch and open one pull request titled `feat: review and publish drafted plans (#85)` closing #85, #86, #87 and #88 only when asked

---

## Dependencies and order

- Phase 1 → Phase 2 (needs the list endpoint and `schema.d.ts`) → Phase 3 (needs the answers endpoint from Phase 1; its backend part is independent of Phase 2 but shares the editor with nothing in Phase 2, so the order is for commit clarity).
- Inside Phase 3: T018 → T019 → T020 → T022 (needs T021) → T023, T024 → backend tests → T032 → frontend T033–T036 (T037 in parallel) → tests T038–T039 → T040.
- Phase 4 follows all three.

## Parallel opportunities

- Phase 1: T007, T008, T009, T010 after T001–T006.
- Phase 2: T015 with T012–T014; T016 after the page exists.
- Phase 3: T025 with T018–T024; T026–T031 once their subjects exist; T037–T039 once T036 exists.

## Implementation strategy

MVP is Phase 1 + Phase 2 (US1): the trainer finds every draft on one page and opens it. Phase 3 adds the review panel (US2) and Build again (US3). Each phase is one reviewed commit that compiles and passes its gates alone.
