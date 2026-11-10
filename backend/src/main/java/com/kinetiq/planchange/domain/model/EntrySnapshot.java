package com.kinetiq.planchange.domain.model;

import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.Measure;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/** One exercise entry of a plan as a plan change sees it; {@code loadKg} is {@code null} when it has no load. */
public record EntrySnapshot(
        ExerciseId exercise,
        Category category,
        Measure measure,
        int sets,
        int amount,
        BigDecimal loadKg,
        int rpe,
        int restSeconds) {

    public EntrySnapshot {
        Objects.requireNonNull(exercise);
        Objects.requireNonNull(category);
        Objects.requireNonNull(measure);
    }

    /** The value of a target; empty for the load of an entry without one. */
    public Optional<BigDecimal> value(Target target) {
        return switch (target) {
            case SETS -> Optional.of(BigDecimal.valueOf(sets));
            case AMOUNT -> Optional.of(BigDecimal.valueOf(amount));
            case LOAD_KG -> Optional.ofNullable(loadKg);
            case RPE -> Optional.of(BigDecimal.valueOf(rpe));
            case REST_SECONDS -> Optional.of(BigDecimal.valueOf(restSeconds));
        };
    }

    /** This entry with one target set to {@code value}; a whole-number target takes the integer part. */
    public EntrySnapshot with(Target target, BigDecimal value) {
        return switch (target) {
            case SETS -> new EntrySnapshot(exercise, category, measure, value.intValue(), amount, loadKg, rpe, restSeconds);
            case AMOUNT -> new EntrySnapshot(exercise, category, measure, sets, value.intValue(), loadKg, rpe, restSeconds);
            case LOAD_KG -> new EntrySnapshot(exercise, category, measure, sets, amount, value, rpe, restSeconds);
            case RPE -> new EntrySnapshot(exercise, category, measure, sets, amount, loadKg, value.intValue(), restSeconds);
            case REST_SECONDS -> new EntrySnapshot(exercise, category, measure, sets, amount, loadKg, rpe, value.intValue());
        };
    }

    /** Sets times repetitions or seconds. */
    public long volume() {
        return (long) sets * amount;
    }
}
