package com.kinetiq.messaging.domain.model;

import java.time.Instant;
import java.util.Objects;

/** The newest message of a conversation as a list row shows it; {@code preview} is its beginning. */
public record LastMessage(String preview, boolean sentByViewer, Instant receivedAt) {

    public LastMessage {
        Objects.requireNonNull(preview);
        Objects.requireNonNull(receivedAt);
    }
}
