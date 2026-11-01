package com.kinetiq.plan.domain.input;

import com.kinetiq.account.domain.model.AccountId;

public interface ManagePlanUseCase {

    /**
     * The plan of one of the trainer's own clients. {@code ClientNotFoundException} for any other account,
     * {@code PlanNotFoundException} for an own client with no plan.
     */
    PlanDetails get(AccountId trainer, AccountId client);

    /**
     * Saves the plan as a whole: a first save creates a draft, a later one replaces its goal, length and sessions.
     * Refused unless the command's version matches the stored plan's, and on a first save unless the client has
     * submitted the quiz.
     */
    PlanDetails save(SavePlanCommand command);
}
