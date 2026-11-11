package com.kinetiq.planchange.domain.input;

import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.model.ExerciseLabel;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeText;
import java.util.Map;
import java.util.Objects;

/**
 * A plan change as its client reads it: the name of their trainer, its text in the client's language and the label of
 * every exercise it shows.
 */
public record ClientPlanChangeView(
        PlanChange planChange, String counterpartName, PlanChangeText text, Map<ExerciseId, ExerciseLabel> exercises) {

    public ClientPlanChangeView {
        Objects.requireNonNull(planChange);
        Objects.requireNonNull(counterpartName);
        Objects.requireNonNull(text);
        exercises = Map.copyOf(exercises);
    }
}
