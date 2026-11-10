package com.kinetiq.planchange.domain.rule;

import com.kinetiq.planchange.domain.model.DuePlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.Rule;
import java.util.Set;

/** What one rule allows plan change content to change. Pure: it reads nothing and changes nothing. */
public interface ContentLimits {

    Rule rule();

    /**
     * The {@link LimitCode} of every limit the content breaks, empty when it keeps them all. Every entry and session
     * the content names exists in {@code plan}.
     */
    Set<String> violations(PlanSnapshot plan, DuePlanChange due, PlanChangeContent content);
}
