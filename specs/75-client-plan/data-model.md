# Data Model: Client sees their plan

One column is added to an existing table. Everything else the client reads is derived.

## Stored

### `plans` (existing, spec 68)

| Column | Change | Meaning |
|---|---|---|
| `client_seen_at` | **new**, `timestamptz`, nullable | When the client first saw their published plan (opened the Plan tab). `null` until then. Set once; never reset. |

Migration `V15__add_plan_seen.sql`:

```sql
alter table plans
    add column client_seen_at timestamptz,
    -- a draft is not visible to the client, so it cannot have been seen
    add constraint ck_plans_seen_published check (client_seen_at is null or first_week is not null);
```

Existing rows get `null`: every published plan counts as not yet seen, which is true, because no client could see one before this story.

Rules:

- Set by one statement, `update plans set client_seen_at = ? where client_id = ? and first_week is not null and client_seen_at is null`. It is the only writer.
- Saving a plan (`PlanRepositoryImpl.replace`) copies goal, weeks, first week and sessions only, so it does not touch the column; publishing a draft leaves it `null`.
- The update does not go through the entity, so `version` and `updated_at` stay as they are: the trainer's editor never sees the plan as changed because the client looked at it.

## Derived (not stored)

### Own plan view

What `ViewOwnPlanUseCase.view` returns for the signed-in client.

| Field | Source |
|---|---|
| plan | the client's published plan; goal, weeks, sessions with entries |
| currentWeek | `Plan.currentWeek(today)`, `today` in the client's time zone (at least 1) |
| finished | `currentWeek > weeks` |
| week | the plan week asked for, default `currentWeek`; absent when finished; holds its number and the date of its Monday (`firstWeek + (number - 1) * 7`) |
| seen | `client_seen_at is not null` |
| library | the client's trainer's exercise library, for names and measures |

### Plan state transitions (client's view)

```
no plan / draft ──(trainer publishes)──▶ published, unseen
published, unseen ──(client opens Plan tab)──▶ published, seen
published, seen ──(trainer saves)──▶ published, seen      # never back to unseen
published ──(current week > weeks)──▶ finished            # derived, reversible by lengthening
```

## Validation

- `week`, when given, is an integer from 1 to `weeks`; anything else (and any `week` for a finished plan) is refused (`plan.week-out-of-range`).
- A client can only read the plan whose `client_id` is their own account id from the sign-in.
