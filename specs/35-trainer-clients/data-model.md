# Data Model: Client list entry (#36)

Nothing is stored. The entry is read from existing tables.

## Domain (`com.kinetiq.client.domain.model`)

| Type | Fields | Notes |
|---|---|---|
| `ClientSummary` | `AccountId id`, `String name`, `Goal goal` (optional), `PlanState planState` | `Goal` is the quiz's goal; `getGoal()` returns `Optional` |
| `PlanState` | `NO_QUIZ`, `PLAN_TO_REVIEW` | grows with the plan stories |

`ClientDirectory` (`domain.output`): `List<ClientSummary> findClientsOf(AccountId trainer)`, ordered by name without regard to case, then by id.

`ListClientsUseCase` (`domain.input`): `List<ClientSummary> listClients(AccountId trainer)`.

## Read

```sql
select a.id, a.name, q.goal
from accounts a
left join quiz_submissions q on q.client_id = a.id
where a.trainer_id = :trainer
order by lower(a.name), a.id
```

`PlanState` is `PLAN_TO_REVIEW` when `q.goal` is not null, otherwise `NO_QUIZ`. Both tables hold one row per client at most (`uq_quiz_submissions_client`), so a client appears once.
