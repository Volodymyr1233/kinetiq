package com.kinetiq.planchange.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.model.PlanChangeId;

/** What a trainer reads of the plan changes of their own clients, with title and reason in the trainer's language. */
public interface ViewPlanChangeUseCase {

    /** One plan change of the trainer's own client; {@code PlanChangeNotFoundException} for any other. */
    PlanChangeView get(AccountId trainer, PlanChangeId id);

    /**
     * The open plan change of the trainer's own client. {@code ClientNotFoundException} for another trainer's client,
     * {@code PlanChangeNotFoundException} when the client has none open.
     */
    PlanChangeSummary openOf(AccountId trainer, AccountId client);
}
