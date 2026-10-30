# Data Model: Client answers the quiz

Nothing in this story is stored on the server. The draft lives in the browser tab (research R3). Story #54 (task #55) stores the submitted answers using the codes and rules below.

## Option codes

| Answer | Codes, in screen order |
|---|---|
| Goal | `BUILD_MUSCLE`, `GET_STRONGER`, `LOSE_FAT`, `MOVE_BETTER`, `GENERAL_FITNESS` |
| Experience | `LESS_THAN_3_MONTHS`, `MONTHS_3_TO_6`, `MONTHS_6_TO_12`, `YEARS_1_TO_2`, `YEARS_2_TO_5`, `YEARS_5_PLUS` |
| Level | `BEGINNER`, `INTERMEDIATE`, `ADVANCED`, `ATHLETE` |
| Likes | `WEIGHTS`, `CARDIO`, `HIIT`, `YOGA_PILATES`, `BODYWEIGHT` |

Labels and descriptions come from the locale files by code (`quiz.goal.options.BUILD_MUSCLE.title`, and so on).

## Quiz draft (browser tab)

Stored as JSON under `sessionStorage["kinetiq.quiz"]`.

| Field | Type | Empty value | Valid when |
|---|---|---|---|
| `answers.goal` | goal code | absent | one of the goal codes |
| `answers.experience` | experience code | absent | one of the experience codes |
| `answers.level` | level code | absent | one of the level codes |
| `answers.age` | text as typed | `""` | trimmed, digits only, 16–99 |
| `answers.height` | text as typed | `""` | trimmed, digits only, 120–230 (cm) |
| `answers.weight` | text as typed | `""` | trimmed, digits with at most one decimal after `.` or `,`, 30–250 (kg) |
| `answers.likes` | list of like codes | `[]` | at least one, each code once |
| `answers.days` | whole number | `3` | 2–6 |
| `answers.notes` | text as typed | `""` | at most 500 UTF-16 units; always valid, since notes are optional |
| `reached` | whole number | `1` | 1–7: the furthest step reached with Continue |

**Loading**: Every field is parsed on its own. A field that fails falls back to its empty value: an unknown code, a list with an unknown code, a non-number, a value out of range, or a typed value that is not valid yet (spec, Edge Cases). A broken or missing entry gives an empty draft.

**Steps and validity**:

| Step | Valid when |
|---|---|
| 1 Goal | `goal` valid |
| 2 Experience | `experience` valid |
| 3 Level | `level` valid |
| 4 About you | `age`, `height` and `weight` all valid |
| 5 Likes | `likes` valid |
| 6 Days per week | `days` valid (true from the start) |
| 7 Notes | always |

**Which step may open** (research R4): the requested step, but no further than `reached`, and no further than the first earlier step that is not valid.

**Lifecycle**: empty → filled step by step → removed when the quiz is submitted (task #57). The tab's storage, the draft with it, is also emptied on signing in, joining and signing out (tasks #24 and #33), and when an account opens a tab that another account used (task #226). The draft is never sent anywhere before submitting.

## Submitted answers (task #55, for reference)

What the draft becomes when story #54 submits it. Defined here because FR-008 makes these rules the single definition.

| Field | Type | Rule |
|---|---|---|
| goal, experience, level | code | one of the codes above |
| age | integer | 16–99 |
| heightCm | integer | 120–230 |
| weightKg | decimal, one place | 30.0–250.0 |
| likes | set of codes | 1–5 distinct |
| daysPerWeek | integer | 2–6 |
| notes | text or none | trimmed; empty after trimming means none; at most 500 UTF-16 units |
