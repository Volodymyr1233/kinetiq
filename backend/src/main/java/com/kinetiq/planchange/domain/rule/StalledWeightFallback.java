package com.kinetiq.planchange.domain.rule;

import com.kinetiq.plan.domain.model.ExerciseEntry;
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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * For every entry with a load of a stalled exercise: the load plus 5%, rounded up to the next 0.5 kg. Nothing else
 * changes; a load that would pass the entry's maximum stays.
 */
public final class StalledWeightFallback implements FixedTransformation {

    private static final BigDecimal FACTOR = new BigDecimal("1.05");
    private static final BigDecimal STEP = new BigDecimal("0.5");
    private static final BigDecimal MAX_LOAD = new BigDecimal(ExerciseEntry.MAX_LOAD_KG);

    @Override
    public Rule rule() {
        return Rule.STALLED_WEIGHT;
    }

    @Override
    public Optional<PlanChangeContent> apply(PlanSnapshot plan, DuePlanChange due) {
        List<TargetChange> changes = new ArrayList<>();
        for (SessionSnapshot session : plan.sessions()) {
            for (int position = 0; position < session.entries().size(); position++) {
                EntrySnapshot entry = session.entries().get(position);
                if (!due.exercises().contains(entry.exercise()) || entry.loadKg() == null) {
                    continue;
                }
                BigDecimal raised = raised(entry.loadKg());
                if (raised.compareTo(MAX_LOAD) <= 0) {
                    changes.add(new TargetChange(
                            new EntryRef(session.day(), position),
                            entry.exercise(),
                            session.name(),
                            Target.LOAD_KG,
                            entry.loadKg(),
                            raised));
                }
            }
        }
        return changes.isEmpty() ? Optional.empty() : Optional.of(PlanChangeContent.ofChanges(changes));
    }

    static BigDecimal raised(BigDecimal load) {
        BigDecimal steps = load.multiply(FACTOR).divide(STEP, 0, RoundingMode.CEILING);
        return steps.multiply(STEP).setScale(ExerciseEntry.LOAD_DECIMALS);
    }
}
