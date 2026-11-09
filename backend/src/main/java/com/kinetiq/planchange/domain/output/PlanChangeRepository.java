package com.kinetiq.planchange.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.model.DuePlanChange;
import java.time.Instant;
import java.util.Optional;

/** The plan changes of a client, as far as detecting a due one needs them. */
public interface PlanChangeRepository {

    /** Whether the client has a plan change that has not ended; an accepted one ends when its effective week begins. */
    boolean hasOpen(AccountId client);

    /** When the latest plan change of the client ended; empty when none has. */
    Optional<Instant> lastEndedAt(AccountId client);

    /** Stores the due plan change as an open one. */
    void open(DuePlanChange due);
}
