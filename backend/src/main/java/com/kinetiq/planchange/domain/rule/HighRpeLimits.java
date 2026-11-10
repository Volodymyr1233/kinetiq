package com.kinetiq.planchange.domain.rule;

import com.kinetiq.planchange.domain.model.DuePlanChange;
import com.kinetiq.planchange.domain.model.EntryRef;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.Target;
import com.kinetiq.planchange.domain.model.TargetChange;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * High RPE may lower only the sets and repetitions (or seconds) of the exercises above RPE 8, so that the volume of
 * each changed entry drops by 5% to 15%.
 */
public final class HighRpeLimits implements ContentLimits {

    private static final Set<Target> ALLOWED = Set.of(Target.SETS, Target.AMOUNT);
    private static final int MIN_PERCENT = 85;
    private static final int MAX_PERCENT = 95;

    @Override
    public Rule rule() {
        return Rule.HIGH_RPE;
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
            }
            if (!due.exercises().contains(change.exercise())) {
                violations.add(LimitCode.ENTRY_NOT_CONCERNED);
            }
        }
        if (!violations.isEmpty()) {
            return violations;
        }
        PlanSnapshot result = plan.apply(content);
        Map<EntryRef, List<TargetChange>> byEntry =
                content.changes().stream().collect(Collectors.groupingBy(TargetChange::entry));
        for (EntryRef entry : byEntry.keySet()) {
            long before = plan.entry(entry).orElseThrow().volume();
            long after = result.entry(entry).orElseThrow().volume();
            // exact on whole numbers: 85% <= after / before <= 95%
            if (after * 100 < before * MIN_PERCENT || after * 100 > before * MAX_PERCENT) {
                violations.add(LimitCode.VOLUME_OUT_OF_RANGE);
            }
        }
        return violations;
    }
}
