package com.kinetiq.planchange.domain.model;

import com.kinetiq.exercise.domain.model.Measure;
import java.util.Objects;

/** How a plan change shows an exercise: its library name in the reader's language, and what its amount counts. */
public record ExerciseLabel(String name, Measure measure) {

    public ExerciseLabel {
        Objects.requireNonNull(name);
        Objects.requireNonNull(measure);
    }
}
