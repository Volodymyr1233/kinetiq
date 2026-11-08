package com.kinetiq.sessionlog.domain.input;

import com.kinetiq.account.domain.model.AccountId;

public interface ViewSessionStatesUseCase {

    /**
     * Where each planned session of plan week {@code week} stands, or of the current plan week when it is {@code null}.
     * A finished plan asked without a week has no sessions. A draft and a missing plan are both
     * {@code PlanNotFoundException}, and a week the plan does not have is {@code PlanWeekOutOfRangeException}.
     */
    SessionStates states(AccountId client, Integer week);
}
