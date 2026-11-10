package com.kinetiq.planchange.domain.model;

import com.kinetiq.exercise.domain.model.ExerciseId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/** One number a rule compared, for the week starting on {@code weekStart}. */
public record Figure(FigureKind kind, Optional<ExerciseId> exercise, LocalDate weekStart, BigDecimal value) {

    public Figure {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(exercise);
        Objects.requireNonNull(weekStart);
        Objects.requireNonNull(value);
        if (exercise.isPresent() != (kind == FigureKind.BEST_LOAD_KG)) {
            throw new IllegalArgumentException("only a best load belongs to an exercise");
        }
    }

    /** A figure of the whole week. */
    public static Figure ofWeek(FigureKind kind, LocalDate weekStart, BigDecimal value) {
        return new Figure(kind, Optional.empty(), weekStart, value);
    }

    /** The best load of one exercise in the week. */
    public static Figure bestLoad(ExerciseId exercise, LocalDate weekStart, BigDecimal value) {
        return new Figure(FigureKind.BEST_LOAD_KG, Optional.of(exercise), weekStart, value);
    }
}
