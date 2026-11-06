package com.kinetiq.plan.domain.input;

import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.plan.domain.model.Session;
import java.time.LocalDate;
import java.util.Map;

/**
 * What a client trains on {@code date}, their local date. {@code session} is set only for {@link State#SESSION}, and
 * {@code library} is their trainer's, which gives its entries their names and measure.
 */
public record TodayView(LocalDate date, State state, Session session, Map<ExerciseId, Exercise> library) {

    public TodayView {
        library = Map.copyOf(library);
    }

    public enum State {
        SESSION,
        REST,
        FINISHED
    }
}
