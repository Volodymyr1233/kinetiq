package com.kinetiq.overview.domain.input;

import com.kinetiq.account.domain.model.AccountId;

public interface ViewOverviewUseCase {

    /**
     * The overview of the trainer's own clients, each by their own local date: a row per client in name order, the
     * tiles across them and the alerts that ask for the trainer. Nobody else's clients, plan changes or drafts.
     */
    Overview overview(AccountId trainer);
}
