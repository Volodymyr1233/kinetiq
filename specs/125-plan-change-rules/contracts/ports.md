# Contracts: Rules detect that a plan change is due

The story exposes no HTTP endpoint and changes no OpenAPI document, so there is no frontend contract. The contracts are the ports of the new `planchange` feature.

## Driving port: `DetectPlanChangesUseCase` (`domain.input`)

| Method | Meaning |
|---|---|
| `List<AccountId> due()` | Clients with a published plan whose local check week (local time minus 3 hours, Monday on or before) is after the last week they were checked for. One query. |
| `void check(AccountId client)` | One transaction: load the client's schedule data (stop if there is no published plan); claim the week; stop if it was already claimed; stop if the client has an open plan change; evaluate the rules over the eligible completed weeks; hand at most one due plan change to the plan change port. |

The sweep (`PlanChangeSweep`, `infrastructure.input`) calls `due()` every 10 minutes and `check` per client, catching and logging a failure (class name only) so one client cannot hold up the others, like `PlanDraftingSweep`.

## Driven port: `TrainingHistory` (`domain.output`)

| Method | Meaning |
|---|---|
| `List<ScheduledClient> clientsWithPublishedPlan()` | Client, time zone, first plan week, plan length and last checked week, for every client with a published plan. |
| `Optional<ScheduledClient> scheduledClient(AccountId client)` | The same data for one client; empty when the client has no published plan. |
| `List<WeekTraining> weeks(AccountId client, LocalDate firstMonday, int count)` | The client's own consecutive weeks, oldest first, with planned and finished sessions and every logged set. |
| `boolean claimWeek(AccountId client, LocalDate week, Instant now)` | Inserts the `weekly_checks` row; `false` when it exists. |

## Driven port: `PlanChangeRepository` (`domain.output`)

The seam to task #132. It is defined here because the selection logic of #130 needs it; its real adapter is built there.

| Method | Meaning |
|---|---|
| `boolean hasOpen(AccountId client)` | Whether the client has a plan change that has not ended. |
| `Optional<Instant> lastEndedAt(AccountId client)` | When the client's latest plan change ended; empty if none ended. A plan change is open until it ends, so this is never a moment still ahead: an accepted plan change stays open, and `hasOpen` stays true, until the start of its effective week. |
| `void open(DuePlanChange due)` | Stores the due plan change as an open plan change. |

Until #132, the only adapter is `LoggingPlanChangeRepository` (precedent: `UnavailablePlanGenerator`): `hasOpen` is `false`, `lastEndedAt` is empty and `open` logs the rule name, creating nothing. With it, a rule that still holds fires again each week and is only logged. Tests use a hand-written in-memory fake.

## Errors

No response is produced. Failures are logged with the exception class name only and retried by the next tick, because the claim rolls back with the transaction.
