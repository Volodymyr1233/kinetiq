package com.kinetiq.planchange.domain.model;

import com.kinetiq.exercise.domain.model.ExerciseId;
import java.util.List;
import java.util.Set;

/** A rule that fired, with the exercises it concerns and the numbers it compared. */
public record Finding(Rule rule, Set<ExerciseId> exercises, List<Figure> figures) {

    public Finding {
        exercises = Set.copyOf(exercises);
        figures = List.copyOf(figures);
    }
}
