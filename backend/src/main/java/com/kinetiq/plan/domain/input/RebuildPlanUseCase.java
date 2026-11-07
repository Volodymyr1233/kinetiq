package com.kinetiq.plan.domain.input;

import com.kinetiq.account.domain.model.AccountId;

public interface RebuildPlanUseCase {

    /**
     * Records that a new automatic draft of the client's draft plan is being built; the drafting job then builds it.
     * Refused with {@code ClientNotFoundException} for any account but an own client, {@code PlanNotFoundException}
     * when there is no plan, {@code PlanAlreadyPublishedException} for a published plan,
     * {@code PlanDraftingInProgressException} while a drafting runs, and {@code PlanRebuildLimitReachedException} once
     * the trainer has used the day's builds. Only an accepted request counts toward the limit.
     */
    void rebuild(AccountId trainer, AccountId client);
}
