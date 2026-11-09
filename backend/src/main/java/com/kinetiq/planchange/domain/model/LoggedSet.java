package com.kinetiq.planchange.domain.model;

import com.kinetiq.exercise.domain.model.ExerciseId;
import java.math.BigDecimal;
import java.util.Objects;

/** One logged set; {@code loadKg} is null for an exercise without a load. */
public record LoggedSet(ExerciseId exercise, BigDecimal loadKg, int rpe) {

    public LoggedSet {
        Objects.requireNonNull(exercise);
    }
}
