package com.kinetiq.plan.domain.input;

import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.plan.domain.model.Session;
import java.time.LocalDate;
import java.util.Map;

/**
 * What a client trains on {@code date}, their local date. {@code session} is set only for {@link State#SESSION}, and
 * {@code library} is their trainer's, which gives its entries their names and measure. {@code next} is set only for
 * {@link State#REST} and only while the plan has a session after {@code date}.
 */
public record TodayView(LocalDate date, State state, Session session, Next next, Map<ExerciseId, Exercise> library) {

    public TodayView {
        library = Map.copyOf(library);
    }

    public TodayView(LocalDate date, State state, Session session, Map<ExerciseId, Exercise> library) {
        this(date, state, session, null, library);
    }

    /** The first session after today and the date it falls on. */
    public record Next(LocalDate date, Session session) {}

    public enum State {
        SESSION,
        REST,
        FINISHED
    }
}
