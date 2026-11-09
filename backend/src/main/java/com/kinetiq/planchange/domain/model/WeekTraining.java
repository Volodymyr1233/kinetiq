package com.kinetiq.planchange.domain.model;

import java.time.LocalDate;
import java.util.List;

/**
 * What a client did in one Monday-to-Sunday week. {@code sessionsInPlan} is how many sessions the plan has a week;
 * {@code plannedSessions} leaves out the days before the plan was published.
 */
public record WeekTraining(
        LocalDate monday, int sessionsInPlan, int plannedSessions, int finishedSessions, List<LoggedSet> sets) {

    public WeekTraining {
        sets = List.copyOf(sets);
    }
}
