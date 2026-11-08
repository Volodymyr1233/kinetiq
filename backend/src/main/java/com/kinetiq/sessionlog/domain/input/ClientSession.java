package com.kinetiq.sessionlog.domain.input;

import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SessionState;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * One session of a client as their trainer reads it in a list: from its session log, or from the plan when it was never
 * started. The state is absent before the plan's publish day, the average RPE while no set is logged, and the session
 * log id when the session was never started. {@code locked} says its 24 hours have ended.
 */
@Getter
public class ClientSession {

    private final LocalDate date;
    private final String name;

    @Getter(AccessLevel.NONE)
    private final SessionState state;

    private final boolean locked;
    private final int setsLogged;
    private final int setsPlanned;

    @Getter(AccessLevel.NONE)
    private final BigDecimal averageRpe;

    @Getter(AccessLevel.NONE)
    private final SessionLogId sessionLogId;

    /** {@code state}, {@code averageRpe} and {@code sessionLogId} may be {@code null}, as the class says. */
    public ClientSession(
            LocalDate date,
            String name,
            SessionState state,
            boolean locked,
            int setsLogged,
            int setsPlanned,
            BigDecimal averageRpe,
            SessionLogId sessionLogId) {
        this.date = Objects.requireNonNull(date);
        this.name = Objects.requireNonNull(name);
        this.state = state;
        this.locked = locked;
        this.setsLogged = setsLogged;
        this.setsPlanned = setsPlanned;
        this.averageRpe = averageRpe;
        this.sessionLogId = sessionLogId;
    }

    public Optional<SessionState> getState() {
        return Optional.ofNullable(state);
    }

    public Optional<BigDecimal> getAverageRpe() {
        return Optional.ofNullable(averageRpe);
    }

    public Optional<SessionLogId> getSessionLogId() {
        return Optional.ofNullable(sessionLogId);
    }
}
