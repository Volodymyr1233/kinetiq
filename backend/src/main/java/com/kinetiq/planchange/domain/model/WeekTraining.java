package com.kinetiq.planchange.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * What a client did in one Monday-to-Sunday week. {@code sessionsInPlan} is how many sessions the plan has a week;
 * {@code plannedSessions} leaves out the days before the plan was published. {@code finishedDays} holds the weekday of
 * every finished session log of the week.
 */
public record WeekTraining(
        LocalDate monday,
        int sessionsInPlan,
        int plannedSessions,
        int finishedSessions,
        List<LoggedSet> sets,
        List<DayOfWeek> finishedDays) {

    public WeekTraining {
        sets = List.copyOf(sets);
        finishedDays = List.copyOf(finishedDays);
    }
}
