package com.kinetiq.plan.domain.service;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;

import java.time.DayOfWeek;
import java.util.List;

/** The usual days for a number of sessions, which a generated plan starts from (FR-013). */
final class SessionDays {

    private SessionDays() {}

    static List<DayOfWeek> forSessions(int count) {
        return switch (count) {
            case 2 -> List.of(MONDAY, THURSDAY);
            case 3 -> List.of(MONDAY, WEDNESDAY, FRIDAY);
            case 4 -> List.of(MONDAY, TUESDAY, THURSDAY, FRIDAY);
            case 5 -> List.of(MONDAY, TUESDAY, WEDNESDAY, FRIDAY, SATURDAY);
            case 6 -> List.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY);
            default -> throw new IllegalArgumentException("sessions per week out of range: " + count);
        };
    }
}
