package com.kinetiq.plan.domain.input;

import com.kinetiq.account.domain.model.AccountId;

/**
 * Runs one drafting from prompt to stored result. It is deliberately not a {@code *UseCase}: those run in a transaction,
 * which would hold a database connection through model calls of minutes.
 */
public interface PlanDrafting {

    /** Blocks until the drafting has ended, and never throws. */
    void run(AccountId client);
}
