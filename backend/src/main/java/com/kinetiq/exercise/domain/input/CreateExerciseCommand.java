package com.kinetiq.exercise.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.Measure;

/** Adds an exercise owned by {@code trainer}; texts are stored trimmed, as typed. */
public record CreateExerciseCommand(
        AccountId trainer,
        String name,
        Category category,
        Measure measure,
        String equipment,
        int sets,
        int amount,
        int restSeconds,
        String cues) {}
