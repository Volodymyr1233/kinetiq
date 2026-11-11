package com.kinetiq.readiness.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.readiness.domain.model.Adjustment;
import com.kinetiq.readiness.domain.model.CheckIn;
import com.kinetiq.readiness.domain.model.Planned;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CheckInUseCase {

    /**
     * The client's check-in for today in their time zone, if any, with today's adjustment, and whether one can still be
     * saved. A draft and a missing plan are both {@code PlanNotFoundException}.
     */
    TodayCheckIn today(AccountId client);

    /**
     * Saves today's check-in, or replaces the answers of the one saved today; the same answers again give the same
     * result, and different ones bring the lighter targets back. Refused, with nothing changed, as
     * {@code PlanNotFoundException} without a published plan, as {@code NoSessionTodayException} on a rest day and for a
     * finished plan, and as {@code TrainingStartedException} once a set of today's session is logged.
     */
    TodayCheckIn save(SaveCheckInCommand command);

    /**
     * Switches today's targets between the lighter and the original ones; the choice in use already gives the same
     * result. Refused, with nothing changed, as for {@link #save}, and as {@code NoAdjustmentException} when today has no
     * check-in or no lighter targets.
     */
    TodayCheckIn choose(ChooseTargetsCommand command);

    /**
     * The adjustment of the client's check-in on {@code date} over that day's planned entries, in their order; empty
     * without a check-in or when nothing gets lighter.
     */
    Optional<Adjustment> adjustmentOn(AccountId client, LocalDate date, List<Planned> entries);

    /**
     * The client's check-ins from {@code from} to {@code to}, both included, oldest first. The caller has checked that it
     * may read this client.
     */
    List<CheckIn> between(AccountId client, LocalDate from, LocalDate to);
}
