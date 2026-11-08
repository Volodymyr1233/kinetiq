package com.kinetiq.sessionlog.domain.input;

import com.kinetiq.account.domain.model.AccountId;

public interface ViewClientTrainingUseCase {

    /**
     * The current plan week, the four most recent sessions and the quiz answers of one of the trainer's own clients, by
     * the client's local date. Another trainer's client and an unknown one are both {@code ClientNotFoundException}.
     */
    ClientOverview overview(AccountId trainer, AccountId client);
}
