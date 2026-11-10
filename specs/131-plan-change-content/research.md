# Research: Plan change content is drafted and checked

Decisions only; each was checked against the existing code (`planchange`, `plan`) and the constitution.

## D1 Where drafting runs

- **Decision**: inside `DetectPlanChangesUseCase.check`, in its transaction, after a rule has been chosen.
- **Rationale**: the weekly claim, the plan change and its figures commit together; a failure (database, bug) rolls the claim back and the next 10-minute tick retries. Because the fixed transformation always succeeds, a rule that fires always ends in a plan change.
- **Alternatives**: draft in a separate job like `PlanDraftingJob` (needs a "drafting" state and a recovery sweep: more moving parts); claim first and draft after (a failure after the claim silently skips the week).

## D2 How rule numbers reach the plan change

- **Decision**: `Finding` gains `figures`: a list of `Figure(kind, exerciseId?, weekStart, value)`. Kinds: `MEAN_RPE` (week mean over all sets), `BEST_LOAD_KG` (per stalled exercise and week), `SESSIONS_PLANNED`, `SESSIONS_FINISHED` (per week). Each rule fills what it measured. Stored in a child table.
- **Rationale**: the reason line ("RPE above 8 for 2 weeks", "rose 2.1% in 3 weeks", "50% finished") and the chart both derive from these; per-week rows are one value per row (3NF) and need no per-language columns.
- **Alternatives**: free-text reason stored (not translatable, not checkable); JSON column (breaks the 3NF rule of the backend guide).

## D3 What the fixed fallback for low completion needs

- **Decision**: `WeekTraining` gains `finishedDays` (the weekdays on which a session was finished). The least-finished session over the window is the plan session whose weekday has the fewest finished logs; ties go to the latest weekday.
- **Rationale**: sessions are unique per weekday in a plan and `JdbcTrainingHistory` already matches logs to the plan by date, so no new join.
- **Alternatives**: join logs to plan sessions by id (the log keeps a copy of the session, not a reference).

## D4 Addressing an entry

- **Decision**: an entry is addressed by `EntryRef(day, position)`: sessions are unique per weekday (`uq_plan_sessions_plan_day`) and entries by position within a session, so the same exercise twice in a session stays unambiguous. A rule that names an exercise concerns all its entries.
- **Alternatives**: exercise id (ambiguous for repeats); entry surrogate id (the schema has none).

## D5 Target model

- **Decision**: `TargetChange(entry, target, oldValue, newValue)` with `Target` in `SETS`, `AMOUNT`, `LOAD_KG`, `RPE`, `REST_SECONDS`; values are `numeric(6,1)`. Session removal is one nullable column pair on the plan change (`removed_day`, `removed_name`); moves are rows `(from_day, from_position, to_day)`. The new position of a moved entry is the end of the target session.
- **Rationale**: matches the story ("old and new value per target") and keeps limits checks pure over a small structure. Session and exercise names are copied onto target rows (`session_name`) because the plan may be edited before the plan change is decided: the screen must show the state the draft was made from. Documented denormalisation, with a comment in the migration.

## D6 The LLM port and what the prompt may hold

- **Decision**: domain builds the prompt (`PlanChangePrompt`, like `PlanPrompt`) and the port `PlanChangeDrafter.draft(PlanChangePromptMessages)` returns a raw `ProposedContent` keyed by prompt keys; the domain maps keys back and checks them. The prompt holds the rule and its numbers, the plan as `s1..sn` sessions (weekday) with `e1..en` entries (category, measure, sets, amount, load, RPE, rest), the entries concerned by the rule, and the weekly aggregates. It holds **no exercise or session names** (they can be written by a trainer), no ids, no dates of birth, no weights of a person.
- **Rationale**: constitution III; the model does not need names to choose which targets move within limits.
- **Adapter**: Spring AI `ChatModel` like `OpenRouterPlanGenerator`, same key property (`kinetiq.plan-drafting.api-key`), a JSON schema answer, nothing from prompt or answer logged, only the exception class. `UnavailablePlanChangeDrafter` stands in when no key is set; every attempt then counts as failed and the fixed transformation is used (SC-002).
- **Alternatives**: reuse `PlanGenerator` (its answer is a whole plan; an LLM rewriting a whole plan is a bigger blast radius than a list of targets).

## D7 Attempt policy

- **Decision**: up to 2 attempts, no pause between them (the second prompt adds the rejection sentences, as `PlanPromptMessages.rejections` does); then the fixed transformation. A draft that changes nothing is a violation.
- **Rationale**: ADR 0002 ("breaks its limits or the plan rules twice").

## D8 Limits arithmetic

- **Decision**: volume of an entry = `sets × amount`. High RPE: per entry of a concerned exercise, new volume must be within `[0.85, 0.95]` of the old, measured on integers with exact rational comparison (`new*100 <= old*95` and `new*100 >= old*85`). Stalled weight: new load within `[+2.5%, +15%]` of the old and a multiple of 0.5 kg, judged on the new (rounded) value; reps `old-2 ≤ new ≤ old`, min 1. Percentages use `BigDecimal` with exact comparison, no doubles. Low completion: structural counts only.
- **Fixed transformations**: high RPE `amount' = max(1, min(amount-1, round_half_up(amount*0.9)))`; stalled `load' = ceil_to_0.5(load*1.05)`; both exempt from percentage limits (clarification), subject to plan rules (`ExerciseEntry` bounds, load at most 999.9). Low completion removes the session finished least often (ties: latest weekday) and moves nothing.
- **Plan rules used**: `Plan.MAX_SESSIONS`, `ExerciseEntry` bounds, 2 sessions minimum, one session per weekday, ≤ 8 entries per session (new for plan changes only; the plan editor has no such limit and is untouched).

## D9 Reaching the detail from the client page

- **Decision**: backend `GET /api/v1/clients/{id}/plan-change` returns the open plan change of that client (or `404 plan-change.not-found`); the client page shows a small card with title, reason, effective week and a link to `/trainer/plan-changes/:id`.
- **Frontend boundary**: features do not import each other, so the client feature owns this query (`features/client/api`) and renders a presentational card from `components/`; the detail page, its query and route belong to `features/planchange`.
- **Alternatives**: embed the plan change in the client detail response (changes an existing contract used by other screens).

## D10 Title and reason

- **Decision**: built on read by `PlanChangeWording` from rule, figures, targets and exercise names, in the language of the person reading (`Language` of the account), with a small plural helper for Polish ("2 tygodnie", "5 ćwiczeń"). Wording never uses a pronoun. Nothing is stored.
- **Rationale**: translatable, always consistent with the numbers, and #137 can post the same text into the conversation in the recipient's language.

## D11 States and ending

- **Decision**: states `PENDING, ACCEPTED_BY_ONE, APPLIED, DECLINED, EXPIRED` are created now (the list is in story #137), with `ended_at` set by later stories; this story only creates `PENDING`. `hasOpen` = state in `PENDING, ACCEPTED_BY_ONE`; `lastEndedAt` = max `ended_at`.
- **Alternatives**: only `PENDING` now and widen later (a migration to change a check constraint for no gain).

## D12 Chart and "Same weeks"

- **Decision**: the chart uses the stored figures (RPE per week with the 8 line; best load per stalled exercise with the +5% line; share finished with the 70% line) through the existing shadcn `chart`. The mockup card "Same weeks" (readiness average, recovery days) is left out: it needs readiness data no story asks for here; the sessions and RPE it shows are in the figures and the table.
