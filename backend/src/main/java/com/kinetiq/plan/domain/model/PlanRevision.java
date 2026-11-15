package com.kinetiq.plan.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * The sessions of a plan as they were before an applied plan change took effect. They are valid for every week before
 * the Monday {@code validUntil}.
 */
public record PlanRevision(PlanId plan, LocalDate validUntil, List<Session> sessions) {

    public PlanRevision {
        Objects.requireNonNull(plan);
        if (Objects.requireNonNull(validUntil).getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("a revision is valid until a Monday");
        }
        sessions = List.copyOf(sessions);
    }
}
