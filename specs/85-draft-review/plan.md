# Implementation Plan: Trainer reviews and publishes a drafted plan

**Branch**: `feat/85-draft-review` | **Date**: 2026-11-07 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/85-draft-review/spec.md`

## Summary

Three tasks, one pull request, one commit per task.

1. **#86 Approvals API.** `GET /api/v1/approvals/drafted-plans` lists the signed-in trainer's unpublished plans, oldest first, from a read model over `plans` and `accounts`. A second read, `GET /api/v1/clients/{id}/quiz-answers`, lets the trainer see the answers beside a draft (needed by #88, placed here with the other reads).
2. **#87 Approvals screen.** The Approvals page lists the drafts, with an empty state, and the sidebar badge shows the count.
3. **#88 Draft panel.** The editor gets the panel (answers, notes, failed-draft message, Build again). `POST /api/v1/clients/{id}/plan/drafting` starts a rebuild: a rate limit per trainer, a drafting row marked as a rebuild, and the existing job and sweep do the work. A finished rebuild replaces the draft in place (same row, new version); a failed one changes nothing.

No new dependency, no new external service, no new public endpoint.

## Technical Context

**Language/Version**: Java 25; TypeScript 6

**Primary Dependencies**: Spring Boot 4.1, Spring Data JPA, `JdbcTemplate` for read models; React 19, TanStack Query 5, react-i18next, shadcn/ui. None added.

**Storage**: PostgreSQL. New migration `V18__add_plan_rebuild.sql`: `plan_draftings.rebuild` and table `plan_build_requests` ([data-model.md](data-model.md)). Number checked against `master` and open pull requests before the pull request opens.

**Testing**: JUnit 5 + AssertJ with hand-written fakes for domain services, `@WebMvcTest` for controllers, `@DataJpaTest`/`@JdbcTest` with Testcontainers for SQL, the application smoke test; Vitest + Testing Library. Automated tests never call the real service (FR-030).

**Target Platform**: REST under `/api/v1`, same origin; trainer screens at 1280 px

**Project Type**: Web application, `backend/` and `frontend/`

**Performance Goals**: Build again answers in under 1 second (SC-005); the list is one query

**Constraints**: No connection held through a model call (spec 79 R4). The limit survives a restart and two simultaneous requests cannot both take the last slot. Nothing of the client reaches a log or the limit table.

**Scale/Scope**: A trainer has tens of clients; the list is not paged.

## Constitution Check

| Principle | Status |
|---|---|
| I. Hexagonal boundaries | Pass. The rebuild rules, the limit arithmetic and the replace-in-place are plain domain code (`PlanDraftingService`, `PlanRebuildService`); SQL stays in output adapters. The trainer's read of quiz answers lives in the `client` feature, which already depends on `quiz` (it uses `Goal`) and which `quiz` does not depend on, so no cycle forms; putting it in `quiz` would have created `quiz -> client -> quiz`. `plan` already depends on `client` and `quiz`. `HexagonalArchitectureTest` covers it. |
| II. One contract | Pass. New endpoints appear in the OpenAPI document; `schema.d.ts` is regenerated in the commit of each task that changes the API ([contracts/api.md](contracts/api.md)). Every refusal has a stable `code`; the frontend translates by code. |
| III. Secure by default | Pass. No public endpoint; all three paths sit under `/api/v1/clients/**` or a new trainer-only `/api/v1/approvals/**` rule. Ownership is checked in the use case and in the SQL; another trainer's client answers `404`. CSRF covers the `POST`. The limit table holds a trainer id and a time, nothing about clients. The prompt rules of spec 79 are untouched. |
| IV. Explainable, approved changes | Pass. Build again does not change a plan the client can see: it only replaces an unpublished draft that the trainer confirms replacing, and a failure keeps the previous draft. Publishing is unchanged. |
| V. Tests verify behaviour | Pass. Tests per behaviour listed in [tasks](tasks.md): ordering and scoping of the list, each refusal, the limit with a fixed clock, replacement and failure keeping the draft, the stale-version refusal, 404 for every operation. |
| VI. Simplicity | Pass. No queue, no scheduler, no retry library, no rate-limit library (R4), no new job: the existing job and sweep are reused. One migration. No paging, tabs or filters. |
| VII. One design system | Pass. `Alert`, `Badge`, `Card`, `AlertDialog`, `Empty`, `Skeleton`, `Spinner` with Kinetiq tokens, EN and PL, checked in the browser light and dark at 1280 px and narrow. |

Re-checked after design: unchanged.

## Project Structure

### Documentation (this feature)

```text
specs/85-draft-review/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/api.md
├── checklists/
└── tasks.md             (/speckit-tasks)
```

### Source Code

```text
backend/src/main/
├── resources/db/migration/V18__add_plan_rebuild.sql
├── resources/db/dev-data/                              R__demo_plans, R__demo_quiz, R__demo_accounts: drafts of each kind,
│                                                       a trainer close to the limit; README demo table
└── java/com/kinetiq/
    ├── plan/
    │   ├── domain/model/        Plan (rebuilt), DraftedPlan, DraftingKind
    │   ├── domain/input/        ListDraftedPlansUseCase, RebuildPlanUseCase
    │   ├── domain/output/       DraftedPlanDirectory, PlanBuildRequests; PlanDraftingRepository changed
    │   ├── domain/service/      PlanDraftingService changed, PlanRebuildService, DraftedPlanListService
    │   ├── domain/exception/    PlanRebuildLimitReachedException
    │   └── infrastructure/
    │       ├── config/          PlanConfig
    │       ├── input/           DraftedPlansController, PlanController (+rebuild), DraftingRunner,
    │       │                    QuizSubmittedListener, responses
    │       └── output/          JdbcDraftedPlanDirectory, PlanBuildRequestsImpl, PlanDraftingRepositoryImpl
    ├── client/
    │   ├── domain/input/        ReviewClientAnswersUseCase
    │   ├── domain/service/      ClientAnswersReviewService
    │   └── infrastructure/input/ ClientQuizAnswersController, QuizAnswersResponse
    ├── quiz/
    │   └── infrastructure/output/ JpaQuizSubmissionRepository (drafting query ignores rebuilds)
    └── common/infrastructure/config/SecurityConfig        /api/v1/approvals/** needs TRAINER

frontend/src/
├── app/                        trainer-area (badge), router (approvals route)
├── features/plan/
│   ├── api/                    drafted-plans, rebuild mutation, quiz answers query
│   ├── components/             draft-panel, rebuild-dialog (alert dialog), rebuilding-state
│   └── pages/                  approvals-page, plan-page changed
├── lib/api/schema.d.ts         regenerated
└── locales/                    en.json, pl.json
```

**Structure Decision**: The list belongs to the `plan` feature for now (it is the only thing in Approvals). When plan changes arrive, Approvals becomes its own feature and takes this list with it, a move of one use case and one controller.

## Design in brief

- **Rebuild is a drafting with a flag.** `plan_draftings` gains `rebuild boolean`. `storeBuilt` and `storeFailed` read the flag when they remove the row: a rebuild replaces the stored draft in place (same id, version raised by the save) or, on failure, only removes the row. A first drafting behaves exactly as today. Whoever removes the row stores, so the job and the sweep still cannot both store, and a result after the 6 minute limit is dropped (R2).
- **Client state needs no change.** `JdbcClientDirectory` already reports `PLAN_DRAFTING` whenever a row exists, so the client list and the Plan tab see a rebuild as drafting. The plan endpoint keeps answering with the stored draft, so the editor can show it read-only (R5).
- **Edits are refused during drafting.** `save` and `publish` check the drafting row for an existing plan too and answer `plan.drafting` (R3). Build again is refused when a row exists, so two requests at once leave one row (the primary key) and one refusal.
- **Limit.** `PlanBuildRequests` records `(trainer, at)`; the domain counts the trainer's requests in the last 24 hours and refuses at 10 with the time the oldest of the counted ones leaves the window. A transaction-scoped advisory lock per trainer makes the count-then-insert atomic (R4).
- **List.** One SQL read model: unpublished plans of the trainer's clients, with name, mark, weeks, session count and creation time, ordered by creation time then plan id (R6).
- **Answers.** `GET /clients/{id}/quiz-answers`, served by the `client` feature, returns the stored answers after the ownership check, `404` otherwise (R7).
- **Client status.** The client's own quiz status counts only first draftings, so a rebuild never changes what the client sees (R10).

## Complexity Tracking

| Item | Why | Simpler option rejected |
|---|---|---|
| Advisory lock per trainer | A count followed by an insert would let two simultaneous requests both pass at 9 | A unique-per-slot table or optimistic retry is more code; a lock is two lines in one adapter |
| `rebuild` flag on the drafting row | The result must replace a draft in place or be dropped, which a first drafting must not do | A second table for rebuilds would duplicate the sweep and the job's guard |
