package com.kinetiq.sessionlog.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * One logged set: its place in the session (the entry's position and the set's number from 1), what the client did,
 * and a copy of the targets it had.
 */
public record SetLog(
        SetLogId id,
        int entryPosition,
        int setNumber,
        SetValues values,
        BigDecimal targetLoadKg,
        int targetAmount,
        int targetRpe,
        Instant loggedAt) {

    public SetLog {
        Objects.requireNonNull(id);
        Objects.requireNonNull(values);
        Objects.requireNonNull(loggedAt);
    }

    SetLog withValues(SetValues changed, Instant at) {
        return new SetLog(id, entryPosition, setNumber, changed, targetLoadKg, targetAmount, targetRpe, at);
    }
}
