package com.kinetiq.planchange.domain.rule;

import com.kinetiq.planchange.domain.model.DuePlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.Rule;
import java.util.HashSet;
import java.util.Set;

/**
 * Low completion removes exactly one session, keeps at least 2 and may move up to 2 of its entries to the sessions
 * that stay; it changes no target. The content itself allows at most 2 moves, and the plan rules keep a session that
 * receives entries at 8 or fewer.
 */
public final class LowCompletionLimits implements ContentLimits {

    private static final int MIN_REMAINING = 2;

    @Override
    public Rule rule() {
        return Rule.LOW_COMPLETION;
    }

    @Override
    public Set<String> violations(PlanSnapshot plan, DuePlanChange due, PlanChangeContent content) {
        Set<String> violations = new HashSet<>();
        if (content.removed().isEmpty()) {
            violations.add(LimitCode.SESSION_REMOVAL_COUNT);
        } else if (plan.sessions().size() - 1 < MIN_REMAINING) {
            violations.add(LimitCode.TOO_FEW_SESSIONS);
        }
        if (!content.changes().isEmpty()) {
            violations.add(LimitCode.TARGET_NOT_ALLOWED);
        }
        return violations;
    }
}
