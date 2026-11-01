# Implementation Plan: Trainer builds, edits and publishes a plan

**Branch**: `feat/68-plan-editor` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/68-plan-editor/spec.md`

## Summary

A new backend feature `plan` holds one plan per client, with sessions on distinct days of the week and exercise entries with targets (#69). Rules live in the domain, at two levels: every plan, and complete plans (#70). Three endpoints under `/api/v1/clients/{id}/plan` read, save and publish, with a version to refuse stale saves (#71, #72). A new frontend feature `plan` fills the Plan tab of the client page with an editor that keeps a local draft until Save (#73, #74). One pull request for the story, one commit per task, merged with rebase or a merge commit.

## Technical Context

**Language/Version**: Java 25; TypeScript 6

**Primary Dependencies**: Spring Boot 4.1 (MVC, Security 7, Data JPA, Flyway, Validation), MapStruct, Lombok; React 19, React Router 8, TanStack Query 5, react-i18next, zod, shadcn/ui. No new dependency.

**Storage**: PostgreSQL; tables `plans`, `plan_sessions`, `plan_session_entries` ([data-model.md](data-model.md))

**Testing**: JUnit 5 + AssertJ (unit with hand-written fakes, `@WebMvcTest`, `@DataJpaTest` + Testcontainers); Vitest + Testing Library

**Target Platform**: Browser SPA, trainer area designed at 1280 px; REST API under `/api/v1`, same origin

**Project Type**: Web application, `backend/` and `frontend/`

**Performance Goals**: SC-001: a two-session plan built and published in under 5 minutes

**Constraints**: Foreign client answers 404; stale saves are refused; a plan's size is bounded (6 sessions × 30 exercises)

**Scale/Scope**: 3 tables, 3 endpoints, 1 route, about 40 to 240 library exercises per trainer

## Constitution Check

| Principle | Status |
|---|---|
| I. Hexagonal boundaries | Pass. New feature `plan`, usual layers. `plan → client → quiz` and `plan → exercise`; other features read plans by SQL, never by class (research R6), so there is no cycle. |
| II. One contract | Pass. `schema.d.ts` is regenerated in the commits that change the API. Errors are problem details with stable codes ([data-model.md](data-model.md), "Rule codes"). The frontend translates by code. |
| III. Secure by default | Pass. No public endpoint; `/api/v1/clients/**` already needs the role `TRAINER`. Ownership goes through `getClient`, which answers 404 for foreign and unknown alike. `plansUsing` counts only the trainer's own plans. No personal data in logs. |
| IV. Plan changes | Not affected. A trainer's edit is not a plan change, because no rule or LLM is involved. Publishing and saving are the trainer's own acts. |
| V. Tests verify behaviour | Pass. A test per rule, per refusal and per screen behaviour (tasks.md). |
| VI. Simplicity | Pass. No status or order columns (R2), no ids in the API (R3), no drag library (R11), no form library for the table (R10). |
| VII. One design system | Pass. Tokens and shadcn components, EN and PL, browser check. The board's departures are listed in the spec. |

Re-checked after design: unchanged. The double statement of value limits (R7) is the existing pattern of `CreateExerciseRequest`, not new complexity.

## Project Structure

### Documentation (this feature)

```text
specs/68-plan-editor/
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
├── resources/db/migration/V13__create_plans.sql
├── resources/db/dev-data/R__demo_plans.sql               R__demo_accounts.sql, R__demo_quiz.sql changed
└── java/com/kinetiq/
    ├── plan/
    │   ├── domain/model/            Plan, Session, ExerciseEntry, PlanId
    │   ├── domain/input/            ManagePlanUseCase, SavePlanCommand, PlanDetails
    │   ├── domain/output/           PlanRepository
    │   ├── domain/service/          PlanManagementService
    │   ├── domain/exception/        PlanNotFoundException, PlanChangedException, PlanAlreadyPublishedException,
    │   │                            PlanRuleViolationException, ...
    │   └── infrastructure/
    │       ├── config/PlanConfig
    │       ├── input/               PlanController, SavePlanRequest, PublishPlanRequest, PlanResponse, PlanWebMapper
    │       └── output/              PlanEntity, PlanSessionEntity, PlanEntryEmbeddable, PlanMapper,
    │                                JpaPlanRepository, PlanRepositoryImpl
    ├── client/                      PlanState, ClientSummary, JdbcClientDirectory
    ├── quiz/                        QuizSubmissionRepository, QuizSubmissionService
    └── exercise/                    ListExercisesUseCase, ExerciseRepository, listing and management services

frontend/src/
├── app/router.ts                    the plan tab's route
├── features/account/components/     sign-out-button asks the leave guard first
├── features/client/                 Outlet context, PlanStateBadge
├── features/exercise/api/           moves the shared read to lib
├── lib/exercise-library.ts          useExercises, CATEGORIES, Exercise
├── lib/leave-guard.ts               the question sign-out asks (research R13)
├── lib/api/schema.d.ts              generated
├── locales/{en,pl}.json
└── features/plan/
    ├── api/plans.ts                 usePlan, useSavePlan, usePublishPlan
    ├── plan-draft.ts                draft, validation, request, estimated time
    ├── pages/plan-page.tsx          loading, error, no quiz, no plan, editor
    └── components/                  plan-editor, session-panel, entries-table, exercise-picker,
                                     validation-summary, leave-guard
```

**Structure Decision**: the existing web application layout, one new feature on each side.

### Commits

| Task | Commit holds |
|---|---|
| #69 | Spec Kit files, the `backend/CLAUDE.md` 3NF rule, migration, entities, repository, domain model without rules, demo data, repository IT |
| #70 | Rules in `Plan`, `Session` and `ExerciseEntry`, unit tests per rule |
| #71 | Service (read, save), controller, `client` and `exercise` changes, `schema.d.ts`, `errors.plan.*` |
| #72 | Publish in service and controller, quiz status, `schema.d.ts` |
| #73 | Route, plan page, editor frame, sessions, table, picker, `lib/exercise-library.ts`, plan state badge |
| #74 | Inline targets, reordering, delete, guard, validation summary, Save and Publish |

The `client`, `exercise` and `quiz` changes (spec FR-017, FR-032, FR-033) are not named in the issues. They go with the task whose API makes them true, because without them publishing leaves wrong statuses on other screens.

## Complexity Tracking

No constitution violations to justify.
