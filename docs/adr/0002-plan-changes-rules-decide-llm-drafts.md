# Plan changes: rules decide, an LLM drafts within limits, a fixed transformation is the fallback

Whether a plan change is due is decided by three rules over logged data (high RPE, stalled weight, low completion), run every Monday over completed weeks. The LLM only drafts what changes, and each rule limits the draft: volume −5% to −15% on the exercises whose RPE was above 8; load +2.5% to +15% on stalled exercises (the specification's "15%" is the cap, not the step, because +15% load on a stalled lift is unsafe); exactly one session fewer, never below two. A draft that breaks its limits or the plan rules twice is replaced by the rule's fixed transformation, so a rule that fires always produces a plan change even when a free model fails.

## Considered options

- Rules only, no LLM: simpler and fully predictable; rejected because the team wants the LLM to choose which targets move, within limits.
- LLM decides when and what: rejected; detection is arithmetic, and every change has to state a reason a person can check.
