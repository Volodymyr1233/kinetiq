package com.kinetiq.planchange.domain.rule;

import com.kinetiq.planchange.domain.model.DuePlanChange;
import com.kinetiq.planchange.domain.model.EntryRef;
import com.kinetiq.planchange.domain.model.EntrySnapshot;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.SessionSnapshot;
import com.kinetiq.planchange.domain.model.Target;
import com.kinetiq.planchange.domain.model.TargetChange;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Stalled weight raises the load of every entry of a stalled exercise by 2.5% to 15%, to a multiple of 0.5 kg, and
 * may lower its repetitions by up to 2, never below 1. Nothing else changes.
 */
public final class StalledWeightLimits implements ContentLimits {

    private static final Set<Target> ALLOWED = Set.of(Target.LOAD_KG, Target.AMOUNT);
    private static final BigDecimal MIN_RISE = new BigDecimal("1.025");
    private static final BigDecimal MAX_RISE = new BigDecimal("1.15");
    private static final BigDecimal LOAD_STEP = new BigDecimal("0.5");
    private static final int MAX_AMOUNT_DROP = 2;

    @Override
    public Rule rule() {
        return Rule.STALLED_WEIGHT;
    }

    @Override
    public Set<String> violations(PlanSnapshot plan, DuePlanChange due, PlanChangeContent content) {
        Set<String> violations = new HashSet<>();
        if (content.removed().isPresent()) {
            violations.add(LimitCode.SESSION_REMOVAL_COUNT);
        }
        for (TargetChange change : content.changes()) {
            if (!ALLOWED.contains(change.target())) {
                violations.add(LimitCode.TARGET_NOT_ALLOWED);
            } else if (!due.exercises().contains(change.exercise())) {
                violations.add(LimitCode.ENTRY_NOT_CONCERNED);
            } else {
                EntrySnapshot entry = plan.entry(change.entry()).orElseThrow();
                violations.addAll(change.target() == Target.LOAD_KG
                        ? loadViolations(entry.loadKg(), change.newValue())
                        : amountViolations(entry.amount(), change.newValue()));
            }
        }
        if (!everyLoadRises(plan, due, content)) {
            violations.add(LimitCode.LOAD_OUT_OF_RANGE);
        }
        return violations;
    }

    // judged on the new value as given, which must already be a multiple of 0.5 kg
    private static Set<String> loadViolations(BigDecimal old, BigDecimal value) {
        Set<String> violations = new HashSet<>();
        if (value.remainder(LOAD_STEP).signum() != 0) {
            violations.add(LimitCode.LOAD_NOT_HALF_KG);
        }
        if (value.compareTo(old.multiply(MIN_RISE)) < 0 || value.compareTo(old.multiply(MAX_RISE)) > 0) {
            violations.add(LimitCode.LOAD_OUT_OF_RANGE);
        }
        return violations;
    }

    private static Set<String> amountViolations(int old, BigDecimal value) {
        boolean allowed = value.compareTo(BigDecimal.valueOf(old)) < 0
                && value.compareTo(BigDecimal.valueOf((long) old - MAX_AMOUNT_DROP)) >= 0
                && value.compareTo(BigDecimal.ONE) >= 0;
        return allowed ? Set.of() : Set.of(LimitCode.AMOUNT_DROP_TOO_LARGE);
    }

    private static boolean everyLoadRises(PlanSnapshot plan, DuePlanChange due, PlanChangeContent content) {
        for (SessionSnapshot session : plan.sessions()) {
            for (int position = 0; position < session.entries().size(); position++) {
                EntrySnapshot entry = session.entries().get(position);
                EntryRef ref = new EntryRef(session.day(), position);
                boolean raised = content.changes().stream()
                        .anyMatch(change -> change.entry().equals(ref) && change.target() == Target.LOAD_KG);
                if (due.exercises().contains(entry.exercise()) && entry.loadKg() != null && !raised) {
                    return false;
                }
            }
        }
        return true;
    }
}
