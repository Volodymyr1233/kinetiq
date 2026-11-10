# Implementation Plan: Plan change content is drafted and checked

**Branch**: `feat/131-plan-change-content` | **Date**: 2026-11-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/131-plan-change-content/spec.md`

## Summary

When a rule of story #125 fires, the weekly check now builds the plan change itself: it asks an LLM (anonymised plan, rule numbers, weekly aggregates) for the content, checks the answer against the limits of the rule and the plan rules, asks once more if needed, and otherwise applies the rule's fixed transformation. The result is stored as a pending plan change (rule, rule numbers as per-week figures, old and new value per target, effective Monday) in the same transaction as the weekly claim. Title and reason are built on read from the stored numbers, in English or Polish. A trainer reads the plan change on a read-only detail screen reached from the client page. Accept, keep, Approvals, notifications and the client view are #137; applying and expiring is #143.

## Technical Context

**Language/Version**: Java 25, Spring Boot 4.1; TypeScript 5, React 19.

**Primary Dependencies**: existing only: Spring MVC, Spring Data JPA, JdbcTemplate (read models), Flyway, Spring AI `ChatModel` (OpenRouter, already used by `plan`), MapStruct, Lombok; shadcn `chart`, `table`, `card`, `badge` on the frontend. No new dependency.

**Storage**: PostgreSQL; migration `V24__create_plan_changes.sql` (next free number; `V23` is `weekly_checks`; re-check `master` and open PRs before the pull request).

**Testing**: JUnit 5 + AssertJ with hand-written fakes (domain), `@DataJpaTest` + Testcontainers (`*IT`) for the persistence adapter, `@WebMvcTest` for controllers, Vitest + Testing Library on the frontend.

**Target Platform**: Linux server (single image serving the SPA); trainer UI at 1280px.

**Project Type**: web application (backend + frontend).

**Performance Goals**: the weekly check of one client, including at most two LLM calls, ends within the existing 10-minute sweep tick; detail opens in under 1 s on the local stack.

**Constraints**: no personal identifier in a prompt; the draft is checked before anything is stored; one plan change per client; a failure leaves nothing stored and is retried by the next tick.

**Scale/Scope**: weekly per client; tens of clients per trainer; 1 plan change per client at a time.

## Constitution Check

*GATE: passes before research and after design.*

| Principle | Check |
|---|---|
| I Hexagonal | New code sits in the `planchange` feature: domain (`model`, `input`, `output`, `service`, `rule`), infrastructure (`input`, `output`, `config`). The LLM is reached through the output port `PlanChangeDrafter`; plan and exercise data are read through the output port `PlanSource` backed by a JDBC read model (same precedent as `JdbcTrainingHistory`). `HexagonalArchitectureTest` must stay green. |
| II One contract | Two read endpoints in the generated OpenAPI; `schema.d.ts` regenerated in the same pull request; errors are problem details with codes (`plan-change.not-found`); no backend text displayed except the server-built title and reason, which the story requires in both languages and which #137 posts into the conversation. |
| III Secure by default | Both endpoints need authentication and are trainer-only; ownership checked in the use case via the client feature's port, and `/api/v1/plan-changes/**` added to the trainer matchers of `SecurityConfig`; another person's resource answers `404`. The prompt carries keys, categories, measures and numbers only: no names, no free text, no ids, not even exercise or session names (they can be trainer-written). |
| IV Explainable changes | Rules decide; the LLM drafts; the draft passes domain limits; the fixed transformation is the fallback; old value, new value and reason are stored/derived for each change; nothing takes effect here (state starts pending). |
| V Tests verify behaviour | Unit tests per limit, per fixed transformation (with rounding), per reason line, prompt-privacy test, persistence IT, controller tests for status codes and `404`. |
| VI Simplicity | One use case to read, one detail endpoint plus one per-client lookup, no new states logic (only the states #137 already lists, with transitions left to it), titles built on read instead of stored text. One justified exception below. |
| VII Design system | Built from Kinetiq tokens and shadcn components; strings in `en.json` and `pl.json`; checked in a real browser, light and dark, at 1280px. |

**Justified complexity**: the LLM calls run inside the weekly check's transaction (one use case call is one transaction, `TransactionConfig`). This is what makes "no partial plan change, retried next tick" true without a compensation step. Cost: a database connection is held for at most two LLM timeouts, once per client per week. `ponytail:` marked in code; move drafting out of the transaction if sweeps ever run many clients concurrently.

## Project Structure

### Documentation (this feature)

```text
specs/131-plan-change-content/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── api.md       # HTTP endpoints, response shape, error codes
│   └── ports.md     # domain ports and the prompt contract
└── tasks.md         # /speckit-tasks
```

### Source Code (repository root)

```text
backend/src/main/java/com/kinetiq/planchange/
├── domain/
│   ├── model/        Finding (+figures), Figure, PlanChange, PlanChangeId, PlanChangeState, TargetChange, Target,
│   │                 EntryRef, SessionMove, PlanChangeContent, PlanSnapshot (+ session/entry), PlanChangeText
│   ├── input/        ViewPlanChangeUseCase, DetectPlanChangesUseCase (unchanged signature)
│   ├── output/       PlanChangeRepository (real), PlanSource, PlanChangeDrafter, TrainingHistory (+finished days)
│   ├── rule/         Rules (add figures); limits: HighRpeLimits, StalledWeightLimits, LowCompletionLimits, PlanRuleLimits;
│   │                 fixed: HighRpeFallback, StalledWeightFallback, LowCompletionFallback
│   ├── service/      PlanChangeDraftingService, PlanChangePrompt, PlanChangeWording, PlanChangeViewService,
│   │                 PlanChangeDetectionService (hands over to drafting)
│   └── exception/    PlanChangeNotFoundException, PlanChangeDraftingFailedException, PlanChangeDrafterUnavailableException
└── infrastructure/
    ├── config/       PlanChangeConfig
    ├── input/        PlanChangeController, PlanChangeResponse, PlanChangeWebMapper, PlanChangeSweep (unchanged)
    └── output/       PlanChangeEntity (+ child entities), JpaPlanChangeRepository, PlanChangeRepositoryImpl,
                      PlanChangeMapper, JdbcPlanSource, OpenRouterPlanChangeDrafter, UnavailablePlanChangeDrafter
                      (LoggingPlanChangeRepository is deleted)
backend/src/main/resources/db/migration/V24__create_plan_changes.sql
backend/src/main/resources/db/dev-data/R__demo_plan_changes.sql
backend/src/test/java/com/kinetiq/planchange/...           unit tests, fakes, *IT, controller test

frontend/src/features/planchange/
├── api/plan-change.ts                      queries: by id, open one of a client
├── components/{plan-change-table,rule-chart}.tsx
└── pages/plan-change-page.tsx              route /trainer/plan-changes/:id
frontend/src/features/client/api/clients.ts + pages/client-page.tsx   lookup + card linking to the open plan change
frontend/src/components/plan-change-card.tsx
frontend/src/locales/{en,pl}.json
frontend/src/lib/api/schema.d.ts            regenerated
```

**Structure Decision**: web application; all backend work in the existing `planchange` feature; frontend detail page in a new `features/planchange` folder; the client page (feature `client`) owns the lookup query and shows a presentational card from `components/` (research D9).

## Commit plan (one commit per task, story pull request)

| Order | Task | Commit scope |
|---|---|---|
| 1 | #132 Plan change model and migration | model, `V24`, persistence adapter + IT, `Finding` figures and `WeekTraining` finished days, real `PlanChangeRepository` replacing the logging stand-in, demo data |
| 2 | #134 Limits per rule | `PlanSnapshot`, `PlanChangeContent`, limits + plan rules + unit tests |
| 3 | #135 Fixed transformation per rule | three fallbacks + unit tests (rounding) |
| 4 | #133 Drafter port and LLM adapter | `PlanChangePrompt`, `PlanChangeDrafter`, OpenRouter + unavailable adapters, `PlanChangeDraftingService`, wiring into detection; privacy test |
| 5 | #136 Reason line and title | `PlanChangeWording` (EN/PL), one test per rule |
| 6 | #289 Trainer plan change detail | read endpoints, OpenAPI + `schema.d.ts`, trainer detail page, client page link, i18n, tests, browser check |

Task #133 depends on #134 and #135 (it applies them); #132 first.
