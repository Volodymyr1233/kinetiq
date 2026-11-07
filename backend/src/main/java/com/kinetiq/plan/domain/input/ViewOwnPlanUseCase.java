package com.kinetiq.plan.domain.input;

import com.kinetiq.account.domain.model.AccountId;

public interface ViewOwnPlanUseCase {

    /**
     * The client's published plan for {@code week}, or for the current plan week when it is {@code null}. A draft and a
     * missing plan are both {@code PlanNotFoundException}, so a client never learns of a draft.
     */
    OwnPlanView view(AccountId client, Integer week);

    /**
     * What the client trains today in their time zone: the session on this weekday, a rest day, or the end of the plan.
     * A draft and a missing plan are both {@code PlanNotFoundException}.
     */
    TodayView today(AccountId client);

    /** Marks the client's published plan as seen; repeating it changes nothing. */
    void acknowledge(AccountId client);

    /**
     * The summary of the client's draft with sessions. A failed draft, a draft without sessions, a missing plan and a
     * published plan are all {@code PlanNotFoundException}, so the answer does not say which.
     */
    DraftSummary draftSummary(AccountId client);
}
