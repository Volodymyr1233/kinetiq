package com.kinetiq.planchange.domain.model;

import java.time.DayOfWeek;
import java.util.Objects;

/**
 * An exercise entry by its place in the plan: a plan has one session per weekday, so the day and the position in the
 * session tell even a repeated exercise apart.
 */
public record EntryRef(DayOfWeek day, int position) {

    public EntryRef {
        Objects.requireNonNull(day);
        if (position < 0) {
            throw new IllegalArgumentException("position must not be negative");
        }
    }
}
