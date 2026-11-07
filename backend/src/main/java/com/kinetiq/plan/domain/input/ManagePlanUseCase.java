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
     * Refused while the client's plan is being drafted, when the command's version is not the stored plan's, and on a
     * first save before the client has submitted the quiz.
     */
    PlanDetails save(SavePlanCommand command);

    /**
     * Publishes the saved draft; week 1 becomes the current week in the client's time zone. Refused unless
     * {@code version} matches the stored plan's, so a trainer never publishes content they have not seen, and unless
     * the plan is complete, not published yet and not being drafted.
     */
    PlanDetails publish(AccountId trainer, AccountId client, long version);
}
