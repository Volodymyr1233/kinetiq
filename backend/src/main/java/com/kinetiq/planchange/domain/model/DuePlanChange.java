package com.kinetiq.planchange.domain.model;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import java.time.LocalDate;
import java.util.Set;

/** What a run hands over when a rule fires: the rule, the weeks it looked at and the exercises it concerns. */
public record DuePlanChange(
        AccountId client, Rule rule, LocalDate windowStart, LocalDate windowEnd, Set<ExerciseId> exercises) {

    public DuePlanChange {
        exercises = Set.copyOf(exercises);
    }
}
