package com.kinetiq.planchange.domain.service;

import com.kinetiq.planchange.domain.exception.PlanChangeNotFoundException;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.model.Side;
import com.kinetiq.planchange.domain.output.PlanChangeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

/**
 * The steps both people share when they decide: read the plan change under a lock so decisions on it follow one
 * another, apply the transition of the aggregate, store the result. Who may decide is checked before.
 */
@RequiredArgsConstructor
class PlanChangeDecider {

    private final PlanChangeRepository planChanges;
    private final Clock clock;

    /** {@code zone} is the client's: the effective week begins in their time zone. */
    PlanChange accept(PlanChangeId id, Side side, ZoneId zone) {
        Instant now = clock.instant();
        return decide(id, change -> change.accept(side, now, LocalDate.ofInstant(now, zone)));
    }

    PlanChange undo(PlanChangeId id, Side side) {
        return decide(id, change -> change.undo(side));
    }

    private PlanChange decide(PlanChangeId id, Function<PlanChange, PlanChange> transition) {
        PlanChange locked = planChanges.findForUpdate(id).orElseThrow(PlanChangeNotFoundException::new);
        PlanChange changed = transition.apply(locked);
        return changed == locked ? locked : planChanges.update(changed);
    }
}
