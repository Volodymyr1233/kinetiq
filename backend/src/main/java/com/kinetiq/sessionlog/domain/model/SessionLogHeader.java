package com.kinetiq.sessionlog.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * What the state of a planned session needs from its session log, read without the entries and sets. {@code finishedAt}
 * is {@code null} until the session is finished.
 */
public record SessionLogHeader(SessionLogId id, LocalDate date, Instant startedAt, Instant finishedAt) {

    public SessionLogHeader {
        Objects.requireNonNull(id);
        Objects.requireNonNull(date);
        Objects.requireNonNull(startedAt);
    }

    public Optional<Instant> finished() {
        return Optional.ofNullable(finishedAt);
    }

    /** Whether its sets can still change. */
    public boolean open(Instant now) {
        return EditWindow.isOpen(startedAt, now);
    }
}
