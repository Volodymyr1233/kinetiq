package com.kinetiq.plan.domain.input;

import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.plan.domain.model.Plan;
import java.util.Map;

/**
 * A plan with what reading it needs: {@code currentWeek}, {@code null} for a draft, in the client's time zone, and the
 * trainer's library, which gives each entry its name and measure.
 */
public record PlanDetails(Plan plan, Integer currentWeek, Map<ExerciseId, Exercise> library) {

    public PlanDetails {
        library = Map.copyOf(library);
    }
}
