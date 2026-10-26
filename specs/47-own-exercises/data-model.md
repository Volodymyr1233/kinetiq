# Data model: Trainer adds and edits their own exercise

No new table. The `exercises` table (V3, spec #13) already has `owner_id` (nullable, foreign key to `accounts`), the English columns `name`, `equipment`, `cues` and the optional Polish ones `name_pl`, `equipment_pl`, `cues_pl`.

## Exercise (aggregate, `exercise.domain.model`)

| Field | Rule |
|---|---|
| `id` | `ExerciseId`, created in the application |
| `owner` | `AccountId`, absent for a starter exercise; set once at creation, never changed |
| `name` | `LocalizedText`; the base text is required, at most 100 characters, not blank |
| `category` | `Category`, one of nine |
| `equipment` | `LocalizedText`; at most 50 characters; empty allowed (shown as "Not set") |
| `measure` | `REPS` or `SECONDS`; chosen at creation, not changed by an update |
| `defaultSets` | 1 to 20 |
| `defaultAmount` | 1 to 3600 (repetitions or seconds) |
| `restSeconds` | 0 to 600 |
| `cues` | `LocalizedText`; at most 500 characters; empty allowed |

An own exercise keeps its text only in the base fields; the Polish fields are empty. New behaviour on the aggregate:

- `isStarter()`: no owner.
- `isOwnedBy(AccountId)`: has an owner and it is that account.
- `edit(name, category, equipment, sets, amount, rest, cues)`: returns the exercise with these values and the same id, owner and measure; revalidates.

The cap (a trainer owns at most 200 exercises, `MAX_OWN_PER_TRAINER`) is also the service's, counted over the exercises it already loads. The clash rule (not a field rule) is the service's: no other exercise the trainer sees has an equal trimmed, case-folded English or Polish name.

## Schema change

`V9__add_exercise_name_index.sql`:

```sql
create unique index ux_exercises_owner_name on exercises (owner_id, lower(name)) where owner_id is not null;
```

It exists only so two simultaneous creates leave one row. Starter rows (owner null) are outside it. Existing data cannot violate it: no row has an owner yet.

## Ownership view

| Exercise | Visible to | Editable by |
|---|---|---|
| Starter (`owner_id` null) | every trainer | nobody |
| Own (`owner_id` = trainer) | that trainer | that trainer |

## Repository port

| Method | Result |
|---|---|
| `findVisibleTo(AccountId trainer)` | starter exercises and the trainer's own (replaces `findStarter`) |
| `findById(ExerciseId)` | `Optional<Exercise>`, any owner; the service decides |
| `save(Exercise)` | inserts or updates; throws `ExerciseNameTakenException` when the unique index refuses |

## Demo data (local profile only)

| Account | Own exercises |
|---|---|
| `demo.trainer@example.com` | three, one measured in seconds, one with no equipment and no cues |
| `demo.trainer2@example.com` (new, confirmed) | one |
