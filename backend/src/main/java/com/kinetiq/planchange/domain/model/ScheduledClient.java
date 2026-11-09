package com.kinetiq.planchange.domain.model;

import com.kinetiq.account.domain.model.AccountId;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

/** A client with a published plan: when to check them and what they were last checked for. */
public record ScheduledClient(
        AccountId client, ZoneId zone, LocalDate firstWeek, int weeks, Optional<LocalDate> lastCheckedWeek) {

    private static final int CHECK_HOUR = 3;

    /**
     * The Monday of the week that {@code now} is checked for: the client's local time minus the check hour, so that
     * Monday 03:00 is a plain date boundary. Done on local time, not on the instant, so daylight saving cannot shift it.
     */
    public LocalDate checkWeek(Instant now) {
        return now.atZone(zone)
                .toLocalDateTime()
                .minusHours(CHECK_HOUR)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public boolean isDue(Instant now) {
        return lastCheckedWeek.map(checked -> checkWeek(now).isAfter(checked)).orElse(true);
    }
}
