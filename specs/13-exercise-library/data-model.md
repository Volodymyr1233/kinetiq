# Data Model: Trainer browses the exercise library

## Exercise (aggregate, `exercise.domain.model`)

| Field | Type | Rule |
|---|---|---|
| `id` | `ExerciseId` (UUID) | created in the application; starter exercises have fixed ids (R6) |
| `owner` | `Optional<AccountId>` | none = starter exercise. Nothing in this story sets it |
| `name` | `LocalizedText` | English required, not blank, at most 100 characters; Polish optional, at most 100 |
| `category` | `Category` | `CHEST`, `BACK`, `LEGS`, `SHOULDERS`, `ARMS`, `CORE`, `FULL_BODY`, `CARDIO`, `MOBILITY` |
| `equipment` | `LocalizedText` | English required, at most 50; Polish optional |
| `measure` | `Measure` | `REPS` or `SECONDS`; says what `defaultAmount` counts |
| `defaultSets` | int | at least 1 |
| `defaultAmount` | int | at least 1; repetitions, or seconds when the measure is `SECONDS` |
| `restSeconds` | int | at least 0 |
| `cues` | `LocalizedText` | English may be empty, at most 500; Polish optional |

Invariants are checked in the constructor (task #14: name required, defaults at least 1). No setters; nothing changes an exercise in this story.

Behaviour:
- `matches(text)`: true when the trimmed text, lower-cased, is part of the lower-cased English or Polish name; blank text matches everything.

## LocalizedText (value, `exercise.domain.model`)

`record LocalizedText(String en, String pl)`; `pl` is an empty string when there is no Polish text. `in(Locale)` returns `pl` when the language is `pl` and `pl` is not blank, else `en`. The mapper turns a null column into the empty string and back.

## Listing (result, `exercise.domain.input`)

`ExerciseListing(List<Entry> entries, int total)`; `Entry(Exercise exercise, int plansUsing)`. `total` counts the whole library regardless of filters (FR-016); `plansUsing` is 0 until story #68 (R5).

## Table `exercises` (migration V3)

| Column | Type | Constraint |
|---|---|---|
| `id` | uuid | `pk_exercises` |
| `owner_id` | uuid null | `fk_exercises_owner` → `accounts(id)`; index `ix_exercises_owner_id` |
| `name` | varchar(100) not null | `ck_exercises_name_not_blank` (`trim(name) <> ''`) |
| `name_pl` | varchar(100) null | |
| `category` | varchar(16) not null | `ck_exercises_category` (the nine values) |
| `equipment` | varchar(50) not null | |
| `equipment_pl` | varchar(50) null | |
| `measure` | varchar(16) not null | `ck_exercises_measure` (`REPS`, `SECONDS`) |
| `default_sets` | integer not null | `ck_exercises_default_sets` (`>= 1`) |
| `default_amount` | integer not null | `ck_exercises_default_amount` (`>= 1`) |
| `rest_seconds` | integer not null | `ck_exercises_rest_seconds` (`>= 0`) |
| `cues` | varchar(500) not null | |
| `cues_pl` | varchar(500) null | |
| `created_at`, `updated_at`, `version` | as in `accounts` | `BaseEntity` |

## Starter set (migration V4)

40 rows, `owner_id` null, fixed ids `00000000-0000-4000-9100-0000000000NN`, created with `now()` and version 0.

| Category | Count | Examples |
|---|---|---|
| Chest | 5 | Bench press / Wyciskanie sztangi leżąc |
| Back | 5 | Lat pulldown / Ściąganie drążka wyciągu górnego; Dead hang (timed) |
| Legs | 6 | Back squat / Przysiad ze sztangą; Wall sit (timed) |
| Shoulders | 4 | Overhead press / Wyciskanie nad głowę |
| Arms | 5 | Biceps curl / Uginanie ramion z hantlami |
| Core | 5 | Plank / Deska (timed); Side plank (timed) |
| Full body | 4 | Deadlift / Martwy ciąg; Farmer's carry (timed) |
| Cardio | 3 | Rowing intervals / Interwały na wioślarzu |
| Mobility | 3 | Hip 90/90 / Mobilizacja bioder 90/90 |

At least three are `SECONDS`. The full list with all fields is written in task #15.

## Relationships

`exercises.owner_id` → `accounts.id` (optional). Plans (story #68) will reference `exercises.id`; fixed starter ids keep those references valid across environments.
