# Feature Specification: Rules detect that a plan change is due

**Feature Branch**: `feat/125-plan-change-rules`

**Created**: 2026-11-09

**Status**: Draft

**Input**: Story #125 "Rules detect that a plan change is due" with tasks #126 (weekly run per client time zone), #127 (rule: high RPE), #128 (rule: stalled weight), #129 (rule: low completion) and #130 (priority, one open plan change and cool-down). Backend only: the story has no screens. Decision: `docs/adr/0002-plan-changes-rules-decide-llm-drafts.md`. Builds on specs `92-session-logging` and `98-finish-session` (logs, finished and missed sessions) and `75-client-plan` (the client's plan and its weeks). The client's time zone is the one held on the account.

## Clarifications

### Session 2026-11-09

- Q: When does an accepted plan change count as "ended" for the cool-down? → A: At the start of its effective week.
- Q: If the plan is edited during a window, which sessions are planned? → A: The current plan's sessions, for every week of the window.
- Q: Does high RPE need a minimum amount of logging per week? → A: No minimum; any week with at least one set that has an RPE counts.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The system checks every client each Monday (Priority: P1)

Every Monday at 03:00 in each client's own time zone, the system looks at that client's completed weeks (Monday to Sunday) and checks the three rules. A client whose local time has just passed Monday 03:00 is checked exactly once for that week, whatever the server's time zone is.

**Why this priority**: Without the weekly run no rule ever gets a chance to fire; it is the engine of the story.

**Independent Test**: With a fixed clock, move time across Monday 03:00 in two clients' different time zones: each is checked once, at its own 03:00, and not again that week.

**Acceptance Scenarios**:

1. **Given** a client in Warsaw and a client in Los Angeles, **When** the clock passes Monday 03:00 in Warsaw, **Then** only the Warsaw client is checked; the Los Angeles client is checked when its own Monday 03:00 passes.
2. **Given** a client already checked this week, **When** the run happens again before the next Monday 03:00, **Then** the client is not checked again.
3. **Given** the system was down at Monday 03:00, **When** it starts later the same week, **Then** each client not yet checked this week is checked once, over the same completed weeks.
4. **Given** a week with a daylight-saving change, **When** the clock passes Monday 03:00 local time, **Then** the client is checked once, over Monday to Sunday of the weeks that just ended.
5. **Given** a client with no published plan (no plan or only a draft), **When** the run happens, **Then** no rule is evaluated and nothing is created.
6. **Given** a plan published less than the window of a rule ago, **When** the run happens, **Then** that rule is not evaluated: weeks before the plan was published never count.

---

### User Story 2 - High RPE calls for a plan change (Priority: P1)

The rule fires when the mean RPE of all logged sets is above 8.0 in each of the last 2 completed weeks.

**Why this priority**: It is the main signal that a plan is too hard and the clearest safety rule.

**Independent Test**: Feed two weeks of logs with a mean RPE of exactly 8.0, then just above 8.0: only the second fires.

**Acceptance Scenarios**:

1. **Given** a mean RPE of 8.0 in both weeks, **When** the rule is checked, **Then** it does not fire.
2. **Given** a mean RPE just above 8.0 (for example 8.01) in both weeks, **When** the rule is checked, **Then** it fires.
3. **Given** a mean above 8.0 in one week and at most 8.0 in the other, **When** the rule is checked, **Then** it does not fire.
4. **Given** a week with no logged sets, **When** the rule is checked, **Then** it does not fire.
5. **Given** the rule fires, **Then** it records which exercises had a mean RPE above 8 in the window, so the later draft can limit its change to them.

---

### User Story 3 - Stalled weight calls for a plan change (Priority: P1)

For each exercise that has a load, the rule fires when the exercise was logged in each of the last 3 completed weeks, its best set rose by less than 5% from week 1 to week 3, and its mean RPE over those weeks is at most 8.

**Why this priority**: It catches clients who are comfortable but no longer progressing.

**Independent Test**: Feed three weeks of logs for one exercise whose best load rises exactly 5%, then less than 5%, then with a mean RPE above 8: only the middle case fires.

**Acceptance Scenarios**:

