# Data Model: Client sees today's session

Nothing is stored or migrated. Today is derived.

## Today view

What `ViewOwnPlanUseCase.today(client)` returns.

| Field | Source |
|---|---|
| `date` | the clock's instant in the client's time zone, as a date |
| `state` | `SESSION` when the plan has a session on `date`'s weekday; `REST` when it has none; `FINISHED` when the current plan week is above the plan's weeks |
| `session` | the plan's session on that weekday, only for `SESSION` |
| `library` | the client's trainer's exercise library, only for `SESSION`, for names and measures |

Rules:

- A plan has at most one session per weekday (plan rule, spec 68), so `SESSION` holds one.
- `FINISHED` wins over a session on that weekday: after the last week nothing is shown.
- The last Sunday of the last week is a normal day (`currentWeek` is still at most `weeks`).
- A draft and a missing plan are `PlanNotFoundException` (`plan.not-found`), not a state.

State transitions are by date only:

```
published plan, day with a session ──▶ SESSION
published plan, day without one    ──▶ REST
current week > weeks               ──▶ FINISHED   (back to SESSION/REST if the trainer lengthens the plan)
```
