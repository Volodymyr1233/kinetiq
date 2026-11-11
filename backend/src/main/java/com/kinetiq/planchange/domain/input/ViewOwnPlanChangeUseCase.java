package com.kinetiq.planchange.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.model.PlanChangeId;

/** What a client reads of their own plan changes, with title and reason in the client's language. */
public interface ViewOwnPlanChangeUseCase {

    /** The client's plan change that has not ended; {@code PlanChangeNotFoundException} when there is none. */
    ClientPlanChangeView current(AccountId client);

    /** One of the client's own plan changes, ended ones too; {@code PlanChangeNotFoundException} for any other. */
    ClientPlanChangeView get(AccountId client, PlanChangeId id);
}