1. **Given** best loads of 100, 102 and 105 kg in weeks 1 to 3 and a mean RPE of 7, **When** the rule is checked, **Then** it does not fire (exactly 5% is not less than 5%).
2. **Given** best loads of 100, 101 and 104.9 kg and a mean RPE of 8, **When** the rule is checked, **Then** it fires.
3. **Given** the same loads and a mean RPE above 8, **When** the rule is checked, **Then** it does not fire: that exercise belongs to the high RPE rule.
4. **Given** an exercise logged in only 2 of the 3 weeks, **When** the rule is checked, **Then** it does not fire for that exercise.
5. **Given** an exercise without a load (bodyweight, time only), **When** the rule is checked, **Then** it is ignored.
6. **Given** several exercises where only some are stalled, **When** the rule fires, **Then** it records exactly the stalled exercises.
7. **Given** the best load fell from week 1 to week 3, **When** the rule is checked, **Then** it fires (a fall is less than a 5% rise).

---

### User Story 4 - Low completion calls for a plan change (Priority: P1)

The rule fires when finished sessions divided by planned sessions is below 70% over the last 2 completed weeks. It never fires for a plan of 2 sessions a week.

**Why this priority**: A plan the client cannot keep to has to change, but a plan already at the minimum cannot be made smaller.

**Independent Test**: A plan of 3 sessions a week with 4 of 6 finished (66.7%) fires; 5 of 6 (83.3%) and exactly 70% do not; a plan of 2 a week with none finished does not fire.

**Acceptance Scenarios**:

1. **Given** 3 sessions a week and 4 of 6 finished, **When** the rule is checked, **Then** it fires.
2. **Given** exactly 70% finished (for example 7 of 10 over two weeks of 5), **When** the rule is checked, **Then** it does not fire.
3. **Given** a plan of 2 sessions a week, **When** the rule is checked, **Then** it never fires, however few sessions are finished.
4. **Given** a session started but not finished before it locked, **When** completion is counted, **Then** it counts as planned and not finished.
5. **Given** a plan published in the middle of the window, **When** completion is counted, **Then** sessions before the publication day are not planned sessions.

---

### User Story 5 - One plan change at a time, by priority, with a cool-down (Priority: P1)

A client has at most one open plan change. When several rules fire in one run, the first of completion, then RPE, then stalled weight wins. A rule does not fire again for a client until its full window has passed since the client's last plan change ended.

**Why this priority**: Without it the trainer would be flooded and the client's plan would change again before the previous change had any effect.

**Independent Test**: With all three rules firing, only a low completion plan change is reported as due; with an open plan change, none is reported; after a plan change ends, rules stay quiet until their windows have passed.

**Acceptance Scenarios**:

1. **Given** all three rules fire, **When** the run finishes, **Then** one plan change is due, caused by low completion.
2. **Given** RPE and stalled weight fire, **Then** the plan change due is caused by RPE.
3. **Given** a client has an open plan change, **When** the run happens, **Then** no rule is evaluated and no plan change is reported as due.
4. **Given** a plan change ended (declined, expired, or accepted and its effective week began) less than a rule's window ago (2 weeks for completion and RPE, 3 for stalled weight), **When** the run happens, **Then** that rule does not fire, and weeks before the end of the plan change do not count.
5. **Given** the winning rule is in cool-down, **When** another rule fires, **Then** the other rule's plan change is reported as due.
6. **Given** two runs of the same week happen at once, **Then** at most one plan change is reported as due.

---

### Edge Cases

