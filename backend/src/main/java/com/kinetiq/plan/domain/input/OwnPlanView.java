package com.kinetiq.plan.domain.input;

import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.plan.domain.model.Plan;
import java.time.LocalDate;
import java.util.Map;

/**
 * A client's published plan as they read it: {@code week} is the plan week asked for, {@code null} once the plan has
 * finished ({@code currentWeek} above its weeks), and {@code library} is their trainer's, which gives each entry its
 * name and measure.
 */
public record OwnPlanView(
        Plan plan, Week week, int currentWeek, boolean finished, boolean seen, Map<ExerciseId, Exercise> library) {

    public OwnPlanView {
        library = Map.copyOf(library);
    }

    /** A plan week: its number and the Monday it starts on. */
    public record Week(int number, LocalDate startsOn) {}
}
