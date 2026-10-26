package com.kinetiq.exercise.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.ExerciseId;

/** Replaces the values of the trainer's exercise {@code id}; the measure cannot change, so it is not here. */
public record UpdateExerciseCommand(
        AccountId trainer,
        ExerciseId id,
        String name,
        Category category,
        String equipment,
        int sets,
        int amount,
        int restSeconds,
        String cues) {}
