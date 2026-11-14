package com.kinetiq.progress.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.progress.domain.model.Period;
import java.time.LocalDate;

public interface ViewProgressUseCase {

    /** The client's own progress; without a published plan {@code PlanNotFoundException}. */
    Progress own(AccountId client, Period period);

    /**
     * The progress of one of the trainer's own clients, equal to what the client reads. Another trainer's client and an
     * unknown one are both {@code ClientNotFoundException}; a client without a published plan is
     * {@code PlanNotFoundException}.
     */
    Progress ofClient(AccountId trainer, AccountId client, Period period);

    /**
     * The client's own PRs of their session log dated {@code date}, and their week streak, the same as {@code own}
     * gives; without a published plan {@code PlanNotFoundException}.
     */
    Rewards rewards(AccountId client, LocalDate date);
}
