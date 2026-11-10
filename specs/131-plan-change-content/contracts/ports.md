# Contract: domain ports and prompt

## Driving ports (`planchange.domain.input`)

| Port | Method | Meaning |
|---|---|---|
| `DetectPlanChangesUseCase` | `due()`, `check(client)` | unchanged signatures. `check` now also builds the content and stores the plan change in the same transaction. |
| `ViewPlanChangeUseCase` | `PlanChangeView get(AccountId trainer, PlanChangeId id)` | one plan change of the trainer's own client, with title and reason in the trainer's language; `PlanChangeNotFoundException` otherwise |
| `ViewPlanChangeUseCase` | `PlanChangeSummary openOf(AccountId trainer, AccountId client)` | the open plan change of an own client; `ClientNotFoundException` for another trainer's client, `PlanChangeNotFoundException` when none is open |

Ownership uses `ViewClientsUseCase.getClient(trainer, client)` of the `client` feature (answers not found for another trainer's client), as `PlanManagementService` does.

## Driven ports (`planchange.domain.output`)

| Port | Methods | Notes |
|---|---|---|
| `PlanChangeRepository` | `hasOpen`, `lastEndedAt` (real now), `PlanChange save(PlanChange)`, `Optional<PlanChange> find(PlanChangeId)`, `Optional<PlanChange> findOpen(AccountId client)` | replaces `open(DuePlanChange)`; `LoggingPlanChangeRepository` is deleted |
| `PlanSource` | `Optional<PlanSnapshot> currentPlan(AccountId client)`; `Map<ExerciseId,ExerciseLabel> exercises(Set<ExerciseId>, Language)` (library name in the language, and measure) | JDBC read model over `plans`, `plan_sessions`, `plan_session_entries`, `exercises` |
| `PlanChangeDrafter` | `ProposedContent draft(PlanChangePromptMessages messages)` | one call is one attempt; throws `PlanChangeDraftingFailedException` (no usable answer) or `PlanChangeDrafterUnavailableException` (no service configured) |
| `TrainingHistory` | adds `finishedDays` to `WeekTraining` | no new method |

## Prompt contract

Sent only through `PlanChangeDrafter`; built in the domain so a unit test can prove what it holds.

```text
RULE: HIGH_RPE | STALLED_WEIGHT | LOW_COMPLETION, with its numbers and limits (text)
WEEKS: one line per week: mean RPE; sessions planned/finished; best load per concerned entry
PLAN: s1 MONDAY
        e1 category=LEGS measure=REPS sets=3 amount=8 loadKg=100.0 rpe=8 restSeconds=120 concerned=true
      s2 WEDNESDAY ...
ANSWER (JSON schema): {
  changes:     [{ entry: "s1e1", sets?, amount?, loadKg? }],
  removeSession: "s3" | null,
  moves:       [{ entry: "s3e2", toSession: "s1" }]
}
```

- Contains no name, email, id (account, plan, exercise, session), date of birth, body weight, note or any text a person wrote; exercise and session names are not sent at all. Keys `sN` and `eN` mean something only inside the prompt.
- The system message fixes the rule's limits in words. A second attempt adds one sentence per violation code of the first answer, as `PlanPromptMessages.rejections` does.
- Test: for a client whose name, email, ids and trainer-written names are known, none of these strings occurs in the system or user message.

## Violation codes (limits)

`limit.entry-not-concerned`, `limit.target-not-allowed`, `limit.volume-out-of-range`, `limit.load-out-of-range`, `limit.load-not-half-kg`, `limit.amount-drop-too-large`, `limit.session-removal-count`, `limit.too-few-sessions`, `limit.too-many-moves`, `limit.move-not-from-removed`, `limit.too-many-entries`, `limit.nothing-changed`, `limit.plan-rule`, `limit.unknown-key`. Internal: used for the second attempt's sentences and tests; never in an API response.
