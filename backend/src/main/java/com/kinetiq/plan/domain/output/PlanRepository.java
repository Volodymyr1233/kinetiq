package com.kinetiq.plan.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.model.Plan;
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
}
