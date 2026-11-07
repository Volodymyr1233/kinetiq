package com.kinetiq.plan.domain.input;

import com.kinetiq.quiz.domain.model.Goal;
import java.time.DayOfWeek;
import java.util.List;

/**
 * What a client may read of their own draft: the days of its sessions in week order, its weeks and its goal. Session
 * names, exercises and loads stay with the trainer until the plan is published.
 */
public record DraftSummary(List<DayOfWeek> trainingDays, int weeks, Goal goal) {

    public DraftSummary {
        trainingDays = List.copyOf(trainingDays);
    }
}
