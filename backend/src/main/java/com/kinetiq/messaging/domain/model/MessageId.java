package com.kinetiq.messaging.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Chosen by the sender, so every attempt to send one message carries the same id. */
public record MessageId(UUID value) {

    public MessageId {
        Objects.requireNonNull(value);
    }
}
