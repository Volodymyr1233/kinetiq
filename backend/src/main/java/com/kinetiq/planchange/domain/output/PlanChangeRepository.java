package com.kinetiq.planchange.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** The stored plan changes of clients. */
public interface PlanChangeRepository {

    /** Whether the client has a plan change that has not ended; applied and declined ones have ended. */
    boolean hasOpen(AccountId client);

    /** When the latest plan change of the client ended; empty when none has. */
    Optional<Instant> lastEndedAt(AccountId client);

    /** Stores a new plan change; the database refuses a second open one for the same client. */
    PlanChange save(PlanChange planChange);

    Optional<PlanChange> find(PlanChangeId id);

    /** Finds the plan change and locks it until the end of the transaction, so decisions on it follow one another. */
    Optional<PlanChange> findForUpdate(PlanChangeId id);

    /** Stores the decision columns, the state and the end time of a plan change that is stored already. */
    PlanChange update(PlanChange planChange);

    /** The plan change of the client that has not ended, if any. */
    Optional<PlanChange> findOpen(AccountId client);

    /** The plan changes with these ids, in any order. */
    List<PlanChange> findAll(Collection<PlanChangeId> ids);

    /** The latest {@code limit} plan changes of the client that ended, the latest first. */
    List<PlanChange> findEnded(AccountId client, int limit);
}
