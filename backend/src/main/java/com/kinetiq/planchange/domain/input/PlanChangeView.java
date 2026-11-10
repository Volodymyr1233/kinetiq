package com.kinetiq.planchange.domain.input;

import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.model.ExerciseLabel;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeText;
import java.util.Map;
import java.util.Objects;

/** A plan change as its trainer reads it: the client's name, its text and the label of every exercise it shows. */
public record PlanChangeView(
        PlanChange planChange, String clientName, PlanChangeText text, Map<ExerciseId, ExerciseLabel> exercises) {

    public PlanChangeView {
        Objects.requireNonNull(planChange);
        Objects.requireNonNull(clientName);
        Objects.requireNonNull(text);
        exercises = Map.copyOf(exercises);
    }
}
