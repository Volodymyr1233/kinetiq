package com.kinetiq.exercise.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ExerciseId(UUID value) {

    public ExerciseId {
        Objects.requireNonNull(value);
    }

    public static ExerciseId random() {
        return new ExerciseId(UUID.randomUUID());
    }
}
