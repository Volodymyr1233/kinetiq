package com.kinetiq.plan.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import java.time.Instant;
import java.util.List;

/** Which clients have a first plan being drafted. */
public interface PlanDraftingRepository {

    /** Records that the client's first plan is being drafted from {@code at}. */
    void start(AccountId client, Instant at);

    /** Ends the client's drafting and tells whether there was one. */
    boolean remove(AccountId client);

    /** The clients whose drafting started before {@code instant}. */
    List<AccountId> startedBefore(Instant instant);
}
