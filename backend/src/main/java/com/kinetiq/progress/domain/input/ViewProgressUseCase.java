package com.kinetiq.progress.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.progress.domain.model.Period;

public interface ViewProgressUseCase {

    /** The client's own progress; without a published plan {@code PlanNotFoundException}. */
    Progress own(AccountId client, Period period);

    /**
     * The progress of one of the trainer's own clients, equal to what the client reads. Another trainer's client and an
     * unknown one are both {@code ClientNotFoundException}; a client without a published plan is
     * {@code PlanNotFoundException}.
     */
    Progress ofClient(AccountId trainer, AccountId client, Period period);
}
