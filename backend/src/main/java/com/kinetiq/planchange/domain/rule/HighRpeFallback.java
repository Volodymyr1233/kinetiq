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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * For every entry of an exercise above RPE 8: repetitions (or seconds) times 0.9, rounded half up, at least one less
 * and never below 1. Sets, load and RPE stay.
 */
public final class HighRpeFallback implements FixedTransformation {

    private static final BigDecimal FACTOR = new BigDecimal("0.9");

    @Override
    public Rule rule() {
        return Rule.HIGH_RPE;
    }

    @Override
    public Optional<PlanChangeContent> apply(PlanSnapshot plan, DuePlanChange due) {
        List<TargetChange> changes = new ArrayList<>();
        for (SessionSnapshot session : plan.sessions()) {
            for (int position = 0; position < session.entries().size(); position++) {
                EntrySnapshot entry = session.entries().get(position);
                int amount = reduced(entry.amount());
                if (due.exercises().contains(entry.exercise()) && amount != entry.amount()) {
                    changes.add(new TargetChange(
                            new EntryRef(session.day(), position),
                            entry.exercise(),
                            session.name(),
                            Target.AMOUNT,
                            BigDecimal.valueOf(entry.amount()),
                            BigDecimal.valueOf(amount)));
                }
            }
        }
        return changes.isEmpty() ? Optional.empty() : Optional.of(PlanChangeContent.ofChanges(changes));
    }

    static int reduced(int amount) {
        int rounded = BigDecimal.valueOf(amount).multiply(FACTOR).setScale(0, RoundingMode.HALF_UP).intValueExact();
        return Math.max(1, Math.min(amount - 1, rounded));
    }
}
