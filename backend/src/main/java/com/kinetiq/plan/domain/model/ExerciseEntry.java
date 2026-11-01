package com.kinetiq.plan.domain.model;

import com.kinetiq.exercise.domain.model.ExerciseId;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * One exercise in its place in a session, with its targets. {@code amount} counts repetitions or seconds, as the
 * exercise measures; {@code loadKg} is {@code null} when there is no added weight.
 */
public record ExerciseEntry(ExerciseId exercise, int sets, int amount, BigDecimal loadKg, int rpe, int restSeconds) {

    public ExerciseEntry {
        Objects.requireNonNull(exercise);
    }
}
