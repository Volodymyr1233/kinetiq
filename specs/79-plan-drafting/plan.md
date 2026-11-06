# Implementation Plan: First plan is drafted from the quiz

**Branch**: `feat/79-plan-drafting` | **Date**: 2026-11-05 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/79-plan-drafting/spec.md`

## Summary

Submitting the quiz records that drafting is due, in the same transaction, and publishes an event. After the commit a virtual thread builds a prompt from the answers and the library, calls the model through a `PlanGenerator` port (Spring AI's OpenAI client pointed at OpenRouter), checks the answer against the plan rules and narrower limits, retries once with the reasons, and stores a draft or an empty failed draft. A sweep ends anything left behind by a restart. The client's status and the trainer's plan state gain the new states, and the waiting screen and Plan tab follow them by polling. One pull request for the story, one commit per task, merged with rebase or a merge commit.

## Technical Context

**Language/Version**: Java 25; TypeScript 6

**Primary Dependencies**: Spring Boot 4.1, Spring AI 2.0.2 (`spring-ai-openai` module only, research R2); React 19, TanStack Query 5, react-i18next, shadcn/ui. One new backend dependency, no new frontend dependency.

**Storage**: PostgreSQL; new table `plan_draftings`, new column `plans.automatic_draft` ([data-model.md](data-model.md))

**Testing**: JUnit 5 + AssertJ (unit with hand-written fakes, `@WebMvcTest`, `@DataJpaTest` + Testcontainers, the application smoke test); Vitest + Testing Library. Automated tests never call the real service (FR-034).

**Target Platform**: REST API under `/api/v1`, same origin; client screens at 375 px, trainer screens at 1280 px

**Project Type**: Web application, `backend/` and `frontend/`

**Performance Goals**: The submit answers in under 1 second (SC-001); drafting ends within 6 minutes (SC-002)

**Constraints**: No database connection is held during a model call (R4). No identifier held as data reaches the prompt, free text is cleaned first, and the logs hold none. One attempt is one request.

**Scale/Scope**: One background job per submitted quiz; a prompt holds up to 240 exercises; no new endpoint

## Constitution Check

| Principle | Status |
|---|---|
| I. Hexagonal boundaries | Pass. The prompt, checks and retry are plain domain code; Spring AI stays in one output adapter. Quiz does not depend on plan: it publishes an event and reads `plan_draftings` by SQL, as it reads `plans` today, so there is no cycle. `HexagonalArchitectureTest` covers it. |
| II. One contract | Pass. `schema.d.ts` is regenerated in the commit that changes the API ([contracts/api.md](contracts/api.md)). The new refusal has a stable code. |
| III. Secure by default | Pass. No new endpoint. The key comes from an environment variable. Identifiers held as data never enter the prompt, and free text follows the 1.2.0 amendment: known identifiers removed, the person told. A test proves it. Logs hold only model, outcome, duration and codes, never an exception message from the service. |
| IV. Explainable, approved changes | Pass. A first draft is not a plan change, and the same safeguards apply: the domain checks the answer before anyone sees it, nothing reaches the client until a trainer publishes, and the note says the system built it. |
| V. Tests verify behaviour | Pass. A test per check, per failure path, per state and per screen state; a sentinel test for personal data (tasks.md). |
| VI. Simplicity | Pass, with one justified exception below. No template generator (R3), no starter or auto-configuration (R2), no queue or retry library, no fields the model does not need (FR-013). |
| VII. One design system | Pass. Existing `Alert`, `Badge`, `Skeleton` and `Spinner` with Kinetiq tokens, EN and PL, browser check. |

Re-checked after design: unchanged.

## Project Structure

### Documentation (this feature)

```text
specs/79-plan-drafting/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/api.md
├── contracts/prompt.md
├── checklists/
└── tasks.md             (/speckit-tasks)
```

### Source Code

```text
.env.example                                            new: OPENROUTER_API_KEY, OPENROUTER_MODEL
CONTEXT.md                                              "Failed draft" added

