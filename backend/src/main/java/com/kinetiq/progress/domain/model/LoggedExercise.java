package com.kinetiq.progress.domain.model;

import com.kinetiq.exercise.domain.model.ExerciseId;
import java.time.LocalDate;
import java.util.Objects;

/**
 * One exercise in one session log: the log's date, the exercise's top set in that session, and the sum and count of
 * the RPE of its logged sets.
 */
public record LoggedExercise(LocalDate date, ExerciseId exercise, TopSet top, int rpeSum, int sets) {

    public LoggedExercise {
        Objects.requireNonNull(date);
        Objects.requireNonNull(exercise);
        Objects.requireNonNull(top);
        if (sets < 1) {
            throw new IllegalArgumentException("a logged exercise has at least one set");
        }
    }
}
