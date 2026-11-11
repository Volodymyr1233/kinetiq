package com.kinetiq.planchange.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.model.PlanChangeId;

/**
 * What a client decides on their own plan changes. Every method answers {@code PlanChangeNotFoundException} for an
 * unknown plan change and another client's alike, and refuses a plan change that has ended or whose effective week
 * has begun.
 */
public interface DecideOwnPlanChangeUseCase {

    /** Accepts; the plan change is applied when the trainer accepted already. Accepting twice changes nothing. */
    ClientPlanChangeView accept(AccountId client, PlanChangeId id);

    /** Takes the client's acceptance back, while the trainer has not accepted. */
    ClientPlanChangeView undo(AccountId client, PlanChangeId id);

    /**
     * Ends the plan change as declined and posts the reason into the conversation with the trainer, in the same step. A
     * second call is refused, so it posts nothing more.
     */
    ClientPlanChangeView keepCurrent(KeepCurrentPlanCommand command);
}
