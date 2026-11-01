package com.kinetiq.plan.domain.model;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Objects;

/** A named training occasion on one day of the week, with its exercises in order. */
public record Session(String name, DayOfWeek day, List<ExerciseEntry> entries) {

    public Session {
        Objects.requireNonNull(name);
        Objects.requireNonNull(day);
        entries = List.copyOf(entries);
    }
}
