package com.kinetiq.plan.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import java.time.Instant;
import java.util.List;

/** The trainer's accepted Build again requests, kept for the daily limit. */
public interface PlanBuildRequests {

    /** Holds the trainer's requests until the transaction ends, so two requests cannot both take the last build. */
    void lock(AccountId trainer);

    /** The times of the trainer's requests after {@code from}, oldest first. */
    List<Instant> since(AccountId trainer, Instant from);

    /** Records a request at {@code at}, and forgets the trainer's requests before {@code forgetBefore}. */
    void record(AccountId trainer, Instant at, Instant forgetBefore);
}
