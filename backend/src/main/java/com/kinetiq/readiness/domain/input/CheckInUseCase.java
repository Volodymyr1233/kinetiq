package com.kinetiq.readiness.domain.input;

import com.kinetiq.account.domain.model.AccountId;

public interface CheckInUseCase {

    /**
     * The client's check-in for today in their time zone, if any, and whether one can still be saved. A draft and a
     * missing plan are both {@code PlanNotFoundException}.
     */
    TodayCheckIn today(AccountId client);

    /**
     * Saves today's check-in, or replaces the answers of the one saved today; the same answers again give the same
     * result. Refused, with nothing changed, as {@code PlanNotFoundException} without a published plan, as
     * {@code NoSessionTodayException} on a rest day and for a finished plan, and as {@code TrainingStartedException}
     * once a set of today's session is logged.
     */
    TodayCheckIn save(SaveCheckInCommand command);
}
