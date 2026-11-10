package com.kinetiq.messaging.domain.model;

import com.kinetiq.exercise.domain.model.LocalizedText;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SetLogId;
import java.time.LocalDate;
import java.util.Objects;

/**
 * The logged set a message is about, as it is when the message is read: its session, its exercise, its place in the
 * session and its RPE.
 */
public record AboutSet(
        SetLogId setLog,
        SessionLogId sessionLog,
        String sessionName,
        LocalDate sessionDate,
        LocalizedText exerciseName,
        int entryPosition,
        int setNumber,
        int rpe) {

    public AboutSet {
        Objects.requireNonNull(setLog);
        Objects.requireNonNull(sessionLog);
        Objects.requireNonNull(sessionName);
        Objects.requireNonNull(sessionDate);
        Objects.requireNonNull(exerciseName);
    }
}
