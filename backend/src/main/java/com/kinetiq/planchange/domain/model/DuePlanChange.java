package com.kinetiq.planchange.domain.model;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 * What a run hands over when a rule fires: the client and their time zone, the rule, the weeks it looked at (oldest
 * first), the exercises it concerns and the numbers it compared.
 */
public record DuePlanChange(
        AccountId client,
        ZoneId zone,
        Rule rule,
        LocalDate windowStart,
        LocalDate windowEnd,
        Set<ExerciseId> exercises,
        List<Figure> figures,
        List<WeekTraining> weeks) {

    public DuePlanChange {
        exercises = Set.copyOf(exercises);
        figures = List.copyOf(figures);
        weeks = List.copyOf(weeks);
    }
}
