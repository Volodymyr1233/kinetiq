package com.kinetiq.planchange.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.model.PlanChangeId;

/**
 * What a trainer decides on the plan changes of their own clients. Every method answers {@code
 * PlanChangeNotFoundException} for an unknown plan change and another trainer's client's alike, and refuses a plan
 * change that has ended or whose effective week has begun.
 */
public interface DecidePlanChangeUseCase {

    /** Accepts; the plan change is applied when the client accepted already. Accepting twice changes nothing. */
    PlanChangeView accept(AccountId trainer, PlanChangeId id);

    /** Takes the trainer's acceptance back, while the client has not accepted. */
    PlanChangeView undo(AccountId trainer, PlanChangeId id);

    /**
     * Ends the plan change as declined and posts the reason into the conversation with the client, in the same step. A
     * second call is refused, so it posts nothing more.
     */
    PlanChangeView keepCurrent(KeepCurrentPlanCommand command);
}
