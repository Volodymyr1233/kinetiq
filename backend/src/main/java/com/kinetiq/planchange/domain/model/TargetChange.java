package com.kinetiq.planchange.domain.model;

import com.kinetiq.exercise.domain.model.ExerciseId;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * One changed value of one entry, with the value it had. The exercise and the session name are those of the plan the
 * content was made from.
 */
public record TargetChange(
        EntryRef entry,
        ExerciseId exercise,
        String sessionName,
        Target target,
        BigDecimal oldValue,
        BigDecimal newValue) {

    public TargetChange {
        Objects.requireNonNull(entry);
        Objects.requireNonNull(exercise);
        Objects.requireNonNull(sessionName);
        Objects.requireNonNull(target);
        Objects.requireNonNull(oldValue);
        Objects.requireNonNull(newValue);
        if (oldValue.compareTo(newValue) == 0) {
            throw new IllegalArgumentException("a target change must change the value");
        }
    }
}
