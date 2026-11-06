package com.kinetiq.plan.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.model.Session;
import java.util.List;
import java.util.Optional;

/** The short reads and writes of a drafting, each in its own transaction, so that no model call holds a connection. */
public interface DraftPlanUseCase {

    /** What a drafting needs; empty when the client's answers or trainer are gone. */
    Optional<DraftInput> input(AccountId client);

    /**
     * Stores the draft with the quiz's goal and 12 weeks, unless the client has a plan by now, or their answers are gone,
     * or {@code trainer} is no longer their trainer. Throws {@code GeneratedPlanRejectedException} when an exercise has
     * left the trainer's library since the prompt was built.
     */
    void storeBuilt(AccountId client, AccountId trainer, List<Session> sessions);

    /** Stores an empty failed draft with the quiz's goal and 12 weeks, under the same conditions as a built one. */
    void storeFailed(AccountId client);
}
