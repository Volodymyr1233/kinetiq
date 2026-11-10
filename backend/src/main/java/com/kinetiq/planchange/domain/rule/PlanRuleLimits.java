package com.kinetiq.planchange.domain.rule;

import com.kinetiq.plan.domain.model.ExerciseEntry;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.planchange.domain.model.EntryRef;
import com.kinetiq.planchange.domain.model.EntrySnapshot;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.SessionMove;
import com.kinetiq.planchange.domain.model.SessionSnapshot;
import com.kinetiq.planchange.domain.model.Target;
import com.kinetiq.planchange.domain.model.TargetChange;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The plan rules every plan change content keeps, whatever its rule: it names only what the plan has, and the plan
 * it leads to is still a valid plan.
 */
public final class PlanRuleLimits {

    /** The most entries a session may end with when a plan change adds entries to it. */
    public static final int MAX_ENTRIES_AFTER_MOVE = 8;

    private static final BigDecimal MAX_LOAD = new BigDecimal(ExerciseEntry.MAX_LOAD_KG);

    /** The {@link LimitCode} of every plan rule the content breaks. */
    public Set<String> violations(PlanSnapshot plan, PlanChangeContent content) {
        Set<String> unknown = unknownKeys(plan, content);
        if (!unknown.isEmpty()) {
            return unknown;
        }
        Set<String> violations = new HashSet<>();
        Set<Map.Entry<EntryRef, Target>> keys = new HashSet<>();
        for (TargetChange change : content.changes()) {
            if (!keys.add(Map.entry(change.entry(), change.target()))) {
                violations.add(LimitCode.PLAN_RULE);
            }
            EntrySnapshot entry = plan.entry(change.entry()).orElseThrow();
            if (entry.value(change.target()).isEmpty()) {
                // an entry without a load never gets one
                violations.add(LimitCode.TARGET_NOT_ALLOWED);
            }
            if (!withinBounds(change.target(), change.newValue())) {
                violations.add(LimitCode.PLAN_RULE);
            }
        }
        if (!violations.isEmpty()) {
            return violations;
        }
        PlanSnapshot result = plan.apply(content);
        int sessions = result.sessions().size();
        if (sessions < Plan.MIN_SESSIONS_WHEN_COMPLETE) {
            violations.add(LimitCode.TOO_FEW_SESSIONS);
        }
        if (sessions > Plan.MAX_SESSIONS) {
            violations.add(LimitCode.PLAN_RULE);
        }
        for (SessionSnapshot session : result.sessions()) {
            int before = plan.session(session.day()).orElseThrow().entries().size();
            if (session.entries().size() > before && session.entries().size() > MAX_ENTRIES_AFTER_MOVE) {
                violations.add(LimitCode.TOO_MANY_ENTRIES);
            }
        }
        return violations;
    }

    // a move goes to a session that stays; anything named must exist in the plan
    private static Set<String> unknownKeys(PlanSnapshot plan, PlanChangeContent content) {
        boolean unknown = content.changes().stream().anyMatch(change -> plan.entry(change.entry()).isEmpty())
                || content.removed().map(removed -> plan.session(removed.day()).isEmpty()).orElse(false)
                || content.moves().stream().anyMatch(move -> plan.entry(move.from()).isEmpty()
                        || plan.session(move.toDay()).isEmpty()
                        || move.toDay() == move.from().day());
        boolean changesRemoved = content.removed()
                .map(removed -> content.changes().stream().anyMatch(change -> change.entry().day() == removed.day()))
                .orElse(false);
        Set<EntryRef> moved = content.moves().stream().map(SessionMove::from).collect(Collectors.toSet());
        boolean movedTwice = moved.size() < content.moves().size();
        return unknown || changesRemoved || movedTwice ? Set.of(LimitCode.UNKNOWN_KEY) : Set.of();
    }

    private static boolean withinBounds(Target target, BigDecimal value) {
        if (target != Target.LOAD_KG && value.stripTrailingZeros().scale() > 0) {
            return false;
        }
        return switch (target) {
            case SETS -> within(value, ExerciseEntry.MIN_SETS, ExerciseEntry.MAX_SETS);
            case AMOUNT -> within(value, ExerciseEntry.MIN_AMOUNT, ExerciseEntry.MAX_AMOUNT);
            case RPE -> within(value, ExerciseEntry.MIN_RPE, ExerciseEntry.MAX_RPE);
            case REST_SECONDS -> within(value, ExerciseEntry.MIN_REST_SECONDS, ExerciseEntry.MAX_REST_SECONDS);
            case LOAD_KG -> value.signum() > 0
                    && value.compareTo(MAX_LOAD) <= 0
                    && value.stripTrailingZeros().scale() <= ExerciseEntry.LOAD_DECIMALS;
        };
    }

    private static boolean within(BigDecimal value, int min, int max) {
        return value.compareTo(BigDecimal.valueOf(min)) >= 0 && value.compareTo(BigDecimal.valueOf(max)) <= 0;
    }
}
