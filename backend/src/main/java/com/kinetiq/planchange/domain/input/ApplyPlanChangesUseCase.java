package com.kinetiq.planchange.domain.input;

import com.kinetiq.planchange.domain.model.PlanChangeId;
import java.util.List;

/** Ends the plan changes whose effective week has begun: applies the ones both people accepted, expires the rest. */
public interface ApplyPlanChangesUseCase {

    /** The plan changes whose effective week has begun in the time zone of their client and that are not settled yet. */
    List<PlanChangeId> due();

    /**
     * Settles one plan change, once: it does nothing when the plan change is settled already or its week has not begun,
     * so a repeated or late run gives the same result as one on time.
     */
    void settle(PlanChangeId id);
}
