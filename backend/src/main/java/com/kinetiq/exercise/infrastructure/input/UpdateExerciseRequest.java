package com.kinetiq.exercise.infrastructure.input;

import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.Exercise;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The new values of an own exercise; the measure cannot change, so there is no field for it. */
record UpdateExerciseRequest(
        @NotBlank @Size(max = Exercise.NAME_MAX_LENGTH) String name,
        @NotNull Category category,
        @NotNull @Size(max = Exercise.EQUIPMENT_MAX_LENGTH) String equipment,
        @Min(1) @Max(Exercise.MAX_SETS) int sets,
        @Min(1) @Max(Exercise.MAX_AMOUNT) int amount,
        @Min(0) @Max(Exercise.MAX_REST_SECONDS) int restSeconds,
        @NotNull @Size(max = Exercise.CUES_MAX_LENGTH) String cues) {}
