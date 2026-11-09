package com.kinetiq.readiness.domain.input;

import com.kinetiq.readiness.domain.model.Adjustment;
import com.kinetiq.readiness.domain.model.CheckIn;
import java.time.LocalDate;
import java.util.Objects;

/**
 * The client's check-in on {@code date}, their local date. {@code open} tells whether one can still be saved, and the
 * targets switched: a session day with no set logged yet. {@code checkIn} is {@code null} until the client checks in,
 * and {@code adjustment} is {@code null} unless today's targets are lighter.
 */
public record TodayCheckIn(LocalDate date, boolean open, CheckIn checkIn, Adjustment adjustment) {

    public TodayCheckIn {
        Objects.requireNonNull(date);
    }

    public TodayCheckIn(LocalDate date, boolean open, CheckIn checkIn) {
        this(date, open, checkIn, null);
    }
}
