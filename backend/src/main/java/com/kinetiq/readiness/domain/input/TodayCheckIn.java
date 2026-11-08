package com.kinetiq.readiness.domain.input;

import com.kinetiq.readiness.domain.model.CheckIn;
import java.time.LocalDate;
import java.util.Objects;

/**
 * The client's check-in on {@code date}, their local date. {@code open} tells whether one can still be saved today: a
 * session day with no set logged yet. {@code checkIn} is {@code null} until the client checks in.
 */
public record TodayCheckIn(LocalDate date, boolean open, CheckIn checkIn) {

    public TodayCheckIn {
        Objects.requireNonNull(date);
    }
}
