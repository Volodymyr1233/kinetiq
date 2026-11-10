package com.kinetiq.planchange.domain.model;

import java.time.DayOfWeek;
import java.util.Objects;

/** The session a plan change takes out of the plan, with its name at that time. */
public record RemovedSession(DayOfWeek day, String name) {

    public RemovedSession {
        Objects.requireNonNull(day);
        Objects.requireNonNull(name);
    }
}
