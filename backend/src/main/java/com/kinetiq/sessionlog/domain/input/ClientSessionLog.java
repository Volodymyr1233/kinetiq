package com.kinetiq.sessionlog.domain.input;

import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.readiness.domain.model.Adjustment;
import com.kinetiq.sessionlog.domain.model.SessionLog;
import java.util.Map;
import java.util.Objects;

/**
 * One session log of a client as their trainer reads it: the log with its sets, the session as the trainer's lists
 * show it, the trainer's library, which gives each entry its name and measure, and the adjustment of its date over the
 * log's own copy, {@code null} without one.
 */
public record ClientSessionLog(
        SessionLog log, ClientSession session, Map<ExerciseId, Exercise> library, Adjustment adjustment) {

    public ClientSessionLog {
        Objects.requireNonNull(log);
        Objects.requireNonNull(session);
        library = Map.copyOf(library);
    }
}
