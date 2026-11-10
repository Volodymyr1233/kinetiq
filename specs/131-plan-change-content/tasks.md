---

description: "Tasks for Plan change content is drafted and checked (story #131)"
---

# Tasks: Plan change content is drafted and checked

**Input**: Design documents from `specs/131-plan-change-content/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api.md, contracts/ports.md, quickstart.md

**Tests**: Required by the constitution (V) and the CLAUDE.md files: plain unit tests with hand-written fakes for the domain, `*IT` with Testcontainers for persistence adapters, `@WebMvcTest` for the controller, Vitest for the frontend. Names follow `shouldX_whenY`. No test that only exercises getters or constructors.

**Organization**: One phase per task issue, which is also one commit of the story pull request, in commit order. Branch: `feat/131-plan-change-content`. Every commit compiles and passes the quality gate of the sides it touches.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: US1 = #132, US3 = #134, US4 = #135, US2 = #133, US5 = #136, US6 = #289 (numbers follow the user stories of `spec.md`)
- Paths are under `backend/` or `frontend/` as written; backend main root `src/main/java/com/kinetiq/planchange/` (`PC/`), tests mirror it under `src/test/java/com/kinetiq/planchange/` (`PCT/`)

## Commit plan (commit per task; merge with rebase or a merge commit so each stays on `master`)

| Order | Commit | Message |
|---|---|---|
| 0 | the spec folder | `docs: add spec for plan change content (#131)` |
| 1 | Phase 1 | `feat(backend): store plan changes (#132)` |
| 2 | Phase 2 | `feat(backend): check plan change drafts against rule limits (#134)` |
| 3 | Phase 3 | `feat(backend): add fixed transformation per rule (#135)` |
| 4 | Phase 4 | `feat(backend): draft plan changes with an LLM (#133)` |
| 5 | Phase 5 | `feat(backend): build plan change title and reason (#136)` |
| 6 | Phase 6 | `feat: add trainer plan change detail (#289)` |

No `Co-Authored-By` or other attribution footer (project `CLAUDE.md`). The pull request closes #131, #132, #133, #134, #135, #136 and #289.

## Phase 0: Spec commit

- [ ] T001 Commit `specs/131-plan-change-content/` (spec, plan, research, data model, contracts, quickstart, tasks, checklist) as the first commit. Do not commit `.specify/feature.json`.

---

## Phase 1: Plan change model and migration (#132) [US1]

**Goal**: a stored plan change exists, with the rule numbers and old/new values; the repository port is real; detection still ends by logging because no content is built yet.

**Independent test**: persistence adapter round trip and the one-open-per-client constraint.

- [ ] T002 [US1] Re-check `master` and open pull requests for migrations (`gh pr list --json number,files`), then write `src/main/resources/db/migration/V24__create_plan_changes.sql` (renumber if taken) with the tables of `data-model.md`: `plan_changes`, `plan_change_targets`, `plan_change_moves`, `plan_change_figures`, `plan_change_exercises`, every constraint named. Keep verbatim: `state` in (`PENDING`,`ACCEPTED_BY_ONE`,`APPLIED`,`DECLINED`,`EXPIRED`); `rule` in (`LOW_COMPLETION`,`HIGH_RPE`,`STALLED_WEIGHT`); `target` in (`SETS`,`AMOUNT`,`LOAD_KG`,`RPE`,`REST_SECONDS`); `kind` in (`MEAN_RPE`,`BEST_LOAD_KG`,`SESSIONS_PLANNED`,`SESSIONS_FINISHED`); `effective_week` and `week_start` are Mondays (`extract(isodow from …) = 1`); `old_value`, `new_value` `numeric(6,1)` and `new_value <> old_value`; `value` `numeric(7,3)`; `removed_day` and `removed_name` both null or both set; unique partial index `uq_plan_changes_one_open_per_client` on `(client_id)` where `state in ('PENDING','ACCEPTED_BY_ONE')`; index on `(client_id, ended_at)`. Comment the `exercise_id`/`session_name` copy on target rows as a deliberate denormalisation.
- [ ] T003 [P] [US1] Add the term **Plan change content** to `CONTEXT.md` (Changing a plan: "The proposed changes of a plan change: targets, removed session and moves." Avoid: Draft, which is an unpublished plan). Domain model in `PC/domain/model/`: `PlanChangeId`, `PlanChangeState`, `Target`, `EntryRef(DayOfWeek day, int position)`, `TargetChange`, `RemovedSession`, `SessionMove`, `FigureKind`, `Figure`, `PlanChangeContent` (invariants: at least one change or a removed session; moves only with a removed session; at most 2 moves) and `PlanChange` (no setters; `PlanChange.create(...)` computes the effective week as the Monday after the creation day in the client's zone and sets state `PENDING`; `isOpen()` true for `PENDING` and `ACCEPTED_BY_ONE`). Unit test the effective week for a Monday, a Sunday and a zone whose local day differs from UTC (`PCT/domain/model/PlanChangeTest`).
- [ ] T004 [US1] Extend what detection hands over: `Finding` gains `List<Figure> figures` and `DuePlanChange` gains `figures` and `List<WeekTraining> weeks` (the rule's window, oldest first); `WeekTraining` gains `List<DayOfWeek> finishedDays` (weekdays of finished session logs). `HighRpeRule` adds `MEAN_RPE` per week; `StalledWeightRule` adds `BEST_LOAD_KG` per stalled exercise and week; `LowCompletionRule` adds `SESSIONS_PLANNED` and `SESSIONS_FINISHED` per week (`PC/domain/rule/`). Update `PC/infrastructure/output/JdbcTrainingHistory.java` to fill `finishedDays` and the existing rule and `JdbcTrainingHistory` tests; add rule assertions that the figures are the numbers the rule compared (boundary cases unchanged).
- [ ] T005 [US1] Replace the `PlanChangeRepository` port in `PC/domain/output/` with `hasOpen`, `lastEndedAt`, `save`, `find(PlanChangeId)`, `findOpen(AccountId client)` (see `contracts/ports.md`); remove `open(DuePlanChange)`. In `PC/domain/service/PlanChangeDetectionService.java` the chosen `DuePlanChange` is only logged (`log.info("Plan change due, rule={}", …)`, rule name only) until phase 4; update the detection tests and the in-memory fake.
- [ ] T006 [US1] Persistence adapter in `PC/infrastructure/output/`: `PlanChangeEntity` and child entities (plain `@Id UUID`, enums as strings, lazy associations), `JpaPlanChangeRepository`, `PlanChangeMapper` (MapStruct), `PlanChangeRepositoryImpl` (`@Repository`). Delete `LoggingPlanChangeRepository.java`. `hasOpen` = open states; `lastEndedAt` = max `ended_at`.
- [ ] T007 [US1] `PCT/infrastructure/output/PlanChangeRepositoryImplIT`: round trip with targets, a removed session with two moves, figures and concerned exercises; a second open plan change for one client is refused by the database while an ended one is not; `lastEndedAt` empty/non-empty; another client's plan change never comes back from `findOpen`.
- [ ] T008 [US1] Demo data `src/main/resources/db/dev-data/R__demo_plan_changes.sql`: one `PENDING` plan change per rule for three different demo clients with a published plan (ids and entries from `R__demo_plans.sql`), with targets, figures and concerned exercises consistent with their plans; idempotent (`on conflict do nothing`); no personal data.
- [ ] T009 [US1] Update `HexagonalArchitectureTest` only if a rule is needed; run `./gradlew check` in `backend/`; commit `feat(backend): store plan changes (#132)`.

---

## Phase 2: Limits per rule (#134) [US3]

**Goal**: a draft is accepted only inside the limits of its rule and the plan rules.

**Independent test**: for each rule, drafts on and just beyond every limit.

- [ ] T010 [P] [US3] `PC/domain/model/PlanSnapshot` (`version`, `firstWeek`, `weeks`, `sessions`), `SessionSnapshot(day, name, entries)`, `EntrySnapshot(exercise, category, measure, sets, amount, loadKg, rpe, restSeconds)`, and `PlanSnapshot.apply(PlanChangeContent)` returning the resulting snapshot (removed session gone, moved entries appended at the end of the receiving session, changed targets replaced; old values are those of the snapshot).
- [ ] T011 [US3] `PC/domain/rule/DraftLimits` interface (`Rule rule()`, `Set<String> violations(PlanSnapshot plan, Finding finding, PlanChangeContent draft)`) and `PlanRuleLimits` (applies to every draft): resulting plan has 2 to 6 sessions (`Plan.MIN_SESSIONS_WHEN_COMPLETE`, `Plan.MAX_SESSIONS`), one session per weekday, each entry within the `ExerciseEntry` bounds (sets 1–`MAX_SETS`, amount 1–`MAX_AMOUNT`, load a positive multiple of 0.1 at most 999.9, RPE 5–10, rest 0–`MAX_REST_SECONDS`), at most 8 entries per session, draft changes something (`limit.nothing-changed`), only known entries (`limit.unknown-key`); a target is never set on an entry that has none (an entry with no load keeps `null`, `limit.target-not-allowed`), with a test.
- [ ] T012 [P] [US3] `HighRpeLimits`: only `SETS` or `AMOUNT` targets (`limit.target-not-allowed`); only entries of `finding.exercises()` (`limit.entry-not-concerned`); per changed entry (each entry of a concerned exercise, not the exercise total) volume `sets × amount` within 85%–95% of the old (exact integer comparison `new*100 >= old*85` and `new*100 <= old*95`, `limit.volume-out-of-range`); load and RPE unchanged. Unit tests on 95%, 85% and just beyond both, a wrong exercise, a changed load.
- [ ] T013 [P] [US3] `StalledWeightLimits`: only `LOAD_KG` and `AMOUNT` targets; only stalled exercises; every concerned entry with a load must rise; new load within +2.5% to +15% of the old (`BigDecimal`, exact), a multiple of 0.5 kg (`limit.load-not-half-kg`), judged on the rounded value; `AMOUNT` may drop by at most 2 and not below 1 (`limit.amount-drop-too-large`), never rise. Unit tests on exactly 2.5%, 15%, just beyond, 0.5 kg step, an unlisted exercise, a lowered load.
- [ ] T014 [P] [US3] `LowCompletionLimits`: exactly one session removed (`limit.session-removal-count`), at least 2 remain (`limit.too-few-sessions`), at most 2 moves (`limit.too-many-moves`), every move comes from the removed session (`limit.move-not-from-removed`) and goes to a remaining session, no session over 8 entries (`limit.too-many-entries`), no target changes. Unit tests per limit, including a plan of 3 sessions and of 2.
- [ ] T015 [US3] `PC/domain/service/PlanChangeLimits`: picks the limits by rule and returns the union of violation codes with `PlanRuleLimits`; test that every rule is covered. Run `./gradlew check`; commit `feat(backend): check plan change drafts against rule limits (#134)`.

---

## Phase 3: Fixed transformation per rule (#135) [US4]

**Goal**: a deterministic draft for each rule.

**Independent test**: hand-computed results per rule, including rounding edges.

- [ ] T016 [P] [US4] `PC/domain/rule/FixedTransformation` interface (`Rule rule()`, `Optional<PlanChangeContent> apply(PlanSnapshot plan, DuePlanChange due)`; empty when the formula changes nothing).
- [ ] T017 [P] [US4] `HighRpeFallback`: for every entry of a concerned exercise `amount' = max(1, min(amount − 1, round-half-up(amount × 0.9)))`; sets, load, RPE untouched; only entries where the value changes (amount 1 stays). Tests: 10→9, 5→4 (−20%, exempt from the 15% limit), 2→1, 1 unchanged, a repeated exercise changes both entries, a seconds exercise (30→27).
- [ ] T018 [P] [US4] `StalledWeightFallback`: for every entry of a stalled exercise with a load `load' = ceil-to-0.5(load × 1.05)`; nothing else; a result above 999.9 is skipped. Tests: 100→105, 101→106.5 (106.05 rounds up), 20→21, 1.0→1.5 (exempt from the 15% limit), bodyweight entry untouched.
- [ ] T019 [P] [US4] `LowCompletionFallback`: removes the plan session finished least often over `due.weeks()` (count `finishedDays` per weekday); a tie goes to the latest weekday; moves nothing; empty when the plan has 2 sessions. Tests: clear minimum, tie between Tuesday and Friday (Friday), a session never finished, plan of 2.
- [ ] T020 [US4] Test that each fallback result passes `PlanRuleLimits` and that the same input gives the same output twice. Run `./gradlew check`; commit `feat(backend): add fixed transformation per rule (#135)`.

---

## Phase 4: Drafter port and LLM adapter (#133) [US2]

**Goal**: a fired rule becomes a stored plan change: LLM draft, checked, one retry, else the fixed transformation.

**Independent test**: the prompt holds no identifier; attempt policy; an end-to-end `check` with fakes.

- [ ] T021 [P] [US2] `PC/domain/output/PlanSource` (`currentPlan(AccountId)`, `exerciseNames(Set<ExerciseId>)`) and `PC/infrastructure/output/JdbcPlanSource` (read model over `plans`, `plan_sessions`, `plan_session_entries`, `exercises`, as `JdbcTrainingHistory`; every query scoped to the one client; returns the plan `version`). `PCT/infrastructure/output/JdbcPlanSourceIT`.
- [ ] T022 [P] [US2] `PC/domain/model/PlanChangePromptMessages(system, user, rejections)`, `ProposedContent` (changes by key, `removeSession`, moves), exceptions `PlanChangeDraftingFailedException`, `PlanChangeDrafterUnavailableException` in `PC/domain/exception/`, and the port `PC/domain/output/PlanChangeDrafter` (`ProposedContent draft(PlanChangePromptMessages)`).
- [ ] T023 [US2] `PC/domain/service/PlanChangePrompt`: builds the messages as in `contracts/ports.md` (keys `sN`/`eN`, category, measure, numbers, concerned flag, weekly aggregates from the figures and weeks, the rule's limits in words; no names, ids, dates or free text; exercise and session names are not sent) and maps a `ProposedContent` back to a `PlanChangeContent` (old values from the snapshot; an unknown key is `limit.unknown-key`). `PCT/domain/service/PlanChangePromptTest`: for a client whose name, email, ids and trainer-written exercise and session names are known, none appears in the system or user message.
- [ ] T024 [US2] `PC/domain/service/PlanChangeDraftingService`: up to 2 attempts (second adds one sentence per violation code of the first), a failed or unavailable attempt counts, then the fixed transformation; returns empty when nothing changes. Tests with a hand-written drafter fake: valid first answer asks once; invalid then valid uses the second; two invalid use the fallback; unavailable uses the fallback; a draft that touches another exercise is rejected; nothing logged but codes.
- [ ] T025 [US2] Wire it into `PlanChangeDetectionService.check`: after choosing the finding, read the plan (stop when it has none), draft, and when content exists and `effective_week` is not after the plan's last week, `save(PlanChange.create(...))` with figures, concerned exercises and `PlanSnapshot.version`, all inside the existing transaction (`ponytail:` comment naming the held-connection ceiling). A failure leaves nothing stored because the claim rolls back. Tests: a fired rule stores one plan change; plan ending before the effective week stores none; a client with an open plan change is not evaluated; two checks of one week store one.
- [ ] T026 [US2] Integration test `PCT/domain/service/PlanChangeDetectionIT` (Testcontainers, real `JdbcTrainingHistory`, `JdbcPlanSource`, `PlanChangeRepositoryImpl`, drafter fake that always fails): logs and a published plan that make a rule fire end in one stored `PENDING` plan change with the rule's fixed transformation, the figures and the plan version; a second `check` of the same week stores nothing.
- [ ] T027 [P] [US2] Adapters in `PC/infrastructure/output/`: `OpenRouterPlanChangeDrafter` (Spring AI `ChatModel`, JSON schema answer, `@ConditionalOnExpression` on `kinetiq.plan-drafting.api-key` like `OpenRouterPlanGenerator`, logs the exception class only, never prompt or answer) and `UnavailablePlanChangeDrafter` (inverse condition). Wire the beans and the schema in `PC/infrastructure/config/PlanChangeConfig.java`. `.env.example` needs no new variable.
- [ ] T028 [US2] Run `./gradlew check`; commit `feat(backend): draft plan changes with an LLM (#133)`.

---

## Phase 5: Reason line and title (#136) [US5]

**Goal**: plain-words title and reason, English and Polish, built on read.

**Independent test**: one test per rule.

- [ ] T029 [P] [US5] `PC/domain/model/PlanChangeText(title, reason)` and `PC/domain/service/PlanChangeWording` (`PlanChangeText of(Language, PlanChange, Map<ExerciseId,String> names)`) with a small Polish plural helper (1 / 2–4 / 5+). Percent in a title: change of the total volume (sets × amount) of the changed entries (high RPE) or the mean load rise of the changed entries (stalled), in whole percent, half up. A title names the exercise when one is affected, otherwise counts them; there is no exercise group. Wording never uses a pronoun.
- [ ] T030 [US5] English wording: high RPE "Cut volume by 10%" / one exercise "Cut Romanian deadlift volume by 10%", reason "Average RPE was 8.3 and 8.6 in the last 2 weeks, above 8."; stalled "Raise back squat load by 10%" / "Raise load on 3 exercises by 10%", reason "Back squat rose 2.1% in 3 weeks, less than 5%." (several: "3 exercises rose less than 5% in 3 weeks."); low completion "Cut to 2 sessions a week", reason "50% of planned sessions were finished in the last 2 weeks, below 70%."
- [ ] T031 [US5] Polish wording of the same texts with correct plurals (for example "Obniż objętość o 10%", "Podnieś obciążenie w 3 ćwiczeniach o 10%", "Ograniczenie do 2 sesji w tygodniu", "Ukończono 50% zaplanowanych sesji w ostatnich 2 tygodniach, poniżej 70%."). Tests `PCT/domain/service/PlanChangeWordingTest`: one per rule in both languages, plural edges (2, 5, 12 exercises), no name or email in any text.
- [ ] T032 [US5] Run `./gradlew check`; commit `feat(backend): build plan change title and reason (#136)`.

---

## Phase 6: Trainer plan change detail (#289) [US6]

**Goal**: the trainer reads a plan change of their own client from the client page.

**Independent test**: open the detail of each rule in the browser; another trainer's id answers `404`.

- [ ] T033 [P] [US6] Backend use case `PC/domain/input/ViewPlanChangeUseCase` (`get`, `openOf`), views `PlanChangeView`, `PlanChangeSummary`, `PC/domain/service/PlanChangeViewService` (ownership through `ViewClientsUseCase.getClient`; language through `CurrentAccountUseCase`; names through `PlanSource`; title and reason through `PlanChangeWording`), `PlanChangeNotFoundException` (`plan-change.not-found`). Unit tests: own client reads, another trainer's client and a missing id both give `PlanChangeNotFoundException`, a Polish trainer gets Polish text.
- [ ] T034 [US6] Web adapter in `PC/infrastructure/input/`: `PlanChangeController` (`GET /api/v1/plan-changes/{id}`, `GET /api/v1/clients/{id}/plan-change`), `PlanChangeResponse`, `ClientPlanChangeResponse`, `PlanChangeWebMapper`; register in `PlanChangeConfig`. Add `"/api/v1/plan-changes/**"` to the trainer `requestMatchers` of `common/infrastructure/config/SecurityConfig.java` (otherwise any signed-in role passes the generic `/api/**` rule). `@WebMvcTest` (with the real security rules): 200 shape, `404 plan-change.not-found`, client role `403 request.not-allowed` on both paths, unauthenticated `401`; no exception text in the body.
- [ ] T035 [US6] With the backend running run `npm run gen:api` in `frontend/` and commit `src/lib/api/schema.d.ts` (never edit by hand).
- [ ] T036 [P] [US6] Add missing shadcn pieces only if needed (`npx shadcn@latest search`, read `docs` first); frontend `src/features/planchange/api/plan-change.ts` (query by id), `components/plan-change-table.tsx` (Exercise, Session, Now, New, Change; `data-density="compact"`, `num` class, old value struck through, removed session and moves rows for low completion), `components/rule-chart.tsx` (shadcn `chart`, the rule's threshold line, text alternative), `pages/plan-change-page.tsx` at route `/trainer/plan-changes/:id` registered in `src/app/router.ts` (lazy, under the trainer area), with loading, not found (translated by `code`) and error states; no accept or keep buttons.
- [ ] T037 [P] [US6] `src/components/plan-change-card.tsx` (presentational: title, reason, effective week, link) and the lookup query in `src/features/client/api/clients.ts`; show the card on `src/features/client/pages/client-page.tsx` when the client has an open plan change.
- [ ] T038 [US6] Strings in `src/locales/en.json` and `pl.json` (rule notes such as "Only exercises with an average RPE above 8 change", column titles, chart labels, states, not found); Vitest tests: the table renders old and new values and change per rule, the card appears only with an open plan change, a `404` shows the translated message.
- [ ] T039 [US6] Run `npm run check` in `frontend/` and `./gradlew check` in `backend/`. Verify in the browser per `quickstart.md`: each rule's detail, EN and PL, light and dark, 1280px, keyboard focus and labels; another trainer's id gives `404`. Commit `feat: add trainer plan change detail (#289)`.

---

## Phase 7: Final verification and pull request

- [ ] T040 Run `/speckit-analyze` and `/speckit-converge`; fix inconsistencies or add tasks.
- [ ] T041 Security pass: no endpoint opened, ownership `404`s proven, prompt privacy test green, logs hold no prompt, answer, name or email (grep the new code for `log.`), error responses carry no exception text; run the `security-review` skill on the branch.
- [ ] T042 Full gates once more (`./gradlew check`, `npm run check`), then open the pull request with `gh` (title in Conventional Commits, `Closes #131`, `Closes #132`, `Closes #133`, `Closes #134`, `Closes #135`, `Closes #136`, `Closes #289`), bind it with the ccd_pr tools.

---

## Dependencies

- Phase 1 first (model, repository, figures). Phase 2 and 3 need Phase 1's `DuePlanChange` and `PlanChangeContent`; they do not need each other. Phase 4 needs 1, 2 and 3. Phase 5 needs 1. Phase 6 needs 1 and 5 (4 for real data, not for tests).
- Within a phase, tasks marked `[P]` touch different files.

## Parallel examples

- Phase 2: T012, T013, T014 together after T010–T011.
- Phase 3: T017, T018, T019 together after T016.
- Phase 6: T036 and T037 together after T035.

## Implementation strategy

MVP is Phases 1–4: a fired rule yields a stored, checked plan change even without an LLM. Phases 5–6 make it readable. Commit after each phase; never leave a commit that does not pass its gate.
