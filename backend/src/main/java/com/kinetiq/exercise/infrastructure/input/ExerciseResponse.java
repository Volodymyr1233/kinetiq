package com.kinetiq.exercise.infrastructure.input;

import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.Measure;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/**
 * An exercise in the language of the request; {@code amount} counts repetitions or seconds, as {@code measure} says.
 * {@code starter} is true for a starter exercise, which nobody can edit.
 */
record ExerciseResponse(
        @NotNull UUID id,
        @NotNull String name,
        @NotNull Category category,
        @NotNull String equipment,
        @NotNull Measure measure,
        @NotNull int sets,
        @NotNull int amount,
        @NotNull int restSeconds,
        @NotNull String cues,
        @NotNull int plansUsing,
        @NotNull boolean starter) {}
