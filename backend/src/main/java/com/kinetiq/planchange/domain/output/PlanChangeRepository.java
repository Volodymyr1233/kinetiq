package com.kinetiq.planchange.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import java.time.Instant;
import java.util.Optional;

/** The stored plan changes of clients. */
public interface PlanChangeRepository {

    /** Whether the client has a plan change that has not ended; an accepted one ends when its effective week begins. */
    boolean hasOpen(AccountId client);

    /** When the latest plan change of the client ended; empty when none has. */
    Optional<Instant> lastEndedAt(AccountId client);

    /** Stores a new plan change; the database refuses a second open one for the same client. */
    PlanChange save(PlanChange planChange);

    Optional<PlanChange> find(PlanChangeId id);

    /** The plan change of the client that has not ended, if any. */
    Optional<PlanChange> findOpen(AccountId client);
}
