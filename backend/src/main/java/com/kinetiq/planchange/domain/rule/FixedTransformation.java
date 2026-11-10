package com.kinetiq.planchange.domain.rule;

import com.kinetiq.planchange.domain.model.DuePlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.Rule;
import java.util.Optional;

/**
 * The fixed formula of one rule, used when no LLM answer keeps the limits. Deterministic, within the plan rules, and
 * not bound by the rule's percentage limits.
 */
public interface FixedTransformation {

    Rule rule();

    /** The content the formula gives; empty when it changes nothing. */
    Optional<PlanChangeContent> apply(PlanSnapshot plan, DuePlanChange due);
}
