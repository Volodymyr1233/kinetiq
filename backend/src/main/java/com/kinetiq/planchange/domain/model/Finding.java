package com.kinetiq.planchange.domain.model;

import com.kinetiq.exercise.domain.model.ExerciseId;
import java.util.Set;

/** A rule that fired, with the exercises it concerns. */
public record Finding(Rule rule, Set<ExerciseId> exercises) {

    public Finding {
        exercises = Set.copyOf(exercises);
    }
}
