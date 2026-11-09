# Implementation Plan: Rules detect that a plan change is due

**Branch**: `feat/125-plan-change-rules` | **Date**: 2026-11-09 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/125-plan-change-rules/spec.md`

## Summary

A scheduled job, running every 10 minutes, finds each client whose local time has passed Monday 03:00 and who has not been checked for that week. Per client, in its own transaction, it claims the week, skips the client if a plan change is open, and evaluates three deterministic rules over the completed weeks (low completion, high RPE, stalled weight). The first rule that fires, in that order and outside its cool-down, hands one due plan change to a port. The work is a new backend feature `planchange`; there is no endpoint and no frontend change. The plan change itself (table, model, states), drafting its content, accepting, declining and expiring it belong to other stories (#131/#132, #137/#138, #143/#144); until #132 a placeholder adapter only logs the due plan change.

## Technical Context

**Language/Version**: Java 25, Spring Boot 4.1 (virtual threads)

**Primary Dependencies**: Spring Scheduling (already enabled in `PlanConfig`), Spring JDBC `JdbcTemplate` (as `JdbcDraftedPlanDirectory`), Flyway. No new dependency.

**Storage**: PostgreSQL; one new table, `weekly_checks` (migration V22). Plan changes are not stored here (task #132).

**Testing**: JUnit 5 + AssertJ; plain unit tests with hand-written fakes and `Clock.fixed` for rules and service; `*IT` with Testcontainers for the JDBC adapter (history and weekly claim); `HexagonalArchitectureTest` unchanged and must pass.

**Target Platform**: the existing backend image (single instance today; the weekly claim also makes two instances safe).

**Project Type**: web-service, backend only.

**Performance Goals**: one tick reads all published-plan clients once; a client's check reads at most 3 weeks of its own logs. Hundreds of clients finish in seconds.

**Constraints**: no endpoint, no LLM, no personal data in logs, one transaction per client, deterministic thresholds on exact integer/decimal arithmetic.

**Scale/Scope**: up to a few thousand clients; each checked once a week.

## Constitution Check

*GATE: passed before Phase 0; re-checked after Phase 1 design: still passed.*

| Principle | Result |
|---|---|
| I. Hexagonal boundaries | Pass. `planchange.domain` holds rules, model and ports with no framework import. Reading other features' data goes through one driven port, `TrainingHistory`, implemented by a JDBC adapter inside `planchange.infrastructure.output` (precedent: `JdbcDraftedPlanDirectory`). `planchange` depends on no other feature, so no cycle. No new ArchUnit rule is needed. |
| II. One contract | Pass. No API change, so no OpenAPI or `schema.d.ts` change. |
| III. Secure by default | Pass. No endpoint is added (nothing to open to the public). Nothing goes to an LLM. Logs carry rule and counts only, never names or emails. Each check reads only that client's own plan and logs. |
| IV. Explainable, approved plan changes | Pass. Rules are deterministic, over logged data. The due plan change carries the rule, the window and the exercises, so the reason can be shown once #132 stores it. The plan itself is not changed here. |
| V. Tests verify behaviour | Pass. Tests target thresholds and edge cases (8.0 and just above, exactly 5%, exactly 70%, 2 sessions a week, priority, cool-down, once per week, time zones, daylight saving). |
| VI. Simplicity | Pass. No config for the thresholds, no rule plug-in mechanism beyond an ordered enum, no history of plan versions, no new dependency. |
| VII. One design system | Not applicable: no UI. |

Definition of Done items that do not apply: another person's resource answers `404` (no endpoint exists to ask), English and Polish strings (no user-facing text), `schema.d.ts` (no API change), browser check (no UI). The only new table, `weekly_checks`, is bookkeeping and needs no demo data.

## Project Structure

### Documentation (this feature)

```text
specs/125-plan-change-rules/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── ports.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
backend/src/main/java/com/kinetiq/planchange/
├── domain/
│   ├── model/           Rule (enum, priority order, window weeks), WeekTraining, LoggedSet, ScheduledClient,
│   │                    Finding, DuePlanChange
│   ├── rule/            LowCompletionRule, HighRpeRule, StalledWeightRule (pure functions over WeekTraining)
│   ├── input/           DetectPlanChangesUseCase (due, check)
│   ├── output/          TrainingHistory, PlanChangeRepository
│   └── service/         PlanChangeDetectionService
└── infrastructure/
    ├── config/          PlanChangeConfig
    ├── input/           PlanChangeSweep (@Scheduled)
    └── output/          JdbcTrainingHistory, LoggingPlanChangeRepository (placeholder until #132)

backend/src/main/resources/db/migration/
└── V22__create_weekly_checks.sql

backend/src/test/java/com/kinetiq/planchange/...   unit tests and *IT next to the code they cover
```

**Structure Decision**: one new feature package `planchange`, like every other feature. Task-to-commit order: #126 (run per time zone and weekly claim, with the rules list still empty), #127, #128 and #129 (one rule each), #130 (priority, open plan change and cool-down, through the plan change port).

## Complexity Tracking

One justified addition, no violation:

| Addition | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| `PlanChangeRepository` port with a logging placeholder adapter | The open-plan-change check, the cool-down and handing over a due plan change need it now; its real adapter is task #132 | Skipping the port would leave #130 untestable and tie this story to #132's schema |
