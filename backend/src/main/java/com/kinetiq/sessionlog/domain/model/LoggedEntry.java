package com.kinetiq.sessionlog.domain.model;

import com.kinetiq.exercise.domain.model.ExerciseId;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * A copy of one planned exercise entry, taken when the session starts, so a later change of the plan does not change a
 * session in progress. {@code amount} counts repetitions or seconds, as the exercise measures; {@code loadKg} is
 * {@code null} when there is no added weight.
 */
public record LoggedEntry(ExerciseId exercise, int sets, int amount, BigDecimal loadKg, int rpe, int restSeconds) {

    public LoggedEntry {
        Objects.requireNonNull(exercise);
    }
}
