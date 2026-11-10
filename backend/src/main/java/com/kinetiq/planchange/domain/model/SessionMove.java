package com.kinetiq.planchange.domain.model;

import com.kinetiq.exercise.domain.model.ExerciseId;
import java.time.DayOfWeek;
import java.util.Objects;

/**
 * An entry of the removed session that goes to the end of the session on {@code toDay}, with its exercise as the plan
 * had it.
 */
public record SessionMove(EntryRef from, ExerciseId exercise, DayOfWeek toDay) {

    public SessionMove {
        Objects.requireNonNull(from);
        Objects.requireNonNull(exercise);
        Objects.requireNonNull(toDay);
    }
}
