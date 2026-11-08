package com.kinetiq.sessionlog.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/** Where one planned session stands. Derived from the calendar and the session log, never stored. */
public enum SessionState {
    DONE,
    IN_PROGRESS,
    MISSED,
    TODAY,
    PLANNED;

    /**
     * The state of the session planned on {@code date}; {@code today} is the client's local date and {@code publishedOn}
     * the day the plan was published. A session that was never started is missed only on or after that day, so a day
     * before the client had the plan has no state. A started session is missed only once it can no longer be finished.
     */
    public static Optional<SessionState> of(
            LocalDate date, LocalDate today, LocalDate publishedOn, Optional<SessionLogHeader> log, Instant now) {
        if (log.isPresent()) {
            SessionLogHeader header = log.get();
            if (header.finished().isPresent()) {
                return Optional.of(DONE);
            }
            return Optional.of(header.open(now) ? IN_PROGRESS : MISSED);
        }
        if (date.equals(today)) {
            return Optional.of(TODAY);
        }
        if (date.isAfter(today)) {
            return Optional.of(PLANNED);
        }
        return date.isBefore(publishedOn) ? Optional.empty() : Optional.of(MISSED);
    }
}
