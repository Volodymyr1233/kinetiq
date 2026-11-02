package com.kinetiq.plan.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.model.Plan;
import java.time.Instant;
import java.util.Optional;

public interface PlanRepository {

    /** Finds the client's plan. */
    Optional<Plan> findByClient(AccountId client);

    /** Finds the client's plan and locks it until the transaction ends, so a version check cannot be raced. */
    Optional<Plan> findByClientForUpdate(AccountId client);

    /**
     * Inserts the plan or replaces the stored one, and returns it with its new version. Every save raises the version,
     * also when only an entry changed. Throws {@code PlanChangedException} when the client already has another plan.
     */
    Plan save(Plan plan);

    /** Whether the client has seen their published plan. */
    boolean isSeenByClient(AccountId client);

    /**
     * Records that the client saw their published plan, once; a draft and a plan already seen stay as they are. The plan's
     * version is not raised, so a trainer's open editor stays valid.
     */
    void markSeenByClient(AccountId client, Instant at);
}
