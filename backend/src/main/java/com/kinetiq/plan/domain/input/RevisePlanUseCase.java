package com.kinetiq.plan.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.model.Session;
import java.time.LocalDate;
import java.util.List;

/** Replaces the sessions of a published plan with those of an applied plan change, keeping the old ones. */
public interface RevisePlanUseCase {

    /**
     * Stores the current sessions as a revision valid until {@code effectiveWeek} and saves the plan with {@code sessions}.
     *
     * @param expectedVersion the version of the plan the caller built {@code sessions} from
     * @throws com.kinetiq.plan.domain.exception.PlanChangedException when the plan was saved since that version
     * @throws com.kinetiq.plan.domain.exception.PlanNotFoundException when the client has no published plan
     */
    void revise(AccountId client, long expectedVersion, LocalDate effectiveWeek, List<Session> sessions);
}