- A client changes time zone: they are checked at the new Monday 03:00 and never twice for the same completed week.
- A client has a plan published during the week: only completed weeks from the publication week count.
- A plan whose last week has passed: no rule is evaluated.
- A set logged without RPE is left out of mean RPE; a week without any RPE counts as no data and the rule does not fire.
- One client's failure does not stop the others from being checked, and does not create a partial plan change.
- Each client is evaluated only over their own plan and logs; nothing of another client is read for them.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST check each client with a published plan once per week, after Monday 03:00 in the client's time zone, over completed weeks (Monday to Sunday in that time zone).
- **FR-002**: The system MUST check a client not yet checked this week when it comes back after a missed run, and MUST NOT check the same client twice for the same week.
- **FR-003**: The system MUST evaluate only weeks from the week the plan was published; a rule whose window is not fully inside the plan MUST NOT be evaluated.
- **FR-004**: The high RPE rule MUST fire when the mean RPE of all sets with an RPE is strictly above 8.0 in each of the last 2 completed weeks.
- **FR-005**: The stalled weight rule MUST fire for an exercise with a load that was logged in each of the last 3 completed weeks, whose best set in week 3 is less than 5% above its best set in week 1, and whose mean RPE over the 3 weeks is at most 8.
- **FR-006**: The low completion rule MUST fire when finished sessions divided by planned sessions is strictly below 70% over the last 2 completed weeks, and MUST NOT fire for a plan of 2 sessions a week.
- **FR-007**: A fired rule MUST report a plan change as due, carrying the rule, the window it looked at and the exercises it concerns (those above RPE 8, or the stalled ones), so its reason can be shown once the plan change is stored (task #132).
- **FR-008**: A client MUST have at most one open plan change; a client with one MUST NOT be evaluated.
- **FR-009**: When several rules fire in one run, the system MUST report one plan change as due, for the first of: low completion, high RPE, stalled weight.
- **FR-010**: A rule MUST NOT fire for a client until its full window (2 weeks for completion and RPE, 3 for stalled weight) has passed since the client's last plan change ended, and weeks before that end MUST NOT count.
- **FR-011**: A failure while checking one client MUST NOT affect other clients, and MUST NOT leave a partial plan change.
- **FR-012**: Rules MUST be deterministic and computed from logged data only; no LLM decides whether a plan change is due (constitution IV).
- **FR-013**: The run MUST expose no endpoint, and MUST NOT write names, emails or other personal data to logs.

### Key Entities *(include if feature involves data)*

- **Rule**: a condition over a client's logs: high RPE, stalled weight or low completion, each with its window.
- **Plan change due**: what the run reports for a client: the rule, the window it looked at and the exercises it concerns. Turning it into a stored plan change, and its content and acceptance, belong to later stories.
- **Weekly check**: the fact that a client was checked for a given week, so that a week is never checked twice.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: At every threshold named in the story (RPE 8.0 and just above, exactly 5%, exactly 70%, 2 sessions a week) the rules decide as specified, proven by tests.
- **SC-002**: Over a simulated month, every client is checked exactly once per week at their own Monday 03:00, including across a daylight-saving change and a missed run.
- **SC-003**: A client with an open plan change is never evaluated, and two runs of the same week check a client once. That a client can never hold two open plan changes is guaranteed by the plan change storage (task #132).
- **SC-004**: With all rules firing, the plan change reported as due is always the low completion one; with an open plan change or a cool-down running, none is reported.
- **SC-005**: A client whose logs sit exactly on a threshold never gets a plan change from it.

## Assumptions

- No plan change exists in the system yet. This story only decides that one is due and hands over the rule, window and exercises; storing a plan change (task #132), its content (the LLM draft and its fixed fallback), acceptance, expiry and screens are other stories. Until storage exists, nothing is created and a rule that still holds is reported again each week.
- A plan is open to evaluation from its first week, which may be partial when it was published mid-week; that week counts as published.
- "Ended" means the plan change was declined, expired or accepted. A declined plan change ends when it is declined; an expired or accepted one ends at the start of its effective week (the week after the plan change was created), so the cool-down counts only weeks trained under the changed plan.
- Planned sessions come from the plan as it is now, for every week of the window; no plan version history is kept.
- "Planned sessions" and "finished sessions" follow session state (`98-finish-session`): Done is finished; Missed and started-but-locked are not.
- The high RPE rule has no minimum number of sets per week; the story sets none.
- The best set of an exercise in a week is its highest load; the mean RPE of an exercise is over its sets that have an RPE.
- The client's time zone comes from their account and is a valid zone id, used as it is at run time.
- A plan of 2 sessions a week is the smallest plan (ADR 0002: never below two), so low completion has nothing to remove there.
