package com.kinetiq.sessionlog.domain.input;

import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SessionState;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * The planned sessions of one plan week, in week order. {@code today} is the client's local date. {@code state} is empty
 * before the plan was published, and {@code sessionLogId} and {@code startedAt} when the session was never started.
 */
public record SessionStates(LocalDate today, List<Entry> sessions) {

    public SessionStates {
        sessions = List.copyOf(sessions);
    }

    public record Entry(
            DayOfWeek day,
            LocalDate date,
            Optional<SessionState> state,
            Optional<SessionLogId> sessionLogId,
            Optional<Instant> startedAt) {}
}
