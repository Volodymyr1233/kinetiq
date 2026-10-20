package com.kinetiq.exercise.infrastructure.input;

import jakarta.validation.constraints.NotNull;
import java.util.List;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/** The matching exercises, and {@code total}, the size of the whole library whatever the filters. */
record ExerciseListResponse(@NotNull int total, @NotNull List<ExerciseResponse> exercises) {}
