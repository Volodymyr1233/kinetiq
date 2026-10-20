package com.kinetiq.exercise.domain.output;

import com.kinetiq.exercise.domain.model.Exercise;
import java.util.List;

public interface ExerciseRepository {

    /**
     * Returns the starter exercises, those with no owner. Story #47 adds {@code findVisibleTo} for a trainer's own
     * exercises, so a listing never shows another trainer's exercise by forgetting a filter.
     */
    List<Exercise> findStarter();
}
