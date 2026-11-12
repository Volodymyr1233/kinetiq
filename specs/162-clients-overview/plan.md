# Implementation Plan: Trainer sees the overview of all clients

**Branch**: `feat/162-clients-overview` | **Date**: 2026-11-12 | **Spec**: `specs/162-clients-overview/spec.md`

## Summary

A new backend feature `overview` derives, for one trainer, a row per client with its client status, the four tiles and the "Needs attention" alerts, and answers them in one `GET /api/v1/clients/overview`. The status rules are a pure domain calculation over a client's plan dates, day logs and check-ins, read for all of the trainer's clients by one read model (four queries). The frontend gets a feature `overview` with the overview screen at `/trainer/clients` and All clients at `/trainer/clients/all`, both on the same query; the old list endpoint `GET /api/v1/clients` loses its only caller and is removed.

## Technical Context

- Backend: Java 25, Spring Boot 4, `JdbcTemplate` read model (as `JdbcClientDirectory`, `JdbcTrainingLog`), no migration.
- Frontend: React 19, TanStack Query, shadcn `Card`, `Alert`, `Badge`, `Table`, `ToggleGroup`, `Progress`, `Skeleton`, `Empty`.
- Scale: a trainer's clients (tens), so one answer, no paging; four queries whatever the number of clients.

## Constitution Check

| Principle | How |
|---|---|
| I Hexagonal | `overview` depends on `client`, `plan`, `planchange` (input ports) and `readiness` (model only); nothing depends on it, so no cycle. Its read model is private to it and reads other features' tables by SQL, as `JdbcTrainingLog` does. |
| II One contract | `schema.d.ts` regenerated in the same pull request; errors only by `code`. |
| III Secure | Under `/api/v1/clients/**` (role TRAINER); every query is bounded by `accounts.trainer_id = ?`; clients and plan changes come from use cases that already check ownership. No LLM. |
| IV Plan changes | Nothing changes a plan; the status is a rule, the plan change texts are the stored ones. |
| V Tests | Status thresholds and window edges as unit tests; the read model against PostgreSQL; the service with fakes; the controller with roles; screens by behaviour. |
| VI Simplicity | One endpoint for both screens; no new dependency; no stored status. Removing the unused list endpoint. |
| VII Design system | Status badges, alert tones, metric cards as in sections 5 and 6.10; an Alert `warning` variant the design system lists and a `low` variant for the readiness scale. |

## Design

### Domain (`com.kinetiq.overview.domain`)

- `model.ClientStatus` (enum, in sort order: `AT_RISK`, `WATCH`, `PLAN_TO_REVIEW`, `NO_PLAN`, `ON_TRACK`), `model.StatusReason` (`LOW_COMPLETION`, `HIGH_RPE`), `model.Completion(done, due)`, `model.Verdict(status, reasons)` with `of(PlanState, boolean running, Completion, BigDecimal averageRpe)` (FR-002, FR-005, FR-006).
- `model.PlanDates(firstWeek, publishedOn, weeks, days)`: `lastDay`, `week(today)`, `finished(today)`, `planned(date)`.
- `model.ClientTraining(plan, days, lastLog, checkIns)`: `completion(today)` (FR-003), `rpe(from, to)` as an `RpeTotal(sum, sets)` whose `mean()` is one decimal half up (FR-004), `week(today)` sessions planned and done.
- `output.TrainingRecords.forTrainer(trainer, since)`: a `ClientTraining` per client of the trainer.
- `input.ViewOverviewUseCase.overview(trainer)` → `input.Overview(tiles, alerts, rows)`; `service.OverviewService` builds it with `ViewClientsUseCase`, `ListDraftedPlansUseCase`, `ViewPlanChangeUseCase.approvals`, `TrainingRecords` and a `Clock`.

### API

`GET /api/v1/clients/overview` → `OverviewResponse`:

```
activeClients, totalClients, sessionsDone, sessionsPlanned, averageRpe?, averageRpeChange?,
draftsWaiting, planChangesWaiting,
alerts: [{ kind, clientId, planChangeId?, planChangeTitle?, waitingFor?, draft?, daysWithoutLog?, lastSessionName?, since? }],
clients: [{ id, name, goal?, planState, planWeek?, planWeeks?, planEndsOn?, status, reasons, sessionsDone?, sessionsDue?,
            averageRpe?, lastLoggedAt?, readiness?, readinessBand?, lighterTargets }]
```

`kind`: `AT_RISK`, `WATCH`, `NO_LOG`, `PLAN_CHANGE`, `DRAFT`, `PLAN_ENDING`, `RECOVERY_DAY`; `waitingFor`: `TRAINER`, `CLIENT`, `BOTH`. A finished plan has `planWeeks` and `planEndsOn` without `planWeek`. `403` for a client, `401` for a visitor.

### Frontend (`src/features/overview`)

- `api/overview.ts`: `useOverview()` under the key `['clients', 'overview']`, so every invalidation of `['clients']` refreshes it.
- `components/status-badge.tsx`, `client-table.tsx` (shared columns, optional sorting), `overview-tiles.tsx`, `attention-list.tsx`.
- `pages/overview-page.tsx` (`/trainer/clients`), `pages/all-clients-page.tsx` (`/trainer/clients/all`, replaces `features/client/pages/clients-page.tsx`).
- `lib/dates.ts` gains a relative time for "Last log".

## Phases

1. #163: status domain and its tests.
2. #164: read model, service, endpoint, demo data, regenerated types.
3. #165: overview screen at `/trainer/clients`; the old table moves to `/trainer/clients/all` unchanged.
4. #166: All clients with the new columns, sorting and filters on the overview query; the list endpoint, now without a caller, removed.
