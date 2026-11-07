package com.kinetiq.plan.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.model.Session;
import java.util.List;
import java.util.Optional;

/** The short reads and writes of a drafting, each in its own transaction, so that no model call holds a connection. */
public interface DraftPlanUseCase {

    /** Records that the client's first plan is being drafted. Part of the submit's transaction. */
    void request(AccountId client);

    /** What a drafting needs; empty when the client's answers or trainer are gone. */
    Optional<DraftInput> input(AccountId client);

    /**
     * Ends the drafting and stores the draft with the quiz's goal and 12 weeks, unless the drafting has ended already,
     * the client's answers are gone, or {@code trainer} is no longer their trainer. A first drafting stores a new draft
     * unless the client has a plan by now; a rebuild replaces the stored draft unless it is gone or published. Throws
     * {@code GeneratedPlanRejectedException} when an exercise has left the trainer's library since the prompt was built,
     * which also undoes ending the drafting, so another attempt can store.
     */
    void storeBuilt(AccountId client, AccountId trainer, List<Session> sessions);

    /**
     * Ends the drafting. A first drafting stores an empty failed draft with the quiz's goal and 12 weeks, under the same
     * conditions; a rebuild leaves the stored draft as it is.
     */
    void storeFailed(AccountId client);

    /** The clients whose drafting has lasted too long to be still running, so a restart cannot leave anyone waiting. */
    List<AccountId> overdue();
}