backend/
├── build.gradle.kts                                    Spring AI BOM, spring-ai-openai
└── src/main/
    ├── resources/application.yaml                      kinetiq.plan-drafting.*
    ├── resources/db/migration/V16__add_plan_drafting.sql
    ├── resources/db/dev-data/                          R__demo_accounts, R__demo_quiz, R__demo_plans changed; README demo table too
    └── java/com/kinetiq/
        ├── plan/
        │   ├── domain/model/        AutomaticDraft, GeneratedPlan, PlanPromptMessages; Plan changed
        │   ├── domain/input/        DraftPlanUseCase, DraftInput, PlanDrafting
        │   ├── domain/output/       PlanGenerator, PlanDraftingRepository
        │   ├── domain/service/      PlanDraftingService, PlanDraftingJob, PlanPrompt, SessionDays, PersonalDataCleaner,
        │   │                        GeneratedPlanChecks
        │   ├── domain/exception/    PlanDraftingInProgressException, GeneratedPlanRejectedException,
        │   │                        PlanGenerationFailedException, PlanGeneratorUnavailableException
        │   └── infrastructure/
        │       ├── config/          PlanConfig changed
        │       ├── input/           QuizSubmittedListener, PlanDraftingSweep; PlanResponse changed
        │       └── output/          OpenRouterPlanGenerator, UnavailablePlanGenerator, PlanDraftingRepositoryImpl;
        │                            PlanEntity, PlanMapper, PlanRepositoryImpl changed
        ├── quiz/
        │   ├── domain/model/        QuizSubmitted; QuizStatus changed
        │   ├── domain/input/        ViewQuizAnswersUseCase
        │   ├── domain/output/       QuizEventPublisher; QuizSubmissionRepository changed
        │   └── infrastructure/      SpringQuizEventPublisher; repository and mapper changed
        └── client/
            ├── domain/model/        PlanState changed
            └── infrastructure/output/JdbcClientDirectory changed

frontend/src/
├── features/quiz/                   waiting page, status check, review notice
├── features/plan/                   drafting state, draft note, plan-page polling
├── features/client/                 badge variants
├── features/account/                nothing but the consent text in locales
├── lib/api/schema.d.ts              regenerated
└── locales/{en,pl}.json
```

**Structure Decision**: The existing layout. Drafting belongs to the `plan` feature, which already depends on `quiz`, `client` and `exercise`.

### How the pieces meet

1. `QuizSubmissionService.submit` saves the answers and calls `QuizEventPublisher.submitted`, which publishes `QuizSubmitted` as a Spring application event.
2. `QuizSubmittedListener` has two handlers. `@EventListener` runs inside the submit's transaction and calls `DraftPlanUseCase.request`, so the record and the answers commit together. `@TransactionalEventListener(AFTER_COMMIT)` hands `PlanDrafting.run` to a virtual thread, so the submit answers at once and a rolled-back submit starts nothing.
3. `PlanDraftingJob.run` holds no transaction. It reads its input through `DraftPlanUseCase.input` (a short transaction), makes at most 2 attempts, and stores through `DraftPlanUseCase.storeBuilt` or `storeFailed` (short transactions).
4. `PlanDraftingSweep` asks `DraftPlanUseCase.overdue` every 30 seconds for drafting older than 5 minutes 30 seconds, and calls `storeFailed` for each in its own transaction, so one bad row cannot hold up the others. That keeps the 6 minute promise of FR-005.

### Commits

| Commit | Holds |
|---|---|
| #80 | Dependency, properties, `.env.example`, the model bean behind a condition, proof that the application starts without a key; the Spec Kit files and the constitution amendment with its `CLAUDE.md` lines |
| #81 | `GeneratedPlan`, `PlanGenerator`, `OpenRouterPlanGenerator`, `UnavailablePlanGenerator` |
| #82 | `PlanPrompt`, `PersonalDataCleaner`, `ViewQuizAnswersUseCase`, the personal-data test |
| #83 | Migration `V16` (the mark and `plan_draftings`), `GeneratedPlanChecks`, `PlanDraftingJob`, failed draft, editor note, badges, demo data |
| #84 | Event, use of `plan_draftings`, sweep, statuses, refused save, waiting screen, Plan tab drafting state, review notice and consent |

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| `PlanDrafting` is a port in `domain.input` that does not end in `UseCase` | `TransactionConfig` wraps every `*UseCase` in a transaction, which would hold a database connection for up to 5 minutes | Exempting one use case by name in `TransactionConfig` hides the exception in infrastructure; calling the model inside a use case exhausts the connection pool under load |
| A table for something that lasts minutes | A restart must not lose it, and it must exist in the submit's transaction | Deriving "drafting" from "answers but no plan" would mark clients from before this story as drafting (User Story 1, scenario 5) |
