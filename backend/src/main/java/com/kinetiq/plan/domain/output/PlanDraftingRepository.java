package com.kinetiq.plan.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.model.DraftingKind;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Which clients have a plan being drafted, and why. */
public interface PlanDraftingRepository {

    /** Records that the client's plan is being drafted from {@code at}. */
    void start(AccountId client, Instant at, DraftingKind kind);

    /** Ends the client's drafting and tells which kind it was; empty when there was none. */
    Optional<DraftingKind> remove(AccountId client);

    /** Whether the client's plan is being drafted. */
    boolean exists(AccountId client);

    /** The clients whose drafting started before {@code instant}. */
    List<AccountId> startedBefore(Instant instant);
}
