package com.kinetiq.sessionlog.domain.input;

import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.sessionlog.domain.model.SessionLog;
import java.util.Map;

/**
 * A session log with what showing it needs: the client's trainer's library, which gives each entry its name and
 * measure. {@code created} is false when the session had been started already.
 */
public record StartedSession(SessionLog log, Map<ExerciseId, Exercise> library, boolean created) {

    public StartedSession {
        library = Map.copyOf(library);
    }
}
