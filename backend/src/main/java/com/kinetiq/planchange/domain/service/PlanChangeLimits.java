package com.kinetiq.planchange.domain.service;

import com.kinetiq.planchange.domain.model.DuePlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.rule.ContentLimits;
import com.kinetiq.planchange.domain.rule.LimitCode;
import com.kinetiq.planchange.domain.rule.PlanRuleLimits;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Checks plan change content against the plan rules and the limits of its rule. */
public final class PlanChangeLimits {

    private final PlanRuleLimits planRules = new PlanRuleLimits();
    private final Map<Rule, ContentLimits> byRule;

    /** One limits per rule; a rule without limits is refused here, so no content goes unchecked. */
    public PlanChangeLimits(List<ContentLimits> limits) {
        this.byRule = limits.stream().collect(Collectors.toUnmodifiableMap(ContentLimits::rule, Function.identity()));
        for (Rule rule : Rule.values()) {
            if (!byRule.containsKey(rule)) {
                throw new IllegalArgumentException("no limits for " + rule);
            }
        }
    }

    /** Every {@link LimitCode} the content breaks; empty when it may be stored. */
    public Set<String> violations(PlanSnapshot plan, DuePlanChange due, PlanChangeContent content) {
        Set<String> violations = new HashSet<>(planRules.violations(plan, content));
        // the rule's limits read the entries the content names, which must exist first
        if (!violations.contains(LimitCode.UNKNOWN_KEY)) {
            violations.addAll(byRule.get(due.rule()).violations(plan, due, content));
        }
        return Set.copyOf(violations);
    }
}
