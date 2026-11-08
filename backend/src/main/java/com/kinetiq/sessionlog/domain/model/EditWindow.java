package com.kinetiq.sessionlog.domain.model;

import java.time.Duration;
import java.time.Instant;

/** The 24 hours, counted from the start of a session whether or not it was finished, in which its sets can change. */
final class EditWindow {

    private static final Duration LENGTH = Duration.ofHours(24);

    private EditWindow() {}

    static boolean isOpen(Instant startedAt, Instant now) {
        return now.isBefore(startedAt.plus(LENGTH));
    }
}
